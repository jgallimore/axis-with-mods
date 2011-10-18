/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis.transport.http;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.encoding.Base64;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPFault;
import org.apache.axis.server.AxisServer;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.utils.NetworkUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;

import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;


public class SimpleAxisWorker implements Runnable {
    protected static Log log =
            LogFactory.getLog(SimpleAxisWorker.class.getName());

    private SimpleAxisServer server;
    private Socket socket;

    // Axis specific constants
    private static String transportName = "SimpleHTTP";

    // HTTP status codes
    private static byte OK[] = ("200 " + Messages.getMessage("ok00")).getBytes();
    private static byte NOCONTENT[] = ("202 " + Messages.getMessage("ok00") + "\n\n").getBytes();
    private static byte UNAUTH[] = ("401 " + Messages.getMessage("unauth00")).getBytes();
    private static byte SENDER[] = "400".getBytes();
    private static byte ISE[] = ("500 " + Messages.getMessage("internalError01")).getBytes();

    // HTTP prefix
    private static byte HTTP[] = "HTTP/1.0 ".getBytes();

    // Standard MIME headers for XML payload
    private static byte XML_MIME_STUFF[] =
            ("\r\nContent-Type: text/xml; charset=utf-8\r\n" +
            "Content-Length: ").getBytes();

    // Standard MIME headers for HTML payload
    private static byte HTML_MIME_STUFF[] =
            ("\r\nContent-Type: text/html; charset=utf-8\r\n" +
            "Content-Length: ").getBytes();

    // Mime/Content separator
    private static byte SEPARATOR[] = "\r\n\r\n".getBytes();

    // Tiddly little response
//    private static final String responseStr =
//            "<html><head><title>SimpleAxisServer</title></head>" +
//            "<body><h1>SimpleAxisServer</h1>" +
//            Messages.getMessage("reachedServer00") +
//            "</html>";
//    private static byte cannedHTMLResponse[] = responseStr.getBytes();

    // ASCII character mapping to lower case
    private static final byte[] toLower = new byte[256];

    static {
        for (int i = 0; i < 256; i++) {
            toLower[i] = (byte) i;
        }

        for (int lc = 'a'; lc <= 'z'; lc++) {
            toLower[lc + 'A' - 'a'] = (byte) lc;
        }
    }

    // buffer for IO
    private static final int BUFSIZ = 4096;

    // mime header for content length
    private static final byte lenHeader[] = "content-length: ".getBytes();
    private static final int lenLen = lenHeader.length;

    // mime header for content type
    private static final byte typeHeader[] = (HTTPConstants.HEADER_CONTENT_TYPE.toLowerCase() + ": ").getBytes();
    private static final int typeLen = typeHeader.length;

    // mime header for content location
    private static final byte locationHeader[] = (HTTPConstants.HEADER_CONTENT_LOCATION.toLowerCase() + ": ").getBytes();
    private static final int locationLen = locationHeader.length;

    // mime header for soap action
    private static final byte actionHeader[] = "soapaction: ".getBytes();
    private static final int actionLen = actionHeader.length;

    // mime header for cookie
    private static final byte cookieHeader[] = "cookie: ".getBytes();
    private static final int cookieLen = cookieHeader.length;

    // mime header for cookie2
    private static final byte cookie2Header[] = "cookie2: ".getBytes();
    private static final int cookie2Len = cookie2Header.length;

    // HTTP header for authentication
    private static final byte authHeader[] = "authorization: ".getBytes();
    private static final int authLen = authHeader.length;

    // mime header for GET
    private static final byte getHeader[] = "GET".getBytes();

    // mime header for POST
    private static final byte postHeader[] = "POST".getBytes();

    // header ender
    private static final byte headerEnder[] = ": ".getBytes();

    // "Basic" auth string
    private static final byte basicAuth[] = "basic ".getBytes();

    public SimpleAxisWorker(SimpleAxisServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    /**
     * Run method
     */ 
    public void run() {
        try {
            execute();
        } finally {
            SimpleAxisServer.getPool().workerDone(this, false);
        }
    }
    
    /**
     * The main workhorse method.
     */
    public void execute () {
        byte buf[] = new byte[BUFSIZ];
        // create an Axis server
        AxisServer engine = server.getAxisServer();

        // create and initialize a message context
        MessageContext msgContext = new MessageContext(engine);
        Message requestMsg = null;

        // Reusuable, buffered, content length controlled, InputStream
        NonBlockingBufferedInputStream is =
                new NonBlockingBufferedInputStream();

        // buffers for the headers we care about
        StringBuffer soapAction = new StringBuffer();
        StringBuffer httpRequest = new StringBuffer();
        StringBuffer fileName = new StringBuffer();
        StringBuffer cookie = new StringBuffer();
        StringBuffer cookie2 = new StringBuffer();
        StringBuffer authInfo = new StringBuffer();
        StringBuffer contentType = new StringBuffer();
        StringBuffer contentLocation = new StringBuffer();

        Message responseMsg = null;

        // prepare request (do as much as possible while waiting for the
        // next connection).  Note the next two statements are commented
        // out.  Uncomment them if you experience any problems with not
        // resetting state between requests:
        //   msgContext = new MessageContext();
        //   requestMsg = new Message("", "String");
        //msgContext.setProperty("transport", "HTTPTransport");
        msgContext.setTransportName(transportName);

        responseMsg = null;

        try {
            // assume the best
            byte[] status = OK;

            // assume we're not getting WSDL
            boolean doWsdl = false;

            // cookie for this session, if any
            String cooky = null;

            String methodName = null;

            try {
                // wipe cookies if we're doing sessions
                if (server.isSessionUsed()) {
                    cookie.delete(0, cookie.length());
                    cookie2.delete(0, cookie2.length());
                }
                authInfo.delete(0, authInfo.length());

                // read headers
                is.setInputStream(socket.getInputStream());
                // parse all headers into hashtable
                MimeHeaders requestHeaders = new MimeHeaders();
                int contentLength = parseHeaders(is, buf, contentType,
                        contentLocation, soapAction,
                        httpRequest, fileName,
                        cookie, cookie2, authInfo, requestHeaders);
                is.setContentLength(contentLength);

                int paramIdx = fileName.toString().indexOf('?');
                if (paramIdx != -1) {
                    // Got params
                    String params = fileName.substring(paramIdx + 1);
                    fileName.setLength(paramIdx);

                    log.debug(Messages.getMessage("filename00",
                            fileName.toString()));
                    log.debug(Messages.getMessage("params00",
                            params));

                    if ("wsdl".equalsIgnoreCase(params))
                        doWsdl = true;

                    if (params.startsWith("method=")) {
                        methodName = params.substring(7);
                    }
                }

                // Real and relative paths are the same for the
                // SimpleAxisServer
                msgContext.setProperty(Constants.MC_REALPATH,
                        fileName.toString());
                msgContext.setProperty(Constants.MC_RELATIVE_PATH,
                        fileName.toString());
                msgContext.setProperty(Constants.MC_JWS_CLASSDIR,
                        "jwsClasses");
                msgContext.setProperty(Constants.MC_HOME_DIR, ".");

                // !!! Fix string concatenation
                String url = "http://" + getLocalHost() + ":" +
                        server.getServerSocket().getLocalPort() + "/" +
                        fileName.toString();
                msgContext.setProperty(MessageContext.TRANS_URL, url);

                String filePart = fileName.toString();
                if (filePart.startsWith("axis/services/")) {
                    String servicePart = filePart.substring(14);
                    int separator = servicePart.indexOf('/');
                    if (separator > -1) {
                        msgContext.setProperty("objectID",
                                       servicePart.substring(separator + 1));
                        servicePart = servicePart.substring(0, separator);
                    }
                    msgContext.setTargetService(servicePart);
                }

                if (authInfo.length() > 0) {
                    // Process authentication info
                    //authInfo = new StringBuffer("dXNlcjE6cGFzczE=");
                    byte[] decoded = Base64.decode(authInfo.toString());
                    StringBuffer userBuf = new StringBuffer();
                    StringBuffer pwBuf = new StringBuffer();
                    StringBuffer authBuf = userBuf;
                    for (int i = 0; i < decoded.length; i++) {
                        if ((char) (decoded[i] & 0x7f) == ':') {
                            authBuf = pwBuf;
                            continue;
                        }
                        authBuf.append((char) (decoded[i] & 0x7f));
                    }

                    if (log.isDebugEnabled()) {
                        log.debug(Messages.getMessage("user00",
                                userBuf.toString()));
                    }

                    msgContext.setUsername(userBuf.toString());
                    msgContext.setPassword(pwBuf.toString());
                }

                // if get, then return simpleton document as response
                if (httpRequest.toString().equals("GET")) {
                    
                    OutputStream out = socket.getOutputStream();
                    out.write(HTTP);
                    if(fileName.length()==0) {
                        out.write("301 Redirect\nLocation: /axis/\n\n".getBytes());
                        out.flush();
                        return;                        
                    }
                    out.write(status);

                    if (methodName != null) {
                        String body =
                            "<" + methodName + ">" +
//                               args +
                            "</" + methodName + ">";
                        String msgtxt =
                            "<SOAP-ENV:Envelope" +
                            " xmlns:SOAP-ENV=\"" + Constants.URI_SOAP12_ENV + "\">" +
                            "<SOAP-ENV:Body>" + body + "</SOAP-ENV:Body>" +
                            "</SOAP-ENV:Envelope>";

                        ByteArrayInputStream istream =
                            new ByteArrayInputStream(msgtxt.getBytes());
                        requestMsg = new Message(istream);
                    } else if (doWsdl) {
                        engine.generateWSDL(msgContext);

                        Document doc = (Document) msgContext.getProperty("WSDL");
                        if (doc != null) {
                            XMLUtils.normalize(doc.getDocumentElement());
                            String response = XMLUtils.PrettyDocumentToString(doc);
                            byte[] respBytes = response.getBytes();

                            out.write(XML_MIME_STUFF);
                            putInt(buf, out, respBytes.length);
                            out.write(SEPARATOR);
                            out.write(respBytes);
                            out.flush();
                            return;
                        }
                    } else {
                        StringBuffer sb = new StringBuffer();
                        sb.append("<h2>And now... Some Services</h2>\n");
                        Iterator i = engine.getConfig().getDeployedServices();
                        sb.append("<ul>\n");
                        while (i.hasNext()) {
                            ServiceDesc sd = (ServiceDesc)i.next();
                            sb.append("<li>\n");
                            sb.append(sd.getName());
                            sb.append(" <a href=\"services/");
                            sb.append(sd.getName());
                            sb.append("?wsdl\"><i>(wsdl)</i></a></li>\n");
                            ArrayList operations = sd.getOperations();
                            if (!operations.isEmpty()) {
                                sb.append("<ul>\n");
                                for (Iterator it = operations.iterator(); it.hasNext();) {
                                    OperationDesc desc = (OperationDesc) it.next();
                                    sb.append("<li>" + desc.getName());
                                }
                                sb.append("</ul>\n");
                            }
                        }
                        sb.append("</ul>\n");

                        byte [] bytes = sb.toString().getBytes();

                        out.write(HTML_MIME_STUFF);
                        putInt(buf, out, bytes.length);
                        out.write(SEPARATOR);
                        out.write(bytes);
                        out.flush();
                        return;
                    }
                } else {

                    // this may be "" if either SOAPAction: "" or if no SOAPAction at all.
                    // for now, do not complain if no SOAPAction at all
                    String soapActionString = soapAction.toString();
                    if (soapActionString != null) {
                        msgContext.setUseSOAPAction(true);
                        msgContext.setSOAPActionURI(soapActionString);
                    }
                    requestMsg = new Message(is,
                            false,
                            contentType.toString(),
                            contentLocation.toString()
                    );
                }

                // Transfer HTTP headers to MIME headers for request message.
                MimeHeaders requestMimeHeaders = requestMsg.getMimeHeaders();
                for (Iterator i = requestHeaders.getAllHeaders(); i.hasNext(); ) {
                    MimeHeader requestHeader = (MimeHeader) i.next();
                    requestMimeHeaders.addHeader(requestHeader.getName(), requestHeader.getValue());
                }
                msgContext.setRequestMessage(requestMsg);
                // put character encoding of request to message context
                // in order to reuse it during the whole process.   
                String requestEncoding = (String) requestMsg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING);
                if (requestEncoding != null) {
                    msgContext.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, requestEncoding);
                }

                // set up session, if any
                if (server.isSessionUsed()) {
                    // did we get a cookie?
                    if (cookie.length() > 0) {
                        cooky = cookie.toString().trim();
                    } else if (cookie2.length() > 0) {
                        cooky = cookie2.toString().trim();
                    }

                    // if cooky is null, cook up a cooky
                    if (cooky == null) {
                        // fake one up!
                        // make it be an arbitrarily increasing number
                        // (no this is not thread safe because ++ isn't atomic)
                        int i = SimpleAxisServer.sessionIndex++;
                        cooky = "" + i;
                    }

                    msgContext.setSession(server.createSession(cooky));
                }

                // invoke the Axis engine
                engine.invoke(msgContext);

                // Retrieve the response from Axis
                responseMsg = msgContext.getResponseMessage();
                
                if (responseMsg == null) {
                    status = NOCONTENT;
                }
            } catch (Exception e) {
                AxisFault af;
                if (e instanceof AxisFault) {
                    af = (AxisFault) e;
                    log.debug(Messages.getMessage("serverFault00"), af);
                    QName faultCode = af.getFaultCode();
                    if (Constants.FAULT_SOAP12_SENDER.equals(faultCode)) {
                        status = SENDER;
                    } else if ("Server.Unauthorized".equals(af.getFaultCode().getLocalPart())) {
                        status = UNAUTH; // SC_UNAUTHORIZED
                    } else {
                        status = ISE; // SC_INTERNAL_SERVER_ERROR
                    }
                } else {
                    status = ISE; // SC_INTERNAL_SERVER_ERROR
                    af = AxisFault.makeFault(e);
                }

                // There may be headers we want to preserve in the
                // response message - so if it's there, just add the
                // FaultElement to it.  Otherwise, make a new one.
                responseMsg = msgContext.getResponseMessage();
                if (responseMsg == null) {
                    responseMsg = new Message(af);
                    responseMsg.setMessageContext(msgContext);
                } else {
                    try {
                        SOAPEnvelope env = responseMsg.getSOAPEnvelope();
                        env.clearBody();
                        env.addBodyElement(new SOAPFault((AxisFault) e));
                    } catch (AxisFault fault) {
                        // Should never reach here!
                    }
                }
            }

            // synchronize the character encoding of request and response
            String responseEncoding = (String) msgContext.getProperty(SOAPMessage.CHARACTER_SET_ENCODING);
            if (responseEncoding != null && responseMsg != null) {
                responseMsg.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, responseEncoding);
            }
            // Send it on its way...
            OutputStream out = socket.getOutputStream();
            out.write(HTTP);
            out.write(status);

            if (responseMsg != null) {
                if (server.isSessionUsed() && null != cooky &&
                        0 != cooky.trim().length()) {
                    // write cookie headers, if any
                    // don't sweat efficiency *too* badly
                    // optimize at will
                    StringBuffer cookieOut = new StringBuffer();
                    cookieOut.append("\r\nSet-Cookie: ")
                            .append(cooky)
                            .append("\r\nSet-Cookie2: ")
                            .append(cooky);
                    // OH, THE HUMILITY!  yes this is inefficient.
                    out.write(cookieOut.toString().getBytes());
                }

                //out.write(XML_MIME_STUFF);
                out.write(("\r\n" + HTTPConstants.HEADER_CONTENT_TYPE + ": " + responseMsg.getContentType(msgContext.getSOAPConstants())).getBytes());
                // Writing the length causes the entire message to be decoded twice.
                //out.write(("\r\n" + HTTPConstants.HEADER_CONTENT_LENGTH + ": " + responseMsg.getContentLength()).getBytes());
                // putInt(out, response.length);

                // Transfer MIME headers to HTTP headers for response message.
                for (Iterator i = responseMsg.getMimeHeaders().getAllHeaders(); i.hasNext(); ) {
                    MimeHeader responseHeader = (MimeHeader) i.next();
                    out.write('\r');
                    out.write('\n');
                    out.write(responseHeader.getName().getBytes());
                    out.write(headerEnder);
                    out.write(responseHeader.getValue().getBytes());
                }

                out.write(SEPARATOR);
                responseMsg.writeTo(out);
            }

            // out.write(response);
            out.flush();
        } catch (Exception e) {
            log.info(Messages.getMessage("exception00"), e);
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (Exception e) {
            }
        }
        if (msgContext.getProperty(MessageContext.QUIT_REQUESTED) != null) {
            // why then, quit!
            try {
                server.stop();
            } catch (Exception e) {
            }
        }

    }

    protected void invokeMethodFromGet(String methodName, String args) throws Exception {

    }

    /**
     * Read all mime headers, returning the value of Content-Length and
     * SOAPAction.
     * @param is         InputStream to read from
     * @param contentType The content type.
     * @param contentLocation The content location
     * @param soapAction StringBuffer to return the soapAction into
     * @param httpRequest StringBuffer for GET / POST
     * @param cookie first cookie header (if doSessions)
     * @param cookie2 second cookie header (if doSessions)
     * @param headers HTTP headers to transfer to MIME headers
     * @return Content-Length
     */
    private int parseHeaders(NonBlockingBufferedInputStream is,
                             byte buf[],
                             StringBuffer contentType,
                             StringBuffer contentLocation,
                             StringBuffer soapAction,
                             StringBuffer httpRequest,
                             StringBuffer fileName,
                             StringBuffer cookie,
                             StringBuffer cookie2,
                             StringBuffer authInfo,
                             MimeHeaders headers)
            throws java.io.IOException {
        int n;
        int len = 0;

        // parse first line as GET or POST
        n = this.readLine(is, buf, 0, buf.length);
        if (n < 0) {
            // nothing!
            throw new java.io.IOException(Messages.getMessage("unexpectedEOS00"));
        }

        // which does it begin with?
        httpRequest.delete(0, httpRequest.length());
        fileName.delete(0, fileName.length());
        contentType.delete(0, contentType.length());
        contentLocation.delete(0, contentLocation.length());

        if (buf[0] == getHeader[0]) {
            httpRequest.append("GET");
            for (int i = 0; i < n - 5; i++) {
                char c = (char) (buf[i + 5] & 0x7f);
                if (c == ' ')
                    break;
                fileName.append(c);
            }
            log.debug(Messages.getMessage("filename01", "SimpleAxisServer", fileName.toString()));
            return 0;
        } else if (buf[0] == postHeader[0]) {
            httpRequest.append("POST");
            for (int i = 0; i < n - 6; i++) {
                char c = (char) (buf[i + 6] & 0x7f);
                if (c == ' ')
                    break;
                fileName.append(c);
            }
            log.debug(Messages.getMessage("filename01", "SimpleAxisServer", fileName.toString()));
        } else {
            throw new java.io.IOException(Messages.getMessage("badRequest00"));
        }

        while ((n = readLine(is, buf, 0, buf.length)) > 0) {

            if ((n <= 2) && (buf[0] == '\n' || buf[0] == '\r') && (len > 0)) break;

            // RobJ gutted the previous logic; it was too hard to extend for more headers.
            // Now, all it does is search forwards for ": " in the buf,
            // then do a length / byte compare.
            // Hopefully this is still somewhat efficient (Sam is watching!).

            // First, search forwards for ": "
            int endHeaderIndex = 0;
            while (endHeaderIndex < n && toLower[buf[endHeaderIndex]] != headerEnder[0]) {
                endHeaderIndex++;
            }
            endHeaderIndex += 2;
            // endHeaderIndex now points _just past_ the ": ", and is
            // comparable to the various lenLen, actionLen, etc. values

            // convenience; i gets pre-incremented, so initialize it to one less
            int i = endHeaderIndex - 1;

            // which header did we find?
            if (endHeaderIndex == lenLen && matches(buf, lenHeader)) {
                // parse content length

                while ((++i < n) && (buf[i] >= '0') && (buf[i] <= '9')) {
                    len = (len * 10) + (buf[i] - '0');
                }
                headers.addHeader(HTTPConstants.HEADER_CONTENT_LENGTH, String.valueOf(len));

            } else if (endHeaderIndex == actionLen
                    && matches(buf, actionHeader)) {

                soapAction.delete(0, soapAction.length());
                // skip initial '"'
                i++;
                while ((++i < n) && (buf[i] != '"')) {
                    soapAction.append((char) (buf[i] & 0x7f));
                }
                headers.addHeader(HTTPConstants.HEADER_SOAP_ACTION, "\"" + soapAction.toString() + "\"");

            } else if (server.isSessionUsed() && endHeaderIndex == cookieLen
                    && matches(buf, cookieHeader)) {

                // keep everything up to first ;
                while ((++i < n) && (buf[i] != ';') && (buf[i] != '\r') && (buf[i] != '\n')) {
                    cookie.append((char) (buf[i] & 0x7f));
                }
                headers.addHeader("Set-Cookie", cookie.toString());

            } else if (server.isSessionUsed() && endHeaderIndex == cookie2Len
                    && matches(buf, cookie2Header)) {

                // keep everything up to first ;
                while ((++i < n) && (buf[i] != ';') && (buf[i] != '\r') && (buf[i] != '\n')) {
                    cookie2.append((char) (buf[i] & 0x7f));
                }
                headers.addHeader("Set-Cookie2", cookie.toString());

            } else if (endHeaderIndex == authLen && matches(buf, authHeader)) {
                if (matches(buf, endHeaderIndex, basicAuth)) {
                    i += basicAuth.length;
                    while (++i < n && (buf[i] != '\r') && (buf[i] != '\n')) {
                        if (buf[i] == ' ') continue;
                        authInfo.append((char) (buf[i] & 0x7f));
                    }
                    headers.addHeader(HTTPConstants.HEADER_AUTHORIZATION, new String(basicAuth) + authInfo.toString());
                } else {
                    throw new java.io.IOException(
                            Messages.getMessage("badAuth00"));
                }
            } else if (endHeaderIndex == locationLen && matches(buf, locationHeader)) {
                while (++i < n && (buf[i] != '\r') && (buf[i] != '\n')) {
                    if (buf[i] == ' ') continue;
                    contentLocation.append((char) (buf[i] & 0x7f));
                }
                headers.addHeader(HTTPConstants.HEADER_CONTENT_LOCATION, contentLocation.toString());
            } else if (endHeaderIndex == typeLen && matches(buf, typeHeader)) {
                while (++i < n && (buf[i] != '\r') && (buf[i] != '\n')) {
                    if (buf[i] == ' ') continue;
                    contentType.append((char) (buf[i] & 0x7f));
                }
                headers.addHeader(HTTPConstants.HEADER_CONTENT_TYPE, contentLocation.toString());
            } else {
                String customHeaderName = new String(buf, 0, endHeaderIndex - 2);
                StringBuffer customHeaderValue = new StringBuffer();
                while (++i < n && (buf[i] != '\r') && (buf[i] != '\n')) {
                    if (buf[i] == ' ') continue;
                    customHeaderValue.append((char) (buf[i] & 0x7f));
                }
                headers.addHeader(customHeaderName, customHeaderValue.toString());
            }

        }
        return len;
    }

    /**
     * does tolower[buf] match the target byte array, up to the target's length?
     */
    public boolean matches(byte[] buf, byte[] target) {
        for (int i = 0; i < target.length; i++) {
            if (toLower[buf[i]] != target[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Case-insensitive match of a target byte [] to a source byte [],
     * starting from a particular offset into the source.
     */
    public boolean matches(byte[] buf, int bufIdx, byte[] target) {
        for (int i = 0; i < target.length; i++) {
            if (toLower[buf[bufIdx + i]] != target[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * output an integer into the output stream
     * @param out       OutputStream to be written to
     * @param value     Integer value to be written.
     */
    private void putInt(byte buf[], OutputStream out, int value)
            throws java.io.IOException {
        int len = 0;
        int offset = buf.length;

        // negative numbers
        if (value < 0) {
            buf[--offset] = (byte) '-';
            value = -value;
            len++;
        }

        // zero
        if (value == 0) {
            buf[--offset] = (byte) '0';
            len++;
        }

        // positive numbers
        while (value > 0) {
            buf[--offset] = (byte) (value % 10 + '0');
            value = value / 10;
            len++;
        }

        // write the result
        out.write(buf, offset, len);
    }

    /**
     * Read a single line from the input stream
     * @param is        inputstream to read from
     * @param b         byte array to read into
     * @param off       starting offset into the byte array
     * @param len       maximum number of bytes to read
     */
    private int readLine(NonBlockingBufferedInputStream is, byte[] b, int off, int len)
            throws java.io.IOException {
        int count = 0, c;

        while ((c = is.read()) != -1) {
            if (c != '\n' && c != '\r') {
                b[off++] = (byte) c;
                count++;
            }
            if (count == len) break;
            if ('\n' == c) {
                int peek = is.peek(); //If the next line begins with tab or space then this is a continuation.
                if (peek != ' ' && peek != '\t') break;
            }
        }
        return count > 0 ? count : -1;
    }

    /**
     * One method for all host name lookups.
     */
    public static String getLocalHost() {
        return NetworkUtils.getLocalHostname();
    }
}

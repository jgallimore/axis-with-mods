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
package org.apache.axis.attachments;

import org.apache.axis.Part;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.IOUtils;
import org.apache.commons.logging.Log;

import javax.activation.DataHandler;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.BufferedInputStream;

/**
 * This simulates the multipart stream.
 *
 * @author Rick Rineholt
 */
public class MultiPartRelatedInputStream extends MultiPartInputStream{

    /** Field log           */
    protected static Log log =
            LogFactory.getLog(MultiPartRelatedInputStream.class.getName());

    /** Field MIME_MULTIPART_RELATED           */
    public static final String MIME_MULTIPART_RELATED = "multipart/related";

    /** Field parts           */
    protected java.util.HashMap parts = new java.util.HashMap();

    /** Field orderedParts           */
    protected java.util.LinkedList orderedParts = new java.util.LinkedList();

    /** Field rootPartLength           */
    protected int rootPartLength = 0;

    /** Field closed           */
    protected boolean closed = false;    // If true the stream has been closed.

    /** Field eos           */
    protected boolean eos =
            false;    // This is set once the SOAP packet has reached the end of stream.

    // protected java.io.InputStream is = null; //The orginal multipart/related stream.
    // This stream controls and manages the  boundary.

    /** Field boundaryDelimitedStream           */
    protected org.apache.axis.attachments.BoundaryDelimitedStream boundaryDelimitedStream =
            null;

    /** Field soapStream           */
    protected java.io.InputStream soapStream =
            null;                            // Set the soap stream once found.

    /** Field soapStreamBDS           */
    protected java.io.InputStream soapStreamBDS =
            null;    // Set to the boundary delimited stream assoc. with soap stream once found.

    /** Field boundary           */
    protected byte[] boundary = null;

    /** Field cachedSOAPEnvelope           */
    protected java.io.ByteArrayInputStream cachedSOAPEnvelope =
            null;                            // Caches the soap stream if it is

    // Still open and a reference to read data in a later attachment occurs.

    /** Field contentLocation           */
    protected String contentLocation = null;

    /** Field contentId           */
    protected String contentId = null;

    /** Field MAX_CACHED */
    private static final int MAX_CACHED = 16 * 1024;

    /**
     * Create a new Multipart stream.
     * @param contentType  the string that holds the contentType
     * @param stream       the true input stream from where the source
     *
     * @throws org.apache.axis.AxisFault if the stream could not be created
     */
    public MultiPartRelatedInputStream(
            String contentType, java.io.InputStream stream)
            throws org.apache.axis.AxisFault {

        super(null);    // don't cache this stream.

        if(!(stream instanceof BufferedInputStream)) {
            stream = new BufferedInputStream(stream);
        }
        
        try {

            // First find the start and boundary parameters. There are real weird rules regard what
            // can be in real headers what needs to be escaped etc  let mail parse it.
            javax.mail.internet.ContentType ct =
                    new javax.mail.internet.ContentType(contentType);
            String rootPartContentId =
                    ct.getParameter("start");       // Get the root part content.

            if (rootPartContentId != null) {
                rootPartContentId = rootPartContentId.trim();

                if (rootPartContentId.startsWith("<")) {
                    rootPartContentId = rootPartContentId.substring(1);
                }

                if (rootPartContentId.endsWith(">")) {
                    rootPartContentId = rootPartContentId.substring(0,
                            rootPartContentId.length() - 1);
                }

            }

            if(ct.getParameter("boundary") != null) {
                String boundaryStr =
                        "--"
                        + ct.getParameter(
                                "boundary");    // The boundary with -- add as always the case.


                // if start is null then the first attachment is the rootpart
                // First read the start boundary -- this is done with brute force since the servlet may swallow the crlf between headers.
                // after this we use the more efficient boundarydelimeted stream.  There should never be any data here anyway.
                byte[][] boundaryMarker = new byte[2][boundaryStr.length() + 2];

                IOUtils.readFully(stream, boundaryMarker[0]);

                boundary = (boundaryStr + "\r\n").getBytes("US-ASCII");

                int current = 0;

                // This just goes brute force one byte at a time to find the first boundary.
                // in most cases this just a crlf.
                for (boolean found = false; !found; ++current) {
                    if (!(found =
                            java.util.Arrays.equals(boundaryMarker[current & 0x1],
                                    boundary))) {
                        System.arraycopy(boundaryMarker[current & 0x1], 1,
                                boundaryMarker[(current + 1) & 0x1], 0,
                                boundaryMarker[0].length - 1);

                        if (stream.read(
                                boundaryMarker[(current + 1) & 0x1],
                                boundaryMarker[0].length - 1, 1) < 1) {
                            throw new org.apache.axis.AxisFault(
                                    Messages.getMessage(
                                            "mimeErrorNoBoundary", new String(boundary)));
                        }
                    }
                }

                // after the first boundary each boundary will have a cr lf at the beginning since after the data in any part there
                // is a cr lf added to put the boundary at the begining of a line.
                boundaryStr = "\r\n" + boundaryStr;
                boundary = boundaryStr.getBytes("US-ASCII");
            } else {
                // Since boundary is not specified, we try to find one.
                for (boolean found = false; !found;) {
                    boundary= readLine(stream);
                    if( boundary == null)
                        throw new org.apache.axis.AxisFault(
                                Messages.getMessage(
                                        "mimeErrorNoBoundary", "--"));
                     found = boundary.length >4  && boundary[2] == '-' &&  boundary[3]== '-';
                }
              }

            // create the boundary delmited stream.
            boundaryDelimitedStream =
                    new org.apache.axis.attachments.BoundaryDelimitedStream(stream,
                            boundary, 1024);

            // Now read through all potential streams until we have found the root part.
            String contentTransferEncoding = null;

            do {
                contentId = null;
                contentLocation = null;
                contentTransferEncoding = null;

                // Read this attachments headers from the stream.
                javax.mail.internet.InternetHeaders headers =
                        new javax.mail.internet.InternetHeaders(
                                boundaryDelimitedStream);

                // Use java mail utility to read through the headers.
                contentId = headers.getHeader(HTTPConstants.HEADER_CONTENT_ID,
                        null);

                // Clean up the headers and remove any < >
                if (contentId != null) {
                    contentId = contentId.trim();

                    if (contentId.startsWith("<")) {
                        contentId = contentId.substring(1);
                    }

                    if (contentId.endsWith(">")) {
                        contentId = contentId.substring(0, contentId.length()
                                - 1);
                    }

                    contentId = contentId.trim();

                  //  if (!contentId.startsWith("cid:")) {
                  //      contentId =
                  //              "cid:"
                  //              + contentId;        // make sure its identified as cid
                  //  }
                }

                contentLocation =
                        headers.getHeader(HTTPConstants.HEADER_CONTENT_LOCATION,
                                null);

                if (contentLocation != null) {
                    contentLocation = contentLocation.trim();

                    if (contentLocation.startsWith("<")) {
                        contentLocation = contentLocation.substring(1);
                    }

                    if (contentLocation.endsWith(">")) {
                        contentLocation = contentLocation.substring(
                                0, contentLocation.length() - 1);
                    }

                    contentLocation = contentLocation.trim();
                }

                contentType =
                        headers.getHeader(HTTPConstants.HEADER_CONTENT_TYPE, null);

                if (contentType != null) {
                    contentType = contentType.trim();
                }

                contentTransferEncoding = headers.getHeader(
                        HTTPConstants.HEADER_CONTENT_TRANSFER_ENCODING, null);

                if (contentTransferEncoding != null) {
                    contentTransferEncoding = contentTransferEncoding.trim();
                }

                java.io.InputStream decodedStream = boundaryDelimitedStream;

                if ((contentTransferEncoding != null)
                        && (0 != contentTransferEncoding.length())) {
                    decodedStream = MimeUtility.decode(decodedStream,
                            contentTransferEncoding);
                }

                if ((rootPartContentId != null) && !rootPartContentId.equals(
                        contentId)) {    // This is a part that has come in prior to the root part. Need to buffer it up.
                    javax.activation.DataHandler dh =
                            new javax.activation.DataHandler(
                                    new org.apache.axis.attachments.ManagedMemoryDataSource(
                                            decodedStream, MAX_CACHED, contentType, true));
                    AttachmentPart ap = new AttachmentPart(dh);

                    if (contentId != null) {
                        ap.setMimeHeader(HTTPConstants.HEADER_CONTENT_ID,
                                contentId);
                    }

                    if (contentLocation != null) {
                        ap.setMimeHeader(HTTPConstants.HEADER_CONTENT_LOCATION,
                                contentLocation);
                    }

                    for (java.util.Enumeration en =
                            headers.getNonMatchingHeaders(new String[]{
                                HTTPConstants.HEADER_CONTENT_ID,
                                HTTPConstants.HEADER_CONTENT_LOCATION,
                                HTTPConstants.HEADER_CONTENT_TYPE}); en.hasMoreElements();) {
                        javax.mail.Header header =
                                (javax.mail.Header) en.nextElement();
                        String name = header.getName();
                        String value = header.getValue();

                        if ((name != null) && (value != null)) {
                            name = name.trim();

                            if (name.length() != 0) {
                                ap.addMimeHeader(name, value);
                            }
                        }
                    }

                    addPart(contentId, contentLocation, ap);

                    boundaryDelimitedStream =
                            boundaryDelimitedStream.getNextStream();    // Gets the next stream.
                }
            } while ((null != boundaryDelimitedStream)
                    && (rootPartContentId != null)
                    && !rootPartContentId.equals(contentId));

            if (boundaryDelimitedStream == null) {
                throw new org.apache.axis.AxisFault(
                        Messages.getMessage("noRoot", rootPartContentId));
            }

            soapStreamBDS = boundaryDelimitedStream;

            if ((contentTransferEncoding != null)
                    && (0 != contentTransferEncoding.length())) {
                soapStream = MimeUtility.decode(boundaryDelimitedStream,
                        contentTransferEncoding);
            } else {
                soapStream =
                        boundaryDelimitedStream;    // This should be the SOAP part
            }

            // Read from the input stream all attachments prior to the root part.
        } catch (javax.mail.internet.ParseException e) {
            throw new org.apache.axis.AxisFault(
                    Messages.getMessage("mimeErrorParsing", e.getMessage()));
        } catch (java.io.IOException e) {
            throw new org.apache.axis.AxisFault(
                    Messages.getMessage("readError", e.getMessage()));
        } catch (javax.mail.MessagingException e) {
            throw new org.apache.axis.AxisFault(
                    Messages.getMessage("readError", e.getMessage()));
        }
    }

    //when searching for a MIME boundary it MUST be terminated with CR LF. LF alone is NOT sufficient.
    private final byte[] readLine(java.io.InputStream is) throws IOException {

        java.io.ByteArrayOutputStream input = new java.io.ByteArrayOutputStream(1024);
        int c = 0;
        input.write('\r');
        input.write('\n');

        int next = -1;
        for (;c != -1;) {
            c = -1 != next ? next :  is.read();
            next = -1;
            switch (c) {
                case -1:
                break;
                case '\r':
                    next = is.read();
                    if(next == '\n')  //found a line.
                        return input.toByteArray();
                    if(next == -1)  return null;
                    //fall through
                default:
                    input.write((byte)c);
                break;
            }
        }
        //even if there is stuff in buffer if EOF then this can't be a boundary.
        return null;
    }

    public Part getAttachmentByReference(final String[] id)
            throws org.apache.axis.AxisFault {

        // First see if we have read it in yet.
        Part ret = null;

        for (int i = id.length - 1; (ret == null) && (i > -1); --i) {
            ret = (AttachmentPart) parts.get(id[i]);
        }

        if (null == ret) {
            ret = readTillFound(id);
        }

        log.debug(Messages.getMessage("return02",
                "getAttachmentByReference(\"" + id
                + "\"", ((ret == null)
                ? "null"
                : ret.toString())));

        return ret;
    }

    /**
     * Add an <code>AttachmentPart</code> together with its content and location
     * IDs.
     *
     * @param contentId     the content ID
     * @param locationId    the location ID
     * @param ap            the <code>AttachmentPart</code>
     */
    protected void addPart(String contentId, String locationId,
                           AttachmentPart ap) {

        if ((contentId != null) && (contentId.trim().length() != 0)) {
            parts.put(contentId, ap);
        }

        if ((locationId != null) && (locationId.trim().length() != 0)) {
            parts.put(locationId, ap);
        }

        orderedParts.add(ap);
    }

    /** Field READ_ALL           */
    protected static final String[] READ_ALL = {
        " * \0 ".intern()};    // Shouldn't never match

    /**
     * Read all data.
     *
     * @throws org.apache.axis.AxisFault if there was a problem reading all the
     *              data
     */
    protected void readAll() throws org.apache.axis.AxisFault {
        readTillFound(READ_ALL);
    }

    public java.util.Collection getAttachments()
            throws org.apache.axis.AxisFault {

        readAll();

        return orderedParts;
    }

    /**
     * This will read streams in till the one that is needed is found.
     *
     * @param id id is the stream being sought.
     *
     * @return the part for the id
     *
     * @throws org.apache.axis.AxisFault
     */
    protected Part readTillFound(final String[] id)
            throws org.apache.axis.AxisFault {

        if (boundaryDelimitedStream == null) {
            return null;    // The whole stream has been consumed already
        }

        Part ret = null;

        try {
            if (soapStreamBDS
                    == boundaryDelimitedStream) {    // Still on the SOAP stream.
                if (!eos) {    // The SOAP packet has not been fully read yet. Need to store it away.
                    java.io.ByteArrayOutputStream soapdata =
                            new java.io.ByteArrayOutputStream(1024 * 8);
                    byte[] buf =
                            new byte[1024 * 16];
                    int byteread = 0;

                    do {
                        byteread = soapStream.read(buf);

                        if (byteread > 0) {
                            soapdata.write(buf, 0, byteread);
                        }
                    } while (byteread > -1);

                    soapdata.close();

                    soapStream = new java.io.ByteArrayInputStream(
                            soapdata.toByteArray());
                }

                boundaryDelimitedStream =
                        boundaryDelimitedStream.getNextStream();
            }

            // Now start searching for the data.
            if (null != boundaryDelimitedStream) {
                do {
                    String contentType = null;
                    String contentId = null;
                    String contentTransferEncoding = null;
                    String contentLocation = null;

                    // Read this attachments headers from the stream.
                    javax.mail.internet.InternetHeaders headers =
                            new javax.mail.internet.InternetHeaders(
                                    boundaryDelimitedStream);

                    contentId = headers.getHeader("Content-Id", null);

                    if (contentId != null) {
                        contentId = contentId.trim();

                        if (contentId.startsWith("<")) {
                            contentId = contentId.substring(1);
                        }

                        if (contentId.endsWith(">")) {
                            contentId =
                                    contentId.substring(0, contentId.length() - 1);
                        }

                     //   if (!contentId.startsWith("cid:")) {
                     //       contentId = "cid:" + contentId;
                     //   }

                        contentId = contentId.trim();
                    }

                    contentType =
                            headers.getHeader(HTTPConstants.HEADER_CONTENT_TYPE,
                                    null);

                    if (contentType != null) {
                        contentType = contentType.trim();
                    }

                    contentLocation =
                            headers.getHeader(HTTPConstants.HEADER_CONTENT_LOCATION,
                                    null);

                    if (contentLocation != null) {
                        contentLocation = contentLocation.trim();
                    }

                    contentTransferEncoding = headers.getHeader(
                            HTTPConstants.HEADER_CONTENT_TRANSFER_ENCODING, null);

                    if (contentTransferEncoding != null) {
                        contentTransferEncoding =
                                contentTransferEncoding.trim();
                    }

                    java.io.InputStream decodedStream = boundaryDelimitedStream;

                    if ((contentTransferEncoding != null)
                            && (0 != contentTransferEncoding.length())) {
                        decodedStream =
                                MimeUtility.decode(decodedStream,
                                        contentTransferEncoding);
                    }

                    ManagedMemoryDataSource source = new ManagedMemoryDataSource(
                                                        decodedStream, ManagedMemoryDataSource.MAX_MEMORY_DISK_CACHED, contentType, true);
                    DataHandler dh = new DataHandler(source);
                    AttachmentPart ap = new AttachmentPart(dh);

                    if (contentId != null) {
                        ap.setMimeHeader(HTTPConstants.HEADER_CONTENT_ID,
                                contentId);
                    }

                    if (contentLocation != null) {
                        ap.setMimeHeader(HTTPConstants.HEADER_CONTENT_LOCATION,
                                contentLocation);
                    }

                    for (java.util.Enumeration en =
                            headers.getNonMatchingHeaders(new String[]{
                                HTTPConstants.HEADER_CONTENT_ID,
                                HTTPConstants.HEADER_CONTENT_LOCATION,
                                HTTPConstants.HEADER_CONTENT_TYPE}); en.hasMoreElements();) {
                        javax.mail.Header header =
                                (javax.mail.Header) en.nextElement();
                        String name = header.getName();
                        String value = header.getValue();

                        if ((name != null) && (value != null)) {
                            name = name.trim();

                            if (name.length() != 0) {
                                ap.addMimeHeader(name, value);
                            }
                        }
                    }

                    addPart(contentId, contentLocation, ap);

                    for (int i = id.length - 1; (ret == null) && (i > -1);
                         --i) {
                        if ((contentId != null) && id[i].equals(
                                contentId)) {    // This is the part being sought
                            ret = ap;
                        } else if ((contentLocation != null)
                                && id[i].equals(contentLocation)) {
                            ret = ap;
                        }
                    }

                    boundaryDelimitedStream =
                            boundaryDelimitedStream.getNextStream();
                } while ((null == ret) && (null != boundaryDelimitedStream));
            }
        } catch (Exception e) {
            throw org.apache.axis.AxisFault.makeFault(e);
        }

        return ret;
    }

    public String getContentLocation() {
        return contentLocation;
    }

    public String getContentId() {
        return contentId;
    }

    public int read(byte[] b, int off, int len) throws java.io.IOException {

        if (closed) {
            throw new java.io.IOException(Messages.getMessage("streamClosed"));
        }

        if (eos) {
            return -1;
        }

        int read = soapStream.read(b, off, len);

        if (read < 0) {
            eos = true;
        }

        return read;
    }

    public int read(byte[] b) throws java.io.IOException {
        return read(b, 0, b.length);
    }

    public int read() throws java.io.IOException {

        if (closed) {
            throw new java.io.IOException(Messages.getMessage("streamClosed"));
        }

        if (eos) {
            return -1;
        }

        int ret = soapStream.read();

        if (ret < 0) {
            eos = true;
        }

        return ret;
    }

    public void close() throws java.io.IOException {

        closed = true;

        soapStream.close();
    }

    public int available() throws java.io.IOException {
        return (closed || eos) ? 0 : soapStream.available();
    }

}

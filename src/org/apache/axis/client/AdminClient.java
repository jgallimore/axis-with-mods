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

package org.apache.axis.client ;

import org.apache.axis.AxisFault;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.deployment.wsdd.WSDDConstants;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.Options;
import org.apache.axis.utils.StringUtils;
import org.apache.commons.logging.Log;

import javax.xml.rpc.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;


/**
 * An admin client object that can be used both from the command line
 * and programmatically.
 *
 * @author Rob Jellinghaus (robj@unrealities.com)
 * @author Doug Davis (dug@us.ibm.com)
 * @author Simeon Simeonov (simeons@macromedia.com)
 */

public class AdminClient
{
    protected static Log log =
        LogFactory.getLog(AdminClient.class.getName());

    private static ThreadLocal defaultConfiguration = new ThreadLocal();

    /**
     * If the user calls this with an EngineConfiguration object, all
     * AdminClients on this thread will use that EngineConfiguration
     * rather than the default one.  This is primarily to enable the
     * deployment of custom transports and handlers.
     *
     * @param config the EngineConfiguration which should be used
     */
    public static void setDefaultConfiguration(EngineConfiguration config)
    {
        defaultConfiguration.set(config);
    }

    private static String getUsageInfo()
    {
        String lSep = System.getProperty("line.separator");
        StringBuffer msg = new StringBuffer();
        // 26 is the # of lines in resources.properties
        msg.append(Messages.getMessage("acUsage00")).append(lSep);
        msg.append(Messages.getMessage("acUsage01")).append(lSep);
        msg.append(Messages.getMessage("acUsage02")).append(lSep);
        msg.append(Messages.getMessage("acUsage03")).append(lSep);
        msg.append(Messages.getMessage("acUsage04")).append(lSep);
        msg.append(Messages.getMessage("acUsage05")).append(lSep);
        msg.append(Messages.getMessage("acUsage06")).append(lSep);
        msg.append(Messages.getMessage("acUsage07")).append(lSep);
        msg.append(Messages.getMessage("acUsage08")).append(lSep);
        msg.append(Messages.getMessage("acUsage09")).append(lSep);
        msg.append(Messages.getMessage("acUsage10")).append(lSep);
        msg.append(Messages.getMessage("acUsage11")).append(lSep);
        msg.append(Messages.getMessage("acUsage12")).append(lSep);
        msg.append(Messages.getMessage("acUsage13")).append(lSep);
        msg.append(Messages.getMessage("acUsage14")).append(lSep);
        msg.append(Messages.getMessage("acUsage15")).append(lSep);
        msg.append(Messages.getMessage("acUsage16")).append(lSep);
        msg.append(Messages.getMessage("acUsage17")).append(lSep);
        msg.append(Messages.getMessage("acUsage18")).append(lSep);
        msg.append(Messages.getMessage("acUsage19")).append(lSep);
        msg.append(Messages.getMessage("acUsage20")).append(lSep);
        msg.append(Messages.getMessage("acUsage21")).append(lSep);
        msg.append(Messages.getMessage("acUsage22")).append(lSep);
        msg.append(Messages.getMessage("acUsage23")).append(lSep);
        msg.append(Messages.getMessage("acUsage24")).append(lSep);
        msg.append(Messages.getMessage("acUsage25")).append(lSep);
        msg.append(Messages.getMessage("acUsage26")).append(lSep);
        return msg.toString();
    }


    /**
     * the object that represents our call
     */
    protected Call call;

    /**
     * Construct an admin client w/o a logger.
     * If the client cannot create a call object, then it does not throw an exception.
     * Instead it prints a message to {@link System.err}.
     * This is for 'historical reasons'
     */
    public AdminClient()
    {
        try {
            initAdminClient();
        } catch (ServiceException e) {
            System.err.println(Messages.getMessage("couldntCall00") + ": " + e);
            call = null;
        }
    }


    /**
     * this is a somwhat contrived variant constructor, one that throws an exception
     * if things go wrong.
     * @param ignored
     */
    public AdminClient(boolean ignored) throws ServiceException {
        initAdminClient();
    }

    /**
     * core initialisation routine
     *
     * @throws ServiceException
     */
    private void initAdminClient() throws ServiceException {
        // Initialize our Service - allow the user to override the
        // default configuration with a thread-local version (see
        // setDefaultConfiguration() above)
        EngineConfiguration config =
                (EngineConfiguration) defaultConfiguration.get();
        Service service;
        if (config != null) {
            service = new Service(config);
        } else {
            service = new Service();
        }
        call = (Call) service.createCall();
    }


    /**
     * External access to our <code>Call</code< object.
     * This will be null if the non-excepting constructor was used
     * and the construction failed.
     * @return the <code>Call</code> object this instance uses
     */
    public Call getCall()
    {
        return call;
    }

    /**
     * process the options then run a list call
     * @param opts
     * @return
     * @throws Exception
     */
    public String list(Options opts) throws Exception {
        processOpts( opts );
        return list();
    }

    /**
     * send a list command
     * @return the response from the call
     * @throws Exception
     */
    public String list() throws Exception {
        log.debug( Messages.getMessage("doList00") );
        String               str   = "<m:list xmlns:m=\"" + WSDDConstants.URI_WSDD + "\"/>" ;
        ByteArrayInputStream input = new ByteArrayInputStream(str.getBytes());
        return process(input);
    }

    /**
     * process the command line ops, then send a quit command
     * @param opts
     * @return
     * @throws Exception
     */
    public String quit(Options opts) throws Exception {
        processOpts( opts );
        return quit();
    }

    /**
     * root element of the undeploy request
     */
    protected static final String ROOT_UNDEPLOY= WSDDConstants.QNAME_UNDEPLOY.getLocalPart();

    /**
     * make a quit command
     * @return
     * @throws Exception
     */
    public String quit() throws Exception {
        log.debug(Messages.getMessage("doQuit00"));
        String               str   = "<m:quit xmlns:m=\"" + WSDDConstants.URI_WSDD + "\"/>";
        ByteArrayInputStream input = new ByteArrayInputStream(str.getBytes());
        return process(input);
    }

    /**
     * undeploy a handler
     * @param handlerName name of the handler to undeploy
     * @return
     * @throws Exception
     */
    public String undeployHandler(String handlerName) throws Exception {
        log.debug(Messages.getMessage("doQuit00"));
        String               str   = "<m:"+ROOT_UNDEPLOY +" xmlns:m=\"" + WSDDConstants.URI_WSDD + "\">" +
                                     "<handler name=\"" + handlerName + "\"/>"+
                                     "</m:"+ROOT_UNDEPLOY +">" ;
        ByteArrayInputStream input = new ByteArrayInputStream(str.getBytes());
        return process(input);
    }

    /**
     * undeploy a service
     * @param serviceName name of service
     * @return
     * @throws Exception
     */
    public String undeployService(String serviceName) throws Exception {
        log.debug(Messages.getMessage("doQuit00"));
        String               str   = "<m:"+ROOT_UNDEPLOY +" xmlns:m=\"" + WSDDConstants.URI_WSDD + "\">" +
                                     "<service name=\"" + serviceName + "\"/>"+
                                     "</m:"+ROOT_UNDEPLOY +">" ;
        ByteArrayInputStream input = new ByteArrayInputStream(str.getBytes());
        return process(input);
    }

    /**
     * <p>Processes a set of administration commands.</p>
     * <p>The following commands are available:</p>
     * <ul>
     *   <li><code>-l<i>url</i></code> sets the AxisServlet URL</li>
     *   <li><code>-h<i>hostName</i></code> sets the AxisServlet host</li>
     *   <li><code>-p<i>portNumber</i></code> sets the AxisServlet port</li>
     *   <li><code>-s<i>servletPath</i></code> sets the path to the
     *              AxisServlet</li>
     *   <li><code>-f<i>fileName</i></code> specifies that a simple file
     *              protocol should be used</li>
     *   <li><code>-u<i>username</i></code> sets the username</li>
     *   <li><code>-w<i>password</i></code> sets the password</li>
     *   <li><code>-d</code> sets the debug flag (for instance, -ddd would
     *      set it to 3)</li>
     *   <li><code>-t<i>name</i></code> sets the transport chain touse</li>
     *   <li><code>list</code> will list the currently deployed services</li>
     *   <li><code>quit</code> will quit (???)</li>
     *   <li><code>passwd <i>value</i></code> changes the admin password</li>
     *   <li><code><i>xmlConfigFile</i></code> deploys or undeploys
     *       Axis components and web services</li>
     * </ul>
     * <p>If <code>-l</code> or <code>-h -p -s</code> are not set, the
     * AdminClient will invoke
     * <code>http://localhost:8080/axis/servlet/AxisServlet</code>.</p>
     *
     * @param args Commands to process
     * @return XML result or null in case of failure. In the case of multiple
     * commands, the XML results will be concatenated, separated by \n
     * @exception Exception Could be an IO exception, an AxisFault or something else
     */

    public String process(String[] args) throws Exception {
        StringBuffer sb = new StringBuffer();

        Options opts = new Options( args );
        opts.setDefaultURL("http://localhost:8080/axis/services/AdminService");

        if (opts.isFlagSet('d') > 0) {
            // Set logger properties... !!!
        }

        args = opts.getRemainingArgs();

        if ( args == null  || opts.isFlagSet('?') > 0) {
            System.out.println(Messages.getMessage("usage00","AdminClient [Options] [list | <deployment-descriptor-files>]"));
            System.out.println("");
            System.out.println(getUsageInfo());
            return null;
        }

        for ( int i = 0 ; i < args.length ; i++ ) {
            InputStream input = null;

            if ( args[i].equals("list") )
              sb.append( list(opts) );
            else if (args[i].equals("quit"))
              sb.append( quit(opts) );
            else if (args[i].equals("passwd")) {
                System.out.println(Messages.getMessage("changePwd00"));
                if (args[i + 1] == null) {
                    System.err.println(Messages.getMessage("needPwd00"));
                    return null;
                }
                String str = "<m:passwd xmlns:m=\"http://xml.apache.org/axis/wsdd/\">";
                str += args[i + 1];
                str += "</m:passwd>";
                input = new ByteArrayInputStream(str.getBytes());
                i++;
                sb.append( process(opts, input) );
            }
            else {
                if(args[i].indexOf(java.io.File.pathSeparatorChar)==-1){
                    System.out.println( Messages.getMessage("processFile00", args[i]) );
                    sb.append( process(opts, args[i] ) );
                } else {
                    java.util.StringTokenizer tokenizer = null ;
                    tokenizer = new java.util.StringTokenizer(args[i],
                                                 java.io.File.pathSeparator);
                    while(tokenizer.hasMoreTokens()) {
                        String file = tokenizer.nextToken();
                        System.out.println( Messages.getMessage("processFile00", file) );
                        sb.append( process(opts, file) );
                        if(tokenizer.hasMoreTokens())
                            sb.append("\n");
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * go from the (parsed) command line to setting properties on our call object.
     * @param opts
     * @throws Exception if call==null
     */
    public void processOpts(Options opts) throws Exception {
        if (call == null) {
            throw new Exception(Messages.getMessage("nullCall00"));
        }

        URL address = new URL(opts.getURL());
        setTargetEndpointAddress(address);
        setLogin(opts.getUser(), opts.getPassword());

        String tName = opts.isValueSet( 't' );
        setTransport(tName);
    }

    /**
     * set the username and password
     * requires that call!=null
     * @param user username
     * @param password password
     */
    public void setLogin(String user, String password) {
        call.setUsername( user );
        call.setPassword( password );
    }

    /**
     * set the URL to deploy to
     * requires that call!=null
     * @param address
     */
    public void setTargetEndpointAddress(URL address) {
        call.setTargetEndpointAddress( address );
    }

    /**
     * set the transport to deploy with.
     * requires that call!=null
     * @param transportName a null or empty value does not trigger a setting
     */
    public void setTransport(String transportName) {
        if(transportName != null && !transportName.equals("")) {
            call.setProperty( Call.TRANSPORT_NAME, transportName );
        }
    }

    public String  process(InputStream input) throws Exception {
        return process(null, input );
    }

    public String process(URL xmlURL) throws Exception {
        return process(null, xmlURL.openStream() );
    }

    /**
     * process an XML file containing a pre-prepared admin message
     * @param xmlFile file to load
     * @return
     * @throws Exception
     */
    public String process(String xmlFile) throws Exception {
        FileInputStream in = new FileInputStream(xmlFile);
        String result =  process(null, in );
        return result ;
    }

    public String process(Options opts, String xmlFile)  throws Exception {
        processOpts( opts );
        return process( xmlFile );
    }

    /**
     * submit the input stream's contents to the endpoint, return the results as a string.
     * The input stream is always closed after the call, whether the request worked or not
     * @param opts options -can be null
     * @param input -input stream for request
     * @return
     * @throws Exception if the call was null
     * @throws AxisFault if the invocation returned an empty response
     */
    public String process(Options opts, InputStream input)  throws Exception {
        try {
            if (call == null) {
                //validate that the call is not null
                throw new Exception(Messages.getMessage("nullCall00"));
            }

            if ( opts != null ) {
                //process options if supplied
                processOpts( opts );
            }

            call.setUseSOAPAction( true);
            call.setSOAPActionURI( "urn:AdminService");

            Vector result = null ;
            Object[]  params = new Object[] { new SOAPBodyElement(input) };
            result = (Vector) call.invoke( params );

            if (result == null || result.isEmpty()) {
                throw new AxisFault(Messages.getMessage("nullResponse00"));
            }

            SOAPBodyElement body = (SOAPBodyElement) result.elementAt(0);
            return body.toString();
        } finally {
            input.close();
        }
    }

    /**
     * Creates in instance of <code>AdminClient</code> and
     * invokes <code>process(args)</code>.
     * <p>Diagnostic output goes to <code>log.info</code>.</p>
     * @param args Commands to process
     */
    public static void main (String[] args)
    {
        try {
            AdminClient admin = new AdminClient();

            String result = admin.process(args);
            if (result != null) {
                System.out.println( StringUtils.unescapeNumericChar(result) );
            } else {
                System.exit(1);
            }
    } catch (AxisFault ae) {
            System.err.println(Messages.getMessage("exception00") + " " + ae.dumpToString());
            System.exit(1);
        } catch (Exception e) {
            System.err.println(Messages.getMessage("exception00") + " " + e.getMessage());
            System.exit(1);
        }
    }
}


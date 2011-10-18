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

package org.apache.axis.transport.http ;

import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.server.AxisServer;
import org.apache.axis.utils.Messages;
import org.apache.axis.AxisFault;
import org.apache.axis.ConfigurationException;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.description.ServiceDesc;
import org.apache.commons.logging.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Iterator;

/**
 * Proof-of-concept "management" servlet for Axis.
 * 
 * Point a browser here to administer the Axis installation.
 * 
 * Right now just starts and stops the server.
 * 
 * @author Glen Daniels (gdaniels@apache.org)
 * @author Steve Loughran
 * xdoclet tags are not active yet; keep web.xml in sync
 * @web.servlet name="AdminServlet"  display-name="Axis Admin Servlet"  load-on-startup="100"
 * @web.servlet-mapping url-pattern="/servlet/AdminServlet"
 */
public class AdminServlet extends AxisServletBase {

    private static Log log =
            LogFactory.getLog(AxisServlet.class.getName());


    /**
     * handle a GET request. Commands are only valid when not in production mode
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.setContentType("text/html; charset=utf-8");
        StringBuffer buffer=new StringBuffer(512);
        buffer.append("<html><head><title>Axis</title></head><body>\n");
        //REVISIT: what happens if there is no engine?
        AxisServer server = getEngine();

        //process command
        String cmd = request.getParameter("cmd");
        if (cmd != null) {
            //who called?
            String callerIP=request.getRemoteAddr();
            if (isDevelopment()) {
                //only in dev mode do these command work
                if (cmd.equals("start")) {
                    log.info(Messages.getMessage("adminServiceStart", callerIP));
                    server.start();
                }
                else if (cmd.equals("stop")) {
                    log.info(Messages.getMessage("adminServiceStop", callerIP));
                    server.stop();
                } 
                else if (cmd.equals("suspend")) {
                    String name = request.getParameter("service"); 
                    log.info(Messages.getMessage("adminServiceSuspend", name, callerIP));
                    SOAPService service = server.getConfig().getService(new QName("",name));
                    service.stop();
                } 
                else if (cmd.equals("resume")) {
                    String name = request.getParameter("service"); 
                    log.info(Messages.getMessage("adminServiceResume", name, callerIP));
                    SOAPService service = server.getConfig().getService(new QName("",name));
                    service.start();
                } 
            } else {
                //in production we log a hostile probe. Remember: logs can be
                //used for DoS attacks themselves.
                log.info(Messages.getMessage("adminServiceDeny", callerIP));
            }
        }

        // display status
        if (server.isRunning()) {
            buffer.append("<H2>");
            buffer.append(Messages.getMessage("serverRun00"));
            buffer.append("</H2>");
        }
        else {
            buffer.append("<H2>");
            buffer.append(Messages.getMessage("serverStop00"));
            buffer.append("</H2>");
        }
        //add commands
        if(isDevelopment()) {
            buffer.append("<p><a href=\"AdminServlet?cmd=start\">start server</a>\n");
            buffer.append("<p><a href=\"AdminServlet?cmd=stop\">stop server</a>\n");

            Iterator i;
            try {
                i = server.getConfig().getDeployedServices();
            } catch (ConfigurationException configException) {
                //turn any internal configuration exceptions back into axis faults
                //if that is what they are
                if(configException.getContainedException() instanceof AxisFault) {
                    throw (AxisFault) configException.getContainedException();
                } else {
                    throw configException;
                }
            }
            
            buffer.append("<p><h2>Services</h2>");
            buffer.append("<ul>");
            while (i.hasNext()) {
                ServiceDesc sd = (ServiceDesc)i.next();
                StringBuffer sb = new StringBuffer();
                sb.append("<li>");
                String name = sd.getName();
                sb.append(name);
                SOAPService service = server.getConfig().getService(new QName("",name));
                if(service.isRunning()) {
                    sb.append("&nbsp;&nbsp;<a href=\"AdminServlet?cmd=suspend&service=" + name + "\">suspend</a>\n");
                } else {
                    sb.append("&nbsp;&nbsp;<a href=\"AdminServlet?cmd=resume&service=" + name + "\">resume</a>\n");
                }
                sb.append("</li>");
                buffer.append(sb.toString());
            }
            buffer.append("</ul>");
        }
        //print load
        buffer.append("<p>");
        buffer.append(Messages.getMessage("adminServiceLoad",
                Integer.toString(getLoadCounter())));
        buffer.append("\n</body></html>\n");
        response.getWriter().print( new String(buffer) );
    }
}

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

package org.apache.axis.server ;

import org.apache.axis.AxisEngine;
import org.apache.axis.AxisFault;
import org.apache.axis.AxisProperties;
import org.apache.axis.Constants;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.Handler;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.client.AxisClient;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.configuration.EngineConfigurationFactoryFinder;
import org.apache.axis.utils.ClassUtils;
import org.apache.axis.utils.Messages;
import org.apache.commons.logging.Log;

import java.util.Map;
/**
 *
 * @author Doug Davis (dug@us.ibm.com)
 * @author Glen Daniels (gdaniels@allaire.com)
 */
public class AxisServer extends AxisEngine
{
    protected static Log log =
        LogFactory.getLog(AxisServer.class.getName());
    private static Log tlog =
        LogFactory.getLog("org.apache.axis.TIME");

    private static AxisServerFactory factory = null;
    
    public static AxisServer getServer(Map environment) throws AxisFault
    {
        if (factory == null) {
            String factoryClassName = AxisProperties.getProperty("axis.ServerFactory");
            if (factoryClassName != null) {
                try {
                    Class factoryClass = ClassUtils.forName(factoryClassName);
                    if (AxisServerFactory.class.isAssignableFrom(factoryClass))
                        factory = (AxisServerFactory)factoryClass.newInstance();
                } catch (Exception e) {
                    // If something goes wrong here, should we just fall
                    // through and use the default one?
                    log.error(Messages.getMessage("exception00"), e);
                }
            }
            
            if (factory == null) {
                factory = new DefaultAxisServerFactory();
            }
        }
        
        return factory.getServer(environment);                
    }

    /**
     * the AxisClient to be used by outcalling Services
     */
    private AxisEngine clientEngine;

    public AxisServer()
    {
        this(EngineConfigurationFactoryFinder.newFactory().getServerEngineConfig());
    }

    public AxisServer(EngineConfiguration config)
    {
        super(config);
        // Server defaults to persisting configuration
        setShouldSaveConfig(true);
    }

    /** Is this server active?  If this is false, any requests will
     * cause a SOAP Server fault to be generated.
     */
    private boolean running = true;

    public boolean isRunning() { return running; }

    /** Start the server.
     */
    public void start()
    {
        // re-init...
        init();
        running = true;
    }

    /** Stop the server.
     */
    public void stop()
    {
        running = false;
    }

    /**
     * Get this server's client engine.  Create it if it does
     * not yet exist.
     */
    public synchronized AxisEngine getClientEngine () {
        if (clientEngine == null) {
            clientEngine = new AxisClient(); // !!!!
        }
        return clientEngine;
    }

    /**
     * Main routine of the AXIS server.  In short we locate the appropriate
     * handler for the desired service and invoke() it.
     */
    public void invoke(MessageContext msgContext) throws AxisFault {
        long t0=0, t1=0, t2=0, t3=0, t4=0, t5=0;
        if( tlog.isDebugEnabled() ) {
            t0=System.currentTimeMillis();
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Enter: AxisServer::invoke");
        }

        if (!isRunning()) {
            throw new AxisFault("Server.disabled",
                                Messages.getMessage("serverDisabled00"),
                                null, null);
        }

        String  hName = null ;
        Handler h     = null ;

        // save previous context
        MessageContext previousContext = getCurrentMessageContext();

        try {
            // set active context
            setCurrentMessageContext(msgContext);

            hName = msgContext.getStrProp( MessageContext.ENGINE_HANDLER );
            if ( hName != null ) {
                if ( (h = getHandler(hName)) == null ) {
                    ClassLoader cl = msgContext.getClassLoader();
                    try {
                        log.debug( Messages.getMessage("tryingLoad00", hName) );
                        Class cls = ClassUtils.forName(hName, true, cl);
                        h = (Handler) cls.newInstance();
                    }
                    catch( Exception e ) {
                        h = null ;
                    }
                }
                if( tlog.isDebugEnabled() ) {
                    t1=System.currentTimeMillis();
                }
                if ( h != null )
                    h.invoke(msgContext);
                else
                    throw new AxisFault( "Server.error",
                                         Messages.getMessage("noHandler00", hName),
                                         null, null );
                if( tlog.isDebugEnabled() ) {
                    t2=System.currentTimeMillis();
                    tlog.debug( "AxisServer.invoke " + hName + " invoke=" +
                                ( t2-t1 ) + " pre=" + (t1-t0 ));
                }
                
            }
            else {
                // This really should be in a handler - but we need to discuss it
                // first - to make sure that's what we want.
                /* Now we do the 'real' work.  The flow is basically:         */
                /*   Transport Specific Request Handler/Chain                   */
                /*   Global Request Handler/Chain                               */
                /*   Protocol Specific-Handler(ie. SOAP, XP)                  */
                /*     ie. For SOAP Handler:                                  */
                /*           - Service Specific Request Handler/Chain           */
                /*           - SOAP Semantic Checks                           */
                /*           - Service Specific Response Handler/Chain          */
                /*   Global Response Handler/Chain                              */
                /*   Transport Specific Response Handler/Chain                  */
                /**************************************************************/

                // When do we call init/cleanup??
                if (log.isDebugEnabled()) {
                    log.debug(Messages.getMessage("defaultLogic00") );
                }

                /*  This is what the entirety of this logic might evolve to:

                hName = msgContext.getStrProp(MessageContext.TRANSPORT);
                if ( hName != null ) {
                if ((h = hr.find( hName )) != null ) {
                h.invoke(msgContext);
                } else {
                log.error(Messages.getMessage("noTransport02", hName));
                }
                } else {
                // No transport set, so use the default (probably just
                // calls the global->service handlers)
                defaultTransport.invoke(msgContext);
                }

                */

                /* Process the Transport Specific Request Chain */
                /**********************************************/
                hName = msgContext.getTransportName();
                SimpleTargetedChain transportChain = null;

                if (log.isDebugEnabled())
                    log.debug(Messages.getMessage("transport01", "AxisServer.invoke", hName));

                if( tlog.isDebugEnabled() ) {
                    t1=System.currentTimeMillis();
                }
                if ( hName != null && (h = getTransport( hName )) != null ) {
                    if (h instanceof SimpleTargetedChain) {
                        transportChain = (SimpleTargetedChain)h;
                        h = transportChain.getRequestHandler();
                        if (h != null)
                            h.invoke(msgContext);
                    }
                }

                if( tlog.isDebugEnabled() ) {
                    t2=System.currentTimeMillis();
                }
                /* Process the Global Request Chain */
                /**********************************/
                if ((h = getGlobalRequest()) != null ) {
                    h.invoke(msgContext);
                }

                /**
                 * At this point, the service should have been set by someone
                 * (either the originator of the MessageContext, or one of the
                 * transport or global Handlers).  If it hasn't been set, we
                 * fault.
                 */
                h = msgContext.getService();
                if (h == null) {
                    // It's possible that we haven't yet parsed the
                    // message at this point.  This is a kludge to
                    // make sure we have.  There probably wants to be
                    // some kind of declarative "parse point" on the handler
                    // chain instead....
                    Message rm = msgContext.getRequestMessage();
                    rm.getSOAPEnvelope().getFirstBody();
                    
                    h = msgContext.getService();
                    if (h == null)
                        throw new AxisFault("Server.NoService",
                                            Messages.getMessage("noService05",
                                                                 "" + msgContext.getTargetService()),
                                            null, null );
                }
                if( tlog.isDebugEnabled() ) {
                    t3=System.currentTimeMillis();
                }
                
                initSOAPConstants(msgContext);
                try {
                    h.invoke(msgContext);
                } catch (AxisFault ae) {
                    if ((h = getGlobalRequest()) != null ) {
                        h.onFault(msgContext);
                    }
                    throw ae;
                }

                if( tlog.isDebugEnabled() ) {
                    t4=System.currentTimeMillis();
                }

                /* Process the Global Response Chain */
                /***********************************/
                if ((h = getGlobalResponse()) != null)
                    h.invoke(msgContext);

                /* Process the Transport Specific Response Chain */
                /***********************************************/
                if (transportChain != null) {
                    h = transportChain.getResponseHandler();
                    if (h != null)
                        h.invoke(msgContext);
                }
                
                if( tlog.isDebugEnabled() ) {
                    t5=System.currentTimeMillis();
                    tlog.debug( "AxisServer.invoke2 " +
                                " preTr=" +
                                ( t1-t0 ) + " tr=" + (t2-t1 ) +
                                " preInvoke=" + ( t3-t2 ) +
                                " invoke=" + ( t4-t3 ) +
                                " postInvoke=" + ( t5-t4 ) +
                                " " + msgContext.getTargetService() + "." +
                                 ((msgContext.getOperation( ) == null) ?
                                 "" : msgContext.getOperation().getName()) );
                }

            }
        } catch (AxisFault e) {
            throw e;
        } catch (Exception e) {
            // Should we even bother catching it ?
            throw AxisFault.makeFault(e);

        } finally {
            // restore previous state
            setCurrentMessageContext(previousContext);
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Exit: AxisServer::invoke");
        }
    }

    /**
     * Extract ans store soap constants info from the envelope
     * @param msgContext
     * @throws AxisFault
     */ 
    private void initSOAPConstants(MessageContext msgContext) throws AxisFault {
        Message msg = msgContext.getRequestMessage();
        if (msg == null)
            return;
        SOAPEnvelope env = msg.getSOAPEnvelope();
        if (env == null)
            return;
        SOAPConstants constants = env.getSOAPConstants();
        if (constants == null)
            return;
        // Ensure that if we get SOAP1.2, then reply using SOAP1.2
        msgContext.setSOAPConstants(constants);
    }

    /**
     *
     */
    public void generateWSDL(MessageContext msgContext) throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("Enter: AxisServer::generateWSDL");
        }

        if (!isRunning()) {
            throw new AxisFault("Server.disabled",
                                Messages.getMessage("serverDisabled00"),
                                null, null);
        }

        String  hName = null ;
        Handler h     = null ;

        // save previous context
        MessageContext previousContext = getCurrentMessageContext();

        try {
            // set active context
            setCurrentMessageContext(msgContext);

            hName = msgContext.getStrProp( MessageContext.ENGINE_HANDLER );
            if ( hName != null ) {
                if ( (h = getHandler(hName)) == null ) {
                    ClassLoader cl = msgContext.getClassLoader();
                    try {
                        log.debug( Messages.getMessage("tryingLoad00", hName) );
                        Class cls = ClassUtils.forName(hName, true, cl);
                        h = (Handler) cls.newInstance();
                    }
                    catch( Exception e ) {
                        throw new AxisFault(
                                "Server.error",
                                Messages.getMessage("noHandler00", hName),
                                null, null );
                    }
                }
                h.generateWSDL(msgContext);
            }
            else {
                // This really should be in a handler - but we need to discuss it
                // first - to make sure that's what we want.
                /* Now we do the 'real' work.  The flow is basically:         */
                /*   Transport Specific Request Handler/Chain                   */
                /*   Global Request Handler/Chain                               */
                /*   Protocol Specific-Handler(ie. SOAP, XP)                  */
                /*     ie. For SOAP Handler:                                  */
                /*           - Service Specific Request Handler/Chain           */
                /*           - SOAP Semantic Checks                           */
                /*           - Service Specific Response Handler/Chain          */
                /*   Global Response Handler/Chain                              */
                /*   Transport Specific Response Handler/Chain                  */
                /**************************************************************/

                // When do we call init/cleanup??
                log.debug( Messages.getMessage("defaultLogic00") );

                /*  This is what the entirety of this logic might evolve to:

                hName = msgContext.getStrProp(MessageContext.TRANSPORT);
                if ( hName != null ) {
                if ((h = hr.find( hName )) != null ) {
                h.generateWSDL(msgContext);
                } else {
                log.error(Messages.getMessage("noTransport02", hName));
                }
                } else {
                // No transport set, so use the default (probably just
                // calls the global->service handlers)
                defaultTransport.generateWSDL(msgContext);
                }

                */

                /* Process the Transport Specific Request Chain */
                /**********************************************/
                hName = msgContext.getTransportName();
                SimpleTargetedChain transportChain = null;

                if (log.isDebugEnabled())
                    log.debug(Messages.getMessage("transport01",
                                                   "AxisServer.generateWSDL",
                                                   hName));
                if ( hName != null && (h = getTransport( hName )) != null ) {
                    if (h instanceof SimpleTargetedChain) {
                        transportChain = (SimpleTargetedChain)h;
                        h = transportChain.getRequestHandler();
                        if (h != null) {
                            h.generateWSDL(msgContext);
                        }
                    }
                }

                /* Process the Global Request Chain */
                /**********************************/
                if ((h = getGlobalRequest()) != null )
                    h.generateWSDL(msgContext);

                /**
                 * At this point, the service should have been set by someone
                 * (either the originator of the MessageContext, or one of the
                 * transport or global Handlers).  If it hasn't been set, we
                 * fault.
                 */
                h = msgContext.getService();
                if (h == null) {
                    // It's possible that we haven't yet parsed the
                    // message at this point.  This is a kludge to
                    // make sure we have.  There probably wants to be
                    // some kind of declarative "parse point" on the handler
                    // chain instead....
                    Message rm = msgContext.getRequestMessage();
                    if (rm != null) {
                        rm.getSOAPEnvelope().getFirstBody();
                        h = msgContext.getService();
                    }
                    if (h == null) {
                        throw new AxisFault(Constants.QNAME_NO_SERVICE_FAULT_CODE,
                                            Messages.getMessage("noService05",
                                                                 "" + msgContext.getTargetService()),
                                            null, null );
                    }
                }

                h.generateWSDL(msgContext);

                /* Process the Global Response Chain */
                /***********************************/
                if ((h = getGlobalResponse()) != null )
                    h.generateWSDL(msgContext);

                /* Process the Transport Specific Response Chain */
                /***********************************************/
                if (transportChain != null) {
                    h = transportChain.getResponseHandler();
                    if (h != null) {
                        h.generateWSDL(msgContext);
                    }
                }
            }
        } catch (AxisFault e) {
            throw e;
        } catch(Exception e) {
            // Should we even bother catching it ?
            throw AxisFault.makeFault(e);
        } finally {
            // restore previous state
            setCurrentMessageContext(previousContext);
        }

        if (log.isDebugEnabled()) {
            log.debug("Exit: AxisServer::generateWSDL");
        }
    }
}

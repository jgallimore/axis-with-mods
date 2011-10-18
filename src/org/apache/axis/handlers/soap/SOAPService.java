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

package org.apache.axis.handlers.soap;

import org.apache.axis.AxisEngine;
import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.Handler;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.attachments.Attachments;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.description.JavaServiceDesc;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.encoding.TypeMappingRegistry;
import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.handlers.HandlerChainImpl;
import org.apache.axis.handlers.HandlerInfoChainFactory;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPFault;
import org.apache.axis.providers.BasicProvider;
import org.apache.axis.session.Session;
import org.apache.axis.utils.LockableHashtable;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.utils.ClassUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;

import javax.xml.rpc.soap.SOAPFaultException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

/** A <code>SOAPService</code> is a Handler which encapsulates a SOAP
 * invocation.  It has an request chain, an response chain, and a pivot-point,
 * and handles the SOAP semantics when invoke()d.
 *
 * @author Glen Daniels (gdaniels@apache.org)
 * @author Doug Davis (dug@us.ibm.com)
 */
public class SOAPService extends SimpleTargetedChain
{
    private static Log log =
        LogFactory.getLog(SOAPService.class.getName());

    /** Valid transports for this service
     * (server side only!)
     *
     * !!! For now, if this is null, we assume all
     * transports are valid.
     */
    private Vector validTransports = null;

    /**
     * Does this service require a high-fidelity SAX recording of messages?
     * (default is true)
     */
    private boolean highFidelityRecording = true;

    /**
     * How does this service wish data which would normally be sent as
     * an attachment to be sent?  Default for requests is
     * org.apache.axis.attachments.Attachments.SEND_TYPE_DEFAULT,
     * and the default for responses is to match the request.
     */
    private int sendType = Attachments.SEND_TYPE_NOTSET;

    /**
     * Our ServiceDescription.  Holds pretty much all the interesting
     * metadata about this service.
     */
    private ServiceDesc serviceDescription = new JavaServiceDesc();
    private AxisEngine engine;

    /**
     * A list of our active service objects (these can have lifetimes and
     * be reaped)
     */
    public Map serviceObjects = new HashMap();
    public int nextObjectID = 1;

    /**
     * List of sessions (for all services), key=serviceName, value=Service
     */
    static private Hashtable sessions = new Hashtable();

    private boolean isRunning = true;
    
    /** 
     * Add this passed in Session to this Service's list of sessions
     */
    public void addSession(Session session) {
      WeakHashMap map = (WeakHashMap) sessions.get( this.getName() );
      if ( map == null )  {
        map = new WeakHashMap();
        sessions.put( this.getName(), map);
      }
      if ( !map.containsKey(session) ) map.put(session, null);
    }

    /** 
     * Remove all of this Service's serviceObjects from it known sessions
     */
    public void clearSessions() {
      WeakHashMap map = (WeakHashMap) sessions.get( this.getName() );
      if ( map == null ) return ;
      Iterator iter = map.keySet().iterator();
      while ( iter.hasNext() ) {
        Session session = (Session) iter.next();
        session.remove( this.getName() );
      }
    }

    /**
     * Actor list - these are just the service-specific ones
     */
    ArrayList actors = new ArrayList();

    /**
     * Get the service-specific actor list
     * @return
     */
    public ArrayList getServiceActors() {
        return actors;
    }

    /**
     * Get the merged actor list for this service, including engine-wide
     * actor URIs.
     *
     * @return
     */
    public ArrayList getActors() {
        ArrayList acts = (ArrayList)actors.clone();  // ??? cache this?

        if (engine != null) {
            acts.addAll(engine.getActorURIs());
        }

        return acts;
    }
    
    public List getRoles() {
        return getActors();
    }

    /**
     * Set the service-specific role list
     *
     * @param roles a List of Strings, each containing a role URI
     */
    public void setRoles(List roles) {
        actors = new ArrayList(roles);
    }

    /** Standard, no-arg constructor.
     */
    public SOAPService()
    {
        setOptionsLockable(true);
        initHashtable();

        // For now, always assume we're the ultimate destination.
        actors.add("");
    }

    /** Constructor with real or null request, pivot, and response
     *  handlers. A special request handler is specified to inject
     *  SOAP semantics.
     */
    public SOAPService(Handler reqHandler, Handler pivHandler,
                       Handler respHandler) {
        this();
        init(reqHandler, new MustUnderstandChecker(this), pivHandler, null, respHandler);
    }

    public TypeMappingRegistry getTypeMappingRegistry()
    {
        return serviceDescription.getTypeMappingRegistry();
    }

    /** Convenience constructor for wrapping SOAP semantics around
     * "service handlers" which actually do work.
     */
    public SOAPService(Handler serviceHandler)
    {
        this();
        init(null, new MustUnderstandChecker(this), serviceHandler, null, null);
    }
    
    /** Tell this service which engine it's deployed to.
     *
     */
    public void setEngine(AxisEngine engine)
    {
        if (engine == null)
            throw new IllegalArgumentException(
                    Messages.getMessage("nullEngine"));

        this.engine = engine;
        ((LockableHashtable)options).setParent(engine.getOptions());
        TypeMappingRegistry tmr = engine.getTypeMappingRegistry();
        getTypeMappingRegistry().delegate(tmr);
    }

    public AxisEngine getEngine() {
        return engine;
    }

    public boolean availableFromTransport(String transportName)
    {
        if (validTransports != null) {
            for (int i = 0; i < validTransports.size(); i++) {
                if (validTransports.elementAt(i).equals(transportName))
                    return true;
            }
            return false;
        }

        return true;
    }

    public Style getStyle() {
        return serviceDescription.getStyle();
    }

    public void setStyle(Style style) {
        serviceDescription.setStyle(style);
    }

    public Use getUse() {
        return serviceDescription.getUse();
    }

    public void setUse(Use style) {
        serviceDescription.setUse(style);
    }

    public ServiceDesc getServiceDescription() {
        return serviceDescription;
    }

    /**
     * Returns a service description with the implementation class filled in.
     * Syncronized to prevent simutaneous modification of serviceDescription.
     */
    public synchronized ServiceDesc getInitializedServiceDesc(
                                                     MessageContext msgContext)
            throws AxisFault {

        if (!serviceDescription.isInitialized()) {

            // Let the provider do the work of filling in the service
            // descriptor.  This is so that it can decide itself how best
            // to map the Operations.  In the future, we may want to support
            // providers which don't strictly map to Java class backends
            // (BSFProvider, etc.), and as such we hand off here.
            if (pivotHandler instanceof BasicProvider) {
                ((BasicProvider)pivotHandler).initServiceDesc(this, msgContext);
            }

        }

        return serviceDescription;
    }

    public void setServiceDescription(ServiceDesc serviceDescription) {
        if (serviceDescription == null) {
            // FIXME: Throw NPE?
            return;
        }
        this.serviceDescription = serviceDescription;
        //serviceDescription.setTypeMapping((TypeMapping)this.getTypeMappingRegistry().getDefaultTypeMapping());
    }

    public void setPropertyParent(Hashtable parent)
    {
        if (options == null) {
            options = new LockableHashtable();
        }
        ((LockableHashtable)options).setParent(parent);
    }

    /**
     * Generate WSDL.  If we have a specific file configured in the
     * ServiceDesc, just return that.  Otherwise run through all the Handlers
     * (including the provider) and call generateWSDL() on them via our
     * parent's implementation.
     */
    public void generateWSDL(MessageContext msgContext) throws AxisFault {
        if (serviceDescription == null ||
                serviceDescription.getWSDLFile() == null) {
            super.generateWSDL(msgContext);
            return;
        }
        InputStream instream = null;

        // Got a WSDL file in the service description, so try and read it
        try {
            String filename= serviceDescription.getWSDLFile();
            File file=new File(filename);
            if(file.exists()) {
                //if this resolves to a file, load it
                instream = new FileInputStream(filename);
            } else if(msgContext.getStrProp(Constants.MC_HOME_DIR)!=null){
                String path = msgContext.getStrProp(Constants.MC_HOME_DIR) +'/' + filename;
                file = new File(path);
                if(file.exists()) {
                    //if this resolves to a file, load it
                    instream = new FileInputStream(path);
                }
            }
            
            if(instream == null) {
                //else load a named resource in our classloader. 
                instream = ClassUtils.getResourceAsStream(this.getClass(),filename);
                if (instream == null) {
                    String errorText=Messages.getMessage("wsdlFileMissing",filename);
                    throw new AxisFault(errorText);
                }
            }
            Document doc = XMLUtils.newDocument(instream);
            msgContext.setProperty("WSDL", doc);
        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        } finally {
            if(instream!=null) {
                try {
                    instream.close();
                } catch (IOException e) { }
            }
        }
    }
    /*********************************************************************
     * Administration and management APIs
     *
     * These can get called by various admin adapters, such as JMX MBeans,
     * our own Admin client, web applications, etc...
     *
     *********************************************************************
     */

    /** Placeholder for "resume this service" method
     */
    public void start()
    {
        isRunning = true;
    }

    /** Placeholder for "suspend this service" method
     */
    public void stop()
    {
        isRunning = false;
    }

    /**
     * Is this service suspended?
     * @return
     */ 
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Make this service available on a particular transport
     */
    public void enableTransport(String transportName)
    {
        if (log.isDebugEnabled()) {
            log.debug(Messages.getMessage(
                "enableTransport00", "" + this, transportName));
        }

        if (validTransports == null)
            validTransports = new Vector();
        validTransports.addElement(transportName);
    }

    /**
     * Disable access to this service from a particular transport
     */
    public void disableTransport(String transportName)
    {
        if (validTransports != null) {
            validTransports.removeElement(transportName);
        }
    }

    public boolean needsHighFidelityRecording() {
        return highFidelityRecording;
    }

    public void setHighFidelityRecording(boolean highFidelityRecording) {
        this.highFidelityRecording = highFidelityRecording;
    }

    // see org.apache.axis.attachments.Attachments
    public int getSendType() {
        return sendType;
    }

    public void setSendType(int sendType) {
        this.sendType = sendType;
    }

    public void invoke(MessageContext msgContext) throws AxisFault {
        HandlerInfoChainFactory handlerFactory = (HandlerInfoChainFactory) this.getOption(Constants.ATTR_HANDLERINFOCHAIN);
        HandlerChainImpl handlerImpl = null;
        if (handlerFactory != null) handlerImpl = (HandlerChainImpl) handlerFactory.createHandlerChain();
        boolean result = true;

        try {
            if (handlerImpl != null) {
                try {
                    result = handlerImpl.handleRequest(msgContext);
                }
                catch (SOAPFaultException e) {
                    msgContext.setPastPivot(true);
                    handlerImpl.handleFault(msgContext);
                    return;
                }
            }

            if (result) {
                try {
                    super.invoke(msgContext);
                } catch (AxisFault e) {
                    msgContext.setPastPivot(true);
                    if (handlerImpl != null) {
                        handlerImpl.handleFault(msgContext);
                    }
                    throw e;
                }
            } else {
                msgContext.setPastPivot(true);
            }

            if ( handlerImpl != null) {
                handlerImpl.handleResponse(msgContext);
            }
        } catch (SOAPFaultException e) {
            msgContext.setPastPivot(true);
            throw AxisFault.makeFault(e);

        } catch (RuntimeException e) {
            SOAPFault fault = new SOAPFault(new AxisFault("Server", "Server Error", null, null));
            SOAPEnvelope env = new SOAPEnvelope();
            env.addBodyElement(fault);
            Message message = new Message(env);
            message.setMessageType(Message.RESPONSE);
            msgContext.setResponseMessage(message);
            throw AxisFault.makeFault(e);
        }
        finally {
            if (handlerImpl != null) {
                handlerImpl.destroy();
            }
        }
    }
}

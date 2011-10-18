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
package org.apache.axis.deployment.wsdd;

import org.apache.axis.AxisEngine;
import org.apache.axis.ConfigurationException;
import org.apache.axis.Constants;
import org.apache.axis.Handler;
import org.apache.axis.WSDDEngineConfiguration;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.encoding.DeserializerFactory;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.SerializerFactory;
import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.encoding.TypeMappingRegistry;
import org.apache.axis.encoding.TypeMappingRegistryImpl;
import org.apache.axis.encoding.ser.ArraySerializerFactory;
import org.apache.axis.encoding.ser.BaseDeserializerFactory;
import org.apache.axis.encoding.ser.BaseSerializerFactory;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.utils.Messages;
import org.apache.commons.logging.Log;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * WSDD deployment element
 *
 * @author James Snell
 * @author Glen Daniels (gdaniels@apache.org)
 */
public class WSDDDeployment
        extends WSDDElement
        implements WSDDTypeMappingContainer,
                WSDDEngineConfiguration
{
    protected static Log log =
            LogFactory.getLog(WSDDDeployment.class.getName());
    private HashMap handlers = new HashMap();
    private HashMap services = new HashMap();
    private HashMap transports = new HashMap();
    private HashMap typeMappings = new HashMap();
    private WSDDGlobalConfiguration globalConfig = null;
    /**
     * Mapping of namespaces -> services
     */
            private HashMap namespaceToServices = new HashMap();
    private AxisEngine engine;

    protected void addHandler(WSDDHandler handler) {
        handlers.put(handler.getQName(), handler);
    }

    protected void addService(WSDDService service) {
        WSDDService oldService = (WSDDService) services.get(service.getQName());
        if (oldService != null) {
            oldService.removeNamespaceMappings(this);
        }
        services.put(service.getQName(), service);
    }

    protected void addTransport(WSDDTransport transport) {
        transports.put(transport.getQName(), transport);
    }

    /**
     * Put a WSDDHandler into this deployment, replacing any other
     * WSDDHandler which might already be present with the same QName.
     *
     * @param handler a WSDDHandler to insert in this deployment
     */
    public void deployHandler(WSDDHandler handler) {
        handler.deployToRegistry(this);
    }

    /**
     * Put a WSDDTransport into this deployment, replacing any other
     * WSDDTransport which might already be present with the same QName.
     *
     * @param transport a WSDDTransport to insert in this deployment
     */
    public void deployTransport(WSDDTransport transport) {
        transport.deployToRegistry(this);
    }

    /**
     * Put a WSDDService into this deployment, replacing any other
     * WSDDService which might already be present with the same QName.
     *
     * @param service a WSDDHandler to insert in this deployment
     */
    public void deployService(WSDDService service) {
        service.deployToRegistry(this);
    }

    /**
     * Remove a named handler
     *
     * @param qname the QName of the handler to remove
     */
    public void undeployHandler(QName qname) {
        handlers.remove(qname);
    }

    /**
     * Remove a named service
     *
     * @param qname the QName of the service to remove
     */
    public void undeployService(QName qname) {
        WSDDService service = (WSDDService) services.get(qname);
        if (service != null) {
            service.removeNamespaceMappings(this);
            services.remove(qname);
        }
    }

    /**
     * Remove a named transport
     *
     * @param qname the QName of the transport to remove
     */
    public void undeployTransport(QName qname) {
        transports.remove(qname);
    }

    public void deployTypeMapping(WSDDTypeMapping typeMapping)
            throws WSDDException {
        QName qname = typeMapping.getQName();
        String encoding = typeMapping.getEncodingStyle();
        // We have to include the encoding in the key
        // because otherwise we would overwrite exiting mappings
        typeMappings.put(qname + encoding, typeMapping);
        if (tmrDeployed)
            deployMapping(typeMapping);
    }

    /**
     * Default constructor
     */
    public WSDDDeployment() {
    }

    /**
     * Create an element in WSDD that wraps an extant DOM element
     *
     * @param e the element to create the deployment from
     * @throws WSDDException when problems occur deploying a service or type mapping.
     */
    public WSDDDeployment(Element e)
            throws WSDDException {
        super(e);
        Element [] elements = getChildElements(e, ELEM_WSDD_HANDLER);
        int i;
        for (i = 0; i < elements.length; i++) {
            WSDDHandler handler = new WSDDHandler(elements[i]);
            deployHandler(handler);
        }
        elements = getChildElements(e, ELEM_WSDD_CHAIN);
        for (i = 0; i < elements.length; i++) {
            WSDDChain chain = new WSDDChain(elements[i]);
            deployHandler(chain);
        }
        elements = getChildElements(e, ELEM_WSDD_TRANSPORT);
        for (i = 0; i < elements.length; i++) {
            WSDDTransport transport = new WSDDTransport(elements[i]);
            deployTransport(transport);
        }
        elements = getChildElements(e, ELEM_WSDD_SERVICE);
        for (i = 0; i < elements.length; i++) {
            try {
                WSDDService service = new WSDDService(elements[i]);
                deployService(service);
            } catch (WSDDNonFatalException ex) {
                // If it's non-fatal, just keep on going
                log.info(Messages.getMessage("ignoringNonFatalException00"), ex);
            } catch (WSDDException ex) {
                // otherwise throw it upwards
                throw ex;
            }
        }
        elements = getChildElements(e, ELEM_WSDD_TYPEMAPPING);
        for (i = 0; i < elements.length; i++) {
            try {
                WSDDTypeMapping mapping = new WSDDTypeMapping(elements[i]);
                deployTypeMapping(mapping);
            } catch (WSDDNonFatalException ex) {
                // If it's non-fatal, just keep on going
                log.info(Messages.getMessage("ignoringNonFatalException00"), ex);
            } catch (WSDDException ex) {
                // otherwise throw it upwards
                throw ex;
            }
        }
        elements = getChildElements(e, ELEM_WSDD_BEANMAPPING);
        for (i = 0; i < elements.length; i++) {
            WSDDBeanMapping mapping = new WSDDBeanMapping(elements[i]);
            deployTypeMapping(mapping);
        }

        elements = getChildElements(e, ELEM_WSDD_ARRAYMAPPING);
        for (i = 0; i < elements.length; i++) {
            WSDDArrayMapping mapping =
                    new WSDDArrayMapping(elements[i]);
            deployTypeMapping(mapping);
        }

        Element el = getChildElement(e, ELEM_WSDD_GLOBAL);
        if (el != null)
            globalConfig = new WSDDGlobalConfiguration(el);
    }

    protected QName getElementName() {
        return QNAME_DEPLOY;
    }

    public void deployToRegistry(WSDDDeployment target)
            throws ConfigurationException {
        WSDDGlobalConfiguration global = getGlobalConfiguration();
        if (global != null) {
            target.setGlobalConfiguration(global);
        }
        Iterator i = handlers.values().iterator();
        while (i.hasNext()) {
            WSDDHandler handler = (WSDDHandler) i.next();
            target.deployHandler(handler);
        }
        i = transports.values().iterator();
        while (i.hasNext()) {
            WSDDTransport transport = (WSDDTransport) i.next();
            target.deployTransport(transport);
        }
        i = services.values().iterator();
        while (i.hasNext()) {
            WSDDService service = (WSDDService) i.next();
            service.deployToRegistry(target);
        }
        i = typeMappings.values().iterator();
        while (i.hasNext()) {
            WSDDTypeMapping mapping = (WSDDTypeMapping) i.next();
            target.deployTypeMapping(mapping);
        }
    }

    private void deployMapping(WSDDTypeMapping mapping)
            throws WSDDException {
        try {
            String encodingStyle = mapping.getEncodingStyle();
            if (encodingStyle == null) {
                encodingStyle = Constants.URI_DEFAULT_SOAP_ENC;
            }
            TypeMapping tm = tmr.getOrMakeTypeMapping(encodingStyle);
            SerializerFactory   ser = null;
            DeserializerFactory deser = null;
            // Try to construct a serializerFactory by introspecting for the
            // following:
            // public static create(Class javaType, QName xmlType)
            // public <constructor>(Class javaType, QName xmlType)
            // public <constructor>()
            //
            // The BaseSerializerFactory createFactory() method is a utility
            // that does this for us.
            //log.debug("start creating sf and df");
            if (mapping.getSerializerName() != null &&
                    !mapping.getSerializerName().equals("")) {
                ser = BaseSerializerFactory.createFactory(mapping.getSerializer(),
                        mapping.getLanguageSpecificType(),
                        mapping.getQName());
            }

            if ((mapping instanceof WSDDArrayMapping) && (ser instanceof ArraySerializerFactory)) {
                WSDDArrayMapping am = (WSDDArrayMapping) mapping;
                ArraySerializerFactory factory = (ArraySerializerFactory) ser;
                factory.setComponentType(am.getInnerType());
            }

            //log.debug("set ser factory");

            if (mapping.getDeserializerName() != null &&
                    !mapping.getDeserializerName().equals("")) {
                deser = BaseDeserializerFactory.createFactory(mapping.getDeserializer(),
                        mapping.getLanguageSpecificType(),
                        mapping.getQName());
            }
            //log.debug("set dser factory");
            tm.register(mapping.getLanguageSpecificType(), mapping.getQName(), ser, deser);
            //log.debug("registered");
        } catch (ClassNotFoundException e) {
            log.error(Messages.getMessage("unabletoDeployTypemapping00", mapping.getQName().toString()), e);
            throw new WSDDNonFatalException(e);
        } catch (Exception e) {
            throw new WSDDException(e);
        }
    }

    public void writeToContext(SerializationContext context)
            throws IOException {
        context.registerPrefixForURI(NS_PREFIX_WSDD, URI_WSDD);
        context.registerPrefixForURI(NS_PREFIX_WSDD_JAVA, URI_WSDD_JAVA);
        context.startElement(QNAME_DEPLOY, null);
        if (globalConfig != null) {
            globalConfig.writeToContext(context);
        }
        Iterator i = handlers.values().iterator();
        while (i.hasNext()) {
            WSDDHandler handler = (WSDDHandler) i.next();
            handler.writeToContext(context);
        }
        i = services.values().iterator();
        while (i.hasNext()) {
            WSDDService service = (WSDDService) i.next();
            service.writeToContext(context);
        }
        i = transports.values().iterator();
        while (i.hasNext()) {
            WSDDTransport transport = (WSDDTransport) i.next();
            transport.writeToContext(context);
        }
        i = typeMappings.values().iterator();
        while (i.hasNext()) {
            WSDDTypeMapping mapping = (WSDDTypeMapping) i.next();
            mapping.writeToContext(context);
        }
        context.endElement();
    }

    /**
     * Get our global configuration
     *
     * @return a global configuration object
     */
    public WSDDGlobalConfiguration getGlobalConfiguration() {
        return globalConfig;
    }

    public void setGlobalConfiguration(WSDDGlobalConfiguration globalConfig) {
        this.globalConfig = globalConfig;
    }

    /**
     * @return an array of type mappings in this deployment
     */
    public WSDDTypeMapping[] getTypeMappings() {
        WSDDTypeMapping[] t = new WSDDTypeMapping[typeMappings.size()];
        typeMappings.values().toArray(t);
        return t;
    }

    /**
     * Return an array of the services in this deployment
     */
    public WSDDService[] getServices() {
        WSDDService [] serviceArray = new WSDDService[services.size()];
        services.values().toArray(serviceArray);
        return serviceArray;
    }

    /**
     * Return the WSDD description for a given named service
     */
    public WSDDService getWSDDService(QName qname) {
        return (WSDDService) services.get(qname);
    }

    /**
     * Return an instance of the named handler.
     *
     * @param name the name of the handler to get
     * @return an Axis handler with the specified QName or null of not found
     */
    public Handler getHandler(QName name) throws ConfigurationException {
        WSDDHandler h = (WSDDHandler) handlers.get(name);
        if (h != null) {
            return h.getInstance(this);
        }
        return null;
    }

    /**
     * Retrieve an instance of the named transport.
     *
     * @param name the <code>QName</code> of the transport
     * @return a <code>Handler</code> implementing the transport
     * @throws ConfigurationException if there was an error resolving the
     *                                transport
     */
    public Handler getTransport(QName name) throws ConfigurationException {
        WSDDTransport t = (WSDDTransport) transports.get(name);
        if (t != null) {
            return t.getInstance(this);
        }
        return null;
    }

    /**
     * Retrieve an instance of the named service.
     *
     * @param name the <code>QName</code> identifying the
     *             <code>Service</code>
     * @return the <code>Service</code> associated with <code>qname</code>
     * @throws ConfigurationException if there was an error resolving the
     *                                qname
     */
    public SOAPService getService(QName name) throws ConfigurationException {
        WSDDService s = (WSDDService) services.get(name);
        if (s != null) {
            return (SOAPService) s.getInstance(this);
        }
        return null;
    }

    public SOAPService getServiceByNamespaceURI(String namespace)
            throws ConfigurationException {
        WSDDService s = (WSDDService) namespaceToServices.get(namespace);
        if (s != null) {
            return (SOAPService) s.getInstance(this);
        }
        return null;
    }

    public void configureEngine(AxisEngine engine)
            throws ConfigurationException {
        this.engine = engine;
    }

    public void writeEngineConfig(AxisEngine engine) throws ConfigurationException {
    }

    TypeMappingRegistry tmr = new TypeMappingRegistryImpl();

    public TypeMapping getTypeMapping(String encodingStyle) throws ConfigurationException {
        return (TypeMapping) getTypeMappingRegistry().getTypeMapping(encodingStyle);
    }

    private boolean tmrDeployed = false;

    public TypeMappingRegistry getTypeMappingRegistry() throws ConfigurationException {
        if (false == tmrDeployed) {
            Iterator i = typeMappings.values().iterator();
            while (i.hasNext()) {
                WSDDTypeMapping mapping = (WSDDTypeMapping) i.next();
                deployMapping(mapping);
            }
            tmrDeployed = true;
        }
        return tmr;
    }

    public Handler getGlobalRequest() throws ConfigurationException {
        if (globalConfig != null) {
            WSDDRequestFlow reqFlow = globalConfig.getRequestFlow();
            if (reqFlow != null)
                return reqFlow.getInstance(this);
        }
        return null;
    }

    public Handler getGlobalResponse() throws ConfigurationException {
        if (globalConfig != null) {
            WSDDResponseFlow respFlow = globalConfig.getResponseFlow();
            if (respFlow != null)
                return respFlow.getInstance(this);
        }
        return null;
    }

    public Hashtable getGlobalOptions() throws ConfigurationException {
        return globalConfig.getParametersTable();
    }

    public List getRoles() {
        return globalConfig == null ? new ArrayList() : globalConfig.getRoles();
    }

    /**
     * Get an enumeration of the services deployed to this engine
     */
    public Iterator  getDeployedServices() throws ConfigurationException {
        ArrayList serviceDescs = new ArrayList();
        for (Iterator i = services.values().iterator(); i.hasNext();) {
            WSDDService service = (WSDDService) i.next();
            try {
                service.makeNewInstance(this);
                serviceDescs.add(service.getServiceDesc());
            } catch (WSDDNonFatalException ex) {
                // If it's non-fatal, just keep on going
                log.info(Messages.getMessage("ignoringNonFatalException00"), ex);
            }
        }
        return serviceDescs.iterator();
    }

    /**
     * Register a particular namepsace which maps to a given WSDDService.
     * This will be used for namespace-based dispatching.
     *
     * @param namespace a namespace URI
     * @param service   the target WSDDService
     */
    public void registerNamespaceForService(String namespace,
                                            WSDDService service) {
        namespaceToServices.put(namespace, service);
    }

    /**
     * Remove a namespace -> WSDDService mapping.
     *
     * @param namespace the namespace URI to unmap
     */
    public void removeNamespaceMapping(String namespace) {
        namespaceToServices.remove(namespace);
    }

    public AxisEngine getEngine() {
        return engine;
    }

    public WSDDDeployment getDeployment() {
        return this;
    }

    public WSDDHandler[] getHandlers() {
        WSDDHandler [] handlerArray = new WSDDHandler[handlers.size()];
        handlers.values().toArray(handlerArray);
        return handlerArray;
    }

    public WSDDHandler getWSDDHandler(QName qname) {
        return (WSDDHandler) handlers.get(qname);
    }

    public WSDDTransport[] getTransports() {
        WSDDTransport [] transportArray = new WSDDTransport[transports.size()];
        transports.values().toArray(transportArray);
        return transportArray;
    }

    public WSDDTransport getWSDDTransport(QName qname) {
        return (WSDDTransport) transports.get(qname);
    }
}

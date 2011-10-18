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
package org.apache.axis.wsdl.toJava;

import org.apache.axis.utils.JavaUtils;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.WSDLUtils;
import org.apache.axis.wsdl.symbolTable.BindingEntry;
import org.apache.axis.wsdl.symbolTable.PortTypeEntry;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SymbolTable;
import org.apache.axis.wsdl.symbolTable.BackslashUtil;

import javax.wsdl.Binding;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This is Wsdl2java's service implementation writer.
 * It writes the <serviceName>Locator.java file.
 */
public class JavaServiceImplWriter extends JavaClassWriter {

    /** Field sEntry */
    private ServiceEntry sEntry;

    /** Field symbolTable */
    private SymbolTable symbolTable;

    /**
     * Constructor.
     * 
     * @param emitter     
     * @param sEntry      
     * @param symbolTable 
     */
    protected JavaServiceImplWriter(Emitter emitter, ServiceEntry sEntry,
                                    SymbolTable symbolTable) {

        super(emitter, sEntry.getName() + "Locator", "service");

        this.sEntry = sEntry;
        this.symbolTable = symbolTable;
    }    // ctor

    /**
     * Returns "extends org.apache.axis.client.Service ".
     * 
     * @return 
     */
    protected String getExtendsText() {
        return "extends org.apache.axis.client.Service ";
    }    // getExtendsText

    /**
     * Returns "implements <serviceInterface>".
     * 
     * @return 
     */
    protected String getImplementsText() {
        return "implements " + sEntry.getName() + ' ';
    }    // getImplementsText

    /**
     * Write the body of the service file.
     * 
     * @param pw 
     * @throws IOException 
     */
    protected void writeFileBody(PrintWriter pw) throws IOException {

        Service service = sEntry.getService();

        // output comments
        writeComment(pw, service.getDocumentationElement(), false);

        // Used to construct the getPort(Class) method.
        Vector getPortIfaces = new Vector();
        Vector getPortStubClasses = new Vector();
        Vector getPortPortNames = new Vector();
        Vector getPortPortXmlNames = new Vector();
        boolean printGetPortNotice = false;

        // get ports
        Map portMap = service.getPorts();
        Iterator portIterator = portMap.values().iterator();

        //Write the constructor for <servicename>Locator
        writeConstructors(pw);
        
        // write a get method for each of the ports with a SOAP binding
        while (portIterator.hasNext()) {
            Port p = (Port) portIterator.next();
            Binding binding = p.getBinding();

            if (binding == null) {
                throw new IOException(Messages.getMessage("emitFailNoBinding01",
                        new String[]{
                            p.getName()}));
            }

            BindingEntry bEntry =
                    symbolTable.getBindingEntry(binding.getQName());

            if (bEntry == null) {
                throw new IOException(
                        Messages.getMessage(
                                "emitFailNoBindingEntry01",
                                new String[]{binding.getQName().toString()}));
            }

            PortTypeEntry ptEntry =
                    symbolTable.getPortTypeEntry(binding.getPortType().getQName());

            if (ptEntry == null) {
                throw new IOException(
                        Messages.getMessage(
                                "emitFailNoPortType01",
                                new String[]{
                                    binding.getPortType().getQName().toString()}));
            }

            // If this isn't an SOAP binding, skip it
            if (bEntry.getBindingType() != BindingEntry.TYPE_SOAP) {
                continue;
            }

            // JSR 101 indicates that the name of the port used
            // in the java code is the name of the wsdl:port.  It
            // does not indicate what should occur if the
            // wsdl:port name is not a java identifier.  The
            // TCK depends on the case-sensitivity being preserved,
            // and the interop tests have port names that are not
            // valid java identifiers.  Thus the following code.

            // java port <--> wsdl port mapping
            String portXmlName = p.getName();
            String portName = (String) bEntry.getDynamicVar(JavaServiceWriter.PORT_NAME + ":" + p.getName());
            if (portName == null) {
                portName = p.getName();
            }

            if (!JavaUtils.isJavaId(portName)) {
                portName = Utils.xmlNameToJavaClass(portName);
            }

            String stubClass = bEntry.getName() + "Stub";
            String bindingType =
                    (String) bEntry.getDynamicVar(JavaBindingWriter.INTERFACE_NAME);

            // getPort(Class) must return a stub for an interface.  Collect all
            // the port interfaces so the getPort(Class) method can be constructed.
            if (getPortIfaces.contains(bindingType)) {
                printGetPortNotice = true;
            }

            getPortIfaces.add(bindingType);
            getPortPortXmlNames.add(portXmlName);
            getPortStubClasses.add(stubClass);
            getPortPortNames.add(portName);

            // Get endpoint address and validate it
            String address = WSDLUtils.getAddressFromPort(p);

            if (address == null) {

                // now what?
                throw new IOException(Messages.getMessage("emitFail02",
                        portName, className));
            }

            try {
                new java.net.URL(address);
            } catch (MalformedURLException e) {

                // this exception may be due to an unrecognized protocol
                // so try to instantiate the protocol handler directly
                // and use that to create the URL
                java.net.URL url = null;
                java.net.URLStreamHandler handler = null;
                String handlerPkgs =
                        System.getProperty("java.protocol.handler.pkgs");

                if (handlerPkgs != null) {
                    int protIndex = address.indexOf(":");

                    if (protIndex > 0) {
                        String protocol = address.substring(0,
                                protIndex);
                        StringTokenizer st =
                                new StringTokenizer(handlerPkgs, "|");

                        while (st.hasMoreTokens()) {
                            String pkg = st.nextToken();
                            String handlerClass = pkg + "." + protocol
                                    + ".Handler";

                            try {
                                Class c = Class.forName(handlerClass);

                                handler =
                                        (java.net.URLStreamHandler) c.newInstance();
                                url = new java.net.URL(null, address,
                                        handler);

                                break;
                            } catch (Exception e2) {
                                url = null;
                            }
                        }
                    }
                }

                if (url == null) {
                    if (emitter.isAllowInvalidURL()) {
                        // Unchecked URL mode
                        System.err.println(Messages.getMessage("emitWarnInvalidURL01", new String[] {portName, className, address}));
                    } else {
                        // Checked URL mode :
                        // URL invalid -> Exception
                        throw new IOException(Messages.getMessage("emitFail03",
                                new String[]{
                                    portName,
                                    className,
                                    address}));                        
                    }
                }
            }

            writeAddressInfo(pw, portName, address, p);

            String wsddServiceName = portName + "WSDDServiceName";

            writeWSDDServiceNameInfo(pw, wsddServiceName, portName, portXmlName);
            writeGetPortName(pw, bindingType, portName);
            writeGetPortNameURL(pw, bindingType, portName, stubClass,
                    wsddServiceName);
            writeSetPortEndpointAddress(pw, portName);
        }

        writeGetPortClass(pw, getPortIfaces, getPortStubClasses,
                getPortPortNames, printGetPortNotice);
        writeGetPortQNameClass(pw, getPortPortNames, getPortPortXmlNames);
        writeGetServiceName(pw, sEntry.getQName());
        writeGetPorts(pw, sEntry.getQName().getNamespaceURI(), getPortPortXmlNames);
        writeSetEndpointAddress(pw, getPortPortNames);
    }    // writeFileBody

    /**
     * write Constructors
     *
     * @param pw
     */
    protected void writeConstructors(PrintWriter pw) {
        //Write the default constructor
        pw.println();
        pw.println("    public " + Utils.getJavaLocalName(sEntry.getName())
                + "Locator() {");
        pw.println("    }");
        pw.println();
    	
        //Write a constructor that accepts an Engine Configuration 
        pw.println();
        pw.println("    public " + Utils.getJavaLocalName(sEntry.getName())
                + "Locator(org.apache.axis.EngineConfiguration config) {");
        pw.println("        super(config);");
        pw.println("    }");

        //Write a constructor that accepts String wsdl location + Service QName
        pw.println();
        pw.println("    public " + Utils.getJavaLocalName(sEntry.getName())
                + "Locator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) "
                + "throws " + javax.xml.rpc.ServiceException.class.getName() + " {");
        pw.println("        super(wsdlLoc, sName);");
        pw.println("    }");
    }

    /**
     * Write the private address field for this port and the public getter for it.
     * 
     * @param pw       
     * @param portName 
     * @param address  
     * @param p        
     */
    protected void writeAddressInfo(PrintWriter pw, String portName,
                                    String address, Port p) {

        // Write the private address field for this port
        pw.println();
        pw.println("    // " + Messages.getMessage("getProxy00", portName));
        writeComment(pw, p.getDocumentationElement(), true);
        pw.println("    private java.lang.String " + portName + "_address = \""
                + address + "\";");

        // Write the public address getter for this field
        pw.println();
        pw.println("    public java.lang.String get" + portName
                + "Address() {");
        pw.println("        return " + portName + "_address;");
        pw.println("    }");
        pw.println();
    }    // writeAddressInfo

    /**
     * Write the private WSDD service name field and the public accessors for it.
     * 
     * @param pw              
     * @param wsddServiceName 
     * @param portName        
     */
    protected void writeWSDDServiceNameInfo(PrintWriter pw,
                                            String wsddServiceName,
                                            String portName, String portXmlName) {

        // Write the private WSDD service name field
        pw.println("    // " + Messages.getMessage("wsddServiceName00"));
        pw.println("    private java.lang.String " + wsddServiceName + " = \""
                + portXmlName + "\";");        
        pw.println();

        // Write the public accessors for the WSDD service name
        pw.println("    public java.lang.String get" + wsddServiceName
                + "() {");
        pw.println("        return " + wsddServiceName + ";");
        pw.println("    }");
        pw.println();
        pw.println("    public void set" + wsddServiceName
                + "(java.lang.String name) {");
        pw.println("        " + wsddServiceName + " = name;");
        pw.println("    }");
        pw.println();
    }    // writeWSDDServiceNameInfo

    /**
     * Write the get<portName>() method.
     * 
     * @param pw          
     * @param bindingType 
     * @param portName    
     */
    protected void writeGetPortName(PrintWriter pw, String bindingType,
                                    String portName) {

        pw.println("    public " + bindingType + " get" + portName
                + "() throws "
                + javax.xml.rpc.ServiceException.class.getName() + " {");
        pw.println("       java.net.URL endpoint;");
        pw.println("        try {");
        pw.println("            endpoint = new java.net.URL(" + portName
                + "_address);");
        pw.println("        }");
        pw.println("        catch (java.net.MalformedURLException e) {");
        pw.println("            throw new javax.xml.rpc.ServiceException(e);");
        pw.println("        }");
        pw.println("        return get" + portName + "(endpoint);");
        pw.println("    }");
        pw.println();
    }    // writeGetPortName

    /**
     * Write the get<portName>(URL) method.
     * 
     * @param pw              
     * @param bindingType     
     * @param portName        
     * @param stubClass       
     * @param wsddServiceName 
     */
    protected void writeGetPortNameURL(PrintWriter pw, String bindingType,
                                       String portName, String stubClass,
                                       String wsddServiceName) {

        pw.println("    public " + bindingType + " get" + portName
                + "(java.net.URL portAddress) throws "
                + javax.xml.rpc.ServiceException.class.getName() + " {");
        pw.println("        try {");
        pw.println("            " + stubClass + " _stub = new " + stubClass
                + "(portAddress, this);");
        pw.println("            _stub.setPortName(get" + wsddServiceName
                + "());");
        pw.println("            return _stub;");
        pw.println("        }");
        pw.println("        catch (org.apache.axis.AxisFault e) {");
        pw.println("            return null;");
        pw.println("        }");
        pw.println("    }");
        pw.println();
    }    // writeGetPortNameURL

    /**
     * Write the set<portName>EndpointAddress(String) method.
     * 
     * @param pw       
     * @param portName 
     */
    protected void writeSetPortEndpointAddress(PrintWriter pw,
                                               String portName) {

        pw.println("    public void set" + portName
                + "EndpointAddress(java.lang.String address) {");
        pw.println("        " + portName + "_address = address;");
        pw.println("    }");
        pw.println();
    }    // writeSetPortEndpointAddress

    /**
     * Write the getPort(Class serviceInterfaceWriter) method.
     * 
     * @param pw                 
     * @param getPortIfaces      
     * @param getPortStubClasses 
     * @param getPortPortNames   
     * @param printGetPortNotice 
     */
    protected void writeGetPortClass(PrintWriter pw, Vector getPortIfaces,
                                     Vector getPortStubClasses,
                                     Vector getPortPortNames,
                                     boolean printGetPortNotice) {

        pw.println("    /**");
        pw.println("     * " + Messages.getMessage("getPortDoc00"));
        pw.println("     * " + Messages.getMessage("getPortDoc01"));
        pw.println("     * " + Messages.getMessage("getPortDoc02"));

        if (printGetPortNotice) {
            pw.println("     * " + Messages.getMessage("getPortDoc03"));
            pw.println("     * " + Messages.getMessage("getPortDoc04"));
        }

        pw.println("     */");
        pw.println(
                "    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws "
                + javax.xml.rpc.ServiceException.class.getName() + " {");

        if (getPortIfaces.size() == 0) {
            pw.println(
                    "        throw new "
                    + javax.xml.rpc.ServiceException.class.getName() + "(\""
                    + Messages.getMessage("noStub")
                    + "  \" + (serviceEndpointInterface == null ? \"null\" : serviceEndpointInterface.getName()));");
        } else {
            pw.println("        try {");

            for (int i = 0; i < getPortIfaces.size(); ++i) {
                String iface = (String) getPortIfaces.get(i);
                String stubClass = (String) getPortStubClasses.get(i);
                String portName = (String) getPortPortNames.get(i);

                pw.println(
                        "            if (" + iface
                        + ".class.isAssignableFrom(serviceEndpointInterface)) {");
                pw.println("                " + stubClass + " _stub = new "
                        + stubClass + "(new java.net.URL(" + portName
                        + "_address), this);");
                pw.println("                _stub.setPortName(get" + portName
                        + "WSDDServiceName());");
                pw.println("                return _stub;");
                pw.println("            }");
            }

            pw.println("        }");
            pw.println("        catch (java.lang.Throwable t) {");
            pw.println("            throw new "
                    + javax.xml.rpc.ServiceException.class.getName()
                    + "(t);");
            pw.println("        }");
            pw.println(
                    "        throw new "
                    + javax.xml.rpc.ServiceException.class.getName() + "(\""
                    + Messages.getMessage("noStub")
                    + "  \" + (serviceEndpointInterface == null ? \"null\" : serviceEndpointInterface.getName()));");
        }

        pw.println("    }");
        pw.println();
    }    // writeGetPortClass

    /**
     * Write the getPort(QName portName, Class serviceInterfaceWriter) method.
     * 
     * @param pw               
     * @param getPortPortNames 
     */
    protected void writeGetPortQNameClass(PrintWriter pw,
                                          Vector getPortPortNames,
            Vector getPortPortXmlNames) {

        pw.println("    /**");
        pw.println("     * " + Messages.getMessage("getPortDoc00"));
        pw.println("     * " + Messages.getMessage("getPortDoc01"));
        pw.println("     * " + Messages.getMessage("getPortDoc02"));
        pw.println("     */");
        pw.println(
                "    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws "
                + javax.xml.rpc.ServiceException.class.getName() + " {");
        pw.println("        if (portName == null) {");
        pw.println("            return getPort(serviceEndpointInterface);");
        pw.println("        }");
        pw.println("        java.lang.String inputPortName = portName.getLocalPart();");
        pw.print("        ");

        for (int i = 0; i < getPortPortNames.size(); ++i) {
            String portName = (String) getPortPortNames.get(i);
            String portXmlName = (String) getPortPortXmlNames.get(i);

            pw.println("if (\"" + portXmlName + "\".equals(inputPortName)) {");
            pw.println("            return get" + portName + "();");
            pw.println("        }");
            pw.print("        else ");
        }

        pw.println(" {");
        pw.println(
                "            java.rmi.Remote _stub = getPort(serviceEndpointInterface);");
        pw.println(
                "            ((org.apache.axis.client.Stub) _stub).setPortName(portName);");
        pw.println("            return _stub;");
        pw.println("        }");
        pw.println("    }");
        pw.println();
    }    // writeGetPortQNameClass

    /**
     * Write the getServiceName method.
     * 
     * @param pw    
     * @param qname 
     */

   protected void writeGetServiceName(PrintWriter pw, QName qname) {
    	String originalServiceName = null;
    	QName qNameWithDifferentLocal = null;
    	QName qNameWithBackslashedLocal = null;
    	
    	originalServiceName = sEntry.getOriginalServiceName();
    	qNameWithDifferentLocal = BackslashUtil.getQNameWithDifferentLocal(qname, originalServiceName);
    	qNameWithBackslashedLocal = BackslashUtil.getQNameWithBackslashedLocal(qNameWithDifferentLocal);
    	
        pw.println("    public javax.xml.namespace.QName getServiceName() {");
        
        pw.println("        return " +  Utils.getNewQName(qNameWithBackslashedLocal) + ";");
        pw.println("    }");
        pw.println();
    }    // writeGetServiceName    

    /**
     * Write the getPorts method.
     * 
     * @param pw        
     * @param portNames 
     */
    protected void writeGetPorts(PrintWriter pw, String namespaceURI, Vector portNames) {

        pw.println("    private java.util.HashSet ports = null;");
        pw.println();
        pw.println("    public java.util.Iterator getPorts() {");
        pw.println("        if (ports == null) {");
        pw.println("            ports = new java.util.HashSet();");

        for (int i = 0; i < portNames.size(); ++i) {
            pw.println("            ports.add(new javax.xml.namespace.QName(\"" + 
                    namespaceURI + "\", \"" + portNames.get(i) + "\"));");
        }

        pw.println("        }");
        pw.println("        return ports.iterator();");
        pw.println("    }");
        pw.println();
    }    // writeGetPorts

    /**
     * Write the setEndpointAddress(String portName, String newAddress)
     * and setEndpointAddress(QName portName, String newAddress) methods.
     * 
     * @param pw        
     * @param portNames 
     */
    protected void writeSetEndpointAddress(PrintWriter pw, Vector portNames) {

        if (portNames.isEmpty()) {
            return;
        }

        // String method
        pw.println("    /**");
        pw.println("    * " + Messages.getMessage("setEndpointDoc00"));
        pw.println("    */");
        pw.println(
                "    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws "
                + javax.xml.rpc.ServiceException.class.getName() + " {");

        pw.println("        ");
        for (Iterator p = portNames.iterator(); p.hasNext();) {
            String name = (String) p.next();

            pw.println("if (\"" + name + "\".equals(portName)) {");
            pw.println("            set" + name + "EndpointAddress(address);");
            pw.println("        }");
            pw.println("        else ");
        }

        pw.println("{ // Unknown Port Name");
        pw.println("            throw new "
                + javax.xml.rpc.ServiceException.class.getName() + "(\" "
                + Messages.getMessage("unknownPortName")
                + "\" + portName);");
        pw.println("        }");
        pw.println("    }");
        pw.println();

        // QName method
        pw.println("    /**");
        pw.println("    * " + Messages.getMessage("setEndpointDoc00"));
        pw.println("    */");
        pw.println(
                "    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws "
                + javax.xml.rpc.ServiceException.class.getName() + " {");
        pw.println(
                "        setEndpointAddress(portName.getLocalPart(), address);");
        pw.println("    }");
        pw.println();
    }    // writeSetEndpointAddress
}    // class JavaServiceImplWriter

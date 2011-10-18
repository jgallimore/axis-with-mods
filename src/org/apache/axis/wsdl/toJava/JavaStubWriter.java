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

import org.apache.axis.Constants;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.JavaUtils;
import org.apache.axis.utils.StringUtils;
import org.apache.axis.wsdl.symbolTable.BindingEntry;
import org.apache.axis.wsdl.symbolTable.CollectionType;
import org.apache.axis.wsdl.symbolTable.DefinedType;
import org.apache.axis.wsdl.symbolTable.FaultInfo;
import org.apache.axis.wsdl.symbolTable.Parameter;
import org.apache.axis.wsdl.symbolTable.Parameters;
import org.apache.axis.wsdl.symbolTable.SchemaUtils;
import org.apache.axis.wsdl.symbolTable.SymbolTable;
import org.apache.axis.wsdl.symbolTable.TypeEntry;
import org.apache.axis.wsdl.symbolTable.DefinedElement;
import org.apache.commons.logging.Log;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Fault;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;

/**
 * This is Wsdl2java's stub writer.  It writes the <BindingName>Stub.java
 * file which contains the <bindingName>Stub class.
 */
public class JavaStubWriter extends JavaClassWriter {

    /** Field log */
    protected static Log log = LogFactory.getLog(JavaStubWriter.class.getName());
    
    /** Field bEntry */
    private BindingEntry bEntry;

    /** Field binding */
    private Binding binding;

    /** Field symbolTable */
    private SymbolTable symbolTable;

    // the maximum number of java type <-> qname binding instructions we'll
    // emit in a single method.  This is important for stubs that handle
    // a large number of schema types, as the generated source can exceed
    // the size in a single method by the VM.

    /** Field MAXIMUM_BINDINGS_PER_METHOD */
    private static final int MAXIMUM_BINDINGS_PER_METHOD = 100;

    /** Field modeStrings */
    static String[] modeStrings = new String[]{"",
                                               "org.apache.axis.description.ParameterDesc.IN",
                                               "org.apache.axis.description.ParameterDesc.OUT",
                                               "org.apache.axis.description.ParameterDesc.INOUT"};

    /** Field styles */
    static Map styles = new HashMap();

    /** Field uses */
    static Map uses = new HashMap();

    static {
        styles.put(Style.DOCUMENT, "org.apache.axis.constants.Style.DOCUMENT");
        styles.put(Style.RPC, "org.apache.axis.constants.Style.RPC");
        styles.put(Style.MESSAGE, "org.apache.axis.constants.Style.MESSAGE");
        styles.put(Style.WRAPPED, "org.apache.axis.constants.Style.WRAPPED");
        uses.put(Use.ENCODED, "org.apache.axis.constants.Use.ENCODED");
        uses.put(Use.LITERAL, "org.apache.axis.constants.Use.LITERAL");
    }

    /** Field OPERDESC_PER_BLOCK */
    static int OPERDESC_PER_BLOCK = 10;

    /**
     * Constructor.
     * 
     * @param emitter     
     * @param bEntry      
     * @param symbolTable 
     */
    public JavaStubWriter(Emitter emitter, BindingEntry bEntry,
                             SymbolTable symbolTable) {

        super(emitter, bEntry.getName() + "Stub", "stub");

        this.bEntry = bEntry;
        this.binding = bEntry.getBinding();
        this.symbolTable = symbolTable;
    }    // ctor

    /**
     * Returns "extends org.apache.axis.client.Stub ".
     * 
     * @return 
     */
    protected String getExtendsText() {
        return "extends org.apache.axis.client.Stub ";
    }    // getExtendsText

    /**
     * Returns "implements <SEI> ".
     * 
     * @return 
     */
    protected String getImplementsText() {
        return "implements "
                + bEntry.getDynamicVar(JavaBindingWriter.INTERFACE_NAME) + " ";
    }    // getImplementsText

    /**
     * Write the body of the binding's stub file.
     * 
     * @param pw 
     * @throws IOException 
     */
    protected void writeFileBody(PrintWriter pw) throws IOException {

        PortType portType = binding.getPortType();
        HashSet types = getTypesInPortType(portType);
        boolean hasMIME = Utils.hasMIME(bEntry);

        if ((types.size() > 0) || hasMIME) {
            pw.println(
                    "    private java.util.Vector cachedSerClasses = new java.util.Vector();");
            pw.println(
                    "    private java.util.Vector cachedSerQNames = new java.util.Vector();");
            pw.println(
                    "    private java.util.Vector cachedSerFactories = new java.util.Vector();");
            pw.println(
                    "    private java.util.Vector cachedDeserFactories = new java.util.Vector();");
        }

        pw.println();
        pw.println(
                "    static org.apache.axis.description.OperationDesc [] _operations;");
        pw.println();
        writeOperationMap(pw);
        pw.println();
        pw.println("    public " + className
                + "() throws org.apache.axis.AxisFault {");
        pw.println("         this(null);");
        pw.println("    }");
        pw.println();
        pw.println(
                "    public " + className
                + "(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {");
        pw.println("         this(service);");
        pw.println("         super.cachedEndpoint = endpointURL;");
        pw.println("    }");
        pw.println();
        pw.println(
                "    public " + className
                + "(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {");
        pw.println("        if (service == null) {");
        pw.println(
                "            super.service = new org.apache.axis.client.Service();");
        pw.println("        } else {");
        pw.println("            super.service = service;");
        pw.println("        }");
        pw.println("        ((org.apache.axis.client.Service)super.service).setTypeMappingVersion(\"" + emitter.getTypeMappingVersion() + "\");");

        List deferredBindings = new ArrayList();

        // keep track of how many type mappings we write out
        int typeMappingCount = 0;

        if (types.size() > 0) {
            Iterator it = types.iterator();

            while (it.hasNext()) {
                TypeEntry type = (TypeEntry) it.next();

                if (!Utils.shouldEmit(type)) {
                    continue;
                }

                // Write out serializer declarations
                if (typeMappingCount == 0) {
                    writeSerializationDecls(
                            pw, hasMIME, binding.getQName().getNamespaceURI());
                }

                // write the type mapping for this type
                // writeSerializationInit(pw, type);
                deferredBindings.add(type);

                // increase the number of type mappings count
                typeMappingCount++;
            }
        }

        // Sort the TypeEntry's by their qname.
        Collections.sort(deferredBindings, new Comparator() {
            public int compare(Object a, Object b) {
                TypeEntry type1 = (TypeEntry)a;
                TypeEntry type2 = (TypeEntry)b;
                return type1.getQName().toString().compareToIgnoreCase(type2.getQName().toString());
            }
        });

        // We need to write out the MIME mapping, even if we don't have
        // any type mappings
        if ((typeMappingCount == 0) && hasMIME) {
            writeSerializationDecls(pw, hasMIME,
                    binding.getQName().getNamespaceURI());

            typeMappingCount++;
        }

        // track whether the number of bindings exceeds the threshold
        // that we allow per method.
        boolean needsMultipleBindingMethods = false;

        if (deferredBindings.size() < MAXIMUM_BINDINGS_PER_METHOD) {

            // small number of bindings, just inline them:
            for (Iterator it = deferredBindings.iterator(); it.hasNext();) {
                writeSerializationInit(pw, (TypeEntry) it.next());
            }
        } else {
            needsMultipleBindingMethods = true;

            int methodCount = calculateBindingMethodCount(deferredBindings);

            // invoke each of the soon-to-be generated addBindings methods
            // from the constructor.
            for (int i = 0; i < methodCount; i++) {
                pw.println("        addBindings" + i + "();");
            }
        }

        pw.println("    }");
        pw.println();

        // emit any necessary methods for assembling binding metadata.
        if (needsMultipleBindingMethods) {
            writeBindingMethods(pw, deferredBindings);
            pw.println();
        }

        pw.println(
                "    protected org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {");
        pw.println("        try {");
        pw.println("            org.apache.axis.client.Call _call = super._createCall();");
        pw.println("            if (super.maintainSessionSet) {");
        pw.println(
                "                _call.setMaintainSession(super.maintainSession);");
        pw.println("            }");
        pw.println("            if (super.cachedUsername != null) {");
        pw.println("                _call.setUsername(super.cachedUsername);");
        pw.println("            }");
        pw.println("            if (super.cachedPassword != null) {");
        pw.println("                _call.setPassword(super.cachedPassword);");
        pw.println("            }");
        pw.println("            if (super.cachedEndpoint != null) {");
        pw.println(
                "                _call.setTargetEndpointAddress(super.cachedEndpoint);");
        pw.println("            }");
        pw.println("            if (super.cachedTimeout != null) {");
        pw.println("                _call.setTimeout(super.cachedTimeout);");
        pw.println("            }");
        pw.println("            if (super.cachedPortName != null) {");
        pw.println("                _call.setPortName(super.cachedPortName);");
        pw.println("            }");
        pw.println(
                "            java.util.Enumeration keys = super.cachedProperties.keys();");
        pw.println("            while (keys.hasMoreElements()) {");
        pw.println(
                "                java.lang.String key = (java.lang.String) keys.nextElement();");
        pw.println(
                "                _call.setProperty(key, super.cachedProperties.get(key));");
        pw.println("            }");

        if (typeMappingCount > 0) {
            pw.println("            // " + Messages.getMessage("typeMap00"));
            pw.println("            // " + Messages.getMessage("typeMap01"));
            pw.println("            // " + Messages.getMessage("typeMap02"));
            pw.println("            // " + Messages.getMessage("typeMap03"));
            pw.println("            // " + Messages.getMessage("typeMap04"));
            pw.println("            synchronized (this) {");
            pw.println("                if (firstCall()) {");

            // Hack alert - we need to establish the encoding style before we register type mappings due
            // to the fact that TypeMappings key off of encoding style
            pw.println("                    // "
                    + Messages.getMessage("mustSetStyle"));

            if (bEntry.hasLiteral()) {
                pw.println("                    _call.setEncodingStyle(null);");
            } else {
                Iterator iterator =
                        bEntry.getBinding().getExtensibilityElements().iterator();

                while (iterator.hasNext()) {
                    Object obj = iterator.next();

                    if (obj instanceof SOAPBinding) {
                        pw.println(
                                "                    _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);");
                        pw.println(
                                "                    _call.setEncodingStyle(org.apache.axis.Constants.URI_SOAP11_ENC);");
                    } else if (obj instanceof UnknownExtensibilityElement) {

                        // TODO: After WSDL4J supports soap12, change this code
                        UnknownExtensibilityElement unkElement =
                                (UnknownExtensibilityElement) obj;
                        QName name =
                                unkElement.getElementType();

                        if (name.getNamespaceURI().equals(
                                Constants.URI_WSDL12_SOAP)
                                && name.getLocalPart().equals("binding")) {
                            pw.println(
                                    "                    _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);");
                            pw.println(
                                    "                    _call.setEncodingStyle(org.apache.axis.Constants.URI_SOAP12_ENC);");
                        }
                    }
                }
            }

            pw.println(
                    "                    for (int i = 0; i < cachedSerFactories.size(); ++i) {");
            pw.println(
                    "                        java.lang.Class cls = (java.lang.Class) cachedSerClasses.get(i);");
            pw.println(
                    "                        javax.xml.namespace.QName qName =");
            pw.println(
                    "                                (javax.xml.namespace.QName) cachedSerQNames.get(i);");
            pw.println(
                    "                        java.lang.Object x = cachedSerFactories.get(i);");
            pw.println(
                    "                        if (x instanceof Class) {");
            pw.println(
                    "                            java.lang.Class sf = (java.lang.Class)");
            pw.println(
                    "                                 cachedSerFactories.get(i);");
            pw.println(
                    "                            java.lang.Class df = (java.lang.Class)");
            pw.println(
                    "                                 cachedDeserFactories.get(i);");
            pw.println(
                    "                            _call.registerTypeMapping(cls, qName, sf, df, false);");

            pw.println("                        }");
            pw.println(
                    "                        else if (x instanceof javax.xml.rpc.encoding.SerializerFactory) {");
            pw.println(
                    "                            org.apache.axis.encoding.SerializerFactory sf = (org.apache.axis.encoding.SerializerFactory)");
            pw.println(
                    "                                 cachedSerFactories.get(i);");
            pw.println(
                    "                            org.apache.axis.encoding.DeserializerFactory df = (org.apache.axis.encoding.DeserializerFactory)");
            pw.println(
                    "                                 cachedDeserFactories.get(i);");
            pw.println(
                    "                            _call.registerTypeMapping(cls, qName, sf, df, false);");

            pw.println("                        }");
            pw.println("                    }");
            pw.println("                }");
            pw.println("            }");
        }

        pw.println("            return _call;");
        pw.println("        }");
        pw.println("        catch (java.lang.Throwable _t) {");
        pw.println("            throw new org.apache.axis.AxisFault(\""
                + Messages.getMessage("badCall01") + "\", _t);");
        pw.println("        }");
        pw.println("    }");
        pw.println();

        List operations = binding.getBindingOperations();

        for (int i = 0; i < operations.size(); ++i) {
            BindingOperation operation = (BindingOperation) operations.get(i);
            Parameters parameters =
                    bEntry.getParameters(operation.getOperation());

            // Get the soapAction from the <soap:operation>
            String soapAction = "";
            String opStyle = null;
            Iterator operationExtensibilityIterator =
                    operation.getExtensibilityElements().iterator();

            for (; operationExtensibilityIterator.hasNext();) {
                Object obj = operationExtensibilityIterator.next();

                if (obj instanceof SOAPOperation) {
                    soapAction = ((SOAPOperation) obj).getSoapActionURI();
                    opStyle = ((SOAPOperation) obj).getStyle();

                    break;
                } else if (obj instanceof UnknownExtensibilityElement) {

                    // TODO: After WSDL4J supports soap12, change this code
                    UnknownExtensibilityElement unkElement =
                            (UnknownExtensibilityElement) obj;
                    QName name =
                            unkElement.getElementType();

                    if (name.getNamespaceURI().equals(Constants.URI_WSDL12_SOAP)
                            && name.getLocalPart().equals("operation")) {
                        if (unkElement.getElement().getAttribute("soapAction")
                                != null) {
                            soapAction = unkElement.getElement().getAttribute(
                                    "soapAction");
                        }

                        opStyle = unkElement.getElement().getAttribute("style");
                    }
                }
            }

            Operation ptOperation = operation.getOperation();
            OperationType type = ptOperation.getStyle();

            // These operation types are not supported.  The signature
            // will be a string stating that fact.
            if ((OperationType.NOTIFICATION.equals(type))
                    || (OperationType.SOLICIT_RESPONSE.equals(type))) {
                pw.println(parameters.signature);
                pw.println();
            } else {
                writeOperation(pw, operation, parameters, soapAction, opStyle,
                        type == OperationType.ONE_WAY, i);
            }
        }
    }    // writeFileBody

    /**
     * Compute the number of addBindings methods we need to generate for the
     * set of TypeEntries used by the generated stub.
     * 
     * @param deferredBindings a <code>List</code> value
     * @return an <code>int</code> value
     */
    private int calculateBindingMethodCount(List deferredBindings) {

        int methodCount = deferredBindings.size() / MAXIMUM_BINDINGS_PER_METHOD;

        if ((deferredBindings.size() % MAXIMUM_BINDINGS_PER_METHOD) != 0) {
            methodCount++;
        }

        return methodCount;
    }

    /**
     * for each of the TypeEntry objects in the deferredBindings list, we need
     * to write code that will associate a class with a schema namespace/name.
     * This method writes a number of private methods out that do this in
     * batches of size MAXIMUM_BINDINGS_PER_METHOD so that generated classes
     * do not end up with a single method that exceeds the 64K limit that the
     * VM imposes on all methods.
     * 
     * @param pw               a <code>PrintWriter</code> value
     * @param deferredBindings a <code>List</code> of TypeEntry objects
     */
    protected void writeBindingMethods(PrintWriter pw, List deferredBindings) {

        int methodCount = calculateBindingMethodCount(deferredBindings);

        for (int i = 0; i < methodCount; i++) {
            pw.println("    private void addBindings" + i + "() {");

            // each method gets its own local variables for use in generating
            // the binding code
            writeSerializationDecls(pw, false, null);

            for (int j = 0; j < MAXIMUM_BINDINGS_PER_METHOD; j++) {
                int absolute = i * MAXIMUM_BINDINGS_PER_METHOD + j;

                if (absolute == deferredBindings.size()) {
                    break;    // last one
                }

                writeSerializationInit(
                        pw, (TypeEntry) deferredBindings.get(absolute));
            }

            pw.println("    }");
        }
    }

    /**
     * Method writeOperationMap
     * 
     * @param pw 
     */
    protected void writeOperationMap(PrintWriter pw) {

        List operations = binding.getBindingOperations();

        pw.println("    static {");
        pw.println(
                "        _operations = new org.apache.axis.description.OperationDesc["
                + operations.size() + "];");

        for (int j = 0, k = 0; j < operations.size(); ++j) {
            if ((j % OPERDESC_PER_BLOCK) == 0) {
                k++;

                pw.println("        _initOperationDesc" + k + "();");
            }
        }

        for (int i = 0, k = 0; i < operations.size(); ++i) {
            if ((i % OPERDESC_PER_BLOCK) == 0) {
                k++;

                pw.println("    }\n");
                pw.println("    private static void _initOperationDesc" + k
                        + "(){");
                pw.println(
                        "        org.apache.axis.description.OperationDesc oper;");
                pw.println(
                        "        org.apache.axis.description.ParameterDesc param;");
            }

            BindingOperation operation = (BindingOperation) operations.get(i);
            Parameters parameters =
                    bEntry.getParameters(operation.getOperation());

            // Get the soapAction from the <soap:operation>
            String opStyle = null;
            Iterator operationExtensibilityIterator =
                    operation.getExtensibilityElements().iterator();

            for (; operationExtensibilityIterator.hasNext();) {
                Object obj = operationExtensibilityIterator.next();

                if (obj instanceof SOAPOperation) {
                    opStyle = ((SOAPOperation) obj).getStyle();

                    break;
                } else if (obj instanceof UnknownExtensibilityElement) {

                    // TODO: After WSDL4J supports soap12, change this code
                    UnknownExtensibilityElement unkElement =
                            (UnknownExtensibilityElement) obj;
                    QName name =
                            unkElement.getElementType();

                    if (name.getNamespaceURI().equals(Constants.URI_WSDL12_SOAP)
                            && name.getLocalPart().equals("operation")) {
                        opStyle = unkElement.getElement().getAttribute("style");
                    }
                }
            }

            Operation ptOperation = operation.getOperation();
            OperationType type = ptOperation.getStyle();

            // These operation types are not supported.  The signature
            // will be a string stating that fact.
            if ((OperationType.NOTIFICATION.equals(type))
                    || (OperationType.SOLICIT_RESPONSE.equals(type))) {
                pw.println(parameters.signature);
                pw.println();
            }

            String operName = operation.getName();
            String indent = "        ";

            pw.println(
                    indent
                    + "oper = new org.apache.axis.description.OperationDesc();");
            pw.println(indent + "oper.setName(\"" + operName + "\");");

            // loop over paramters and set up in/out params
            for (int j = 0; j < parameters.list.size(); ++j) {
                Parameter p = (Parameter) parameters.list.get(j);

                // Get the QName representing the parameter type
                QName paramType = Utils.getXSIType(p);

                // Set the javaType to the name of the type
                String javaType = Utils.getParameterTypeName(p);
                    if (javaType != null) {
                        javaType += ".class, ";
                    } else {
                        javaType = "null, ";
                    }

                // Get the text representing newing a QName for the name and type
                String paramNameText = Utils.getNewQNameWithLastLocalPart(p.getQName());
                String paramTypeText = Utils.getNewQName(paramType);

                // Generate the addParameter call with the
                // name qname, typeQName, optional javaType, and mode
                boolean isInHeader = p.isInHeader();
                boolean isOutHeader = p.isOutHeader();

                pw.println("        param = new org.apache.axis.description.ParameterDesc(" +
                           paramNameText + ", " +
                           modeStrings[p.getMode()] + ", " +
                           paramTypeText + ", " +
                           javaType +
                           isInHeader + ", " + isOutHeader + ");");

                QName itemQName = Utils.getItemQName(p.getType());
                if (itemQName != null) {
                    pw.println("        param.setItemQName(" +
                               Utils.getNewQName(itemQName) + ");");
                }

                if (p.isOmittable())
                    pw.println("        param.setOmittable(true);");

                if (p.isNillable())
                    pw.println("        param.setNillable(true);");

                pw.println("        oper.addParameter(param);");
            }

            // set output type
            Parameter returnParam = parameters.returnParam;
            if (returnParam != null) {

                // Get the QName for the return Type
                QName returnType = Utils.getXSIType(returnParam);

                // Get the javaType
                String javaType = Utils.getParameterTypeName(returnParam);

                if (javaType == null) {
                    javaType = "";
                } else {
                    javaType += ".class";
                }

                pw.println("        oper.setReturnType("
                        + Utils.getNewQName(returnType) + ");");
                pw.println("        oper.setReturnClass(" + javaType + ");");

                QName returnQName = returnParam.getQName();

                if (returnQName != null) {
                    pw.println("        oper.setReturnQName("
                            + Utils.getNewQNameWithLastLocalPart(returnQName) + ");");
                }

                if (returnParam.isOutHeader()) {
                    pw.println("        oper.setReturnHeader(true);");
                }

                QName itemQName = Utils.getItemQName(returnParam.getType());
                if (itemQName != null) {
                    pw.println("        param = oper.getReturnParamDesc();");
                    pw.println("        param.setItemQName(" +
                               Utils.getNewQName(itemQName) + ");");
                }

            } else {
                pw.println(
                        "        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);");
            }

            boolean hasMIME = Utils.hasMIME(bEntry, operation);
            Style style = Style.getStyle(opStyle, bEntry.getBindingStyle());
            Use use = bEntry.getInputBodyType(operation.getOperation());

            if ((style == Style.DOCUMENT) && symbolTable.isWrapped()) {
                style = Style.WRAPPED;
            }

            if (!hasMIME) {
                pw.println("        oper.setStyle(" + styles.get(style) + ");");
                pw.println("        oper.setUse(" + uses.get(use) + ");");
            }

            // Register fault/exception information for this operation
            writeFaultInfo(pw, operation);
            pw.println(indent + "_operations[" + i + "] = oper;");
            pw.println("");
        }

        pw.println("    }");
    }

    /**
     * This method returns a set of all the TypeEntry in a given PortType.
     * The elements of the returned HashSet are Types.
     * 
     * @param portType 
     * @return 
     */
    private HashSet getTypesInPortType(PortType portType) {

        HashSet types = new HashSet();
        HashSet firstPassTypes = new HashSet();

        // Get all the types from all the operations
        List operations = portType.getOperations();

        for (int i = 0; i < operations.size(); ++i) {
            Operation op = (Operation) operations.get(i);

            firstPassTypes.addAll(getTypesInOperation(op));
        }

        // Add all the types nested and derived from the types
        // in the first pass.
        Iterator i = firstPassTypes.iterator();

        while (i.hasNext()) {
            TypeEntry type = (TypeEntry) i.next();

            if (!types.contains(type)) {
                types.add(type);
                types.addAll(type.getNestedTypes(symbolTable, true));
            }
        }

         if(emitter.isAllWanted()) {
             HashMap rawSymbolTable = symbolTable.getHashMap();
             for(Iterator j = rawSymbolTable.values().iterator(); j.hasNext(); ) {
                 Vector typeVector = (Vector)j.next();
                 for(Iterator k = typeVector.iterator(); k.hasNext(); ) {
                     Object symbol = k.next();
                     if(symbol instanceof DefinedType) {
                         TypeEntry type = (TypeEntry)symbol;
                         if(!types.contains(type)) {
                             types.add(type);
                         }
                     }
                 }
             }
         }        
        return types;
    }    // getTypesInPortType

    /**
     * This method returns a set of all the TypeEntry in a given Operation.
     * The elements of the returned HashSet are TypeEntry.
     * 
     * @param operation 
     * @return 
     */
    private HashSet getTypesInOperation(Operation operation) {

        HashSet types = new HashSet();
        Vector v = new Vector();
        Parameters params = bEntry.getParameters(operation);

        // Loop over parameter types for this operation
        for (int i = 0; i < params.list.size(); i++) {
            Parameter p = (Parameter) params.list.get(i);

            v.add(p.getType());
        }

        // Add the return type
        if (params.returnParam != null) {
            v.add(params.returnParam.getType());
        }

        // Collect all the types in faults
        Map faults = operation.getFaults();

        if (faults != null) {
            Iterator i = faults.values().iterator();

            while (i.hasNext()) {
                Fault f = (Fault) i.next();

                partTypes(v, f.getMessage().getOrderedParts(null));
            }
        }

        // Put all these types into a set.  This operation eliminates all duplicates.
        for (int i = 0; i < v.size(); i++) {
            types.add(v.get(i));
        }

        return types;
    }    // getTypesInOperation

    /**
     * This method returns a vector of TypeEntry for the parts.
     * 
     * @param v     
     * @param parts 
     */
    private void partTypes(Vector v, Collection parts) {

        Iterator i = parts.iterator();

        while (i.hasNext()) {
            Part part = (Part) i.next();
            QName qType = part.getTypeName();

            if (qType != null) {
                v.add(symbolTable.getType(qType));
            } else {
                qType = part.getElementName();

                if (qType != null) {
                    v.add(symbolTable.getElement(qType));
                }
            }
        }    // while
    }        // partTypes

    /**
     * This function writes the regsiterFaultInfo API calls
     * 
     * @param pw     
     * @param bindOp 
     */
    protected void writeFaultInfo(PrintWriter pw, BindingOperation bindOp) {

        Map faultMap = bEntry.getFaults();

        // Get the list of faults for this operation
        ArrayList faults = (ArrayList) faultMap.get(bindOp);

        // check for no faults
        if (faults == null) {
            return;
        }

        // For each fault, register its information
        for (Iterator faultIt = faults.iterator(); faultIt.hasNext();) {
            FaultInfo info = (FaultInfo) faultIt.next();
            QName qname = info.getQName();
            Message message = info.getMessage();

            // if no parts in fault, skip it!
            if (qname == null) {
                continue;
            }

            // Get the Exception class name
            String className = Utils.getFullExceptionName(message, symbolTable);

            // output the registration API call
            pw.println(
                    "        oper.addFault(new org.apache.axis.description.FaultDesc(");
            pw.println("                      " + Utils.getNewQName(qname)
                    + ",");
            pw.println("                      \"" + className + "\",");
            pw.println("                      "
                    + Utils.getNewQName(info.getXMLType()) + ", ");
            pw.println("                      "
                    + Utils.isFaultComplex(message, symbolTable));
            pw.println("                     ));");
        }
    }

    /**
     * In the stub constructor, write the serializer code for the complex types.
     * 
     * @param pw        
     * @param hasMIME   
     * @param namespace 
     */
    protected void writeSerializationDecls(PrintWriter pw, boolean hasMIME,
                                         String namespace) {

        pw.println("            java.lang.Class cls;");
        pw.println("            javax.xml.namespace.QName qName;");
        pw.println("            javax.xml.namespace.QName qName2;");
        pw.println(
                "            java.lang.Class beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;");
        pw.println(
                "            java.lang.Class beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;");
        pw.println(
                "            java.lang.Class enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;");
        pw.println(
                "            java.lang.Class enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;");
        pw.println(
                "            java.lang.Class arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;");
        pw.println(
                "            java.lang.Class arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;");
        pw.println(
                "            java.lang.Class simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;");
        pw.println(
                "            java.lang.Class simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;");
        pw.println(
                "            java.lang.Class simplelistsf = org.apache.axis.encoding.ser.SimpleListSerializerFactory.class;");
        pw.println(
                "            java.lang.Class simplelistdf = org.apache.axis.encoding.ser.SimpleListDeserializerFactory.class;");
        
        if (hasMIME) {
            pw.println(
                    "            java.lang.Class mimesf = org.apache.axis.encoding.ser.JAFDataHandlerSerializerFactory.class;");
            pw.println(
                    "            java.lang.Class mimedf = org.apache.axis.encoding.ser.JAFDataHandlerDeserializerFactory.class;");
            pw.println();

            QName qname = new QName(namespace, "DataHandler");

            pw.println("            qName = new javax.xml.namespace.QName(\""
                    + qname.getNamespaceURI() + "\", \""
                    + qname.getLocalPart() + "\");");
            pw.println("            cachedSerQNames.add(qName);");
            pw.println("            cls = javax.activation.DataHandler.class;");
            pw.println("            cachedSerClasses.add(cls);");
            pw.println("            cachedSerFactories.add(mimesf);");
            pw.println("            cachedDeserFactories.add(mimedf);");
            pw.println();
        }
    }    // writeSerializationDecls

    /**
     * Method writeSerializationInit
     * 
     * @param pw   
     * @param type 
     */
    protected void writeSerializationInit(PrintWriter pw, TypeEntry type) {

        QName qname = type.getQName();

        pw.println("            qName = new javax.xml.namespace.QName(\""
                + qname.getNamespaceURI() + "\", \"" + qname.getLocalPart()
                + "\");");
        pw.println("            cachedSerQNames.add(qName);");
        pw.println("            cls = " + type.getName() + ".class;");
        pw.println("            cachedSerClasses.add(cls);");

        if (type.getName().endsWith("[]")) {
            if (SchemaUtils.isListWithItemType(type.getNode())) {
                pw.println("            cachedSerFactories.add(simplelistsf);");
                pw.println("            cachedDeserFactories.add(simplelistdf);");
            } else {
                // We use a custom serializer if WSDL told us the component type of the array.
                // Both factories must be an instance, so we create a ArrayDeserializerFactory
                if (type.getComponentType() != null) {
                    QName ct = type.getComponentType();
                    QName name = type.getItemQName();
                    pw.println("            qName = new javax.xml.namespace.QName(\""
                            + ct.getNamespaceURI() + "\", \"" + ct.getLocalPart()
                            + "\");");
                    if(name != null) {
                        pw.println("            qName2 = new javax.xml.namespace.QName(\""
                                + name.getNamespaceURI() + "\", \"" + name.getLocalPart()
                                + "\");");
                    } else {
                        pw.println("            qName2 = null;");
                    }
                    pw.println("            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));");
                    pw.println("            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());");
                } else {
                    pw.println("            cachedSerFactories.add(arraysf);");
                    pw.println("            cachedDeserFactories.add(arraydf);");
                }
            }
        } else if ((type.getNode() != null) && (Utils.getEnumerationBaseAndValues(
                type.getNode(), symbolTable) != null)) {
            pw.println("            cachedSerFactories.add(enumsf);");
            pw.println("            cachedDeserFactories.add(enumdf);");
        } else if (type.isSimpleType()) {
            pw.println("            cachedSerFactories.add(org.apache.axis.encoding.ser.BaseSerializerFactory.createFactory(" +
                    "org.apache.axis.encoding.ser.SimpleSerializerFactory.class, cls, qName));");
            pw.println("            cachedDeserFactories.add(org.apache.axis.encoding.ser.BaseDeserializerFactory.createFactory(" +
                    "org.apache.axis.encoding.ser.SimpleDeserializerFactory.class, cls, qName));");
        } else if (type.getBaseType() != null) {

            // serializers are not required for types derived from base types
            // java type to qname mapping is anyway established by default
            // note that we have to add null to the serfactories vector to
            // keep the order of other entries, this is not going to screw
            // up because if type mapping returns null for a serialization
            // factory, it is assumed to be not-defined and the delegate
            // will be checked, the end delegate is DefaultTypeMappingImpl
            // that'll get it right with the base type name
            pw.println("            cachedSerFactories.add(null);");
            pw.println("            cachedDeserFactories.add(simpledf);");
        } else {
            pw.println("            cachedSerFactories.add(beansf);");
            pw.println("            cachedDeserFactories.add(beandf);");
        }

        pw.println();
    }    // writeSerializationInit

    /**
     * Write the stub code for the given operation.
     * 
     * @param pw         
     * @param operation  
     * @param parms      
     * @param soapAction 
     * @param opStyle    
     * @param oneway     
     * @param opIndex    
     */
    protected void writeOperation(PrintWriter pw, BindingOperation operation,
                                Parameters parms, String soapAction,
                                String opStyle, boolean oneway, int opIndex) {

        writeComment(pw, operation.getDocumentationElement(), true);
        
        if (parms.signature == null) {
        	return;
        }
        pw.println(parms.signature + " {");
        pw.println("        if (super.cachedEndpoint == null) {");
        pw.println(
                "            throw new org.apache.axis.NoEndPointException();");
        pw.println("        }");
        pw.println("        org.apache.axis.client.Call _call = createCall();");
        pw.println("        _call.setOperation(_operations[" + opIndex + "]);");

        // SoapAction
        if (soapAction != null) {
            pw.println("        _call.setUseSOAPAction(true);");
            pw.println("        _call.setSOAPActionURI(\"" + soapAction
                    + "\");");
        }

        boolean hasMIME = Utils.hasMIME(bEntry, operation);

        // Encoding: literal or encoded use.
        Use use = bEntry.getInputBodyType(operation.getOperation());

        if (use == Use.LITERAL) {

            // Turn off encoding
            pw.println("        _call.setEncodingStyle(null);");

            // turn off XSI types
            pw.println(
                    "        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);");
        }

        if (hasMIME || (use == Use.LITERAL)) {

            // If it is literal, turn off multirefs.
            // 
            // If there are any MIME types, turn off multirefs.
            // I don't know enough about the guts to know why
            // attachments don't work with multirefs, but they don't.
            pw.println(
                    "        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);");
        }

        Style style = Style.getStyle(opStyle, bEntry.getBindingStyle());

        if ((style == Style.DOCUMENT) && symbolTable.isWrapped()) {
            style = Style.WRAPPED;
        }

        Iterator iterator =
                bEntry.getBinding().getExtensibilityElements().iterator();

        while (iterator.hasNext()) {
            Object obj = iterator.next();

            if (obj instanceof SOAPBinding) {
                pw.println(
                        "        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);");
            } else if (obj instanceof UnknownExtensibilityElement) {

                // TODO: After WSDL4J supports soap12, change this code
                UnknownExtensibilityElement unkElement =
                        (UnknownExtensibilityElement) obj;
                QName name =
                        unkElement.getElementType();

                if (name.getNamespaceURI().equals(Constants.URI_WSDL12_SOAP)
                        && name.getLocalPart().equals("binding")) {
                    pw.println(
                            "        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);");
                }
            }
        }

        // Operation name
        if (style == Style.WRAPPED) {

            // We need to make sure the operation name, which is what we
            // wrap the elements in, matches the Qname of the parameter
            // element.
            Map partsMap =
                    operation.getOperation().getInput().getMessage().getParts();
            Iterator i = partsMap.values().iterator();
            QName q = null;
            while (q == null && i.hasNext()) {
            	Part p = (Part) i.next();
            	q = p.getElementName();
            }
            if(q != null) {
                pw.println("        _call.setOperationName(" + Utils.getNewQName(q)
                        + ");");
            } else {
                log.warn(Messages.getMessage("missingPartsForMessage00",operation.getOperation().getInput().getMessage().getQName().toString())); 
            }
        } else {
            QName elementQName = Utils.getOperationQName(operation, bEntry,
                    symbolTable);

            if (elementQName != null) {
                pw.println("        _call.setOperationName("
                        + Utils.getNewQName(elementQName) + ");");
            }
        }

        pw.println();

        // Set the headers
        pw.println("        setRequestHeaders(_call);");

        // Set the attachments
        pw.println("        setAttachments(_call);");

        // Set DIME flag if needed
        if (bEntry.isOperationDIME(operation.getOperation().getName())) {
            pw.println(
                    "        _call.setProperty(_call.ATTACHMENT_ENCAPSULATION_FORMAT, _call.ATTACHMENT_ENCAPSULATION_FORMAT_DIME);");
        }

        // Invoke the operation
        if (oneway) {
            pw.print("        _call.invokeOneWay(");
        } else {
            pw.print(" try {");
            pw.print("        java.lang.Object _resp = _call.invoke(");
        }

        pw.print("new java.lang.Object[] {");
        writeParameters(pw, parms);
        pw.println("});");
        pw.println();

        if (!oneway) {
            writeResponseHandling(pw, parms);
        }

        pw.println("    }");
        pw.println();
    }    // writeOperation

    /**
     * Method writeParameters
     * 
     * @param pw    
     * @param parms 
     */
    protected void writeParameters(PrintWriter pw, Parameters parms) {

        // Write the input and inout parameter list
        boolean needComma = false;

        for (int i = 0; i < parms.list.size(); ++i) {
            Parameter p = (Parameter) parms.list.get(i);

            if (p.getMode() != Parameter.OUT) {
                if (needComma) {
                    pw.print(", ");
                } else {
                    needComma = true;
                }

                String javifiedName = Utils.xmlNameToJava(p.getName());

                if (p.getMode() != Parameter.IN) {
                    javifiedName += ".value";
                }

                if (p.getMIMEInfo() == null && !p.isOmittable()) {
                    javifiedName = Utils.wrapPrimitiveType(p.getType(),
                            javifiedName);
                }

                pw.print(javifiedName);
            }
        }
    }    // writeParamters

    /**
     * Method writeResponseHandling
     * 
     * @param pw    
     * @param parms 
     */
    protected void writeResponseHandling(PrintWriter pw, Parameters parms) {

        pw.println("        if (_resp instanceof java.rmi.RemoteException) {");
        pw.println("            throw (java.rmi.RemoteException)_resp;");
        pw.println("        }");

        int allOuts = parms.outputs + parms.inouts;

        if (allOuts > 0) {
            pw.println("        else {");
            pw.println("            extractAttachments(_call);");

            if (allOuts == 1) {
                if (parms.returnParam != null) {
                    writeOutputAssign(pw, "return ",
                            parms.returnParam, "_resp");
                } else {

                    // The resp object must go into a holder
                    int i = 0;
                    Parameter p = (Parameter) parms.list.get(i);

                    while (p.getMode() == Parameter.IN) {
                        p = (Parameter) parms.list.get(++i);
                    }

                    String javifiedName = Utils.xmlNameToJava(p.getName());
                    String qnameName = Utils.getNewQNameWithLastLocalPart(p.getQName());

                    pw.println("            java.util.Map _output;");
                    pw.println(
                            "            _output = _call.getOutputParams();");
                    writeOutputAssign(pw,
                                      javifiedName + ".value = ",
                                      p,
                                      "_output.get(" + qnameName + ")");
                }
            } else {

                // There is more than 1 output.  Get the outputs from getOutputParams.
                pw.println("            java.util.Map _output;");
                pw.println("            _output = _call.getOutputParams();");

                for (int i = 0; i < parms.list.size(); ++i) {
                    Parameter p = (Parameter) parms.list.get(i);
                    String javifiedName = Utils.xmlNameToJava(p.getName());
                    String qnameName = 
                            Utils.getNewQNameWithLastLocalPart(p.getQName());

                    if (p.getMode() != Parameter.IN) {
                        writeOutputAssign(pw,
                                          javifiedName + ".value = ",
                                          p,
                                          "_output.get(" + qnameName + ")");
                    }
                }

                if (parms.returnParam != null) {
                    writeOutputAssign(pw,
                                      "return ", 
                                      parms.returnParam,
                                      "_resp");
                }
            }

            pw.println("        }");
        } else {
            pw.println("        extractAttachments(_call);");
        }
        // End catch
        // Get faults
        
        Map faults = parms.faults;
        // Get faults of signature
        List exceptionsThrowsList = new ArrayList();
        
        int index = parms.signature.indexOf("throws");
        if (index != -1) {
            String[] thrExcep = StringUtils.split(parms.signature.substring(index+6),',');
            for (int i = 0; i < thrExcep.length; i++) {
                exceptionsThrowsList.add(thrExcep[i].trim());
            }
        }
        pw.println("  } catch (org.apache.axis.AxisFault axisFaultException) {");
        if (faults != null && faults.size() > 0) {
            pw.println("    if (axisFaultException.detail != null) {");
            for (Iterator faultIt = exceptionsThrowsList.iterator(); faultIt
                    .hasNext();) {
                String exceptionFullName = (String) faultIt.next();
                pw.println("        if (axisFaultException.detail instanceof "
                        + exceptionFullName + ") {");
                pw.println("              throw (" + exceptionFullName
                        + ") axisFaultException.detail;");
                pw.println("         }");
            }
            pw.println("   }");
        }
        pw.println("  throw axisFaultException;");
        pw.println("}");
    }    // writeResponseHandling

    /**
     * writeOutputAssign
     * 
     * @param pw       
     * @param target   (either "return" or "something ="
     * @param source   (source String)
     */
    protected void writeOutputAssign(PrintWriter pw, String target,
                                   Parameter param,
                                   String source) {

        TypeEntry type = param.getType();
        
        if ((type != null) && (type.getName() != null)) {

            String typeName = type.getName();
            // If minOccurs="0" and singular or array with nillable underlying
            // type get the corresponding wrapper type.
            if ((param.isOmittable() && param.getType().getDimensions().equals(""))
                || (param.getType() instanceof CollectionType
                    && ((CollectionType) param.getType()).isWrapped())
                || param.getType().getUnderlTypeNillable()) {

                typeName = Utils.getWrapperType(type);
            }

            // Try casting the output to the expected output.
            // If that fails, use JavaUtils.convert()
            pw.println("            try {");
            pw.println("                " + target
                    + Utils.getResponseString(param, source));
            pw.println(
                    "            } catch (java.lang.Exception _exception) {");
            pw.println(
                    "                " + target
                    + Utils.getResponseString(param,
                            "org.apache.axis.utils.JavaUtils.convert(" +
                            source + ", " + typeName + ".class)"));
            pw.println("            }");
        } else {
            pw.println("              " + target
                    + Utils.getResponseString(param, source));
        }
    }
}    // class JavaStubWriter

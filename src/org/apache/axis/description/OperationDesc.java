/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
package org.apache.axis.description;

import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.commons.logging.Log;

import javax.xml.namespace.QName;
import javax.wsdl.OperationType;
import java.io.Serializable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;


/**
 * An OperationDesc is an abstract description of an operation on a service.
 *
 * !!! WORK IN PROGRESS
 *
 * @author Glen Daniels (gdaniels@apache.org)
 */
public class OperationDesc implements Serializable {
    // Constants for "message style" operation patterns.  If this OperationDesc
    // is message style, the Java method will have one of these signatures:

    // public SOAPBodyElement [] method(SOAPBodyElement [])
    public static final int MSG_METHOD_BODYARRAY = 1;
    // public void method(SOAPEnvelope, SOAPEnvelope)
    public static final int MSG_METHOD_SOAPENVELOPE = 2;
    // public Element [] method(Element [])
    public static final int MSG_METHOD_ELEMENTARRAY = 3;
    // public Document method(Document)
    public static final int MSG_METHOD_DOCUMENT = 4;

    public static final int MSG_METHOD_NONCONFORMING = -4;
    
    public static Map mepStrings = new HashMap();
    
    static {
        mepStrings.put("request-response", OperationType.REQUEST_RESPONSE);
        mepStrings.put("oneway", OperationType.ONE_WAY);
        mepStrings.put("solicit-response", OperationType.SOLICIT_RESPONSE);
        mepStrings.put("notification", OperationType.NOTIFICATION);
    }

    protected static Log log =
        LogFactory.getLog(OperationDesc.class.getName());

    /** The service we're a part of */
    private ServiceDesc parent;

    /** Parameter list */
    private ArrayList parameters = new ArrayList();

    /** The operation name (String, or QName?) */
    private String name;

    /** An XML QName which should dispatch to this method */
    private QName elementQName;

    /** The actual Java method associated with this operation, if known */
    private transient Method method;

    /** This operation's style/use. If null, we default to our parent's */
    private Style style = null;
    private Use   use = null;

    /** The number of "in" params (i.e. IN or INOUT) for this operation */
    private int numInParams = 0;
    /** The number of "out" params (i.e. OUT or INOUT) for this operation */
    private int numOutParams = 0;

    /** A unique SOAPAction value for this operation */
    private String soapAction = null;

    /** Faults for this operation */
    private ArrayList faults = null;

    private ParameterDesc returnDesc = new ParameterDesc();

    /** If we're a message-style operation, what's our signature? */
    private int messageOperationStyle = -1;

    /** The documentation for the operation */
	private String documentation = null;
    
    /** The MEP for this Operation - uses the WSDL4J OperationType for now
     * but we might want to have our own extensible enum for WSDL 2.0
     */ 
    private OperationType mep = OperationType.REQUEST_RESPONSE;

    /**
     * Default constructor.
     */
    public OperationDesc() {
        returnDesc.setMode(ParameterDesc.OUT);
        returnDesc.setIsReturn(true);
    }

    /**
     * "Complete" constructor
     */
    public OperationDesc(String name, ParameterDesc [] parameters, QName returnQName) {
        this.name = name;
        returnDesc.setQName(returnQName);
        returnDesc.setMode(ParameterDesc.OUT);
        returnDesc.setIsReturn(true);
        for (int i = 0; i < parameters.length; i++) {
            addParameter(parameters[i]);
        }
    }

    /**
     * Return the operation's name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the operation's name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * get the documentation for the operation
     */
	public String getDocumentation() {
    	return documentation; 
    }

    /**
     * set the documentation for the operation
     */
	public void setDocumentation(String documentation) {
    	this.documentation = documentation;
    }

    public QName getReturnQName() {
        return returnDesc.getQName();
    }

    public void setReturnQName(QName returnQName) {
        returnDesc.setQName(returnQName);
    }

    public QName getReturnType() {
        return returnDesc.getTypeQName();
    }

    public void setReturnType(QName returnType) {
        log.debug("@" + Integer.toHexString(hashCode())  + "setReturnType(" + returnType +")");
        returnDesc.setTypeQName(returnType);
    }

    public Class getReturnClass() {
        return returnDesc.getJavaType();
    }

    public void setReturnClass(Class returnClass) {
        returnDesc.setJavaType(returnClass);
    }

    public QName getElementQName() {
        return elementQName;
    }

    public void setElementQName(QName elementQName) {
        this.elementQName = elementQName;
    }

    public ServiceDesc getParent() {
        return parent;
    }

    public void setParent(ServiceDesc parent) {
        this.parent = parent;
    }

    public String getSoapAction() {
        return soapAction;
    }

    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    public void setStyle(Style style)
    {
        this.style = style;
    }

    /**
     * Return the style of the operation, defaulting to the parent
     * ServiceDesc's style if we don't have one explicitly set.
     */
    public Style getStyle()
    {
        if (style == null) {
            if (parent != null) {
                return parent.getStyle();
            }
            return Style.DEFAULT; // Default
        }

        return style;
    }

    public void setUse(Use use)
    {
        this.use = use;
    }

    /**
     * Return the use of the operation, defaulting to the parent
     * ServiceDesc's use if we don't have one explicitly set.
     */
    public Use getUse()
    {
        if (use == null) {
            if (parent != null) {
                return parent.getUse();
            }
            return Use.DEFAULT; // Default
        }

        return use;
    }

    public void addParameter(ParameterDesc param)
    {
        // Should we enforce adding INs then INOUTs then OUTs?

        param.setOrder(getNumParams());
        parameters.add(param);
        if ((param.getMode() == ParameterDesc.IN) ||
            (param.getMode() == ParameterDesc.INOUT)) {
            numInParams++;
        }
        if ((param.getMode() == ParameterDesc.OUT) ||
            (param.getMode() == ParameterDesc.INOUT)) {
            numOutParams++;
        }
       log.debug("@" + Integer.toHexString(hashCode())  + " added parameter >" + param + "@" + Integer.toHexString(param.hashCode()) + "<total parameters:" +getNumParams());
    }
    
    public void addParameter(QName paramName,
                             QName xmlType,
                             Class javaType,
                             byte parameterMode,
                             boolean inHeader,
                             boolean outHeader) {
        ParameterDesc param =
                new ParameterDesc(paramName, parameterMode, xmlType,
                                  javaType, inHeader, outHeader);
        addParameter(param);
    }

    public ParameterDesc getParameter(int i)
    {
        if (parameters.size() <= i)
            return null;

        return (ParameterDesc)parameters.get(i);
    }

    public ArrayList getParameters() {
        return parameters;
    }

    /**
     * Set the parameters wholesale.
     *
     * @param newParameters an ArrayList of ParameterDescs
     */
    public void setParameters(ArrayList newParameters) {
        parameters = new ArrayList(); //Keep numInParams correct.
        numInParams = 0;
        numOutParams = 0;
        
        for( java.util.ListIterator li= newParameters.listIterator();
             li.hasNext(); ){
            addParameter((ParameterDesc) li.next());
        }
    }
    
    public int getNumInParams() {
        return numInParams;
    }

    public int getNumOutParams() {
        return numOutParams;
    }

    public int getNumParams() {
        return parameters.size();
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    /**
     * Is the return value in the header of the response message?
     */
    public boolean isReturnHeader() {
        return returnDesc.isOutHeader();
    }

    /**
     * Set whether the return value is in the response message.
     */
    public void setReturnHeader(boolean value) {
        returnDesc.setOutHeader(value);
    }

    public ParameterDesc getParamByQName(QName qname)
    {
        for (Iterator i = parameters.iterator(); i.hasNext();) {
            ParameterDesc param = (ParameterDesc) i.next();
            if (param.getQName().equals(qname))
                return param;
        }

        return null;
    }

    public ParameterDesc getInputParamByQName(QName qname)
    {
        ParameterDesc param = null;

        param = getParamByQName(qname);

        if ((param == null) || (param.getMode() == ParameterDesc.OUT)) {
            param = null;
        }

        return param;
    }

    public ParameterDesc getOutputParamByQName(QName qname)
    {
        ParameterDesc param = null;

        for (Iterator i = parameters.iterator(); i.hasNext();) {
            ParameterDesc pnext = (ParameterDesc)i.next();
            if (pnext.getQName().equals(qname) &&
                    pnext.getMode() != ParameterDesc.IN) {
                param = pnext;
                break;
            }
        }

        if (param == null) {
            if (null == returnDesc.getQName() ){
                param= new ParameterDesc( returnDesc); //Create copy
                param.setQName(qname);
            }
            else if ( qname.equals(returnDesc.getQName())) {
                param = returnDesc;
            }
        }

        return param;
    }

    /**
     * Return a list of ALL "in" params (including INOUTs)
     * 
     * Note: if we were sure the order went IN->INOUT->OUT, we could optimize
     * this.
     * 
     * @return
     */ 
    public ArrayList getAllInParams() {
        ArrayList result = new ArrayList();
        for (Iterator i = parameters.iterator(); i.hasNext();) {
            ParameterDesc desc = (ParameterDesc) i.next();
            if (desc.getMode() != ParameterDesc.OUT) {
                result.add(desc);
            }
        }
        return result;
    }
    
    /**
     * Return a list of ALL "out" params (including INOUTs)
     * 
     * Note: if we were sure the order went IN->INOUT->OUT, we could optimize
     * this.
     * 
     * @return
     */ 
    public ArrayList getAllOutParams() {
        ArrayList result = new ArrayList();
        for (Iterator i = parameters.iterator(); i.hasNext();) {
            ParameterDesc desc = (ParameterDesc) i.next();
            if (desc.getMode() != ParameterDesc.IN) {
                result.add(desc);
            }
        }
        return result;        
    }
    /**
     * Returns an ordered list of out params (NOT inouts)
     */
    public ArrayList getOutParams() {
        ArrayList result = new ArrayList();
        for (Iterator i = parameters.iterator(); i.hasNext();) {
            ParameterDesc desc = (ParameterDesc) i.next();
            if (desc.getMode() == ParameterDesc.OUT) {
                result.add(desc);
            }
        }
        return result;
    }

    public void addFault(FaultDesc fault)
    {
        if (faults == null)
            faults = new ArrayList();
        faults.add(fault);
    }

    public ArrayList getFaults()
    {
        return faults;
    }
    
    /**
     * Returns the FaultDesc for the fault class given.
     * Returns null if not found.
     */ 
    public FaultDesc getFaultByClass(Class cls) {
        if (faults == null || cls == null) {
            return null;
        }

        while (cls != null) {
            // Check each class in the inheritance hierarchy, stopping at
            // java.* or javax.* classes.

            for (Iterator iterator = faults.iterator(); iterator.hasNext();) {
                FaultDesc desc = (FaultDesc) iterator.next();
                if (cls.getName().equals(desc.getClassName())) {
                    return desc;
                }
            }

            cls = cls.getSuperclass();
            if (cls != null && (cls.getName().startsWith("java.") ||
                    cls.getName().startsWith("javax."))) {
                cls = null;
            }
        }

        return null;
    }

    /**
     * Returns the FaultDesc for the fault class given.
     * Returns null if not found.
     */ 
    public FaultDesc getFaultByClass(Class cls, boolean checkParents) {
        if (checkParents) {
            return getFaultByClass(cls);
        } 
        
        if (faults == null || cls == null) {
            return null;
        }
        
        for (Iterator iterator = faults.iterator(); iterator.hasNext();) {
            FaultDesc desc = (FaultDesc) iterator.next();
            if (cls.getName().equals(desc.getClassName())) {
                return desc;
            }
        }
        
        return null;
    }

    /**
     * Returns the FaultDesc for a QName (which is typically found
     * in the details element of a SOAP fault).
     * Returns null if not found.
     */ 
    public FaultDesc getFaultByQName(QName qname) {
        if (faults != null) {
            for (Iterator iterator = faults.iterator(); iterator.hasNext();) {
                FaultDesc desc = (FaultDesc) iterator.next();
                if (qname.equals(desc.getQName())) {
                    return desc;
                }
            }
        }
        return null;
    }
     /**
     * Returns the FaultDesc for an XMLType. 
     * Returns null if not found.
     */ 
    public FaultDesc getFaultByXmlType(QName xmlType) {
        if (faults != null) {
            for (Iterator iterator = faults.iterator(); iterator.hasNext();) {
                FaultDesc desc = (FaultDesc) iterator.next();
                if (xmlType.equals(desc.getXmlType())) {
                    return desc;
                }
            }
        }
        return null;
    }
    public ParameterDesc getReturnParamDesc() {
        return returnDesc;
    }

    public String toString() {
        return toString("");
    }
    public String toString(String indent) {
        String text ="";
        text+=indent+"name:        " + getName() + "\n";
        text+=indent+"returnQName: " + getReturnQName() + "\n";
        text+=indent+"returnType:  " + getReturnType() + "\n";
        text+=indent+"returnClass: " + getReturnClass() + "\n";
        text+=indent+"elementQName:" + getElementQName() + "\n";
        text+=indent+"soapAction:  " + getSoapAction() + "\n";
        text+=indent+"style:       " + getStyle().getName() + "\n";
        text+=indent+"use:         " + getUse().getName() + "\n";
        text+=indent+"numInParams: " + getNumInParams() + "\n";
        text+=indent+"method:" + getMethod() + "\n";
        for (int i=0; i<parameters.size(); i++) {
            text+=indent+" ParameterDesc[" + i + "]:\n";
            text+=indent+ ((ParameterDesc)parameters.get(i)).toString("  ") + "\n";
        }
        if (faults != null) {
            for (int i=0; i<faults.size(); i++) {
                text+=indent+" FaultDesc[" + i + "]:\n";
                text+=indent+ ((FaultDesc)faults.get(i)).toString("  ") + "\n";
            }
        }
        return text;
    }

    public int getMessageOperationStyle() {
        return messageOperationStyle;
    }

    public void setMessageOperationStyle(int messageOperationStyle) {
        this.messageOperationStyle = messageOperationStyle;
    }

    public OperationType getMep() {
        return mep;
    }

    public void setMep(OperationType mep) {
        this.mep = mep;
    }
    
    /**
     * Set the MEP using a string like "request-response"
     * @param mepString
     */ 
    public void setMep(String mepString) {
        OperationType newMep = (OperationType)mepStrings.get(mepString);
        if (newMep != null) {
            mep = newMep;
        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (method != null){
            out.writeObject(method.getDeclaringClass());
            out.writeObject(method.getName());
            out.writeObject(method.getParameterTypes());
        } else {
            out.writeObject(null);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
        in.defaultReadObject();
        Class clazz = (Class) in.readObject();
        if (clazz != null){
            String methodName = (String) in.readObject();
            Class[] parameterTypes = (Class[]) in.readObject();
            try {
                method = clazz.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                throw new IOException("Unable to deserialize the operation's method: "+ methodName);
            }
        }
    }
}


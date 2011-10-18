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
package org.apache.axis.message;

import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.description.ParameterDesc;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.utils.JavaUtils;
import org.apache.axis.utils.Messages;
import org.apache.axis.constants.Style;
import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.commons.logging.Log;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** An RPC parameter
 *
 * @author Glen Daniels (gdaniels@apache.org)
 */
public class RPCParam extends MessageElement implements Serializable
{
    protected static Log log =
        LogFactory.getLog(RPCParam.class.getName());

    private Object value = null;
    private int countSetCalls = 0; // counts number of calls to set

    private ParameterDesc paramDesc;

    /**
     * Do we definitely want (or don't want) to send xsi:types?  If null
     * (the default), just do whatever our SerializationContext is configured
     * to do.  If TRUE or FALSE, the SerializationContext will do what we
     * want.
     */
    private Boolean wantXSIType = null;

    private static Method valueSetMethod;
    static {
        Class cls = RPCParam.class;
        try {
            valueSetMethod = cls.getMethod("set", new Class[] {Object.class});
        } catch (NoSuchMethodException e) {
            log.error(Messages.getMessage("noValue00", "" + e));
            throw new RuntimeException(e.getMessage());
        }
    }

    /** Constructor for building up messages.
     */
    public RPCParam(String name, Object value)
    {
        this(new QName("", name), value);
    }

    public RPCParam(QName qname, Object value)
    {
        super(qname);
        if (value instanceof java.lang.String) {
            try {
                this.addTextNode((String) value);
            } catch (SOAPException e) {
                throw new RuntimeException(Messages.getMessage("cannotCreateTextNode00"));
            } 
        } else {
            this.value = value;
        }
    }

    public RPCParam(String namespace, String name, Object value)
    {
        this(new QName(namespace, name), value);
    }
    
    public void setRPCCall(RPCElement call)
    {
        parent = call;
    }
    
    public Object getObjectValue()
    {
        return value;
    }
    
    public void setObjectValue(Object value)
    {
        this.value = value;
    }

    /**
     * This set method is registered during deserialization
     * to set the deserialized value.
     * If the method is called multiple times, the 
     * value is automatically changed into a container to 
     * hold all of the values.
     * @param newValue is the deserialized object
     */
    public void set(Object newValue) {
        countSetCalls++;
        // If this is the first call,
        // simply set the value.
        if (countSetCalls==1) {
            this.value = newValue;
            return;
        }
        // If this is the second call, create an
        // ArrayList to hold all the values
        else if (countSetCalls==2) {
            ArrayList list = new ArrayList();
            list.add(this.value);
            this.value = list;
        } 
        // Add the new value to the list
        ((ArrayList) this.value).add(newValue);
    }

    public static Method getValueSetMethod()
    {
        return valueSetMethod;
    }

    public ParameterDesc getParamDesc() {
        return paramDesc;
    }

    public void setParamDesc(ParameterDesc paramDesc) {
        this.paramDesc = paramDesc;
    }

    public void setXSITypeGeneration(Boolean value) {
        this.wantXSIType = value; 
    }

    public Boolean getXSITypeGeneration() {
        return this.wantXSIType;
    }

    public void serialize(SerializationContext context)
        throws IOException
    {
        // Set the javaType to value's class unless 
        // parameter description information exists.
        // Set the xmlType using the parameter description
        // information.  (an xmlType=null causes the
        // serialize method to search for a compatible xmlType)
        Class javaType = value == null ? null: value.getClass();
        QName xmlType = null;
        // we'll send a null unless our description tells us
        // that we may be omitted
        Boolean sendNull = Boolean.TRUE;
        if (paramDesc != null) {
            if (javaType == null) {
                javaType = paramDesc.getJavaType() != null ?
                    paramDesc.getJavaType(): javaType;
            } else if (!(javaType.equals(paramDesc.getJavaType()))) {
                Class clazz = JavaUtils.getPrimitiveClass(javaType);
                if(clazz == null || !clazz.equals(paramDesc.getJavaType())) {
                    if (!(javaType.equals(
                            JavaUtils.getHolderValueType(paramDesc.getJavaType())))) {

                        // This must (assumedly) be a polymorphic type - in ALL
                        // such cases, we must send an xsi:type attribute.
                        wantXSIType = Boolean.TRUE;
                    }
                }
            }
            xmlType = paramDesc.getTypeQName();
            QName itemQName = paramDesc.getItemQName();
            if (itemQName == null) {
                MessageContext mc = context.getMessageContext();
                if (mc != null && mc.getOperation() != null && mc.getOperation().getStyle() == Style.DOCUMENT) {
                    itemQName = Constants.QNAME_LITERAL_ITEM;
                }
            }
            context.setItemQName(itemQName);

            QName itemType = paramDesc.getItemType();
            context.setItemType(itemType);

            // don't send anything if we're able to be omitted,
            // although we'll prefer to send xsi:nill if possible
            if (paramDesc.isOmittable() && !paramDesc.isNillable())
                sendNull = Boolean.FALSE;
        }
        context.serialize(getQName(),  // element qname
                          null,   // no extra attrs
                          value,  // value
                          xmlType, // java/xml type
                          sendNull, wantXSIType);
    }

    private void writeObject(ObjectOutputStream out)
        throws IOException {
        if (getQName() == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeObject(getQName().getNamespaceURI());
            out.writeObject(getQName().getLocalPart());
        }
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) 
        throws IOException, ClassNotFoundException {
        if (in.readBoolean()) {
            setQName(new QName((String)in.readObject(),
                              (String)in.readObject()));
        } 
        in.defaultReadObject();
    }

    protected void outputImpl(SerializationContext context) throws Exception {
        serialize(context);
    }

    public String getValue() {
        return getValueDOM();
    }

    /**
     * @see javax.xml.soap.SOAPElement#addTextNode(java.lang.String)
     */
    public SOAPElement addTextNode(String s) throws SOAPException {
        value = s;
        return super.addTextNode(s);
    }
    /**
     * @see javax.xml.soap.Node#setValue(java.lang.String)
     */
    public void setValue(String value) {
        this.value = value;
        super.setValue(value);
    }
}

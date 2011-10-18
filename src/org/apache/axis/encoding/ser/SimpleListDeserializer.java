/*
 * Copyright 2001,2004 The Apache Software Foundation.
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

package org.apache.axis.encoding.ser;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

import org.apache.axis.description.TypeDesc;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.Deserializer;
import org.apache.axis.encoding.SimpleType;
import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.message.SOAPHandler;
import org.apache.axis.utils.BeanPropertyDescriptor;
import org.apache.axis.utils.Messages;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Deserializer for 
 * <xsd:simpleType ...>
 *   <xsd:list itemType="...">
 * </xsd:simpleType>
 * based on SimpleDeserializer
 *
 * @author Ias (iasandcb@tmax.co.kr)
 */
public class SimpleListDeserializer extends SimpleDeserializer {

    StringBuffer val = new StringBuffer();
    private Constructor constructor = null;
    private Map propertyMap = null;
    private HashMap attributeMap = null;
    private DeserializationContext context = null;

    public QName xmlType;
    public Class javaType;

    private TypeDesc typeDesc = null;

    protected SimpleListDeserializer cacheStringDSer = null;
    protected QName cacheXMLType = null;
    /**
     * The Deserializer is constructed with the xmlType and
     * javaType (which could be a java primitive like int.class)
     */
    public SimpleListDeserializer(Class javaType, QName xmlType) {
        super (javaType, xmlType);

        this.xmlType = xmlType;
        this.javaType = javaType;
    }
    public SimpleListDeserializer(Class javaType, QName xmlType, TypeDesc typeDesc) {
        super (javaType, xmlType, typeDesc);

        this.xmlType = xmlType;
        this.javaType = javaType;
        this.typeDesc = typeDesc;
    }
   
    
    /**
     * Reset deserializer for re-use
     */
    public void reset() {
        val.setLength(0); // Reset string buffer back to zero
        attributeMap = null; // Remove attribute map
        isNil = false; // Don't know if nil
        isEnded = false; // Indicate the end of element not yet called
    }

    /**
     * The Factory calls setConstructor.
     */
    public void setConstructor(Constructor c)
    {
        constructor = c;
    }

    /**
     * There should not be nested elements, so thow and exception if this occurs.
     */
    public SOAPHandler onStartChild(String namespace,
                                    String localName,
                                    String prefix,
                                    Attributes attributes,
                                    DeserializationContext context)
        throws SAXException
    {
        throw new SAXException(
                Messages.getMessage("cantHandle00", "SimpleDeserializer"));
    }

    /**
     * Append any characters received to the value.  This method is defined
     * by Deserializer.
     */
    public void characters(char [] chars, int start, int end)
        throws SAXException
    {
        val.append(chars, start, end);
    }

    /**
     * Append any characters to the value.  This method is defined by
     * Deserializer.
     */
    public void onEndElement(String namespace, String localName,
                           DeserializationContext context)
        throws SAXException
    {
        if (isNil || val == null) {
            value = null;
            return;
        }
        try {
            value = makeValue(val.toString());
        } catch (InvocationTargetException ite) {
            Throwable realException = ite.getTargetException();
            if (realException instanceof Exception)
               throw new SAXException((Exception)realException);
            else
               throw new SAXException(ite.getMessage());
        } catch (Exception e) {
            throw new SAXException(e);
        }

        // If this is a SimpleType, set attributes we have stashed away
        setSimpleTypeAttributes();
    }

    /**
     * Convert the string that has been accumulated into an Object.  Subclasses
     * may override this. 
     * @param source the serialized value to be deserialized
     * @throws Exception any exception thrown by this method will be wrapped
     */
    public Object makeValue(String source) throws Exception
    {
        // According to XML Schema Spec Part 0: Primer 2.3.1 - white space delimitor
        StringTokenizer tokenizer = new StringTokenizer(source.trim());
        int length = tokenizer.countTokens();
        Object list = Array.newInstance(javaType, length);
        for (int i = 0; i < length; i++) {
            String token = tokenizer.nextToken();
            Array.set(list, i, makeUnitValue(token));
        }
        return list;
    }

    private Object makeUnitValue(String source) throws Exception
    {
        // If the javaType is a boolean, except a number of different sources
        if (javaType == boolean.class || javaType == Boolean.class) {
            // This is a pretty lame test, but it is what the previous code did.
            switch (source.charAt(0)) {
                case '0': case 'f': case 'F':
                    return Boolean.FALSE;
                    
                   case '1': case 't': case 'T':
                       return Boolean.TRUE;
                       
                      default:
                          throw new NumberFormatException(
                                  Messages.getMessage("badBool00"));
            }
            
        }
        
        // If expecting a Float or a Double, need to accept some special cases.
        if (javaType == float.class ||
                javaType == java.lang.Float.class) {
            if (source.equals("NaN")) {
                return new Float(Float.NaN);
            } else if (source.equals("INF")) {
                return new Float(Float.POSITIVE_INFINITY);
            } else if (source.equals("-INF")) {
                return new Float(Float.NEGATIVE_INFINITY);
            }
        }
        if (javaType == double.class ||
                javaType == java.lang.Double.class) {
            if (source.equals("NaN")) {
                return new Double(Double.NaN);
            } else if (source.equals("INF")) {
                return new Double(Double.POSITIVE_INFINITY);
            } else if (source.equals("-INF")) {
                return new Double(Double.NEGATIVE_INFINITY);
            }
        }
        if (javaType == QName.class) {
            int colon = source.lastIndexOf(":");
            String namespace = colon < 0 ? "" :
                context.getNamespaceURI(source.substring(0, colon));
            String localPart = colon < 0 ? source : 
                source.substring(colon + 1);
            return new QName(namespace, localPart);
        }

        return constructor.newInstance(new Object [] { source });
    }
    /**
     * Set the bean properties that correspond to element attributes.
     *
     * This method is invoked after startElement when the element requires
     * deserialization (i.e. the element is not an href and the value is not nil.)
     * @param namespace is the namespace of the element
     * @param localName is the name of the element
     * @param prefix is the prefix of the element
     * @param attributes are the attributes on the element...used to get the type
     * @param context is the DeserializationContext
     */
    public void onStartElement(String namespace, String localName,
                               String prefix, Attributes attributes,
                               DeserializationContext context)
            throws SAXException
    {

        this.context = context;

        // If we have no metadata, we have no attributes.  Q.E.D.
        if (typeDesc == null)
            return;

        // loop through the attributes and set bean properties that
        // correspond to attributes
        for (int i=0; i < attributes.getLength(); i++) {
            QName attrQName = new QName(attributes.getURI(i),
                                        attributes.getLocalName(i));
            String fieldName = typeDesc.getFieldNameForAttribute(attrQName);
            if (fieldName == null)
                continue;

            // look for the attribute property
            BeanPropertyDescriptor bpd =
                    (BeanPropertyDescriptor) propertyMap.get(fieldName);
            if (bpd != null) {
                if (!bpd.isWriteable() || bpd.isIndexed() ) continue ;

                // determine the QName for this child element
                TypeMapping tm = context.getTypeMapping();
                Class type = bpd.getType();
                QName qn = tm.getTypeQName(type);
                if (qn == null)
                    throw new SAXException(
                            Messages.getMessage("unregistered00", type.toString()));

                // get the deserializer
                Deserializer dSer = context.getDeserializerForType(qn);
                if (dSer == null)
                    throw new SAXException(
                            Messages.getMessage("noDeser00", type.toString()));
                if (! (dSer instanceof SimpleListDeserializer))
                    throw new SAXException(
                            Messages.getMessage("AttrNotSimpleType00",
                                                 bpd.getName(),
                                                 type.toString()));

                // Success!  Create an object from the string and save
                // it in our attribute map for later.
                if (attributeMap == null) {
                    attributeMap = new HashMap();
                }
                try {
                    Object val = ((SimpleListDeserializer)dSer).
                        makeValue(attributes.getValue(i));
                    attributeMap.put(fieldName, val);
                } catch (Exception e) {
                    throw new SAXException(e);
                }
            } // if
        } // attribute loop
    } // onStartElement

    /**
     * Process any attributes we may have encountered (in onStartElement)
     */
    private void setSimpleTypeAttributes() throws SAXException {
        // if this isn't a simpleType bean, wont have attributes
        if (! SimpleType.class.isAssignableFrom(javaType) ||
            attributeMap == null)
            return;

        // loop through map
        Set entries = attributeMap.entrySet();
        for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object val = entry.getValue();

            BeanPropertyDescriptor bpd =
                    (BeanPropertyDescriptor) propertyMap.get(name);
            if (!bpd.isWriteable() || bpd.isIndexed()) continue;
            try {
                bpd.set(value, val );
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
    }

}

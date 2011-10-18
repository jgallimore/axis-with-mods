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

package org.apache.axis.encoding.ser;

import org.apache.axis.Constants;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.Deserializer;
import org.apache.axis.encoding.DeserializerImpl;
import org.apache.axis.encoding.DeserializerTarget;
import org.apache.axis.message.SOAPHandler;
import org.apache.axis.utils.ClassUtils;
import org.apache.axis.utils.JavaUtils;
import org.apache.axis.utils.Messages;
import org.apache.axis.wsdl.symbolTable.SchemaUtils;
import org.apache.commons.logging.Log;
import org.apache.axis.soap.SOAPConstants;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;


/**
 * An ArrayDeserializer handles deserializing SOAP
 * arrays.
 * 
 * Some code borrowed from ApacheSOAP - thanks to Matt Duftler!
 * 
 * @author Glen Daniels (gdaniels@apache.org)
 * 
 * Multi-reference stuff:
 * @author Rich Scheuerle (scheu@us.ibm.com)
 */
public class ArrayDeserializer extends DeserializerImpl
{
    protected static Log log =
        LogFactory.getLog(ArrayDeserializer.class.getName());

    public QName arrayType = null;
    public int curIndex = 0;
    QName defaultItemType;
    int length;
    Class arrayClass = null;
    ArrayList mDimLength = null;  // If set, array of multi-dim lengths 
    ArrayList mDimFactor = null;  // If set, array of factors for multi-dim []
    SOAPConstants soapConstants = SOAPConstants.SOAP11_CONSTANTS;

    /**
     * This method is invoked after startElement when the element requires
     * deserialization (i.e. the element is not an href & the value is not nil)
     * DeserializerImpl provides default behavior, which simply
     * involves obtaining a correct Deserializer and plugging its handler.
     * @param namespace is the namespace of the element
     * @param localName is the name of the element
     * @param prefix is the prefix of the element
     * @param attributes are the attrs on the element...used to get the type
     * @param context is the DeserializationContext
     */
    public void onStartElement(String namespace, String localName,
                             String prefix, Attributes attributes,
                             DeserializationContext context)
        throws SAXException
    {
        // Deserializing the xml array requires processing the
        // xsi:type= attribute, the soapenc:arrayType attribute,
        // and the xsi:type attributes of the individual elements.
        //
        // The xsi:type=<qName> attribute is used to determine the java
        // type of the array to instantiate.  Axis expects it
        // to be set to the generic "soapenc:Array" or to
        // a specific qName.  If the generic "soapenc:Array"
        // specification is used, Axis determines the array
        // type by examining the soapenc:arrayType attribute.
        //
        // The soapenc:arrayType=<qname><dims> is used to determine
        // i) the number of dimensions, 
        // ii) the length of each dimension,
        // iii) the default xsi:type of each of the elements.
        //
        // If the arrayType attribute is missing, Axis assumes
        // a single dimension array with length equal to the number
        // of nested elements.  In such cases, the default xsi:type of 
        // the elements is determined using the array xsi:type.
        //
        // The xsi:type attributes of the individual elements of the
        // array are used to determine the java type of the element.
        // If the xsi:type attribute is missing for an element, the 
        // default xsi:type value is used.

        if (log.isDebugEnabled()) {
            log.debug("Enter: ArrayDeserializer::startElement()");
        }

        soapConstants = context.getSOAPConstants();

        // Get the qname for the array type=, set it to null if
        // the generic type is used.
        QName typeQName = context.getTypeFromAttributes(namespace,
                                                        localName,
                                                        attributes);
        if (typeQName == null) {
            typeQName = getDefaultType();
        }

        if (typeQName != null &&
            Constants.equals(Constants.SOAP_ARRAY, typeQName)) {
            typeQName = null;
        }

        // Now get the arrayType value
        QName arrayTypeValue = context.getQNameFromString(
                      Constants.getValue(attributes,
                                         Constants.URIS_SOAP_ENC,
                                         soapConstants.getAttrItemType()));

        // The first part of the arrayType expression is 
        // the default item type qname.
        // The second part is the dimension information
        String dimString = null;
        QName innerQName = null;
        String innerDimString = "";
        if (arrayTypeValue != null) {
            if (soapConstants != SOAPConstants.SOAP12_CONSTANTS) {
                // Doing SOAP 1.1
                // Array dimension noted like this : [][x]
                String arrayTypeValueNamespaceURI =
                    arrayTypeValue.getNamespaceURI();
                String arrayTypeValueLocalPart =
                    arrayTypeValue.getLocalPart();

                int leftBracketIndex =
                    arrayTypeValueLocalPart.lastIndexOf('[');
                int rightBracketIndex =
                    arrayTypeValueLocalPart.lastIndexOf(']');
                if (leftBracketIndex == -1
                    || rightBracketIndex == -1
                    || rightBracketIndex < leftBracketIndex) {
                        throw new IllegalArgumentException(
                          Messages.getMessage("badArrayType00",
                                               "" + arrayTypeValue));
                }

                dimString =
                    arrayTypeValueLocalPart.substring(leftBracketIndex + 1,
                                                      rightBracketIndex);
                arrayTypeValueLocalPart =
                    arrayTypeValueLocalPart.substring(0, leftBracketIndex);

                // If multi-dim array set to soapenc:Array
                if (arrayTypeValueLocalPart.endsWith("]")) {
                    defaultItemType = Constants.SOAP_ARRAY;
                    int bracket = arrayTypeValueLocalPart.indexOf("[");
                    innerQName = new QName(arrayTypeValueNamespaceURI,
                                           arrayTypeValueLocalPart.substring(0, bracket));
                    innerDimString = arrayTypeValueLocalPart.substring(bracket);
                } else {
                    defaultItemType = new QName(arrayTypeValueNamespaceURI,
                                                arrayTypeValueLocalPart);
                }

            } else {
                String arraySizeValue = attributes.getValue(soapConstants.getEncodingURI(), Constants.ATTR_ARRAY_SIZE);
                int leftStarIndex = arraySizeValue.lastIndexOf('*');

                // Skip to num if any
                if (leftStarIndex != -1) {
                    // "*" => ""
                    if (leftStarIndex == 0 && arraySizeValue.length() == 1) {
                        // "* *" => ""
                    } else if (leftStarIndex == (arraySizeValue.length() - 1)) {
                        throw new IllegalArgumentException(
                          Messages.getMessage("badArraySize00",
                                               "" + arraySizeValue));
                        // "* N" => "N"
                    } else {
                        dimString = arraySizeValue.substring(leftStarIndex + 2);
                        innerQName = arrayTypeValue;
                        innerDimString = arraySizeValue.substring(0, leftStarIndex + 1);
                    }
                } else {
                    dimString = arraySizeValue;
                }

                if (innerDimString == null || innerDimString.length() == 0) {
                    defaultItemType = arrayTypeValue;
                } else {
                    defaultItemType = Constants.SOAP_ARRAY12;
                }
            }
        }

        // If no type QName and no defaultItemType qname, use xsd:anyType
        if (defaultItemType == null && typeQName == null) {
            Class destClass = context.getDestinationClass();
            if (destClass != null && destClass.isArray()) {
                // This will get set OK down below...
            } else {
                defaultItemType = Constants.XSD_ANYTYPE;
            }
        }
        
        // Determine the class type for the array.
        arrayClass = null;
        if (typeQName != null) {
            arrayClass = context.getTypeMapping().
                getClassForQName(typeQName);
        } 
        
        if (typeQName == null || arrayClass == null) {
            // type= information is not sufficient.
            // Get an array of the default item type.
            Class arrayItemClass = null;
            QName compQName = defaultItemType;

            // Nested array, use the innermost qname
            String dims = "[]";
            if (innerQName != null) {
                compQName = innerQName;

                if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
                    // With SOAP 1.2 Array, we append [] for each * found
                    int offset = 0;
                    while ((offset = innerDimString.indexOf('*', offset)) != -1) {
                        dims += "[]";
                        offset ++;
                    }
                } else {
                    // With SOAP 1.1 Array, we can append directly the complete innerDimString
                    dims += innerDimString;
                }
            }

            // item Class
            arrayItemClass = context.getTypeMapping().getClassForQName(compQName);
            if (arrayItemClass != null) {
                try {
                    // Append the dimension found to the classname computed from the itemClass
                    // to form the array classname
                    //
                    String loadableArrayClassName = JavaUtils.getLoadableClassName(
                            JavaUtils.getTextClassName(arrayItemClass.getName()) + dims);
                    arrayClass = ClassUtils.forName(loadableArrayClassName,
                                                    true,
                                                    arrayItemClass.getClassLoader());
                } catch (Exception e) {
                    throw new SAXException(
                            Messages.getMessage("noComponent00",
                                                "" + defaultItemType));
                }
            }
        }
        if (arrayClass == null) {
            arrayClass = context.getDestinationClass();
        }

        if (arrayClass == null) {
            throw new SAXException(
               Messages.getMessage("noComponent00",  "" + defaultItemType));
        }

        if (dimString == null || dimString.length() == 0) {
            // Size determined using length of the members
            value = new ArrayListExtension(arrayClass);
        } else {
            try
            {
                StringTokenizer tokenizer;
                if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
                    tokenizer = new StringTokenizer(dimString);
                } else {
                    tokenizer = new StringTokenizer(dimString, "[],");
                }

                length = Integer.parseInt(tokenizer.nextToken());
                if (tokenizer.hasMoreTokens()) {
                        // If the array is passed as a multi-dimensional array
                        // (i.e. int[2][3]) then store all of the 
                        // mult-dim lengths.
                        // The valueReady method uses this array to set the
                        // proper mult-dim element.
                        mDimLength = new ArrayList();
                        mDimLength.add(new Integer(length));
                        
                        while(tokenizer.hasMoreTokens()) {
                            mDimLength.add(
                                new Integer(
                                    Integer.parseInt(tokenizer.nextToken())));
                        }
                    }

                // Create an ArrayListExtension class to store the ArrayList
                // plus converted objects.
                ArrayList list = new ArrayListExtension(arrayClass, length);

                // This is expensive as our array may not grown this big.
                // Prevents problems when XML claims a huge size
                // that it doesn't actually fill.
                //for (int i = 0; i < length; i++) {
                //    list.add(null);
                //}
                value = list;

            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException(
                        Messages.getMessage("badInteger00", dimString));
            }
        }

        // If soapenc:offset specified, set the current index accordingly
        String offset = Constants.getValue(attributes,
                                         Constants.URIS_SOAP_ENC,
                                         Constants.ATTR_OFFSET);
        if (offset != null) {
            if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
                throw new SAXException(Messages.getMessage("noSparseArray"));
            }

            int leftBracketIndex = offset.lastIndexOf('[');
            int rightBracketIndex = offset.lastIndexOf(']');

            if (leftBracketIndex == -1
                || rightBracketIndex == -1
                || rightBracketIndex < leftBracketIndex)
            {
                throw new SAXException(
                        Messages.getMessage("badOffset00", offset));
            }

            curIndex =
                convertToIndex(offset.substring(leftBracketIndex + 1,
                                                rightBracketIndex),
                               "badOffset00");
        }

        if (log.isDebugEnabled()) {
            log.debug("Exit: ArrayDeserializer::startElement()");
        }
    }


    /**
     * onStartChild is called on each child element.
     * @param namespace is the namespace of the child element
     * @param localName is the local name of the child element
     * @param prefix is the prefix used on the name of the child element
     * @param attributes are the attributes of the child element
     * @param context is the deserialization context.
     * @return is a Deserializer to use to deserialize a child (must be
     * a derived class of SOAPHandler) or null if no deserialization should
     * be performed.
     */
    public SOAPHandler onStartChild(String namespace,
                                    String localName,
                                    String prefix,
                                    Attributes attributes,
                                    DeserializationContext context)
        throws SAXException
    {
        if (log.isDebugEnabled()) {
            log.debug("Enter: ArrayDeserializer.onStartChild()");
        }

        // If the position attribute is set, 
        // use it to update the current index
        if (attributes != null) {
            String pos =
                Constants.getValue(attributes,
                                   Constants.URIS_SOAP_ENC,
                                   Constants.ATTR_POSITION);
            if (pos != null) {
                if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
                    throw new SAXException(Messages.getMessage("noSparseArray"));
                }

                int leftBracketIndex = pos.lastIndexOf('[');
                int rightBracketIndex = pos.lastIndexOf(']');

                if (leftBracketIndex == -1
                    || rightBracketIndex == -1
                    || rightBracketIndex < leftBracketIndex)
                {
                    throw new SAXException(
                            Messages.getMessage("badPosition00", pos));
                }
                
                curIndex = 
                    convertToIndex(pos.substring(leftBracketIndex + 1,
                                                 rightBracketIndex),
                                   "badPosition00");
            }

            // If the xsi:nil attribute, set the value to null 
            // and return since there is nothing to deserialize.
            if (context.isNil(attributes)) {
                setChildValue(null, new Integer(curIndex++));
                return null;
            }
        }

        // Use the xsi:type setting on the attribute if it exists.
        QName itemType = context.getTypeFromAttributes(namespace,
                                                       localName,
                                                       attributes);

        // Get the deserializer for the type. 
        Deserializer dSer = null;
        if (itemType != null && (context.getCurElement().getHref() == null)) {
            dSer = context.getDeserializerForType(itemType);
        }

        if (dSer == null) {
            // No deserializer can be found directly.  Need to look harder
            QName defaultType = defaultItemType;
            Class javaType = null;
            if (arrayClass != null &&
                arrayClass.isArray() &&
                defaultType == null) {
                javaType = arrayClass.getComponentType();
                defaultType = context.getTypeMapping().getTypeQName(javaType);
            }

            // We don't have a deserializer, the safest thing to do
            // is to set up using the DeserializerImpl below.  
            // The DeserializerImpl will take care of href/id and
            // install the appropriate serializer, etc.  The problem 
            // is that takes a lot of time and will occur 
            // all the time if no xsi:types are sent.  Most of the
            // time an item is a simple schema type (i.e. String)
            // so the following shortcut is used to get a Deserializer
            // for these cases. 
            if (itemType == null && dSer == null) {
                if (defaultType != null && SchemaUtils.isSimpleSchemaType(defaultType)) {
                    dSer = context.getDeserializer(javaType, defaultType);
                }
            }
            
            // If no deserializer is 
            // found, the deserializer is set to DeserializerImpl().
            // It is possible that the element has an href, thus we
            // won't know the type until the definitition is encountered.
            if (dSer == null) {
                dSer = new DeserializerImpl();
                // Determine a default type for the deserializer
                if (itemType == null) {
                    dSer.setDefaultType(defaultType);
                }
            }
        }


        // Register the callback value target, and
        // keep track of this index so we know when it has been set.
        dSer.registerValueTarget(
            new DeserializerTarget(this, new Integer(curIndex)));
        
        // The framework handles knowing when the value is complete, as
        // long as we tell it about each child we're waiting on...
        addChildDeserializer(dSer);

        curIndex++;

        // In case of multi-array, we need to specify the destination class
        // of the children elements of this element array deserializer.
        context.setDestinationClass(arrayClass.getComponentType());
        
        if (log.isDebugEnabled()) {
            log.debug("Exit: ArrayDeserializer.onStartChild()");
        }
        
        return (SOAPHandler)dSer;
    }

    public void onEndChild(String namespace, String localName, DeserializationContext context) throws SAXException {
        // reverse onStartChild operation.
        context.setDestinationClass(arrayClass);
    }

    public void characters(char[] chars, int i, int i1) throws SAXException {
        for (int idx = i; i < i1; i++) {
            if (!Character.isWhitespace(chars[idx]))
                throw new SAXException(Messages.getMessage("charsInArray"));            
        }
    }

    /**
     * set is called during deserialization to assign
     * the Object value to the array position indicated by hint.
     * The hint is always a single Integer.  If the array being
     * deserialized is a multi-dimensional array, the hint is 
     * converted into a series of indices to set the correct
     * nested position.
     * The array deserializer always deserializes into
     * an ArrayList, which is converted and copied into the
     * actual array after completion (by valueComplete).
     * It is important to wait until all indices have been 
     * processed before invoking valueComplete.
     * @param value value of the array element
     * @param hint index of the array element (Integer)
     **/
    public void setChildValue(Object value, Object hint) throws SAXException
    { 
        if (log.isDebugEnabled()) {
            log.debug("Enter: ArrayDeserializer::setValue(" + value + ", " + hint + ")");
        }
        ArrayList list = (ArrayList)this.value;
        int offset = ((Integer)hint).intValue();

        if (this.mDimLength == null) {
            // Normal Case: Set the element in the list
            // grow the list if necessary to accomodate the new member
            while (list.size() <= offset) {
                list.add(null);
            }

            list.set(offset, value);
        } else {
            // Multi-Dim Array case:  Need to find the nested ArrayList
            // and set the proper element.

            // Convert the offset into a series of indices
            ArrayList mDimIndex = toMultiIndex(offset);

            // Get/Create the nested ArrayList
            for(int i=0; i < mDimLength.size(); i++) {
                int length = ((Integer)mDimLength.get(i)).intValue();
                int index  = ((Integer)mDimIndex.get(i)).intValue();
                while (list.size() < length) {
                    list.add(null);
                }
                // If not the last dimension, get the nested ArrayList
                // Else set the value
                if (i < mDimLength.size()-1) {
                    if (list.get(index) == null) {
                        list.set(index, new ArrayList());
                    }
                    list = (ArrayList) list.get(index);                    
                } else {
                    list.set(index, value);
                }
            }
        }
    }

    /**
     * When valueComplete() is invoked on the array, 
     * first convert the array value into the expected array.
     * Then call super.valueComplete() to inform referents
     * that the array value is ready.
     **/
    public void valueComplete() throws SAXException
    { 
        if (componentsReady()) {
           try {
                if (arrayClass != null) {
                    value = JavaUtils.convert(value, arrayClass);
                } 
           } catch (RuntimeException e) {
               // We must ignore exceptions from convert for Arrays with null - why?
           }
        }     
        
        super.valueComplete();
    }

    /**
     * Converts the given string to an index.
     * Assumes the string consists of a brackets surrounding comma 
     * separated digits.  For example "[2]" or [2,3]".
     * The routine returns a single index.
     * For example "[2]" returns 2.
     * For example "[2,3]" depends on the size of the multiple dimensions.
     *   if the dimensions are "[3,5]" then 13 is returned (2*5) + 3.
     * @param text representing index text
     * @param exceptKey exception message key
     * @return index 
     */
    private int convertToIndex(String text, String exceptKey)
        throws SAXException {
        StringTokenizer tokenizer = new StringTokenizer(text, "[],");
        int index = 0;
        try {
            if (mDimLength == null) {
                // Normal Case: Single dimension
                index = Integer.parseInt(tokenizer.nextToken());
                if (tokenizer.hasMoreTokens()) {
                    throw new SAXException(
                        Messages.getMessage(exceptKey, text));
                }
            }
            else {
                // Multiple Dimensions: 
                int dim = -1;
                ArrayList work = new ArrayList();
                while(tokenizer.hasMoreTokens()) {
                    // Problem if the number of dimensions specified exceeds
                    // the number of dimensions of arrayType
                    dim++;
                    if (dim >= mDimLength.size()) {
                        throw new SAXException(
                            Messages.getMessage(exceptKey, text));
                    }
                    // Get the next token and convert to integer
                    int workIndex = Integer.parseInt(tokenizer.nextToken());

                    // Problem if the index is out of range.                     
                    if (workIndex < 0 || 
                        workIndex >= 
                            ((Integer)mDimLength.get(dim)).intValue()) {
                        throw new SAXException(
                            Messages.getMessage(exceptKey, text));
                    }
                    work.add(new Integer(workIndex));
                }
                index = toSingleIndex(work); // Convert to single index
            }
        } catch (SAXException e) {
            throw e;
        } catch (Exception e) {
            throw new SAXException(Messages.getMessage(exceptKey, text));
        }
        return index;
    } 

    /**
     * Converts single index to list of multiple indices.
     * @param single index
     * @return list of multiple indices or null if not multiple indices.
     */
    private ArrayList toMultiIndex(int single) {
        if (mDimLength == null) 
            return null;

        // Calculate the index factors if not already known
        if (mDimFactor == null) {
            mDimFactor = new ArrayList();
            for (int i=0; i < mDimLength.size(); i++) {
                int factor = 1;
                for (int j=i+1; j<mDimLength.size(); j++) {
                    factor *= ((Integer)mDimLength.get(j)).intValue();
                }
                mDimFactor.add(new Integer(factor));
            }
        }

        ArrayList rc = new ArrayList();
        for (int i=0; i < mDimLength.size(); i++) {
            int factor = ((Integer)mDimFactor.get(i)).intValue();
            rc.add(new Integer(single / factor));
            single = single % factor;
        }
        return rc;
    }

    /**
     * Converts multiple index to single index.
     * @param indexArray list of multiple indices
     * @return single index
     */
    private int toSingleIndex(ArrayList indexArray) {
        if (mDimLength == null || indexArray == null) 
            return -1;

        // Calculate the index factors if not already known
        if (mDimFactor == null) {
            mDimFactor = new ArrayList();
            for (int i=0; i < mDimLength.size(); i++) {
                int factor = 1;
                for (int j=i+1; j<mDimLength.size(); j++) {
                    factor *= ((Integer)mDimLength.get(j)).intValue();
                }
                mDimFactor.add(new Integer(factor));
            }
        }

        int single = 0;
        for (int i=0; i < indexArray.size(); i++) {
            single += ((Integer)mDimFactor.get(i)).intValue()*
                ((Integer)indexArray.get(i)).intValue();
        }
        return single;
    }

    /**
     * During processing, the Array Deserializer stores the array in 
     * an ArrayListExtension class.  This class contains all of the
     * normal function of an ArrayList, plus it keeps a list of the
     * converted array values.  This class is essential to support
     * arrays that are multi-referenced.
     **/
    public class ArrayListExtension extends ArrayList 
        implements JavaUtils.ConvertCache {
        private HashMap table = null;
        private Class arrayClass = null;  // The array class.
        /**
         * Constructors
         */
        ArrayListExtension(Class arrayClass) {
            super();
            this.arrayClass = arrayClass;
            // Don't use the array class as a hint 
            // if it can't be instantiated
            if (arrayClass == null ||
                arrayClass.isInterface() ||
                java.lang.reflect.Modifier.isAbstract(
                    arrayClass.getModifiers())) {
                arrayClass = null;
            }                
        }
        ArrayListExtension(Class arrayClass, int length) {
            // Sanity check the array size, 50K is big enough to start
            super(length > 50000 ? 50000 : length);
            this.arrayClass = arrayClass;
            // Don't use the array class as a hint 
            // if it can't be instantiated
            if (arrayClass == null ||
                arrayClass.isInterface() ||
                java.lang.reflect.Modifier.isAbstract(
                    arrayClass.getModifiers())) {
                arrayClass = null;
            } 
        }
        /**
         * Store converted value
         **/
        public void setConvertedValue(Class cls, Object value) {
            if (table == null)
                table = new HashMap();
            table.put(cls, value);
        }
        /**
         * Get previously converted value
         **/
        public Object getConvertedValue(Class cls) {
            if (table == null)
                return null;
            return table.get(cls);
        }

        /**
         * Get the destination array class described by the xml
         **/         
        public Class getDestClass() {
            return arrayClass;
        }
    }
   
}

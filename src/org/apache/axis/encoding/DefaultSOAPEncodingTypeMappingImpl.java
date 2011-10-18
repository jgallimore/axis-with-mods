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

package org.apache.axis.encoding;

import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.axis.encoding.ser.Base64SerializerFactory;
import org.apache.axis.encoding.ser.Base64DeserializerFactory;
import org.apache.axis.encoding.ser.ArraySerializerFactory;
import org.apache.axis.encoding.ser.ArrayDeserializerFactory;

/**
 *
 * This is the implementation of the axis Default JAX-RPC SOAP Encoding TypeMapping
 * See DefaultTypeMapping for more information.
 * 
 * @author Rich Scheuerle (scheu@us.ibm.com)
 */
public class DefaultSOAPEncodingTypeMappingImpl extends DefaultTypeMappingImpl {
    private static DefaultSOAPEncodingTypeMappingImpl tm = null;
    /**
     * Construct TypeMapping
     */
    public static synchronized TypeMappingImpl getSingleton() {
        if (tm == null) {
            tm = new DefaultSOAPEncodingTypeMappingImpl();
        }
        return tm;
    }

    public static TypeMappingDelegate createWithDelegate() {
        TypeMappingDelegate ret = new TypeMappingDelegate(new DefaultSOAPEncodingTypeMappingImpl());
        MessageContext mc = MessageContext.getCurrentContext();
        TypeMappingDelegate tm = null;
        if (mc != null) {
            tm = (TypeMappingDelegate)mc.getTypeMappingRegistry().getDefaultTypeMapping();
        } else {
            tm = DefaultTypeMappingImpl.getSingletonDelegate();
        }
        ret.setNext(tm);
        return ret;
    }

    protected DefaultSOAPEncodingTypeMappingImpl() {
        super(true);
        registerSOAPTypes();
    }

    /**
     * Register the SOAP encoding data types.  This is split out into a
     * method so it can happen either before or after the XSD mappings.
     */
    private void registerSOAPTypes() {
        // SOAP Encoded strings are treated as primitives.
        // Everything else is not.
        myRegisterSimple(Constants.SOAP_STRING, java.lang.String.class);
        myRegisterSimple(Constants.SOAP_BOOLEAN, java.lang.Boolean.class);
        myRegisterSimple(Constants.SOAP_DOUBLE, java.lang.Double.class);
        myRegisterSimple(Constants.SOAP_FLOAT, java.lang.Float.class);
        myRegisterSimple(Constants.SOAP_INT, java.lang.Integer.class);
        myRegisterSimple(Constants.SOAP_INTEGER, java.math.BigInteger.class);
        myRegisterSimple(Constants.SOAP_DECIMAL, java.math.BigDecimal.class);
        myRegisterSimple(Constants.SOAP_LONG, java.lang.Long.class);
        myRegisterSimple(Constants.SOAP_SHORT, java.lang.Short.class);
        myRegisterSimple(Constants.SOAP_BYTE, java.lang.Byte.class);

        myRegister(Constants.SOAP_BASE64,     byte[].class,
                   new Base64SerializerFactory(byte[].class,
                                               Constants.SOAP_BASE64),
                   new Base64DeserializerFactory(byte[].class,
                                                 Constants.SOAP_BASE64)
        );
        myRegister(Constants.SOAP_BASE64BINARY,     byte[].class,
                   new Base64SerializerFactory(byte[].class,
                                               Constants.SOAP_BASE64BINARY),
                   new Base64DeserializerFactory(byte[].class,
                                                 Constants.SOAP_BASE64BINARY)
        );

        myRegister(Constants.SOAP_ARRAY12,     java.util.Collection.class,
                   new ArraySerializerFactory(),
                   new ArrayDeserializerFactory()
        );
        myRegister(Constants.SOAP_ARRAY12,     java.util.ArrayList.class,
                   new ArraySerializerFactory(),
                   new ArrayDeserializerFactory()
        );

        myRegister(Constants.SOAP_ARRAY12,     Object[].class,
                   new ArraySerializerFactory(),
                   new ArrayDeserializerFactory()
        );

        myRegister(Constants.SOAP_ARRAY,     java.util.ArrayList.class,
                   new ArraySerializerFactory(),
                   new ArrayDeserializerFactory()
        );

        // All array objects automatically get associated with the SOAP_ARRAY.
        // There is no way to do this with a hash table,
        // so it is done directly in getTypeQName.
        // Internally the runtime uses ArrayList objects to hold arrays...
        // which is the reason that ArrayList is associated with SOAP_ARRAY.
        // In addition, handle all objects that implement the List interface
        // as a SOAP_ARRAY
        myRegister(Constants.SOAP_ARRAY,     java.util.Collection.class,
                   new ArraySerializerFactory(),
                   new ArrayDeserializerFactory()
        );

        myRegister(Constants.SOAP_ARRAY,     Object[].class,
                   new ArraySerializerFactory(),
                   new ArrayDeserializerFactory()
        );


    }
}

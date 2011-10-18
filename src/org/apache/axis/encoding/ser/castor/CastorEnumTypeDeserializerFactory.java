/*
 * Copyright 2002,2004 The Apache Software Foundation.
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

package org.apache.axis.encoding.ser.castor;

import org.apache.axis.encoding.ser.BaseDeserializerFactory;
import org.apache.axis.encoding.DeserializerFactory;

import javax.xml.namespace.QName;

/**
 * A CastorEnumTypeDeserializer Factory
 * 
 * @author Ozzie Gurkan
 */
public class CastorEnumTypeDeserializerFactory extends BaseDeserializerFactory {

    public CastorEnumTypeDeserializerFactory(Class javaType, QName xmlType) {
        super(CastorEnumTypeDeserializer.class, xmlType, javaType);
    }
    public static DeserializerFactory create(Class javaType, QName xmlType) {
        return new CastorEnumTypeDeserializerFactory(javaType, xmlType);
    }
}

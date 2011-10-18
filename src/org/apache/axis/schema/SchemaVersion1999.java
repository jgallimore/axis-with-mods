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

package org.apache.axis.schema;

import org.apache.axis.Constants;
import org.apache.axis.encoding.TypeMappingImpl;
import org.apache.axis.encoding.ser.CalendarDeserializerFactory;
import org.apache.axis.encoding.ser.CalendarSerializerFactory;

import javax.xml.namespace.QName;

/**
 * 1999 Schema characteristics.
 *
 * @author Glen Daniels (gdaniels@apache.org)
 */
public class SchemaVersion1999 implements SchemaVersion {
    public static QName QNAME_NIL = new QName(Constants.URI_1999_SCHEMA_XSI,
                                              "null");

    /**
     * Package-access constructor - access this through SchemaVersion
     * constants.
     */
    SchemaVersion1999() {
    }

    /**
     * Get the appropriate QName for the "null"/"nil" attribute for this
     * Schema version.
     * @return {http://www.w3.org/1999/XMLSchema-instance}null
     */
    public QName getNilQName() {
        return QNAME_NIL;
    }

    /**
     * The XSI URI
     * @return the XSI URI
     */
    public String getXsiURI() {
        return Constants.URI_1999_SCHEMA_XSI;
    }

    /**
     * The XSD URI
     * @return the XSD URI
     */
    public String getXsdURI() {
        return Constants.URI_1999_SCHEMA_XSD;
    }

    /**
     * Register the schema specific type mappings
     */
    public void registerSchemaSpecificTypes(TypeMappingImpl tm) {
        
        // Register the timeInstant type
        tm.register(java.util.Calendar.class, 
                    Constants.XSD_TIMEINSTANT1999,
                    new CalendarSerializerFactory(java.util.Calendar.class,
                                                  Constants.XSD_TIMEINSTANT1999),
                    new CalendarDeserializerFactory(java.util.Calendar.class,
                                                    Constants.XSD_TIMEINSTANT1999));
    }
}

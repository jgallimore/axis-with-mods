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

import org.apache.axis.attachments.OctetStream;
import org.apache.axis.attachments.OctetStreamDataSource;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.encoding.SerializationContext;
import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * application/octet-stream DataHandler Serializer
 * @author Davanum Srinivas (dims@yahoo.com)
 */
public class OctetStreamDataHandlerSerializer extends JAFDataHandlerSerializer {

    protected static Log log =
            LogFactory.getLog(OctetStreamDataHandlerSerializer.class.getName());

    /**
     * Serialize a Source DataHandler quantity.
     */
    public void serialize(QName name, Attributes attributes,
                          Object value, SerializationContext context)
            throws IOException {
        DataHandler dh = new DataHandler(
                new OctetStreamDataSource("source", (OctetStream) value));
        super.serialize(name, attributes, dh, context);
    } // serialize
} // class PlainTextDataHandlerSerializer

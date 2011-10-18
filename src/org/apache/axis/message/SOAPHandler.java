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

/** A <code>SOAPHandler</code>
 * 
 * @author Glen Daniels (gdaniels@allaire.com)
 */

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.TypeMappingRegistry;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.soap.SOAPException;
import java.io.CharArrayWriter;

public class SOAPHandler extends DefaultHandler
{
    public MessageElement myElement = null;
    private MessageElement[] myElements;
    private int myIndex = 0;

    private CharArrayWriter val;
    
    public SOAPHandler() {
    }

   /**
    * This constructor allows deferred setting of any elements
    * @param elements array of message elements to be populated
    * @param index position in array where the message element is to be created
    */
    public SOAPHandler(MessageElement[] elements, int index) {
        myElements = elements;
        myIndex = index;
    }
    
    public void startElement(String namespace, String localName,
                             String prefix, Attributes attributes,
                             DeserializationContext context)
        throws SAXException
    {
        SOAPConstants soapConstants = context.getSOAPConstants();

        if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
            String encodingStyle = attributes.getValue(Constants.URI_SOAP12_ENV,
                                Constants.ATTR_ENCODING_STYLE);

            if (encodingStyle != null && !encodingStyle.equals("")
                && !encodingStyle.equals(Constants.URI_SOAP12_NOENC)
                && !Constants.isSOAP_ENC(encodingStyle)) {
                TypeMappingRegistry tmr = context.getTypeMappingRegistry();
                // TODO: both soap encoding style is registered ?
                if (tmr.getTypeMapping(encodingStyle) == tmr.getDefaultTypeMapping()) {
                    AxisFault fault = new AxisFault(Constants.FAULT_SOAP12_DATAENCODINGUNKNOWN,
                        null, Messages.getMessage("invalidEncodingStyle"), null, null, null);

                    throw new SAXException(fault);
                }
            }
        }


        // By default, make a new element
        if (!context.isDoneParsing() && !context.isProcessingRef()) {
            if (myElement == null) {
                try {
                    myElement = makeNewElement(namespace, localName, prefix,
                                               attributes, context);
                } catch (AxisFault axisFault) {
                    throw new SAXException(axisFault);
                }
            }
            context.pushNewElement(myElement);
        }
    }

    public MessageElement makeNewElement(String namespace, String localName,
                             String prefix, Attributes attributes,
                             DeserializationContext context)
        throws AxisFault
    {
        return new MessageElement(namespace, localName,
                                               prefix, attributes, context);
    }

    public void endElement(String namespace, String localName,
                           DeserializationContext context)
        throws SAXException
    {
        if (myElement != null) {
            addTextNode();	

            if (myElements != null) {
                myElements[myIndex] = myElement;
            }
            myElement.setEndIndex(context.getCurrentRecordPos());
        }
    }
    
    public SOAPHandler onStartChild(String namespace, 
                                    String localName,
                                    String prefix,
                                    Attributes attributes,
                                    DeserializationContext context)
        throws SAXException
    {
        addTextNode();
        SOAPHandler handler = new SOAPHandler();
        return handler;
    }

    private void addTextNode() throws SAXException {
        if (myElement != null) {
            if (val != null && val.size() > 0) {
                String s = StringUtils.strip(val.toString());
                val.reset();
                
                // we need to check the length of STRIPPED string 
                // to avoid appending ignorable white spaces as 
                // message elmenet's child. 
                // (SOAPHeader and others does not accept text children...
                // but in SAAJ 1.2's DOM view, this could be incorrect.
                // we need to keep the ignorable white spaces later)
                if(s.length()>0){
                    try {
                        // add unstripped string as text child.
                        myElement.addTextNode(s);
                    } catch (SOAPException e) {
                        throw new SAXException(e);
                    }
                }
            }
        }
    }
    
    public void onEndChild(String namespace, String localName,
                           DeserializationContext context)
        throws SAXException
    {
    }

    public void characters(char[] chars, int start, int end) 
        throws SAXException 
    {
        if (val == null) {
            val = new CharArrayWriter();
        }
        val.write(chars, start, end);
    }
}

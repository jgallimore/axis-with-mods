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

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.client.AxisClient;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.configuration.NullProvider;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.schema.SchemaVersion;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.Mapping;
import org.apache.axis.utils.Messages;
import org.apache.commons.logging.Log;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;

/**
 * Implementation of a SOAP Envelope
 */ 
public class SOAPEnvelope extends MessageElement
    implements javax.xml.soap.SOAPEnvelope
{
    protected static Log log =
        LogFactory.getLog(SOAPEnvelope.class.getName());

    private SOAPHeader header;
    private SOAPBody body;

    public Vector trailers = new Vector();
    private SOAPConstants soapConstants;
    private SchemaVersion schemaVersion = SchemaVersion.SCHEMA_2001;

    // This is a hint to any service description to tell it what
    // "type" of message we are.  This might be "request", "response",
    // or anything else your particular service descripton requires.
    //
    // This gets passed back into the service description during
    // deserialization
    public String messageType;
    private boolean recorded;

    public SOAPEnvelope()
    {
        this(true, SOAPConstants.SOAP11_CONSTANTS);
    }

    public SOAPEnvelope(SOAPConstants soapConstants)
    {
        this(true, soapConstants);
    }

    public SOAPEnvelope(SOAPConstants soapConstants,
                        SchemaVersion schemaVersion)
    {
        this(true, soapConstants, schemaVersion);
    }

    public SOAPEnvelope(boolean registerPrefixes, SOAPConstants soapConstants)
    {
        this (registerPrefixes, soapConstants, SchemaVersion.SCHEMA_2001);
    }
    
    public SOAPEnvelope(boolean registerPrefixes,
                        SOAPConstants soapConstants,
                        SchemaVersion schemaVersion)
    {    
        // FIX BUG http://nagoya.apache.org/bugzilla/show_bug.cgi?id=18108
        super(Constants.ELEM_ENVELOPE,
               Constants.NS_PREFIX_SOAP_ENV,
               (soapConstants != null) ? soapConstants.getEnvelopeURI() : Constants.DEFAULT_SOAP_VERSION.getEnvelopeURI());

        if (soapConstants == null)
          soapConstants = Constants.DEFAULT_SOAP_VERSION;
        // FIX BUG http://nagoya.apache.org/bugzilla/show_bug.cgi?id=18108        
        
        this.soapConstants = soapConstants;
        this.schemaVersion = schemaVersion;
        header = new SOAPHeader(this, soapConstants);
        body = new SOAPBody(this, soapConstants);

        if (registerPrefixes) {
            if (namespaces == null)
                namespaces = new ArrayList();

            namespaces.add(new Mapping(soapConstants.getEnvelopeURI(),
                                       Constants.NS_PREFIX_SOAP_ENV));
            namespaces.add(new Mapping(schemaVersion.getXsdURI(),
                                       Constants.NS_PREFIX_SCHEMA_XSD));
            namespaces.add(new Mapping(schemaVersion.getXsiURI(),
                                       Constants.NS_PREFIX_SCHEMA_XSI));
        }

        setDirty();
    }
    
    public SOAPEnvelope(InputStream input) throws SAXException {
        InputSource is = new InputSource(input);
        // FIX BUG http://nagoya.apache.org/bugzilla/show_bug.cgi?id=18108
        //header = new SOAPHeader(this, soapConstants); // soapConstants = null!
        header = new SOAPHeader(this, Constants.DEFAULT_SOAP_VERSION); // soapConstants = null!
        // FIX BUG http://nagoya.apache.org/bugzilla/show_bug.cgi?id=18108
        DeserializationContext dser = null ;
        AxisClient     tmpEngine = new AxisClient(new NullProvider());
        MessageContext msgContext = new MessageContext(tmpEngine);
        dser = new DeserializationContext(is, msgContext,
                                          Message.REQUEST, this );
        dser.parse();
    }

    /**
     * Get the Message Type (REQUEST/RESPONSE)
     * @return message type
     */ 
    public String getMessageType()
    {
        return messageType;
    }

    /**
     * Set the Message Type (REQUEST/RESPONSE)
     * @param messageType
     */ 
    public void setMessageType(String messageType)
    {
        this.messageType = messageType;
    }

    /**
     * Get all the BodyElement's in the soap body
     * @return vector with body elements
     * @throws AxisFault
     */ 
    public Vector getBodyElements() throws AxisFault
    {
        if (body != null) {
            return body.getBodyElements();
        } else {
            return new Vector();
        }
    }

    /**
     * Return trailers
     * @return vector of some type
     */ 
    public Vector getTrailers()
    {
        return trailers;
    }

    /**
     * Get the first BodyElement in the SOAP Body
     * @return first Body Element
     * @throws AxisFault
     */ 
    public SOAPBodyElement getFirstBody() throws AxisFault
    {
        if (body == null) {
            return null;
        } else {
            return body.getFirstBody();
        }
    }

    /**
     * Get Headers
     * @return Vector containing Header's
     * @throws AxisFault
     */ 
    public Vector getHeaders() throws AxisFault
    {
        if (header != null) {
            return header.getHeaders();
        } else {
            return new Vector();
        }
    }

    /**
     * Get all the headers targeted at a list of actors.
     */
    public Vector getHeadersByActor(ArrayList actors)
    {
        if (header != null) {
            return header.getHeadersByActor(actors);
        } else {
            return new Vector();
        }
    }

    /**
     * Add a HeaderElement
     * @param hdr
     */ 
    public void addHeader(SOAPHeaderElement hdr)
    {
        if (header == null) {
            header = new SOAPHeader(this, soapConstants);
        }
        hdr.setEnvelope(this);
        header.addHeader(hdr);
        _isDirty = true;
    }

    /**
     * Add a SOAP Body Element
     * @param element
     */ 
    public void addBodyElement(SOAPBodyElement element)
    {
        if (body == null) {
            body = new SOAPBody(this, soapConstants);
        }
        element.setEnvelope(this);
        body.addBodyElement(element);

        _isDirty = true;
    }

    /**
     * Remove all headers
     */ 
    public void removeHeaders() {
        if (header != null) {
            removeChild(header);
        }
        header = null;
    }

    /**
     * Set the SOAP Header
     * @param hdr
     */ 
    public void setHeader(SOAPHeader hdr) {
        if(this.header != null) {
            removeChild(this.header);
        }
        header = hdr;
        try {
            header.setParentElement(this);
        } catch (SOAPException ex) {
            // class cast should never fail when parent is a SOAPEnvelope
            log.fatal(Messages.getMessage("exception00"), ex);
        }
    }

    /**
     * Remove a Header Element from SOAP Header
     * @param hdr
     */ 
    public void removeHeader(SOAPHeaderElement hdr)
    {
        if (header != null) {
            header.removeHeader(hdr);
            _isDirty = true;
        }
    }

    /**
     * Remove the SOAP Body
     */ 
    public void removeBody() {
        if (body != null) {
            removeChild(body);
        }
        body = null;
    }

    /**
     * Set the soap body
     * @param body
     */ 
    public void setBody(SOAPBody body) {
        if(this.body != null) {
            removeChild(this.body);
        }
        this.body = body;
        try {
            body.setParentElement(this);
        } catch (SOAPException ex) {
            // class cast should never fail when parent is a SOAPEnvelope
            log.fatal(Messages.getMessage("exception00"), ex);
        }
    }

    /**
     * Remove a Body Element from the soap body
     * @param element
     */ 
    public void removeBodyElement(SOAPBodyElement element)
    {
        if (body != null) {
            body.removeBodyElement(element);
            _isDirty = true;
        }
    }

    /**
     * Remove an element from the trailer
     * @param element
     */ 
    public void removeTrailer(MessageElement element)
    {
        if (log.isDebugEnabled())
            log.debug(Messages.getMessage("removeTrailer00"));
        trailers.removeElement(element);
        _isDirty = true;
    }

    /**
     * clear the elements in the soap body
     */ 
    public void clearBody()
    {
        if (body != null) {
            body.clearBody();
            _isDirty = true;
        }
    }

    /**
     * Add an element to the trailer
     * @param element
     */ 
    public void addTrailer(MessageElement element)
    {
        if (log.isDebugEnabled())
            log.debug(Messages.getMessage("removeTrailer00"));
        element.setEnvelope(this);
        trailers.addElement(element);
        _isDirty = true;
    }

    /**
     * Get a header by name (always respecting the currently in-scope
     * actors list)
     */
    public SOAPHeaderElement getHeaderByName(String namespace,
                                             String localPart)
        throws AxisFault
    {
        return getHeaderByName(namespace, localPart, false);
    }

    /**
     * Get a header by name, filtering for headers targeted at this
     * engine depending on the accessAllHeaders parameter.
     */
    public SOAPHeaderElement getHeaderByName(String namespace,
                                             String localPart,
                                             boolean accessAllHeaders)
        throws AxisFault
    {
        if (header != null) {
            return header.getHeaderByName(namespace,
                                          localPart,
                                          accessAllHeaders);
        } else {
            return null;
        }
    }

    /**
     * Get a body element given its name
     * @param namespace
     * @param localPart
     * @return
     * @throws AxisFault
     */ 
    public SOAPBodyElement getBodyByName(String namespace, String localPart)
        throws AxisFault
    {
        if (body == null) {
            return null;
        } else {
            return body.getBodyByName(namespace, localPart);
        }
    }

    /**
     * Get an enumeration of header elements given the namespace and localpart
     * @param namespace
     * @param localPart
     * @return
     * @throws AxisFault
     */ 
    public Enumeration getHeadersByName(String namespace, String localPart)
        throws AxisFault
    {
        return getHeadersByName(namespace, localPart, false);
    }

    /**
     * Return an Enumeration of headers which match the given namespace
     * and localPart.  Depending on the value of the accessAllHeaders
     * parameter, we will attempt to filter on the current engine's list
     * of actors.
     *
     * !!! NOTE THAT RIGHT NOW WE ALWAYS ASSUME WE'RE THE "ULTIMATE
     * DESTINATION" (i.e. we match on null actor).  IF WE WANT TO FULLY SUPPORT
     * INTERMEDIARIES WE'LL NEED TO FIX THIS.
     */
    public Enumeration getHeadersByName(String namespace, String localPart,
                                        boolean accessAllHeaders)
        throws AxisFault
    {
        if (header != null) {
            return header.getHeadersByName(namespace,
                                           localPart,
                                           accessAllHeaders);
        } else {
            return new Vector().elements();
        }
    }

    /** Should make SOAPSerializationException?
     */
    public void outputImpl(SerializationContext context)
        throws Exception
    {
        boolean oldPretty = context.getPretty();
        context.setPretty(true);

        // Register namespace prefixes.
        if (namespaces != null) {
            for (Iterator i = namespaces.iterator(); i.hasNext(); ) {
                Mapping mapping = (Mapping)i.next();
                context.registerPrefixForURI(mapping.getPrefix(),
                                             mapping.getNamespaceURI());
            }
        }

        Enumeration enumeration;

        // Output <SOAP-ENV:Envelope>
        context.startElement(new QName(soapConstants.getEnvelopeURI(),
                                       Constants.ELEM_ENVELOPE), attributes);

        
        // Output <SOAP-ENV:Envelope>'s each child as it appears.
        Iterator i = getChildElements();
        while (i.hasNext()) {            
            NodeImpl node = (NodeImpl)i.next();
            
            if (node instanceof SOAPHeader) {
                header.outputImpl(context);
            } else if (node instanceof SOAPBody) {
                body.outputImpl(context);                
            } else if (node instanceof MessageElement) {
                ((MessageElement)node).output(context);
            } else {
                node.output(context);
            }    
        }
        
        // Output trailers
        enumeration = trailers.elements();
        while (enumeration.hasMoreElements()) {
            MessageElement element = (MessageElement)enumeration.nextElement();
            element.output(context);
            // Output this independent element
        }

        // Output </SOAP-ENV:Envelope>
        context.endElement();

        context.setPretty(oldPretty);
    }

    /**
     * Get the soap constants for this envelope
     * @return
     */ 
    public SOAPConstants getSOAPConstants() {
        return soapConstants;
    }

    /**
     * Set the soap constants for this envelope
     * @param soapConstants
     */ 
    public void setSoapConstants(SOAPConstants soapConstants) {
        this.soapConstants = soapConstants;
    }

    /**
     * Get the schema version for this envelope
     * @return
     */ 
    public SchemaVersion getSchemaVersion() {
        return schemaVersion;
    }
 
    /**
     * Set the schema version for this envelope
     * @param schemaVersion
     */ 
    public void setSchemaVersion(SchemaVersion schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    /**
     * Add a soap body if one does not exist
     * @return
     * @throws SOAPException
     */ 
    public javax.xml.soap.SOAPBody addBody() throws SOAPException {
        if (body == null) {
            body = new SOAPBody(this, soapConstants);
            _isDirty = true;
            body.setOwnerDocument(getOwnerDocument());
            return body;
        } else {
            throw new SOAPException(Messages.getMessage("bodyPresent"));
        }
    }

    /**
     * Add a soap header if one does not exist
     * @return
     * @throws SOAPException
     */ 
    public javax.xml.soap.SOAPHeader addHeader() throws SOAPException {
        if (header == null) {
            header = new SOAPHeader(this, soapConstants);
            header.setOwnerDocument(getOwnerDocument());
            return header;
        } else {
            throw new SOAPException(Messages.getMessage("headerPresent"));
        }
    }

    /**
     * create a Name given the local part
     * @param localName
     * @return
     * @throws SOAPException
     */ 
    public javax.xml.soap.Name createName(String localName)
        throws SOAPException {
        return new PrefixedQName(null, localName,  null);
    }

    /**
     * Create a name given local part, prefix and uri
     * @param localName
     * @param prefix
     * @param uri
     * @return
     * @throws SOAPException
     */ 
    public javax.xml.soap.Name createName(String localName,
                                          String prefix,
                                          String uri)
        throws SOAPException {
        return new PrefixedQName(uri, localName, prefix);
    }

    /**
     * Get the soap body
     * @return
     * @throws SOAPException
     */ 
    public javax.xml.soap.SOAPBody getBody() throws SOAPException {
        return body;
    }

    /**
     * Get the soap header
     * @return
     * @throws SOAPException
     */ 
    public javax.xml.soap.SOAPHeader getHeader() throws SOAPException {
        return header;
    }

    public void setSAAJEncodingCompliance(boolean comply) {
        this.body.setSAAJEncodingCompliance(comply);
    }
    
    public Node removeChild(Node oldChild) throws DOMException {
        if(oldChild == header) {
            header = null;
        } else if(oldChild == body) {
            body = null;
        }
        return super.removeChild(oldChild);
    }

    public Node cloneNode(boolean deep)
    {
        SOAPEnvelope envelope = (SOAPEnvelope)super.cloneNode( deep );

        if( !deep )
        {
            envelope.body = null;
            envelope.header = null;
        }

        return envelope;
    }

    protected void childDeepCloned( NodeImpl oldNode, NodeImpl newNode )
    {
        if( oldNode == body )
        {
            body = (SOAPBody)newNode;

            try {
                body.setParentElement(this);
            } catch (SOAPException ex) {
                // class cast should never fail when parent is a SOAPEnvelope
                log.fatal(Messages.getMessage("exception00"), ex);
            }
        }
        else
        if( oldNode == header )
        {
            header = (SOAPHeader)newNode;
        }
    }
    
    public void setOwnerDocument(org.apache.axis.SOAPPart sp) {
        super.setOwnerDocument(sp);
        if(body != null) {
            body.setOwnerDocument(sp);
            setOwnerDocumentForChildren(((NodeImpl)body).children, sp);
        }
        if(header != null){
            header.setOwnerDocument(sp);
            setOwnerDocumentForChildren(((NodeImpl)body).children, sp);
        }
    }
    
    private void setOwnerDocumentForChildren(List children, org.apache.axis.SOAPPart sp) {
    	if (children == null) {
            return;
        }
        int size = children.size();
        for (int i = 0; i < size; i++) {
            NodeImpl node = (NodeImpl) children.get(i);
            node.setOwnerDocument(sp);
            setOwnerDocumentForChildren(node.children, sp);  // recursively
    	}
    }

    public void setRecorded(boolean recorded) {
        this.recorded = recorded;
    }

    public boolean isRecorded() {
        return recorded;
    }

    public void setDirty(boolean dirty) {
        if (recorder != null && !_isDirty && dirty && isRecorded()){
            recorder.clear();
            recorder = null;
        }
        setDirty();
    }
}

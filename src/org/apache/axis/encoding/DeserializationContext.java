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

import org.apache.axis.MessageContext;
import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.AxisFault;
import org.apache.axis.constants.Use;
import org.apache.axis.attachments.Attachments;
import org.apache.axis.description.TypeDesc;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.NSStack;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.utils.JavaUtils;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.cache.MethodCache;
import org.apache.axis.schema.SchemaVersion;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.message.IDResolver;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.SAX2EventRecorder;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPHandler;
import org.apache.axis.message.EnvelopeBuilder;
import org.apache.axis.message.EnvelopeHandler;
import org.apache.axis.message.NullAttributes;
import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParser;
import javax.xml.rpc.JAXRPCException;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * This interface describes the AXIS DeserializationContext, note that
 * an AXIS compliant DeserializationContext must extend the org.xml.sax.helpers.DefaultHandler.
 */

public class DeserializationContext extends DefaultHandler
        implements javax.xml.rpc.encoding.DeserializationContext, LexicalHandler {
    protected static Log log =
            LogFactory.getLog(DeserializationContext.class.getName());

    // invariant member variable to track low-level logging requirements
    // we cache this once per instance lifecycle to avoid repeated lookups
    // in heavily used code.
    private final boolean debugEnabled = log.isDebugEnabled();

    static final SchemaVersion schemaVersions[] = new SchemaVersion [] {
        SchemaVersion.SCHEMA_1999,
        SchemaVersion.SCHEMA_2000,
        SchemaVersion.SCHEMA_2001,
    };

    private NSStack namespaces = new NSStack();

    private Locator locator;

    // Class used for deserialization using class metadata from
    // downstream deserializers
    private Class destClass;

    // for performance reasons, keep the top of the stack separate from
    // the remainder of the handlers, and therefore readily available.
    private SOAPHandler topHandler = null;
    private ArrayList pushedDownHandlers = new ArrayList();

    //private SAX2EventRecorder recorder = new SAX2EventRecorder();
    private SAX2EventRecorder recorder = null;
    private SOAPEnvelope envelope;

    /* A map of IDs -> IDResolvers */
    private HashMap idMap;
    private LocalIDResolver localIDs;

    private HashMap fixups;

    static final SOAPHandler nullHandler = new SOAPHandler();

    protected MessageContext msgContext;

    private boolean doneParsing = false;
    protected InputSource inputSource = null;

    private MessageElement curElement;

    protected int startOfMappingsPos = -1;

    private static final Class[] DESERIALIZER_CLASSES =
            new Class[] {String.class, Class.class, QName.class};
    private static final String DESERIALIZER_METHOD = "getDeserializer";

    // This is a hack to associate the first schema namespace we see with
    // the correct SchemaVersion.  It assumes people won't often be mixing
    // schema versions in a given document, which I think is OK. --Glen
    protected boolean haveSeenSchemaNS = false;

    public void deserializing(boolean isDeserializing) {
        doneParsing = isDeserializing;
    }

    /**
     * Construct Deserializer using MessageContext and EnvelopeBuilder handler
     * @param ctx is the MessageContext
     * @param initialHandler is the EnvelopeBuilder handler
     */
    public DeserializationContext(MessageContext ctx,
                                  SOAPHandler initialHandler)
    {
        msgContext = ctx;

        // If high fidelity is required, record the whole damn thing.
        if (ctx == null || ctx.isHighFidelity())
            recorder = new SAX2EventRecorder();

        if (initialHandler instanceof EnvelopeBuilder) {
            envelope = ((EnvelopeBuilder)initialHandler).getEnvelope();
            envelope.setRecorder(recorder);
        }

        pushElementHandler(new EnvelopeHandler(initialHandler));
    }

    /**
     * Construct Deserializer
     * @param is is the InputSource
     * @param ctx is the MessageContext
     * @param messageType is the MessageType to construct an EnvelopeBuilder
     */
    public DeserializationContext(InputSource is,
                                  MessageContext ctx,
                                  String messageType)
    {
        msgContext = ctx;
        EnvelopeBuilder builder = new EnvelopeBuilder(messageType, ctx != null ? ctx.getSOAPConstants() : null);
        // If high fidelity is required, record the whole damn thing.
        if (ctx == null || ctx.isHighFidelity())
            recorder = new SAX2EventRecorder();

        envelope = builder.getEnvelope();
        envelope.setRecorder(recorder);

        pushElementHandler(new EnvelopeHandler(builder));

        inputSource = is;
    }

    private SOAPConstants soapConstants = null;

    /**
     * returns the soap constants.
     */
    public SOAPConstants getSOAPConstants(){
        if (soapConstants != null)
            return soapConstants;
        if (msgContext != null) {
            soapConstants = msgContext.getSOAPConstants();
            return soapConstants;
        } else {
            return Constants.DEFAULT_SOAP_VERSION;
        }
    }

    /**
     * Construct Deserializer
     * @param is is the InputSource
     * @param ctx is the MessageContext
     * @param messageType is the MessageType to construct an EnvelopeBuilder
     * @param env is the SOAPEnvelope to construct an EnvelopeBuilder
     */
    public DeserializationContext(InputSource is,
                                  MessageContext ctx,
                                  String messageType,
                                  SOAPEnvelope env)
    {
        EnvelopeBuilder builder = new EnvelopeBuilder(env, messageType);

        msgContext = ctx;

        // If high fidelity is required, record the whole damn thing.
        if (ctx == null || ctx.isHighFidelity())
            recorder = new SAX2EventRecorder();

        envelope = builder.getEnvelope();
        envelope.setRecorder(recorder);

        pushElementHandler(new EnvelopeHandler(builder));

        inputSource = is;
    }

    /**
     * Create a parser and parse the inputSource
     */
    public void parse() throws SAXException
    {
        if (inputSource != null) {
            SAXParser parser = XMLUtils.getSAXParser();
            try {
                parser.setProperty("http://xml.org/sax/properties/lexical-handler", this);
                parser.parse(inputSource, this);

                try {
                    // cleanup - so that the parser can be reused.
                    parser.setProperty("http://xml.org/sax/properties/lexical-handler", nullLexicalHandler);
                } catch (Exception e){
                    // Ignore.
                }

                // only release the parser for reuse if there wasn't an
                // error.  While parsers should be reusable, don't trust
                // parsers that died to clean up appropriately.
                XMLUtils.releaseSAXParser(parser);
            } catch (IOException e) {
                throw new SAXException(e);
            }
            inputSource = null;
        }
    }

    /**
     * Get current MessageElement
     **/
    public MessageElement getCurElement() {
        return curElement;
    }

    /**
     * Set current MessageElement
     **/
    public void setCurElement(MessageElement el)
    {
        curElement = el;
        if (curElement != null && curElement.getRecorder() != recorder) {
            recorder = curElement.getRecorder();
        }
    }


    /**
     * Get MessageContext
     */
    public MessageContext getMessageContext()
    {
        return msgContext;
    }

    /**
     * Returns this context's encoding style.  If we've got a message
     * context then we'll get the style from that; otherwise we'll
     * return a default.
     *
     * @return a <code>String</code> value
     */
    public String getEncodingStyle() 
    {
        return msgContext == null ?
                Use.ENCODED.getEncoding() : msgContext.getEncodingStyle();
    }    

    /**
     * Get Envelope
     */
    public SOAPEnvelope getEnvelope()
    {
        return envelope;
    }

    /**
     * Get Event Recorder
     */
    public SAX2EventRecorder getRecorder()
    {
        return recorder;
    }

    /**
     * Set Event Recorder
     */
    public void setRecorder(SAX2EventRecorder recorder)
    {
        this.recorder = recorder;
    }

    /**
     * Get the Namespace Mappings.  Returns null if none are present.
     **/
    public ArrayList getCurrentNSMappings()
    {
        return namespaces.cloneFrame();
    }

    /**
     * Get the Namespace for a particular prefix
     */
    public String getNamespaceURI(String prefix)
    {
        String result = namespaces.getNamespaceURI(prefix);
        if (result != null)
            return result;

        if (curElement != null)
            return curElement.getNamespaceURI(prefix);

        return null;
    }

    /**
     * Construct a QName from a string of the form <prefix>:<localName>
     * @param qNameStr is the prefixed name from the xml text
     * @return QName
     */
    public QName getQNameFromString(String qNameStr)
    {
        if (qNameStr == null)
            return null;

        // OK, this is a QName, so look up the prefix in our current mappings.
        int i = qNameStr.indexOf(':');
        String nsURI;
        if (i == -1) {
            nsURI = getNamespaceURI("");
        } else {
            nsURI = getNamespaceURI(qNameStr.substring(0, i));
        }

        return new QName(nsURI, qNameStr.substring(i + 1));
    }

    /**
     * Create a QName for the type of the element defined by localName and
     * namespace from the XSI type.
     * @param namespace of the element
     * @param localName is the local name of the element
     * @param attrs are the attributes on the element
     */
    public QName getTypeFromXSITypeAttr(String namespace, String localName,
                                          Attributes attrs) {
        // Check for type
        String type = Constants.getValue(attrs, Constants.URIS_SCHEMA_XSI,
                                         "type");
        if (type != null) {
            // Return the type attribute value converted to a QName
            return getQNameFromString(type);
        }
        return null;
    }

    /**
     * Create a QName for the type of the element defined by localName and
     * namespace with the specified attributes.
     * @param namespace of the element
     * @param localName is the local name of the element
     * @param attrs are the attributes on the element
     */
    public QName getTypeFromAttributes(String namespace, String localName,
                                       Attributes attrs)
    {
        QName typeQName = getTypeFromXSITypeAttr(namespace, localName, attrs);
        if ( (typeQName == null) && Constants.isSOAP_ENC(namespace) ) {

            // If the element is a SOAP-ENC element, the name of the element is the type.
            // If the default type mapping accepts SOAP 1.2, then use then set
            // the typeQName to the SOAP-ENC type.
            // Else if the default type mapping accepts SOAP 1.1, then
            // convert the SOAP-ENC type to the appropriate XSD Schema Type.
            if (namespace.equals(Constants.URI_SOAP12_ENC)) {
                typeQName = new QName(namespace, localName);
            } else if (localName.equals(Constants.SOAP_ARRAY.getLocalPart())) {
                typeQName = Constants.SOAP_ARRAY;
            } else if (localName.equals(Constants.SOAP_STRING.getLocalPart())) {
                typeQName = Constants.SOAP_STRING;
            } else if (localName.equals(Constants.SOAP_BOOLEAN.getLocalPart())) {
                typeQName = Constants.SOAP_BOOLEAN;
            } else if (localName.equals(Constants.SOAP_DOUBLE.getLocalPart())) {
                typeQName = Constants.SOAP_DOUBLE;
            } else if (localName.equals(Constants.SOAP_FLOAT.getLocalPart())) {
                typeQName = Constants.SOAP_FLOAT;
            } else if (localName.equals(Constants.SOAP_INT.getLocalPart())) {
                typeQName = Constants.SOAP_INT;
            } else if (localName.equals(Constants.SOAP_LONG.getLocalPart())) {
                typeQName = Constants.SOAP_LONG;
            } else if (localName.equals(Constants.SOAP_SHORT.getLocalPart())) {
                typeQName = Constants.SOAP_SHORT;
            } else if (localName.equals(Constants.SOAP_BYTE.getLocalPart())) {
                typeQName = Constants.SOAP_BYTE;
            }
        }

        // If we still have no luck, check to see if there's an arrayType
        // (itemType for SOAP 1.2) attribute, in which case this is almost
        // certainly an array.

        if (typeQName == null && attrs != null) {
            String encURI = getSOAPConstants().getEncodingURI();
            String itemType = getSOAPConstants().getAttrItemType();
            for (int i = 0; i < attrs.getLength(); i++) {
                if (encURI.equals(attrs.getURI(i)) &&
                        itemType.equals(attrs.getLocalName(i))) {
                    return new QName(encURI, "Array");
                }
            }
        }

        return typeQName;
    }

    /**
     * Convenenience method that returns true if the value is nil
     * (due to the xsi:nil) attribute.
     * @param attrs are the element attributes.
     * @return true if xsi:nil is true
     */
    public boolean isNil(Attributes attrs) {
        return JavaUtils.isTrueExplicitly(
                    Constants.getValue(attrs, Constants.QNAMES_NIL),
                    false);
    }

    /**
     * Get a Deserializer which can turn a given xml type into a given
     * Java type
     */
    public final Deserializer getDeserializer(Class cls, QName xmlType) {
        if (xmlType == null)
            return null;

        DeserializerFactory dserF = null;
        Deserializer dser = null;
        try {
            dserF = (DeserializerFactory) getTypeMapping().
                    getDeserializer(cls, xmlType);
        } catch (JAXRPCException e) {
            log.error(Messages.getMessage("noFactory00", xmlType.toString()));
        }
        if (dserF != null) {
            try {
                dser = (Deserializer) dserF.getDeserializerAs(Constants.AXIS_SAX);
            } catch (JAXRPCException e) {
                log.error(Messages.getMessage("noDeser00", xmlType.toString()));
            }
        }
        return dser;
    }

    /**
     * Convenience method to get the Deserializer for a specific
     * java class from its meta data.
     * @param cls is the Class used to find the deserializer
     * @return Deserializer
     */
    public Deserializer getDeserializerForClass(Class cls) {
        if (cls == null) {
           cls = destClass;
        }
        if (cls == null) {
            return null;
        }
//        if (cls.isArray()) {
//            cls = cls.getComponentType();
//        }
        if (javax.xml.rpc.holders.Holder.class.isAssignableFrom(cls)) {
            try {
                cls = cls.getField("value").getType();
            } catch (Exception e) {
            }
        }

        Deserializer dser = null;

        QName type = getTypeMapping().getTypeQName(cls);
        dser = getDeserializer(cls, type);
        if (dser != null)
            return dser;

        try {
            Method method = 
                MethodCache.getInstance().getMethod(cls,
                                                    DESERIALIZER_METHOD, 
                                                    DESERIALIZER_CLASSES);     
            if (method != null) {
                TypeDesc typedesc = TypeDesc.getTypeDescForClass(cls);
                if (typedesc != null) {
                    dser = (Deserializer) method.invoke(null,
                        new Object[] {getEncodingStyle(), cls, typedesc.getXmlType()});
                }
            }
        } catch (Exception e) {
            log.error(Messages.getMessage("noDeser00", cls.getName()));
        }
        return dser;
    }

     /**
     * Allows the destination class to be set so that downstream
     * deserializers like ArrayDeserializer can pick it up when
     * deserializing its components using getDeserializerForClass
     * @param destClass is the Class of the component to be deserialized
     */
    public void setDestinationClass(Class destClass) {
        this.destClass = destClass;
    }

    /**
     * Allows the destination class to be retrieved so that downstream
     * deserializers like ArrayDeserializer can pick it up when
     * deserializing its components using getDeserializerForClass
     * @return the Class of the component to be deserialized
     */
    public Class getDestinationClass() {
        return destClass;
    }

    /**
     * Convenience method to get the Deserializer for a specific
     * xmlType.
     * @param xmlType is QName for a type to deserialize
     * @return Deserializer
     */
    public final Deserializer getDeserializerForType(QName xmlType) {
        return getDeserializer(null, xmlType);
    }

    /**
     * Get the TypeMapping for this DeserializationContext
     */
    public TypeMapping getTypeMapping()
    {
        if (msgContext == null || msgContext.getTypeMappingRegistry() == null) {
            return (TypeMapping) new org.apache.axis.encoding.TypeMappingRegistryImpl().getTypeMapping(
                    null);
        }
        TypeMappingRegistry tmr = msgContext.getTypeMappingRegistry();
        return (TypeMapping) tmr.getTypeMapping(getEncodingStyle());
    }

    /**
     * Get the TypeMappingRegistry we're using.
     * @return TypeMapping or null
     */
    public TypeMappingRegistry getTypeMappingRegistry() {
        return msgContext.getTypeMappingRegistry();
    }

    /**
     * Get the MessageElement for the indicated id (where id is the #value of an href)
     * If the MessageElement has not been processed, the MessageElement will
     * be returned.  If the MessageElement has been processed, the actual object
     * value is stored with the id and this routine will return null.
     * @param id is the value of an href attribute
     * @return MessageElement or null
     */
    public MessageElement getElementByID(String id)
    {
        if((idMap !=  null)) {
            IDResolver resolver = (IDResolver)idMap.get(id);
            if(resolver != null) {
                Object ret = resolver.getReferencedObject(id);
                if (ret instanceof MessageElement)
                    return (MessageElement)ret;
            }
        }

        return null;
    }

    /**
     * Gets the MessageElement or actual Object value associated with the href value.
     * The return of a MessageElement indicates that the referenced element has
     * not been processed.  If it is not a MessageElement, the Object is the
     * actual deserialized value.
     * In addition, this method is invoked to get Object values via Attachments.
     * @param href is the value of an href attribute (or an Attachment id)
     * @return MessageElement other Object or null
     */
    public Object getObjectByRef(String href) {
        Object ret= null;
        if(href != null){
            if((idMap !=  null)){
                IDResolver resolver = (IDResolver)idMap.get(href);
                if(resolver != null)
                   ret = resolver.getReferencedObject(href);
            }
            if( null == ret && !href.startsWith("#")){
                //Could this be an attachment?
                Message msg= null;
                if(null != (msg=msgContext.getCurrentMessage())){
                    Attachments attch= null;
                    if( null != (attch= msg.getAttachmentsImpl())){
                        try{
                        ret= attch.getAttachmentByReference(href);
                        }catch(AxisFault e){
                            throw new RuntimeException(e.toString() + JavaUtils.stackToString(e));
                        }
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Add the object associated with this id (where id is the value of an id= attribute,
     * i.e. it does not start with #).
     * This routine is called to associate the deserialized object
     * with the id specified on the XML element.
     * @param id (id name without the #)
     * @param obj is the deserialized object for this id.
     */
    public void addObjectById(String id, Object obj)
    {
        // The resolver uses the href syntax as the key.
        String idStr = '#' + id;
        if ((idMap == null) || (id == null))
            return ;

        IDResolver resolver = (IDResolver)idMap.get(idStr);
        if (resolver == null)
            return ;

        resolver.addReferencedObject(idStr, obj);
        return;
    }

    /**
     * During deserialization, an element with an href=#id<int>
     * may be encountered before the element defining id=id<int> is
     * read.  In these cases, the getObjectByRef method above will
     * return null.  The deserializer is placed in a table keyed
     * by href (a fixup table). After the element id is processed,
     * the deserializer is informed of the value so that it can
     * update its target(s) with the value.
     * @param href (#id syntax)
     * @param dser is the deserializer of the element
     */
    public void registerFixup(String href, Deserializer dser)
    {
        if (fixups == null)
            fixups = new HashMap();

        Deserializer prev = (Deserializer) fixups.put(href, dser);

        // There could already be a deserializer in the fixup list
        // for this href.  If so, the easiest way to get all of the
        // targets updated is to move the previous deserializers
        // targets to dser.
        if (prev != null && prev != dser) {
            dser.moveValueTargets(prev);
            if (dser.getDefaultType() == null) {
                dser.setDefaultType(prev.getDefaultType());
            }
        }
    }

    /**
     * Register the MessageElement with this id (where id is id= form without the #)
     * This routine is called when the MessageElement with an id is read.
     * If there is a Deserializer in our fixup list (described above),
     * the 'fixup' deserializer is given to the MessageElement.  When the
     * MessageElement is completed, the 'fixup' deserializer is informed and
     * it can set its targets.
     * @param id (id name without the #)
     * @param elem is the MessageElement
     */
    public void registerElementByID(String id, MessageElement elem)
    {
        if (localIDs == null)
            localIDs = new LocalIDResolver();

        String absID = '#' + id;

        localIDs.addReferencedObject(absID, elem);

        registerResolverForID(absID, localIDs);

        if (fixups != null) {
            Deserializer dser = (Deserializer)fixups.get(absID);
            if (dser != null) {
                elem.setFixupDeserializer(dser);
            }
        }
    }

    /**
     * Each id can have its own kind of resolver.  This registers a
     * resolver for the id.
     */
    public void registerResolverForID(String id, IDResolver resolver)
    {
        if ((id == null) || (resolver == null)) {
            // ??? Throw nullPointerException?
            return;
        }

        if (idMap == null)
            idMap = new HashMap();

        idMap.put(id, resolver);
    }

    /**
     * Return true if any ids are being tracked by this DeserializationContext
     *
     * @return true if any ides are being tracked by this DeserializationContext
     */
    public boolean hasElementsByID()
    {
        return idMap == null ? false : idMap.size() > 0;
    }

    /**
     * Get the current position in the record.
     */
    public int getCurrentRecordPos()
    {
        if (recorder == null) return -1;
        return recorder.getLength() - 1;
    }

    /**
     * Get the start of the mapping position
     */
    public int getStartOfMappingsPos()
    {
        if (startOfMappingsPos == -1) {
            return getCurrentRecordPos() + 1;
        }

        return startOfMappingsPos;
    }

    /**
     * Push the MessageElement into the recorder
     */
    public void pushNewElement(MessageElement elem)
    {
        if (debugEnabled) {
            log.debug("Pushing element " + elem.getName());
        }

        if (!doneParsing && (recorder != null)) {
            recorder.newElement(elem);
        }

        try {
            if(curElement != null)
                elem.setParentElement(curElement);
        } catch (Exception e) {
            /*
             * The only checked exception that may be thrown from setParent
             * occurs if the parent already has an explicit object value,
             * which should never occur during deserialization.
             */
            log.fatal(Messages.getMessage("exception00"), e);
        }
        curElement = elem;

        if (elem.getRecorder() != recorder)
            recorder = elem.getRecorder();
    }

    /****************************************************************
     * Management of sub-handlers (deserializers)
     */

    public void pushElementHandler(SOAPHandler handler)
    {
        if (debugEnabled) {
            log.debug(Messages.getMessage("pushHandler00", "" + handler));
        }

        if (topHandler != null) pushedDownHandlers.add(topHandler);
        topHandler = handler;
    }

    /** Replace the handler at the top of the stack.
     *
     * This is only used when we have a placeholder Deserializer
     * for a referenced object which doesn't know its type until we
     * hit the referent.
     */
    public void replaceElementHandler(SOAPHandler handler)
    {
        topHandler = handler;
    }

    public SOAPHandler popElementHandler()
    {
        SOAPHandler result = topHandler;

        int size = pushedDownHandlers.size();
        if (size > 0) {
            topHandler = (SOAPHandler) pushedDownHandlers.remove(size-1);
        } else {
            topHandler = null;
        }

        if (debugEnabled) {
            if (result == null) {
                log.debug(Messages.getMessage("popHandler00", "(null)"));
            } else {
                log.debug(Messages.getMessage("popHandler00", "" + result));
            }
        }

        return result;
    }

    boolean processingRef = false;
    public void setProcessingRef(boolean ref) {
        processingRef = ref;
    }
    public boolean isProcessingRef() {
        return processingRef;
    }

    /****************************************************************
     * SAX event handlers
     */
    public void startDocument() throws SAXException {
        // Should never receive this in the midst of a parse.
        if (!doneParsing && (recorder != null))
            recorder.startDocument();
    }

    /**
     * endDocument is invoked at the end of the document.
     */
    public void endDocument() throws SAXException {
        if (debugEnabled) {
            log.debug("Enter: DeserializationContext::endDocument()");
        }
        if (!doneParsing && (recorder != null))
            recorder.endDocument();

        doneParsing = true;

        if (debugEnabled) {
            log.debug("Exit: DeserializationContext::endDocument()");
        }
    }
    /**
     * Return if done parsing document.
     */
    public boolean isDoneParsing() {return doneParsing;}

    /** Record the current set of prefix mappings in the nsMappings table.
     *
     * !!! We probably want to have this mapping be associated with the
     *     MessageElements, since they may potentially need access to them
     *     long after the end of the prefix mapping here.  (example:
     *     when we need to record a long string of events scanning forward
     *     in the document to find an element with a particular ID.)
     */
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException
    {
        if (debugEnabled) {
            log.debug("Enter: DeserializationContext::startPrefixMapping(" + prefix + ", " + uri + ")");
        }

        if (!doneParsing && (recorder != null)) {
            recorder.startPrefixMapping(prefix, uri);
        }

        if (startOfMappingsPos == -1) {
            namespaces.push();
            startOfMappingsPos = getCurrentRecordPos();
        }

        if (prefix != null) {
            namespaces.add(uri, prefix);
        } else {
            namespaces.add(uri, "");
        }

        if (!haveSeenSchemaNS && msgContext != null) {
            // If we haven't yet seen a schema namespace, check if this
            // is one.  If so, set the SchemaVersion appropriately.
            // Hopefully the schema def is on the outermost element so we
            // get this over with quickly.
            for (int i = 0; !haveSeenSchemaNS && i < schemaVersions.length;
                 i++) {
                SchemaVersion schemaVersion = schemaVersions[i];
                if (uri.equals(schemaVersion.getXsdURI()) ||
                        uri.equals(schemaVersion.getXsiURI())) {
                    msgContext.setSchemaVersion(schemaVersion);
                    haveSeenSchemaNS = true;
                }
            }
        }

        if (topHandler != null) {
            topHandler.startPrefixMapping(prefix, uri);
        }

        if (debugEnabled) {
            log.debug("Exit: DeserializationContext::startPrefixMapping()");
        }
    }

    public void endPrefixMapping(String prefix)
        throws SAXException
    {
        if (debugEnabled) {
            log.debug("Enter: DeserializationContext::endPrefixMapping(" + prefix + ")");
        }

        if (!doneParsing && (recorder != null)) {
            recorder.endPrefixMapping(prefix);
        }

        if (topHandler != null) {
            topHandler.endPrefixMapping(prefix);
        }

        if (debugEnabled) {
            log.debug("Exit: DeserializationContext::endPrefixMapping()");
        }
    }

    public void setDocumentLocator(Locator locator)
    {
        if (!doneParsing && (recorder != null)) {
            recorder.setDocumentLocator(locator);
        }
        this.locator = locator;
    }

    public Locator getDocumentLocator() {
        return locator;
    }

    public void characters(char[] p1, int p2, int p3) throws SAXException {
        if (!doneParsing && (recorder != null)) {
            recorder.characters(p1, p2, p3);
        }
        if (topHandler != null) {
            topHandler.characters(p1, p2, p3);
        }
    }

    public void ignorableWhitespace(char[] p1, int p2, int p3) throws SAXException {
        if (!doneParsing && (recorder != null)) {
            recorder.ignorableWhitespace(p1, p2, p3);
        }
        if (topHandler != null) {
            topHandler.ignorableWhitespace(p1, p2, p3);
        }
    }

    public void processingInstruction(String p1, String p2) throws SAXException {
        // must throw an error since SOAP 1.1 doesn't allow
        // processing instructions anywhere in the message
        throw new SAXException(Messages.getMessage("noInstructions00"));
    }

    public void skippedEntity(String p1) throws SAXException {
        if (!doneParsing && (recorder != null)) {
            recorder.skippedEntity(p1);
        }
        topHandler.skippedEntity(p1);
    }

    /**
     * startElement is called when an element is read.  This is the big work-horse.
     *
     * This guy also handles monitoring the recording depth if we're recording
     * (so we know when to stop).
     */
    public void startElement(String namespace, String localName,
                             String qName, Attributes attributes)
        throws SAXException
    {
        if (debugEnabled) {
            log.debug("Enter: DeserializationContext::startElement(" + namespace + ", " + localName + ")");
        }

        if (attributes == null || attributes.getLength() == 0) {
            attributes = NullAttributes.singleton;
        } else {
            attributes = new AttributesImpl(attributes);

            SOAPConstants soapConstants = getSOAPConstants();
            if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
                if (attributes.getValue(soapConstants.getAttrHref()) != null &&
                    attributes.getValue(Constants.ATTR_ID) != null) {

                    AxisFault fault = new AxisFault(Constants.FAULT_SOAP12_SENDER,
                        null, Messages.getMessage("noIDandHREFonSameElement"), null, null, null);

                    throw new SAXException(fault);

                }
            }

        }

        SOAPHandler nextHandler = null;

        String prefix = "";
        int idx = qName.indexOf(':');
        if (idx > 0) {
            prefix = qName.substring(0, idx);
        }

        if (topHandler != null) {
            nextHandler = topHandler.onStartChild(namespace,
                                                       localName,
                                                       prefix,
                                                       attributes,
                                                       this);
        }

        if (nextHandler == null) {
            nextHandler = new SOAPHandler();
        }

        pushElementHandler(nextHandler);

        nextHandler.startElement(namespace, localName, prefix,
                                 attributes, this);

        if (!doneParsing && (recorder != null)) {
            recorder.startElement(namespace, localName, qName,
                                  attributes);
            if (!doneParsing) {
                curElement.setContentsIndex(recorder.getLength());
            }
        }

        if (startOfMappingsPos != -1) {
            startOfMappingsPos = -1;
        } else {
            // Push an empty frame if there are no mappings
            namespaces.push();
        }

        if (debugEnabled) {
            log.debug("Exit: DeserializationContext::startElement()");
        }
    }

    /**
     * endElement is called at the end tag of an element
     */
    public void endElement(String namespace, String localName, String qName)
        throws SAXException
    {
        if (debugEnabled) {
            log.debug("Enter: DeserializationContext::endElement(" + namespace + ", " + localName + ")");
        }

        if (!doneParsing && (recorder != null)) {
            recorder.endElement(namespace, localName, qName);
        }

        try {
            SOAPHandler handler = popElementHandler();
            handler.endElement(namespace, localName, this);

            if (topHandler != null) {
                topHandler.onEndChild(namespace, localName, this);
            } else {
                // We should be done!
            }

        } finally {
            if (curElement != null) {
                curElement = (MessageElement)curElement.getParentElement();
            }

            namespaces.pop();

            if (debugEnabled) {
                String name = curElement != null ?
                        curElement.getClass().getName() + ":" +
                        curElement.getName() : null;
                log.debug("Popped element stack to " + name);
                log.debug("Exit: DeserializationContext::endElement()");
            }
        }
    }

    /**
     * This class is used to map ID's to an actual value Object or Message
     */
    private static class LocalIDResolver implements IDResolver
    {
        HashMap idMap = null;

        /**
         * Add object associated with id
         */
        public void addReferencedObject(String id, Object referent)
        {
            if (idMap == null) {
                idMap = new HashMap();
            }

            idMap.put(id, referent);
        }

        /**
         * Get object referenced by href
         */
        public Object getReferencedObject(String href)
        {
            if ((idMap == null) || (href == null)) {
                return null;
            }
            return idMap.get(href);
        }
    }

    public void startDTD(java.lang.String name,
                     java.lang.String publicId,
                     java.lang.String systemId)
              throws SAXException
    {
        /* It is possible for a malicious user to send us bad stuff in
           the <!DOCTYPE ../> tag that will cause a denial of service
           Example:
           <?xml version="1.0" ?>
            <!DOCTYPE foobar [
                <!ENTITY x0 "hello">
                <!ENTITY x1 "&x0;&x0;">
                <!ENTITY x2 "&x1;&x1;">
                  ...
                <!ENTITY x99 "&x98;&x98;">
                <!ENTITY x100 "&x99;&x99;">
            ]>
        */
        throw new SAXException(Messages.getMessage("noInstructions00"));
        /* if (recorder != null)
            recorder.startDTD(name, publicId, systemId);
        */
    }

    public void endDTD()
            throws SAXException
    {
        if (recorder != null)
            recorder.endDTD();
    }

    public void startEntity(java.lang.String name)
                 throws SAXException
    {
        if (recorder != null)
            recorder.startEntity(name);
    }

    public void endEntity(java.lang.String name)
               throws SAXException
    {
        if (recorder != null)
            recorder.endEntity(name);
    }

    public void startCDATA()
                throws SAXException
    {
        if (recorder != null)
            recorder.startCDATA();
    }

    public void endCDATA()
              throws SAXException
    {
        if (recorder != null)
            recorder.endCDATA();
    }

    public void comment(char[] ch,
                    int start,
                    int length)
             throws SAXException
    {
        if (recorder != null)
            recorder.comment(ch, start, length);
    }

    public InputSource resolveEntity(String publicId, String systemId)
    {
        return XMLUtils.getEmptyInputSource();
    }


    /** We only need one instance of this dummy handler to set into the parsers. */
    private static final NullLexicalHandler nullLexicalHandler = new NullLexicalHandler();

    /**
     * It is illegal to set the lexical-handler property to null. To facilitate
     * discarding the heavily loaded instance of DeserializationContextImpl from
     * the SAXParser instance that is kept in the Stack maintained by XMLUtils
     * we use this class.
     */
    private static class NullLexicalHandler implements LexicalHandler {
    	public void startDTD(String arg0, String arg1, String arg2) throws SAXException {}
    	public void endDTD() throws SAXException {}
    	public void startEntity(String arg0) throws SAXException {}
    	public void endEntity(String arg0) throws SAXException {}
    	public void startCDATA() throws SAXException {}
    	public void endCDATA() throws SAXException {}
    	public void comment(char[] arg0, int arg1, int arg2) throws SAXException {}
    }
}


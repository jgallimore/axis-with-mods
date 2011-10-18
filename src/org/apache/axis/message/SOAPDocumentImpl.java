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

import javax.xml.namespace.QName;
import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.SOAPPart;
import org.apache.axis.utils.Mapping;
import org.apache.axis.utils.XMLUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

/**
 * SOAPDcoumentImpl implements the Document API for SOAPPART. At the moment, it
 * again delgate the XERCES DOM Implementation Here is my argument on it: I
 * guess that there is 3 way to implement this. - fully implement the DOM API
 * here myself. => This is too much and duplicated work. - extends XERCES
 * Implementation => this makes we are fixed to one Implementation - choose
 * delgate depends on the user's parser preference => This is the practically
 * best solution I have now
 * 
 * @author Heejune Ahn (cityboy@tmax.co.kr)
 *  
 */

public class SOAPDocumentImpl
implements org.w3c.dom.Document, java.io.Serializable {

    // Depending on the user's parser preference
    protected Document delegate = null;
    protected SOAPPart soapPart = null;

    /**
     * Construct the Document
     * 
     * @param sp the soap part
     */
    public SOAPDocumentImpl(SOAPPart sp) {
        try {
            delegate = XMLUtils.newDocument();
        } catch (ParserConfigurationException e) {
            // Do nothing
        }
        soapPart = sp;
    }

    /**
     * @todo : link with SOAP
     * 
     * @return
     */
    public DocumentType getDoctype() {
        return delegate.getDoctype();
    }

    public DOMImplementation getImplementation() {
        return delegate.getImplementation();
    }

    /**
     * should not be called, the method will be handled in SOAPPart
     * 
     * @return
     */
    public Element getDocumentElement() {
        return soapPart.getDocumentElement();
    }

    /**
     * based on the tagName, we will make different kind SOAP Elements Instance
     * Is really we can determine the Type by the Tagname???
     * 
     * @todo : verify this method
     * 
     * @param tagName
     * @return @throws
     *         DOMException
     */

    public org.w3c.dom.Element createElement(String tagName)
    throws DOMException {
        int index = tagName.indexOf(":");
        String prefix, localname;
        if (index < 0) {
            prefix = "";
            localname = tagName;
        } else {
            prefix = tagName.substring(0, index);
            localname = tagName.substring(index + 1);
        }

        try {
            SOAPEnvelope soapenv =
                (org.apache.axis.message.SOAPEnvelope) soapPart.getEnvelope();
            if (soapenv != null) {
                if (tagName.equalsIgnoreCase(Constants.ELEM_ENVELOPE))
                    new SOAPEnvelope();
                if (tagName.equalsIgnoreCase(Constants.ELEM_HEADER))
                    return new SOAPHeader(soapenv, soapenv.getSOAPConstants());
                if (tagName.equalsIgnoreCase(Constants.ELEM_BODY))
                    return new SOAPBody(soapenv, soapenv.getSOAPConstants());
                if (tagName.equalsIgnoreCase(Constants.ELEM_FAULT))
                    return new SOAPEnvelope();
                if (tagName.equalsIgnoreCase(Constants.ELEM_FAULT_DETAIL))
                    return new SOAPFault(new AxisFault(tagName));
                else {
                    return new MessageElement("", prefix, localname);
                }
            } else {
                return new MessageElement("", prefix, localname);
            }

        } catch (SOAPException se) {
            throw new DOMException(DOMException.INVALID_STATE_ERR, "");
        }
    }

    /**
     * 
     * Creates an empty <code>DocumentFragment</code> object. @todo not
     * implemented yet
     * 
     * @return A new <code>DocumentFragment</code>.
     */
    public DocumentFragment createDocumentFragment() {
        return delegate.createDocumentFragment();
    }
    /**
     * Creates a <code>Text</code> node given the specified string.
     * 
     * @param data
     *            The data for the node.
     * @return The new <code>Text</code> object.
     */
    public org.w3c.dom.Text createTextNode(String data) {
        org.apache.axis.message.Text me =
            new org.apache.axis.message.Text(delegate.createTextNode(data));
        me.setOwnerDocument(soapPart);
        return me;

    }

    /**
     * Creates a <code>Comment</code> node given the specified string.
     * 
     * @param data
     *            The data for the node.
     * @return The new <code>Comment</code> object.
     */
    public Comment createComment(String data) {
        return new org.apache.axis.message.CommentImpl(data);
    }

    /**
     * Creates a <code>CDATASection</code> node whose value is the specified
     * string.
     * 
     * @param data
     *            The data for the <code>CDATASection</code> contents.
     * @return The new <code>CDATASection</code> object.
     * @exception DOMException
     *                NOT_SUPPORTED_ERR: Raised if this document is an HTML
     *                document.
     */
    public CDATASection createCDATASection(String data) throws DOMException {
        return new CDATAImpl(data);
    }

    /**
     * Creates a <code>ProcessingInstruction</code> node given the specified
     * name and data strings.
     * 
     * @param target
     *            The target part of the processing instruction.
     * @param data
     *            The data for the node.
     * @return The new <code>ProcessingInstruction</code> object.
     * @exception DOMException
     *                INVALID_CHARACTER_ERR: Raised if the specified target
     *                contains an illegal character. <br>NOT_SUPPORTED_ERR:
     *                Raised if this document is an HTML document.
     */
    public ProcessingInstruction createProcessingInstruction(
            String target,
            String data)
    throws DOMException {
        throw new java.lang.UnsupportedOperationException(
        "createProcessingInstruction");
    }

    /**
     * @todo: How Axis will maintain the Attribute representation ?
     */
    public Attr createAttribute(String name) throws DOMException {
        return delegate.createAttribute(name);
    }

    /**
     * @param name
     * @return @throws
     *         DOMException
     */
    public EntityReference createEntityReference(String name)
    throws DOMException {
        throw new java.lang.UnsupportedOperationException(
        "createEntityReference");
    }

    // implemented by yoonforh 2004-07-30 02:48:50
    public Node importNode(Node importedNode, boolean deep) throws DOMException {
    	Node targetNode = null;
    
    	int type = importedNode.getNodeType();
    	switch (type) {
    	case ELEMENT_NODE :
    	    Element el = (Element) importedNode;
    	    if (deep) {
        		targetNode = new SOAPBodyElement(el);
        		break;
    	    }
    
    	    SOAPBodyElement target = new SOAPBodyElement();
    	    org.w3c.dom.NamedNodeMap attrs = el.getAttributes();
    	    for (int i = 0; i < attrs.getLength(); i++) {
        		org.w3c.dom.Node att = attrs.item(i);
        		if (att.getNamespaceURI() != null &&
        		    att.getPrefix() != null &&
        		    att.getNamespaceURI().equals(Constants.NS_URI_XMLNS) &&
        		    att.getPrefix().equals("xmlns")) {
        		    Mapping map = new Mapping(att.getNodeValue(), att.getLocalName());
        		    target.addMapping(map);
        		}
        		if (att.getLocalName() != null) {
        		    target.addAttribute(att.getPrefix(),
        					att.getNamespaceURI(),
        					att.getLocalName(),
        					att.getNodeValue());
        		} else if (att.getNodeName() != null) {
        		    target.addAttribute(att.getPrefix(),
        					att.getNamespaceURI(),
        					att.getNodeName(),
        					att.getNodeValue());
        		}
    	    }
    
    	    if (el.getLocalName() == null) {
    	        target.setName(el.getNodeName());
    	    } else {
    	        target.setQName(new QName(el.getNamespaceURI(), el.getLocalName()));
    	    }
    	    targetNode = target;
    	    break;
    
    	case ATTRIBUTE_NODE :
    	    if (importedNode.getLocalName() == null) {
    	        targetNode = createAttribute(importedNode.getNodeName());
    	    } else {
    	        targetNode = createAttributeNS(importedNode.getNamespaceURI(),
    					       importedNode.getLocalName());
    	    }
    	    break;
    
    	case TEXT_NODE :
    	    targetNode = createTextNode(importedNode.getNodeValue());
    	    break;
    
    	case CDATA_SECTION_NODE :
    	    targetNode = createCDATASection(importedNode.getNodeValue());
    	    break;
    
    	case COMMENT_NODE :
    	    targetNode = createComment(importedNode.getNodeValue());
    	    break;
    
    	case DOCUMENT_FRAGMENT_NODE :
    	    targetNode = createDocumentFragment();
    	    if (deep) {
        		org.w3c.dom.NodeList children = importedNode.getChildNodes();
        		for (int i = 0; i < children.getLength(); i++){
        		    targetNode.appendChild(importNode(children.item(i), true));
        		}
    	    }
    	    break;
    
    	case ENTITY_REFERENCE_NODE :
    	    targetNode = createEntityReference(importedNode.getNodeName());
    	    break;
    
    	case PROCESSING_INSTRUCTION_NODE :
    	    ProcessingInstruction pi = (ProcessingInstruction) importedNode;
    	    targetNode = createProcessingInstruction(pi.getTarget(), pi.getData());
    	    break;
    
    	case ENTITY_NODE :
    	    // TODO : ...
    	    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Entity nodes are not supported.");
    
    	case NOTATION_NODE :
    	    // TODO : any idea?
    	    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Notation nodes are not supported.");
    
    	case DOCUMENT_TYPE_NODE :
    	    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "DocumentType nodes cannot be imported.");
    
    	case DOCUMENT_NODE :
    	    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Document nodes cannot be imported.");
    
    	default :
    	    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Node type (" + type + ") cannot be imported.");
    	}
    
    	return targetNode;
    }

    /**
     * Return SOAPElements (what if they want SOAPEnvelope or Header/Body?)
     * 
     * @param namespaceURI
     * @param qualifiedName
     * @return @throws
     *         DOMException
     */
    public Element createElementNS(String namespaceURI, String qualifiedName)
    throws DOMException {
        org.apache.axis.soap.SOAPConstants soapConstants = null;
        if (Constants.URI_SOAP11_ENV.equals(namespaceURI)) {
            soapConstants = org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS;
        } else if (Constants.URI_SOAP12_ENV.equals(namespaceURI)) {
            soapConstants = org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS;
        }

        // For special SOAP Element
        MessageElement me = null;
        if (soapConstants != null) {
            if (qualifiedName.equals(Constants.ELEM_ENVELOPE)) {
                // TODO: confirm SOAP 1.1!
                me = new SOAPEnvelope(soapConstants); 
            } else if (qualifiedName.equals(Constants.ELEM_HEADER)) {
                me = new SOAPHeader(null, soapConstants);
                // Dummy SOAPEnv required?
            } else if (qualifiedName.equals(Constants.ELEM_BODY)) {
                me = new SOAPBody(null, soapConstants);
            } else if (qualifiedName.equals(Constants.ELEM_FAULT)) {
                me = null;
            } else if (qualifiedName.equals(Constants.ELEM_FAULT_DETAIL)) {
                // TODO:
                me = null;
            } else {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                "No such Localname for SOAP URI");
            }
            // TODO:
            return null;
            // general Elements
        } else {
            me = new MessageElement(namespaceURI, qualifiedName);
        }

        if (me != null)
            me.setOwnerDocument(soapPart);

        return me;

    }

    /**
     * Attribute is not particularly dealt with in SAAJ.
     *  
     */
    public Attr createAttributeNS(String namespaceURI, String qualifiedName)
    throws DOMException {
        return delegate.createAttributeNS(namespaceURI, qualifiedName);
    }

    /**
     * search the SOAPPart in order of SOAPHeader and SOAPBody for the
     * requested Element name
     *  
     */
    public NodeList getElementsByTagNameNS(
            String namespaceURI,
            String localName) {
        try {
        	NodeListImpl list = new NodeListImpl();
            if (soapPart != null) {
                SOAPEnvelope soapEnv =
                    (org.apache.axis.message.SOAPEnvelope) soapPart
                    .getEnvelope();
                SOAPHeader header =
                    (org.apache.axis.message.SOAPHeader) soapEnv.getHeader();
                if (header != null) {
                	list.addNodeList(header.getElementsByTagNameNS(
                            namespaceURI,
                            localName));
                }
                SOAPBody body =
                    (org.apache.axis.message.SOAPBody) soapEnv.getBody();
                if (body != null) {
                	list.addNodeList(body.getElementsByTagNameNS(
                            namespaceURI,
                            localName));
                }
            }
            return list;
        } catch (SOAPException se) {
            throw new DOMException(DOMException.INVALID_STATE_ERR, "");
        }
    }

    /**
     * search the SOAPPart in order of SOAPHeader and SOAPBody for the
     * requested Element name
     *  
     */
    public NodeList getElementsByTagName(String localName) {

        try {
            NodeListImpl list = new NodeListImpl();
            if (soapPart != null) {
                SOAPEnvelope soapEnv =
                    (org.apache.axis.message.SOAPEnvelope) soapPart
                    .getEnvelope();
                SOAPHeader header =
                    (org.apache.axis.message.SOAPHeader) soapEnv.getHeader();
                if (header != null) {
                    list.addNodeList(header.getElementsByTagName(localName));
                }
                SOAPBody body =
                    (org.apache.axis.message.SOAPBody) soapEnv.getBody();
                if (body != null) {
                    list.addNodeList(body.getElementsByTagName(localName));
                }
            }
            return list;
        } catch (SOAPException se) {
            throw new DOMException(DOMException.INVALID_STATE_ERR, "");
        }
    }
    /**
     * Returns the <code>Element</code> whose <code>ID</code> is given by
     * <code>elementId</code>. If no such element exists, returns <code>null</code>.
     * Behavior is not defined if more than one element has this <code>ID</code>.
     * The DOM implementation must have information that says which attributes
     * are of type ID. Attributes with the name "ID" are not of type ID unless
     * so defined. Implementations that do not know whether attributes are of
     * type ID or not are expected to return <code>null</code>.
     * 
     * @param elementId
     *            The unique <code>id</code> value for an element.
     * @return The matching element.
     * @since DOM Level 2
     */
    public Element getElementById(String elementId) {
        return delegate.getElementById(elementId);
    }

    /**
     * Node Implementation
     *  
     */

    public String getNodeName() {
        return null;
    }

    public String getNodeValue() throws DOMException {
        throw new DOMException(
                DOMException.NO_DATA_ALLOWED_ERR,
                "Cannot use TextNode.get in " + this);
    }

    public void setNodeValue(String nodeValue) throws DOMException {
        throw new DOMException(
                DOMException.NO_DATA_ALLOWED_ERR,
                "Cannot use TextNode.set in " + this);
    }

    /**
     * override it in sub-classes
     * 
     * @return
     */
    public short getNodeType() {
        return Node.DOCUMENT_NODE;
    }

    public Node getParentNode() {
        return null;
    }

    public NodeList getChildNodes() {
        try {
            if (soapPart != null) {
                NodeListImpl children = new NodeListImpl();
                children.addNode(soapPart.getEnvelope());
                return children;
            } else {
                return NodeListImpl.EMPTY_NODELIST;
            }
        } catch (SOAPException se) {
            throw new DOMException(DOMException.INVALID_STATE_ERR, "");
        }

    }

    /**
     * Do we have to count the Attributes as node ????
     * 
     * @return
     */
    public Node getFirstChild() {
        try {
            if (soapPart != null)
                return (org.apache.axis.message.SOAPEnvelope) soapPart
                .getEnvelope();
            else
                return null;
        } catch (SOAPException se) {
            throw new DOMException(DOMException.INVALID_STATE_ERR, "");
        }

    }

    /**
     * @return
     */
    public Node getLastChild() {
        try {
            if (soapPart != null)
                return (org.apache.axis.message.SOAPEnvelope) soapPart
                .getEnvelope();
            else
                return null;
        } catch (SOAPException se) {
            throw new DOMException(DOMException.INVALID_STATE_ERR, "");
        }

    }

    public Node getPreviousSibling() {
        return null;
    }
    public Node getNextSibling() {

        return null;
    }

    public NamedNodeMap getAttributes() {
        return null;
    }

    /**
     * 
     * we have to have a link to them...
     */
    public Document getOwnerDocument() {
        return null;
    }

    /**
     */
    public Node insertBefore(Node newChild, Node refChild)
    throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }

    public Node replaceChild(Node newChild, Node oldChild)
    throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }

    public Node removeChild(Node oldChild) throws DOMException {
        try {
            Node envNode;
            if (soapPart != null) {
                envNode = soapPart.getEnvelope();
                if (envNode.equals(oldChild)) {
                    return envNode;
                }
            }
            throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
        } catch (SOAPException se) {
            throw new DOMException(DOMException.INVALID_STATE_ERR, "");
        }
    }

    public Node appendChild(Node newChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }

    public boolean hasChildNodes() {
        try {
            if (soapPart != null) {
                if (soapPart.getEnvelope() != null) {
                    return true;
                }
            }
            return false;
        } catch (SOAPException se) {
            throw new DOMException(DOMException.INVALID_STATE_ERR, "");
        }

    }

    /**
     * @todo: Study it more.... to implement the deep mode correctly.
     *  
     */
    public Node cloneNode(boolean deep) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }

    /**
     * @todo: is it OK to simply call the superclass?
     *  
     */
    public void normalize() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }

    // TODO: fill appropriate features
    private String[] features = { "foo", "bar" };
    private String version = "version 2.0";

    public boolean isSupported(String feature, String version) {
        if (!version.equalsIgnoreCase(version))
            return false;
        else
            return true;
    }

    public String getPrefix() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }
    public void setPrefix(String prefix) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }

    public String getNamespaceURI() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }
    public void setNamespaceURI(String nsURI) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }

    public String getLocalName() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }

    public boolean hasAttributes() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "");
    }
}

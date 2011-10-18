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
import org.apache.axis.description.FaultDesc;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.Messages;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.namespace.QName;
import javax.xml.soap.DetailEntry;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import java.util.List;
import java.util.Locale;
import java.util.Iterator;

/** A Fault body element.
 *
 * @author Sam Ruby (rubys@us.ibm.com)
 * @author Glen Daniels (gdaniels@apache.org)
 * @author Tom Jordahl (tomj@macromedia.com)
 */
public class SOAPFault extends SOAPBodyElement implements javax.xml.soap.SOAPFault
{
    protected AxisFault fault;
    protected String prefix;
    private java.util.Locale locale;
    protected Detail detail = null;
    
    public SOAPFault(String namespace, String localName, String prefix,
                     Attributes attrs, DeserializationContext context)
        throws AxisFault
    {
        super(namespace, localName, prefix, attrs, context);
    }
    
    public SOAPFault(AxisFault fault)
    {
        this.fault = fault;
    }
    
    public void outputImpl(SerializationContext context)
            throws Exception
    {
        SOAPConstants soapConstants = context.getMessageContext() == null ?
                                        SOAPConstants.SOAP11_CONSTANTS :
                                        context.getMessageContext().getSOAPConstants();

        namespaceURI = soapConstants.getEnvelopeURI();
        name = Constants.ELEM_FAULT;
        
        context.registerPrefixForURI(prefix, soapConstants.getEnvelopeURI());
        context.startElement(new QName(this.getNamespaceURI(),
                                       this.getName()),
                             attributes);
        
        // XXX - Can fault be anything but an AxisFault here?
        if (fault instanceof AxisFault) {
            AxisFault axisFault = fault;
            if (axisFault.getFaultCode() != null) {
                // Do this BEFORE starting the element, so the prefix gets
                // registered if needed.
                if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
                    String faultCode = context.qName2String(axisFault.getFaultCode());
                    context.startElement(Constants.QNAME_FAULTCODE_SOAP12, null);
                    context.startElement(Constants.QNAME_FAULTVALUE_SOAP12, null);
                    context.writeSafeString(faultCode);
                    context.endElement();
                    QName[] subcodes = axisFault.getFaultSubCodes();
                    if (subcodes != null) {
                        for (int i = 0; i < subcodes.length; i++) {
                            faultCode = context.qName2String(subcodes[i]);
                            context.startElement(Constants.QNAME_FAULTSUBCODE_SOAP12, null);
                            context.startElement(Constants.QNAME_FAULTVALUE_SOAP12, null);
                            context.writeSafeString(faultCode);
                            context.endElement();
                        }

                        for (int i = 0; i < subcodes.length; i++)
                            context.endElement();

                    }
                    context.endElement();
                } else {
                    String faultCode = context.qName2String(axisFault.getFaultCode());
                    context.startElement(Constants.QNAME_FAULTCODE, null);
                    context.writeSafeString(faultCode);
                    context.endElement();
                }
            }
            
            if (axisFault.getFaultString() != null) {
                if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
                    context.startElement(Constants.QNAME_FAULTREASON_SOAP12, null);
                    AttributesImpl attrs = new AttributesImpl();
                    attrs.addAttribute("http://www.w3.org/XML/1998/namespace", "lang", "xml:lang", "CDATA", "en");
                    context.startElement(Constants.QNAME_TEXT_SOAP12, attrs);
                } else
                    context.startElement(Constants.QNAME_FAULTSTRING, null);
                context.writeSafeString(axisFault.getFaultString());
                context.endElement();
                if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
                    context.endElement();
                }
            }
            
            if (axisFault.getFaultActor() != null) {
                if (soapConstants == SOAPConstants.SOAP12_CONSTANTS)
                    context.startElement(Constants.QNAME_FAULTROLE_SOAP12, null);
                else
                    context.startElement(Constants.QNAME_FAULTACTOR, null);

                context.writeSafeString(axisFault.getFaultActor());
                context.endElement();
            }

            if (axisFault.getFaultNode() != null) {
                if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
                    context.startElement(Constants.QNAME_FAULTNODE_SOAP12, null);
                    context.writeSafeString(axisFault.getFaultNode());
                    context.endElement();
                }
            }
            
            // get the QName for this faults detail element
            QName qname = getFaultQName(fault.getClass(), context);
            if (qname == null && fault.detail != null) {
                qname = getFaultQName(fault.detail.getClass(), context);
            }
            if (qname == null) {
                // not the greatest, but...
                qname = new QName("", "faultData");
            }
            Element[] faultDetails = axisFault.getFaultDetails();
            if (faultDetails != null) {
                if (soapConstants == SOAPConstants.SOAP12_CONSTANTS)
                    context.startElement(Constants.QNAME_FAULTDETAIL_SOAP12, null);
                else
                    context.startElement(Constants.QNAME_FAULTDETAILS, null);

                // Allow the fault to write its data, if any
                axisFault.writeDetails(qname, context);
                // Then output any other elements
                for (int i = 0; i < faultDetails.length; i++) {
                    context.writeDOMElement(faultDetails[i]);
                }

                if (detail!= null) {
                    for (Iterator it = detail.getChildren().iterator(); it.hasNext();) {
                        ((NodeImpl)it.next()).output(context);
                    }
                }
        
                context.endElement();
            }
        }
        
        context.endElement();
    }

    private QName getFaultQName(Class cls, SerializationContext context) {
        QName qname = null;       
        if (! cls.equals(AxisFault.class)) {
            FaultDesc faultDesc = null;
            if (context.getMessageContext() != null) {
                OperationDesc op = context.getMessageContext().getOperation();
                if (op != null) {
                    faultDesc = op.getFaultByClass(cls);
                }
            }
                
            if (faultDesc != null) {
                qname = faultDesc.getQName();
            }
        }
        return qname;
    }

    public AxisFault getFault()
    {
        return fault;
    }
    
    public void setFault(AxisFault fault)
    {
        this.fault = fault;
    }
    
    /**
     * Sets this <CODE>SOAPFaultException</CODE> object with the given
     *   fault code.
     *
     *   <P>Fault codes, which given information about the fault,
     *   are defined in the SOAP 1.1 specification.</P>
     * @param   faultCode a <CODE>String</CODE> giving
     *     the fault code to be set; must be one of the fault codes
     *     defined in the SOAP 1.1 specification
     * @throws  SOAPException if there was an error in
     *     adding the <CODE>faultCode</CODE> to the underlying XML
     *     tree.
     */
    public void setFaultCode(String faultCode) throws SOAPException {
        fault.setFaultCodeAsString(faultCode);
    }
    
    /**
     * Gets the fault code for this <CODE>SOAPFaultException</CODE>
     * object.
     * @return a <CODE>String</CODE> with the fault code
     */
    public String getFaultCode() {
        return fault.getFaultCode().getLocalPart();
    }
    
    /**
     *  Sets this <CODE>SOAPFaultException</CODE> object with the given
     *   fault actor.
     *
     *   <P>The fault actor is the recipient in the message path who
     *   caused the fault to happen.</P>
     * @param   faultActor a <CODE>String</CODE>
     *     identifying the actor that caused this <CODE>
     *     SOAPFaultException</CODE> object
     * @throws  SOAPException  if there was an error in
     *     adding the <CODE>faultActor</CODE> to the underlying XML
     *     tree.
     */
    public void setFaultActor(String faultActor) throws SOAPException {
        fault.setFaultActor(faultActor);
    }
    
    /**
     * Gets the fault actor for this <CODE>SOAPFaultException</CODE>
     * object.
     * @return  a <CODE>String</CODE> giving the actor in the message
     *     path that caused this <CODE>SOAPFaultException</CODE> object
     * @see #setFaultActor(java.lang.String) setFaultActor(java.lang.String)
     */
    public String getFaultActor() {
        return fault.getFaultActor();
    }
    
    /**
     * Sets the fault string for this <CODE>SOAPFaultException</CODE>
     * object to the given string.
     *
     * @param faultString a <CODE>String</CODE>
     *     giving an explanation of the fault
     * @throws  SOAPException  if there was an error in
     *     adding the <CODE>faultString</CODE> to the underlying XML
     *     tree.
     * @see #getFaultString() getFaultString()
     */
    public void setFaultString(String faultString) throws SOAPException {
        fault.setFaultString(faultString);
    }
    
    /**
     * Gets the fault string for this <CODE>SOAPFaultException</CODE>
     * object.
     * @return a <CODE>String</CODE> giving an explanation of the
     *     fault
     */
    public String getFaultString() {
        return fault.getFaultString();
    }
    
    /**
     * Returns the detail element for this <CODE>SOAPFaultException</CODE>
     *   object.
     *
     *   <P>A <CODE>Detail</CODE> object carries
     *   application-specific error information related to <CODE>
     *   SOAPBodyElement</CODE> objects.</P>
     * @return  a <CODE>Detail</CODE> object with
     *     application-specific error information
     */
    public javax.xml.soap.Detail getDetail() {
        List children = this.getChildren();
        if(children==null || children.size()<=0)
            return null;

        // find detail element
        for (int i=0; i < children.size(); i++) {
            Object obj = children.get(i);
            if (obj instanceof javax.xml.soap.Detail) {
                return (javax.xml.soap.Detail) obj;
            }
        }
        return null;
    }
    
    /**
     * Creates a <CODE>Detail</CODE> object and sets it as the
     *   <CODE>Detail</CODE> object for this <CODE>SOAPFaultException</CODE>
     *   object.
     *
     *   <P>It is illegal to add a detail when the fault already
     *   contains a detail. Therefore, this method should be called
     *   only after the existing detail has been removed.</P>
     * @return the new <CODE>Detail</CODE> object
     * @throws  SOAPException  if this
     *     <CODE>SOAPFaultException</CODE> object already contains a valid
     *     <CODE>Detail</CODE> object
     */
    public javax.xml.soap.Detail addDetail() throws SOAPException {
        if(getDetail() != null){
            throw new SOAPException(Messages.getMessage("valuePresent"));
        }
        Detail detail = convertToDetail(fault);
        addChildElement(detail);
        return detail;
    }

    public void setFaultCode(Name faultCodeQName) throws SOAPException {
        String uri = faultCodeQName.getURI();
        String local = faultCodeQName.getLocalName();
        String prefix = faultCodeQName.getPrefix();

        this.prefix = prefix;
        QName qname = new QName(uri,local);
        fault.setFaultCode(qname);
    }

    public Name getFaultCodeAsName() {
        QName qname = fault.getFaultCode();
        String uri = qname.getNamespaceURI();
        String local = qname.getLocalPart();
        return new PrefixedQName(uri, local, prefix);
    }

    public void setFaultString(String faultString, Locale locale) throws SOAPException {
        fault.setFaultString(faultString);
        this.locale = locale;
    }

    public Locale getFaultStringLocale() {
        return locale;
    }

    /**
     * Convert the details in an AxisFault to a Detail object
     *
     * @param fault source of the fault details
     * @return a detail element contructed from the AxisFault details
     * @throws SOAPException
     */
    private Detail convertToDetail(AxisFault fault)
            throws SOAPException
    {
        detail = new Detail();
        Element[] darray = fault.getFaultDetails();
        fault.setFaultDetail(new Element[]{});
        for (int i = 0; i < darray.length; i++)
        {
            Element detailtEntryElem = darray[i];
            DetailEntry detailEntry = detail.addDetailEntry(
                    new PrefixedQName(detailtEntryElem.getNamespaceURI(),
                            detailtEntryElem.getLocalName(), detailtEntryElem.getPrefix()));
            copyChildren(detailEntry, detailtEntryElem);
        }
        return detail;
    }

    /**
     * Copy the children of a DOM element to a SOAPElement.
     *
     * @param soapElement target of the copy
     * @param domElement source for the copy
     * @throws SOAPException
     */
    private static void copyChildren(SOAPElement soapElement, Element domElement)
            throws SOAPException
    {
        org.w3c.dom.NodeList nl = domElement.getChildNodes();
        for (int j = 0; j < nl.getLength(); j++)
        {
            org.w3c.dom.Node childNode = nl.item(j);
            if (childNode.getNodeType() == Node.TEXT_NODE)
            {
                soapElement.addTextNode(childNode.getNodeValue());
                break; // only one text node assmed
            }
            if (childNode.getNodeType() == Node.ELEMENT_NODE)
            {
                String uri = childNode.getNamespaceURI();
                SOAPElement childSoapElement = null;
                if (uri == null)
                {
                    childSoapElement = soapElement.addChildElement(childNode.getLocalName
                            ());
                }
                else
                {
                    childSoapElement = soapElement.addChildElement(
                            childNode.getLocalName(),
                            childNode.getPrefix(), uri);
                }
                copyChildren(childSoapElement, (Element) childNode);
            }
        }
    }
}

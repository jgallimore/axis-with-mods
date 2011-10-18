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

import org.apache.axis.message.SOAPHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.util.Vector;

/**
 * This interface describes the AXIS Deserializer.
 * A compliant implementiation must extend either
 * the AXIS SoapHandler (org.apache.axis.message.SOAPHandler)
 * or the AXIS DeserializerImpl (org.apache.axis.encoding.DeserializerImpl)
 *
 * The DeserializerImpl provides a lot of the default behavior including the
 * support for id/href.  So you may want to try extending it as opposed to
 * extending SoapHandler.
 *
 * An Axis compliant Deserializer must provide one or more
 * of the following methods:
 *
 * public &lt;constructor&gt;(Class javaType, QName xmlType)
 * public &lt;constructo&gt;()
 *
 * This will allow for construction of generic factories that introspect the class
 * to determine how to construct a deserializer.
 * The xmlType, javaType arguments are filled in with the values known by the factory.
 */
public interface Deserializer extends javax.xml.rpc.encoding.Deserializer, Callback {

    /**
     * Get the deserialized value.
     * @return Object representing deserialized value or null
     */
    public Object getValue();

    /**
     * Set the deserialized value.
     * @param value Object representing deserialized value
     */
    public void setValue(Object value);

    /**
     * If the deserializer has component values (like ArrayDeserializer)
     * this method gets the specific component via the hint.
     * The default implementation returns null.
     * @return Object representing deserialized value or null
     */
    public Object getValue(Object hint);

    /**
     * If the deserializer has component values (like ArrayDeserializer)
     * this method sets the specific component via the hint.
     * The default implementation does nothing.
     * @param value Object representing deserialized value or null
     */
    public void setChildValue(Object value, Object hint) throws SAXException;

   /**
     * In some circumstances an element may not have
     * a type attribute, but a default type qname is known from
     * information in the container.  For example,
     * an element of an array may not have a type= attribute,
     * so the default qname is the component type of the array.
     * This method is used to communicate the default type information
     * to the deserializer.
     */
    public void setDefaultType(QName qName);
    public QName getDefaultType();

    /**
     * For deserializers of non-primitives, the value may not be
     * known until later (due to multi-referencing).  In such
     * cases the deserializer registers Target object(s).  When
     * the value is known, the set(value) will be invoked for
     * each Target registered with the Deserializer.  The Target
     * object abstracts the function of setting a target with a
     * value.  See the Target interface for more info.
     * @param target Target
     */
    public void registerValueTarget(Target target);

    /**
     * Get the Value Targets of the Deserializer.
     * @return Vector of Target objects or null
     */
    public Vector getValueTargets();

    /**
     * Remove the Value Targets of the Deserializer.
     */
    public void removeValueTargets() ;

   /**
     * Move someone else's targets to our own (see DeserializationContext)
     *
     * The DeserializationContext only allows one Deserializer to
     * wait for a unknown multi-ref'ed value.  So to ensure
     * that all of the targets are updated, this method is invoked
     * to copy the Target objects to the waiting Deserializer.
     * @param other is the Deserializer to copy targets from.
     */
    public void moveValueTargets(Deserializer other);

    /**
     * Some deserializers (ArrayDeserializer) require
     * all of the component values to be known before the
     * value is complete.
     * (For the ArrayDeserializer this is important because
     * the elements are stored in an ArrayList, and all values
     * must be known before the ArrayList is converted into the
     * expected array.
     *
     * This routine is used to indicate when the components are ready.
     * The default (true) is useful for most Deserializers.
     */
    public boolean componentsReady();

    /**
     * The valueComplete() method is invoked when the
     * end tag of the element is read.  This results
     * in the setting of all registered Targets (see
     * registerValueTarget).
     * Note that the valueComplete() only processes
     * the Targets if componentReady() returns true.
     * So if you override componentReady(), then your
     * specific Deserializer will need to call valueComplete()
     * when your components are ready (See ArrayDeserializer)
     */
    public void valueComplete() throws SAXException;


    /**
     * The following are the SAX specific methods.
     * DeserializationImpl provides default behaviour, which
     * in most cases is appropriate.
     */

    /**
     * This method is invoked when an element start tag is encountered.
     * DeserializerImpl provides default behavior, which involves the following:
     *   - directly handling the deserialization of a nill value
     *   - handling the registration of the id value.
     *   - handling the registration of a fixup if this element is an href.
     *   - calling onStartElement to do the actual deserialization if not nill or href cases.
     * @param namespace is the namespace of the element
     * @param localName is the name of the element
     * @param qName is the prefixed qName of the element
     * @param attributes are the attributes on the element...used to get the type
     * @param context is the DeserializationContext
     *
     * Normally a specific Deserializer (FooDeserializer) should extend DeserializerImpl.
     * Here is the flow that will occur in such cases:
     *   1) DeserializerImpl.startElement(...) will be called and do the id/href/nill stuff.
     *   2) If real deserialization needs to take place DeserializerImpl.onStartElement will be
     *      invoked, which will attempt to install the specific Deserializer (FooDeserializer)
     *   3) The FooDeserializer.startElement(...) will be called to do the Foo specific stuff.
     *      This results in a call to FooDeserializer.onStartElement(...) if startElement was
     *      not overridden.
     *   4) The onChildElement(...) method is called for each child element.  Nothing occurs
     *      if not overridden.  The FooDeserializer.onStartChild(...) method should return
     *      the deserializer for the child element.
     *   5) When the end tag is reached, the endElement(..) method is invoked.  The default
     *      behavior is to handle hrefs/ids, call onEndElement and then call the Deserializer
     *      valueComplete method.
     *
     * So the methods that you potentially want to override are:
     *   onStartElement, onStartChild, componentsReady, set(object, hint)
     *
     * You probably should not override startElement or endElement.
     * If you need specific behaviour at the end of the element consider overriding
     * onEndElement.
     *
     * See the pre-existing Deserializers for more information.
     */
    public void startElement(String namespace, String localName,
                             String qName, Attributes attributes,
                             DeserializationContext context)
        throws SAXException;

    /**
     * This method is invoked after startElement when the element requires
     * deserialization (i.e. the element is not an href and the value is not nil.)
     * DeserializerImpl provides default behavior, which simply
     * involves obtaining a correct Deserializer and plugging its handler.
     * @param namespace is the namespace of the element
     * @param localName is the name of the element
     * @param prefix is the prefix of the element
     * @param attributes are the attributes on the element...used to get the type
     * @param context is the DeserializationContext
     */
    public void onStartElement(String namespace, String localName,
                             String prefix, Attributes attributes,
                             DeserializationContext context)
        throws SAXException;

    /**
     * onStartChild is called on each child element.
     * The default behavior supplied by DeserializationImpl is to do nothing.
     * A specific deserializer may perform other tasks.  For example a
     * BeanDeserializer will construct a deserializer for the indicated
     * property and return it.
     * @param namespace is the namespace of the child element
     * @param localName is the local name of the child element
     * @param prefix is the prefix used on the name of the child element
     * @param attributes are the attributes of the child element
     * @param context is the deserialization context.
     * @return is a Deserializer to use to deserialize a child (must be
     * a derived class of SOAPHandler) or null if no deserialization should
     * be performed.
     */
    public SOAPHandler onStartChild(String namespace, String localName,
                             String prefix, Attributes attributes,
                             DeserializationContext context)
        throws SAXException;

    /**
     * endElement is called when the end element tag is reached.
     * It handles href/id information for multi-ref processing
     * and invokes the valueComplete() method of the deserializer
     * which sets the targets with the deserialized value.
     * @param namespace is the namespace of the child element
     * @param localName is the local name of the child element
     * @param context is the deserialization context
     */
    public void endElement(String namespace, String localName,
                           DeserializationContext context)
        throws SAXException;

   /**
     * onEndElement is called by endElement.  It is not called
     * if the element has an href.
     * @param namespace is the namespace of the child element
     * @param localName is the local name of the child element
     * @param context is the deserialization context
     */
    public void onEndElement(String namespace, String localName,
                           DeserializationContext context)
        throws SAXException;

}



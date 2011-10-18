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
package org.apache.axis.wsdl.symbolTable;

import org.apache.axis.Constants;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.URLHashSet;
import org.apache.axis.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Import;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.http.HTTPBinding;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap.SOAPHeaderFault;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.holders.BooleanHolder;
import javax.xml.rpc.holders.IntHolder;
import javax.xml.rpc.holders.QNameHolder;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * This class represents a table of all of the top-level symbols from a set of WSDL Definitions and
 * DOM Documents:  XML types; WSDL messages, portTypes, bindings, and services.
 * <p/>
 * This symbolTable contains entries of the form <key, value> where key is of type QName and value is
 * of type Vector.  The Vector's elements are all of the objects that have the given QName.  This is
 * necessary since names aren't unique among the WSDL types.  message, portType, binding, service,
 * could all have the same QName and are differentiated merely by type.  SymbolTable contains
 * type-specific getters to bypass the Vector layer:
 * public PortTypeEntry getPortTypeEntry(QName name), etc.
 */
public class SymbolTable {

    // used to cache dervied types
    protected HashMap derivedTypes = new HashMap();

    // Should the contents of imported files be added to the symbol table?

    /** Field addImports */
    private boolean addImports;

    // The actual symbol table.  This symbolTable contains entries of the form
    // <key, value> where key is of type QName and value is of type Vector.  The
    // Vector's elements are all of the objects that have the given QName.  This
    // is necessary since names aren't unique among the WSDL types.  message,
    // portType, binding, service, could all have the same QName and are
    // differentiated merely by type.  SymbolTable contains type-specific
    // getters to bypass the Vector layer:
    // public PortTypeEntry getPortTypeEntry(QName name), etc.

    /** Field symbolTable */
    private HashMap symbolTable = new HashMap();

    // a map of qnames -> Elements in the symbol table

    /** Field elementTypeEntries */
    private final Map elementTypeEntries = new HashMap();

    // an unmodifiable wrapper so that we can share the index with others, safely

    /** Field elementIndex */
    private final Map elementIndex =
            Collections.unmodifiableMap(elementTypeEntries);

    // a map of qnames -> Types in the symbol table

    /** Field typeTypeEntries */
    private final Map typeTypeEntries = new HashMap();

    // an unmodifiable wrapper so that we can share the index with others, safely

    /** Field typeIndex */
    private final Map typeIndex = Collections.unmodifiableMap(typeTypeEntries);

    /**
     * cache of nodes -> base types for complexTypes.  The cache is
     * built on nodes because multiple TypeEntry objects may use the
     * same node.
     */
    protected final Map node2ExtensionBase =
            new HashMap();    // allow friendly access

    /** Field verbose */
    private boolean verbose;

    /** Field quiet */
    protected boolean quiet;

    /** Field btm */
    private BaseTypeMapping btm = null;

    // should we attempt to treat document/literal WSDL as "rpc-style"

    /** Field nowrap */
    private boolean nowrap;

    // Did we encounter wraped mode WSDL

    /** Field wrapped */
    private boolean wrapped = false;

    /** Field ANON_TOKEN */
    public static final String ANON_TOKEN = ">";

    /** Field def */
    private Definition def = null;

    /** Field wsdlURI */
    private String wsdlURI = null;

    /** If this is false, we will "unwrap" literal arrays, generating a plan "String[]" instead
     * of "ArrayOfString" when encountering an element containing a single maxOccurs="unbounded"
     * inner element.
     */
    private boolean wrapArrays;

    Set arrayTypeQNames = new HashSet();

    /** Field elementFormDefaults */
    private final Map elementFormDefaults = new HashMap();
    /**
     * Construct a symbol table with the given Namespaces.
     * 
     * @param btm        
     * @param addImports 
     * @param verbose    
     * @param nowrap     
     */
    public SymbolTable(BaseTypeMapping btm, boolean addImports,
                       boolean verbose, boolean nowrap) {

        this.btm = btm;
        this.addImports = addImports;
        this.verbose = verbose;
        this.nowrap = nowrap;
    }    // ctor

    /**
     * Method isQuiet
     * 
     * @return 
     */
    public boolean isQuiet() {
        return quiet;
    }    

    /**
     * Method setQuiet
     * 
     * @param quiet
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }   

    /**
     * Get the raw symbol table HashMap.
     * 
     * @return 
     */
    public HashMap getHashMap() {
        return symbolTable;
    }    // getHashMap

    /**
     * Get the list of entries with the given QName.  Since symbols can share QNames, this list is
     * necessary.  This list will not contain any more than one element of any given SymTabEntry.
     * 
     * @param qname 
     * @return 
     */
    public Vector getSymbols(QName qname) {
        return (Vector) symbolTable.get(qname);
    }    // get

    /**
     * Get the entry with the given QName of the given class.  If it does not exist, return null.
     * 
     * @param qname 
     * @param cls   
     * @return 
     */
    public SymTabEntry get(QName qname, Class cls) {

        Vector v = (Vector) symbolTable.get(qname);

        if (v == null) {
            return null;
        } else {
            for (int i = 0; i < v.size(); ++i) {
                SymTabEntry entry = (SymTabEntry) v.elementAt(i);

                if (cls.isInstance(entry)) {
                    return entry;
                }
            }

            return null;
        }
    }    // get

    /**
     * Get the type entry for the given qname.
     * 
     * @param qname           
     * @param wantElementType boolean that indicates type or element (for type= or ref=)
     * @return 
     */
    public TypeEntry getTypeEntry(QName qname, boolean wantElementType) {

        if (wantElementType) {
            return getElement(qname);
        } else {
            return getType(qname);
        }
    }    // getTypeEntry

    /**
     * Get the Type TypeEntry with the given QName.  If it doesn't
     * exist, return null.
     * 
     * @param qname 
     * @return 
     */
    public Type getType(QName qname) {
        return (Type) typeTypeEntries.get(qname);
    }    // getType

    /**
     * Get the Element TypeEntry with the given QName.  If it doesn't
     * exist, return null.
     * 
     * @param qname 
     * @return 
     */
    public Element getElement(QName qname) {
        return (Element) elementTypeEntries.get(qname);
    }    // getElement

    /**
     * Get the MessageEntry with the given QName.  If it doesn't exist, return null.
     * 
     * @param qname 
     * @return 
     */
    public MessageEntry getMessageEntry(QName qname) {
        return (MessageEntry) get(qname, MessageEntry.class);
    }    // getMessageEntry

    /**
     * Get the PortTypeEntry with the given QName.  If it doesn't exist, return null.
     * 
     * @param qname 
     * @return 
     */
    public PortTypeEntry getPortTypeEntry(QName qname) {
        return (PortTypeEntry) get(qname, PortTypeEntry.class);
    }    // getPortTypeEntry

    /**
     * Get the BindingEntry with the given QName.  If it doesn't exist, return null.
     * 
     * @param qname 
     * @return 
     */
    public BindingEntry getBindingEntry(QName qname) {
        return (BindingEntry) get(qname, BindingEntry.class);
    }    // getBindingEntry

    /**
     * Get the ServiceEntry with the given QName.  If it doesn't exist, return null.
     * 
     * @param qname 
     * @return 
     */
    public ServiceEntry getServiceEntry(QName qname) {
        return (ServiceEntry) get(qname, ServiceEntry.class);
    }    // getServiceEntry

    /**
     * Get the list of all the XML schema types in the symbol table.  In other words, all entries
     * that are instances of TypeEntry.
     * 
     * @return 
     * @deprecated use specialized get{Element,Type}Index() methods instead
     */
    public Vector getTypes() {

        Vector v = new Vector();

        v.addAll(elementTypeEntries.values());
        v.addAll(typeTypeEntries.values());

        return v;
    }    // getTypes

    /**
     * Return an unmodifiable map of qnames -> Elements in the symbol
     * table.
     * 
     * @return an unmodifiable <code>Map</code> value
     */
    public Map getElementIndex() {
        return elementIndex;
    }

    /**
     * Return an unmodifiable map of qnames -> Elements in the symbol
     * table.
     * 
     * @return an unmodifiable <code>Map</code> value
     */
    public Map getTypeIndex() {
        return typeIndex;
    }

    /**
     * Return the count of TypeEntries in the symbol table.
     * 
     * @return an <code>int</code> value
     */
    public int getTypeEntryCount() {
        return elementTypeEntries.size() + typeTypeEntries.size();
    }

    /**
     * Get the Definition.  The definition is null until
     * populate is called.
     * 
     * @return 
     */
    public Definition getDefinition() {
        return def;
    }    // getDefinition

    /**
     * Get the WSDL URI.  The WSDL URI is null until populate
     * is called, and ONLY if a WSDL URI is provided.
     * 
     * @return 
     */
    public String getWSDLURI() {
        return wsdlURI;
    }    // getWSDLURI

    /**
     * Are we wrapping literal soap body elements.
     * 
     * @return 
     */
    public boolean isWrapped() {
        return wrapped;
    }

    /**
     * Turn on/off element wrapping for literal soap body's.
     * 
     * @param wrapped 
     */
    public void setWrapped(boolean wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Dump the contents of the symbol table.  For debugging purposes only.
     * 
     * @param out 
     */
    public void dump(java.io.PrintStream out) {

        out.println();
        out.println(Messages.getMessage("symbolTable00"));
        out.println("-----------------------");

        Iterator it = symbolTable.values().iterator();

        while (it.hasNext()) {
            Vector v = (Vector) it.next();

            for (int i = 0; i < v.size(); ++i) {
                out.println(v.elementAt(i).getClass().getName());
                out.println(v.elementAt(i));
            }
        }

        out.println("-----------------------");
    }    // dump

    /**
     * Call this method if you have a uri for the WSDL document
     * 
     * @param uri wsdlURI the location of the WSDL file.
     * @throws IOException                  
     * @throws WSDLException                
     * @throws SAXException                 
     * @throws ParserConfigurationException 
     */
    public void populate(String uri)
            throws IOException, WSDLException, SAXException,
            ParserConfigurationException {
        populate(uri, null, null);
    }    // populate

    /**
     * Method populate
     * 
     * @param uri      
     * @param username 
     * @param password 
     * @throws IOException                  
     * @throws WSDLException                
     * @throws SAXException                 
     * @throws ParserConfigurationException 
     */
    public void populate(String uri, String username, String password)
            throws IOException, WSDLException, SAXException,
            ParserConfigurationException {

        if (verbose) {
            System.out.println(Messages.getMessage("parsing00", uri));
        }

        Document doc = XMLUtils.newDocument(uri, username, password);

        this.wsdlURI = uri;

        try {
            File f = new File(uri);

            if (f.exists()) {
                uri = f.toURL().toString();
            }
        } catch (Exception e) {
        }

        populate(uri, doc);
    }    // populate

    /**
     * Call this method if your WSDL document has already been parsed as an XML DOM document.
     * 
     * @param context context This is directory context for the Document.  If the Document were from file "/x/y/z.wsdl" then the context could be "/x/y" (even "/x/y/z.wsdl" would work).  If context is null, then the context becomes the current directory.
     * @param doc     doc This is the XML Document containing the WSDL.
     * @throws IOException                  
     * @throws SAXException                 
     * @throws WSDLException                
     * @throws ParserConfigurationException 
     */
    public void populate(String context, Document doc)
            throws IOException, SAXException, WSDLException,
            ParserConfigurationException {

        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();

        reader.setFeature("javax.wsdl.verbose", verbose);

        this.def = reader.readWSDL(context, doc);

        add(context, def, doc);
    }    // populate

    /**
     * Add the given Definition and Document information to the symbol table (including imported
     * symbols), populating it with SymTabEntries for each of the top-level symbols.  When the
     * symbol table has been populated, iterate through it, setting the isReferenced flag
     * appropriately for each entry.
     * 
     * @param context 
     * @param def     
     * @param doc     
     * @throws IOException                  
     * @throws SAXException                 
     * @throws WSDLException                
     * @throws ParserConfigurationException 
     */
    protected void add(String context, Definition def, Document doc)
            throws IOException, SAXException, WSDLException,
            ParserConfigurationException {

        URL contextURL = (context == null)
                ? null
                : getURL(null, context);

        populate(contextURL, def, doc, null);
        processTypes();
        checkForUndefined();
        populateParameters();
        setReferences(def, doc);    // uses wrapped flag set in populateParameters
    }    // add

    /**
     * Scan the Definition for undefined objects and throw an error.
     * 
     * @param def      
     * @param filename 
     * @throws IOException 
     */
    private void checkForUndefined(Definition def, String filename)
            throws IOException {

        if (def != null) {

            // Bindings
            Iterator ib = def.getBindings().values().iterator();

            while (ib.hasNext()) {
                Binding binding = (Binding) ib.next();

                if (binding.isUndefined()) {
                    if (filename == null) {
                        throw new IOException(
                                Messages.getMessage(
                                        "emitFailtUndefinedBinding01",
                                        binding.getQName().getLocalPart()));
                    } else {
                        throw new IOException(
                                Messages.getMessage(
                                        "emitFailtUndefinedBinding02",
                                        binding.getQName().getLocalPart(), filename));
                    }
                }
            }

            // portTypes
            Iterator ip = def.getPortTypes().values().iterator();

            while (ip.hasNext()) {
                PortType portType = (PortType) ip.next();

                if (portType.isUndefined()) {
                    if (filename == null) {
                        throw new IOException(
                                Messages.getMessage(
                                        "emitFailtUndefinedPort01",
                                        portType.getQName().getLocalPart()));
                    } else {
                        throw new IOException(
                                Messages.getMessage(
                                        "emitFailtUndefinedPort02",
                                        portType.getQName().getLocalPart(), filename));
                    }
                }
            }

            /*
             * tomj: This is a bad idea, faults seem to be undefined
             * / RJB reply:  this MUST be done for those systems that do something with
             * / messages.  Perhaps we have to do an extra step for faults?  I'll leave
             * / this commented for now, until someone uses this generator for something
             * / other than WSDL2Java.
             *           // Messages
             *           Iterator i = def.getMessages().values().iterator();
             *           while (i.hasNext()) {
             *               Message message = (Message) i.next();
             *               if (message.isUndefined()) {
             *                   throw new IOException(
             *                           Messages.getMessage("emitFailtUndefinedMessage01",
             *                                   message.getQName().getLocalPart()));
             *               }
             *           }
             */
        }
    }

    /**
     * Scan the symbol table for undefined types and throw an exception.
     * 
     * @throws IOException 
     */
    private void checkForUndefined() throws IOException {

        Iterator it = symbolTable.values().iterator();

        while (it.hasNext()) {
            Vector v = (Vector) it.next();

            for (int i = 0; i < v.size(); ++i) {
                SymTabEntry entry = (SymTabEntry) v.get(i);

                // Report undefined types
                if (entry instanceof UndefinedType) {
                    QName qn = entry.getQName();

                    // Special case dateTime/timeInstant that changed
                    // from version to version.
                    if ((qn.getLocalPart().equals(
                            "dateTime") && !qn.getNamespaceURI().equals(
                                    Constants.URI_2001_SCHEMA_XSD)) || (qn.getLocalPart().equals(
                                            "timeInstant") && qn.getNamespaceURI().equals(
                                                    Constants.URI_2001_SCHEMA_XSD))) {
                        throw new IOException(
                                Messages.getMessage(
                                        "wrongNamespace00", qn.getLocalPart(),
                                        qn.getNamespaceURI()));
                    }

                    // Check for a undefined XSD Schema Type and throw
                    // an unsupported message instead of undefined
                    if (SchemaUtils.isSimpleSchemaType(qn)) {
                        throw new IOException(
                                Messages.getMessage(
                                        "unsupportedSchemaType00", qn.getLocalPart()));
                    }

                    // last case, its some other undefined thing
                    throw new IOException(
                            Messages.getMessage(
                                    "undefined00", qn.toString()));
                }    // if undefined
                else if (entry instanceof UndefinedElement) {
                    throw new IOException(
                            Messages.getMessage(
                                    "undefinedElem00", entry.getQName().toString()));
                }
            }
        }
    }                // checkForUndefined

    /**
     * Add the given Definition and Document information to the symbol table (including imported
     * symbols), populating it with SymTabEntries for each of the top-level symbols.
     * NOTE:  filename is used only by checkForUndefined so that it can report which WSDL file
     * has the problem.  If we're on the primary WSDL file, then we don't know the name and
     * filename will be null.  But we know the names of all imported files.
     */
    private URLHashSet importedFiles = new URLHashSet();

    /**
     * Method populate
     * 
     * @param context  
     * @param def      
     * @param doc      
     * @param filename 
     * @throws IOException                  
     * @throws ParserConfigurationException 
     * @throws SAXException                 
     * @throws WSDLException                
     */
    private void populate(
            URL context, Definition def, Document doc, String filename)
            throws IOException, ParserConfigurationException, SAXException,
            WSDLException {

        if (doc != null) {
            populateTypes(context, doc);

            if (addImports) {

                // Add the symbols from any xsd:import'ed documents.
                lookForImports(context, doc);
            }
        }

        if (def != null) {
            checkForUndefined(def, filename);

            if (addImports) {

                // Add the symbols from the wsdl:import'ed WSDL documents
                Map imports = def.getImports();
                Object[] importKeys = imports.keySet().toArray();

                for (int i = 0; i < importKeys.length; ++i) {
                    Vector v = (Vector) imports.get(importKeys[i]);

                    for (int j = 0; j < v.size(); ++j) {
                        Import imp = (Import) v.get(j);

                        if (!importedFiles.contains(imp.getLocationURI())) {
                            importedFiles.add(imp.getLocationURI());

                            URL url = getURL(context, imp.getLocationURI());

                            populate(url, imp.getDefinition(),
                                    XMLUtils.newDocument(url.toString()),
                                    url.toString());
                        }
                    }
                }
            }

            populateMessages(def);
            populatePortTypes(def);
            populateBindings(def);
            populateServices(def);
        }
    }    // populate

    /**
     * This is essentially a call to "new URL(contextURL, spec)" with extra handling in case spec is
     * a file.
     * 
     * @param contextURL 
     * @param spec       
     * @return 
     * @throws IOException 
     */
    private static URL getURL(URL contextURL, String spec) throws IOException {

        // First, fix the slashes as windows filenames may have backslashes
        // in them, but the URL class wont do the right thing when we later
        // process this URL as the contextURL.
        String path = spec.replace('\\', '/');

        // See if we have a good URL.
        URL url = null;

        try {

            // first, try to treat spec as a full URL
            url = new URL(contextURL, path);

            // if we are deail with files in both cases, create a url
            // by using the directory of the context URL.
            if ((contextURL != null) && url.getProtocol().equals("file")
                    && contextURL.getProtocol().equals("file")) {
                url = getFileURL(contextURL, path);
            }
        } catch (MalformedURLException me) {

            // try treating is as a file pathname
            url = getFileURL(contextURL, path);
        }

        // Everything is OK with this URL, although a file url constructed
        // above may not exist.  This will be caught later when the URL is
        // accessed.
        return url;
    }    // getURL

    /**
     * Method getFileURL
     * 
     * @param contextURL 
     * @param path       
     * @return 
     * @throws IOException 
     */
    private static URL getFileURL(URL contextURL, String path)
            throws IOException {

        if (contextURL != null) {

            // get the parent directory of the contextURL, and append
            // the spec string to the end.
            String contextFileName = contextURL.getFile();
            URL parent = null;
            File parentFile = new File(contextFileName).getParentFile(); 
            if ( parentFile != null ) {
                parent = parentFile.toURL(); 
            }
            if (parent != null) {
                return new URL(parent, path);
            }
        }

        return new URL("file", "", path);
    }    // getFileURL

    /**
     * Recursively find all xsd:import'ed objects and call populate for each one.
     * 
     * @param context 
     * @param node    
     * @throws IOException                  
     * @throws ParserConfigurationException 
     * @throws SAXException                 
     * @throws WSDLException                
     */
    private void lookForImports(URL context, Node node)
            throws IOException, ParserConfigurationException, SAXException,
            WSDLException {

        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if ("import".equals(child.getLocalName())) {
                NamedNodeMap attributes = child.getAttributes();
                Node namespace = attributes.getNamedItem("namespace");

                // skip XSD import of soap encoding
                if ((namespace != null)
                        && isKnownNamespace(namespace.getNodeValue())) {
                    continue;
                }

                Node importFile = attributes.getNamedItem("schemaLocation");

                if (importFile != null) {
                    URL url = getURL(context, importFile.getNodeValue());

                    if (!importedFiles.contains(url)) {
                        importedFiles.add(url);

                        String filename = url.toString();

                        populate(url, null, XMLUtils.newDocument(filename),
                                filename);
                    }
                }
            }

            lookForImports(context, child);
        }
    }    // lookForImports

    /**
     * Check if this is a known namespace (soap-enc or schema xsd or schema xsi or xml)
     * 
     * @param namespace 
     * @return true if this is a know namespace.
     */
    public boolean isKnownNamespace(String namespace) {

        if (Constants.isSOAP_ENC(namespace)) {
            return true;
        }

        if (Constants.isSchemaXSD(namespace)) {
            return true;
        }

        if (Constants.isSchemaXSI(namespace)) {
            return true;
        }

        if (namespace.equals(Constants.NS_URI_XML)) {
            return true;
        }

        return false;
    }

    /**
     * Populate the symbol table with all of the Types from the Document.
     * 
     * @param context 
     * @param doc     
     * @throws IOException                  
     * @throws SAXException                 
     * @throws WSDLException                
     * @throws ParserConfigurationException 
     */
    public void populateTypes(URL context, Document doc)
            throws IOException, SAXException, WSDLException,
            ParserConfigurationException {
        addTypes(context, doc, ABOVE_SCHEMA_LEVEL);
    }    // populateTypes

    /**
     * Utility method which walks the Document and creates Type objects for
     * each complexType, simpleType, attributeGroup or element referenced or defined.
     * <p/>
     * What goes into the symbol table?  In general, only the top-level types
     * (ie., those just below
     * the schema tag).  But base types and references can
     * appear below the top level.  So anything
     * at the top level is added to the symbol table,
     * plus non-Element types (ie, base and refd)
     * that appear deep within other types.
     */
    private static final int ABOVE_SCHEMA_LEVEL = -1;

    /** Field SCHEMA_LEVEL */
    private static final int SCHEMA_LEVEL = 0;

    /**
     * Method addTypes
     * 
     * @param context 
     * @param node    
     * @param level   
     * @throws IOException                  
     * @throws ParserConfigurationException 
     * @throws WSDLException                
     * @throws SAXException                 
     */
    private void addTypes(URL context, Node node, int level)
            throws IOException, ParserConfigurationException, WSDLException,
            SAXException {

        if (node == null) {
            return;
        }

        // Get the kind of node (complexType, wsdl:part, etc.)
        String localPart = node.getLocalName();

        if (localPart != null) {
            boolean isXSD =
                    Constants.isSchemaXSD(node.getNamespaceURI());

            if (((isXSD && localPart.equals("complexType"))
                    || localPart.equals("simpleType"))) {

                // If an extension or restriction is present,
                // create a type for the reference
                Node re = SchemaUtils.getRestrictionOrExtensionNode(node);

                if ((re != null) && (Utils.getAttribute(re, "base") != null)) {
                    createTypeFromRef(re);
                }

                Node list = SchemaUtils.getListNode(node);
                if (list != null && Utils.getAttribute(list,"itemType") != null) {
                    createTypeFromRef(list);
                }

                Node union = SchemaUtils.getUnionNode(node);
                if (union != null) {
                    QName [] memberTypes = Utils.getMemberTypeQNames(union);
                    if (memberTypes != null) {
                        for (int i=0;i<memberTypes.length;i++) {
                            if (SchemaUtils.isSimpleSchemaType(memberTypes[i]) &&
                                getType(memberTypes[i]) == null) {
                                symbolTablePut(new BaseType(memberTypes[i]));
                            }
                        }
                    }
                }

                // This is a definition of a complex type.
                // Create a Type.
                createTypeFromDef(node, false, false);
            } else if (isXSD && localPart.equals("element")) {

                // Create a type entry for the referenced type
                createTypeFromRef(node);

                // If an extension or restriction is present,
                // create a type for the reference
                Node re = SchemaUtils.getRestrictionOrExtensionNode(node);

                if ((re != null) && (Utils.getAttribute(re, "base") != null)) {
                    createTypeFromRef(re);
                }

                // Create a type representing an element.  (This may
                // seem like overkill, but is necessary to support ref=
                // and element=.
                createTypeFromDef(node, true, level > SCHEMA_LEVEL);
            } else if (isXSD && localPart.equals("attributeGroup")) {

                // bug 23145: support attributeGroup (Brook Richan)
                // Create a type entry for the referenced type
                createTypeFromRef(node);

                // Create a type representing an attributeGroup.
                createTypeFromDef(node, false, level > SCHEMA_LEVEL);
            }  else if (isXSD && localPart.equals("group")) {
                // Create a type entry for the referenced type
                createTypeFromRef(node);
                // Create a type representing an group
                createTypeFromDef(node, false, level > SCHEMA_LEVEL);
            } else if (isXSD && localPart.equals("attribute")) {

                // Create a type entry for the referenced type
                BooleanHolder forElement = new BooleanHolder();
                QName refQName = Utils.getTypeQName(node, forElement,
                        false);

                if ((refQName != null) && !forElement.value) {
                    createTypeFromRef(node);

                    // Get the symbol table entry and make sure it is a simple
                    // type
                    if (refQName != null) {
                        TypeEntry refType = getTypeEntry(refQName, false);

                        if ((refType != null)
                                && (refType instanceof Undefined)) {

                            // Don't know what the type is.
                            // It better be simple so set it as simple
                            refType.setSimpleType(true);
                        } else if ((refType == null)
                                || (!(refType instanceof BaseType)
                                && !refType.isSimpleType())) {

                            // Problem if not simple
                            throw new IOException(
                                    Messages.getMessage(
                                            "AttrNotSimpleType01",
                                            refQName.toString()));
                        }
                    }
                }
                createTypeFromDef(node, true, level > SCHEMA_LEVEL);
            } else if (isXSD && localPart.equals("any")) {

                // Map xsd:any element to special xsd:any "type"
                if (getType(Constants.XSD_ANY) == null) {
                    Type type = new BaseType(Constants.XSD_ANY);

                    symbolTablePut(type);
                }
            } else if (localPart.equals("part")
                    && Constants.isWSDL(node.getNamespaceURI())) {

                // This is a wsdl part.  Create an TypeEntry representing the reference
                createTypeFromRef(node);
            } else if (isXSD && localPart.equals("include")) {
                String includeName = Utils.getAttribute(node, "schemaLocation");

                if (includeName != null) {
                    URL url = getURL(context, includeName);
                    Document includeDoc = XMLUtils.newDocument(url.toString());

                    // Vidyanand : Fix for Bug #15124
                    org.w3c.dom.Element schemaEl =
                            includeDoc.getDocumentElement();

                    if (!schemaEl.hasAttribute("targetNamespace")) {
                        org.w3c.dom.Element parentSchemaEl =
                                (org.w3c.dom.Element) node.getParentNode();

                        if (parentSchemaEl.hasAttribute("targetNamespace")) {

                            // we need to set two things in here
                            // 1. targetNamespace
                            // 2. setup the xmlns=<targetNamespace> attribute
                            String tns =
                                    parentSchemaEl.getAttribute("targetNamespace");

                            schemaEl.setAttribute("targetNamespace", tns);
                            schemaEl.setAttribute("xmlns", tns);
                        }
                    }

                    populate(url, null, includeDoc, url.toString());
                }
            }
        }

        if (level == ABOVE_SCHEMA_LEVEL) {
            if ((localPart != null)
                && localPart.equals("schema")) {
                level = SCHEMA_LEVEL;
                String targetNamespace = ((org.w3c.dom.Element) node).getAttribute("targetNamespace");
                String elementFormDefault = ((org.w3c.dom.Element) node).getAttribute("elementFormDefault");
                if (targetNamespace != null && targetNamespace.length() > 0) {
                    elementFormDefault = (elementFormDefault == null || elementFormDefault.length() == 0) ?
                            "unqualified" : elementFormDefault;
                    if(elementFormDefaults.get(targetNamespace)==null) {
                        elementFormDefaults.put(targetNamespace, elementFormDefault);
                    }
                }
            }
        } else {
            ++level;
        }

        // Recurse through children nodes
        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            addTypes(context, children.item(i), level);
        }
    }    // addTypes

    /**
     * Create a TypeEntry from the indicated node, which defines a type
     * that represents a complexType, simpleType or element (for ref=).
     * 
     * @param node             
     * @param isElement        
     * @param belowSchemaLevel 
     * @throws IOException 
     */
    private void createTypeFromDef(
            Node node, boolean isElement, boolean belowSchemaLevel)
            throws IOException {

        // Get the QName of the node's name attribute value
        QName qName = Utils.getNodeNameQName(node);

        if (qName != null) {

            // If the qname is already registered as a base type,
            // don't create a defining type/element.
            if (!isElement && (btm.getBaseName(qName) != null)) {
                return;
            }

            // If the node has a type or ref attribute, get the
            // qname representing the type
            BooleanHolder forElement = new BooleanHolder();
            QName refQName = Utils.getTypeQName(node, forElement,
                    false);

            if (refQName != null) {

                // Error check - bug 12362
                if (qName.getLocalPart().length() == 0) {
                    String name = Utils.getAttribute(node, "name");

                    if (name == null) {
                        name = "unknown";
                    }

                    throw new IOException(Messages.getMessage("emptyref00",
                            name));
                }

                // Now get the TypeEntry
                TypeEntry refType = getTypeEntry(refQName, forElement.value);

                if (!belowSchemaLevel) {
                    if (refType == null) {
                        throw new IOException(
                                Messages.getMessage(
                                        "absentRef00", refQName.toString(),
                                        qName.toString()));
                    }

                    symbolTablePut(new DefinedElement(qName, refType, node,
                            ""));
                }
            } else {

                // Flow to here indicates no type= or ref= attribute.
                // See if this is an array or simple type definition.
                IntHolder numDims = new IntHolder();
                BooleanHolder underlTypeNillable = new BooleanHolder();

                // If we're supposed to unwrap arrays, supply someplace to put the "inner" QName
                // so we can propagate it into the appropriate metadata container.
                QNameHolder itemQName = wrapArrays ? null : new QNameHolder();
                BooleanHolder forElement2 = new BooleanHolder();

                numDims.value = 0;

                QName arrayEQName = 
                        SchemaUtils.getArrayComponentQName(node,
                                                           numDims,
                                                           underlTypeNillable,
                                                           itemQName,
                                                           forElement2,
                                                           this);

                if (arrayEQName != null) {

                    // Get the TypeEntry for the array element type
                    refQName = arrayEQName;

                    TypeEntry refType = getTypeEntry(refQName, forElement2.value);

                    if (refType == null) {
//                        arrayTypeQNames.add(refQName);

                        // Not defined yet, add one
                        String baseName = btm.getBaseName(refQName);

                        if (baseName != null) {
                            refType = new BaseType(refQName);
                        } else if(forElement2.value) {
                            refType = new UndefinedElement(refQName);
                        } else {
                            refType = new UndefinedType(refQName);
                        }

                        symbolTablePut(refType);
                    }

                    // Create a defined type or element that references refType
                    String dims = "";

                    while (numDims.value > 0) {
                        dims += "[]";

                        numDims.value--;
                    }

                    TypeEntry defType = null;

                    if (isElement) {
                        if (!belowSchemaLevel) {
                            defType =
                               new DefinedElement(qName, refType, node, dims);
                            // Save component type for ArraySerializer
                            defType.setComponentType(arrayEQName);
                            if (itemQName != null)
                                defType.setItemQName(itemQName.value);
                        }
                    } else {
                        defType = new DefinedType(qName, refType, node, dims);
                        // Save component type for ArraySerializer
                        defType.setComponentType(arrayEQName);
                        defType.setUnderlTypeNillable(underlTypeNillable.value);
                        if (itemQName != null)
                            defType.setItemQName(itemQName.value);
                    }

                    if (defType != null) {
                        symbolTablePut(defType);
                    }
                } else {

                    // Create a TypeEntry representing this  type/element
                    String baseName = btm.getBaseName(qName);

                    if (baseName != null) {
                        symbolTablePut(new BaseType(qName));
                    } else {

                        // Create a type entry, set whether it should
                        // be mapped as a simple type, and put it in the
                        // symbol table.
                        TypeEntry te = null;
                        TypeEntry parentType = null;

                        if (!isElement) {
                            te = new DefinedType(qName, node);

                            // check if we are an anonymous type underneath
                            // an element.  If so, we point the refType of the
                            // element to us (the real type).
                            if (qName.getLocalPart().indexOf(ANON_TOKEN) >= 0) {
                                Node parent = node.getParentNode();
                                QName parentQName =
                                        Utils.getNodeNameQName(parent);
                                parentType = getElement(parentQName);
                            }
                        } else {
                            if (!belowSchemaLevel) {
                                te = new DefinedElement(qName, node);
                            }
                        }

                        if (te != null) {
                            if (SchemaUtils.isSimpleTypeOrSimpleContent(node)) {
                                te.setSimpleType(true);
                            }
                            te = (TypeEntry)symbolTablePut(te);

                            if (parentType != null) {
                                parentType.setRefType(te);
                            }
                        }
                    }
                }
            }
        }
    }    // createTypeFromDef

    /**
     * Node may contain a reference (via type=, ref=, or element= attributes) to
     * another type.  Create a Type object representing this referenced type.
     *
     * @param node
     * @throws IOException
     */
    protected void createTypeFromRef(Node node) throws IOException {

        // Get the QName of the node's type attribute value
        BooleanHolder forElement = new BooleanHolder();
        QName qName = Utils.getTypeQName(node, forElement, false);

        if (qName == null || (Constants.isSchemaXSD(qName.getNamespaceURI()) &&
                qName.getLocalPart().equals("simpleRestrictionModel"))) {
            return;
        }

        // Error check - bug 12362
        if (qName.getLocalPart().length() == 0) {
            String name = Utils.getAttribute(node, "name");

            if (name == null) {
                name = "unknown";
            }

            throw new IOException(Messages.getMessage("emptyref00", name));
        }

        // Get Type or Element depending on whether type attr was used.
        TypeEntry type = getTypeEntry(qName, forElement.value);

        // A symbol table entry is created if the TypeEntry is not found
        if (type == null) {

            // See if this is a special QName for collections
            if (qName.getLocalPart().indexOf("[") > 0) {
                QName containedQName = Utils.getTypeQName(node,
                        forElement, true);
                TypeEntry containedTE = getTypeEntry(containedQName,
                        forElement.value);

                if (!forElement.value) {

                    // Case of type and maxOccurs
                    if (containedTE == null) {

                        // Collection Element Type not defined yet, add one.
                        String baseName = btm.getBaseName(containedQName);

                        if (baseName != null) {
                            containedTE = new BaseType(containedQName);
                        } else {
                            containedTE = new UndefinedType(containedQName);
                        }

                        symbolTablePut(containedTE);
                    }
                    boolean wrapped = qName.getLocalPart().endsWith("wrapped");
                    symbolTablePut(new CollectionType(qName, containedTE,
                            node, "[]", wrapped));
                } else {

                    // Case of ref and maxOccurs
                    if (containedTE == null) {
                        containedTE = new UndefinedElement(containedQName);

                        symbolTablePut(containedTE);
                    }

                    symbolTablePut(new CollectionElement(qName,
                            containedTE, node,
                            "[]"));
                }
            } else {

                // Add a BaseType or Undefined Type/Element
                String baseName = btm.getBaseName(qName);

                if (baseName != null) {
                    symbolTablePut(new BaseType(qName));

                    // bugzilla 23145: handle attribute groups
                    // soap/encoding is treated as a "known" schema
                    // so now let's act like we know it
                } else if (qName.equals(Constants.SOAP_COMMON_ATTRS11)) {
                    symbolTablePut(new BaseType(qName));

                    // the 1.1 commonAttributes type contains two attributes
                    // make sure those attributes' types are in the symbol table
                    // attribute name = "id" type = "xsd:ID"
                    if (getTypeEntry(Constants.XSD_ID, false) == null) {
                        symbolTablePut(new BaseType(Constants.XSD_ID));
                    }

                    // attribute name = "href" type = "xsd:anyURI"
                    if (getTypeEntry(Constants.XSD_ANYURI, false) == null) {
                        symbolTablePut(new BaseType(Constants.XSD_ANYURI));
                    }
                } else if (qName.equals(Constants.SOAP_COMMON_ATTRS12)) {
                    symbolTablePut(new BaseType(qName));

                    // the 1.2 commonAttributes type contains one attribute
                    // make sure the attribute's type is in the symbol table
                    // attribute name = "id" type = "xsd:ID"
                    if (getTypeEntry(Constants.XSD_ID, false) == null) {
                        symbolTablePut(new BaseType(Constants.XSD_ID));
                    }
                } else if (qName.equals(Constants.SOAP_ARRAY_ATTRS11)) {
                    symbolTablePut(new BaseType(qName));

                    // the 1.1 arrayAttributes type contains two attributes
                    // make sure the attributes' types are in the symbol table
                    // attribute name = "arrayType" type = "xsd:string"
                    if (getTypeEntry(Constants.XSD_STRING, false) == null) {
                        symbolTablePut(new BaseType(Constants.XSD_STRING));
                    }

                    // attribute name = "offset" type = "soapenc:arrayCoordinate"
                    // which is really an xsd:string
                } else if (qName.equals(Constants.SOAP_ARRAY_ATTRS12)) {
                    symbolTablePut(new BaseType(qName));

                    // the 1.2 arrayAttributes type contains two attributes
                    // make sure the attributes' types are in the symbol table
                    // attribute name = "arraySize" type = "2003soapenc:arraySize"
                    // which is really a hairy beast that is not
                    // supported, yet; so let's just use string
                    if (getTypeEntry(Constants.XSD_STRING, false) == null) {
                        symbolTablePut(new BaseType(Constants.XSD_STRING));
                    }

                    // attribute name = "itemType" type = "xsd:QName"
                    if (getTypeEntry(Constants.XSD_QNAME, false) == null) {
                        symbolTablePut(new BaseType(Constants.XSD_QNAME));
                    }
                } else if (forElement.value == false) {
                    symbolTablePut(new UndefinedType(qName));
                } else {
                    symbolTablePut(new UndefinedElement(qName));
                }
            }
        }
    }    // createTypeFromRef

    /**
     * Populate the symbol table with all of the MessageEntry's from the Definition.
     *
     * @param def
     * @throws IOException
     */
    private void populateMessages(Definition def) throws IOException {

        Iterator i = def.getMessages().values().iterator();

        while (i.hasNext()) {
            Message message = (Message) i.next();
            MessageEntry mEntry = new MessageEntry(message);

            symbolTablePut(mEntry);
        }
    }    // populateMessages

    /**
     * ensures that a message in a <code>&lt;input&gt;</code>, <code>&lt;output&gt;</code>,
     * or <code>&lt;fault&gt;</fault> element in an <code>&lt;operation&gt;</code>
     * element is valid. In particular, ensures that
     * <ol>
     * <li>an attribute <code>message</code> is present (according to the
     * XML Schema for WSDL 1.1 <code>message</code> is <strong>required</strong>
     * <p/>
     * <li>the value of attribute <code>message</code> (a QName) refers to
     * an already defined message
     * </ol>
     * <p/>
     * <strong>Note</strong>: this method should throw a <code>javax.wsdl.WSDLException</code> rather than
     * a <code>java.io.IOException</code>
     *
     * @param message the message object
     * @throws IOException thrown, if the message is not valid
     */
    protected void ensureOperationMessageValid(Message message)
            throws IOException {

        // make sure the message is not null (i.e. there is an
        // attribute 'message ')
        //
        if (message == null) {
            throw new IOException(
                    "<input>,<output>, or <fault> in <operation ..> without attribute 'message' found. Attribute 'message' is required.");
        }

        // make sure the value of the attribute refers to an
        // already defined message
        //
        if (message.isUndefined()) {
            throw new IOException(
                    "<input ..>, <output ..> or <fault ..> in <portType> with undefined message found. message name is '"
                    + message.getQName().toString() + "'");
        }
    }

    /**
     * ensures that an an element <code>&lt;operation&gt;</code> within
     * an element <code>&lt;portType&gt;<code> is valid. Throws an exception
     * if the operation is not valid.
     * <p/>
     * <strong>Note</strong>: this method should throw a <code>javax.wsdl.WSDLException</code>
     * rather than a <code>java.io.IOException</code>
     *
     * @param operation the operation element
     * @throws IOException              thrown, if the element is not valid.
     * @throws IllegalArgumentException thrown, if operation is null
     */
    protected void ensureOperationValid(Operation operation)
            throws IOException {

        if (operation == null) {
            throw new IllegalArgumentException(
                    "parameter 'operation' must not be null");
        }

        Input input = operation.getInput();
        Message message;

        if (input != null) {
            message = input.getMessage();
            if (message == null) {
                throw new IOException(
                        "No 'message' attribute in <input> for operation '" +
                        operation.getName() + "'");
            }
            ensureOperationMessageValid(message);
        }

        Output output = operation.getOutput();

        if (output != null) {
            message = output.getMessage();
            if (message == null) {
                throw new IOException(
                        "No 'message' attribute in <output> for operation '" +
                        operation.getName() + "'");
            }
            ensureOperationMessageValid(output.getMessage());
        }

        Map faults = operation.getFaults();

        if (faults != null) {
            Iterator it = faults.values().iterator();

            while (it.hasNext()) {
                Fault fault = (Fault)it.next();
                message = fault.getMessage();
                if (message == null) {
                    throw new IOException(
                            "No 'message' attribute in <fault> named '" +
                            fault.getName() + "' for operation '" +
                            operation.getName() + "'");
                }
                ensureOperationMessageValid(message);
            }
        }
    }

    /**
     * ensures that an an element <code>&lt;portType&gt;</code>
     * is valid. Throws an exception if the portType is not valid.
     * <p/>
     * <strong>Note</strong>: this method should throw a <code>javax.wsdl.WSDLException</code>
     * rather than a <code>java.io.IOException</code>
     *
     * @param portType the portType element
     * @throws IOException              thrown, if the element is not valid.
     * @throws IllegalArgumentException thrown, if operation is null
     */
    protected void ensureOperationsOfPortTypeValid(PortType portType)
            throws IOException {

        if (portType == null) {
            throw new IllegalArgumentException(
                    "parameter 'portType' must not be null");
        }

        List operations = portType.getOperations();

        // no operations defined ? -> valid according to the WSDL 1.1 schema
        //
        if ((operations == null) || (operations.size() == 0)) {
            return;
        }

        // check operations defined in this portType
        //
        Iterator it = operations.iterator();

        while (it.hasNext()) {
            Operation operation = (Operation) it.next();

            ensureOperationValid(operation);
        }
    }

    /**
     * Populate the symbol table with all of the PortTypeEntry's from the Definition.
     *
     * @param def
     * @throws IOException
     */
    private void populatePortTypes(Definition def) throws IOException {

        Iterator i = def.getPortTypes().values().iterator();

        while (i.hasNext()) {
            PortType portType = (PortType) i.next();

            // If the portType is undefined, then we're parsing a Definition
            // that didn't contain a portType, merely a binding that referred
            // to a non-existent port type.  Don't bother with it.
            if (!portType.isUndefined()) {
                ensureOperationsOfPortTypeValid(portType);

                PortTypeEntry ptEntry = new PortTypeEntry(portType);

                symbolTablePut(ptEntry);
            }
        }
    }    // populatePortTypes

    /**
     * Create the parameters and store them in the bindingEntry.
     *
     * @throws IOException
     */
    private void populateParameters() throws IOException {

        Iterator it = symbolTable.values().iterator();

        while (it.hasNext()) {
            Vector v = (Vector) it.next();

            for (int i = 0; i < v.size(); ++i) {
                if (v.get(i) instanceof BindingEntry) {
                    BindingEntry bEntry = (BindingEntry) v.get(i);

                    // Skip non-soap bindings
                    if (bEntry.getBindingType() != BindingEntry.TYPE_SOAP) {
                        continue;
                    }

                    Binding binding = bEntry.getBinding();
                    Collection bindOperations = bEntry.getOperations();
                    PortType portType = binding.getPortType();
                    HashMap parameters = new HashMap();
                    Iterator operations =
                            portType.getOperations().iterator();

                    // get parameters
                    while (operations.hasNext()) {
                        Operation operation = (Operation) operations.next();

                        // See if the PortType operation has a corresponding
                        // Binding operation and report an error if it doesn't.
                        if (!bindOperations.contains(operation)) {
                            throw new IOException(
                                    Messages.getMessage(
                                            "emitFailNoMatchingBindOperation01",
                                            operation.getName(),
                                            portType.getQName().getLocalPart()));
                        }

                        String namespace =
                                portType.getQName().getNamespaceURI();
                        Parameters parms = getOperationParameters(operation,
                                namespace, bEntry);
                        parameters.put(operation, parms);
                    }

                    bEntry.setParameters(parameters);
                }
            }
        }
    }    // populateParameters

    /**
     * For the given operation, this method returns the parameter info conveniently collated.
     * There is a bit of processing that is needed to write the interface, stub, and skeleton.
     * Rather than do that processing 3 times, it is done once, here, and stored in the
     * Parameters object.
     *
     * @param operation
     * @param namespace
     * @param bindingEntry
     * @return
     * @throws IOException
     */
    public Parameters getOperationParameters(
            Operation operation, String namespace, BindingEntry bindingEntry)
            throws IOException {

        Parameters parameters = new Parameters();

        // The input and output Vectors of Parameters
        Vector inputs = new Vector();
        Vector outputs = new Vector();
        List parameterOrder = operation.getParameterOrdering();

        // Handle parameterOrder="", which is techinically illegal
        if ((parameterOrder != null) && parameterOrder.isEmpty()) {
            parameterOrder = null;
        }

        Input input = operation.getInput();
        Output output = operation.getOutput();

        parameters.mep = operation.getStyle();

        // All input parts MUST be in the parameterOrder list.  It is an error otherwise.
        if (parameterOrder != null && !wrapped) {
            if (input != null) {
                Message inputMsg = input.getMessage();
                Map allInputs = inputMsg.getParts();
                Collection orderedInputs =
                        inputMsg.getOrderedParts(parameterOrder);

                if (allInputs.size() != orderedInputs.size()) {
                    throw new IOException(
                            Messages.getMessage("emitFail00", operation.getName()));
                }
            }
        }

        boolean literalInput = false;
        boolean literalOutput = false;

        if (bindingEntry != null) {
            literalInput = (bindingEntry.getInputBodyType(operation)
                    == Use.LITERAL);
            literalOutput = (bindingEntry.getOutputBodyType(operation)
                    == Use.LITERAL);
        }

        // Collect all the input parameters
        if ((input != null) && (input.getMessage() != null)) {
            getParametersFromParts(inputs,
                    input.getMessage().getOrderedParts(null),
                    literalInput, operation.getName(),
                    bindingEntry);
        }

        // Collect all the output parameters
        if ((output != null) && (output.getMessage() != null)) {
            getParametersFromParts(outputs,
                    output.getMessage().getOrderedParts(null),
                    literalOutput, operation.getName(),
                    bindingEntry);
        }

        if (parameterOrder != null && !wrapped) {

            // Construct a list of the parameters in the parameterOrder list, determining the
            // mode of each parameter and preserving the parameterOrder list.
            for (int i = 0; i < parameterOrder.size(); ++i) {
                String name = (String) parameterOrder.get(i);

                // index in the inputs Vector of the given name, -1 if it doesn't exist.
                int index = getPartIndex(name, inputs);

                // index in the outputs Vector of the given name, -1 if it doesn't exist.
                int outdex = getPartIndex(name, outputs);

                if (index >= 0) {

                    // The mode of this parameter is either in or inout
                    addInishParm(inputs, outputs, index, outdex, parameters,
                            true);
                } else if (outdex >= 0) {
                    addOutParm(outputs, outdex, parameters, true);
                } else {
                    System.err.println(Messages.getMessage("noPart00", name));
                }
            }
        }

        // Some special case logic for JAX-RPC, but also to make things
        // nicer for the user.
        // If we have a single input and output with the same name
        // instead of: void echo(StringHolder inout)
        // Do this:  string echo(string in)
        if (wrapped && (inputs.size() == 1) && (outputs.size() == 1)
                &&
                Utils.getLastLocalPart(((Parameter) inputs.get(0)).getName()).equals(
                Utils.getLastLocalPart(((Parameter) outputs.get(0)).getName()))
                ) {

            // add the input and make sure its a IN not an INOUT
            addInishParm(inputs, null, 0, -1, parameters, false);
        } else {

            // Get the mode info about those parts that aren't in the
            // parameterOrder list. Since they're not in the parameterOrder list,
            // the order is, first all in (and inout) parameters, then all out
            // parameters, in the order they appear in the messages.
            for (int i = 0; i < inputs.size(); i++) {
                Parameter p = (Parameter) inputs.get(i);
                int outdex = getPartIndex(p.getName(), outputs);

                addInishParm(inputs, outputs, i, outdex, parameters, false);
            }
        }

        // Now that the remaining in and inout parameters are collected,
        // determine the status of outputs.  If there is only 1, then it
        // is the return value.  If there are more than 1, then they are
        // out parameters.
        if (outputs.size() == 1) {
            parameters.returnParam = (Parameter) outputs.get(0);

            parameters.returnParam.setMode(Parameter.OUT);

            if (parameters.returnParam.getType() instanceof DefinedElement) {
                parameters.returnParam.setQName(
                        parameters.returnParam.getType().getQName());
            }

            ++parameters.outputs;
        } else {
            for (int i = 0; i < outputs.size(); i++) {
                addOutParm(outputs, i, parameters, false);
            }
        }

        parameters.faults = operation.getFaults();

        // before we return the paramters,
        // make sure we dont have a duplicate name
        Vector used = new Vector(parameters.list.size());
        Iterator i = parameters.list.iterator();

        while (i.hasNext()) {
            Parameter parameter = (Parameter) i.next();
            int count = 2;

            while (used.contains(parameter.getName())) {

                // duplicate, add a suffix and try again
                parameter.setName(parameter.getName()
                        + Integer.toString(count++));
            }

            used.add(parameter.getName());
        }

        return parameters;
    }    // parameters

    /**
     * Return the index of the given name in the given Vector, -1 if it doesn't exist.
     *
     * @param name
     * @param v
     * @return
     */
    private int getPartIndex(String name, Vector v) {
        name = Utils.getLastLocalPart(name);
        for (int i = 0; i < v.size(); i++) {
            String paramName = ((Parameter) v.get(i)).getName();
            paramName = Utils.getLastLocalPart(paramName);
            if (name.equals(paramName)) {
                return i;
            }
        }

        return -1;
    }    // getPartIndex

    /**
     * Add an in or inout parameter to the parameters object.
     *
     * @param inputs
     * @param outputs
     * @param index
     * @param outdex
     * @param parameters
     * @param trimInput
     */
    private void addInishParm(Vector inputs, Vector outputs, int index,
                              int outdex, Parameters parameters,
                              boolean trimInput) {

        Parameter p = (Parameter) inputs.get(index);

        // If this is an element, we want the XML to reflect the element name
        // not the part name.  Same check is made in addOutParam below.
        if (p.getType() instanceof DefinedElement) {
            DefinedElement de = (DefinedElement) p.getType();

            p.setQName(de.getQName());
        }

        // If this is a collection we want the XML to reflect the type in
        // the collection, not foo[unbounded].
        // Same check is made in addOutParam below.
        if (p.getType() instanceof CollectionElement) {
            p.setQName(p.getType().getRefType().getQName());
        }

        // Should we remove the given parameter type/name entries from the Vector?
        if (trimInput) {
            inputs.remove(index);
        }

        // At this point we know the name and type of the parameter, and that it's at least an
        // in parameter.  Now check to see whether it's also in the outputs Vector.  If it is,
        // then it's an inout parameter.
        if (outdex >= 0) {
            Parameter outParam = (Parameter) outputs.get(outdex);

            TypeEntry paramEntry = p.getType();
            TypeEntry outParamEntry = outParam.getType();
//            String paramLastLocalPart = Utils.getLastLocalPart(paramEntry.getQName().getLocalPart());
//            String outParamLastLocalPart = Utils.getLastLocalPart(outParamEntry.getQName().getLocalPart());
            if (paramEntry.equals(outParamEntry)) {
                outputs.remove(outdex);
                p.setMode(Parameter.INOUT);

                ++parameters.inouts;
            }
            /*
            else if (paramLastLocalPart.equals(outParamLastLocalPart)) {
                outputs.remove(outdex);
                p.setMode(Parameter.INOUT);

                ++parameters.inouts;
                if (paramEntry.isBaseType()) {
                    if (paramEntry.getBaseType().equals(outParamEntry.getBaseType())) {
                        outputs.remove(outdex);
                        p.setMode(Parameter.INOUT);

                        ++parameters.inouts;
                    }
                }
                else if (paramEntry.getRefType() != null) {
                    if (paramEntry.getRefType().equals(outParamEntry.getRefType())) {
                        outputs.remove(outdex);
                        p.setMode(Parameter.INOUT);

                        ++parameters.inouts;
                    }
                }
                else {
                    ++parameters.inputs;
                }
            }
        */
            else {

                // If we're here, we have both an input and an output
                // part with the same name but different types.... guess
                // it's not really an inout....
                //
                // throw new IOException(Messages.getMessage("differentTypes00",
                // new String[] { p.getName(),
                // p.getType().getQName().toString(),
                // outParam.getType().getQName().toString()
                // }
                // ));
                // There is some controversy about this, and the specs are
                // a bit vague about what should happen if the types don't
                // agree.  Throwing an error is not correct with document/lit
                // operations, as part names get resused (i.e. "body").
                // See WSDL 1.1 section 2.4.6,
                // WSDL 1.2 working draft 9 July 2002 section 2.3.1
                ++parameters.inputs;
            }
        } else {
            ++parameters.inputs;
        }

        parameters.list.add(p);
    }    // addInishParm

    /**
     * Add an output parameter to the parameters object.
     *
     * @param outputs
     * @param outdex
     * @param parameters
     * @param trim
     */
    private void addOutParm(Vector outputs, int outdex, Parameters parameters,
                            boolean trim) {

        Parameter p = (Parameter) outputs.get(outdex);

        // If this is an element, we want the XML to reflect the element name
        // not the part name.  Same check is made in addInishParam above.
        if (p.getType() instanceof DefinedElement) {
            DefinedElement de = (DefinedElement) p.getType();

            p.setQName(de.getQName());
        }

        // If this is a collection we want the XML to reflect the type in
        // the collection, not foo[unbounded].
        // Same check is made in addInishParam above.
        if (p.getType() instanceof CollectionElement) {
            p.setQName(p.getType().getRefType().getQName());
        }

        if (trim) {
            outputs.remove(outdex);
        }

        p.setMode(Parameter.OUT);

        ++parameters.outputs;

        parameters.list.add(p);
    }    // addOutParm

    /**
     * This method returns a vector containing Parameters which represent
     * each Part (shouldn't we call these "Parts" or something?)
     *
     * This routine does the wrapped doc/lit processing.
     * It is also used for generating Faults, and this really confuses things
     * but we need to do the same processing for the fault messages.
     *
     * This whole method is waaaay too complex.
     * It needs rewriting (for instance, we sometimes new up
     * a Parameter, then ignore it in favor of another we new up.)
     *
     * @param v       The output vector of parameters
     * @param parts   The parts of the message
     * @param literal Are we in a literal operation (or fault)?
     * @param opName  The operation (or fault) name
     * @param bindingEntry The binding for this operation - can be NULL if we are looking at a fault
     * @throws IOException when encountering an error in the WSDL
     */
    public void getParametersFromParts(Vector v,
                                       Collection parts,
                                       boolean literal,
                                       String opName,
                                       BindingEntry bindingEntry)
            throws IOException {

        // Determine if there's only one element.  For wrapped
        // style, we normally only have 1 part which is an
        // element.  But with MIME we could have any number of
        // types along with that single element.  As long as
        // there's only ONE element, and it's the same name as
        // the operation, we can unwrap it.
        int numberOfElements = 0;
        boolean possiblyWrapped = false;
        Iterator i = parts.iterator();

        while (i.hasNext()) {
            Part part = (Part) i.next();

            if (part.getElementName() != null) {
                ++numberOfElements;

                if (part.getElementName().getLocalPart().equals(opName)) {
                    possiblyWrapped = true;
                }
            }
        }

        // Try to sense "wrapped" document literal mode
        // if we haven't been told not to.
        // Criteria:
        // - If there is a single element part,
        // - That part is an element
        // - That element has the same name as the operation
        // - That element has no attributes (check done below)

        if (!nowrap && literal && (numberOfElements == 1) && possiblyWrapped) {
            wrapped = true;
        }

        i = parts.iterator();

        while (i.hasNext()) {
            Parameter param = new Parameter();
            Part part = (Part) i.next();
            QName elementName = part.getElementName();
            QName typeName = part.getTypeName();
            String partName = part.getName();

            // if we are either:
            //   1. encoded
            //   2. literal & not wrapped.
            if (!literal || !wrapped || (elementName == null)) {
                param.setName(partName);

                // Add this type or element name
                if (typeName != null) {
                    param.setType(getType(typeName));
                } else if (elementName != null) {

                    // Just an FYI: The WSDL spec says that for use=encoded
                    // that parts reference an abstract type using the type attr
                    // but we kinda do the right thing here, so let it go.
                    // if (!literal)
                    // error...
                    param.setType(getElement(elementName));
                } else {

                    // no type or element
                    throw new IOException(
                            Messages.getMessage(
                                    "noTypeOrElement00", new String[]{partName,
                                                                      opName}));
                }

                fillParamInfo(param, bindingEntry, opName, partName);
                v.add(param);

                continue;    // next part
            }

            // flow to here means wrapped literal !
            // See if we can map all the XML types to java(?) types
            // if we can, we use these as the types
            Node node = null;
            TypeEntry typeEntry = null;

            if ((typeName != null)
                    && (bindingEntry == null || bindingEntry.getMIMETypes().size() == 0)) {

                // Since we can't (yet?) make the Axis engine generate the right
                // XML for literal parts that specify the type attribute,
                // (unless they're MIME types) abort processing with an
                // error if we encounter this case
                //
                // node = getTypeEntry(typeName, false).getNode();
                String bindingName = (bindingEntry == null)
                        ? "unknown"
                        : bindingEntry.getBinding().getQName().toString();

                throw new IOException(Messages.getMessage("literalTypePart00",
                        new String[]{
                            partName,
                            opName,
                            bindingName}));
            }

            // Get the node which corresponds to the type entry for this
            // element.  i.e.:
            // <part name="part" element="foo:bar"/>
            // ...
            // <schema targetNamespace="foo">
            // <element name="bar"...>  <--- This one
            typeEntry = getTypeEntry(elementName, true);
            node = typeEntry.getNode();

            // Check if this element is of the form:
            // <element name="foo" type="tns:foo_type"/>
            BooleanHolder forElement = new BooleanHolder();
            QName type = Utils.getTypeQName(node, forElement,
                    false);
            if ((type != null) && !forElement.value) {

                // If in fact we have such a type, go get the node that
                // corresponds to THAT definition.
                typeEntry = getTypeEntry(type, false);
                node = typeEntry.getNode();
            }

            Vector vTypes = null;

            // If we have nothing at this point, we're in trouble.
            if (node == null) {
              // If node is null, that means the element in question has no type declaration,
              // therefore is not a wrapper element.
              wrapped = false;
                if (verbose) {
                    System.out.println(Messages.getMessage("cannotDoWrappedMode00", elementName.toString()));
                }
            } else {

                // check for attributes
                if (typeEntry.getContainedAttributes() != null) {
                    // can't do wrapped mode
                    wrapped = false;
                }

                if (!SchemaUtils.isWrappedType(node)) {
                    // mark the type entry as not just literal referenced
                    // This doesn't work, but it may help in the future.
                    // The problem is "wrapped" is a symbol table wide flag,
                    // which means if one operation breaks the rules
                    // implemented in isWrappedType(), then everything goes bad
                    // For example, see bug Axis-1900.
                    typeEntry.setOnlyLiteralReference(false);
                    wrapped = false;
                }

                // Get the nested type entries.
                // TODO - If we are unable to represent any of the types in the
                // element, we need to use SOAPElement/SOAPBodyElement.
                // I don't believe getContainedElementDecl does the right thing yet.
                vTypes = typeEntry.getContainedElements();
            }

            // IF we got the type entries and we didn't find attributes
            // THEN use the things in this element as the parameters
            if ((vTypes != null) && wrapped) {

                // add the elements in this list
                for (int j = 0; j < vTypes.size(); j++) {
                    ElementDecl elem = (ElementDecl) vTypes.elementAt(j);
                    Parameter p = new Parameter();

                    p.setQName(elem.getQName());
                    // If the parameter is a anonymous complex type, the parameter
                    // name should just be the name of the element, not >foo>element
                    String paramName = p.getName();
                    final int gt = paramName.lastIndexOf(ANON_TOKEN);
                    if (gt != 1) {
                        paramName = paramName.substring(gt+1);
                    }
                    p.setName(paramName);
                    p.setType(elem.getType());
                    p.setOmittable(elem.getMinOccursIs0());
                    p.setNillable(elem.getNillable());
                    fillParamInfo(p, bindingEntry, opName, partName);
                    v.add(p);
                }
            } else {

                // - we were unable to get the types OR
                // - we found attributes
                // so we can't use wrapped mode.
                param.setName(partName);

                if (typeName != null) {
                    param.setType(getType(typeName));
                } else if (elementName != null) {
                    param.setType(getElement(elementName));
                }

                fillParamInfo(param, bindingEntry, opName, partName);
                v.add(param);
            }
        }                    // while
    }                        // getParametersFromParts

    /**
     * Set the header information for this paramter
     *
     * @param param        Parameter to modify
     * @param bindingEntry Binding info for this operation/parameter
     * @param opName       the operation we are processing
     * @param partName     the part we are processing
     */
    private void fillParamInfo(Parameter param, BindingEntry bindingEntry,
                               String opName, String partName) {

        // If we don't have a binding, can't do anything
        if (bindingEntry == null)
            return;

        setMIMEInfo(param, bindingEntry.getMIMEInfo(opName, partName));

        boolean isHeader = false;

        // Is this parameter in an Input header?
        if (bindingEntry.isInHeaderPart(opName, partName)) {
            isHeader = true;
            param.setInHeader(true);
        }

        // Is this parameter in an Output header?
        if (bindingEntry.isOutHeaderPart(opName, partName)) {
            isHeader = true;
            param.setOutHeader(true);
        }

        // If this parameter is part of a header, find the binding operation
        // that we are processing and get the QName of the parameter.
        if (isHeader && (bindingEntry.getBinding() != null)) {
            List list = bindingEntry.getBinding().getBindingOperations();

            for (int i = 0; (list != null) && (i < list.size()); i++) {
                BindingOperation operation = (BindingOperation) list.get(i);

                if (operation.getName().equals(opName)) {
                    if (param.isInHeader()) {
                        QName qName = getBindedParameterName(
                                operation.getBindingInput().getExtensibilityElements(),
                                param);

                        if(qName!= null) {
                            param.setQName(qName);
                        }
                    } else if (param.isOutHeader()) {
                        QName qName = getBindedParameterName(
                                operation.getBindingOutput().getExtensibilityElements(),
                                param);

                        if(qName!= null) {
                            param.setQName(qName);
                        }
                    }
                }
            }
        }
    }

    /**
     * Method getBindedParameterName
     *
     * @param elements
     * @param p
     * @return
     */
    private QName getBindedParameterName(List elements, Parameter p) {

        // If the parameter can either be in the message header or in the
        // message body.
        // When it is in the header, there may be a SOAPHeader (soap:header)
        // with its part name. The namespace used is the one of the soap:header.
        // When it is in the body, if there is a SOAPBody with its part name,
        // the namespace used is the one of this soap:body.
        //
        // If the parameter is in the body and there is a soap:body with no parts,
        // its namespace is used for the parameter.
        QName paramName = null;
        String defaultNamespace = null;
        String parameterPartName = p.getName();

        for (Iterator k = elements.iterator(); k.hasNext();) {
            ExtensibilityElement element = (ExtensibilityElement) k.next();

            if (element instanceof SOAPBody) {
                SOAPBody bodyElement = (SOAPBody) element;
                List parts = bodyElement.getParts();

                if ((parts == null) || (parts.size() == 0)) {
                    defaultNamespace = bodyElement.getNamespaceURI();
                } else {
                    boolean found = false;

                    for (Iterator l = parts.iterator(); l.hasNext();) {
                        Object o = l.next();

                        if(o instanceof String) {
                            if (parameterPartName.equals((String)o)) {
                                paramName =
                                        new QName(bodyElement.getNamespaceURI(),
                                                parameterPartName);
                                found = true;
                                break;
                            }
                        }
                    }

                    if (found) {
                        break;
                    }
                }
            } else if (element instanceof SOAPHeader) {
                SOAPHeader headerElement = (SOAPHeader) element;
                String part = headerElement.getPart();

                if (parameterPartName.equals(part)) {
                    paramName = new QName(headerElement.getNamespaceURI(),
                            parameterPartName);
                    break;
                }
            }
        }

        if ((paramName == null) && (!p.isInHeader()) && (!p.isOutHeader())) {
            if (defaultNamespace != null) {
                paramName = new QName(defaultNamespace, parameterPartName);
            } else {
                paramName = p.getQName();
            }
        }

        return paramName;
    }

    /**
     * Set the MIME type.  This can be determine in one of two ways:
     * 1.  From WSDL 1.1 MIME constructs on the binding (passed in);
     * 2.  From AXIS-specific xml MIME types.
     *
     * @param p
     * @param mimeInfo
     */
    private void setMIMEInfo(Parameter p, MimeInfo mimeInfo) {

        // If there is no binding MIME construct (ie., the mimeType parameter is
        // null), then get the MIME type from the AXIS-specific xml MIME type.
        if (mimeInfo == null && p.getType() != null) {
            QName mimeQName = p.getType().getQName();

            if (mimeQName.getNamespaceURI().equals(Constants.NS_URI_XMLSOAP)) {
                if (Constants.MIME_IMAGE.equals(mimeQName)) {
                    mimeInfo = new MimeInfo("image/jpeg", "");
                } else if (Constants.MIME_PLAINTEXT.equals(mimeQName)) {
                    mimeInfo = new MimeInfo("text/plain", "");
                } else if (Constants.MIME_MULTIPART.equals(mimeQName)) {
                    mimeInfo = new MimeInfo("multipart/related", "");
                } else if (Constants.MIME_SOURCE.equals(mimeQName)) {
                    mimeInfo = new MimeInfo("text/xml", "");
                } else if (Constants.MIME_OCTETSTREAM.equals(mimeQName)) {
                    mimeInfo = new MimeInfo("application/octet-stream", "");
                }
            }
        }

        p.setMIMEInfo(mimeInfo);
    }    // setMIMEType

    /**
     * Populate the symbol table with all of the BindingEntry's from the Definition.
     *
     * @param def
     * @throws IOException
     */
    private void populateBindings(Definition def) throws IOException {

        Iterator i = def.getBindings().values().iterator();

        while (i.hasNext()) {
            Binding binding = (Binding) i.next();
            BindingEntry bEntry = new BindingEntry(binding);

            symbolTablePut(bEntry);

            Iterator extensibilityElementsIterator =
                    binding.getExtensibilityElements().iterator();

            while (extensibilityElementsIterator.hasNext()) {
                Object obj = extensibilityElementsIterator.next();

                if (obj instanceof SOAPBinding) {
                    bEntry.setBindingType(BindingEntry.TYPE_SOAP);

                    SOAPBinding sb = (SOAPBinding) obj;
                    String style = sb.getStyle();

                    if ("rpc".equalsIgnoreCase(style)) {
                        bEntry.setBindingStyle(Style.RPC);
                    }
                } else if (obj instanceof HTTPBinding) {
                    HTTPBinding hb = (HTTPBinding) obj;

                    if (hb.getVerb().equalsIgnoreCase("post")) {
                        bEntry.setBindingType(BindingEntry.TYPE_HTTP_POST);
                    } else {
                        bEntry.setBindingType(BindingEntry.TYPE_HTTP_GET);
                    }
                } else if (obj instanceof UnknownExtensibilityElement) {

                    // TODO: After WSDL4J supports soap12, change this code
                    UnknownExtensibilityElement unkElement =
                            (UnknownExtensibilityElement) obj;
                    QName name =
                            unkElement.getElementType();

                    if (name.getNamespaceURI().equals(Constants.URI_WSDL12_SOAP)
                            && name.getLocalPart().equals("binding")) {
                        bEntry.setBindingType(BindingEntry.TYPE_SOAP);

                        String style =
                                unkElement.getElement().getAttribute("style");

                        if ("rpc".equalsIgnoreCase(style)) {
                            bEntry.setBindingStyle(Style.RPC);
                        }
                    }
                }
            }

            // Step through the binding operations, setting the following as appropriate:
            // - hasLiteral
            // - body types
            // - mimeTypes
            // - headers
            HashMap attributes = new HashMap();
            List bindList = binding.getBindingOperations();
            HashMap faultMap = new HashMap();    // name to SOAPFault from WSDL4J

            for (Iterator opIterator = bindList.iterator();
                 opIterator.hasNext();) {
                BindingOperation bindOp =
                        (BindingOperation) opIterator.next();
                Operation operation = bindOp.getOperation();
                BindingInput bindingInput = bindOp.getBindingInput();
                BindingOutput bindingOutput = bindOp.getBindingOutput();
                String opName = bindOp.getName();

                // First, make sure the binding operation matches a portType operation
                String inputName = (bindingInput == null)
                        ? null
                        : bindingInput.getName();
                String outputName = (bindingOutput == null)
                        ? null
                        : bindingOutput.getName();

                if (binding.getPortType().getOperation(
                        opName, inputName, outputName) == null) {
                    throw new IOException(Messages.getMessage("unmatchedOp",
                            new String[]{
                                opName,
                                inputName,
                                outputName}));
                }

                ArrayList faults = new ArrayList();

                // input
                if (bindingInput != null) {
                    if (bindingInput.getExtensibilityElements() != null) {
                        Iterator inIter =
                                bindingInput.getExtensibilityElements().iterator();

                        fillInBindingInfo(bEntry, operation, inIter, faults,
                                true);
                    }
                }

                // output
                if (bindingOutput != null) {
                    if (bindingOutput.getExtensibilityElements() != null) {
                        Iterator outIter =
                                bindingOutput.getExtensibilityElements().iterator();

                        fillInBindingInfo(bEntry, operation, outIter, faults,
                                false);
                    }
                }

                // faults
                faultsFromSOAPFault(binding, bindOp, operation, faults);

                // Add this fault name and info to the map
                faultMap.put(bindOp, faults);

                Use inputBodyType = bEntry.getInputBodyType(operation);
                Use outputBodyType = bEntry.getOutputBodyType(operation);

                // Associate the portType operation that goes with this binding
                // with the body types.
                attributes.put(bindOp.getOperation(),
                        new BindingEntry.OperationAttr(inputBodyType,
                                outputBodyType, faultMap));

                // If the input or output body uses literal, flag the binding as using literal.
                // NOTE:  should I include faultBodyType in this check?
                if ((inputBodyType == Use.LITERAL)
                        || (outputBodyType == Use.LITERAL)) {
                    bEntry.setHasLiteral(true);
                }

                bEntry.setFaultBodyTypeMap(operation, faultMap);
            }    // binding operations

            bEntry.setFaults(faultMap);
        }
    }            // populateBindings

    /**
     * Fill in some binding information:  bodyType, mimeType, header info.
     *
     * @param bEntry
     * @param operation
     * @param it
     * @param faults
     * @param input
     * @throws IOException
     */
    private void fillInBindingInfo(
            BindingEntry bEntry, Operation operation, Iterator it, ArrayList faults, boolean input)
            throws IOException {

        for (; it.hasNext();) {
            Object obj = it.next();

            if (obj instanceof SOAPBody) {
                setBodyType(((SOAPBody) obj).getUse(), bEntry, operation,
                        input);
            } else if (obj instanceof SOAPHeader) {
                SOAPHeader header = (SOAPHeader) obj;

                setBodyType(header.getUse(), bEntry, operation, input);

                // Note, this only works for explicit headers - those whose
                // parts come from messages used in the portType's operation
                // input/output clauses - it does not work for implicit
                // headers - those whose parts come from messages not used in
                // the portType's operation's input/output clauses.  I don't
                // know what we're supposed to emit for implicit headers.
                bEntry.setHeaderPart(operation.getName(), header.getPart(),
                        input
                        ? BindingEntry.IN_HEADER
                        : BindingEntry.OUT_HEADER);

                // Add any soap:headerFault info to the faults array
                Iterator headerFaults = header.getSOAPHeaderFaults().iterator();

                while (headerFaults.hasNext()) {
                    SOAPHeaderFault headerFault =
                            (SOAPHeaderFault) headerFaults.next();

                    faults.add(new FaultInfo(headerFault, this));
                }
            } else if (obj instanceof MIMEMultipartRelated) {
                bEntry.setBodyType(
                        operation,
                        addMIMETypes(
                                bEntry, (MIMEMultipartRelated) obj, operation), input);
            } else if (obj instanceof UnknownExtensibilityElement) {
                UnknownExtensibilityElement unkElement =
                        (UnknownExtensibilityElement) obj;
                QName name =
                        unkElement.getElementType();

                if (name.getNamespaceURI().equals(Constants.URI_DIME_WSDL)
                        && name.getLocalPart().equals("message")) {
                    fillInDIMEInformation(unkElement, input, operation, bEntry);
                }

                // TODO: After WSDL4J supports soap12, change this code
                if (name.getNamespaceURI().equals(Constants.URI_WSDL12_SOAP)
                        && name.getLocalPart().equals("body")) {
                    setBodyType(unkElement.getElement().getAttribute("use"),
                            bEntry, operation, input);
                }

                // TODO: After WSDL4J supports soap12, change this code
                if (name.getNamespaceURI().equals(Constants.URI_WSDL12_SOAP)
                        && name.getLocalPart().equals("header")) {
                    setBodyType(unkElement.getElement().getAttribute("use"),
                            bEntry, operation, input);

                    // Note, this only works for explicit headers - those whose
                    // parts come from messages used in the portType's operation
                    // input/output clauses - it does not work for implicit
                    // headers - those whose parts come from messages not used in
                    // the portType's operation's input/output clauses.  I don't
                    // know what we're supposed to emit for implicit headers.
                    bEntry.setHeaderPart(
                            operation.getName(),
                            unkElement.getElement().getAttribute("part"), input
                            ? BindingEntry.IN_HEADER
                            : BindingEntry.OUT_HEADER);

                    // Add any soap12:headerFault info to the faults array
                    NodeList headerFaults =
                            unkElement.getElement().getChildNodes();

                    for (int i = 0; i < headerFaults.getLength(); i++) {
                        String faultMessage =
                                unkElement.getElement().getAttribute("message");
                        String faultPart =
                                unkElement.getElement().getAttribute("part");
                        String faultUse =
                                unkElement.getElement().getAttribute("use");
                        String faultNamespaceURI =
                                unkElement.getElement().getAttribute("namespace");
                        QName faultMessageQName = null;
                        int sep = faultMessage.indexOf(':');

                        if (sep == -1) {
                            faultMessageQName = new QName(faultMessage);
                        } else {
                            faultMessageQName =
                                    new QName(faultMessage.substring(0, sep),
                                            faultMessage.substring(sep + 1));
                        }

                        faults.add(new FaultInfo(faultMessageQName, faultPart,
                                faultUse, faultNamespaceURI,
                                this));
                    }
                }
            }
        }
    }    // fillInBindingInfo

    /**
     * Fill in DIME information
     *
     * @param unkElement
     * @param input
     * @param operation
     * @param bEntry
     */
    private void fillInDIMEInformation(UnknownExtensibilityElement unkElement,
                                       boolean input, Operation operation,
                                       BindingEntry bEntry) {

        String layout = unkElement.getElement().getAttribute("layout");

        // TODO: what to do with layout info?
        if (layout.equals(Constants.URI_DIME_CLOSED_LAYOUT)) {
        } else if (layout.equals(Constants.URI_DIME_OPEN_LAYOUT)) {
        }

        Map parts = null;

        if (input) {
            parts = operation.getInput().getMessage().getParts();
        } else {
            parts = operation.getOutput().getMessage().getParts();
        }

        if (parts != null) {
            Iterator iterator = parts.values().iterator();

            while (iterator.hasNext()) {
                Part part = (Part) iterator.next();

                if (part != null) {
                    String dims = "";
                    org.w3c.dom.Element element = null;

                    if (part.getTypeName() != null) {
                        TypeEntry partType = getType(part.getTypeName());

                        if (partType.getDimensions().length() > 0) {
                            dims = partType.getDimensions();
                            partType = partType.getRefType();
                        }

                        element = (org.w3c.dom.Element) partType.getNode();
                    } else if (part.getElementName() != null) {
                        TypeEntry partElement =
                                getElement(part.getElementName()).getRefType();

                        element = (org.w3c.dom.Element) partElement.getNode();

                        QName name = getInnerCollectionComponentQName(element);

                        if (name != null) {
                            dims += "[]";
                            partElement = getType(name);
                            element =
                                    (org.w3c.dom.Element) partElement.getNode();
                        } else {
                            name = getInnerTypeQName(element);

                            if (name != null) {
                                partElement = getType(name);
                                element =
                                        (org.w3c.dom.Element) partElement.getNode();
                            }
                        }
                    }

                    if (element != null) {
                        org.w3c.dom.Element e =
                                (org.w3c.dom.Element) XMLUtils.findNode(
                                        element,
                                        new QName(
                                                Constants.URI_DIME_CONTENT, "mediaType"));

                        if (e != null) {
                            String value = e.getAttribute("value");

                            bEntry.setOperationDIME(operation.getName());
                            bEntry.setMIMEInfo(operation.getName(),
                                    part.getName(), value, dims);
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the faults from the soap:fault clause.
     *
     * @param binding
     * @param bindOp
     * @param operation
     * @param faults
     * @throws IOException
     */
    private void faultsFromSOAPFault(
            Binding binding, BindingOperation bindOp, Operation operation, ArrayList faults)
            throws IOException {

        Iterator faultMapIter = bindOp.getBindingFaults().values().iterator();

        for (; faultMapIter.hasNext();) {
            BindingFault bFault = (BindingFault) faultMapIter.next();

            // Set default entry for this fault
            String faultName = bFault.getName();

            // Check to make sure this fault is named
            if ((faultName == null) || (faultName.length() == 0)) {
                throw new IOException(
                        Messages.getMessage(
                                "unNamedFault00", bindOp.getName(),
                                binding.getQName().toString()));
            }

            boolean foundSOAPFault = false;
            String soapFaultUse = "";
            String soapFaultNamespace = "";
            Iterator faultIter =
                    bFault.getExtensibilityElements().iterator();

            for (; faultIter.hasNext();) {
                Object obj = faultIter.next();

                if (obj instanceof SOAPFault) {
                    foundSOAPFault = true;
                    soapFaultUse = ((SOAPFault) obj).getUse();
                    soapFaultNamespace = ((SOAPFault) obj).getNamespaceURI();

                    break;
                } else if (obj instanceof UnknownExtensibilityElement) {

                    // TODO: After WSDL4J supports soap12, change this code
                    UnknownExtensibilityElement unkElement =
                            (UnknownExtensibilityElement) obj;
                    QName name =
                            unkElement.getElementType();

                    if (name.getNamespaceURI().equals(Constants.URI_WSDL12_SOAP)
                            && name.getLocalPart().equals("fault")) {
                        if (unkElement.getElement().getAttribute("use")
                                != null) {
                            soapFaultUse =
                                    unkElement.getElement().getAttribute("use");
                        }

                        if (unkElement.getElement().getAttribute("namespace")
                                != null) {
                            soapFaultNamespace =
                                    unkElement.getElement().getAttribute(
                                            "namespace");
                        }
                    }
                }
            }

            // Check to make sure we have a soap:fault element
            if (!foundSOAPFault) {
                throw new IOException(
                        Messages.getMessage(
                                "missingSoapFault00", faultName, bindOp.getName(),
                                binding.getQName().toString()));
            }

            // TODO error checking:
            // if use=literal, no use of namespace on the soap:fault
            // if use=encoded, no use of element on the part
            // Check this fault to make sure it matches the one
            // in the matching portType Operation
            Fault opFault = operation.getFault(bFault.getName());

            if (opFault == null) {
                throw new IOException(Messages.getMessage("noPortTypeFault",
                        new String[]{
                            bFault.getName(),
                            bindOp.getName(),
                            binding.getQName().toString()}));
            }

            // put the updated entry back in the map
            faults.add(new FaultInfo(opFault, Use.getUse(soapFaultUse),
                    soapFaultNamespace, this));
        }
    }    // faultsFromSOAPFault

    /**
     * Set the body type.
     *
     * @param use
     * @param bEntry
     * @param operation
     * @param input
     */
    private void setBodyType(String use,
                             BindingEntry bEntry,
                             Operation operation,
                             boolean input)
    {

        if (use == null) {
            // Deprecated
            // throw new IOException(Messages.getMessage("noUse",
            //        operation.getName()));
            // for WS-I BP 1.0 R2707.
            // Set default of use to literal.
            use = "literal";
        }

        if (use.equalsIgnoreCase("literal")) {
            bEntry.setBodyType(operation, Use.LITERAL, input);
        }
    }    // setBodyType

    /**
     * Add the parts that are really MIME types as MIME types.
     * A side effect is to return the body Type of the given
     * MIMEMultipartRelated object.
     *
     * @param bEntry
     * @param mpr
     * @param op
     * @return
     * @throws IOException
     */
    private Use addMIMETypes(
            BindingEntry bEntry, MIMEMultipartRelated mpr, Operation op)
            throws IOException {

        Use bodyType = Use.ENCODED;
        List parts = mpr.getMIMEParts();
        Iterator i = parts.iterator();

        while (i.hasNext()) {
            MIMEPart part = (MIMEPart) i.next();
            List elems = part.getExtensibilityElements();
            Iterator j = elems.iterator();

            while (j.hasNext()) {
                Object obj = j.next();

                if (obj instanceof MIMEContent) {
                    MIMEContent content = (MIMEContent) obj;
                    TypeEntry typeEntry = findPart(op, content.getPart());
                    if (typeEntry == null) {
                        throw new RuntimeException(Messages.getMessage("cannotFindPartForOperation00", content.getPart(),
                                op.getName(), content.getType()));
                    }
                    String dims = typeEntry.getDimensions();

                    if ((dims.length() <= 0)
                            && (typeEntry.getRefType() != null)) {
                        Node node = typeEntry.getRefType().getNode();

                        if (getInnerCollectionComponentQName(node) != null) {
                            dims += "[]";
                        }
                    }

                    String type = content.getType();

                    if ((type == null) || (type.length() == 0)) {
                        type = "text/plain";
                    }

                    bEntry.setMIMEInfo(op.getName(), content.getPart(), type,
                            dims);
                } else if (obj instanceof SOAPBody) {
                    String use = ((SOAPBody) obj).getUse();

                    if (use == null) {
                        throw new IOException(
                                Messages.getMessage("noUse", op.getName()));
                    }

                    if (use.equalsIgnoreCase("literal")) {
                        bodyType = Use.LITERAL;
                    }
                } else if (obj instanceof UnknownExtensibilityElement) {

                    // TODO: After WSDL4J supports soap12, change this code
                    UnknownExtensibilityElement unkElement =
                            (UnknownExtensibilityElement) obj;
                    QName name =
                            unkElement.getElementType();

                    if (name.getNamespaceURI().equals(Constants.URI_WSDL12_SOAP)
                            && name.getLocalPart().equals("body")) {
                        String use =
                                unkElement.getElement().getAttribute("use");

                        if (use == null) {
                            throw new IOException(
                                    Messages.getMessage("noUse", op.getName()));
                        }

                        if (use.equalsIgnoreCase("literal")) {
                            bodyType = Use.LITERAL;
                        }
                    }
                }
            }
        }

        return bodyType;
    }    // addMIMETypes

    /**
     * Method findPart
     *
     * @param operation
     * @param partName
     * @return
     */
    private TypeEntry findPart(Operation operation, String partName) {

        Map parts = operation.getInput().getMessage().getParts();
        Iterator iterator = parts.values().iterator();
        TypeEntry part = findPart(iterator, partName);

        if (part == null) {
            parts = operation.getOutput().getMessage().getParts();
            iterator = parts.values().iterator();
            part = findPart(iterator, partName);
        }

        return part;
    }

    /**
     * Method findPart
     *
     * @param iterator
     * @param partName
     * @return
     */
    private TypeEntry findPart(Iterator iterator, String partName) {

        while (iterator.hasNext()) {
            Part part = (Part) iterator.next();

            if (part != null) {
                String typeName = part.getName();

                if (partName.equals(typeName)) {
                    if (part.getTypeName() != null) {
                        return getType(part.getTypeName());
                    } else if (part.getElementName() != null) {
                        return getElement(part.getElementName());
                    }
                }
            }
        }

        return null;
    }

    /**
     * Populate the symbol table with all of the ServiceEntry's from the Definition.
     *
     * @param def
     * @throws IOException
     */

    private void populateServices(Definition def) throws IOException {
    	String originalName = null;
        Iterator i = def.getServices().values().iterator();

        while (i.hasNext()) {
            Service service = (Service) i.next();
            originalName = service.getQName().getLocalPart();
            // do a bit of name validation
            if ((service.getQName() == null)
                    || (service.getQName().getLocalPart() == null)
                    || service.getQName().getLocalPart().equals("")) {
                throw new IOException(Messages.getMessage("BadServiceName00"));
            }

            // behave as though backslashes were never there
            service.setQName(BackslashUtil.getQNameWithBackslashlessLocal(service.getQName()));
            ServiceEntry sEntry = new ServiceEntry(service);
            // we'll need this later when it is time print a backslash escaped service name
            sEntry.setOriginalServiceName(originalName);
            symbolTablePut(sEntry);
            populatePorts(service.getPorts());
        }
    }    // populateServices


    /**
     * populates the symbol table with port elements defined within a &lt;service&gt;
     * element.
     *
     * @param ports a map of name->port pairs (i.e. what is returned by service.getPorts()
     * @throws IOException thrown, if an IO or WSDL error is detected
     * @see javax.wsdl.Service#getPorts()
     * @see javax.wsdl.Port
     */
    private void populatePorts(Map ports) throws IOException {

        if (ports == null) {
            return;
        }

        Iterator it = ports.values().iterator();

        while (it.hasNext()) {
            Port port = (Port) it.next();
            String portName = port.getName();
            Binding portBinding = port.getBinding();

            // make sure there is a port name. The 'name' attribute for WSDL ports is
            // mandatory
            //
            if (portName == null) {

                // REMIND: should rather be a javax.wsdl.WSDLException ?
                throw new IOException(
                        Messages.getMessage("missingPortNameException"));
            }

            // make sure there is a binding for the port. The 'binding' attribute for
            // WSDL ports is mandatory
            //
            if (portBinding == null) {

                // REMIND: should rather be a javax.wsdl.WSDLException ?
                throw new IOException(
                        Messages.getMessage("missingBindingException"));
            }

            // make sure the port name is unique among all port names defined in this
            // WSDL document.
            //
            // NOTE: there's a flaw in com.ibm.wsdl.xml.WSDLReaderImpl#parsePort() and
            // com.ibm.wsdl.xml.WSDLReaderImpl#addPort(). These methods do not enforce
            // the port name exists and is unique. Actually, if two port definitions with
            // the same name exist within the same service element, only *one* port
            // element is present after parsing and the following exception is not thrown.
            //
            // If two ports with the same name exist in different service elements,
            // the exception below is thrown. This is conformant to the WSDL 1.1 spec (sec 2.6)
            // , which states: "The name attribute provides a unique name among all ports
            // defined within in the enclosing WSDL document."
            //
            //
            if (existsPortWithName(new QName(portName))) {

                // REMIND: should rather be a javax.wsdl.WSDLException ?
                throw new IOException(
                        Messages.getMessage("twoPortsWithSameName", portName));
            }

            PortEntry portEntry = new PortEntry(port);

            symbolTablePut(portEntry);
        }
    }

    /**
     * Set each SymTabEntry's isReferenced flag.  The default is false.  If no other symbol
     * references this symbol, then leave it false, otherwise set it to true.
     * (An exception to the rule is that derived types are set as referenced if
     * their base type is referenced.  This is necessary to support generation and
     * registration of derived types.)
     *
     * @param def
     * @param doc
     */
    private void setReferences(Definition def, Document doc) {

        Map stuff = def.getServices();

        if (stuff.isEmpty()) {
            stuff = def.getBindings();

            if (stuff.isEmpty()) {
                stuff = def.getPortTypes();

                if (stuff.isEmpty()) {
                    stuff = def.getMessages();

                    if (stuff.isEmpty()) {
                        for (Iterator i = elementTypeEntries.values().iterator();
                             i.hasNext();) {
                            setTypeReferences((TypeEntry) i.next(), doc, false);
                        }

                        for (Iterator i = typeTypeEntries.values().iterator();
                             i.hasNext();) {
                            setTypeReferences((TypeEntry) i.next(), doc, false);
                        }
                    } else {
                        Iterator i = stuff.values().iterator();

                        while (i.hasNext()) {
                            Message message = (Message) i.next();
                            MessageEntry mEntry =
                                    getMessageEntry(message.getQName());

                            setMessageReferences(mEntry, def, doc, false);
                        }
                    }
                } else {
                    Iterator i = stuff.values().iterator();

                    while (i.hasNext()) {
                        PortType portType = (PortType) i.next();
                        PortTypeEntry ptEntry =
                                getPortTypeEntry(portType.getQName());

                        setPortTypeReferences(ptEntry, null, def, doc);
                    }
                }
            } else {
                Iterator i = stuff.values().iterator();

                while (i.hasNext()) {
                    Binding binding = (Binding) i.next();
                    BindingEntry bEntry = getBindingEntry(binding.getQName());

                    setBindingReferences(bEntry, def, doc);
                }
            }
        } else {
            Iterator i = stuff.values().iterator();

            while (i.hasNext()) {
                Service service = (Service) i.next();
                ServiceEntry sEntry = getServiceEntry(service.getQName());

                setServiceReferences(sEntry, def, doc);
            }
        }
    }    // setReferences

    /**
     * Set the isReferenced flag to true on the given TypeEntry and all
     * SymTabEntries that it refers to.
     *
     * @param entry
     * @param doc
     * @param literal
     */
    private void setTypeReferences(TypeEntry entry, Document doc,
                                   boolean literal) {

        // Check to see if already processed.
        if ((entry.isReferenced() && !literal)
                || (entry.isOnlyLiteralReferenced() && literal)) {
            return;
        }

        if (wrapped) {

            // If this type is ONLY referenced from a literal usage in a binding,
            // then isOnlyLiteralReferenced should return true.
            if (!entry.isReferenced() && literal) {
                entry.setOnlyLiteralReference(true);
            }

            // If this type was previously only referenced as a literal type,
            // but now it is referenced in a non-literal manner, turn off the
            // onlyLiteralReference flag.
            else if (entry.isOnlyLiteralReferenced() && !literal) {
                entry.setOnlyLiteralReference(false);
            }
        }

        // If we don't want to emit stuff from imported files, only set the
        // isReferenced flag if this entry exists in the immediate WSDL file.
        Node node = entry.getNode();

        if (addImports || (node == null) || (node.getOwnerDocument() == doc)) {
            entry.setIsReferenced(true);

            if (entry instanceof DefinedElement) {
                BooleanHolder forElement = new BooleanHolder();
                QName referentName = Utils.getTypeQName(node,
                        forElement, false);

                if (referentName != null) {
                    TypeEntry referent = getTypeEntry(referentName,
                            forElement.value);

                    if (referent != null) {
                        setTypeReferences(referent, doc, literal);
                    }
                }

                // If the Defined Element has an anonymous type,
                // process it with the current literal flag setting.
                QName anonQName =
                        SchemaUtils.getElementAnonQName(entry.getNode());

                if (anonQName != null) {
                    TypeEntry anonType = getType(anonQName);

                    if (anonType != null) {
                        setTypeReferences(anonType, doc, literal);

                        return;
                    }
                }
            }
        }

        HashSet nestedTypes = entry.getNestedTypes(this, true);
        Iterator it = nestedTypes.iterator();

        while (it.hasNext()) {
            TypeEntry nestedType = (TypeEntry) it.next();
            TypeEntry refType = entry.getRefType();

            if (nestedType == null) {
            	continue;
            }

            // If this entry has a referenced type that isn't
            // the same as the nested type
            // AND the OnlyLiteral reference switch is on
            // THEN turn the OnlyLiteral reference switch off.
            // If only someone had put a comment here saying why we do this...
            if ((refType != null)
                    && !refType.equals(nestedType)
                    && nestedType.isOnlyLiteralReferenced()) {
                nestedType.setOnlyLiteralReference(false);
            }

            if (!nestedType.isReferenced()) {

                // setTypeReferences(nestedType, doc, literal);
                if (nestedType != entry) {
                    setTypeReferences(nestedType, doc, false);
                }
            }
        }
    }    // setTypeReferences

    /**
     * Set the isReferenced flag to true on the given MessageEntry and all
     * SymTabEntries that it refers to.
     *
     * @param entry
     * @param def
     * @param doc
     * @param literal
     */
    private void setMessageReferences(MessageEntry entry, Definition def,
                                      Document doc, boolean literal) {

        // If we don't want to emit stuff from imported files, only set the
        // isReferenced flag if this entry exists in the immediate WSDL file.
        Message message = entry.getMessage();

        if (addImports) {
            entry.setIsReferenced(true);
        } else {

            // NOTE:  I thought I could have simply done:
            // if (def.getMessage(message.getQName()) != null)
            // but that method traces through all imported messages.
            Map messages = def.getMessages();

            if (messages.containsValue(message)) {
                entry.setIsReferenced(true);
            }
        }

        // Set all the message's types
        Iterator parts = message.getParts().values().iterator();

        while (parts.hasNext()) {
            Part part = (Part) parts.next();
            TypeEntry type = getType(part.getTypeName());

            if (type != null) {
                setTypeReferences(type, doc, literal);
            }

            type = getElement(part.getElementName());

            if (type != null) {
                setTypeReferences(type, doc, literal);

                TypeEntry refType = type.getRefType();

                if (refType != null) {
                    setTypeReferences(refType, doc, literal);
                }
            }
        }
    }    // setMessageReference

    /**
     * Set the isReferenced flag to true on the given PortTypeEntry and all
     * SymTabEntries that it refers to.
     *
     * @param entry
     * @param bEntry
     * @param def
     * @param doc
     */
    private void setPortTypeReferences(PortTypeEntry entry,
                                       BindingEntry bEntry, Definition def,
                                       Document doc) {

        // If we don't want to emit stuff from imported files, only set the
        // isReferenced flag if this entry exists in the immediate WSDL file.
        PortType portType = entry.getPortType();

        if (addImports) {
            entry.setIsReferenced(true);
        } else {

            // NOTE:  I thought I could have simply done:
            // if (def.getPortType(portType.getQName()) != null)
            // but that method traces through all imported portTypes.
            Map portTypes = def.getPortTypes();

            if (portTypes.containsValue(portType)) {
                entry.setIsReferenced(true);
            }
        }

        // Set all the portType's messages
        Iterator operations = portType.getOperations().iterator();

        // For each operation, query its input, output, and fault messages
        while (operations.hasNext()) {
            Operation operation = (Operation) operations.next();
            Input input = operation.getInput();
            Output output = operation.getOutput();

            // Find out if this reference is a literal reference or not.
            boolean literalInput = false;
            boolean literalOutput = false;

            if (bEntry != null) {
                literalInput = bEntry.getInputBodyType(operation)
                        == Use.LITERAL;
                literalOutput = bEntry.getOutputBodyType(operation)
                        == Use.LITERAL;
            }

            // Query the input message
            if (input != null) {
                Message message = input.getMessage();

                if (message != null) {
                    MessageEntry mEntry = getMessageEntry(message.getQName());

                    if (mEntry != null) {
                        setMessageReferences(mEntry, def, doc, literalInput);
                    }
                }
            }

            // Query the output message
            if (output != null) {
                Message message = output.getMessage();

                if (message != null) {
                    MessageEntry mEntry = getMessageEntry(message.getQName());

                    if (mEntry != null) {
                        setMessageReferences(mEntry, def, doc, literalOutput);
                    }
                }
            }

            // Query the fault messages
            Iterator faults = operation.getFaults().values().iterator();

            while (faults.hasNext()) {
                Message message = ((Fault) faults.next()).getMessage();

                if (message != null) {
                    MessageEntry mEntry = getMessageEntry(message.getQName());

                    if (mEntry != null) {
                        setMessageReferences(mEntry, def, doc, false);
                    }
                }
            }
        }
    }    // setPortTypeReferences

    /**
     * Set the isReferenced flag to true on the given BindingEntry and all
     * SymTabEntries that it refers to ONLY if this binding is a SOAP binding.
     *
     * @param entry
     * @param def
     * @param doc
     */
    private void setBindingReferences(BindingEntry entry, Definition def,
                                      Document doc) {

        if (entry.getBindingType() == BindingEntry.TYPE_SOAP) {

            // If we don't want to emit stuff from imported files, only set the
            // isReferenced flag if this entry exists in the immediate WSDL file.
            Binding binding = entry.getBinding();

            if (addImports) {
                entry.setIsReferenced(true);
            } else {

                // NOTE:  I thought I could have simply done:
                // if (def.getBindng(binding.getQName()) != null)
                // but that method traces through all imported bindings.
                Map bindings = def.getBindings();

                if (bindings.containsValue(binding)) {
                    entry.setIsReferenced(true);
                }
            }

            // Set all the binding's portTypes
            PortType portType = binding.getPortType();
            PortTypeEntry ptEntry = getPortTypeEntry(portType.getQName());

            if (ptEntry != null) {
                setPortTypeReferences(ptEntry, entry, def, doc);
            }
        }
    }    // setBindingReferences

    /**
     * Set the isReferenced flag to true on the given ServiceEntry and all
     * SymTabEntries that it refers to.
     *
     * @param entry
     * @param def
     * @param doc
     */
    private void setServiceReferences(ServiceEntry entry, Definition def,
                                      Document doc) {

        // If we don't want to emit stuff from imported files, only set the
        // isReferenced flag if this entry exists in the immediate WSDL file.
        Service service = entry.getService();

        if (addImports) {
            entry.setIsReferenced(true);
        } else {

            // NOTE:  I thought I could have simply done:
            // if (def.getService(service.getQName()) != null)
            // but that method traces through all imported services.
            Map services = def.getServices();

            if (services.containsValue(service)) {
                entry.setIsReferenced(true);
            }
        }

        // Set all the service's bindings
        Iterator ports = service.getPorts().values().iterator();

        while (ports.hasNext()) {
            Port port = (Port) ports.next();
            Binding binding = port.getBinding();

            if (binding != null) {
                BindingEntry bEntry = getBindingEntry(binding.getQName());

                if (bEntry != null) {
                    setBindingReferences(bEntry, def, doc);
                }
            }
        }
    }    // setServiceReferences

    /**
     * Put the given SymTabEntry into the symbol table, if appropriate.
     *
     * @param entry
     * @throws IOException
     */
    private SymTabEntry symbolTablePut(SymTabEntry entry) throws IOException {

        QName name = entry.getQName();

        SymTabEntry e = get(name, entry.getClass());

        if (e == null) {
            e = entry;

            // An entry of the given qname of the given type doesn't exist yet.
            if ((entry instanceof Type)
                    && (get(name, UndefinedType.class) != null)) {

                // A undefined type exists in the symbol table, which means
                // that the type is used, but we don't yet have a definition for
                // the type.  Now we DO have a definition for the type, so
                // replace the existing undefined type with the real type.
                if (((TypeEntry) get(name, UndefinedType.class)).isSimpleType()
                        && !((TypeEntry) entry).isSimpleType()) {

                    // Problem if the undefined type was used in a
                    // simple type context.
                    throw new IOException(
                            Messages.getMessage(
                                    "AttrNotSimpleType01", name.toString()));
                }

                Vector v = (Vector) symbolTable.get(name);

                for (int i = 0; i < v.size(); ++i) {
                    Object oldEntry = v.elementAt(i);

                    if (oldEntry instanceof UndefinedType) {

                        // Replace it in the symbol table
                        v.setElementAt(entry, i);

                        // Replace it in the types index
                        typeTypeEntries.put(name, entry);

                        // Update all of the entries that refer to the unknown type
                        ((UndefinedType) oldEntry).update((Type) entry);
                    }
                }
            } else if ((entry instanceof Element)
                    && (get(name, UndefinedElement.class) != null)) {

                // A undefined element exists in the symbol table, which means
                // that the element is used, but we don't yet have a definition for
                // the element.  Now we DO have a definition for the element, so
                // replace the existing undefined element with the real element.
                Vector v = (Vector) symbolTable.get(name);

                for (int i = 0; i < v.size(); ++i) {
                    Object oldEntry = v.elementAt(i);

                    if (oldEntry instanceof UndefinedElement) {

                        // Replace it in the symbol table
                        v.setElementAt(entry, i);

                        // Replace it in the elements index
                        elementTypeEntries.put(name, entry);

                        // Update all of the entries that refer to the unknown type
                        ((Undefined) oldEntry).update((Element) entry);
                    }
                }
            } else {

                // Add this entry to the symbol table
                Vector v = (Vector) symbolTable.get(name);

                if (v == null) {
                    v = new Vector();

                    symbolTable.put(name, v);
                }

                v.add(entry);

                // add TypeEntries to specialized indices for
                // fast lookups during reference resolution.
                if (entry instanceof Element) {
                    elementTypeEntries.put(name, entry);
                } else if (entry instanceof Type) {
                    typeTypeEntries.put(name, entry);
                }
            }
        } else {
            if (!quiet) {
                System.out.println(Messages.getMessage("alreadyExists00",
                                                       "" + name));
            }
        }

        return e;
    }    // symbolTablePut

    /**
     * checks whether there exists a WSDL port with a given name in the current
     * symbol table
     *
     * @param name the QName of the port. Note: only the local part of the qname is relevant,
     *             since port names are not qualified with a namespace. They are of type nmtoken in WSDL 1.1
     *             and of type ncname in WSDL 1.2
     * @return true, if there is a port element with the specified name; false, otherwise
     */
    protected boolean existsPortWithName(QName name) {

        Vector v = (Vector) symbolTable.get(name);

        if (v == null) {
            return false;
        }

        Iterator it = v.iterator();

        while (it.hasNext()) {
            Object o = it.next();

            if (o instanceof PortEntry) {
                return true;
            }
        }

        return false;
    }

    /**
     * Method getInnerCollectionComponentQName
     *
     * @param node
     * @return
     */
    private QName getInnerCollectionComponentQName(Node node) {

        if (node == null) {
            return null;
        }

        QName name = SchemaUtils.getCollectionComponentQName(node, new QNameHolder(), new BooleanHolder(), this);

        if (name != null) {
            return name;
        }

        // Dive into the node if necessary
        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            name = getInnerCollectionComponentQName(children.item(i));

            if (name != null) {
                return name;
            }
        }

        return null;
    }

    /**
     * Method getInnerTypeQName
     * 
     * @param node 
     * @return 
     */
    private static QName getInnerTypeQName(Node node) {

        if (node == null) {
            return null;
        }

        BooleanHolder forElement = new BooleanHolder();
        QName name = Utils.getTypeQName(node, forElement, true);

        if (name != null) {
            return name;
        }

        // Dive into the node if necessary
        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            name = getInnerTypeQName(children.item(i));

            if (name != null) {
                return name;
            }
        }

        return null;
    }
    
    protected void processTypes() {
        for (Iterator i = typeTypeEntries.values().iterator(); i.hasNext(); ) {
            Type type = (Type) i.next();
            Node node = type.getNode();            
            
            // Process the attributes
            Vector attributes = 
                    SchemaUtils.getContainedAttributeTypes(node, this);

            if (attributes != null) {
                type.setContainedAttributes(attributes);
            }

            // Process the elements
            Vector elements =
                    SchemaUtils.getContainedElementDeclarations(node, this);

            if (elements != null) {
                type.setContainedElements(elements);
            }
        }
    }
    
    public List getMessageEntries() {
        List messageEntries = new ArrayList();
        Iterator iter = symbolTable.values().iterator();
        while (iter.hasNext()) {
            Vector v = (Vector)iter.next();
            for (int i = 0; i < v.size(); ++i) {
                SymTabEntry entry = (SymTabEntry)v.elementAt(i);
                if (entry instanceof MessageEntry) {
                    messageEntries.add(entry);
                }
            }
        }

        return messageEntries;
    }

    public void setWrapArrays(boolean wrapArrays) {
        this.wrapArrays = wrapArrays;
    }

    public Map getElementFormDefaults() {
        return elementFormDefaults;
    }
}

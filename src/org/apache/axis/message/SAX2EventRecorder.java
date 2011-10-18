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

import org.apache.axis.encoding.DeserializationContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * This class records SAX2 Events and allows
 * the events to be replayed by start and stop index
 */
public class SAX2EventRecorder { 
    
    private static final Integer Z = new Integer(0);

    private static final Integer STATE_SET_DOCUMENT_LOCATOR = new Integer(0);
    private static final Integer STATE_START_DOCUMENT = new Integer(1);
    private static final Integer STATE_END_DOCUMENT = new Integer(2);
    private static final Integer STATE_START_PREFIX_MAPPING = new Integer(3);
    private static final Integer STATE_END_PREFIX_MAPPING = new Integer(4);
    private static final Integer STATE_START_ELEMENT = new Integer(5);
    private static final Integer STATE_END_ELEMENT = new Integer(6);
    private static final Integer STATE_CHARACTERS = new Integer(7);
    private static final Integer STATE_IGNORABLE_WHITESPACE = new Integer(8);
    private static final Integer STATE_PROCESSING_INSTRUCTION = new Integer(9);
    private static final Integer STATE_SKIPPED_ENTITY = new Integer(10);
    
    // This is a "custom" event which tells DeserializationContexts
    // that the current element is moving down the stack...
    private static final Integer STATE_NEWELEMENT = new Integer(11);

    // Lexical handler events...
    private static final Integer STATE_START_DTD = new Integer(12);
    private static final Integer STATE_END_DTD = new Integer(13);
    private static final Integer STATE_START_ENTITY = new Integer(14);
    private static final Integer STATE_END_ENTITY = new Integer(15);
    private static final Integer STATE_START_CDATA = new Integer(16);
    private static final Integer STATE_END_CDATA = new Integer(17);
    private static final Integer STATE_COMMENT = new Integer(18);
    
    org.xml.sax.Locator locator;
    objArrayVector events = new objArrayVector();
    
    public void clear() {
        locator = null;
        events = new objArrayVector();
    }
    public int getLength()
    {
        return events.getLength();
    }
    
    public int setDocumentLocator(org.xml.sax.Locator p1) {
        locator = p1;
        return events.add(STATE_SET_DOCUMENT_LOCATOR, Z,Z,Z,Z);
    }
    public int startDocument() {
        return events.add(STATE_START_DOCUMENT, Z,Z,Z,Z);
    }
    public int endDocument() {
        return events.add(STATE_END_DOCUMENT, Z,Z,Z,Z);
    }
    public int startPrefixMapping(String p1, String p2) {
        return events.add(STATE_START_PREFIX_MAPPING, p1, p2, Z,Z);
    }
    public int endPrefixMapping(String p1) {
        return events.add(STATE_END_PREFIX_MAPPING, p1,Z,Z,Z);
    }
    public int startElement(String p1, String p2, String p3, org.xml.sax.Attributes p4) {
        return events.add(STATE_START_ELEMENT, p1, p2, p3, p4);
    }
    public int endElement(String p1, String p2, String p3) {
        return events.add(STATE_END_ELEMENT, p1, p2, p3, Z);
    }
    public int characters(char[] p1, int p2, int p3) {
        return events.add(STATE_CHARACTERS, 
                          (Object)clone(p1, p2, p3), 
                          Z, Z, Z);
    }
    public int ignorableWhitespace(char[] p1, int p2, int p3) {
        return events.add(STATE_IGNORABLE_WHITESPACE,
                          (Object)clone(p1, p2, p3),
                          Z, Z, Z);
    }
    public int processingInstruction(String p1, String p2) {
        return events.add(STATE_PROCESSING_INSTRUCTION, p1, p2, Z,Z);
    }
    public int skippedEntity(String p1) {
        return events.add(STATE_SKIPPED_ENTITY, p1, Z,Z,Z);
    }
    
    public void startDTD(java.lang.String name,
                     java.lang.String publicId,
                     java.lang.String systemId) {
        events.add(STATE_START_DTD, name, publicId, systemId, Z);
    }
    public void endDTD() {
        events.add(STATE_END_DTD, Z, Z, Z, Z);
    }
    public void startEntity(java.lang.String name) {
        events.add(STATE_START_ENTITY, name, Z, Z, Z);
    }
    public void endEntity(java.lang.String name) {
        events.add(STATE_END_ENTITY, name, Z, Z, Z);
    }
    public void startCDATA() {
        events.add(STATE_START_CDATA, Z, Z, Z, Z);
    }
    public void endCDATA() {
        events.add(STATE_END_CDATA, Z, Z, Z, Z);
    }
    public void comment(char[] ch,
                    int start,
                    int length) {
        events.add(STATE_COMMENT, 
                   (Object)clone(ch, start, length), 
                   Z, Z, Z);
    }
    
    public int newElement(MessageElement elem) {
        return events.add(STATE_NEWELEMENT, elem, Z,Z,Z);
    }
    
    public void replay(ContentHandler handler) throws SAXException {
        if (events.getLength() > 0) {
            replay(0, events.getLength() - 1, handler);
        }
    }
    
    public void replay(int start, int stop, ContentHandler handler) throws SAXException {
        // Special case : play the whole thing for [0, -1]
        if ((start == 0) && (stop == -1)) {
            replay(handler);
            return;
        }
        
        if (stop + 1 > events.getLength() ||
            stop < start) {
            return; // should throw an error here
        }        
        
        LexicalHandler lexicalHandler = null;
        if (handler instanceof LexicalHandler) {
            lexicalHandler = (LexicalHandler) handler;
        }
        
        for (int n = start; n <= stop; n++) {
            Object event = events.get(n,0);
            if (event == STATE_START_ELEMENT) {
                handler.startElement((String)events.get(n,1), 
                                     (String)events.get(n,2),
                                     (String)events.get(n,3),
                                     (org.xml.sax.Attributes)events.get(n,4));
                
            } else if (event == STATE_END_ELEMENT) {
                handler.endElement((String)events.get(n,1), 
                                   (String)events.get(n,2),
                                   (String)events.get(n,3));
                
            } else if (event == STATE_CHARACTERS) {
                char[] data = (char[])events.get(n,1);
                handler.characters(data, 0, data.length);
                
            } else if (event == STATE_IGNORABLE_WHITESPACE) {
                char[] data = (char[])events.get(n,1);
                handler.ignorableWhitespace(data, 0, data.length);
                
            } else if (event == STATE_PROCESSING_INSTRUCTION) {
                handler.processingInstruction((String)events.get(n,1),
                                              (String)events.get(n,2));
                
            } else if (event == STATE_SKIPPED_ENTITY) {
                handler.skippedEntity((String)events.get(n,1));
                
            } else if (event == STATE_SET_DOCUMENT_LOCATOR) {
                handler.setDocumentLocator(locator);
                
            } else if (event == STATE_START_DOCUMENT) {
                handler.startDocument();
                
            } else if (event == STATE_END_DOCUMENT) {
                handler.endDocument();
                
            } else if (event == STATE_START_PREFIX_MAPPING) {
                handler.startPrefixMapping((String)events.get(n, 1),
                                           (String)events.get(n, 2));
                
            } else if (event == STATE_END_PREFIX_MAPPING) {
                handler.endPrefixMapping((String)events.get(n, 1));
                
            } else if (event == STATE_START_DTD && lexicalHandler != null) {
                lexicalHandler.startDTD((String)events.get(n,1), 
                                   (String)events.get(n,2),
                                   (String)events.get(n,3));
            } else if (event == STATE_END_DTD && lexicalHandler != null) {
                lexicalHandler.endDTD();
            
            } else if (event == STATE_START_ENTITY && lexicalHandler != null) {
                lexicalHandler.startEntity((String)events.get(n,1));
            
            } else if (event == STATE_END_ENTITY && lexicalHandler != null) {
                lexicalHandler.endEntity((String)events.get(n,1));
            
            } else if (event == STATE_START_CDATA && lexicalHandler != null) {
                lexicalHandler.startCDATA();
            
            } else if (event == STATE_END_CDATA && lexicalHandler != null) {
                lexicalHandler.endCDATA();
            
            } else if (event == STATE_COMMENT && lexicalHandler != null) {
                char[] data = (char[])events.get(n,1);
                lexicalHandler.comment(data, 0, data.length);
            
            } else if (event == STATE_NEWELEMENT) {
                if (handler instanceof DeserializationContext) {
                    DeserializationContext context =
                              (DeserializationContext)handler;
                    context.setCurElement(
                              (MessageElement)(events.get(n,1)));
                }
            }
        }
    }

    private static char[] clone(char[] in, int off, int len) {
        char[] out = new char[len];
        System.arraycopy(in, off, out, 0, len);
        return out;
    }
    
/////////////////////////////////////////////
    class objArrayVector {
        private int RECORD_SIZE = 5;
        private int currentSize = 0;
        private Object[] objarray = new Object[50 * RECORD_SIZE];  // default to 50 5 field records
        
        public int add(Object p1, Object p2, Object p3, Object p4, Object p5) {
            if (currentSize == objarray.length) {
                Object[] newarray = new Object[currentSize * 2];
                System.arraycopy(objarray, 0, newarray, 0, currentSize);
                objarray = newarray;
            }
            int pos = currentSize / RECORD_SIZE;
            objarray[currentSize++] = p1;
            objarray[currentSize++] = p2;
            objarray[currentSize++] = p3;
            objarray[currentSize++] = p4;
            objarray[currentSize++] = p5;
            return pos;
        }

        public Object get(int pos, int fld) {
            return objarray[(pos * RECORD_SIZE) + fld];
        }
    
        public int getLength() {
            return (currentSize / RECORD_SIZE);
        }
    }
/////////////////////////////////////////////
            
}

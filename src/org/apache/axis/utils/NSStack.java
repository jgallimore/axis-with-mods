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
package org.apache.axis.utils;

import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.AxisProperties;
import org.apache.axis.Constants;
import org.apache.commons.logging.Log;

import java.util.ArrayList;

/**
 * The abstraction this class provides is a push down stack of variable
 * length frames of prefix to namespace mappings.  Used for keeping track
 * of what namespaces are active at any given point as an XML document is
 * traversed or produced.
 *
 * From a performance point of view, this data will both be modified frequently
 * (at a minimum, there will be one push and pop per XML element processed),
 * and scanned frequently (many of the "good" mappings will be at the bottom
 * of the stack).  The one saving grace is that the expected maximum 
 * cardinalities of the number of frames and the number of total mappings
 * is only in the dozens, representing the nesting depth of an XML document
 * and the number of active namespaces at any point in the processing.
 *
 * Accordingly, this stack is implemented as a single array, will null
 * values used to indicate frame boundaries.
 *
 * @author James Snell
 * @author Glen Daniels (gdaniels@apache.org)
 * @author Sam Ruby (rubys@us.ibm.com)
 */
public class NSStack {
    protected static Log log =
        LogFactory.getLog(NSStack.class.getName());
    
    private Mapping[] stack;
    private int top = 0;
    private int iterator = 0;
    private int currentDefaultNS = -1;
    private boolean optimizePrefixes = true;
    
    // invariant member variable to track low-level logging requirements
    // we cache this once per instance lifecycle to avoid repeated lookups
    // in heavily used code.
    private final boolean traceEnabled = log.isTraceEnabled();

    public NSStack(boolean optimizePrefixes) {
        this.optimizePrefixes = optimizePrefixes;
        stack = new Mapping[32];
        stack[0] = null;
    }

    public NSStack() {
        stack = new Mapping[32];
        stack[0] = null;
    }
    
    /**
     * Create a new frame at the top of the stack.
     */
    public void push() {
        top ++;

        if (top >= stack.length) {
           Mapping newstack[] = new Mapping[stack.length*2];
           System.arraycopy (stack, 0, newstack, 0, stack.length);
           stack = newstack;
        }

        if (traceEnabled)
            log.trace("NSPush (" + stack.length + ")");

        stack[top] = null;
    }
    
    /**
     * Remove the top frame from the stack.
     */
    public void pop() {
        clearFrame();

        top--;

        // If we've moved below the current default NS, figure out the new
        // default (if any)
        if (top < currentDefaultNS) {
            // Reset the currentDefaultNS to ignore the frame just removed.
            currentDefaultNS = top;
            while (currentDefaultNS > 0) {
                if (stack[currentDefaultNS] != null &&
                        stack[currentDefaultNS].getPrefix().length() == 0)
                    break;
                currentDefaultNS--;
            }
        }
        
        if (top == 0) {
            if (traceEnabled)
                log.trace("NSPop (" + Messages.getMessage("empty00") + ")");

            return;
        }
        
        if (traceEnabled){
            log.trace("NSPop (" + stack.length + ")");
        }
    }
    
    /**
     * Return a copy of the current frame.  Returns null if none are present.
     */
    public ArrayList cloneFrame() {
        if (stack[top] == null) return null;

        ArrayList clone = new ArrayList();

        for (Mapping map=topOfFrame(); map!=null; map=next()) {
            clone.add(map);
        }

        return clone;
    }

    /**
     * Remove all mappings from the current frame.
     */
    private void clearFrame() {
        while (stack[top] != null) top--;
    }

    /**
     * Reset the embedded iterator in this class to the top of the current
     * (i.e., last) frame.  Note that this is not threadsafe, nor does it
     * provide multiple iterators, so don't use this recursively.  Nor
     * should you modify the stack while iterating over it.
     */
    public Mapping topOfFrame() {
        iterator = top;
        while (stack[iterator] != null) iterator--;
        iterator++;
        return next();
    }

    /**
     * Return the next namespace mapping in the top frame.
     */
    public Mapping next() {
        if (iterator > top) {
            return null;
        } else {
            return stack[iterator++];
        }
    }

    /**
     * Add a mapping for a namespaceURI to the specified prefix to the top
     * frame in the stack.  If the prefix is already mapped in that frame,
     * remap it to the (possibly different) namespaceURI.
     */
    public void add(String namespaceURI, String prefix) {
        int idx = top;
        prefix = prefix.intern();
        try {
            // Replace duplicate prefixes (last wins - this could also fault)
            for (int cursor=top; stack[cursor]!=null; cursor--) {
                if (stack[cursor].getPrefix() == prefix) {
                    stack[cursor].setNamespaceURI(namespaceURI);
                    idx = cursor;
                    return;
                }
            }
            
            push();
            stack[top] = new Mapping(namespaceURI, prefix);
            idx = top;
        } finally {
            // If this is the default namespace, note the new in-scope
            // default is here.
            if (prefix.length() == 0) {
                currentDefaultNS = idx;
            }
        }
    }
    
    /**
     * Return an active prefix for the given namespaceURI.  NOTE : This
     * may return null even if the namespaceURI was actually mapped further
     * up the stack IF the prefix which was used has been repeated further
     * down the stack.  I.e.:
     * 
     * <pre:outer xmlns:pre="namespace">
     *   <pre:inner xmlns:pre="otherNamespace">
     *      *here's where we're looking*
     *   </pre:inner>
     * </pre:outer>
     * 
     * If we look for a prefix for "namespace" at the indicated spot, we won't
     * find one because "pre" is actually mapped to "otherNamespace"
     */ 
    public String getPrefix(String namespaceURI, boolean noDefault) {
        if ((namespaceURI == null) || (namespaceURI.length()==0))
            return null;
        
        if(optimizePrefixes) {
            // If defaults are OK, and the given NS is the current default,
            // return "" as the prefix to favor defaults where possible.
            if (!noDefault && currentDefaultNS > 0 && stack[currentDefaultNS] != null &&
                    namespaceURI == stack[currentDefaultNS].getNamespaceURI())
                return "";
        }
        namespaceURI = namespaceURI.intern();

        for (int cursor=top; cursor>0; cursor--) {
            Mapping map = stack[cursor];
            if (map == null) 
                continue;

            if (map.getNamespaceURI() == namespaceURI) {
                String possiblePrefix = map.getPrefix();
                if (noDefault && possiblePrefix.length() == 0)
                    continue;
    
                // now make sure that this is the first occurance of this 
                // particular prefix
                for (int cursor2 = top; true; cursor2--) {
                    if (cursor2 == cursor)
                        return possiblePrefix;
                    map = stack[cursor2];
                    if (map == null)
                        continue;
                    if (possiblePrefix == map.getPrefix())
                        break;
                }
            }
        }
        
        return null;
    }

    /**
     * Return an active prefix for the given namespaceURI, including
     * the default prefix ("").
     */ 
    public String getPrefix(String namespaceURI) {
        return getPrefix(namespaceURI, false);
    }
    
    /**
     * Given a prefix, return the associated namespace (if any).
     */
    public String getNamespaceURI(String prefix) {
        if (prefix == null)
            prefix = "";

        prefix = prefix.intern();

        for (int cursor=top; cursor>0; cursor--) {
            Mapping map = stack[cursor];
            if (map == null) continue;
        
            if (map.getPrefix() == prefix)
                return map.getNamespaceURI();
        }
        
        return null;
    }
    
    /**
     * Produce a trace dump of the entire stack, starting from the top and
     * including frame markers.
     */
    public void dump(String dumpPrefix)
    {
        for (int cursor=top; cursor>0; cursor--) {
            Mapping map = stack[cursor];

            if (map == null) {
                log.trace(dumpPrefix + Messages.getMessage("stackFrame00"));
            } else {
                log.trace(dumpPrefix + map.getNamespaceURI() + " -> " + map.getPrefix());
            }
        }
    }
}

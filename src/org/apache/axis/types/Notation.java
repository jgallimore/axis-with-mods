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
package org.apache.axis.types;

import org.apache.axis.Constants;
import org.apache.axis.description.AttributeDesc;
import org.apache.axis.description.ElementDesc;
import org.apache.axis.description.FieldDesc;
import org.apache.axis.description.TypeDesc;

/**
 * Custom class for supporting XSD data type NOTATION.
 *
 * @author Davanum Srinivas <dims@yahoo.com>
 * @see <a href="http://www.w3.org/TR/xmlschema-1/#element-notation">XML Schema Part 1: 3.12 Notation Declarations</a>
 */

public class Notation implements java.io.Serializable {
    NCName name;
    URI publicURI;
    URI systemURI;

    public Notation() {
    }

    public Notation(NCName name, URI publicURI, URI systemURI) {
        this.name = name;
        this.publicURI = publicURI;
        this.systemURI = systemURI;
    }

    public NCName getName() {
        return name;
    }

    public void setName(NCName name) {
        this.name = name;
    }

    public URI getPublic() {
        return publicURI;
    }

    public void setPublic(URI publicURI) {
        this.publicURI = publicURI;
    }

    public URI getSystem() {
        return systemURI;
    }

    public void setSystem(URI systemURI) {
        this.systemURI = systemURI;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Notation))
            return false;
        Notation other = (Notation) obj;
        if (name == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!name.equals(other.getName())) {
            return false;
        }
        if (publicURI == null) {
            if (other.getPublic() != null) {
                return false;
            }
        } else if (!publicURI.equals(other.getPublic())) {
            return false;
        }
        if (systemURI == null) {
            if (other.getSystem() != null) {
                return false;
            }
        } else if (!systemURI.equals(other.getSystem())) {
            return false;
        }
        return true;
    }

    /**
     * Returns the sum of the hashcodes of {name,publicURI,systemURI}
     * for whichever properties in that set is non null.  This is
     * consistent with the implementation of equals, as required by
     * {@link java.lang.Object#hashCode() Object.hashCode}.
     *
     * @return an <code>int</code> value
     */
    public int hashCode() {
        int hash = 0;
        if (null != name) {
            hash += name.hashCode();
        }
        if (null != publicURI) {
            hash += publicURI.hashCode();
        }
        if (null != systemURI) {
            hash += systemURI.hashCode();
        }
        return hash;
    }


    // Type metadata
    private static TypeDesc typeDesc;

    static {
        typeDesc = new TypeDesc(Notation.class);
        FieldDesc field;

        // An attribute with a specified QName
        field = new AttributeDesc();
        field.setFieldName("name");
        field.setXmlName(Constants.XSD_NCNAME);
        typeDesc.addFieldDesc(field);

        // An attribute with a default QName
        field = new AttributeDesc();
        field.setFieldName("public");
        field.setXmlName(Constants.XSD_ANYURI);
        typeDesc.addFieldDesc(field);

        // An element with a specified QName
        ElementDesc element = null;
        element = new ElementDesc();
        element.setFieldName("system");
        element.setXmlName(Constants.XSD_ANYURI);
        // per, http://www.w3.org/TR/xmlschema-1/#element-notation,
        // "system" property can be null  
        element.setNillable(true);
        typeDesc.addFieldDesc(field);
    }

    public static TypeDesc getTypeDesc() {
        return typeDesc;
    }
}

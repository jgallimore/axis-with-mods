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

import org.apache.axis.utils.Messages;

/**
 * Custom class for supporting XSD data type NormalizedString.
 * normalizedString represents white space normalized strings.
 * The base type of normalizedString is string.
 *
 * @author Chris Haddad <chaddad@cobia.net>
 * @see <a href="http://www.w3.org/TR/xmlschema-2/#normalizedString">XML Schema Part 2: Datatypes 3.3.1</a>
 */
public class NormalizedString extends Object implements java.io.Serializable {

    String m_value = null;   // JAX-RPC maps xsd:string to java.lang.String

    public NormalizedString() {
        super();
    }

    /**
     *
     * ctor for NormalizedString
     * @param stValue is the String value
     * @throws IllegalArgumentException if invalid format
     */
    public NormalizedString(String stValue) throws IllegalArgumentException {
        setValue(stValue);
    }

    /**
     *
     * validates the data and sets the value for the object.
     * @param stValue String value
     * @throws IllegalArgumentException if invalid format
     */
    public void setValue(String stValue) throws IllegalArgumentException {
        if (NormalizedString.isValid(stValue) == false)
            throw new IllegalArgumentException(
               Messages.getMessage("badNormalizedString00") +
               " data=[" + stValue + "]");
        m_value = stValue;
    }

    public String toString(){
        return m_value;
    }

    public int hashCode(){
        return m_value.hashCode();
    }

    /**
     *
     * validate the value against the xsd definition for the object
     *
     * The value space of normalizedString is the set of strings that
     * do not contain the carriage return (#xD), line feed (#xA) nor
     * tab (#x9) characters. The lexical space of normalizedString is
     * the set of strings that do not contain the carriage return (#xD)
     * nor tab (#x9) characters.
     *
     * @param stValue the String to test
     * @returns true if valid normalizedString
     */
    public static boolean isValid(String stValue)  {
        int scan;

        for (scan = 0; scan < stValue.length(); scan++) {
            char cDigit = stValue.charAt(scan);
            switch (cDigit) {
                case 0x09:
                case 0x0A:
                case 0x0D:
                    return false;
                default:
                    break;
            }
        }
        return true;
    }

    public boolean equals(Object object)  {
        String s1 = object.toString();
        return s1.equals(m_value);
    }
}

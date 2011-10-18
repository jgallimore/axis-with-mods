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
 * Custom class for supporting primitive XSD data type UnsignedShort
 *
 * @author Chris Haddad <chaddad@cobia.net>
 * @see <a href="http://www.w3.org/TR/xmlschema-2/#unsignedShort">XML Schema 3.3.23</a>
 */
public class UnsignedShort extends UnsignedInt {

    public UnsignedShort() {

    }

    /**
     * ctor for UnsignedShort
     * @exception NumberFormatException will be thrown if validation fails
     */
    public UnsignedShort(long sValue) throws NumberFormatException {
      setValue(sValue);
    }

    public UnsignedShort(String sValue) throws NumberFormatException {
      setValue(Long.parseLong(sValue));
    }

    /**
     *
     * validates the data and sets the value for the object.
     *
     * @param sValue value
     */
    public void setValue(long sValue) throws NumberFormatException {
        if (UnsignedShort.isValid(sValue) == false)
            throw new NumberFormatException(
                Messages.getMessage("badUnsignedShort00") +
                    String.valueOf(sValue) + "]");
        lValue = new Long(sValue);
    }

    /**
     *
     * validate the value against the xsd definition
     *
     */
    public static boolean isValid(long sValue) {
      if ( (sValue < 0L  ) || (sValue > 65535L) )
        return false;
      else
        return true;
    }

}

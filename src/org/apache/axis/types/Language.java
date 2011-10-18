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
 * Custom class for supporting XSD data type language
 * language represents natural language identifiers as defined by [RFC 1766]. 
 * The value space of language is the set of all strings that are valid language identifiers 
 * as defined in the language identification section of [XML 1.0 (Second Edition)]. 
 * The lexical space of language is the set of all strings that are valid language identifiers 
 * as defined in the language identification section of [XML 1.0 (Second Edition)]. 
 * The base type of language is token. 
 *
 * @author Eddie Pick <eddie@pick.eu.org>
 * @see <a href="http://www.w3.org/TR/xmlschema-2/#language">XML Schema 3.3.3</a>
 */
public class Language extends Token {

    public Language() {
        super();
    }

    /**
     * ctor for Language
     * @exception IllegalArgumentException will be thrown if validation fails
     */
    public Language(String stValue) throws IllegalArgumentException {
        try {
            setValue(stValue);
        }
        catch (IllegalArgumentException e) {
            // recast normalizedString exception as token exception
            throw new IllegalArgumentException(
                Messages.getMessage("badLanguage00") + "data=[" +
                stValue + "]");
        }
    }

   /**
    *
    * validate the value against the xsd definition
    * TODO
    * @see <a href="http://www.ietf.org/rfc/rfc1766.txt">RFC1766</a>
    * Language-Tag = Primary-tag *( "-" Subtag )
    * Primary-tag = 1*8ALPHA
    * Subtag = 1*8ALPHA
    */
    public static boolean isValid(String stValue) {
        return true;
    }
}

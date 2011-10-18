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

import java.util.StringTokenizer;

/**
 * Custom class for supporting XSD data type Entities
 * 
 * @author Davanum Srinivas <dims@yahoo.com>
 * @see <a href="http://www.w3.org/TR/xmlschema-2/#ENTITIES">XML Schema 3.3.12 ENTITIES</a>
 */
public class Entities extends NCName {
    private Entity[] entities;
    
    public Entities() {
        super();
    }
    /**
     * ctor for Entities
     * @exception IllegalArgumentException will be thrown if validation fails
     */
    public Entities (String stValue) throws IllegalArgumentException {
        StringTokenizer tokenizer = new StringTokenizer(stValue);
        int count = tokenizer.countTokens();
        entities = new Entity[count];
        for(int i=0;i<count;i++){
            entities[i] = new Entity(tokenizer.nextToken());
        }
    }
}

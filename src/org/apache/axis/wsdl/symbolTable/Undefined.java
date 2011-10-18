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

import java.io.IOException;

/**
 * This Undefined interface is implemented by UndefinedType and UndefinedElement.
 */
public interface Undefined {

    /**
     * Register referrant TypeEntry so that
     * the code can update the TypeEntry when the Undefined Element or Type is defined
     * 
     * @param referrant 
     */
    public void register(TypeEntry referrant);

    /**
     * Call update with the actual TypeEntry.  This updates all of the
     * referrant TypeEntry's that were registered.
     * 
     * @param def 
     * @throws IOException 
     */
    public void update(TypeEntry def) throws IOException;
}

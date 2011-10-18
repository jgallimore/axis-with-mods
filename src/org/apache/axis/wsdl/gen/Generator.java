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
package org.apache.axis.wsdl.gen;

import java.io.IOException;

/**
 * This is the interface for all writers.  All writers, very simply, must
 * support a write method.
 * <p/>
 * Writer and WriterFactory are part of the Writer framework.  Folks who want
 * to use the emitter to generate stuff from WSDL should do 3 things:
 * 1.  Write implementations of the Writer interface, one each for PortType,
 * Binding, Service, and Type.  These implementations generate the stuff
 * for each of these WSDL types.
 * 2.  Write an implementation of the WriterFactory interface that returns
 * instantiations of these Writer implementations as appropriate.
 * 3.  Implement a class with a main method (like Wsdl2java) that instantiates
 * an emitter and passes it the WriterFactory implementation
 */
public interface Generator {

    /**
     * Generate something.
     * 
     * @throws IOException 
     */
    public void generate() throws IOException;
}

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

package org.apache.axis.components.script;

/**
 * This interface defines a Script module functionality
 * @author <a href="mailto:dims@yahoo.com">Davanum Srinivas</a>
 * @since 2.0
 */
public interface Script {
    public Object run(String language, String name, String scriptStr, String methodName, Object[] argValues)
            throws Exception;
}


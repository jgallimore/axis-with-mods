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

package org.apache.axis.transport.http;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;

/**
 * The QSHandler interface defines an interface for classes that handle the
 * actions necessary when a particular query string is encountered in an AXIS
 * servlet invocation.
 *
 * @author Curtiss Howard
 */

public interface QSHandler {
     /**
      * Performs the action associated with this particular query string
      * handler.
      *
      * @param msgContext a MessageContext object containing message context
      *        information for this query string handler.
      * @throws AxisFault if an error occurs.
      */
     
     public void invoke (MessageContext msgContext) throws AxisFault;
}

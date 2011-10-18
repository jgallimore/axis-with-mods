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
package org.apache.axis.handlers;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.components.logger.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Map;

/**
 * Handles JAXRPC style handlers.
 *
 * @author Davanum Srinivas (dims@yahoo.com)
 */
public class JAXRPCHandler extends BasicHandler {
    protected static Log log =
            LogFactory.getLog(JAXRPCHandler.class.getName());

    protected HandlerChainImpl impl = new HandlerChainImpl();

    public void init() {
        super.init();
        String className = (String)getOption("className");
        if (className != null) {
            addNewHandler(className, getOptions());
        }
    }

    public void addNewHandler(String className, Map options) {
        impl.addNewHandler(className, options);
    }

    public void invoke(MessageContext msgContext) throws AxisFault {
        log.debug("Enter: JAXRPCHandler::enter invoke");
        if (!msgContext.getPastPivot()) {
            impl.handleRequest(msgContext);
        } else {
            impl.handleResponse(msgContext);
        }
        log.debug("Enter: JAXRPCHandler::exit invoke");
    }

    public void onFault(MessageContext msgContext) {
        impl.handleFault(msgContext);
    }

    public void cleanup() {
        impl.destroy();
    }
}

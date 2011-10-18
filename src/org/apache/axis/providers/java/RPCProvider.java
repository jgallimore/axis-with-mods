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

package org.apache.axis.providers.java;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.constants.Style;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.message.RPCElement;
import org.apache.axis.message.RPCHeaderParam;
import org.apache.axis.message.RPCParam;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.JavaUtils;
import org.apache.axis.utils.Messages;
import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.rpc.holders.Holder;
import javax.wsdl.OperationType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

/**
 * Implement message processing by walking over RPCElements of the
 * envelope body, invoking the appropriate methods on the service object.
 *
 * @author Doug Davis (dug@us.ibm.com)
 */
public class RPCProvider extends JavaProvider {
    protected static Log log =
            LogFactory.getLog(RPCProvider.class.getName());

    /**
     * Process the current message.
     * Result in resEnv.
     *
     * @param msgContext self-explanatory
     * @param reqEnv the request envelope
     * @param resEnv the response envelope
     * @param obj the service object itself
     */
    public void processMessage(MessageContext msgContext,
                               SOAPEnvelope reqEnv,
                               SOAPEnvelope resEnv,
                               Object obj)
            throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Enter: RPCProvider.processMessage()");
        }

        SOAPService service = msgContext.getService();
        ServiceDesc serviceDesc = service.getServiceDescription();
        RPCElement body = getBody(reqEnv, msgContext);

        Vector args = null;
        try {
            args = body.getParams();
        } catch (SAXException e) {
            if(e.getException() != null)
                throw e.getException();
            throw e;
        }
        int numArgs = args.size();
        OperationDesc operation = getOperationDesc(msgContext, body);

        // Create the array we'll use to hold the actual parameter
        // values.  We know how big to make it from the metadata.
        Object[] argValues = new Object[operation.getNumParams()];

        // A place to keep track of the out params (INOUTs and OUTs)
        ArrayList outs = new ArrayList();

        // Put the values contained in the RPCParams into an array
        // suitable for passing to java.lang.reflect.Method.invoke()
        // Make sure we respect parameter ordering if we know about it
        // from metadata, and handle whatever conversions are necessary
        // (values -> Holders, etc)
        for (int i = 0; i < numArgs; i++) {
            RPCParam rpcParam = (RPCParam) args.get(i);
            Object value = rpcParam.getObjectValue();

            // first check the type on the paramter
            ParameterDesc paramDesc = rpcParam.getParamDesc();

            // if we found some type info try to make sure the value type is
            // correct.  For instance, if we deserialized a xsd:dateTime in
            // to a Calendar and the service takes a Date, we need to convert
            if (paramDesc != null && paramDesc.getJavaType() != null) {

                // Get the type in the signature (java type or its holder)
                Class sigType = paramDesc.getJavaType();

                // Convert the value into the expected type in the signature
                value = JavaUtils.convert(value, sigType);

                rpcParam.setObjectValue(value);
                if (paramDesc.getMode() == ParameterDesc.INOUT) {
                    outs.add(rpcParam);
                }
            }

            // Put the value (possibly converted) in the argument array
            // make sure to use the parameter order if we have it
            if (paramDesc == null || paramDesc.getOrder() == -1) {
                argValues[i] = value;
            } else {
                argValues[paramDesc.getOrder()] = value;
            }

            if (log.isDebugEnabled()) {
                log.debug("  " + Messages.getMessage("value00",
                        "" + argValues[i]));
            }
        }

        // See if any subclasses want a crack at faulting on a bad operation
        // FIXME : Does this make sense here???
        String allowedMethods = (String) service.getOption("allowedMethods");
        checkMethodName(msgContext, allowedMethods, operation.getName());

        // Now create any out holders we need to pass in
        int count = numArgs;
        for (int i = 0; i < argValues.length; i++) {

            // We are interested only in OUT/INOUT
            ParameterDesc param = operation.getParameter(i);
            if(param.getMode() == ParameterDesc.IN)
                continue;

            Class holderClass = param.getJavaType();
            if (holderClass != null &&
                    Holder.class.isAssignableFrom(holderClass)) {
                int index = count;
                // Use the parameter order if specified or just stick them to the end.
                if (param.getOrder() != -1) {
                    index = param.getOrder();
                } else {
                    count++;
                }
                // If it's already filled, don't muck with it
                if (argValues[index] != null) {
                    continue;
                }
                argValues[index] = holderClass.newInstance();
                // Store an RPCParam in the outs collection so we
                // have an easy and consistent way to write these
                // back to the client below
                RPCParam p = new RPCParam(param.getQName(),
                        argValues[index]);
                p.setParamDesc(param);
                outs.add(p);
            } else {
                throw new AxisFault(Messages.getMessage("badOutParameter00",
                        "" + param.getQName(),
                        operation.getName()));
            }
        }

        // OK!  Now we can invoke the method
        Object objRes = null;
        try {
            objRes = invokeMethod(msgContext,
                                  operation.getMethod(),
                                  obj, argValues);
        } catch (IllegalArgumentException e) {
            String methodSig = operation.getMethod().toString();
            String argClasses = "";
            for (int i = 0; i < argValues.length; i++) {
                if (argValues[i] == null) {
                    argClasses += "null";
                } else {
                    argClasses += argValues[i].getClass().getName();
                }
                if (i + 1 < argValues.length) {
                    argClasses += ",";
                }
            }
            log.info(Messages.getMessage("dispatchIAE00",
                    new String[]{methodSig, argClasses}),
                    e);
            throw new AxisFault(Messages.getMessage("dispatchIAE00",
                    new String[]{methodSig, argClasses}),
                    e);
        }

        /** If this is a one-way operation, there is nothing more to do.
         */
        if (OperationType.ONE_WAY.equals(operation.getMep()))
            return;

        RPCElement resBody = createResponseBody(body, msgContext, operation, serviceDesc, objRes, resEnv, outs);
        resEnv.addBodyElement(resBody);
    }

    protected RPCElement getBody(SOAPEnvelope reqEnv, MessageContext msgContext) throws Exception {
        SOAPService service = msgContext.getService();
        ServiceDesc serviceDesc = service.getServiceDescription();
        OperationDesc operation = msgContext.getOperation();
        Vector bodies = reqEnv.getBodyElements();
        if (log.isDebugEnabled()) {
            log.debug(Messages.getMessage("bodyElems00", "" + bodies.size()));
            if(bodies.size()>0){
                log.debug(Messages.getMessage("bodyIs00", "" + bodies.get(0)));
            }
        }
        RPCElement body = null;        // Find the first "root" body element, which is the RPC call.
        for (int bNum = 0; body == null && bNum < bodies.size(); bNum++) {
            // If this is a regular old SOAPBodyElement, and it's a root,
            // we're probably a non-wrapped doc/lit service.  In this case,
            // we deserialize the element, and create an RPCElement "wrapper"
            // around it which points to the correct method.
            // FIXME : There should be a cleaner way to do this...
            if (!(bodies.get(bNum) instanceof RPCElement)) {
                SOAPBodyElement bodyEl = (SOAPBodyElement) bodies.get(bNum);
                // igors: better check if bodyEl.getID() != null
                // to make sure this loop does not step on SOAP-ENC objects
                // that follow the parameters! FIXME?
                if (bodyEl.isRoot() && operation != null && bodyEl.getID() == null) {
                    ParameterDesc param = operation.getParameter(bNum);
                    // at least do not step on non-existent parameters!
                    if (param != null) {
                        Object val = bodyEl.getValueAsType(param.getTypeQName());
                        body = new RPCElement("",
                                              operation.getName(),
                                              new Object[]{val});
                    }
                }
            } else {
                body = (RPCElement) bodies.get(bNum);
            }
        }        // special case code for a document style operation with no
        // arguments (which is a strange thing to have, but whatever)
        if (body == null) {
            // throw an error if this isn't a document style service
            if (!(serviceDesc.getStyle().equals(Style.DOCUMENT))) {
                throw new Exception(Messages.getMessage("noBody00"));
            }

            // look for a method in the service that has no arguments,
            // use the first one we find.
            ArrayList ops = serviceDesc.getOperations();
            for (Iterator iterator = ops.iterator(); iterator.hasNext();) {
                OperationDesc desc = (OperationDesc) iterator.next();
                if (desc.getNumInParams() == 0) {
                    // found one with no parameters, use it
                    msgContext.setOperation(desc);
                    // create an empty element
                    body = new RPCElement(desc.getName());
                    // stop looking
                    break;
                }
            }

            // If we still didn't find anything, report no body error.
            if (body == null) {
                throw new Exception(Messages.getMessage("noBody00"));
            }
        }
        return body;
    }

    protected OperationDesc getOperationDesc(MessageContext msgContext, RPCElement body) throws SAXException, AxisFault {
        SOAPService service = msgContext.getService();
        ServiceDesc serviceDesc = service.getServiceDescription();
        String methodName = body.getMethodName();

        // FIXME (there should be a cleaner way to do this)
        OperationDesc operation = msgContext.getOperation();
        if (operation == null) {
            QName qname = new QName(body.getNamespaceURI(),
                    body.getName());
            operation = serviceDesc.getOperationByElementQName(qname);

        if (operation == null) {
            SOAPConstants soapConstants = msgContext == null ?
                    SOAPConstants.SOAP11_CONSTANTS :
                    msgContext.getSOAPConstants();
            if (soapConstants == SOAPConstants.SOAP12_CONSTANTS) {
                AxisFault fault =
                        new AxisFault(Constants.FAULT_SOAP12_SENDER,
                                      Messages.getMessage("noSuchOperation",
                                                          methodName),
                                      null,
                                      null);
                fault.addFaultSubCode(Constants.FAULT_SUBCODE_PROC_NOT_PRESENT);
                throw new SAXException(fault);
            } else {
                throw new AxisFault(Constants.FAULT_CLIENT, Messages.getMessage("noSuchOperation", methodName),
                        null, null);
            }
            } else {
                 msgContext.setOperation(operation);
            }
        }
        return operation;
    }

    protected RPCElement createResponseBody(RPCElement body, MessageContext msgContext, OperationDesc operation, ServiceDesc serviceDesc, Object objRes, SOAPEnvelope resEnv, ArrayList outs) throws Exception
    {
        String methodName = body.getMethodName();
        /* Now put the result in the result SOAPEnvelope */
        RPCElement resBody = new RPCElement(methodName + "Response");
        resBody.setPrefix(body.getPrefix());
        resBody.setNamespaceURI(body.getNamespaceURI());
        resBody.setEncodingStyle(msgContext.getEncodingStyle());
        try {
            // Return first
            if (operation.getMethod().getReturnType() != Void.TYPE) {
                QName returnQName = operation.getReturnQName();
                if (returnQName == null) {
                    String nsp = body.getNamespaceURI();
                    if(nsp == null || nsp.length()==0) {
                        nsp = serviceDesc.getDefaultNamespace();
                    }
                    returnQName = new QName(msgContext.isEncoded() ? "" :
                                                nsp,
                                            methodName + "Return");
                }

                RPCParam param = new RPCParam(returnQName, objRes);
                param.setParamDesc(operation.getReturnParamDesc());

                if (!operation.isReturnHeader()) {
                    // For SOAP 1.2 rpc style, add a result
                    if (msgContext.getSOAPConstants() == SOAPConstants.SOAP12_CONSTANTS &&
                            (serviceDesc.getStyle().equals(Style.RPC))) {
                        RPCParam resultParam = new RPCParam(Constants.QNAME_RPC_RESULT, returnQName);
                        resultParam.setXSITypeGeneration(Boolean.FALSE);
                        resBody.addParam(resultParam);
                    }
                    resBody.addParam(param);
                } else {
                    resEnv.addHeader(new RPCHeaderParam(param));
                }

            }

            // Then any other out params
            if (!outs.isEmpty()) {
                for (Iterator i = outs.iterator(); i.hasNext();) {
                    // We know this has a holder, so just unwrap the value
                    RPCParam param = (RPCParam) i.next();
                    Holder holder = (Holder) param.getObjectValue();
                    Object value = JavaUtils.getHolderValue(holder);
                    ParameterDesc paramDesc = param.getParamDesc();

                    param.setObjectValue(value);
                    if (paramDesc != null && paramDesc.isOutHeader()) {
                        resEnv.addHeader(new RPCHeaderParam(param));
                    } else {
                        resBody.addParam(param);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
        return resBody;
    }

    /**
     * This method encapsulates the method invocation.             
     *
     * @param msgContext MessageContext
     * @param method the target method.
     * @param obj the target object
     * @param argValues the method arguments
     */
    protected Object invokeMethod(MessageContext msgContext,
                                  Method method, Object obj,
                                  Object[] argValues)
            throws Exception {
        return (method.invoke(obj, argValues));
    }

    /**
     * Throw an AxisFault if the requested method is not allowed.
     *
     * @param msgContext MessageContext
     * @param allowedMethods list of allowed methods
     * @param methodName name of target method
     */
    protected void checkMethodName(MessageContext msgContext,
                                   String allowedMethods,
                                   String methodName)
            throws Exception {
        // Our version doesn't need to do anything, though inherited
        // ones might.
    }
}

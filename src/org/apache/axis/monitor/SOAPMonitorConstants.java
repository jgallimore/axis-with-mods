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

package org.apache.axis.monitor;

/**
 * SOAP Monitor Service constants
 *
 * @author Brian Price (pricebe@us.ibm.com)
 */

public class SOAPMonitorConstants {

  /**
   * SOAP message types
   */
  public static final int SOAP_MONITOR_REQUEST  = 0;
  public static final int SOAP_MONITOR_RESPONSE = 1;

  /** 
   * Servlet initialization parameter names
   */
  public static final String SOAP_MONITOR_PORT = "SOAPMonitorPort";

  /**
   * Unique SOAP monitor id tag
   */
  public static final String SOAP_MONITOR_ID = "SOAPMonitorId";
}

/**
 * WhiteMesaSoap12AddTestSvcTestCase.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package test.wsdl.soap12.additional;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.client.Call;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.message.*;
import org.apache.axis.soap.SOAP12Constants;
import org.apache.axis.soap.SOAPConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import test.wsdl.soap12.additional.xsd.SOAPStruct;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import java.util.Vector;
import java.net.URL;

/**
 * Additional SOAP 1.2 tests.
 * 
 * For details, see:
 *  
 * http://www.w3.org/2000/xp/Group/2/03/soap1.2implementation.html#addtests
 * 
 * Auto-generated from WhiteMesa's WSDL, with additional coding by:
 * @author Davanum Srinivas (dims@apache.org)
 * @author Glen Daniels (gdaniels@apache.org)
 */ 
public class WhiteMesaSoap12AddTestSvcTestCase extends junit.framework.TestCase {
    public static final String STRING_VAL = "SOAP 1.2 is cool!";
    public static final float FLOAT_VAL = 3.14F;
    public static final Float FLOAT_OBJVAL = new Float(FLOAT_VAL);
    public static final int INT_VAL = 69;
    public static final Integer INT_OBJVAL = new Integer(INT_VAL);

    public final String TEST_NS = "http://soapinterop.org/";
    public final QName ECHO_STRING_QNAME = new QName(TEST_NS, "echoString");
    
    // Endpoints
    // TODO : Shouldn't be hardcoded!
//    public static final String HOST = "http://localhost:8080";
    public static String HOST = "http://www.whitemesa.net";
    public static String RPC_ENDPOINT = HOST + "/soap12/add-test-rpc";
    public static String DOC_ENDPOINT = HOST + "/soap12/add-test-doc";
    public static String GET_DOC_ENDPOINT = HOST + "/soap12/add-test-doc/getTime";
    public static String GET_RPC_ENDPOINT = HOST + "/soap12/add-test-rpc/getTime";
    public static String DOC_INT_ENDPOINT = HOST + "/soap12/add-test-doc-int";
    public static String DOC_INT_UC_ENDPOINT = HOST + "/soap12/add-test-doc-int-uc";
    private QName SOAPSTRUCT_QNAME = new QName("http://example.org/ts-tests/xsd", "SOAPStruct");

    static String configFile = null;

    public static void main(String[] args) throws Exception {
        // If we have an argument, it's a configuration file.
        if (args.length > 0) {
            configFile = args[0];
        }
        WhiteMesaSoap12AddTestSvcTestCase tester = new WhiteMesaSoap12AddTestSvcTestCase("testXMLP5");
        tester.setUp();
        tester.testXMLP19();
        System.out.println("Done.");
//        junit.textui.TestRunner.run(WhiteMesaSoap12AddTestSvcTestCase.class);
    }

    public WhiteMesaSoap12AddTestSvcTestCase(java.lang.String name) {
        super(name);
    }

    public void testSoap12AddTestDocUpperPortWSDL() throws Exception {
        javax.xml.rpc.ServiceFactory serviceFactory = javax.xml.rpc.ServiceFactory.newInstance();
        java.net.URL url = new java.net.URL(new test.wsdl.soap12.additional.WhiteMesaSoap12AddTestSvcLocator().getSoap12AddTestDocUpperPortAddress() + "?WSDL");
        javax.xml.rpc.Service service = serviceFactory.createService(url, new test.wsdl.soap12.additional.WhiteMesaSoap12AddTestSvcLocator().getServiceName());
        assertTrue(service != null);
    }

    public void testSoap12AddTestRpcPortWSDL() throws Exception {
        javax.xml.rpc.ServiceFactory serviceFactory = javax.xml.rpc.ServiceFactory.newInstance();
        java.net.URL url = new java.net.URL(new test.wsdl.soap12.additional.WhiteMesaSoap12AddTestSvcLocator().getSoap12AddTestRpcPortAddress() + "?WSDL");
        javax.xml.rpc.Service service = serviceFactory.createService(url, new test.wsdl.soap12.additional.WhiteMesaSoap12AddTestSvcLocator().getServiceName());
        assertTrue(service != null);
    }

    public void testSoap12AddTestDocIntermediaryPortWSDL() throws Exception {
        javax.xml.rpc.ServiceFactory serviceFactory = javax.xml.rpc.ServiceFactory.newInstance();
        java.net.URL url = new java.net.URL(new test.wsdl.soap12.additional.WhiteMesaSoap12AddTestSvcLocator().getSoap12AddTestDocIntermediaryPortAddress() + "?WSDL");
        javax.xml.rpc.Service service = serviceFactory.createService(url, new test.wsdl.soap12.additional.WhiteMesaSoap12AddTestSvcLocator().getServiceName());
        assertTrue(service != null);
    }

    public void testSoap12AddTestDocPortWSDL() throws Exception {
        javax.xml.rpc.ServiceFactory serviceFactory = javax.xml.rpc.ServiceFactory.newInstance();
        java.net.URL url = new java.net.URL(new test.wsdl.soap12.additional.WhiteMesaSoap12AddTestSvcLocator().getSoap12AddTestDocPortAddress() + "?WSDL");
        javax.xml.rpc.Service service = serviceFactory.createService(url, new test.wsdl.soap12.additional.WhiteMesaSoap12AddTestSvcLocator().getServiceName());
        assertTrue(service != null);
    }

    protected void setUp() throws Exception {
        if (configFile == null) {
            configFile = System.getProperty("configFile");
        }

        if (configFile == null) {
            return;
        }

        Document doc = XMLUtils.newDocument(configFile);
        NodeList nl = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (!(node instanceof Element))
                continue;
            Element el = (Element) node;
            String tag = el.getLocalName();
            String data = XMLUtils.getChildCharacterData(el);
            if ("host".equals(tag)) {
                HOST = data;
                RPC_ENDPOINT = HOST + "/soap12/add-test-rpc";
                DOC_ENDPOINT = HOST + "/soap12/add-test-doc";
                GET_DOC_ENDPOINT = HOST + "/soap12/add-test-doc/getTime";
                GET_RPC_ENDPOINT = HOST + "/soap12/add-test-rpc/getTime";
                DOC_INT_ENDPOINT = HOST + "/soap12/add-test-doc-int";
                DOC_INT_UC_ENDPOINT = HOST + "/soap12/add-test-doc-int-uc";
            } else if ("rpcEndpoint".equals(tag)) {
                RPC_ENDPOINT = data;
            } else if ("docEndpoint".equals(tag)) {
                DOC_ENDPOINT = data;
            } else if ("getRpcEndpoint".equals(tag)) {
                GET_RPC_ENDPOINT = data;
            } else if ("getDocEndpoint".equals(tag)) {
                GET_DOC_ENDPOINT = data;
            } else if ("docIntEndpoint".equals(tag)) {
                DOC_INT_ENDPOINT = data;
            } else if ("docIntUcEndpoint".equals(tag)) {
                DOC_INT_UC_ENDPOINT = data;
            }
        }
    }

    /**
     * Test xmlp-1 - call echoString with no arguments (even though it expects
     * one).  Confirm bad arguments fault from endpoint.
     * 
     * @throws Exception
     */ 
    public void testXMLP1() throws Exception {
        Call call = new Call(RPC_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        try {
            call.invoke(ECHO_STRING_QNAME, null);
        } catch (AxisFault fault) {
            assertEquals(Constants.FAULT_SOAP12_SENDER, fault.getFaultCode());
            QName [] subCodes = fault.getFaultSubCodes();
            assertNotNull(subCodes);
            assertEquals(1, subCodes.length);
            assertEquals(Constants.FAULT_SUBCODE_BADARGS, subCodes[0]);
            return;
        }
        fail("Didn't catch expected fault");
    }
    
    /**
     * Test xmlp-2, using the GET webmethod.
     * 
     * @throws Exception
     */ 
    public void testXMLP2() throws Exception {
        Call call = new Call(GET_DOC_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        call.setProperty(SOAP12Constants.PROP_WEBMETHOD, "GET");
        call.setOperationStyle(Style.DOCUMENT);
        call.setOperationUse(Use.LITERAL);
        call.invoke();
        SOAPEnvelope env = call.getMessageContext().getResponseMessage().getSOAPEnvelope();
        Object result = env.getFirstBody().getValueAsType(Constants.XSD_TIME);
        assertEquals(org.apache.axis.types.Time.class, result.getClass());
        // Suppose we could check the actual time here too, but we aren't
        // gonna for now.
    }

    /**
     * Test xmlp-3, using the GET webmethod and RPC mode (i.e. deal with
     * the rpc:result element).
     * 
     * @throws Exception
     */ 
    public void testXMLP3() throws Exception {
        Call call = new Call(GET_RPC_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        call.setProperty(SOAP12Constants.PROP_WEBMETHOD, "GET");
        call.setOperationStyle(Style.RPC);
        call.setReturnType(Constants.XSD_TIME);
        Object ret = call.invoke("", new Object [] {});
        assertEquals(org.apache.axis.types.Time.class, ret.getClass());
        // Suppose we could check the actual time here too, but we aren't
        // gonna for now.
    }
    
    public void testXMLP4() throws Exception {
        Call call = new Call(RPC_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        call.registerTypeMapping(SOAPStruct.class, SOAPSTRUCT_QNAME,
                                 new BeanSerializerFactory(SOAPStruct.class, SOAPSTRUCT_QNAME),
                                 new BeanDeserializerFactory(SOAPStruct.class, SOAPSTRUCT_QNAME));
        call.addParameter(new QName("", "inputFloat"),
                          Constants.XSD_FLOAT, ParameterMode.IN);
        call.addParameter(new QName("", "inputInteger"),
                          Constants.XSD_INT, ParameterMode.IN);
        call.addParameter(new QName("", "inputString"),
                          Constants.XSD_STRING, ParameterMode.IN);
        call.setReturnType(SOAPSTRUCT_QNAME);
        SOAPStruct ret = (SOAPStruct)call.invoke(
                new QName(TEST_NS, "echoSimpleTypesAsStruct"),
                new Object [] {
                    new Float(FLOAT_VAL),
                    new Integer(INT_VAL),
                    STRING_VAL 
                });
        assertEquals(STRING_VAL, ret.getVarString());
        assertEquals(FLOAT_VAL, ret.getVarFloat(), 0.0004F);
        assertEquals(INT_VAL, ret.getVarInt());
    }
    
    public void testXMLP5() throws Exception {
        Call call = new Call(RPC_ENDPOINT);
        try {
            call.invoke(new QName(TEST_NS, "echoVoid"), null);        
        } catch (AxisFault fault) {
            // Got the expected Fault - make sure it looks right
            assertEquals(Constants.FAULT_VERSIONMISMATCH, fault.getFaultCode());
            return;
        }
        fail("Didn't catch expected fault");
    }
    
    public void testXMLP6() throws Exception {
        Call call = new Call(RPC_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        SOAPHeaderElement unknownHeader =
                new SOAPHeaderElement("http://example.org",
                                      "unknown",
                                      "Nobody understands me!");
        unknownHeader.setMustUnderstand(true);
        call.addHeader(unknownHeader);
        try {
            call.invoke(new QName(TEST_NS, "echoVoid"), null);        
        } catch (AxisFault fault) {
            // Got the expected Fault - make sure it looks right
            assertEquals(Constants.FAULT_SOAP12_MUSTUNDERSTAND,
                         fault.getFaultCode());
            return;
        }
        fail("Didn't catch expected fault");        
    }
    
    public void testXMLP7() throws Exception {
        URL url = new URL(DOC_ENDPOINT);
        test.wsdl.soap12.additional.Soap12AddTestDocBindingStub binding;
        try {
            binding = (test.wsdl.soap12.additional.Soap12AddTestDocBindingStub)
                          new test.wsdl.soap12.additional.WhiteMesaSoap12AddTestSvcLocator().getSoap12AddTestDocPort(url);
        }
        catch (javax.xml.rpc.ServiceException jre) {
            if(jre.getLinkedCause()!=null)
                jre.getLinkedCause().printStackTrace();
            throw new junit.framework.AssertionFailedError("JAX-RPC ServiceException caught: " + jre);
        }
        assertNotNull("binding is null", binding);

        // Time out after a minute
        binding.setTimeout(60000);

        // Test operation
        try {
            binding.echoSenderFault(STRING_VAL);
        } catch (java.rmi.RemoteException e) {
            if (e instanceof AxisFault) {
                AxisFault af = (AxisFault)e;
                assertEquals(Constants.FAULT_SOAP12_SENDER,
                             af.getFaultCode());
                return; // success
            }
        }
        
        fail("Should have received sender fault!");
    }
    
    public void testXMLP8() throws Exception {
        Call call = new Call(DOC_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        QName qname = new QName(TEST_NS, "echoReceiverFault");
        try {
            call.invoke(qname, null);
        } catch (AxisFault af) {
            assertEquals(Constants.FAULT_SOAP12_RECEIVER,
                    af.getFaultCode());
            return; // success
        }
        
        fail("Should have received receiver fault!");
    }
    
    /**
     * Test xmlp-9 : do an "echoString" call with a bad (unknown) encoding
     * style on the argument.  Confirm Sender fault from endpoint.
     * 
     * @throws Exception
     */ 
    public void testXMLP9() throws Exception {
        Call call = new Call(RPC_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        SOAPEnvelope reqEnv = new SOAPEnvelope(SOAPConstants.SOAP12_CONSTANTS);
        SOAPBodyElement body = new SOAPBodyElement(new PrefixedQName(TEST_NS, "echoString", "ns"));
        reqEnv.addBodyElement(body);
        MessageElement arg = new MessageElement("", "inputString");
        arg.setEncodingStyle("http://this-is-a-bad-encoding-style");
        body.addChild(arg);
        try {
            call.invoke(reqEnv);
        } catch (AxisFault fault) {
            assertEquals(Constants.FAULT_SOAP12_DATAENCODINGUNKNOWN, fault.getFaultCode());            
            return;
        }
        fail("Didn't catch expected fault");                
    }

    /**
     * Test xmlp-10 : reply with the schema types of the arguments, in order
     */
//    public void testXMLP10() throws Exception {
//        Call call = new Call(RPC_ENDPOINT);
//        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
//        SOAPEnvelope reqEnv = new SOAPEnvelope(SOAPConstants.SOAP12_CONSTANTS);
//        SOAPBodyElement body = new SOAPBodyElement(
//                new PrefixedQName(TEST_NS,
//                        "echoSimpleTypesAsStructOfSchemaTypes", "ns"));
//        reqEnv.addBodyElement(body);
//        MessageElement arg = new MessageElement("", "input1");
//        arg.setObjectValue(new Integer(5));
//        body.addChild(arg);
//        arg = new MessageElement("", "input2");
//        arg.setObjectValue(new Float(5.5F));
//        body.addChild(arg);
//        arg = new MessageElement("", "input3");
//        arg.setObjectValue("hi there");
//        body.addChild(arg);
//        arg = new MessageElement("", "input4");
//        Text text = new Text("untyped");
//        arg.addChild(text);
//        body.addChild(arg);
//        call.invoke(reqEnv);
//    }

    /**
     * Test xmlp-11 : send a string where an integer is expected, confirm
     * BadArguments fault.
     * 
     * @throws Exception
     */ 
    public void testXMLP11() throws Exception {
        Call call = new Call(RPC_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        call.setProperty(Call.SEND_TYPE_ATTR, Boolean.FALSE);
        try {
            call.invoke(new QName(TEST_NS, "echoInteger"),
                        new Object [] { new RPCParam("inputInteger",
                                                     "ceci n'est pas un int")
                                      }
                       );
        } catch (AxisFault fault) {
            assertEquals(Constants.FAULT_SOAP12_SENDER, fault.getFaultCode());
            QName [] subCodes = fault.getFaultSubCodes();
            assertNotNull(subCodes);
            assertEquals(1, subCodes.length);
            assertEquals(Constants.FAULT_SUBCODE_BADARGS, subCodes[0]);
            return;
        }
        fail("Didn't catch expected fault");        
    }
    
    /**
     * Test xmlp-12 : unknown method call to RPC endpoint.  Confirm
     * "ProcedureNotPresent" subcode of "Sender" fault.
     * 
     * @throws Exception
     */ 
    public void testXMLP12() throws Exception {
        Call call = new Call(RPC_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        call.addParameter(new QName("inputInteger"), Constants.XSD_INT, ParameterMode.IN);
        try {
            call.invoke(new QName(TEST_NS, "unknownFreakyMethod"), new Object [] { new Integer(5) });
        } catch (AxisFault fault) {
            assertEquals(Constants.FAULT_SOAP12_SENDER, fault.getFaultCode());
            QName [] subCodes = fault.getFaultSubCodes();
            assertNotNull(subCodes);
            assertEquals(1, subCodes.length);
            assertEquals(Constants.FAULT_SUBCODE_PROC_NOT_PRESENT, subCodes[0]);
            return;
        }
        fail("Didn't catch expected fault");
    }
    
    /**
     * Test xmlp-13 : doc/lit echoString which sends back the original
     * message via a transparent "forwarding intermediary"
     * 
     */ 
    public void testXMLP13() throws Exception {
        String ARG = "i FeEL sTrAnGEly CAsEd (but I like it)";
        Call call = new Call(DOC_INT_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        call.setOperationStyle(Style.WRAPPED);
        call.addParameter(new QName(TEST_NS, "inputString"),
                          Constants.XSD_STRING, ParameterMode.IN);
        call.setReturnType(Constants.XSD_STRING);
        
        String ret = (String)call.invoke(ECHO_STRING_QNAME, new Object [] { ARG });
        assertEquals("Return didn't match argument", ARG, ret);        
    }

    /**
     * Test xmlp-14 : doc/lit echoString which sends back the original
     * message via an "active intermediary" (translating the string
     * to uppercase)
     * 
     */ 
    public void testXMLP14() throws Exception {
        String ARG = "i FeEL sTrAnGEly CAsEd (and dream of UPPER)";
        Call call = new Call(DOC_INT_UC_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        call.setOperationStyle(Style.WRAPPED);
        call.addParameter(new QName(TEST_NS, "inputString"),
                          Constants.XSD_STRING, ParameterMode.IN);
        call.setReturnType(Constants.XSD_STRING);
        
        String ret = (String)call.invoke(ECHO_STRING_QNAME, new Object [] { ARG });
        assertEquals("Return wasn't uppercased argument", ARG.toUpperCase(), ret);
    }
    
    public void testXMLP15() throws Exception {
        String HEADER_VAL = "I'm going to be discarded!";
        String HEADER_NS = "http://test-xmlp-15";
        String HEADER_NAME = "unknown";
        
        Call call = new Call(DOC_INT_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);
        call.setOperationStyle(Style.WRAPPED);
        call.setOperationUse(Use.LITERAL);

        SOAPHeaderElement header = new SOAPHeaderElement(HEADER_NS, HEADER_NAME);
        header.setRole(Constants.URI_SOAP12_NEXT_ROLE);
        header.setObjectValue(HEADER_VAL);
        call.addHeader(header);
        
        call.invoke(ECHO_STRING_QNAME, new Object [] { "body string" });
        
        SOAPEnvelope respEnv = call.getMessageContext().getResponseMessage().getSOAPEnvelope();
        
        // Confirm we got no headers back
        Vector headers = respEnv.getHeaders();
        assertTrue("Headers Vector wasn't empty", headers.isEmpty());
    }
    
    public void testXMLP16() throws Exception {
        String HEADER_VAL = "I'm going all the way through!";
        String HEADER_NS = "http://test-xmlp-16";
        String HEADER_NAME = "unknown";
        QName HEADER_QNAME = new QName(HEADER_NS, HEADER_NAME);
        
        Call call = new Call(DOC_INT_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);

        SOAPHeaderElement header = new SOAPHeaderElement(HEADER_NS, HEADER_NAME);
        header.setRole(Constants.URI_SOAP12_NONE_ROLE);
        header.setObjectValue(HEADER_VAL);
        call.addHeader(header);
        
        call.invoke(ECHO_STRING_QNAME, new Object [] { "body string" });
        
        SOAPEnvelope respEnv = call.getMessageContext().getResponseMessage().getSOAPEnvelope();
        
        // Confirm we got our header back
        Vector headers = respEnv.getHeaders();
        assertEquals(1, headers.size());
        SOAPHeaderElement respHeader = (SOAPHeaderElement)headers.get(0);
        assertEquals(Constants.URI_SOAP12_NONE_ROLE, respHeader.getRole());
        assertEquals(HEADER_QNAME, respHeader.getQName());
        assertEquals(HEADER_VAL, respHeader.getValue());
    }
    
    public void testXMLP17() throws Exception {
        String HEADER_VAL = "I'm going all the way through!";
        String HEADER_NS = "http://test-xmlp-17";
        String HEADER_NAME = "seekrit";
        QName HEADER_QNAME = new QName(HEADER_NS, HEADER_NAME);
        
        Call call = new Call(DOC_INT_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);

        SOAPHeaderElement header = new SOAPHeaderElement(HEADER_NS, HEADER_NAME);
        header.setRole(Constants.URI_SOAP12_ULTIMATE_ROLE);
        header.setObjectValue(HEADER_VAL);
        call.addHeader(header);
        
        call.invoke(ECHO_STRING_QNAME, new Object [] { "body string" });
        
        SOAPEnvelope respEnv = call.getMessageContext().getResponseMessage().getSOAPEnvelope();
        
        // Confirm we got a single header back, targeted at the ultimate
        // receiver
        Vector headers = respEnv.getHeaders();
        assertEquals(1, headers.size());
        SOAPHeaderElement respHeader = (SOAPHeaderElement)headers.get(0);
        assertEquals(Constants.URI_SOAP12_ULTIMATE_ROLE, respHeader.getRole());
        assertEquals(HEADER_QNAME, respHeader.getQName());
        assertEquals(HEADER_VAL, respHeader.getValue());
    }

    public void testXMLP18() throws Exception {
        String HEADER_VAL = "I'm going all the way through!";
        String HEADER_NS = "http://test-xmlp-17";
        String HEADER_NAME = "seekrit";
        QName HEADER_QNAME = new QName(HEADER_NS, HEADER_NAME);
        
        Call call = new Call(DOC_INT_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);

        SOAPHeaderElement header = new SOAPHeaderElement(HEADER_NS, HEADER_NAME);
        header.setRole(Constants.URI_SOAP12_NEXT_ROLE);
        header.setRelay(true);
        header.setObjectValue(HEADER_VAL);
        call.addHeader(header);
        
        call.invoke(ECHO_STRING_QNAME, new Object [] { "body string" });
        
        SOAPEnvelope respEnv = call.getMessageContext().getResponseMessage().getSOAPEnvelope();
        
        // Confirm we got a single header back, targeted at the ultimate
        // receiver
        Vector headers = respEnv.getHeaders();
        assertEquals(1, headers.size());
        SOAPHeaderElement respHeader = (SOAPHeaderElement)headers.get(0);
        assertEquals(Constants.URI_SOAP12_NEXT_ROLE, respHeader.getRole());
        assertTrue(respHeader.getRelay());
        assertEquals(HEADER_QNAME, respHeader.getQName());
        assertEquals(HEADER_VAL, respHeader.getValue());
    }

    public void testXMLP19() throws Exception {
        String HEADER_VAL = "I'm going to generate a fault!";
        String HEADER_NS = "http://test-xmlp-17";
        String HEADER_NAME = "seekrit";
        
        Call call = new Call(DOC_INT_ENDPOINT);
        call.setSOAPVersion(SOAPConstants.SOAP12_CONSTANTS);

        SOAPHeaderElement header = new SOAPHeaderElement(HEADER_NS, HEADER_NAME);
        header.setRole(Constants.URI_SOAP12_NEXT_ROLE);
        header.setMustUnderstand(true);
        header.setObjectValue(HEADER_VAL);
        call.addHeader(header);
        
        try {
            call.invoke(ECHO_STRING_QNAME, new Object [] { "body string" });
        } catch (AxisFault fault) {
            // Got the expected Fault - make sure it looks right
            assertEquals(Constants.FAULT_SOAP12_MUSTUNDERSTAND,
                         fault.getFaultCode());
            return;
        }

        fail("Didn't catch expected fault");        
    }
}

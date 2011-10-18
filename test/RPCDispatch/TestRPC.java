package test.RPCDispatch;

import junit.framework.TestCase;
import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.message.RPCElement;
import org.apache.axis.message.RPCParam;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.providers.java.RPCProvider;
import org.apache.axis.server.AxisServer;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.util.Vector;

/**
 * Test org.apache.axis.handlers.RPCDispatcher
 *
 * @author Sam Ruby <rubys@us.ibm.com>
 */
public class TestRPC extends TestCase {

    private final String header =
        "<?xml version=\"1.0\"?>\n" +
        "<soap:Envelope " +
             "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
             "xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
             "xmlns:xsi=\"" + Constants.URI_DEFAULT_SCHEMA_XSI + "\" " +
             "xmlns:xsd=\"" + Constants.URI_DEFAULT_SCHEMA_XSD + "\">\n" +
             "<soap:Body>\n";

    private final String footer =
             "</soap:Body>\n" +
        "</soap:Envelope>\n";

    private SimpleProvider provider = new SimpleProvider();
    private AxisServer engine = new AxisServer(provider);

    private String SOAPAction = "urn:reverse";

    public TestRPC(String name) {
        super(name);
        engine.init();
    }

    /**
     * Invoke a given RPC method, and return the result
     * @return Deserialized result
     */
    private final Object rpc(String method, Object[] parms)
        throws AxisFault, SAXException
    {

        // Create the message context
        MessageContext msgContext = new MessageContext(engine);

        // Set the dispatch either by SOAPAction or methodNS
        String methodNS = null;
        msgContext.setTargetService(SOAPAction);

        // Construct the soap request
        SOAPEnvelope envelope = new SOAPEnvelope();
        msgContext.setRequestMessage(new Message(envelope));
        RPCElement body = new RPCElement(methodNS, method, parms);
        envelope.addBodyElement(body);

        // Invoke the Axis engine
        engine.invoke(msgContext);

        // Extract the response Envelope
        Message message = msgContext.getResponseMessage();
        envelope = (SOAPEnvelope)message.getSOAPEnvelope();
        assertNotNull("SOAP envelope was null", envelope);

        // Extract the body from the envelope
        body = (RPCElement)envelope.getFirstBody();
        assertNotNull("SOAP body was null", body);

        // Extract the list of parameters from the body
        Vector arglist = body.getParams();
        assertNotNull("SOAP argument list was null", arglist);
        if (arglist.size()==0) return null;

        // Return the first parameter
        RPCParam param = (RPCParam) arglist.get(0);
        return param.getObjectValue();
    }

    /**
     * Test a simple method that reverses a string
     */
    public void testReverseString() throws Exception {
        // Register the reverseString service
        SOAPService reverse = new SOAPService(new RPCProvider());
        reverse.setOption("className", "test.RPCDispatch.Service");
        reverse.setOption("allowedMethods", "reverseString");
        provider.deployService(new QName(null,SOAPAction), reverse);

        // invoke the service and verify the result
        assertEquals("Did not reverse the string correctly.", "cba",
                     rpc("reverseString", new Object[] {"abc"}));
    }

    /**
     * Test a simple method that reverses a string (but fails)
     */
    public void testReverseStringThatShouldFail() throws Exception {
        try {
            // Register the reverseString service
            SOAPService reverse = new SOAPService(new RPCProvider());
            reverse.setOption("className", "test.RPCDispatch.Service");
            reverse.setOption("allowedMethods", "reverseString2");
            provider.deployService(new QName(null,SOAPAction), reverse);

            // invoke the service and verify the result
            rpc("reverseString", new Object[] {"abc"});
            throw new junit.framework.AssertionFailedError("Should not reach here");
        } catch (AxisFault af){
            //This test should cause an exception.
            return;
        }
    }
    
    /**
     * Test a simple method that reverses a string (with a comma delimiter
     */
    public void testReverseStringWithCommaDelimiter() throws Exception {
        // Register the reverseString service
        SOAPService reverse = new SOAPService(new RPCProvider());
        reverse.setOption("className", "test.RPCDispatch.Service");
        reverse.setOption("allowedMethods", "reverseString2,reverseString");
        provider.deployService(new QName(null,SOAPAction), reverse);

        // invoke the service and verify the result
        assertEquals("Did not reverse the string correctly.", "cba", rpc("reverseString", new Object[] {"abc"}));
    }

    /**
     * Test a simple method that reverses a string (with a space delimiter
     */
    public void testReverseStringWithSpaceDelimiter() throws Exception {
        // Register the reverseString service
        SOAPService reverse = new SOAPService(new RPCProvider());
        reverse.setOption("className", "test.RPCDispatch.Service");
        reverse.setOption("allowedMethods", "reverseString2 reverseString");
        provider.deployService(new QName(null,SOAPAction), reverse);

        // invoke the service and verify the result
        assertEquals("Did not reverse the string correctly.", "cba", rpc("reverseString", new Object[] {"abc"}));
    }

    /**
     * Test a simple method that reverses a string (with a '*'
     */
    public void testReverseStringWithStar() throws Exception {
        // Register the reverseString service
        SOAPService reverse = new SOAPService(new RPCProvider());
        reverse.setOption("className", "test.RPCDispatch.Service");
        reverse.setOption("allowedMethods", "*");
        provider.deployService(new QName(null,SOAPAction), reverse);

        // invoke the service and verify the result
        assertEquals("Did not reverse the string correctly.", "cba",
                     rpc("reverseString", new Object[] {"abc"}));
    }
    
    /**
     * Test a method that reverses a data structure
     */
    public void testReverseData() throws Exception {
        // Register the reverseData service
        SOAPService reverse = new SOAPService(new RPCProvider());
        reverse.setOption("className", "test.RPCDispatch.Service");
        reverse.setOption("allowedMethods", "reverseData");
        provider.deployService(new QName(null, SOAPAction), reverse);

        // invoke the service and verify the result
        Data input    = new Data(5, "abc", 3);
        Data expected = new Data(3, "cba", 5);
        assertEquals("Did not reverse the data as expected.", expected, rpc("reverseData", new Object[] {input}));
    }

    /**
     * Test a simple method that returns a field from the message context
     */
    public void testMessageContextImplicit() throws Exception {
        // Register the targetService service
        SOAPService tgtSvc = new SOAPService(new RPCProvider());
        tgtSvc.setOption("className", "test.RPCDispatch.Service");
        tgtSvc.setOption("allowedMethods", "targetServiceImplicit");
        provider.deployService(new QName(null, SOAPAction), tgtSvc);

        // invoke the service and verify the result
        assertEquals("SOAP Action did not equal the targetService.", 
            SOAPAction, rpc("targetServiceImplicit", new Object[] {}));
    }

    /**
     * Test a simple method that accepts and returns a null
     */
    public void testNull() throws Exception {
        // Register the echoInt service
        SOAPService echoInt = new SOAPService(new RPCProvider());
        echoInt.setOption("className", "test.RPCDispatch.Service");
        echoInt.setOption("allowedMethods", "echoInt");
        provider.deployService(new QName(null, SOAPAction), echoInt);

        // invoke the service and verify the result
        assertNull("The result was not null as expected.", rpc("echoInt", new Object[] {null}));
    }
    
    /**
     * Test faults
     */
    public void testSimpleFault() throws Exception {
        // Register the reverseData service
        SOAPService simpleFault = new SOAPService(new RPCProvider());
        simpleFault.setOption("className", "test.RPCDispatch.Service");
        simpleFault.setOption("allowedMethods", "simpleFault");
        provider.deployService(new QName(null, SOAPAction), simpleFault);

        try {
            rpc("simpleFault", new Object[] {"foobar"});
        } catch (AxisFault result) {
            assertEquals("faultString was not set correctly.",
                "test.RPCDispatch.Service$TestFault: foobar",
                result.getFaultString());
            return;
        }

        fail("Did not get an expected fault!");
    }

    public static void main(String args[])
    {
      try {
        TestRPC tester = new TestRPC("RPC test");
        tester.testReverseString();
        tester.testReverseData();
        tester.testSimpleFault();
        tester.testReverseStringThatShouldFail();
        tester.testReverseStringWithCommaDelimiter();
        tester.testReverseStringWithSpaceDelimiter();
        tester.testReverseStringWithStar();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
}

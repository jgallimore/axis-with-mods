package test.functional;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.axis.utils.ClassUtils;

/**
 * Axis's FunctionalTests test client/server interactions.
 */
public class FunctionalTests extends TestCase
{
    public FunctionalTests(String name)
    {
        super(name);
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();

        // Echo test - end to end serialization and deserialization /
        // interop tests.
        suite.addTestSuite(TestEchoSample.class);

        // Test the JAX-RPC compliance samples
        suite.addTestSuite(TestJAXRPCSamples.class);

        // Test the JAXM compliance samples
        suite.addTestSuite(TestJAXMSamples.class);
        
        // stock sample test
        // run this BEFORE ALL OTHER TESTS to minimize confusion;
        // this will run the JWS test first, and we want to know that
        // nothing else has been deployed
        suite.addTestSuite(TestStockSample.class);

        // JWS global types test (deploys a typeMapping)
        suite.addTestSuite(TestJWSGlobalTypes.class);

        // TCP transport sample test
        suite.addTestSuite(TestTCPTransportSample.class);
        
        // file transport sample test
        suite.addTestSuite(TestTransportSample.class);

        // bid-buy test
        suite.addTestSuite(TestBidBuySample.class);

        // "Raw" echo service test.
        suite.addTestSuite(TestMiscSample.class);

        // Proxy service test.
        //suite.addTestSuite(TestProxySample.class);

        // Element service test.
        suite.addTestSuite(TestElementSample.class);

        // Message service test.
        suite.addTestSuite(TestMessageSample.class);
        
        // test.rpc test
        suite.addTestSuite(TestIF3SOAP.class);
        
        // samples.fault test
        suite.addTestSuite(TestFaultsSample.class);

        suite.addTestSuite(TestEncoding.class);

        // Attachments service test.
        try{
          if( null != ClassUtils.forName("javax.activation.DataHandler") &&
              null != ClassUtils.forName("javax.mail.internet.MimeMultipart")){
                suite.addTestSuite( ClassUtils.forName("test.functional.TestAttachmentsSample"));
          }
        }catch( Throwable t){;}

        // MIME headers test.
        // BROKEN - COMMENTED OUT FOR NOW --gdaniels
        //suite.addTestSuite(TestMimeHeaders.class);

        suite.addTestSuite(TestAutoTypes.class);
        
        return suite;
    }
}

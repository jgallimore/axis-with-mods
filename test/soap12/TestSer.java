package test.soap12;

import junit.framework.TestCase;
import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.encoding.TypeMappingRegistry;
import org.apache.axis.message.RPCElement;
import org.apache.axis.message.RPCParam;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.server.AxisServer;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.XMLUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import test.encoding.Data;
import test.encoding.DataDeserFactory;
import test.encoding.DataSerFactory;

import javax.xml.namespace.QName;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/** Little serialization test with a struct.
 */
public class TestSer extends TestCase {
    Log log =
            LogFactory.getLog(TestSer.class.getName());

    public static final String myNS = "urn:myNS";

    public TestSer(String name) {
        super(name);
    }

    public void testDataNoHrefs () throws Exception {
        doTestData(false);
    }

    public void testDataWithHrefs () throws Exception {
        doTestData(true);
    }

    public void doTestData (boolean multiref) throws Exception {
        MessageContext msgContext = new MessageContext(new AxisServer());
        msgContext.setSOAPConstants(SOAPConstants.SOAP12_CONSTANTS);
        msgContext.setProperty(Constants.MC_NO_OPERATION_OK, Boolean.TRUE);
        
        SOAPEnvelope msg = new SOAPEnvelope(SOAPConstants.SOAP12_CONSTANTS);
        RPCParam arg1 = new RPCParam("urn:myNamespace", "testParam", "this is a string");

        Data data = new Data();
        data.stringMember = "String member";
        data.floatMember = new Float("4.54");

        RPCParam arg2 = new RPCParam("", "struct", data);
        RPCElement body = new RPCElement("urn:myNamespace", "method1", new Object[]{ arg1, arg2 });
        msg.addBodyElement(body);

        Writer stringWriter = new StringWriter();
        SerializationContext context = new SerializationContext(stringWriter, msgContext);
        context.setDoMultiRefs(multiref);

        // Create a TypeMapping and register the specialized Type Mapping
        TypeMappingRegistry reg = context.getTypeMappingRegistry();
        TypeMapping tm = (TypeMapping) reg.createTypeMapping();
        tm.setSupportedEncodings(new String[] {Constants.URI_SOAP12_ENC});
        reg.register(Constants.URI_SOAP12_ENC, tm);

        QName dataQName = new QName("typeNS", "Data");
        tm.register(Data.class, dataQName, new DataSerFactory(), new DataDeserFactory());

        msg.output(context);

        String msgString = stringWriter.toString();

        log.debug("---");
        log.debug(msgString);
        log.debug("---");

        StringReader reader = new StringReader(msgString);

        DeserializationContext dser = new DeserializationContext(
            new InputSource(reader), msgContext, org.apache.axis.Message.REQUEST);
        dser.parse();

        SOAPEnvelope env = dser.getEnvelope();
        RPCElement rpcElem = (RPCElement)env.getFirstBody();
        RPCParam struct = rpcElem.getParam("struct");
        assertNotNull("No <struct> param", struct);

        Data val = (Data)struct.getObjectValue();
        assertNotNull("No value for struct param", val);

        assertEquals("Data and Val string members are not equal", data.stringMember, val.stringMember);
        assertEquals("Data and Val float members are not equal",data.floatMember.floatValue(),
                     val.floatMember.floatValue(), 0.00001F);
    }

    /**
     * Test RPC element serialization when we have no MessageContext
     */
    public void testRPCElement()
    {
        try {
            SOAPEnvelope env = new SOAPEnvelope();
            RPCElement method = new RPCElement("ns",
                                               "method",
                                               new Object [] { "argument" });
            env.addBodyElement(method);
            String soapStr = env.toString();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // If there was no exception, we succeeded in serializing it.
    }

    public void testEmptyXMLNS()
    {
        try {
            MessageContext msgContext = new MessageContext(new AxisServer());
            msgContext.setSOAPConstants(SOAPConstants.SOAP12_CONSTANTS);
            msgContext.setProperty(Constants.MC_NO_OPERATION_OK, Boolean.TRUE);
            
            String req =
                "<xsd1:A xmlns:xsd1=\"urn:myNamespace\">"
                    + "<xsd1:B>"
                    + "<xsd1:C>foo bar</xsd1:C>"
                    + "</xsd1:B>"
                    + "</xsd1:A>";

            StringWriter stringWriter=new StringWriter();
            StringReader reqReader = new StringReader(req);
            InputSource reqSource = new InputSource(reqReader);

            Document document = XMLUtils.newDocument(reqSource);

            String msgString = null;

            SOAPEnvelope msg = new SOAPEnvelope(SOAPConstants.SOAP12_CONSTANTS);
            RPCParam arg1 = new RPCParam("urn:myNamespace", "testParam", document.getFirstChild());
            arg1.setXSITypeGeneration(Boolean.FALSE);

            RPCElement body = new RPCElement("urn:myNamespace", "method1", new Object[] { arg1 });
            msg.addBodyElement(body);
            body.setEncodingStyle(Constants.URI_LITERAL_ENC);

            SerializationContext context = new SerializationContext(stringWriter, msgContext);
            msg.output(context);

            msgString = stringWriter.toString();
            assertTrue(msgString.indexOf("xmlns=\"\"")==-1);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}

package test.encoding;

import junit.framework.TestCase;
import org.apache.axis.AxisEngine;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.axis.message.SOAPBody;
import org.apache.axis.server.AxisServer;
import org.apache.axis.utils.XMLUtils;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.Diff;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.StringWriter;

/**

 * Verify that deserialization actually can cause the soap service

 * to be set...

 */

public class TestDOM extends XMLTestCase {

    public TestDOM(String name) {
        super(name);
    }

    private String header =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<SOAP-ENV:Envelope" +
        " SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"" +
        " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
        " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" +
        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
        "  <SOAP-ENV:Header>\n" +
        "    <SOAP-SEC:signature SOAP-ENV:actor=\"null\" SOAP-ENV:mustUnderstand=\"1\"" +
        " xmlns:SOAP-SEC=\"http://schemas.xmlsoap.org/soap/security/\">\n" +
        "       <Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
        "       </Signature>\n" +
        "    </SOAP-SEC:signature>\n" +
        "  </SOAP-ENV:Header>\n" +
        "  <SOAP-ENV:Body id=\"body\">\n" +
        "    <ns1:getQuote xmlns:ns1=\"urn:xmltoday-delayed-quotes\">\n";

    private String request1 =
        "      <symbol xsi:type=\"xsd:string\">IBM</symbol>\n";

    private String request2 =
        "      <addResult xsi:type=\"xsd:int\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">4</addResult>\n";

    private String footer =
        "    </ns1:getQuote>\n" +
        "  </SOAP-ENV:Body>\n" +
        "</SOAP-ENV:Envelope>";

    public void testDOM() throws Exception {

       // setup
       AxisEngine engine = new AxisServer();
       engine.init();
       MessageContext msgContext = new MessageContext(engine);
       msgContext.setHighFidelity(true);
       String request = header + request1 + footer;
       Message message = new Message(request);
       message.setMessageContext(msgContext);

       // Now completely round trip it
       message.getSOAPEnvelope();
       // Element dom = message.getAsDOM();
       String result = message.getSOAPPartAsString();

       assertXMLEqual("Request is not the same as the result.", request, result);
    }

    public void testHeaders() throws Exception {
       AxisEngine engine = new AxisServer();
       engine.init();
       MessageContext msgContext = new MessageContext(engine);
       msgContext.setHighFidelity(true);
        String request = header + request1 + footer;
       Message message = new Message(request);
       message.setMessageContext(msgContext);

       // Now completely round trip it
       SOAPEnvelope envelope = message.getSOAPEnvelope();
       envelope.addHeader(new SOAPHeaderElement("foo1", "foo1"));
       envelope.addHeader(new SOAPHeaderElement("foo2", "foo2"));
       envelope.addHeader(new SOAPHeaderElement("foo3", "foo3"));
       String result = message.getSOAPPartAsString();

       assertTrue(result.indexOf("foo1")!=-1);
       assertTrue(result.indexOf("foo2")!=-1);
       assertTrue(result.indexOf("foo3")!=-1);

       Message message2 = new Message(result);
       message2.setMessageContext(msgContext);
       message2.getSOAPEnvelope();
       String result2 = message2.getSOAPPartAsString();

       assertTrue(result2.indexOf("foo1")!=-1);
       assertTrue(result2.indexOf("foo2")!=-1);
       assertTrue(result2.indexOf("foo3")!=-1);
    }

    /**
     * Test for Bug 7132
     */
    public void testAttributes() throws Exception {
       AxisEngine engine = new AxisServer();
       engine.init();
       MessageContext msgContext = new MessageContext(engine);
       msgContext.setHighFidelity(true);
       String request = header + request2 + footer;
       Message message = new Message(request);
       message.setMessageContext(msgContext);
       SOAPEnvelope envelope = message.getSOAPEnvelope();
       SOAPBodyElement bodyElement = (SOAPBodyElement)envelope.getBodyElements().elementAt(0);
       MessageElement me = (MessageElement) bodyElement.getChildren().get(0);
       org.xml.sax.Attributes atts = me.getCompleteAttributes();
       assertTrue(atts.getLength()==2);
    }

    public void testEmptyNode() throws Exception
    {
        SOAPBodyElement body = new SOAPBodyElement(XMLUtils.newDocument().createElementNS(null,"tmp"));
        assertXMLEqual("<tmp/>",body.toString());
    }

    public void testNodeWithAttribute() throws Exception
    {
        org.w3c.dom.Element element = XMLUtils.newDocument().createElementNS(null,"tmp");
        element.setAttributeNS(null,"attrib", "foo");
        SOAPBodyElement body = new SOAPBodyElement(element);
        assertXMLEqual("<tmp attrib=\"foo\"/>",body.toString());
    }

    public void testDOM2() throws Exception
    {
        // Simulate receiving a signed message.
        //
        String xml1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <soapenv:Body>\n";
        String xml2 = "    <SASLResponse xmlns=\"urn:liberty:sa:2004-04\">\n" +
            "      <Status code=\"OK\" comment=\"Authenticated\"/>\n" +
            "    </SASLResponse>\n";
        String xml3 =
            "  </soapenv:Body>\n" +
            "</soapenv:Envelope>";

        DeserializationContext ctx = new DeserializationContext(new InputSource(new StringReader(xml1 + xml2 + xml3)), null, "response");
        ctx.parse();

        SOAPEnvelope env = ctx.getEnvelope();
        SOAPBody body = (SOAPBody) env.getBody();

        // I am using the body child as my "token".  The basic idea is that
        // this element must be serialized _exactly_ as it was received.
        MessageElement elt = (MessageElement) body.getFirstChild();
        assertTrue(!elt.isDirty());

        StringWriter writer = new StringWriter();
        SerializationContext serializeContext = new SerializationContext(writer, null);
        serializeContext.setSendDecl(false);
        elt.output(serializeContext);
        writer.close();

        assertXMLIdentical("Deserialization invalidated XML",
                new Diff(xml2, writer.getBuffer().toString()), true);
    }

    public static void main(String [] args) throws Exception
    {
        TestDOM tester = new TestDOM("TestDOM");
        tester.testAttributes();
        tester.testHeaders();
        tester.testNodeWithAttribute();
        tester.testEmptyNode();
        tester.testDOM();
    }
}


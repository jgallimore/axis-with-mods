package test.saaj;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.Name;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.Text;
import javax.xml.soap.Detail;
import javax.xml.soap.DetailEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import junit.framework.AssertionFailedError;

public class TestEnvelope extends junit.framework.TestCase {

    public TestEnvelope(String name) {
        super(name);
    }


    String xmlString =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "                   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "                   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            " <soapenv:Header>\n" +
            "  <shw:Hello xmlns:shw=\"http://www.jcommerce.net/soap/ns/SOAPHelloWorld\">\n" +
            "    <shw:Myname>Tony</shw:Myname>\n" +
            "  </shw:Hello>\n" +
            " </soapenv:Header>\n" +
            " <soapenv:Body>\n" +
            "  <shw:Address xmlns:shw=\"http://www.jcommerce.net/soap/ns/SOAPHelloWorld\">\n" +
            "    <shw:City>GENT</shw:City>\n" +
            "  </shw:Address>\n" +
            " </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    // Test JAXM methods...
    public void testEnvelope() throws Exception {
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage smsg =
                mf.createMessage(new MimeHeaders(), new ByteArrayInputStream(xmlString.getBytes()));
        SOAPPart sp = smsg.getSOAPPart();
        SOAPEnvelope se = (SOAPEnvelope)sp.getEnvelope();
        //smsg.writeTo(System.out);
        assertTrue(se != null);
    }

    // Test JAXM methods...
    public void testEnvelope2() throws Exception {
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage smsg =
                mf.createMessage(new MimeHeaders(), new ByteArrayInputStream(xmlString.getBytes()));
        SOAPPart sp = smsg.getSOAPPart();
        SOAPEnvelope se = (SOAPEnvelope)sp.getEnvelope();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        smsg.writeTo(baos);
        SOAPBody body = smsg.getSOAPPart().getEnvelope().getBody();
        assertTrue(body != null);
    }
    
    public void testEnvelopeWithLeadingComment() throws Exception {
    	String soapMessageWithLeadingComment =
    		"<?xml version='1.0' encoding='UTF-8'?>" + 
			"<!-- Comment -->" +
			"<env:Envelope xmlns:env='http://schemas.xmlsoap.org/soap/envelope/'>" +
			"<env:Body><echo><arg0>Hello</arg0></echo></env:Body>" +
			"</env:Envelope>";
    	
    	SOAPConnectionFactory scFactory = SOAPConnectionFactory.newInstance();
    	SOAPConnection con = scFactory.createConnection();
    	
    	MessageFactory factory = MessageFactory.newInstance();
    	SOAPMessage message =
    		factory.createMessage(new MimeHeaders(), 
    				new ByteArrayInputStream(soapMessageWithLeadingComment.getBytes()));
        SOAPPart part = message.getSOAPPart();
        SOAPEnvelope envelope = (SOAPEnvelope) part.getEnvelope();
        //message.writeTo(System.out);
        assertTrue(envelope != null);
    }
    
    private SOAPEnvelope getSOAPEnvelope() throws Exception {
        SOAPConnectionFactory scFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection con = scFactory.createConnection();

        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage message = factory.createMessage();
        SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
        return envelope;
    }

    public void testAttributes() throws Exception {
        SOAPEnvelope envelope = getSOAPEnvelope();
        SOAPBody body = envelope.getBody();

        Name name1 = envelope.createName("MyAttr1");
        String value1 = "MyValue1";
        Name name2 = envelope.createName("MyAttr2");
        String value2 = "MyValue2";
        Name name3 = envelope.createName("MyAttr3");
        String value3 = "MyValue3";
        body.addAttribute(name1, value1);
        body.addAttribute(name2, value2);
        body.addAttribute(name3, value3);
        java.util.Iterator iterator = body.getAllAttributes();
        assertTrue(getIteratorCount(iterator) == 3);
        iterator = body.getAllAttributes();
        boolean foundName1 = false;
        boolean foundName2 = false;
        boolean foundName3 = false;
        while (iterator.hasNext()) {
            Name name = (Name) iterator.next();
            if (name.equals(name1))
                foundName1 = true;
            else if (name.equals(name2))
                foundName2 = true;
            else if (name.equals(name3))
                foundName3 = true;
        }
        assertTrue(foundName1 && foundName2 && foundName3);
    }

    public void testFaults() throws Exception {
        SOAPEnvelope envelope = getSOAPEnvelope();
        SOAPBody body = envelope.getBody();
        SOAPFault sf = body.addFault();
        sf.setFaultCode("myFault");
        String fc = sf.getFaultCode();
        assertTrue(fc.equals("myFault"));
    }

    public void testFaults2() throws Exception {
        SOAPEnvelope envelope = getSOAPEnvelope();
        SOAPBody body = envelope.getBody();
        SOAPFault sf = body.addFault();

        assertTrue(body.getFault() != null);

        Detail d1 = sf.addDetail();
        Name name = envelope.createName("GetLastTradePrice", "WOMBAT",
            "http://www.wombat.org/trader");
        d1.addDetailEntry(name);
        
        Detail d2 = sf.getDetail();
        assertTrue(d2 != null);
        Iterator i = d2.getDetailEntries();
        assertTrue(getIteratorCount(i) == 1);
        i = d2.getDetailEntries();
        while(i.hasNext()) {
            DetailEntry de = (DetailEntry)i.next();
            assertEquals(de.getElementName(),name);
        }
    }
    
    public void testHeaderElements() throws Exception {
        SOAPEnvelope envelope = getSOAPEnvelope();
        SOAPBody body = envelope.getBody();
        SOAPHeader hdr = envelope.getHeader();

        SOAPHeaderElement she1 = hdr.addHeaderElement(envelope.createName("foo1", "f1", "foo1-URI"));
        she1.setActor("actor-URI");
        java.util.Iterator iterator = hdr.extractHeaderElements("actor-URI");
        int cnt = 0;
        while (iterator.hasNext()) {
            cnt++;
            SOAPHeaderElement she = (SOAPHeaderElement) iterator.next();
            assertTrue(she.equals(she1));
        }
        assertTrue(cnt == 1);
        iterator = hdr.extractHeaderElements("actor-URI");
        assertTrue(!iterator.hasNext());
    }

    public void testText1() throws Exception {
        SOAPEnvelope envelope = getSOAPEnvelope();
        Iterator iStart = envelope.getChildElements();
        int countStart = getIteratorCount(iStart);
        SOAPElement se = envelope.addTextNode("<txt>This is text</txt>");
        assertTrue(se != null);
        assertTrue(envelope.getValue().equals("<txt>This is text</txt>"));
        Iterator i = envelope.getChildElements();
        int count = getIteratorCount(i);
        assertTrue(count == countStart + 1);
    }

    public void testText2() throws Exception {
        SOAPEnvelope envelope = getSOAPEnvelope();
	    SOAPElement se = envelope.addTextNode("This is text");
	    Iterator iterator = se.getChildElements();
	    Node n = null;
	    while (iterator.hasNext()) {
            n = (Node)iterator.next();
            if (n instanceof Text)
                break;
	    }
	    assertTrue(n instanceof Text);
		Text t = (Text)n;
		assertTrue(!t.isComment());
    }

    public void testText3() throws Exception {
        SOAPEnvelope envelope = getSOAPEnvelope();
	    SOAPElement se = envelope.addTextNode("<!-- This is a comment -->");
	    Iterator iterator = se.getChildElements();
	    Node n = null;
	    while (iterator.hasNext()) {
            n = (Node)iterator.next();
            if (n instanceof Text)
                break;
	    }
	    assertTrue(n instanceof Text);
		Text t = (Text)n;
		assertTrue(t.isComment());
    }

    public void testText4() throws SOAPException, IOException {
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage smsg =
            mf.createMessage(new MimeHeaders(), new ByteArrayInputStream(xmlString.getBytes()));

        // Make some change to the message
        SOAPPart sp = smsg.getSOAPPart();
        SOAPEnvelope envelope = sp.getEnvelope();
        envelope.addTextNode("<!-- This is a comment -->");
        
        boolean passbody = false;
        for (Iterator i = envelope.getChildElements(); i.hasNext(); ) {
            Node n = (Node) i.next();
            if (n instanceof SOAPElement) {
                SOAPElement se = (SOAPElement) n;
                System.out.println("soap element = " + se.getNodeName());
                if (se.getNamespaceURI().equals(SOAPConstants.URI_NS_SOAP_ENVELOPE) 
                    && se.getLocalName().equals("Body")) {
                    passbody = true;
                }
            }
            
            if (n instanceof Text) {
                Text t = (Text)n; 
                System.out.println("text = " + t.getValue());
                if (t.getValue().equals("<!-- This is a comment -->")) {
                    assertEquals(true, passbody);
                    return;
                }
            }            
        }
        throw new AssertionFailedError("Text is not added to expected position.");
    }

    private int getIteratorCount(java.util.Iterator i) {
        int count = 0;
        while (i.hasNext()) {
            count++;
            i.next();
        }
        return count;
    }

    public static void main(String[] args) throws Exception {
        test.saaj.TestEnvelope tester = new test.saaj.TestEnvelope("TestEnvelope");
        tester.testFaults2();
        tester.testEnvelope();
        tester.testText3();
        tester.testText2();
        tester.testText1();
        tester.testHeaderElements();
        tester.testFaults();
        tester.testAttributes();
    }
}

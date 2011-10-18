package test.encoding;

import junit.framework.TestCase;
import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.encoding.TypeMappingRegistry;
import org.apache.axis.message.RPCElement;
import org.apache.axis.message.RPCParam;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.server.AxisServer;
import org.apache.axis.utils.JavaUtils;
import samples.echo.SOAPStruct;
import samples.echo.SOAPStructStruct;

import javax.xml.namespace.QName;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
/**
 * Test deserialization of SOAP responses
 */
public class TestDeser extends TestCase {

    private String header;
    private String footer;
    private AxisServer server = new AxisServer();

    public TestDeser(String name) {
        this(name, Constants.URI_DEFAULT_SCHEMA_XSI,
                   Constants.URI_DEFAULT_SCHEMA_XSD);
    }

    public TestDeser(String name, String NS_XSI, String NS_XSD) {
        super(name);

        header =
            "<?xml version=\"1.0\"?>\n" +
            "<soap:Envelope " +
              "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
              "xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
              "xmlns:me=\"http://soapinterop.org/xsd\" " +
              "xmlns:this=\"http://encoding.test\" " + 
              "xmlns:xsi=\"" + NS_XSI + "\" " +
              "xmlns:xsd=\"" + NS_XSD + "\">\n" +
              "<soap:Body>\n" +
                "<methodResult xmlns=\"http://tempuri.org/\">\n";

        footer =
                "</methodResult>\n" +
              "</soap:Body>\n" +
            "</soap:Envelope>\n";

        TypeMappingRegistry tmr = server.getTypeMappingRegistry();
        TypeMapping tm = (TypeMapping) tmr.createTypeMapping();
        tm.setSupportedEncodings(new String[] {Constants.URI_DEFAULT_SOAP_ENC});
        tmr.register(Constants.URI_DEFAULT_SOAP_ENC, tm);
        tm.register(java.lang.String[].class, 
                    new QName("http://soapinterop.org/xsd", "ArrayOfString"),
                    new org.apache.axis.encoding.ser.ArraySerializerFactory(),
                    new org.apache.axis.encoding.ser.ArrayDeserializerFactory());
        tm.register(java.lang.Object[].class, 
                    new QName("http://soapinterop.org/xsd", "ArrayOfObject"),
                    new org.apache.axis.encoding.ser.ArraySerializerFactory(),
                    new org.apache.axis.encoding.ser.ArrayDeserializerFactory());
        tm.register(samples.echo.SOAPStruct.class, 
                    new QName("http://soapinterop.org/xsd", "SOAPStruct"),
                    new org.apache.axis.encoding.ser.BeanSerializerFactory(
                          samples.echo.SOAPStruct.class,
                          new QName("http://soapinterop.org/xsd", "SOAPStruct")),
                    new org.apache.axis.encoding.ser.BeanDeserializerFactory(
                          samples.echo.SOAPStruct.class,
                          new QName("http://soapinterop.org/xsd", "SOAPStruct")));
        tm.register(samples.echo.SOAPStruct[].class, 
                    new QName("http://soapinterop.org/xsd", "ArrayOfSOAPStruct"),
                    new org.apache.axis.encoding.ser.ArraySerializerFactory(),
                    new org.apache.axis.encoding.ser.ArrayDeserializerFactory());
        tm.register(samples.echo.SOAPStructStruct.class,
                    new QName("http://soapinterop.org/xsd", "SOAPStructStruct"),
                    new org.apache.axis.encoding.ser.BeanSerializerFactory(
                          samples.echo.SOAPStructStruct.class,
                          new QName("http://soapinterop.org/xsd", "SOAPStructStruct")),
                    new org.apache.axis.encoding.ser.BeanDeserializerFactory(
                          samples.echo.SOAPStructStruct.class,
                          new QName("http://soapinterop.org/xsd", "SOAPStructStruct")));
        tm.register(test.encoding.IndexPropBean.class, 
                    new QName("http://encoding.test", "IndexPropBean"),
                    new org.apache.axis.encoding.ser.BeanSerializerFactory(
                          test.encoding.IndexPropBean.class,
                          new QName("http://encoding.test", "IndexPropBean")),
                    new org.apache.axis.encoding.ser.BeanDeserializerFactory(
                          test.encoding.IndexPropBean.class,
                          new QName("http://encoding.test", "IndexPropBean")));
    }

    /**
     * Verify that two objects have the same value, handling arrays...
     */
    private static boolean equals(Object obj1, Object obj2) {
       if ( (obj1 == null) || (obj2 == null) ) return (obj1 == obj2);
       if (obj1.equals(obj2)) return true;
       if (obj2.getClass().isArray() && obj1.getClass().isArray()) {
           if (Array.getLength(obj1) != Array.getLength(obj2)) return false;
           for (int i=0; i<Array.getLength(obj1); i++)
               if (!equals(Array.get(obj1,i),Array.get(obj2,i))) return false;
           return true;
       }
       if ((obj1 instanceof List) && (obj2 instanceof List)) {
           List list1 = (List)obj1;
           List list2 = (List)obj2;
           if (list1.size() != list2.size()) return false;
           for (int i=0; i < list1.size(); i++) {
               if (!equals(list1.get(i), list2.get(i))) return false;
           }
           return true;
       }
       if ((obj1 instanceof Map) && (obj2 instanceof Map)) {
           Map map1 = (Map)obj1;
           Map map2 = (Map)obj2;
           Set keys1 = map1.keySet();
           Set keys2 = map2.keySet();
           if (!(keys1.equals(keys2))) return false;
           Iterator i = keys1.iterator();
           while (i.hasNext()) {
               Object key = i.next();
               if (!map1.get(key).equals(map2.get(key)))
                   return false;
           }
           return true;
       }
       
       return false;
    }

    /**
     * Verify that a given XML deserialized produces the expected result
     */
    protected void deserialize(String data, Object expected)
        throws Exception {
        deserialize(data, expected, false);
    }

    protected void deserialize(String data, Object expected, boolean tryConvert)
            throws Exception {
        deserialize(data, expected,tryConvert,"");
    }


    /**
     * Verify that a given XML deserialized produces the expected result
     * @param data
     * @param expected
     * @param tryConvert
     * @param comment extra text to add
     * @throws Exception
     */
    protected void deserialize(String data, Object expected, boolean tryConvert, String comment)
       throws Exception
    {
       Message message = new Message(header + data + footer);
       message.setMessageContext(new MessageContext(server));
       String postfix="";
       if(comment!=null) {
           postfix=" "+comment;
       }

       SOAPEnvelope envelope = (SOAPEnvelope)message.getSOAPEnvelope();
       assertNotNull("SOAP envelope should not be null"+ postfix, envelope);

       RPCElement body = (RPCElement)envelope.getFirstBody();
       assertNotNull("SOAP body should not be null" + postfix, body);

       Vector arglist = body.getParams();
       assertNotNull("arglist", arglist);
       assertTrue("param.size()<=0 {Should be > 0}" + postfix, arglist.size()>0);

       RPCParam param = (RPCParam) arglist.get(0);
       assertNotNull("SOAP param should not be null" + postfix, param);

       Object result = param.getObjectValue();
       if (!equals(result, expected)) {
           String errorText = "Failed to decode " + data + postfix+" : ";
           // Try to convert to the expected class
           if (tryConvert) {
               Object result2 = JavaUtils.convert(result, expected.getClass());
               if (!equals(result2, expected)) {

                   assertEquals(errorText, expected, result);
               }
           } else {
               assertEquals(errorText, expected, result);
           }
       }
    }

    public void testString() throws Exception {
        deserialize("<result xsi:type=\"xsd:string\">abc</result>",
                    "abc");
    }

    public void testBoolean() throws Exception {
        deserialize("<result xsi:type=\"xsd:boolean\">false</result>",
                    new Boolean(false));
        deserialize("<result xsi:type=\"xsd:boolean\">true</result>",
                    new Boolean(true));
        deserialize("<result xsi:type=\"xsd:boolean\">0</result>",
                    new Boolean(false));
        deserialize("<result xsi:type=\"xsd:boolean\">1</result>",
                    new Boolean(true));
    }

    public void testDouble() throws Exception {
        deserialize("<result xsi:type=\"xsd:double\">3.14</result>",
                    new Double(3.14));
    }

    public void testDoubleNaN() throws Exception {
        deserialize("<result xsi:type=\"xsd:double\">NaN</result>",
                    new Double(Double.NaN));
    }

    public void testDoubleINF() throws Exception {
        deserialize("<result xsi:type=\"xsd:double\">INF</result>",
                    new Double(Double.POSITIVE_INFINITY));
    }

    public void testDoubleNINF() throws Exception {
        deserialize("<result xsi:type=\"xsd:double\">-INF</result>",
                    new Double(Double.NEGATIVE_INFINITY));
    }

    public void testFloat() throws Exception {
        deserialize("<result xsi:type=\"xsd:float\">3.14</result>",
                    new Float(3.14F));
    }

    public void testFloatNaN() throws Exception {
        deserialize("<result xsi:type=\"xsd:float\">NaN</result>",
                    new Float(Float.NaN));
    }

    public void testFloatINF() throws Exception {
        deserialize("<result xsi:type=\"xsd:float\">INF</result>",
                    new Float(Float.POSITIVE_INFINITY));
    }

    public void testFloatNINF() throws Exception {
        deserialize("<result xsi:type=\"xsd:float\">-INF</result>",
                    new Float(Float.NEGATIVE_INFINITY));
    }

    public void testInt() throws Exception {
        deserialize("<result xsi:type=\"xsd:int\">10</result>",
                    new Integer(10));
    }

    public void testLong() throws Exception {
        deserialize("<result xsi:type=\"xsd:long\">17</result>",
                    new Long(17));
    }

    public void testShort() throws Exception {
        deserialize("<result xsi:type=\"xsd:short\">3</result>",
                    new Short((short)3));
    }

    public void testQName() throws Exception {
        String ns = "http://test";
        String local = "bar";

        deserialize("<result xmlns:foo=\"" + ns + "\" xsi:type=\"xsd:QName\">foo:" + local + "</result>",
                    new QName(ns, local));

        deserialize("<result xmlns:foo=\"" + ns + "\" xsi:type=\"xsd:QName\">\n     foo:" + local + "   \r\n        </result>",
                    new QName(ns, local));
    }

    public void testArray() throws Exception {
        Vector v = new Vector();
        v.addElement("abc");
        v.addElement("def");
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                            "soapenc:arrayType=\"xsd:string[2]\"> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:type=\"xsd:string\">def</item>" +
                    "</result>",
                    v, true);
    }
    
    public void testSparseArray1() throws Exception {
        ArrayList list = new ArrayList(4);
        list.add(null);
        list.add(null);
        list.add("abc");
        list.add("def");
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                            "soapenc:arrayType=\"xsd:string[4]\" " +
                            "soapenc:offset=\"[2]\"> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:type=\"xsd:string\">def</item>" +
                    "</result>",
                    list, true);
    }
    
    public void testSparseArray2() throws Exception {
        ArrayList list = new ArrayList(4);
        list.add("abc");
        list.add(null);
        list.add("def");
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                            "soapenc:arrayType=\"xsd:string[4]\"> " +
      "<item soapenc:position=\"[0]\" xsi:type=\"xsd:string\">abc</item>" +
      "<item soapenc:position=\"[2]\" xsi:type=\"xsd:string\">def</item>" +
                    "</result>",
                    list, true);
    }

    public void testHugeSparseArray() throws Exception {
        ArrayList list = new ArrayList(4);
        list.add("abc");
        list.add(null);
        list.add("def");
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                            "soapenc:arrayType=\"xsd:string[50000000]\"> " +
      "<item soapenc:position=\"[0]\" xsi:type=\"xsd:string\">abc</item>" +
      "<item soapenc:position=\"[2]\" xsi:type=\"xsd:string\">def</item>" +
                    "</result>",
                    list, true);
    }
    public void testMap() throws Exception {
        HashMap m = new HashMap();
        m.put("abcKey", "abcVal");
        m.put("defKey", "defVal");
        deserialize("<result xsi:type=\"xmlsoap:Map\" " +
                    "xmlns:xmlsoap=\"http://xml.apache.org/xml-soap\"> " +
                      "<item>" +
                       "<key xsi:type=\"xsd:string\">abcKey</key>" +
                       "<value xsi:type=\"xsd:string\">abcVal</value>" +
                      "</item><item>" +
                       "<key xsi:type=\"xsd:string\">defKey</key>" +
                       "<value xsi:type=\"xsd:string\">defVal</value>" +
                      "</item>" +
                    "</result>",
                    m);
    }

    public void testHashtable() throws Exception {
        Hashtable ht = new Hashtable();
        ht.put("abcKey", "abcVal");
        ht.put("defKey", "defVal");
        deserialize("<result xsi:type=\"xmlsoap:Map\" " +
                    "xmlns:xmlsoap=\"http://xml.apache.org/xml-soap\"> " +
                      "<item>" +
                       "<key xsi:type=\"xsd:string\">abcKey</key>" +
                       "<value xsi:type=\"xsd:string\">abcVal</value>" +
                      "</item><item>" +
                       "<key xsi:type=\"xsd:string\">defKey</key>" +
                       "<value xsi:type=\"xsd:string\">defVal</value>" +
                      "</item>" +
                    "</result>",
                    ht, true);
    }

    public void testUntyped() throws Exception {
         deserialize("<result>10</result>", "10");
    }

    public void testSOAPString() throws Exception {
        deserialize("<result xsi:type=\"soapenc:string\">abc</result>",
                    "abc");
    }

    // Complicated array tests

    // type=soapenc:Array
    public void testArrayA() throws Exception {
        String[] s = new String[] {"abc", "def"};
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                            "soapenc:arrayType=\"xsd:string[2]\"> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:type=\"xsd:string\">def</item>" +
                    "</result>",
                    s, true);
    }

    // Like above but missing [2] dimension
    public void testArrayB() throws Exception {
        String[] s = new String[] {"abc", "def"};
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                            "soapenc:arrayType=\"xsd:string[]\"> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:type=\"xsd:string\">def</item>" +
                    "</result>",
                    s, true);
    }


    // Like above but no xsi:type on elements
    public void testArrayC() throws Exception {
        String[] s = new String[] {"abc", "def"};
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                            "soapenc:arrayType=\"xsd:string[]\"> " +
                       "<item>abc</item>" +
                       "<item>def</item>" +
                    "</result>",
                    s, true);
    }

    // Now try with arrayType=xsd:anyType
    public void testArrayD() throws Exception {
        String[] s = new String[] {"abc", "def"};
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                       "soapenc:arrayType=\"xsd:anyType[]\"> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:type=\"xsd:string\">def</item>" +
                    "</result>",
                    s, true);
    }

    // Now try without arrayType
    public void testArrayE() throws Exception {
        String[] s = new String[] {"abc", "def"};
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                       "> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:type=\"xsd:string\">def</item>" +
                    "</result>",
                    s, true);
    }

    // Use a specific array type, not soapenc:Array
    public void testArrayF() throws Exception {
        String[] s = new String[] {"abc", "def"};
        deserialize("<result xsi:type=\"me:ArrayOfString\" " +
                       "> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:type=\"xsd:string\">def</item>" +
                    "</result>",
                    s, true);
    }

    // Same as above without individual item types
    public void testArrayG() throws Exception {
        String[] s = new String[] {"abc", "def"};
        deserialize("<result xsi:type=\"me:ArrayOfString\" " +
                       "> " +
                       "<item>abc</item>" +
                       "<item>def</item>" +
                    "</result>",
                    s, true);
    }

    // Same as above except result is an Object[]
    public void testArrayH() throws Exception {
        Object[] s = new Object[] {new String("abc"), new String("def")};
        deserialize("<result xsi:type=\"me:ArrayOfString\" " +
                       "> " +
                       "<item>abc</item>" +
                       "<item>def</item>" +
                    "</result>",
                    s, true);
    }

    // Ooh La La
    // Array of Object containing a String and an Integer
    public void testArrayI() throws Exception {
        Object[] s = new Object[] {new String("abc"), new Integer(123)};
        deserialize("<result xsi:type=\"me:ArrayOfObject\" " +
                       "> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:type=\"soapenc:int\">123</item>" +
                    "</result>",
                    s, true);
    }

    // Same as above using arrayType=xsd:anyType
    public void testArrayJ() throws Exception {
        Object[] s = new Object[] {new String("abc"), new Integer(123)};
        deserialize("<result xsi:type=\"me:ArrayOfObject\" " +
                       "soapenc:arrayType=\"xsd:anyType[]\"> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:type=\"soapenc:int\">123</item>" +
                    "</result>",
                    s, true);
    }

    // Same as above using type=soapenc:Array and no arrayType...bare bones
    public void testArrayK() throws Exception {
        Object[] s = new Object[] {new String("abc"), new Integer(123)};
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                       "> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:type=\"soapenc:int\">123</item>" +
                    "</result>",
                    s, true);
    }

    // This was created from a return received from a .NET service
    public void testArrayL() throws Exception {
        SOAPStruct[] s = new SOAPStruct[] {
                new SOAPStruct(),
                new SOAPStruct(),
                new SOAPStruct()};
            s[0].setVarInt(1);
            s[0].setVarString("one");
            s[0].setVarFloat(1.1F);
            s[1].setVarInt(2);
            s[1].setVarString("two");
            s[1].setVarFloat(2.2F);
            s[2].setVarInt(3);
            s[2].setVarString("three");
            s[2].setVarFloat(3.3F);

        deserialize("<soapenc:Array id=\"ref-7\" soapenc:arrayType=\"me:SOAPStruct[3]\">" +
                    "<item href=\"#ref-8\"/>" + 
                    "<item href=\"#ref-9\"/>" +
                    "<item href=\"#ref-10\"/>" + 
                    "</soapenc:Array>" +

                    "<me:SOAPStruct id=\"ref-8\">" +
                    "<varString xsi:type=\"xsd:string\">one</varString>" +
                    "<varInt xsi:type=\"xsd:int\">1</varInt>" +
                    "<varFloat xsi:type=\"xsd:float\">1.1</varFloat>" +
                    "</me:SOAPStruct>" +

                    "<me:SOAPStruct id=\"ref-9\">" +
                    "<varString xsi:type=\"xsd:string\">two</varString>" +
                    "<varInt xsi:type=\"xsd:int\">2</varInt>" +
                    "<varFloat xsi:type=\"xsd:float\">2.2</varFloat>" +
                    "</me:SOAPStruct>" +

                    "<me:SOAPStruct id=\"ref-10\">" +
                    "<varString xsi:type=\"xsd:string\">three</varString>" +
                    "<varInt xsi:type=\"xsd:int\">3</varInt>" +
                    "<varFloat xsi:type=\"xsd:float\">3.3</varFloat>" +
                    "</me:SOAPStruct>",
                    s, true);
    }

    // Like above without multiref
    public void testArrayM() throws Exception {
        SOAPStruct[] s = new SOAPStruct[] {
                new SOAPStruct(),
                new SOAPStruct(),
                new SOAPStruct()};
            s[0].setVarInt(1);
            s[0].setVarString("one");
            s[0].setVarFloat(1.1F);
            s[1].setVarInt(2);
            s[1].setVarString("two");
            s[1].setVarFloat(2.2F);
            s[2].setVarInt(3);
            s[2].setVarString("three");
            s[2].setVarFloat(3.3F);
        deserialize("<soapenc:Array id=\"ref-7\" soapenc:arrayType=\"me:SOAPStruct[3]\">" +
                    "<me:SOAPStruct>" +
                    "<varString xsi:type=\"xsd:string\">one</varString>" +
                    "<varInt xsi:type=\"xsd:int\">1</varInt>" +
                    "<varFloat xsi:type=\"xsd:float\">1.1</varFloat>" +
                    "</me:SOAPStruct>" +

                    "<me:SOAPStruct>" +
                    "<varString xsi:type=\"xsd:string\">two</varString>" +
                    "<varInt xsi:type=\"xsd:int\">2</varInt>" +
                    "<varFloat xsi:type=\"xsd:float\">2.2</varFloat>" +
                    "</me:SOAPStruct>" +

                    "<me:SOAPStruct>" +
                    "<varString xsi:type=\"xsd:string\">three</varString>" +
                    "<varInt xsi:type=\"xsd:int\">3</varInt>" +
                    "<varFloat xsi:type=\"xsd:float\">3.3</varFloat>" +
                    "</me:SOAPStruct>" +

                    "</soapenc:Array>",
                    s, true);
    }
    // Deserialize Bean with discreet names
    public void testBeanWithIndexedPropA() throws Exception {
        IndexPropBean s = new IndexPropBean();
        s.setName(new String[] {"hello", "goodbye"});
        deserialize("<result xsi:type=\"this:IndexPropBean\" " + "> " +
                    "<name href=\"#ref-1\"/>" +
                    "<name href=\"#ref-2\"/>" +
                    "</result>" +
                    "<item id=\"ref-1\" xsi:type=\"xsd:string\">hello</item>" +
                    "<item id=\"ref-2\" xsi:type=\"xsd:string\">goodbye</item>",
                    s, true);
    }
    // Deserialize Bean with names in an array
    public void testBeanWithIndexedPropB() throws Exception {
        IndexPropBean s = new IndexPropBean();
        s.setName(new String[] {"hello", "goodbye"});
        deserialize("<result xsi:type=\"this:IndexPropBean\" " + "> " +
                    "<name href=\"#ref-0\" /> " +
                    "</result>" +
                    "<soapenc:Array id=\"ref-0\" soapenc:arrayType=\"xsd:string[2]\"> " +
                    "<item xsi:type=\"xsd:string\">hello</item>" +
                    "<item xsi:type=\"xsd:string\">goodbye</item>" +
                    "</soapenc:Array>",
                    s, true);
    }
    // Struct within Struct
    public void testStructStruct() throws Exception {

        SOAPStruct s = new samples.echo.SOAPStruct();
        s.setVarInt(1);
        s.setVarString("one");
        s.setVarFloat(1.1F);
        SOAPStructStruct ss = new SOAPStructStruct();
        ss.setVarString("hello");
        ss.setVarInt(2);
        ss.setVarFloat(2.2F);
        ss.setVarStruct(s);

        deserialize("<whatever xsi:type=\"me:SOAPStructStruct\">" +
                    "<varString xsi:type=\"xsd:string\">hello</varString>" +
                    "<varInt xsi:type=\"xsd:int\">2</varInt>" +
                    "<varFloat xsi:type=\"xsd:float\">2.2</varFloat>" +
                    "<varStruct xsi:type=\"me:SOAPStruct\">" +
                    "<varString xsi:type=\"xsd:string\">one</varString>" +
                    "<varInt xsi:type=\"xsd:int\">1</varInt>" +
                    "<varFloat xsi:type=\"xsd:float\">1.1</varFloat>" +
                    "</varStruct>" +
                    "</whatever>" , ss, true);
    }
    
/*
    public void testBug18390() throws Exception {
        String[] s = new String[] {"GW671055X"};
        deserialize("<molNames soapenc:arrayType=\"xsd:string[1]\"> " +
                       "<xsd:string xsi:type=\"xsd:string\">GW671055X</xsd:string>" +
                    "</molNames>",
                    s, true);
    }
    
    public void testBug25536() throws Exception {
        int[] array = new int[]{3,4};
        deserialize("<myFavoriteNumbers soapenc:arrayType=\"xsd:int[2]\">\n" +
                "   <number>3</number> \n" +
                "   <number>4</number> \n" +
                "</myFavoriteNumbers>",array,true);
    }
*/

    // Struct within Struct
    public void testStructStruct2() throws Exception {
        SOAPStruct s = new samples.echo.SOAPStruct();
        s.setVarInt(1);
        s.setVarString("one");
        s.setVarFloat(1.1F);
        SOAPStructStruct ss = new SOAPStructStruct();
        ss.setVarString("hello");
        ss.setVarInt(2);
        ss.setVarFloat(2.2F);
        ss.setVarStruct(s);

        deserialize("<whatever xsi:type=\"me:SOAPStructStruct\">" +
                    "<varString>hello</varString>" +
                    "<varInt>2</varInt>" +
                    "<varFloat>2.2</varFloat>" +
                    "<varStruct>" +
                    "<varString>one</varString>" +
                    "<varInt>1</varInt>" +
                    "<varFloat>1.1</varFloat>" +
                    "</varStruct>" +
                    "</whatever>" , ss, true);
    }
}

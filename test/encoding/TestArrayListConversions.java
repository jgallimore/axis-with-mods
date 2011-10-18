package test.encoding;

import junit.framework.TestCase;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.providers.java.RPCProvider;
import org.apache.axis.server.AxisServer;
import org.apache.axis.transport.local.LocalTransport;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class TestArrayListConversions extends TestCase {
    private static final String SERVICE_NAME = "TestArrayConversions";

    private AxisServer server;
    private LocalTransport transport;

    public TestArrayListConversions() {
        super("service");
    }

    public TestArrayListConversions(String name) {
        super(name);
    }

    private static boolean equals(List list, Object obj) {
        if ((list == null) || (obj == null))
            return false;

        if (!obj.getClass().isArray()) return false;

        Object[] array = (Object[]) obj;
        Iterator iter = list.iterator();

        for (int i = 0; i < array.length; i++) {
            if (!(array[i].equals(iter.next()))) {
                return false;
            }
        }

        return true;
    }

    protected void setUp() throws Exception {
        try {
            SimpleProvider provider = new SimpleProvider();
            server = new AxisServer(provider);
            transport = new LocalTransport(server);

            SOAPService service = new SOAPService(new RPCProvider());
            service.setEngine(server);

            service.setOption("className", "test.encoding.TestArrayListConversions");
            service.setOption("allowedMethods", "*");

            ServiceDesc desc = service.getInitializedServiceDesc(null);
            desc.setDefaultNamespace(SERVICE_NAME);

            provider.deployService(SERVICE_NAME, service);
        } catch (Exception exp) {
            exp.printStackTrace();
        }

    }

    public void testVectorConversion() throws Exception {
        Call call = new Call(new Service());
        call.setTransport(transport);

        Vector v = new Vector();
        v.addElement("Hi there!");
        v.addElement("This'll be a SOAP Array and then a LinkedList!");
        call.setOperationName(new QName(SERVICE_NAME, "echoLinkedList"));
        Object ret = call.invoke(new Object[]{v});
        if (!equals(v, ret)) assertEquals("Echo LinkedList mangled the result.  Result is underneath.\n" + ret, v, ret);
    }

    public void testLinkedListConversion() throws Exception {
        Call call = new Call(new Service());
        call.setTransport(transport);

        LinkedList l = new LinkedList();
        l.add("Linked list item #1");
        l.add("Second linked list item");
        l.add("This will be a SOAP Array then a Vector!");

        call.setOperationName(new QName(SERVICE_NAME, "echoVector"));
        Object ret = call.invoke(new Object[]{l});
        if (!equals(l, ret)) assertEquals("Echo Vector mangled the result.  Result is underneath.\n" + ret, l, ret);
    }

    public void testArrayConversion() throws Exception {
        Call call = new Call(new Service());
        call.setTransport(transport);

        Vector v = new Vector();
        v.addElement("Hi there!");
        v.addElement("This'll be a SOAP Array");

        call.setOperationName(new QName(SERVICE_NAME, "echoArray"));
        Object ret = call.invoke(new Object[]{v});
        if (!equals(v, ret)) assertEquals("Echo Array mangled the result.  Result is underneath\n" + ret, v, ret);
    }

    /**
     * Test the setReturnClass() API on Call by asking the runtime to
     * give us back a Vector instead of an array.  Confirm we get a Vector
     * back, and that it matches the data we send.
     */
    public void testReturnAsVector() throws Exception {
        Call call = new Call(new Service());
        call.setTransport(transport);

        LinkedList l = new LinkedList();
        l.add("Linked list item #1");
        l.add("Second linked list item");
        l.add("This will be a SOAP Array then a Vector!");

        call.setOperationName(new QName(SERVICE_NAME, "echoArray"));
        call.addParameter("arg0", null, LinkedList.class, ParameterMode.IN);
        call.setReturnClass(Vector.class);
        Object ret = call.invoke(new Object[]{l});
        assertEquals("Return wasn't a Vector!", Vector.class, ret.getClass());
        Vector v = (Vector)ret;
        assertEquals("Sizes were different", l.size(), v.size());
        for (int i = 0; i < l.size(); i++) {
            String s = (String)l.get(i);
            assertEquals("Value " + i + " didn't match", s, v.get(i));
        }
    }

    public static void main(String[] args) {
        TestArrayListConversions tester = new TestArrayListConversions("TestArrayListConversions");
        try {
            tester.setUp();
            tester.testArrayConversion();
            tester.testLinkedListConversion();
            tester.testVectorConversion();
            tester.testReturnAsVector();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /****************************************************************
     *
     * Service methods - this class is also deployed as an Axis RPC
     * service for convenience.  These guys just echo various things.
     *
     */
    public LinkedList echoLinkedList(LinkedList l) {
        return l;
    }

    public Vector echoVector(Vector v) {
        return v;
    }

    public Object[] echoArray(Object[] array) {
        return array;
    }
}

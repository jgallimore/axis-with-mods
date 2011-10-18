package test.RPCDispatch;

import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.AxisFault;
import org.apache.axis.message.RPCElement;
import org.apache.axis.utils.DOM2Writer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Test WebService
 */
public class Service {

    /**
     * Reverse the order of characters in a string
     */
    public String reverseString(String input) throws Exception {
       String result = "";
       for (int i=input.length(); i>0; ) result+=input.charAt(--i);
       return result;
    }

    /**
     * Concatenate two strings - used to test out-of-order parameter
     * matching.
     */
    public String concatenate(String a1, String a2)
    {
        return a1 + a2;
    }

    /**
     * Reverse the order of a struct
     */
    public Data reverseData(Data input) throws Exception {
       Data result = new Data();
       result.setField1(input.getField3());
       result.setField2(reverseString(input.getField2()));
       result.setField3(input.getField1());
       return result;
    }

    /**
     * Return the target service (should be this!)
     */
    public String targetServiceImplicit() throws Exception {
       return MessageContext.getCurrentContext().getTargetService();
    }

    /**
     * Return the target service (should be this!)
     */
    public String argAsDOM(Data input) throws Exception {

       // get the first parameter
       Message message = MessageContext.getCurrentContext().getRequestMessage();
       RPCElement body = (RPCElement)message.getSOAPEnvelope().getFirstBody();
       NodeList parms = body.getAsDOM().getChildNodes();
       Node parm1 = null;
       for (int i=0; i<parms.getLength(); i++) {
           parm1 = parms.item(i);
           if (parm1.getNodeType() == Node.ELEMENT_NODE) break;
       }

       // convert it to a DOM and back to a string, and return the result.
       return DOM2Writer.nodeToString(parm1, true);

    }

    /**
     * Return the value passed (including nulls!)
     */
    public Integer echoInt(Integer value) throws Exception {
       return value;
    }

    /**
     * Return the boolean and String arguments concatenated
     */
    public String overloaded(boolean b, String s)
    {
        return b + s;
    }

    /**
     * Return the String and boolean arguments concatenated
     */
    public String overloaded(String s, boolean b)
    {
        return s + b;
    }

    /**
     * Return the boolean value passed in
     */
    public boolean overloaded(boolean b)
    {
        return b;
    }

    /**
     * Return the Boolean value passed in
     */
    public Boolean testBoolean(Boolean b)
    {
        return b;
    }

    /**
     * Return the Float value passed in
     */
    public Float testFloat(Float b)
    {
        return b;
    }

    /**
     * Return the Double value passed in
     */
    public Double testDouble(Double b)
    {
        return b;
    }

    /**
     * Return the int passed in (this and the function above test overloaded
     * method dispatch)
     */
    public int overloaded(int i)
    {
        return i;
    }

    /**
     * Return the int and String arguments concatenated
     */
    public String overloaded(int i, String s)
    {
        return i + s;
    }
    
    /**
     * Echo a string array (this is for testing that String->String[]
     * conversions do NOT happen when using encoded arrays)
     */ 
    public void arrayMethod(String [] arg) throws AxisFault {
        throw new AxisFault("You shouldn't have called me!");
    }

    /**
     * Simple exception to be used in generating faults
     */
    class TestFault extends Exception {
        TestFault(String msg) throws Exception {
            super(msg);
            if (msg == null) throw new Exception("default value");
        }
    }

    /**
     * Simple fault.
     */
    public String simpleFault(String value) throws Exception {
       TestFault fault = new TestFault(value);
       throw fault;
    }


}

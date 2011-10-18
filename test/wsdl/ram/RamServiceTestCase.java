/**
 * RamServiceTestCase.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package test.wsdl.ram;

public class RamServiceTestCase extends junit.framework.TestCase {
    public RamServiceTestCase(String name) {
        super(name);
    }

    public void testRamWSDL() throws Exception {
        javax.xml.rpc.ServiceFactory serviceFactory = javax.xml.rpc.ServiceFactory.newInstance();
        java.net.URL url = new java.net.URL(new test.wsdl.ram.RamServiceLocator().getRamAddress() + "?WSDL");
        javax.xml.rpc.Service service = serviceFactory.createService(url, new test.wsdl.ram.RamServiceLocator().getServiceName());
        assertTrue(service != null);
    }

    public void test1RamValidate() {
        Ram binding;
        try {
            binding = new RamServiceLocator().getRam();
        }
        catch (javax.xml.rpc.ServiceException jre) {
            throw new junit.framework.AssertionFailedError("JAX-RPC ServiceException caught: " + jre);
        }
        assertTrue("binding is null", binding != null);

        try {
            RamData[] input = new RamData[1];

            input[0] = new RamData();
            input[0].setBankInfoPaymentType("BIPT");
            input[0].setBankInfoRequestType("BIRT");
            input[0].setCallingClientPortNumber("CCPN");
            input[0].setCallingClientUserIdentifier("CCUI");

            CreditCard cc = new CreditCard();
            cc.setName("NAME");
            cc.setNumber("NUMBER");
            cc.setExpires(new java.util.Date());
            cc.setPostalCode("PC");
            input[0].setCreditCard(cc);

            Fee[] fees = new Fee[1];
            fees[0] = new Fee();
            fees[0].setAmount(20.0);
            fees[0].setQuantity(50);
            fees[0].setCode("CODE");
            input[0].setFees(fees);

            input[0].setMailRoomDate(new java.util.Date());
            input[0].setSaleOtherPaymentTotalAmount(100.0);
            input[0].setSalePostingReferenceText("SPRT");

            Response[] output = binding.validate(input);
            java.util.Date resDate = output[0].getAccountingDate();
            java.util.Date now = new java.util.Date();
            assertTrue("Time check failed.  Result date = " + resDate + ", current time = " + now, resDate.before(now));
        }
        catch (java.rmi.RemoteException re) {
            throw new junit.framework.AssertionFailedError("Remote Exception caught: " + re);
        }
    }

    public static void main(String[] args){
        RamServiceTestCase test = new RamServiceTestCase("RamServiceTestCase");
        test.test1RamValidate();
    }
}

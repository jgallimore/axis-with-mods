/**
 * DataTypesSoapImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis Wsdl2java emitter.
 */

package test.wsdl.datatypes;

public class DataTypes_BindingImpl implements DataTypes_PortType {
    public java.lang.String sayHello() throws java.rmi.RemoteException {
        return "Hello World!";
    }

    public java.lang.String sayHelloName(java.lang.String name) throws java.rmi.RemoteException {
        return "Hello " + name;
    }

    public ArrayOfInt getIntArray() throws java.rmi.RemoteException {
        int[] a = new int[5];
        for (int i=0; i<5; i++)
            a[i] = i*10;
        ArrayOfInt array = new ArrayOfInt();
        array.set_int(a);
        return array;
    }

    public Mode getMode() throws java.rmi.RemoteException {
        return new Mode(Mode._Off);
    }

    public Order getOrder() throws java.rmi.RemoteException {
        Order myOrder = new Order();

         myOrder.setPrice(34.5);
         myOrder.setOrderID(323232);

         return myOrder;
    }

    public ArrayOfOrder getOrders() throws java.rmi.RemoteException {
        Order [] myOrders = new Order[2];

        myOrders[0] = new Order();
        myOrders[0].setPrice(34.5);
        myOrders[0].setOrderID(323232);
        myOrders[1] = new Order();
        myOrders[1].setPrice(99.4);
        myOrders[1].setOrderID(645645);
        ArrayOfOrder array = new ArrayOfOrder();
        array.setOrder(myOrders);
        return array;
    }

    public java.lang.String[] getSimpleList(java.lang.String[] inputList) throws java.rmi.RemoteException {
        java.lang.String[] outputList;
        if (inputList == null) {
            outputList = null;
        }
        else {
            outputList = new java.lang.String[inputList.length];
            for (int i = 0; i < inputList.length; i++) {
                outputList[i] = inputList[i] + "_response";
            }
        }

        return outputList;
    }

    public test.wsdl.datatypes.UsageType getUsageType() throws java.rmi.RemoteException { 
	return new test.wsdl.datatypes.UsageType(test.wsdl.datatypes.UsageType._value1);
    }
}

/**
 * MArrayTestsServiceTestCase.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis Wsdl2java emitter.
 */

package test.wsdl.marrays;

import java.util.HashMap;
import java.util.Map;

public class MArrayTestsServiceTestCase extends junit.framework.TestCase {
    test.wsdl.marrays.MArrayTests binding;

    public MArrayTestsServiceTestCase(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        binding = new MArrayTestsServiceLocator().getMArrayTests();
        assertTrue("binding is null", binding != null);
    }

    public void testMArrayTestsWSDL() throws Exception {
        javax.xml.rpc.ServiceFactory serviceFactory = javax.xml.rpc.ServiceFactory.newInstance();
        java.net.URL url = new java.net.URL(new test.wsdl.marrays.MArrayTestsServiceLocator().getMArrayTestsAddress() + "?WSDL");
        javax.xml.rpc.Service service = serviceFactory.createService(url, new test.wsdl.marrays.MArrayTestsServiceLocator().getServiceName());
        assertTrue(service != null);
    }

    public void testMArrayTest1() throws Exception {
        // In each case below, the elements of the array are either nill, -1 or
        // i + 10j + 100k
        // The remote service adds 1000 to each element that is i + 10j + 100k
        // Test 1: 3-Dim array of values.  This could be serialized 
        // as a multi-dimensional array.
        int[][][] in = new int[3][3][3];
        int[][][] rc;
        fill(in);
        rc = binding.testIntArray(in);
        assertTrue("Test 1 Failed", validate(in, rc));
    }

    public void testMArrayTest2() throws Exception {        
        // Test 2: 3-Dim array of values (but one dimension has different lengths or nil).
        int[][][] in = new int[3][3][];
        for (int i=0; i<3; i++) {
            for(int j=1; j<3; j++) {
                in[i][j] = new int[i+j];
            }
        }
        int[][][] rc;
        fill(in);
        rc = binding.testIntArray(in);
        assertTrue("Test 2 Failed", validate(in, rc));
    }

    public void testMArrayTest3() throws Exception {       
        // Test 1F: 3-Dim array of values.  This could be serialized 
        // as a multi-dimensional array.
        Foo[][][] in = new Foo[3][3][3];
        Foo[][][] rc;
        fillFoo(in);
        rc = binding.testFooArray(in);
        assertTrue("Test 1F Failed", validateFoo(in, rc));
    }

    public void testMArrayTest4() throws Exception {        
        // Test 2F: 3-Dim array of values (but one dimension has different lengths or nil).
        Foo[][][] in = new Foo[3][3][];
        for (int i=0; i<3; i++) {
            for(int j=1; j<3; j++) {
                in[i][j] = new Foo[i+j];
            }
        }
        Foo[][][] rc;
        fillFoo(in);
        rc = binding.testFooArray(in);
        assertTrue("Test 2F Failed", validateFoo(in, rc));
    }

    public void testMArrayTest5() throws Exception {        
        // Test 3F: Some of the Foo elements are multi-referenced.   
        Foo[][][] in = new Foo[3][3][3];
        Foo[][][] rc;
        fillFoo(in);
        
        // Diagonals are set to same Foo
        in[0][0][0] = new Foo();
        in[0][0][0].setValue(-1);
        in[1][1][1] = in[0][0][0];
        in[2][2][2] = in[0][0][0];
        
        rc = binding.testFooArray(in);
        assertTrue("Test 3F Failed (a)", validateFoo(in, rc));
        assertTrue("Test 3F Failed (b)", rc[0][0][0] == rc[1][1][1]);
        assertTrue("Test 3F Failed (c)", rc[0][0][0] == rc[2][2][2]);
    }

    public void testMArrayTest6() throws Exception {        
        // Test 3G: Combination of Foo and DerivedFoo.   
        Foo[][][] in = new Foo[3][3][3];
        Foo[][][] rc;
        fillFoo(in);
        
        // Diagonals are set to same Foo
        in[0][0][0] = new DerivedFoo();
        in[0][0][0].setValue(-1);
        ((DerivedFoo)in[0][0][0]).setValue2(7);
        in[1][1][1] = in[0][0][0];
        in[2][2][2] = in[0][0][0];
        
        rc = binding.testFooArray(in);
        assertTrue("Test 3G Failed (a)", validateFoo(in, rc));
        assertTrue("Test 3G Failed (b)", rc[0][0][0] == rc[1][1][1]);
        assertTrue("Test 3G Failed (c)", rc[0][0][0] == rc[2][2][2]);
        assertTrue("Test 3G Failed (d)", ((DerivedFoo)rc[2][2][2]).getValue2() == 7);
    }
    
        // This test is no longer valid if Axis treats arrays as always single-ref
        /*        
        try {
        // Test 4F: Foo arrays are multi-referenced.   
        Foo[][][] in = new Foo[3][3][3];
        Foo[][][] rc;
        fillFoo(in);
        
        // Same Foo array
        Foo[] fooArray = new Foo[3];
        fooArray[0] = new Foo();
        fooArray[0].setValue(-1);
        fooArray[1] = new Foo();
        fooArray[1].setValue(-1);
        fooArray[2] = new Foo();
        fooArray[2].setValue(-1);
        in[0][0] = fooArray;
        in[1][1] = fooArray;
        in[2][2] = fooArray;
        
        rc = binding.testFooArray(in);
        assertTrue("Test 4F Failed (a)", validateFoo(in, rc));
        assertTrue("Test 4F Failed (b)", rc[0][0] == rc[1][1]);
        assertTrue("Test 4F Failed (c)", rc[0][0] == rc[2][2]);
        } catch (java.rmi.RemoteException re) {
        throw new junit.framework.AssertionFailedError("Remote Exception caught: " + re );
        }
        */

    public void testMArrayTest7() throws Exception {        
        // Test 3F: Some of the Foo elements are multi-referenced.   
        HashMap map = new HashMap();
        Foo[] array = new Foo[1];
        array[0] = new Foo();
        array[0].setValue(123);
        map.put("hello", array);
        
        Map rc;
        
        rc = binding.testMapFooArray(map);
        assertTrue("Test Map Failed (a)", rc != null);
        assertTrue("Test Map Failed (b)", rc.get("hello").getClass().isArray());
        Foo[] rcArray = (Foo[]) rc.get("hello");
        assertTrue("Test Map Failed (c)", rcArray.length == 1 && rcArray[0].getValue() == 123);
    }

    public void fill(int[][][] array) {
        for (int i=0; i < array.length; i++) {
            int[][] array2 = array[i];
                if (array2 != null)
                    for (int j=0; j < array2.length; j++) {
                        int[] array3 = array2[j];
                        if (array3 != null)
                            for (int k=0; k <array3.length; k++) {
                                array3[k] = i + 10*j + 100*k;
                            }
                    }
        }
    }
    public boolean validate(int[][][] orig, int[][][] rc) {
        for (int i=0; i < orig.length; i++) {
            int[][] array2 = orig[i];
                if (array2 != null)
                    for (int j=0; j < array2.length; j++) {
                        int[] array3 = array2[j];
                        if (array3 != null)
                            for (int k=0; k <array3.length; k++) {
                                if ((array3[k] == -1 && rc[i][j][k] == -1) ||
                                    (array3[k]+1000 == rc[i][j][k]))
                                    ; // Okay
                                else
                                    return false;
                            }
                    }
        }
        return true;
    }
    public void fillFoo(Foo[][][] array) {
        for (int i=0; i < array.length; i++) {
            Foo[][] array2 = array[i];
                if (array2 != null)
                    for (int j=0; j < array2.length; j++) {
                        Foo[] array3 = array2[j];
                        if (array3 != null)
                            for (int k=0; k <array3.length; k++) {
                                if (array3[k] == null)
                                    array3[k] = new Foo();
                                array3[k].setValue(i + 10*j + 100*k);
                            }
                    }
        }
    }
    public boolean validateFoo(Foo[][][] orig, Foo[][][] rc) {
        for (int i=0; i < orig.length; i++) {
            Foo[][] array2 = orig[i];
                if (array2 != null)
                    for (int j=0; j < array2.length; j++) {
                        Foo[] array3 = array2[j];
                        if (array3 != null)

                            for (int k=0; k <array3.length; k++) {
                                if ((array3[k].getValue() == -1 && rc[i][j][k].getValue() == -1) ||
                                    (array3[k].getValue()+1000 == rc[i][j][k].getValue()))
                                    ; // Okay
                                else 
                                    return false;
                            }
                    }
        }
        return true;
    }
}


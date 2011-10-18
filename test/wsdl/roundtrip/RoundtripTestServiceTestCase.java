/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.wsdl.roundtrip;

import junit.framework.TestCase;
import test.wsdl.roundtrip.holders.BondInvestmentHolder;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.holders.StringHolder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * This class contains the test methods to verify that Java mapping
 * to XML/WSDL works as specified by the JAX-RPC specification.
 *
 * The following items are tested:
 * - Primitives
 * - Standard Java Classes
 * - Arrays
 * - Multiple Arrays
 * - JAX-RPC Value Types
 * - Nillables (when used with literal element declarations) 
 *
 * @version   1.00  06 Feb 2002
 * @author    Brent Ulbricht
 */
public class RoundtripTestServiceTestCase extends TestCase {

    private RoundtripPortType binding = null;
    private RoundtripPortType binding2 = null;
    private static final double DOUBLE_DELTA = 0.0D;
    private static final float FLOAT_DELTA = 0.0F;

    /**
     *  The Junit framework requires that each class that subclasses
     *  TestCase define a constructor accepting a string.  This method
     *  can be used to specify a specific testXXXXX method in this
     *  class to run.
     */
    public RoundtripTestServiceTestCase(String name) {
        super(name);
    } // Constructor

    /**
     *  The setUp method executes before each test method in this class
     *  to get the binding.
     */
    public void setUp() {

        try {
            binding = new RoundtripPortTypeServiceLocator().getRoundtripTest();
            binding2 = new RoundtripPortTypeServiceLocator().getRoundtripTest2();
        } catch (ServiceException jre) {
            fail("JAX-RPC ServiceException caught: " + jre);
        }
        assertTrue("binding is null", binding != null);

    } // setUp

    /**
     *  Test to insure that a JAX-RPC Value Type works correctly.  StockInvestment
     *  subclasses Investment and should pass data members in both the Investment 
     *  and StockInvestment classes across the wire correctly.
     */
    public void testStockInvestment() throws Exception {
        StockInvestment stock = new StockInvestment();
        stock.setName("International Business Machines");
        stock.setId(1);
        stock.setTradeExchange("NYSE");
        stock.setLastTradePrice(200.55F);
        float lastTradePrice = binding.getRealtimeLastTradePrice(stock);
        assertEquals("The expected and actual values did not match.",
                201.25F,
                lastTradePrice,
                FLOAT_DELTA);
        // Make sure static field dontMapToWSDL is not mapped.
        try {
            (StockInvestment.class).
                    getDeclaredMethod("getDontMapToWSDL",
                            new Class[] {});
            fail("Should not map static member dontMapToWSDL");
        } catch (NoSuchMethodException e) {
            // Cool the method should not be in the class
        }

        // Make sure private field avgYearlyReturn is not mapped.
        try {
            (StockInvestment.class).getDeclaredMethod("getAvgYearlyReturn",
                    new Class[] {});
            fail("Should not map private member avgYearlyReturn");
        } catch (NoSuchMethodException e) {
            // Cool the method should not be in the class
        }
    } // testStockInvestment

    /**
     *  Like the above test, but uses the alternate port.
     */
    public void testStockInvestmentWithPort2() throws Exception {
        StockInvestment stock = new StockInvestment();
        stock.setName("International Business Machines");
        stock.setId(1);
        stock.setTradeExchange("NYSE");
        stock.setLastTradePrice(200.55F);
        float lastTradePrice = binding2.getRealtimeLastTradePrice(stock);
        assertEquals("The expected and actual values did not match.",
                201.25F,
                lastTradePrice,
                FLOAT_DELTA);
        // Make sure static field dontMapToWSDL is not mapped.
        try {
            (StockInvestment.class).getDeclaredMethod("getDontMapToWSDL",
                    new Class[] {});
            fail("Should not map static member dontMapToWSDL");
        } catch (NoSuchMethodException e) {
            // Cool the method should not be in the class
        }

        // Make sure private field avgYearlyReturn is not mapped.
        try {
            (StockInvestment.class).getDeclaredMethod("getAvgYearlyReturn",
                    new Class[] {});
            fail("Should not map private member avgYearlyReturn");
        } catch (NoSuchMethodException e) {
            // Cool the method should not be in the class
        }
    } // testStockInvestmentWithPort2

    /**
     *  Test to insure that a JAX-RPC Value Type works correctly.  PreferredStockInvestment
     *  subclasses StockInvestment and should pass data members in both the Investment, 
     *  StockInvestment, and PreferredStockInvestment classes across the wire correctly.
     */
    public void testPreferredStockInvestment() throws RemoteException {
        PreferredStockInvestment oldStock = new PreferredStockInvestment();
        oldStock.setName("SOAP Inc.");
        oldStock.setId(202);
        oldStock.setTradeExchange("NASDAQ");
        oldStock.setLastTradePrice(10.50F);
        oldStock.setDividendsInArrears(100.44D);
        oldStock.setPreferredYield(new BigDecimal("7.00"));
        PreferredStockInvestment newStock = binding.getDividends(oldStock);
        assertEquals("The expected and actual values did not match.",
                newStock.getName(),
                "AXIS Inc.");
        assertEquals("The expected and actual values did not match.",
                203,
                newStock.getId());
        assertEquals("The expected and actual values did not match.",
                "NASDAQ",
                newStock.getTradeExchange());
        assertEquals("The expected and actual values did not match.",
                101.44D,
                newStock.getDividendsInArrears(),
                DOUBLE_DELTA);
        assertEquals("The expected and actual values did not match.",
                new BigDecimal("8.00"),
                newStock.getPreferredYield());
        assertEquals("The expected and actual values did not match.",
                11.50F,
                newStock.getLastTradePrice(),
                FLOAT_DELTA);
    } // testPreferredStockInvestment

    /**
     *  The BondInvestment class contains all the supported data members:
     *  primitives, standard Java classes, arrays, and primitive wrapper
     *  classes.  This test insures that the data is transmitted across
     *  the wire correctly.
     */
    public void testRoundtripBondInvestment() throws RemoteException {
        CallOptions[] callOptions = new CallOptions[2];
        callOptions[0] = new CallOptions();
        Calendar date = Calendar.getInstance();
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        date.setTimeZone(gmt);
        date.setTime(new Date(1013441507388L));
        callOptions[0].setCallDate(date);
        callOptions[1] = new CallOptions();
        date = Calendar.getInstance();
        date.setTimeZone(gmt);
        date.setTime(new Date(1013441507390L));
        callOptions[1].setCallDate(date);
        HashMap map = new HashMap();
        map.put("Test", "Test Works");

        short[] shortArray = {(short) 30};
        byte[] byteArray = {(byte) 1};
        Short[] wrapperShortArray = {new Short((short) 23), new Short((short) 56)};
        Byte[] wrapperByteArray = {new Byte((byte) 2), new Byte((byte) 15)};

        BondInvestment sendValue = new BondInvestment();

        sendValue.setMap(map);
        sendValue.setOptions(callOptions);
        sendValue.setOptions2(callOptions);
        sendValue.setOptions3(callOptions[0]);
        sendValue.setWrapperShortArray(wrapperShortArray);
        sendValue.setWrapperByteArray(wrapperByteArray);
        sendValue.setWrapperDouble(new Double(2323.232D));
        sendValue.setWrapperFloat(new Float(23.023F));
        sendValue.setWrapperInteger(new Integer(2093));
        sendValue.setWrapperShort(new Short((short) 203));
        sendValue.setWrapperByte(new Byte((byte) 20));
        sendValue.setWrapperBoolean(new Boolean(true));
        sendValue.setShortArray(shortArray);
        sendValue.setByteArray(byteArray);
        date = Calendar.getInstance();
        date.setTimeZone(gmt);
        date.setTime(new Date(1012937861996L));
        sendValue.setCallableDate(date);
        sendValue.setBondAmount(new BigDecimal("2675.23"));
        sendValue.setPortfolioType(new BigInteger("2093"));
        sendValue.setTradeExchange("NYSE");
        sendValue.setFiftyTwoWeekHigh(45.012D);
        sendValue.setLastTradePrice(87895.32F);
        sendValue.setYield(5475L);
        sendValue.setStockBeta(32);
        sendValue.setDocType((short) 35);
        sendValue.setTaxIndicator((byte) 3);

        BondInvestment actual = binding.methodBondInvestmentInOut(sendValue);
        date.setTime(new Date(1013441507308L));

        assertEquals("Returned map is not correct.",
                actual.getMap().get("Test"), "Test Works");
        assertEquals("The expected and actual values did not match.",
                date,
                actual.getOptions()[0].getCallDate());
        date.setTime(new Date(1013441507328L));
        assertEquals("The expected and actual values did not match.",
                date,
                actual.getOptions()[1].getCallDate());
        assertEquals("The expected and actual values did not match.",
                new Short((short) 33),
                actual.getWrapperShortArray()[0]);
        assertEquals("The expected and actual values did not match.",
                new Short((short) 86),
                actual.getWrapperShortArray()[1]);
        assertEquals("The expected and actual values did not match.",
                new Byte((byte) 4),
                actual.getWrapperByteArray()[0]);
        assertEquals("The expected and actual values did not match.",
                new Byte((byte) 18),
                actual.getWrapperByteArray()[1]);
        assertEquals("The expected and actual values did not match.",
                new Double(33.232D),
                actual.getWrapperDouble());
        assertEquals("The expected and actual values did not match.",
                new Float(2.23F),
                actual.getWrapperFloat());
        assertEquals("The expected and actual values did not match.",
                new Integer(3),
                actual.getWrapperInteger());
        assertEquals("The expected and actual values did not match.",
                new Short((short) 2),
                actual.getWrapperShort());
        assertEquals("The expected and actual values did not match.",
                new Byte((byte) 21),
                actual.getWrapperByte());
        assertEquals("The expected and actual values did not match.",
                new Boolean(false),
                actual.getWrapperBoolean());
        assertEquals("The expected and actual values did not match.",
                (short) 36,
                actual.getShortArray()[0]);
        assertEquals("The expected and actual values did not match.",
                (byte) 7,
                actual.getByteArray()[0]);
        date.setTime(new Date(1012937862997L));
        assertEquals("The expected and actual values did not match.",
                date,
                actual.getCallableDate());
        assertEquals("The expected and actual values did not match.",
                new BigDecimal("2735.23"),
                actual.getBondAmount());
        assertEquals("The expected and actual values did not match.",
                new BigInteger("21093"),
                actual.getPortfolioType());
        assertEquals("The expected and actual values did not match.",
                new String("AMEX"),
                actual.getTradeExchange());
        assertEquals("The expected and actual values did not match.",
                415.012D,
                actual.getFiftyTwoWeekHigh(),
                DOUBLE_DELTA);
        assertEquals("The expected and actual values did not match.",
                8795.32F,
                actual.getLastTradePrice(),
                FLOAT_DELTA);
        assertEquals("The expected and actual values did not match.",
                575L,
                actual.getYield());
        assertEquals("The expected and actual values did not match.",
                3,
                actual.getStockBeta());
        assertEquals("The expected and actual values did not match.",
                (short) 45,
                actual.getDocType());
        assertEquals("The expected and actual values did not match.",
                (byte) 8,
                actual.getTaxIndicator());
    } // testRoundtripBondInvestment

    /**
     *  The BondInvestment class contains all the supported data members:
     *  primitives, standard Java classes, arrays, and primitive wrapper
     *  classes.  This test insures that a BondInvestment class received
     *  by a remote method contains the expected values.
     */
    public void testBondInvestmentOut() throws RemoteException {
        BondInvestment actual = binding.methodBondInvestmentOut();
        Calendar date = Calendar.getInstance();
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        date.setTimeZone(gmt);
        date.setTime(new Date(1013441507308L));
        assertEquals("Returned map is not correct.",
                actual.getMap().get("Test"), "Test Works");
        assertEquals("The expected and actual values did not match.",
                date,
                actual.getOptions()[0].getCallDate());
        date.setTime(new Date(1013441507328L));
        assertEquals("The expected and actual values did not match.",
                date,
                actual.getOptions()[1].getCallDate());
        assertEquals("The expected and actual values did not match.",
                new Short((short) 33),
                actual.getWrapperShortArray()[0]);
        assertEquals("The expected and actual values did not match.",
                new Short((short) 86),
                actual.getWrapperShortArray()[1]);
        assertEquals("The expected and actual values did not match.",
                new Byte((byte) 4),
                actual.getWrapperByteArray()[0]);
        assertEquals("The expected and actual values did not match.",
                new Byte((byte) 18),
                actual.getWrapperByteArray()[1]);
        assertEquals("The expected and actual values did not match.",
                new Double(33.232D),
                actual.getWrapperDouble());
        assertEquals("The expected and actual values did not match.",
                new Float(2.23F),
                actual.getWrapperFloat());
        assertEquals("The expected and actual values did not match.",
                new Integer(3),
                actual.getWrapperInteger());
        assertEquals("The expected and actual values did not match.",
                new Short((short) 2),
                actual.getWrapperShort());
        assertEquals("The expected and actual values did not match.",
                new Byte((byte) 21),
                actual.getWrapperByte());
        assertEquals("The expected and actual values did not match.",
                new Boolean(false),
                actual.getWrapperBoolean());
        assertEquals("The expected and actual values did not match.",
                (short) 36,
                actual.getShortArray()[0]);
        assertEquals("The expected and actual values did not match.",
                (byte) 7,
                actual.getByteArray()[0]);
        date.setTime(new Date(1012937862997L));
        assertEquals("The expected and actual values did not match.",
                date,
                actual.getCallableDate());
        assertEquals("The expected and actual values did not match.",
                new BigDecimal("2735.23"),
                actual.getBondAmount());
        assertEquals("The expected and actual values did not match.",
                new BigInteger("21093"),
                actual.getPortfolioType());
        assertEquals("The expected and actual values did not match.",
                new String("AMEX"),
                actual.getTradeExchange());
        assertEquals("The expected and actual values did not match.",
                415.012D,
                actual.getFiftyTwoWeekHigh(),
                DOUBLE_DELTA);
        assertEquals("The expected and actual values did not match.",
                8795.32F,
                actual.getLastTradePrice(),
                FLOAT_DELTA);
        assertEquals("The expected and actual values did not match.",
                575L,
                actual.getYield());
        assertEquals("The expected and actual values did not match.",
                3,
                actual.getStockBeta());
        assertEquals("The expected and actual values did not match.",
                (short) 45,
                actual.getDocType());
        assertEquals("The expected and actual values did not match.",
                (byte) 8,
                actual.getTaxIndicator());
    } // testBondInvestmentOut

    /**
     *  The BondInvestment class contains all the supported data members:
     *  primitives, standard Java classes, arrays, and primitive wrapper
     *  classes.  This test insures that a remote method can recieve the
     *  BondInvestment class and that its values match the expected values.
     */
    public void testBondInvestmentIn() throws RemoteException {
        CallOptions[] callOptions = new CallOptions[2];
        callOptions[0] = new CallOptions();
        Calendar date = Calendar.getInstance();
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        date.setTimeZone(gmt);
        date.setTime(new Date(1013441507388L));
        callOptions[0].setCallDate(date);
        callOptions[1] = new CallOptions();
        date = Calendar.getInstance();
        date.setTimeZone(gmt);
        date.setTime(new Date(1013441507390L));
        callOptions[1].setCallDate(date);
        HashMap map = new HashMap();
        map.put("Test", "Test Works");


        short[] shortArray = {(short) 30};
        byte[] byteArray = {(byte) 1};
        Short[] wrapperShortArray = {new Short((short) 23), new Short((short) 56)};
        Byte[] wrapperByteArray = {new Byte((byte) 2), new Byte((byte) 15)};

        BondInvestment sendValue = new BondInvestment();

        sendValue.setMap(map);
        sendValue.setOptions(callOptions);
        sendValue.setOptions2(callOptions);
        sendValue.setOptions3(callOptions[0]);
        sendValue.setWrapperShortArray(wrapperShortArray);
        sendValue.setWrapperByteArray(wrapperByteArray);
        sendValue.setWrapperDouble(new Double(2323.232D));
        sendValue.setWrapperFloat(new Float(23.023F));
        sendValue.setWrapperInteger(new Integer(2093));
        sendValue.setWrapperShort(new Short((short) 203));
        sendValue.setWrapperByte(new Byte((byte) 20));
        sendValue.setWrapperBoolean(new Boolean(true));
        sendValue.setShortArray(shortArray);
        sendValue.setByteArray(byteArray);
        date = Calendar.getInstance();
        date.setTimeZone(gmt);
        date.setTime(new Date(1012937861996L));
        sendValue.setCallableDate(date);
        sendValue.setBondAmount(new BigDecimal("2675.23"));
        sendValue.setPortfolioType(new BigInteger("2093"));
        sendValue.setTradeExchange("NYSE");
        sendValue.setFiftyTwoWeekHigh(45.012D);
        sendValue.setLastTradePrice(87895.32F);
        sendValue.setYield(5475L);
        sendValue.setStockBeta(32);
        sendValue.setDocType((short) 35);
        sendValue.setTaxIndicator((byte) 3);

        binding.methodBondInvestmentIn(sendValue);
    } // testBondInvestmentIn

    /**
     *  Test the overloaded method getId with a BondInvestment.
     */
    public void testBondInvestmentGetId() throws RemoteException {
        CallOptions[] callOptions = new CallOptions[2];
        callOptions[0] = new CallOptions();
        Calendar date = Calendar.getInstance();
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        date.setTimeZone(gmt);
        date.setTime(new Date(1013441507388L));
        callOptions[0].setCallDate(date);
        callOptions[1] = new CallOptions();
        date = Calendar.getInstance();
        date.setTimeZone(gmt);
        date.setTime(new Date(1013441507390L));
        callOptions[1].setCallDate(date);

        short[] shortArray = {(short) 30};
        byte[] byteArray = {(byte) 1};
        Short[] wrapperShortArray = {new Short((short) 23), new Short((short) 56)};
        Byte[] wrapperByteArray = {new Byte((byte) 2), new Byte((byte) 15)};

        BondInvestment sendValue = new BondInvestment();

        sendValue.setOptions(callOptions);
        sendValue.setOptions2(callOptions);
        sendValue.setOptions3(callOptions[0]);
        sendValue.setWrapperShortArray(wrapperShortArray);
        sendValue.setWrapperByteArray(wrapperByteArray);
        sendValue.setWrapperDouble(new Double(2323.232D));
        sendValue.setWrapperFloat(new Float(23.023F));
        sendValue.setWrapperInteger(new Integer(2093));
        sendValue.setWrapperShort(new Short((short) 203));
        sendValue.setWrapperByte(new Byte((byte) 20));
        sendValue.setWrapperBoolean(new Boolean(true));
        sendValue.setShortArray(shortArray);
        sendValue.setByteArray(byteArray);
        date = Calendar.getInstance();
        date.setTimeZone(gmt);
        date.setTime(new Date(1012937861996L));
        sendValue.setCallableDate(date);
        sendValue.setBondAmount(new BigDecimal("2675.23"));
        sendValue.setPortfolioType(new BigInteger("2093"));
        sendValue.setTradeExchange("NYSE");
        sendValue.setFiftyTwoWeekHigh(45.012D);
        sendValue.setLastTradePrice(87895.32F);
        sendValue.setYield(5475L);
        sendValue.setStockBeta(32);
        sendValue.setDocType((short) 35);
        sendValue.setTaxIndicator((byte) 3);
        sendValue.setId(-123);

        int id = binding.getId(sendValue);
        assertEquals("The wrong id was sent back", -123, id);
    } // testBondInvestmentGetId

    /**
     *  Test the overloaded method getId with a StockInvestment.
     */
    public void testInvestmentGetId() throws RemoteException {
        StockInvestment stock = new StockInvestment();
        stock.setName("International Business Machines");
        stock.setId(1);
        stock.setTradeExchange("NYSE");
        stock.setLastTradePrice(200.55F);

        // Temporarily commented out until I can get this to work.
        int id = binding.getId(stock);
        assertEquals("The wrong id was sent back", 1, id);
    } // testInvestmentGetId

    /**
     *  Test to insure that a multiple array sent by a remote method can be
     *  received and its values match the expected values.
     */
    public void testMethodStringMArrayOut() throws RemoteException {
        String[][] expected = {{"Out-0-0"}, {"Out-1-0"}};
        String[][] actual = binding.methodStringMArrayOut();
        assertEquals("The expected and actual values did not match.",
                expected[0][0],
                actual[0][0]);
        assertEquals("The expected and actual values did not match.",
                expected[1][0],
                actual[1][0]);
    } // testMethodStringMArrayOut

    /**
     *  Test to insure that a multiple array can be sent to a remote method.  The
     *  server matches the received array against its expected values.
     */
    public void testMethodStringMArrayIn() throws RemoteException {
        String[][] sendArray = {{"In-0-0", "In-0-1"}, {"In-1-0", "In-1-1"}};
        binding.methodStringMArrayIn(sendArray);
    } // testMethodStringMArrayIn

    /**
     *  Test to insure that a multiple array matches the expected values on both
     *  the client and server.
     */
    public void testMethodStringMArrayInOut() throws RemoteException {
        String[][] sendArray = {{"Request-0-0", "Request-0-1"}, {"Request-1-0", "Request-1-1"}};
        String[][] expected = {{"Response-0-0", "Response-0-1"}, {"Response-1-0", "Response-1-1"}};
        String[][] actual = binding.methodStringMArrayInOut(sendArray);
        assertEquals("The expected and actual values did not match.",
                expected[0][0],
                actual[0][0]);
        assertEquals("The expected and actual values did not match.",
                expected[0][1],
                actual[0][1]);
        assertEquals("The expected and actual values did not match.",
                expected[1][0],
                actual[1][0]);
        assertEquals("The expected and actual values did not match.",
                expected[1][1],
                actual[1][1]);
    } // testMethodStringMArrayInOut

    /**
     *  Test to insure that an int array can be sent by a remote method and
     *  the received values match the expected values on the client.
     */
    public void testMethodIntArrayOut() throws RemoteException {
        int[] expected = {3, 78, 102};
        int[] actual = binding.methodIntArrayOut();
        assertEquals("The expected and actual values did not match.",
                expected[0],
                actual[0]);
        assertEquals("The expected and actual values did not match.",
                expected[1],
                actual[1]);
        assertEquals("The expected and actual values did not match.",
                expected[2],
                actual[2]);
    } // testMethodIntArrayOut

    /**
     *  Test to insure that an int array can be sent to a remote method.  The server
     *  checks the received array against its expected values.
     */
    public void testMethodIntArrayIn() throws RemoteException {
        int[] sendValue = {91, 54, 47, 10};
        binding.methodIntArrayIn(sendValue);
    } // testMethodIntArrayIn

    /**
     *  Test to insure that an int array can roundtrip between the client
     *  and server.  The actual and expected values are compared on both
     *  the client and server.
     */
    public void testMethodIntArrayInOut() throws RemoteException {
        int[] sendValue = {90, 34, 45, 239, 45, 10};
        int[] expected = {12, 39, 50, 60, 28, 39};
        int[] actual = binding.methodIntArrayInOut(sendValue);
        assertEquals("The expected and actual values did not match.",
                expected[0],
                actual[0]);
        assertEquals("The expected and actual values did not match.",
                expected[1],
                actual[1]);
        assertEquals("The expected and actual values did not match.",
                expected[2],
                actual[2]);
        assertEquals("The expected and actual values did not match.",
                expected[3],
                actual[3]);
        assertEquals("The expected and actual values did not match.",
                expected[4],
                actual[4]);
        assertEquals("The expected and actual values did not match.",
                expected[5],
                actual[5]);
    } // testMethodIntArrayInOut

    /**
     *  Test to insure that all the XML -> Java types can be sent to a remote 
     *  method.  The server checks for the expected values.
     */
    public void testMethodAllTypesIn() throws RemoteException {
        byte[] sendByteArray = {(byte) 5, (byte) 10, (byte) 12};
        Byte[] sendWrapperByteArray = {new Byte((byte) 9), new Byte((byte) 7)};
        Calendar dateTime = Calendar.getInstance();
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        dateTime.setTimeZone(gmt);
        dateTime.setTime(new Date(1012937861986L));
        binding.methodAllTypesIn(new String("Request methodAllTypesIn"),
                new BigInteger("545"),
                new BigDecimal("546.545"),
                dateTime,
                dateTime,
                true,
                (byte) 2,
                (short) 14,
                234,
                10900L,
                23098.23F,
                2098098.01D,
                sendByteArray,
                new Boolean(false),
                new Byte((byte) 11),
                new Short((short) 45),
                new Integer(101),
                new Long(232309L),
                new Float(67634.12F),
                new Double(892387.232D),
                sendWrapperByteArray);
    } // testMethodAllTypesIn

    /**
     *  Test to insure that a primitive byte array matches the expected values on
     *  both the client and server.
     */
    public void testMethodByteArray() throws RemoteException {
        byte[] expected = {(byte) 5, (byte) 4};
        byte[] sendByte = {(byte) 3, (byte) 9};
        byte[] actual = binding.methodByteArray(sendByte);
        assertEquals("The expected and actual values did not match.",
                expected[0],
                actual[0]);
        assertEquals("The expected and actual values did not match.",
                expected[1],
                actual[1]);
    } // testMethodByteArray

    /**
     *  Test to insure that a Calendar object matches the expected values
     *  on both the client and server.
     */
    public void testMethodDateTime() throws RemoteException {
        Calendar expected = Calendar.getInstance();
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        expected.setTimeZone(gmt);
        expected.setTime(new Date(1012937861800L));
        Calendar parameter = Calendar.getInstance();
        parameter.setTimeZone(gmt);
        parameter.setTime(new Date(1012937861996L));
        Calendar actual = binding.methodDateTime(parameter);
        assertEquals("The expected and actual values did not match.",
                expected,
                actual);
    } // testMethodDateTime

    /**
     * Just do the same thing that testMethodDateTime does.  The REAL
     * test here is a compile test.  Both Calendar and Date map to
     * xsd:dateTime.  The original SEI in this roundtrip test contained
     * method:  "Date methodDate(Date)".  But taking that Java -> WSDL ->
     * Java should result in:  "Calendar methodDate(Calendar)".  If that
     * didn't happen, then the compile would fail.
     */
    public void testMethodDate() throws RemoteException {
        Calendar expected = Calendar.getInstance();
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        expected.setTimeZone(gmt);
        expected.setTime(new Date(1012937861800L));
        Calendar parameter = Calendar.getInstance();
        parameter.setTimeZone(gmt);
        parameter.setTime(new Date(1012937861996L));
        Calendar actual = binding.methodDate(parameter);
        assertEquals("The expected and actual values did not match.",
                expected,
                actual);
    } // testMethodDate

    /**
     *  Test to insure that a BigDecimal matches the expected values on 
     *  both the client and server.
     */
    public void testMethodBigDecimal() throws RemoteException {
        BigDecimal expected = new BigDecimal("903483.304");
        BigDecimal actual = binding.methodBigDecimal(new BigDecimal("3434.456"));
        assertEquals("The expected and actual values did not match.",
                expected,
                actual);
    } // testMethodBigDecimal

    /**
     *  Test to insure that a BigInteger matches the expected values on 
     *  both the client and server.
     */
    public void testMethodBigInteger() throws RemoteException {
        BigInteger expected = new BigInteger("2323");
        BigInteger actual = binding.methodBigInteger(new BigInteger("8789"));
        assertEquals("The expected and actual values did not match.",
                expected,
                actual);
    } // testMethodBigInteger

    /**
     *  Test to insure that a String matches the expected values on
     *  both the client and server.
     */
    public void testMethodString() throws RemoteException {
        String expected = "Response";
        String actual = binding.methodString(new String("Request"));
        assertEquals("The expected and actual values did not match.",
                expected,
                actual);
    } // testMethodString

    /**
     *  Test to insure that a primitive double matches the expected
     *  values on both the client and server.
     */
    public void testMethodDouble() throws RemoteException {
        double expected = 567.547D;
        double actual = binding.methodDouble(87502.002D);
        assertEquals("The expected and actual values did not match.",
                expected,
                actual,
                DOUBLE_DELTA);
    } // testMethodDouble

    /**
     *  Test to insure that a primitive float matches the expected
     *  values on both the client and server.
     */
    public void testMethodFloat() throws RemoteException {
        float expected = 12325.545F;
        float actual = binding.methodFloat(8787.25F);
        assertEquals("The expected and actual values did not match.",
                expected,
                actual,
                FLOAT_DELTA);
    } // testMethodFloat

    /**
     *  Test to insure that a primitive long matches the expected
     *  values on both the client and server.
     */
    public void testMethodLong() throws RemoteException {
        long expected = 787985L;
        long actual = binding.methodLong(45425L);
        assertEquals("The expected and actual values did not match.",
                expected,
                actual);
    } // testMethodLong

    /**
     *  Test to insure that a primitive int matches the expected
     *  values on both the client and server.
     */
    public void testMethodInt() throws RemoteException {
        int expected = 10232;
        int actual = binding.methodInt(1215);
        assertEquals("The expected and actual values did not match.",
                expected,
                actual);
    } // testMethodInt

    /**
     *  Test to insure that a primitive short matches the expected
     *  values on both the client and server.
     */
    public void testMethodShort() throws RemoteException {
        short expected = (short) 124;
        short actual = binding.methodShort((short) 302);
        assertEquals("The expected and actual values did not match.",
                expected,
                actual);
    } // testMethodShort

    /**
     *  Test to insure that a primitive byte matches the expected
     *  values on both the client and server.
     */
    public void testMethodByte() throws RemoteException {
        byte expected = (byte) 35;
        byte actual = binding.methodByte((byte) 61);
        assertEquals("The expected and actual values did not match.",
                expected,
                actual);
    } // testMethodByte

    /**
     *  Test to insure that a primitive boolean matches the expected
     *  values on both the client and server.
     */
    public void testMethodBoolean() throws RemoteException {
        boolean expected = false;
        boolean actual = binding.methodBoolean(true);
        assertEquals("The expected and actual values did not match.",
                expected,
                actual);
    } // testMethodBoolean


    /**
     *  Test to insure that an array of a  user defined class matches
     *  the expected values on both the client and server.
     */
    public void testMethodCallOptions() throws RemoteException {
        CallOptions[] callOptions = new CallOptions[1];
        callOptions[0] = new CallOptions();
        Calendar cal = Calendar.getInstance();
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        cal.setTimeZone(gmt);
        cal.setTime(new Date(1013459984577L));
        callOptions[0].setCallDate(cal);

        CallOptions[] actual = binding.methodCallOptions(callOptions);
        cal.setTime(new Date(1013459984507L));
        assertEquals("The expected and actual values did not match.",
                cal,
                actual[0].getCallDate());
    } // testMethodCallOptions

    /**
     *  Test to insure that a wrapper Float object matches
     *  the expected values on both the client and server.
     */
    public void testMethodSoapFloat() throws RemoteException {
        Float actual = binding.methodSoapFloat(new Float(23423.234F));
        assertEquals("The expected and actual values did not match.",
                new Float(232.23F),
                actual);
    } // testMethodSoapFloat

    /**
     *  Test to insure that a wrapper Double object matches
     *  the expected values on both the client and server.
     */
    public void testMethodSoapDouble() throws RemoteException {
        Double actual = binding.methodSoapDouble(new Double(123423.234D));
        assertEquals("The expected and actual values did not match.",
                new Double(2232.23D),
                actual);
    } // testMethodSoapDouble

    /**
     *  Test to insure that a wrapper Boolean object matches
     *  the expected values on both the client and server.
     */
    public void testMethodSoapBoolean() throws RemoteException {
        Boolean actual = binding.methodSoapBoolean(new Boolean(true));
        assertEquals("The expected and actual values did not match.",
                new Boolean(false),
                actual);
    } // testMethodSoapBoolean

    /**
     *  Test to insure that a wrapper Byte object matches
     *  the expected values on both the client and server.
     */
    public void testMethodSoapByte() throws RemoteException {
        Byte actual = binding.methodSoapByte(new Byte((byte) 9));
        assertEquals("The expected and actual values did not match.",
                new Byte((byte) 10),
                actual);
    } // testMethodSoapByte

    /**
     *  Test to insure that a wrapper Short object matches
     *  the expected values on both the client and server.
     */
    public void testMethodSoapShort() throws RemoteException {
        Short actual = binding.methodSoapShort(new Short((short) 32));
        assertEquals("The expected and actual values did not match.",
                new Short((short) 44),
                actual);
    } // testMethodSoapShort

    /**
     *  Test to insure that a wrapper Integer object matches
     *  the expected values on both the client and server.
     */
    public void testMethodSoapInt() throws RemoteException {
        Integer actual = binding.methodSoapInt(new Integer(332));
        assertEquals("The expected and actual values did not match.",
                new Integer(441),
                actual);
    } // testMethodSoapInt

    /**
     *  Test to insure that a wrapper Long object matches
     *  the expected values on both the client and server.
     */
    public void testMethodSoapLong() throws RemoteException {
        Long actual = binding.methodSoapLong(new Long(3321L));
        assertEquals("The expected and actual values did not match.",
                new Long(4412L),
                actual);
    } // testMethodSoapLong

    /**
     *  Test to insure that a user defined exception can be
     *  thrown and received.
     */
    public void testInvalidTickerSymbol() throws RemoteException {
        try {
            binding.throwInvalidTickerException();
            fail("Should have received an InvalidTickerSymbol exception.");
        } catch (InvalidTickerSymbol its) {
            // Test was successful
            assertEquals("The expected and actual values did not match.",
                    "ABC",
                    its.getTickerSymbol());
        }
    } // testInvalidTickerSymbol

    /**
     *  Test to insure that more than one user defined exception can be
     *  defined in a method.
     */
    public void testInvalidTradeExchange() throws RemoteException {
        try {
            binding.throwInvalidTradeExchange();
            fail("TRY: Should have received an InvalidTradeExchange exception.");
        } catch (InvalidTradeExchange ite) {
            // Test was successful
            assertEquals("The expected and actual values did not match.",
                    "XYZ",
                    ite.getTradeExchange());
        } catch (InvalidTickerSymbol its) {
            fail("ITS: Should have received an InvalidTradeExchange exception.");
        } catch (InvalidCompanyId ici) {
            fail("ICI: Should have received an InvalidTradeExchange exception.");
        }
    } // testInvalidTradeExchange

    /**
     * Make sure holder inout parameters can be round tripped.
     */
    public void testHolderTest() throws RemoteException {
        StringHolder sh = new StringHolder("hi there");
        BondInvestment bi = new BondInvestment();
        BondInvestmentHolder bih = new BondInvestmentHolder(bi);
        binding.holderTest(sh, bih);
    } // testHolderTest

} // End class RoundtripTestServiceTestCase

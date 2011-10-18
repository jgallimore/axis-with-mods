/*
 * Copyright 2002-2004 The Apache Software Foundation.
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

package test.types;

import junit.framework.TestCase;
import org.apache.axis.types.MonthDay;

import java.text.NumberFormat;

/**
 * Test validation of types.MonthDay
 */
public class TestMonthDay extends TestCase {

    public TestMonthDay(String name) {
        super(name);
    }

    /**
     * Run a failure test.  values should be invalid.
     */
    private void runFailTest(int month, int day, String tz) throws Exception {
        MonthDay oMonthDay = null;
        try {
            oMonthDay = new MonthDay(month, day, tz);
        }
        catch (Exception e) { // catch the validation exception
        }
        // object is not instantiated on bad data value
        assertNull("validation restriction failed [ month=" +
                String.valueOf(month) + ",day=" + String.valueOf(day) + 
                   ",tz=" + tz + "]. did not restrict bad value.", oMonthDay);
    }

    private void runFailTest(String source) throws Exception {
        MonthDay oMonthDay = null;
        try {
            oMonthDay = new MonthDay(source);
        }
        catch (Exception e) { // catch the validation exception
        }
        // object is not instantiated on bad data value
        assertNull("validation restriction failed [ " + source +
                 "]. did not restrict bad value.", oMonthDay);
    }

    /**
     * Run a successful test.  values should be valid.
     */
    private void runPassTest(int month, int day, String tz) throws Exception {
        MonthDay oMonthDay = null;
        try {
            oMonthDay = new MonthDay(month, day, tz);
        }
        catch (Exception e) { // catch the validation exception
            assertTrue("Validation exception thrown on valid input", false);
        }
        assertEquals("MonthDay month not equal", month, oMonthDay.getMonth());
        assertEquals("MonthDay day not equal", day, oMonthDay.getDay());
        assertEquals("MonthDay timezone not equal", tz, oMonthDay.getTimezone());
    }
    
    private void runPassTest(String source) throws Exception {
        MonthDay oMonthDay = null;
        try {
            oMonthDay = new MonthDay(source);
        }
        catch (Exception e) { // catch the validation exception
            assertTrue("Validation exception thrown on valid input", false);
        }
        assertEquals("MonthDay.toString() not equal", source, oMonthDay.toString());
    }

    /**
     * Test that a normal date succeeeds
     */
    public void testNormal() throws Exception {
        // test all twelve months (1/1, 2/2, etc)
        for (int m=1; m < 13; m++) {
            runPassTest(m, m, null);
        }
    }
    public void testNormalString() throws Exception {
        // test all twelve months
        // use NumberFormat to ensure leading zeros
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(2);
        for (int m=1; m < 13; m++) {
            String s = "--" + nf.format(m) + "-05";
            runPassTest(s);
        }
    }
    public void testNormalString2() throws Exception {
        // check for leading zeros in toString().
        runPassTest("--01-01");
    }
    public void testNormalTimezone() throws Exception {
        runPassTest("--01-01Z");
    }
    public void testNormalPositiveTimezone() throws Exception {
        runPassTest("--02-11+05:00");
    }
    public void testNormalNegativeTimezone() throws Exception {
        runPassTest("--03-11-11:00");
    }

    /**
     * Test that badly formatted strings fail
     */ 
    public void testBadString() throws Exception {
        runFailTest("07-13Z");
        runFailTest("-07-13");
        runFailTest("xx07-13");
        runFailTest("garbage");
    }
    
    /**
     * Test that a bad month fails
     */
    public void testBadMonth() throws Exception {
        runFailTest(13, 20, null);
    }
    public void testBadMonthString() throws Exception {
        runFailTest("--13-13");
    }
    public void testBadMonthString2() throws Exception {
        runFailTest("--1-01");
    }

    /**
     * Test that a bad day fails
     */
    public void testBadDay() throws Exception {
        runFailTest(1, 32, null);
    }
    public void testBadDayString() throws Exception {
        runFailTest("--08-32");
    }
    public void testBadDayString2() throws Exception {
        runFailTest("--1-01");
    }
    public void testEndOfMonthDays() throws Exception {
        runFailTest(1, 32, null);
        runPassTest(1, 31, null);
        runFailTest(2, 30, null);
        runPassTest(2, 29, null);
        runFailTest(3, 32, null);
        runPassTest(3, 31, null);
        runFailTest(4, 31, null);
        runPassTest(4, 30, null);
        runFailTest(5, 32, null);
        runPassTest(5, 30, null);
        runFailTest(6, 31, null);
        runPassTest(6, 30, null);
        runFailTest(7, 32, null);
        runPassTest(7, 31, null);
        runFailTest(8, 32, null);
        runPassTest(8, 31, null);
        runFailTest(9, 31, null);
        runPassTest(9, 30, null);
        runFailTest(10, 32, null);
        runPassTest(10, 31, null);
        runFailTest(11, 31, null);
        runPassTest(11, 30, null);
        runFailTest(12, 32, null);
        runPassTest(12, 31, null);
    }
    
    /**
     * Test that a bad timezone fails
     */
    public void testBadTimezone() throws Exception {
        runFailTest(12, 31, "badzone");
    }
    public void testBadTimezoneString() throws Exception {
        runFailTest("--07-23+EDT");
    }


}

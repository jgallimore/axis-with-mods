package test.encoding;

import org.apache.axis.Constants;
import org.apache.axis.types.Day;
import org.apache.axis.types.Duration;
import org.apache.axis.types.HexBinary;
import org.apache.axis.types.Month;
import org.apache.axis.types.MonthDay;
import org.apache.axis.types.NCName;
import org.apache.axis.types.NMToken;
import org.apache.axis.types.Name;
import org.apache.axis.types.NegativeInteger;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.NonPositiveInteger;
import org.apache.axis.types.NormalizedString;
import org.apache.axis.types.PositiveInteger;
import org.apache.axis.types.Time;
import org.apache.axis.types.Token;
import org.apache.axis.types.URI;
import org.apache.axis.types.UnsignedByte;
import org.apache.axis.types.UnsignedInt;
import org.apache.axis.types.UnsignedLong;
import org.apache.axis.types.UnsignedShort;
import org.apache.axis.types.Year;
import org.apache.axis.types.YearMonth;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * Test deserialization of SOAP responses
 */
public class TestDeser2001 extends TestDeser {

    public TestDeser2001(String name) {
        super(name, Constants.URI_2001_SCHEMA_XSI,
                    Constants.URI_2001_SCHEMA_XSD);
    }

    /**
     * Test deserialization of Date responses
     */
    public void testMinDate() throws Exception {
        Calendar date = Calendar.getInstance();
        date.set(1999, 04, 31, 0, 0, 0);
        date.set(Calendar.MILLISECOND,0);
        deserialize("<result xsi:type=\"xsd:date\">" +
                       "1999-05-31" +
                     "</result>",
                     date.getTime());
    }

    /**
     * Test deserialization of dateTime (Calendar) responses
     */
    public void testMinDateTime() throws Exception {
        Calendar date = Calendar.getInstance();
        date.set(1999,04,31, 12, 01, 30);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));
        date.set(Calendar.MILLISECOND,0);
        deserialize("<result xsi:type=\"xsd:dateTime\">" +
                       "1999-05-31T12:01:30Z" +
                     "</result>",
                     date);
    }

    public void testDateTimeZ() throws Exception {
        Calendar date = Calendar.getInstance();
        date.set(1999,04,31,12,01,30);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));
        date.set(Calendar.MILLISECOND,150);
        deserialize("<result xsi:type=\"xsd:dateTime\">" +
                       "1999-05-31T12:01:30.150Z" +
                     "</result>",
                     date);
    }

    public void testDateTZ() throws Exception {
        Calendar date = Calendar.getInstance();
        date.set(1999, 04, 31, 0, 0, 0);
        date.set(Calendar.MILLISECOND,0);
        deserialize("<result xsi:type=\"xsd:date\">" +
                       "1999-05-31" +
                     "</result>",
                     date.getTime());
    }

    public void testDateTimeTZ() throws Exception {
        Calendar date = Calendar.getInstance();
        date.set(1999,04,31,12,01,30);
        date.set(Calendar.MILLISECOND,150);
        deserialize("<result xsi:type=\"xsd:dateTime\">" +
                       "1999-05-31T12:01:30.150" + calcGMTOffset(date) +
                     "</result>",
                     date);
    }

    /**
     * Test the xsd:Time deserialization
     */
    public void testTimeZ() throws Exception {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 12);
        date.set(Calendar.MINUTE, 01);
        date.set(Calendar.SECOND, 30);
        date.set(Calendar.MILLISECOND,150);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));
        Time time = new Time(date);
        deserialize("<result xsi:type=\"xsd:time\">" +
                       "12:01:30.150Z" +
                     "</result>",
                     time);
    }

/* Commented out for now (not sure how I feel about checking in tests which
   break the build without fixes...)  --Glen
   
    public void testTimePacific() throws Exception {
        deserializeCalendar(TimeZone.getTimeZone("PST8PDT"));
    }
*/

    /**
     * this isnt a test, it is here to list timezones
     */

    public void NotestListTimeZones() throws Exception {
        String ids[] = TimeZone.getAvailableIDs();
        for (int i = 9; i < ids.length; i++) {
            System.out.println(ids[i]);
        }
    }

    /**
     * test local time costs
     * @throws Exception
     */
    public void NotestTimeLocal() throws Exception {
        deserializeCalendar(TimeZone.getDefault());
    }

//    /**
//     * test that this works in Wintertime
//     * @throws Exception
//     */
    public void testTimeUKWinter() throws Exception {
        deserializeCalendar(TimeZone.getTimeZone("GMT+0:00"));
    }

    public void testTimeUKSummer() throws Exception {
        deserializeCalendar(TimeZone.getTimeZone("GMT+1:00"));
    }

    public void testTimeUK() throws Exception {
        deserializeCalendar(TimeZone.getTimeZone("Europe/London"));
    }

    /**
     * this test is here to track down odd behaviour on one system related to these TZ tests
     *
     */
    public void testTimeZoneLogicWorks() {

        TimeZone tz=TimeZone.getTimeZone("GMT");
        //assertEquals(0,tz.getDSTSavings());
        assertEquals(0,tz.getRawOffset());
        Time time=new Time("12:01:30.150+00:00");
        String timeVal=time.toString();
        assertEquals("12:01:30.150Z",timeVal);
    }

    private void deserializeCalendar(TimeZone tz) throws Exception {
        deserializeCalendar(2004, 1, 1, tz);
        deserializeCalendar(2004, 7, 1, tz);
    }

    private void deserializeCalendar(int year, int month,int day,TimeZone tz) throws Exception {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.YEAR, year);
        date.set(Calendar.MONTH, month);
        date.set(Calendar.DAY_OF_MONTH, day);
        date.set(Calendar.HOUR_OF_DAY, 12);
        date.set(Calendar.MINUTE, 01);
        date.set(Calendar.SECOND, 30);
        date.set(Calendar.MILLISECOND, 150);
        date.setTimeZone(tz);
        Time time = new Time(date);
        String offset = calcGMTOffset(date);
        //diagnostics string
        String comment=" [time="+time.toString()+"; tz="+tz.getDisplayName()
                +"; offset="+offset+"]";

        deserialize("<result xsi:type=\"xsd:time\">" +
                       "12:01:30.150" + offset +
                     "</result>",
                     time,
                    false,
                    comment);
    }


    private static final int MILLISECONDS_IN_MINUTE = 60000;
    private static final int MILLISECONDS_IN_HOUR = 60 * MILLISECONDS_IN_MINUTE;

    /**
     *
     * calculate the offset from GMT of the current time zone.
     * If the underlying time zone of the calendar contains historical
     * summer time information, the offset will be corrected for summertime
     * if the date of the calendar is a summertime date
     * @param cal
     * @return an offset string such as +3:00 or -2:30. GMT is returned as -00:00
     */
    private String calcGMTOffset(Calendar cal) {
        int msecOffset = cal.get(Calendar.ZONE_OFFSET) +
                cal.get(Calendar.DST_OFFSET);
        int hourOffset = Math.abs(msecOffset / MILLISECONDS_IN_HOUR);
        String offsetString = msecOffset > 0 ? "+" : "-";
        offsetString += hourOffset >= 10 ? "" + hourOffset : "0" + hourOffset;
        offsetString += ":";
        int minOffset = Math.abs(msecOffset % MILLISECONDS_IN_HOUR) / MILLISECONDS_IN_MINUTE;
        if (minOffset == 0) {
            offsetString += "00";
        }
        else {
            offsetString += minOffset >= 10 ? "" + minOffset : "0" + minOffset;
        }
        return offsetString;
    }

    public void testBase64() throws Exception {
        deserialize("<result xsi:type=\"xsd:base64Binary\">QmFzZTY0</result>",
                    "Base64".getBytes());
    }

    public void testBase64Null() throws Exception {
        deserialize("<result xsi:type=\"xsd:base64Binary\"></result>",
                    new byte[0]);
    }

    public void testHex() throws Exception {
        deserialize("<result xsi:type=\"xsd:hexBinary\">50A9</result>",
                    new HexBinary("50A9"),true);
    }

    public void testHexNull() throws Exception {
        deserialize("<result xsi:type=\"xsd:hexBinary\"></result>",
                    new HexBinary(""),true);
    }

    public void testToken() throws Exception {
        deserialize("<result xsi:type=\"xsd:token\">abcdefg</result>",
                    new Token("abcdefg"),true);
    }

    public void testNormalizedString() throws Exception {
        deserialize("<result xsi:type=\"xsd:normalizedString\">abcdefg</result>",
                    new NormalizedString("abcdefg"),true);
    }

    public void testUnsignedLong() throws Exception {
        deserialize("<result xsi:type=\"xsd:unsignedLong\">100</result>",
                    new UnsignedLong(100),true);
    }

    public void testUnsignedInt() throws Exception {
        deserialize("<result xsi:type=\"xsd:unsignedInt\">101</result>",
                    new UnsignedInt(101),true);
    }

    public void testUnsignedShort() throws Exception {
        deserialize("<result xsi:type=\"xsd:unsignedShort\">102</result>",
                    new UnsignedShort(102),true);
    }

    public void testUnsignedByte() throws Exception {
        deserialize("<result xsi:type=\"xsd:unsignedByte\">103</result>",
                    new UnsignedByte(103),true);
    }

    public void testNonNegativeInteger() throws Exception {
        deserialize("<result xsi:type=\"xsd:nonNegativeInteger\">12345678901234567890</result>",
                    new NonNegativeInteger("12345678901234567890"), true);
    }

    public void testNonPositiveInteger() throws Exception {
        deserialize("<result xsi:type=\"xsd:nonPositiveInteger\">-12345678901234567890</result>",
                    new NonPositiveInteger("-12345678901234567890"), true);
    }

    public void testNegativeInteger() throws Exception {
        deserialize("<result xsi:type=\"xsd:negativeInteger\">-12345678901234567890</result>",
                    new NegativeInteger("-12345678901234567890"), true);
    }

    public void testPositiveInteger() throws Exception {
        deserialize("<result xsi:type=\"xsd:positiveInteger\">12345678901234567890</result>",
                    new PositiveInteger("12345678901234567890"), true);
    }

    public void testName() throws Exception {
        deserialize("<result xsi:type=\"xsd:Name\">:Braves</result>",
                    new Name(":Braves"),true);
    }

    public void testNCName() throws Exception {
        deserialize("<result xsi:type=\"xsd:NCName\">_Atlanta.Braves</result>",
                    new NCName("_Atlanta.Braves"),true);
    }

    public void testNMToken() throws Exception {
        deserialize("<result xsi:type=\"xsd:NMTOKEN\">_A.B.C.1-2-3</result>",
                    new NMToken("_A.B.C.1-2-3"),true);
    }

    public void testQName() throws Exception {
        deserialize("<result xsi:type=\"xsd:QName\" xmlns:qns=\"namespace\">qns:localPart</result>", new QName("namespace", "localPart"), true);
    }

    public void testMapWithNils() throws Exception {
        HashMap m = new HashMap();
        m.put(null, new Boolean("false"));
        m.put("hi", null);
        deserialize("<result xsi:type=\"xmlsoap:Map\" " +
                    "xmlns:xmlsoap=\"http://xml.apache.org/xml-soap\"> " +
                      "<item>" +
                       "<key xsi:nil=\"true\"/>" +
                       "<value xsi:type=\"xsd:boolean\">false</value>" +
                      "</item><item>" +
                       "<key xsi:type=\"xsd:string\">hi</key>" +
                       "<value xsi:nil=\"true\"/>" +
                      "</item>" +
                    "</result>",
                    m);
    }

    public void testArrayWithNilInt() throws Exception {
        ArrayList list = new ArrayList(4);
        list.add(new Integer(1));
        list.add(null);
        list.add(new Integer(3));
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                            "soapenc:arrayType=\"xsd:int[3]\"> " +
                       "<item xsi:type=\"xsd:int\">1</item>" +
                       "<item xsi:nil=\"true\"/>" +
                       "<item xsi:type=\"xsd:int\">3</item>" +
                    "</result>",
                    list, true);
    }

    public void testArrayWithNilString() throws Exception {
        ArrayList list = new ArrayList(4);
        list.add("abc");
        list.add(null);
        list.add("def");
        deserialize("<result xsi:type=\"soapenc:Array\" " +
                            "soapenc:arrayType=\"xsd:string[3]\"> " +
                       "<item xsi:type=\"xsd:string\">abc</item>" +
                       "<item xsi:nil=\"true\"/>" +
                       "<item xsi:type=\"xsd:string\">def</item>" +
                    "</result>",
                    list, true);
    }

    public void testNilSOAPBoolean() throws Exception {
        deserialize("<result xsi:type=\"soapenc:boolean\" xsi:nil=\"true\" />",
                    null);
    }

    public void testYearMonth() throws Exception {
        org.apache.axis.types.YearMonth ym = new YearMonth(2002, 8);
        deserialize("<result xsi:type=\"xsd:gYearMonth\">2002-08</result>",
                     ym);
    }
    public void testYear() throws Exception {
        org.apache.axis.types.Year ym = new Year(2002);
        deserialize("<result xsi:type=\"xsd:gYear\">2002</result>",
                     ym);
    }
    public void testMonth() throws Exception {
        org.apache.axis.types.Month ym = new Month(8);
        deserialize("<result xsi:type=\"xsd:gMonth\">--08--</result>",
                     ym);
    }
    public void testDay() throws Exception {
        org.apache.axis.types.Day ym = new Day(15);
        deserialize("<result xsi:type=\"xsd:gDay\">---15</result>",
                     ym);
    }
    public void testMonthDay() throws Exception {
        org.apache.axis.types.MonthDay ym = new MonthDay(8, 5);
        deserialize("<result xsi:type=\"xsd:gMonthDay\">--08-05</result>",
                     ym);
    }
    public void testDuration() throws Exception {
        org.apache.axis.types.Duration ym = new Duration(false, 2, 3, 8, 8, 1, 3.3);
        deserialize("<result xsi:type=\"xsd:duration\">P2Y3M8DT8H1M3.3S</result>",
                     ym);
        org.apache.axis.types.Duration ym2 = new Duration(true, 2, 3, 8, 8, 1, 3.3);
        deserialize("<result xsi:type=\"xsd:duration\">-P2Y3M8DT8H1M3.3S</result>",
                     ym2);
    }
    public void testAnyURI() throws Exception {
        org.apache.axis.types.URI uri = new URI("urn:this-is-a-test");
        deserialize("<result xsi:type=\"xsd:anyURI\">urn:this-is-a-test</result>",
                     uri);
        uri = new URI("http", "www.macromedia.com", "/testing", "query=1", null);
        deserialize("<result xsi:type=\"xsd:anyURI\">http://www.macromedia.com/testing?query=1</result>",
                     uri);
    }

    public static void main() throws Exception {
        TestDeser2001 deser=new TestDeser2001("");
        deser.testTimeUKWinter();
    }
}

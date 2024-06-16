package de.tsl2.nano.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;

import org.junit.Test;

import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.Xml;

public class XmlTest {

    @Test
    public void testIsXmlSimple() {
        new Xml().isParseable(" <test/> ");
    }

    @Test
    public void testIsXmlNegative() {
        Xml xml = new Xml();
        assertFalse(xml.isParseable(""));
        assertFalse(xml.isParseable(Util.toJson(new TypeBean(1, "test", 2.2, new LinkedList()))));
    }

    @Test
    public void testIsXmlComplex() {
        String s = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
                "<beanDefinition clazz=\"org.anonymous.project.Charge\" name=\"Charge\" isNested=\"false\" isdefault=\"false\" xmlns=\"http://sourceforge.net/projects/tsl2nano ./beandef.xsd\">\n"
                + //
                "   <presentable class=\"de.tsl2.nano.h5.Html5Presentable\" type=\"0\" style=\"0\" visible=\"true\" searchable=\"true\" nesting=\"false\">\n"
                + //
                "      <label>Charge</label>\n" + //
                "      <description>Charge</description>\n" + //
                "      <icon>icons/clock.png</icon>\n" + //
                "   </presentable>\n" + //
                "   <valueExpression expression=\"{chargeitem} ({fromdate}: {value})\" type=\"org.anonymous.project.Charge\"/>\n"
                + //
                "   <isconnected>false</isconnected>\n" + //
                "   <attribute name=\"fromdate\">\n" + //
                "      <attributeDefinition id=\"false\" unique=\"false\" doValidation=\"true\" composition=\"false\" cascading=\"false\" generatedValue=\"false\">\n"
                + //
                "         <declaring class=\"de.tsl2.nano.core.cls.BeanAttribute\" declaringClass=\"org.anonymous.project.Charge\" name=\"fromdate\"/>\n"
                + //
                "         <constraint type=\"java.util.Date\" nullable=\"false\" length=\"19\" scale=\"0\" precision=\"0\">\n"
                + //
                "            <format class=\"de.tsl2.nano.format.RegExpFormat\" regExpFlags=\"0\" maxCharacterCount=\"19\" fullMatch=\"true\" isAbleToParse=\"true\">\n"
                + //
                "               <pattern><![CDATA[[0-3]\\d\\.[0-1]\\d(\\.[1-2]\\d\\d\\d)?( [0-2]\\d\\:[0-5]\\d(\\:[0-5]\\d)?)?]]></pattern>\n"
                + //
                "               <parser parsingType=\"java.sql.Timestamp\" scale=\"0\" precision=\"-1\"/>\n" + //
                "               <entry pattern=\"[0-3]\\d\\.[0-1]\\d(\\.[1-2]\\d\\d\\d)?( [0-2]\\d\\:[0-5]\\d(\\:[0-5]\\d)?)?\">01.01.2016 00:00:00</entry>\n"
                + //
                "            </format>\n" + //
                "         </constraint>\n" + //
                "         <temporalType>java.sql.Timestamp</temporalType>\n" + //
                "         <presentable class=\"de.tsl2.nano.h5.RuleCover\">\n" + //
                "            <ruleCover name=\"&amp;weekcolor\">\n" + //
                "               <delegate class=\"de.tsl2.nano.h5.Html5Presentable\" type=\"24\" style=\"4\" visible=\"true\" searchable=\"true\" nesting=\"false\">\n"
                + //
                "                  <label>charge.fromdate</label>\n" + //
                "                  <description>charge.fromdate</description>\n" + //
                "                  <enabler class=\"de.tsl2.nano.action.IActivable$1\" active=\"true\"/>\n" + //
                "               </delegate>\n" + //
                "               <rule for-property=\"layoutConstraints\" name=\"&amp;weekcolor\"/>\n" + //
                "               <rule for-property=\"presentable\" name=\"-REF-\"/>\n" + //
                "            </ruleCover>\n" + //
                "         </presentable>\n" + //
                "         <columnDefinition name=\"fromdate\" columnIndex=\"0\" sortIndex=\"-1\" isSortUpDirection=\"true\" width=\"-1\" standardSummary=\"false\">\n"
                + //
                "            <format class=\"java.text.SimpleDateFormat\">\n" + //
                "               <reflection type=\"java.text.SimpleDateFormat\">\n" + //
                "                  <member name=\"pattern\">\n" + //
                "                     <object class=\"java.lang.String\">dd.MM EE</object>\n" + //
                "                  </member>\n" + //
                "               </reflection>\n" + //
                "            </format>\n" + //
                "            <presentable class=\"de.tsl2.nano.h5.RuleCover\">\n" + //
                "               <ruleCover name=\"&amp;weekcolor\">\n" + //
                "                  <delegate class=\"de.tsl2.nano.h5.Html5Presentable\" type=\"24\" style=\"4\" visible=\"true\" searchable=\"true\" nesting=\"false\">\n"
                + //
                "                     <label>charge.fromdate</label>\n" + //
                "                     <description>charge.fromdate</description>\n" + //
                "                     <enabler class=\"de.tsl2.nano.action.IActivable$1\" active=\"true\"/>\n" + //
                "                  </delegate>\n" + //
                "                  <rule for-property=\"layoutConstraints\" name=\"&amp;weekcolor\"/>\n" + //
                "                  <rule for-property=\"presentable\" name=\"-REF-\"/>\n" + //
                "               </ruleCover>\n" + //
                "            </presentable>\n" + //
                "         </columnDefinition>\n" + //
                "      </attributeDefinition>\n" + //
                "   </attribute>\n" + //
                "</beanDefinition>";

        assertTrue(new Xml().isParseable(s));
    }

    @Test
    public void testSerialization() {
        TypeBean o1 = new TypeBean(1, "test1", 1.1, null);
        TypeBean o2 = new TypeBean(2, "test2", 2.2, Arrays.asList(o1));
        String expected = createXmlString();
        assertEquals(expected, new Xml().serialize(o2));
        TypeBean deserialized = new Xml().toObject(TypeBean.class, expected);
        assertEquals(expected, new Xml().serialize(deserialized));
    }

    private String createXmlString() {
        return "<TypeBean><connections><index>1</index> <name>test1</name> <value>1.1</value></connections> <index>2</index> <name>test2</name> <value>2.2</value></TypeBean>";
    }
}

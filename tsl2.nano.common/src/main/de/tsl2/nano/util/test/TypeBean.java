/*
 * Copyright © 2002-2009 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.util.test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Simple Test-Bean, holding (public) attributes of most types.
 * 
 * don't change the annotations. they are used by junit testing!!!
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class TypeBean implements Serializable, Comparable<TypeBean> {
    public static final String ATTR_STRING = "string";
    public static final String ATTR_BIGDECIMAL = "bigdecimal";
    public static final String ATTR_DATE = "date";
    public static final String ATTR_TIME = "time";
    public static final String ATTR_TIMESTAMP = "timestamp";
    public static final String ATTR_OBJECT = "object";
    public static final String ATTR_COLLECTION = "collection";
    public static final String ATTR_MAP = "map";
    public static final String ATTR_ARRAYPRIMITIVE = "arrayPrimitive";
    public static final String ATTR_ARRAYOBJECT = "arrayObject";
    public static final String ATTR_WEEKDAYENUM = "weekdayEnum";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_BINARY = "binary";
    public static final String ATTR_RELATION = "relation";

    public static final String ATTR_PRIMITIVEBOOLEAN = "primitiveBoolean";
    public static final String ATTR_PRIMITIVEBYTE = "primitiveByte";
    public static final String ATTR_PRIMITIVECHAR = "primitiveChar";
    public static final String ATTR_PRIMITIVESHORT = "primitiveShort";
    public static final String ATTR_PRIMITIVEINT = "primitiveInt";
    public static final String ATTR_PRIMITIVELONG = "primitiveLong";
    public static final String ATTR_PRIMITIVEFLOAT = "primitiveFloat";
    public static final String ATTR_PRIMITIVEDOUBLE = "primitiveDouble";

    public static final String ATTR_IMMUTABLEBOOLEAN = "immutableBoolean";
    public static final String ATTR_IMMUTABLEBYTE = "immutableByte";
    public static final String ATTR_IMMUTABLECHAR = "immutableChar";
    public static final String ATTR_IMMUTABLESHORT = "immutableShort";
    public static final String ATTR_IMMUTABLEINTEGER = "immutableInteger";
    public static final String ATTR_IMMUTABLELONG = "immutableLong";
    public static final String ATTR_IMMUTABLEFLOAT = "immutableFloat";
    public static final String ATTR_IMMUTABLEDOUBLE = "immutableDouble";
    /** serialVersionUID */
    private static final long serialVersionUID = -6960081067363829887L;
    
    /*
     * all attributes public for test purposes...
     */
    public String string;
    public boolean primitiveBoolean;
    public byte primitiveByte;
    public char primitiveChar;
    public short primitiveShort;
    public int primitiveInt;
    public long primitiveLong;
    public float primitiveFloat;
    public double primitiveDouble;

    public Boolean immutableBoolean;
    public Byte immutableByte;
    public Character immutableChar;
    public Short immutableShort;
    public Integer immutableInteger;
    public Long immutableLong;
    public Float immutableFloat;
    public Double immutableDouble;

    public BigDecimal bigDecimal;

    public Date date;
    public Time time;
    public Timestamp timestamp;
    public Object object;

    public Collection<?> collection;
    public Map<?,?> map;
    
    @Deprecated
    public Object[] arrayObject;
    public int[] arrayPrimitive;
    public byte[] binary;
    public WeekdayEnum weekdayEnum;
    
    public Class type;

    public TypeBean relation;
    
    /**
     * @return Returns the string.
     */
    public String getString() {
        return string;
    }

    /**
     * @param string The string to set.
     */
    public void setString(String string) {
        this.string = string;
    }

    /**
     * @return Returns the primitiveBoolean.
     */
    public boolean isPrimitiveBoolean() {
        return primitiveBoolean;
    }

    /**
     * @param primitiveBoolean The primitiveBoolean to set.
     */
    public void setPrimitiveBoolean(boolean primitiveBoolean) {
        this.primitiveBoolean = primitiveBoolean;
    }

    /**
     * @return Returns the primitiveByte.
     */
    public byte getPrimitiveByte() {
        return primitiveByte;
    }

    /**
     * @param primitiveByte The primitiveByte to set.
     */
    public void setPrimitiveByte(byte primitiveByte) {
        this.primitiveByte = primitiveByte;
    }

    /**
     * @return Returns the primitiveChar.
     */
    public char getPrimitiveChar() {
        return primitiveChar;
    }

    /**
     * @param primitiveChar The primitiveChar to set.
     */
    public void setPrimitiveChar(char primitiveChar) {
        this.primitiveChar = primitiveChar;
    }

    /**
     * @return Returns the primitiveShort.
     */
    public short getPrimitiveShort() {
        return primitiveShort;
    }

    /**
     * @param primitiveShort The primitiveShort to set.
     */
    public void setPrimitiveShort(short primitiveShort) {
        this.primitiveShort = primitiveShort;
    }

    /**
     * @return Returns the primitiveInt.
     */
    public int getPrimitiveInt() {
        return primitiveInt;
    }

    /**
     * @param primitiveInt The primitiveInt to set.
     */
    public void setPrimitiveInt(int primitiveInt) {
        this.primitiveInt = primitiveInt;
    }

    /**
     * @return Returns the primitiveLong.
     */
    public long getPrimitiveLong() {
        return primitiveLong;
    }

    /**
     * @param primitiveLong The primitiveLong to set.
     */
    public void setPrimitiveLong(long primitiveLong) {
        this.primitiveLong = primitiveLong;
    }

    /**
     * @return Returns the primitiveFloat.
     */
    public float getPrimitiveFloat() {
        return primitiveFloat;
    }

    /**
     * @param primitiveFloat The primitiveFloat to set.
     */
    public void setPrimitiveFloat(float primitiveFloat) {
        this.primitiveFloat = primitiveFloat;
    }

    /**
     * @return Returns the primitiveDouble.
     */
    public double getPrimitiveDouble() {
        return primitiveDouble;
    }

    /**
     * @param primitiveDouble The primitiveDouble to set.
     */
    public void setPrimitiveDouble(double primitiveDouble) {
        this.primitiveDouble = primitiveDouble;
    }

    /**
     * @return Returns the immutableBoolean.
     */
    public Boolean getImmutableBoolean() {
        return immutableBoolean;
    }

    /**
     * @param immutableBoolean The immutableBoolean to set.
     */
    public void setImmutableBoolean(Boolean immutableBoolean) {
        this.immutableBoolean = immutableBoolean;
    }

    /**
     * @return Returns the immutableByte.
     */
    public Byte getImmutableByte() {
        return immutableByte;
    }

    /**
     * @param immutableByte The immutableByte to set.
     */
    public void setImmutableByte(Byte immutableByte) {
        this.immutableByte = immutableByte;
    }

    /**
     * @return Returns the immutableChar.
     */
    public Character getImmutableChar() {
        return immutableChar;
    }

    /**
     * @param immutableChar The immutableChar to set.
     */
    public void setImmutableChar(Character immutableChar) {
        this.immutableChar = immutableChar;
    }

    /**
     * @return Returns the immutableShort.
     */
    public Short getImmutableShort() {
        return immutableShort;
    }

    /**
     * @param immutableShort The immutableShort to set.
     */
    public void setImmutableShort(Short immutableShort) {
        this.immutableShort = immutableShort;
    }

    /**
     * @return Returns the immutableInteger.
     */
    public Integer getImmutableInteger() {
        return immutableInteger;
    }

    /**
     * @param immutableInteger The immutableInteger to set.
     */
    public void setImmutableInteger(Integer immutableInteger) {
        this.immutableInteger = immutableInteger;
    }

    /**
     * @return Returns the immutableLong.
     */
    public Long getImmutableLong() {
        return immutableLong;
    }

    /**
     * @param immutableLong The immutableLong to set.
     */
    public void setImmutableLong(Long immutableLong) {
        this.immutableLong = immutableLong;
    }

    /**
     * @return Returns the immutableFloat.
     */
    public Float getImmutableFloat() {
        return immutableFloat;
    }

    /**
     * @param immutableFloat The immutableFloat to set.
     */
    public void setImmutableFloat(Float immutableFloat) {
        this.immutableFloat = immutableFloat;
    }

    /**
     * @return Returns the immutableDouble.
     */
    public Double getImmutableDouble() {
        return immutableDouble;
    }

    /**
     * @param immutableDouble The immutableDouble to set.
     */
    public void setImmutableDouble(Double immutableDouble) {
        this.immutableDouble = immutableDouble;
    }

    /**
     * @return Returns the bigDecimal.
     */
    public BigDecimal getBigDecimal() {
        return bigDecimal;
    }

    /**
     * @param bigDecimal The bigDecimal to set.
     */
    public void setBigDecimal(BigDecimal bigDecimal) {
        this.bigDecimal = bigDecimal;
    }

    /**
     * @return Returns the date.
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param date The date to set.
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * @return Returns the time.
     */
    public Time getTime() {
        return time;
    }

    /**
     * @param time The time to set.
     */
    public void setTime(Time time) {
        this.time = time;
    }

    /**
     * @return Returns the timestamp.
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp The timestamp to set.
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return Returns the object.
     */
    public Object getObject() {
        return object;
    }

    /**
     * @param object The object to set.
     */
    public void setObject(Object object) {
        this.object = object;
    }

    /**
     * @return Returns the collection.
     */
    public Collection<?> getCollection() {
        return collection;
    }

    /**
     * @param collection The collection to set.
     */
    public void setCollection(Collection<?> collection) {
        this.collection = collection;
    }

    /**
     * @return Returns the arrayObject.
     */
    public Object[] getArrayObject() {
        return arrayObject;
    }

    /**
     * @param arrayObject The arrayObject to set.
     */
    public void setArrayObject(Object[] arrayObject) {
        this.arrayObject = arrayObject;
    }

    /**
     * @return Returns the arrayPrimitive.
     */
    @Deprecated
    public int[] getArrayPrimitive() {
        return arrayPrimitive;
    }

    /**
     * @param arrayPrimitive The arrayPrimitive to set.
     */
    public void setArrayPrimitive(int[] arrayPrimitive) {
        this.arrayPrimitive = arrayPrimitive;
    }

    /**
     * @return Returns the weekdayEnum.
     */
    public WeekdayEnum getWeekdayEnum() {
        return weekdayEnum;
    }

    /**
     * @param weekdayEnum The weekdayEnum to set.
     */
    public void setWeekdayEnum(WeekdayEnum weekdayEnum) {
        this.weekdayEnum = weekdayEnum;
    }

    /**
     * @return Returns the type.
     */
    public Class getType() {
        return type;
    }

    /**
     * @param type The type to set.
     */
    public void setType(Class type) {
        this.type = type;
    }

    /**
     * @return Returns the map.
     */
    public Map<?, ?> getMap() {
        return map;
    }

    /**
     * @param map The map to set.
     */
    public void setMap(Map<?, ?> map) {
        this.map = map;
    }

    /**
     * @return Returns the binary.
     */
    public byte[] getBinary() {
        return binary;
    }

    /**
     * @param binary The binary to set.
     */
    public void setBinary(byte[] binary) {
        this.binary = binary;
    }

    /**
     * @return Returns the relation.
     */
    public TypeBean getRelation() {
        return relation;
    }

    /**
     * @param relation The relation to set.
     */
    public void setRelation(TypeBean relation) {
        this.relation = relation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(TypeBean o) {
        return new Double(primitiveDouble).compareTo(o.primitiveDouble) ;
    }
}

package de.tsl2.nano.util.test;
 
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
 
/**
* extend this class for your test. create a (not junit) test-method with all method-parameters you need, but the last method-parameter should be the list of expected values (see
* {@link #expect(Parameter...)}. each junit test method should call this method for each test case. Use {@link #expect(Parameter...)} to define your validation checks. If your test case has
* calculated its values, you should check
* them through {@link Parameter#validate(Comparable)}. Please call {@link #checkCoverage(List)} at the end to throw an exception if not all parameters were checked.
* <p/>
* Example:
*
 * <pre>
* public class PruefeAufVertragTest extends BaseTest {
*
 *   @Test
*   public void testHatKeinenRabattVertrag() throws Exception {
*     String kasseIK;
*     Date abgabeDatum;
*     String pzn;
*     boolean isSprechstundenBedarf;
*     int faktor;
*    
 *     test(kasseIK = "0815", abgabeDatum = d(15, 8, 2015), pzn = "9490w0498", faktor = 1, isSprechstundenBedarf = true, expect(p("hatKeinenVertrag", false)));
*   }
*  
 *   public void test(String kasseIK, Date abgabedDatum, String pzn, int faktor, boolean isSprechstundenBedarf, List<Parameter<?>> expected) throws Exception {
*     Rezept rezept = mockRezept(kasseIK, abgabedDatum, isSprechstundenBedarf);
*     RezeptPositionIF pos = mockRezeptPosition(rezept, pzn, faktor);
*     boolean result = rezept.hatKeinenRabattVertrag(pos);
*     check(expected, "hatKeinenVertrag", result);
*     checkCoverage(expected);
*   }
*   ...
* }
* </pre>
*
 * @author Tom, Thomas Schneider
*/
public class BaseTest extends TextComparison {
 
  /**
   * convenience to create a set of parameter
   *
   * @param pars parameter
   * @return list of parameter
   */
  protected static final List<Parameter<?>> expect(Parameter<?> ... pars) {
    return Arrays.asList(pars);
  }
 
  protected final void check(List<Parameter<?>> pars, String name, Object result) {
    int i = pars.indexOf(p(name, null));
    if (i < 0)
      throw new IllegalArgumentException(name + " couldn't be found in " + pars);
    pars.get(i).validate(result);
  }
 
  /**
   * convenience to create a set of parameter
   *
   * @param pars parameter
   * @return list of parameter
   */
  protected static final void checkCoverage(List<Parameter<?>> pars) {
    for (Parameter<?> p : pars) {
      if (!p.isValid())
        throw new IllegalStateException(p + " was not checked through call of validate(..)");
    }
  }
 
  public final <V extends Comparable<V>> Parameter<V> p(String name, V exptected) {
    return p(name, exptected, Condition.EQUALS);
  }
 
  /**
   * convenience for DSL calls
   *
   * @param name parameter name
   * @param exptected expected result value
   * @param cond {@link Condition}
   * @return new {@link Parameter}
   */
  public final <V extends Comparable<V>> Parameter<V> p(String name, V exptected, Condition cond) {
    return this.new Parameter<V>(name, exptected, cond);
  }
 
  public static final Date d(int tag, int monat, int jahr) {
    return new Date(tag, monat, jahr);
  }
 
  public class Parameter<T extends Comparable<T>> {
 
    String name;
    T exptected;
    Condition cond;
    boolean valid = false;
 
    Parameter(String name, T exptected) {
      this(name, exptected, Condition.EQUALS);
    }
 
    Parameter(String name, T exptected, Condition cond) {
      super();
      this.name = name;
      this.exptected = exptected;
      this.cond = cond;
    }
 
    @SuppressWarnings("unchecked")
    public void validate(Object result) {
      validate((T) result);
    }
 
    public void validate(T result) {
      valid = false;
      String msg = this + " <-- result = " + result;
      switch (cond) {
        case EQUALS:
          assertTrue(msg, exptected == null || exptected.equals(result));
          break;
        case NULL:
          assertTrue(msg, result == null);
          break;
        case NOTNULL:
          assertTrue(msg, result != null);
          break;
        case GT:
          assertTrue(msg, exptected == null || exptected.compareTo(result) > 0);
          break;
        case GE:
          assertTrue(msg, exptected == null || exptected.compareTo(result) >= 0);
          break;
        case LT:
          assertTrue(msg, exptected == null || exptected.compareTo(result) < 0);
          break;
        case LE:
          assertTrue(msg, exptected == null || exptected.compareTo(result) <= 0);
          break;
 
        default:
          break;
      }
      valid = true;
    }
 
    public boolean isValid() {
      return valid;
    }
 
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
      return name.equals(((Parameter<T>) obj).name);
    }
 
    @Override
    public int hashCode() {
      return name.hashCode();
    }
 
    @Override
    public String toString() {
      return getClass().getSimpleName() + "(" + name + ": " + cond + " <" + exptected + ">)";
    }
  }
 
  /**
   * condition for exptected value and calculated result.
   */
  public enum Condition {
    EQUALS, NULL, NOTNULL, GT, GE, LT, LE;
  }
}
package de.tsl2.nano.core.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * generic util to provide data in correct type and format.
 * it is a new implementation to replace ObjectUtil.wrap(), DefaultFormat, FormatUtil in future version.
 * UNUSED YET
 */
@SuppressWarnings({"unchecked", "rawtypes", "PMD.AssignmentInOperand", "PMD.ControlStatementBraces"})
public final class ObjectMapper {
  private static final Logger LOG = Logger.getLogger(ObjectMapper.class.getSimpleName());

  private static final String METHOD_VALUEOF = "valueOf";
  private static final String METHOD_PARSE = "parse";

  private static final Map<Class, Function> wrappers = new LinkedHashMap<>();
  private static final Map<Class, Format> formats = new LinkedHashMap<>();
  static { // IMPROVE: externalize to e.g. json/yaml configuration
    // formats.put(Number.class, NumberFormat.getInstance());
    // formats.put(Short.class, NumberFormat.getInstance());
    // formats.put(Integer.class, NumberFormat.getInstance());
    formats.put(Float.class, NumberFormat.getInstance());
    formats.put(Double.class, NumberFormat.getInstance());
    formats.put(BigInteger.class, NumberFormat.getInstance());
    formats.put(BigDecimal.class, DecimalFormat.getInstance());
    formats.put(Date.class, DateFormat.getInstance());
    formats.put(Time.class, DateFormat.getInstance());
    formats.put(LocalDate.class, createFormat(o -> o.toString(), s -> LocalDate.parse(s)));
    // IMPROVE: if neccessary add implementations for e.g. special date, byte/streams etc.
  }

  private ObjectMapper() {}

  public static <T> T get(Function<String, T> getter, String varName, T defaultValue) {
    Object o = get(getter, varName, (Class<T>) defaultValue.getClass(), false);
    return o == null ? defaultValue : (T) o;
  }

  public static <T> T get(Function<String, T> getter, String varName, Class<T> type, boolean mandatory, Object... wrappingArguments) {
    Object o = getter.apply(varName);
    if (mandatory) {
      if (o == null) {
        throw new NullPointerException(varName);
      } else if (o instanceof String && o.toString().trim().isEmpty()) {
        throw new IllegalArgumentException(varName + " must not be empty");
      }
    } else if (type != null && !type.isAssignableFrom(type)) {
      return ObjectMapper.wrap(o, type, wrappingArguments);
    }
    return (T) o;
  }

  public static void addFormat(Class<?> type, Format f) {
    formats.put(type, f);
  }
  public static <T> void addWrapper(Class<T> type, Function<?, T> wrapper) {
    wrappers.put(type, wrapper);
  }

  /** IMPROVE: replace if-else with map of wrappers */
  public static <T> T wrap(Object o, Class<T> type, Object... wrappingArguments) {
    T result;
    if (o == null || type.isAssignableFrom(o.getClass())) {
      return (T) o;
    } else if (wrappers.containsKey(type)) {
      return (T) wrappers.get(type).apply(o);
    } else if (o instanceof Number && Number.class.isAssignableFrom(type)) {
      return wrapNumber((Number) o, (Class<Number>) type);
    } else if ((result = formatWith(o, type)) != null) {
      return result;
    } else if ((result = wrapWith(o, type, METHOD_VALUEOF, wrappingArguments)) != null) {
      return result;
    } else if ((result = wrapWith(o, type, METHOD_PARSE, wrappingArguments)) != null) {
      return result;
    } else if ((result = construct(o, type, wrappingArguments)) != null) {
      return result;
    } else {
      return (T) o;
    }
  }

  private static <T> T formatWith(Object o, Class<T> type) {
    Format format = String.class.isAssignableFrom(type) ? getFormat(o.getClass()) : getFormat(type);
    if (format != null) {
      if (String.class.isAssignableFrom(type)) {
        return (T) format.format(o);
      } else if (o instanceof CharSequence) {
        try { // simple cast to (T) wont show error at this point
          return cast(format.parseObject(o.toString()), type);
        } catch (ParseException e) {
          LOG.warning(e.toString());
          return null;
        }
      } else {
        return null;
      }
    }
    return null;
  }

  /** convenience to create specific formats */
  public static <T, R> Format createFormat(final Function<T, String> formatter, final Function<String, R> parser) {
    return new Format() {
      @Override
      public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(formatter.apply((T) obj));
        return toAppendTo;
      }

      @Override
      public Object parseObject(String source, ParsePosition pos) {
        pos.setIndex(1);
        return parser.apply(source);
      }
    };
  }

  private static <T> T cast(Object o, Class<T> type) {
    if (o instanceof Number && Number.class.isAssignableFrom(type)) {
      o = wrapNumber((Number) o, (Class<Number>) type);
    }
    return type.cast(o);
  }

  static Format getFormat(Class type) {
    return formats.get(type);
  }

  /** if available, calls type.<methodName>(o) and returns new instance - otherwise null. if invocation fails, throw IllegalArgumentException */
  static <T> T wrapWith(Object o, Class<T> type, String methodName, Object... wrappingArguments) {
    try {
      Class[] argTypes = getWrappingArgTypes(o.getClass(), wrappingArguments);
      Object[] args = getWrappingArgs(o, wrappingArguments);
      Method method = getMethod(type, methodName, argTypes);
      // method.setAccessible(true);
      return method == null ? null : (T) method.invoke(null, args);
    } catch (NoSuchMethodException | SecurityException e) {
      return null;
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalArgumentException(e);
    }

  }

  private static <T> Method getMethod(Class<T> type, String methodName, Class[] argTypes) throws NoSuchMethodException {
    ArrayList<Method> methods = new ArrayList(Arrays.asList(type.getMethods()));
    methods.addAll(Arrays.asList(type.getDeclaredMethods()));
    return methods.stream().filter(m -> m.getName().equals(methodName) && hasCompatibleArgTypes(argTypes, m.getParameterTypes())).findFirst().orElse(null);
  }

  private static boolean hasCompatibleArgTypes(Class[] argTypes, Class<?>[] parameterTypes) {
    if (argTypes.length != parameterTypes.length)
      return false;
    for (int i = 0; i < argTypes.length; i++) {
      if (!parameterTypes[i].isAssignableFrom(argTypes[i]))
        return false;
    }
    return true;
  }

  /** if available, calls contructor(o) and returns new instance - otherwise null. if invocation fails, throw IllegalArgumentException */
  static <T> T construct(Object o, Class<T> type, Object... wrappingArguments) {
    try {
      Class[] argTypes = getWrappingArgTypes(o.getClass(), wrappingArguments);
      Object[] args = getWrappingArgs(o, wrappingArguments);
      Constructor<?> constructor = getConstructor(type, argTypes);
      return constructor == null ? null : (T) constructor.newInstance(args);
    } catch (NoSuchMethodException | SecurityException e) {
      return null;
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static <T> Constructor<T> getConstructor(Class<T> type, Class[] argTypes) throws NoSuchMethodException {
    ArrayList<Constructor> constructors = new ArrayList(Arrays.asList(type.getConstructors()));
    constructors.addAll(Arrays.asList(type.getDeclaredConstructors()));
    return constructors.stream().filter(c -> hasCompatibleArgTypes(argTypes, c.getParameterTypes())).findFirst().orElse(null);
  }

  static Class[] getWrappingArgTypes(Class type, Object... wrappingArguments) {
    Class[] argTypes = new Class[wrappingArguments.length + 1];
    argTypes[0] = type;
    for (int i = 0; i < wrappingArguments.length; i++) {
      argTypes[i + 1] = wrappingArguments[i] != null ? wrappingArguments[i].getClass() : Object.class;
    }
    return argTypes;
  }

  static Object[] getWrappingArgs(Object o, Object... wrappingArguments) {
    Object[] args = new Object[wrappingArguments.length + 1];
    args[0] = o;
    for (int i = 0; i < wrappingArguments.length; i++) {
      args[i + 1] = wrappingArguments[i];
    }
    return args;
  }

  /** wrap and cast from current number to another number type */
  static <T> T wrapNumber(Number n, Class<? extends Number> type) {
    // the compiler needs the double cast with (T)
    if (Byte.class.isAssignableFrom(type)) {
      return (T) type.cast(n.byteValue());
    } else if (Short.class.isAssignableFrom(type)) {
      return (T) type.cast(n.shortValue());
    } else if (Integer.class.isAssignableFrom(type)) {
      return (T) type.cast(n.intValue());
    } else if (Long.class.isAssignableFrom(type)) {
      return (T) type.cast(n.longValue());
    } else if (Float.class.isAssignableFrom(type)) {
      return (T) type.cast(n.floatValue());
    } else if (Double.class.isAssignableFrom(type)) {
      return (T) type.cast(n.doubleValue());
    } else {
      throw new IllegalArgumentException("cannot wrap number " + n + " to type " + type);
    }
  }

  public static <T> T getDefaultOnNull(Class<T> type) {
    if (Boolean.class.isAssignableFrom(type)) {
      return (T) Boolean.FALSE;
    } else if (Number.class.isAssignableFrom(type)) {
      return wrapWith("0", type, METHOD_VALUEOF);
    } else if (String.class.isAssignableFrom(type) || Character.class.isAssignableFrom(type)) {
      return wrapWith("", type, METHOD_VALUEOF);
    } else {
      return null;
    }
  }
}


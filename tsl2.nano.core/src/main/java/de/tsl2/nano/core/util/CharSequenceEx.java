package de.tsl2.nano.core.util;

/** as jdk does not provide a useable CharSequence interface extension with important methods like indexOf() 
 * which is implemented by all Standard implementation like String, 
 * StringBuilder and StringBuffer (using Appendable and AbstractStringBuilder (not public!)), 
 * we provide it here 
 * TODO: provide a proxy (class name: StringEx) through DelegatorProxy using the CharSequenceEx interface and
 * the methods of StringUtil.Strings, String, StringBuilder, StringBuffer
 */
public interface CharSequenceEx extends CharSequence, Appendable {
    int indexOf(String sub);

    int lastIndexOf(String sub);

    String substring(String from, String to);

    String substring(String from, String to, int start);

    String substring(String from, String to, int start, boolean lastTo);

    String extract(String regex);

    String[] extractAll(String regex);

    String cut(int len);

    String trim(String charsToTrim);
}

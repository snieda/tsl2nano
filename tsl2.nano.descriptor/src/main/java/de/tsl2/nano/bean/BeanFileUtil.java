package de.tsl2.nano.bean;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.text.Format;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanCollector;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Provides read/write files to/from flat files like csv, tabsheet, markdown-table and html-table
 * 
 * @author Thomas Schneider
 */
public class BeanFileUtil {
    private static final Log LOG = LogFactory.getLog(BeanFileUtil.class);

    public enum FileType {CSV, TABSHEET, HTML, MARKDOWN_TABLE};
    
    /**
     * convenience for {@link #fromFlatFile(Reader, Class, String...)}.
     */
    public static <T> Collection<T> fromFile(String fileName, final FileType fileType, Class<T> rootType, String... attributeNames) {
    	assert fileType != null && !Util.in(fileType, FileType.HTML) : "FileType HTML is not supported!";
    	
        return Util.trY(() -> fromFlatFile(reader(fileName),
                FileType.CSV.equals(fileType) ? "," : "\t",
                rootType,
                null,
                attributeNames));
    }

    /**
     * convenience for {@link #fromFlatFile(Reader, Class, String...)}.
     */
    public static <T> Collection<T> fromFlatFile(String fileName, Class<T> rootType, String... attributeNames) {
        return Util.trY(() -> fromFlatFile(reader(fileName), rootType, null, attributeNames));
    }

    /**
     * convenience for {@link #fromFlatFile(Reader, Class, String...)}.
     */
    public static <T> Collection<T> fromFlatFile(String fileName,
            String separation,
            Class<T> rootType,
            String... attributeNames) {
        return Util.trY(() ->fromFlatFile(reader(fileName),
                separation,
                rootType,
                null,
                attributeNames));
    }

	private static BufferedReader reader(String fileName) throws FileNotFoundException {
		return new BufferedReader(new FileReader(FileUtil.userDirFile(fileName)));
	}

    /**
     * reads a flat (like csv) file and tries to put the values to the given bean type. the given bean type must have a
     * default constructor.
     * 
     * @param <T>
     * @param r normally a buffered reader.
     * @param rootType root bean type to be instantiated and filled to the result collection
     * @param attributeNames (optional) simple attribute names or point-separated relation expressions. use null to
     *            ignore the token
     * @return filled collection of beans
     */
    public static <T> Collection<T> fromFlatFile(Reader r,
            Class<T> rootType,
            Map<String, Format> formats,
            String... attributeNames) {
        return fromFlatFile(r, null, Bean.newBean(rootType), formats, attributeNames);
    }

    /**
     * reads a flat (like csv) file and tries to put the values to the given bean type. the given bean type must have a
     * default constructor.
     * 
     * @param <T>
     * @param r normally a buffered reader.
     * @param separation separation character. if this is null, the attributeNames must contain at least one
     *            column-index.
     * @param rootType root bean type to be instantiated and filled to the result collection
     * @param attributeNames (optional) simple attribute names or point-separated relation expressions. use null to
     *            ignore the token
     * @return filled collection of beans
     */
    public static <T> Collection<T> fromFlatFile(Reader r,
            String separation,
            Class<T> rootType,
            Map<String, Format> formats,
            String... attributeNames) {
        return fromFlatFile(r, separation, Bean.newBean(rootType), formats, attributeNames);
    }

    /**
     * reads a flat file (like csv) and tries to put the values to the given bean type. the given bean type should have
     * a default constructor. the given bean holds an example instance of your root-type. it is possible to provide an
     * overridden bean, to implement the method {@link Bean#newInstance(Object...)} to initialize your desired instance.
     * a new instance will be created on each new line.
     * </p>
     * there are two possibilities to use this method:</br>
     * - with a field separator (like comma or semicolon)</br>
     * - with line-column definitions (like '1-10:myBeanAttributeName)
     * </p>
     * 
     * with the first alternative, you give a separator (not null) and the pure attribute names of the given rootType
     * (included in your bean). it is possible to give attribute-relations like 'myAttr1.myAttr2.myAttr3'. to ignore
     * fields, use <code>null</code> as beanattribute-name.
     * </p>
     * 
     * the second alternative needs all beanattribute names with a column-prefix like 'begin-end:attributename'. for
     * example: 1-11:date. it is possible to use bean relations as in the first alternative, too.
     * <p/>
     * Please notice, that the column-indexes are one-based - the first column is 1 - and the end-index will not be
     * included like in String.substring(begin, end). e.g. to read '01.01.2001' you need 1-11. the indexes are
     * equivalent to standard texteditors like notepad++.
     * 
     * @param <T> root type
     * @param r normally a buffered reader.
     * @param separation separation character. if this is null, the attributeNames must contain at least one
     *            column-index.
     * @param bean root bean type to be instantiated and filled to the result collection
     * @param attributeFormats (optional) some format-instances to parse to the right object. used if found, otherwise
     *            standard formatters will be used.
     * @param attributeNames simple attribute names or point-separated relation expressions. use null to ignore the
     *            token
     * @return filled collection of beans
     */
    public static <T> Collection<T> fromFlatFile(Reader r,
            String separation,
            Bean<T> bean,
            Map<String, Format> attributeFormats,
            String... attributeNames) {
        /*
         * do some validation checks
         */
        if (Util.isEmpty(attributeNames)) {
            attributeNames = bean.getAttributeNames();
        }
        if (separation == null) {
            boolean hasColumnIndexes = false;
            for (String n : attributeNames) {
                if (n != null && n.contains(":")) {
                    hasColumnIndexes = true;
                    break;
                }
            }
            if (!hasColumnIndexes) {
                throw ManagedException
                    .implementationError(
                        "if you don't give a separation-character, you should give at least one column-index in your attribute-names",
                        null);
            }
        }

        final Collection<T> result = new LinkedList<T>();
        final StreamTokenizer st = new StreamTokenizer(r);
        /*
         * to remove parsing of numbers by the tokenizer we have to reset all!
         * ugly jdk implementation - perhaps we should use Scanner.
         */
        st.resetSyntax();
        st.wordChars(0x00, 0xFF);
//        st.quoteChar('\"');
        st.whitespaceChars('\r', '\r');
        st.whitespaceChars('\n', '\n');
        st.eolIsSignificant(true);
        st.commentChar('#');
//            st.slashSlashComments(true);
//            st.slashStarComments(true);
        int ttype = 0;
        final Class<T> rootType = bean.getClazz();
//        bean.newInstance();
        final Map<String, Exception> errors = new Hashtable<String, Exception>();
        final String rootInfo = rootType.getSimpleName() + ".";
        /*
         * prepare the format cache to parse strings with performance
         */
        final Map<String, Format> formatCache = new HashMap<String, Format>();
        if (attributeFormats != null) {
            formatCache.putAll(attributeFormats);
        }
        /*
         * prepared fixed columns
         */
        int begin, end;
        String attrName;
        final Map<String, Point> attributeColumns = new LinkedHashMap<String, Point>(attributeNames.length);
        for (int i = 0; i < attributeNames.length; i++) {
            if (separation != null || attributeNames[i] == null) {
                attributeColumns.put((attributeNames[i] != null ? attributeNames[i] : "null:" + String.valueOf(i)),
                    null);
            } else {
                begin = Integer.valueOf(StringUtil.substring(attributeNames[i], null, "-"));
                end = Integer.valueOf(StringUtil.substring(attributeNames[i], "-", ":"));
                if (end <= begin || begin < 0 || end < 1) {
                    throw new IllegalArgumentException("The given range " + attributeNames[i] + " is illegal!");
                }
                attrName = StringUtil.substring(attributeNames[i], ":", null);
                //store one-based indexes
                attributeColumns.put(attrName, new Point(begin - 1, end - 1));
            }
        }
        final Set<String> cols = attributeColumns.keySet();
        boolean filled = false;
        /*
         * do the reading, collecting all errors to throw only one exception at the end
         */
        try {
        	int markdownTableHeader = 0;
        	boolean isMarkdownTable = false;
            String t;
            while ((ttype = st.nextToken()) != StreamTokenizer.TT_EOF) {
                if (ttype != StreamTokenizer.TT_EOL && st.sval.trim().length() > 0) {
                	// ignore markdown table styling
                	if (markdownTableHeader < 3 && (st.sval.startsWith("|") || st.sval.startsWith("--"))) {
                		markdownTableHeader++;
                		if (markdownTableHeader == 3)
                			isMarkdownTable = true;
                		continue;
                	} else if (isMarkdownTable) {
                		st.sval = st.sval.substring(1).replaceAll("\\s*[|]\\s*", separation);
                	}
                	
                    bean.newInstance();
                    int lastSep = 0;
                    for (final String attr : cols) {
                        final Point c = attributeColumns.get(attr);
                        if (c != null) {
                            if (c.x >= st.sval.length() || c.y > st.sval.length()) {
                                throw new StringIndexOutOfBoundsException("The range " + c.x
                                    + "-"
                                    + c.y
                                    + " is not available on line "
                                    + st.lineno()
                                    + " with length "
                                    + st.sval.length()
                                    + ":"
                                    + st.sval);
                            }
                            t = st.sval.substring(c.x, c.y);
                        } else {
                            t = StringUtil.substring(st.sval, null, separation, lastSep);
                        }
                        lastSep += t.length() + (c != null ? 0 : separation.length());
                        //at line end, no separation char will occur
                        if (st.sval.length() < lastSep) {
                            lastSep = st.sval.length();
                        }
                        if (attr == null || attr.startsWith("null:")) {
                            LOG.info("ignoring line " + st.lineno()
                                + ", token '"
                                + t
                                + "' at column "
                                + (lastSep - t.length()));
                            continue;
                        }
                        t = StringUtil.trim(t, " \"");
                        final String info = "reading line " + st.lineno() + ":'" + t + "' -> " + rootInfo + attr;
                        try {
                            Object newValue = null;
                            if (!Util.isEmpty(t)) {
                                final BeanAttribute beanAttribute = BeanAttribute.getBeanAttribute(rootType, attr);
                                Format parser = formatCache.get(attr);
                                if (parser == null) {
                                    parser = FormatUtil.getDefaultFormat(beanAttribute.getType(), true);
                                    formatCache.put(attr, parser);
                                }
                                newValue = parser.parseObject(t);
                                bean.setValue(beanAttribute.getName(), newValue);
                            }
                            LOG.debug(info + "(" + newValue + ")");
                            filled = true;
                        } catch (final Exception e) {
                            LOG.info("problem on " + info);
                            LOG.error(e.toString());
                            errors.put(info, e);
                        }
                    }
                    if (filled) {
                        result.add(bean.getInstance());
                    }
                }
            }
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
        if (errors.size() > 0) {
            throw new ManagedException(StringUtil.toFormattedString(errors, 80, true));
        }
        LOG.info("import finished - imported items: " + result.size() + " of type " + rootType.getSimpleName());
        return result;
    }

    public static void toFile(Collection<?> col, String filename, FileType type) {
    	BeanCollector<?,? extends Object> collector = BeanCollector.getBeanCollector(col, 0);
    	String data;
    	switch (type) {
		case CSV:
			data = presentAsCSV(collector);
			break;
		case TABSHEET:
			data = presentAsTabSheet(collector);
			break;
		case MARKDOWN_TABLE:
			data = presentAsMarkdownTable(collector);
			break;
		case HTML:
			data = presentAsHtmlTable(collector);
			break;
		default:
			throw new IllegalArgumentException(Util.asString(type));
		}
    	FileUtil.writeBytes(data.getBytes(), filename, false);
    }
    
    /**
     * delegates to {@link #present(BeanCollector, String, String, String, String, String, String, String, String)}.
     */
    public static String presentAsCSV(BeanCollector collector) {
        return present(collector, "", "", "", "\n", "", ",", null, null);
    }

    /**
     * delegates to {@link #present(BeanCollector, String, String, String, String, String, String, String, String)}.
     */
    public static String presentAsTabSheet(BeanCollector collector) {
        return present(collector, "", "", "", "\n", "", "\t", null, null);
    }

    /**
     * creates a simple html-table as presentation for the given collector.
     * 
     * @param collector
     * @return see {@link #present(BeanCollector, String, String, String, String, String, String, String, String)}
     */
    public static String presentAsHtmlTable(BeanCollector collector) {
        return present(collector, "<table>\n", "</table>", "<tr>", "</tr>\n", "<td>", "\"</td>", "", ": <div/>\"");
    }

    public static String presentAsMarkdownTable(BeanCollector collector) {
        return present(collector, StringUtil.fixString(79, '-'), "\n" + StringUtil.fixString(79, '-'), "\n|", "", " ", " |", null, null);
    }

    /**
     * creates a string representing all items with all attributes of the given beancollector (holding a collection of
     * items).
     * <p/>
     * All parameters without nameBegin and nameEnd must not be null!
     * 
     * @param collector holding a list - defining the attribute presentation.
     * @param header text header
     * @param footer text footer
     * @param rowBegin text on a new line
     * @param rowEnd text on line end
     * @param colBegin text on new column
     * @param colEnd text on column end
     * @param nameBegin (optional) if not null, starting text of a fields name. if null, no field name will be presented
     * @param nameEnd (optional) if not null, ending text of a fields name. if null, no field name will be presented
     * @return string presentation of given collector
     */
    public static String present(BeanCollector collector,
            String header,
            String footer,
            String rowBegin,
            String rowEnd,
            String colBegin,
            String colEnd,
            String nameBegin,
            String nameEnd) {
        Collection c = collector.getCurrentData();
        List<IAttributeDefinition> attributes = collector.getBeanAttributes();
        StringBuilder buf = new StringBuilder(c.size() * attributes.size() * 30 + 100);
        buf.append(header);
        if (!Util.isEmpty(header) && ENV.get("bean.flatfile.createheader", true)) {
        	buf.append(rowBegin);
	        for (IAttributeDefinition a : attributes) {
	            buf.append(colBegin + (nameBegin != null && nameEnd != null ? nameBegin + a.getName() + nameEnd : "")
	                + a.getName() + colEnd);
	        }
        	buf.append(rowEnd + header);
        }
        for (Object o : c) {
            buf.append(rowBegin);
            for (IAttributeDefinition a : attributes) {
                buf.append(colBegin + (nameBegin != null && nameEnd != null ? nameBegin + a.getName() + nameEnd : "")
                    + collector.getColumnText(o, a) + colEnd);
            }
            buf.append(rowEnd);
        }
        buf.append(footer);
        return buf.toString();
    }

}

/**
 * To avoid using package awt, we can't use java.awt.Point - but we need a simple Point.
 */
class Point {
    int x, y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

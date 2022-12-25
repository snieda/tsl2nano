package de.tsl2.nano.specification.rules;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.specification.ParType;
import de.tsl2.nano.specification.Pool;
import de.tsl2.nano.tree.STree;
import de.tsl2.nano.tree.Tree;

/**
 * Reads a CSV-file containing a decision table and creates rule conditions. enables creating (business) rules on
 * decision tables done by product managers in tools like Excel - to be transformed/interpreted by this class into a
 * machine readable rule.
 * <p/>
 * Each line starts with a key/name followed by one or more values. If the line starts with an empty cell, the key from
 * last line will be used to append the values of the new line.<br/>
 * #KEYWORD Description:
 * 
 * <pre>
 * - name (otional)         : rule name. if null the file name will be used
 * - description (optional) : rule description
 * - parameter...(optional) : additional (used in condition expressions) parameters with default value
 * - matrix                 : decision table
 * </pre>
 * 
 * Decision Table Description:<br/>
 * The first line is the header, starting with keyword {@link #KEY_MATRIX} followed by condition names.<br>
 * The following lines start with the name of a rule parameter, followed by all possible conditions.
 * <p/>
 * Conditions:
 * 
 * <pre>
 * All conditions have the form: [OPERATOR]{VALUE}
 * operators are: =, !=, <, <=, >, >=
 * if no operator is given, = will be used
 * a value must be a Comparable and can referenz another parameter
 * </pre>
 * 
 * The first line that starts with an empty cell will end the matrix (decision table). the last line of the matrix will
 * be the exptected result line.
 * <p/>
 * Example:
 * 
 * <pre>
 * Name;ABR-7493;;;;;;;
 * Beschreibung;"Rezeptfehler ""Betragshorror"" bei §302: Rezept kann ohne Änderung der Daten nicht abgerechnet werden.";;;;;;;
 * ;"Rezeptfehler ""Betragshorror"" bei §300, Rechnungstyp Arzneimittel oder Hilfsmittel: Rezept kann ohne Änderung der Daten nicht abgerechnet werden.";;;;;;;
 * ;"Rezeptfehler ""Betragshorror"" bei §300, Rechnungstyp Pflegehilfsmittel: Rezept kann ohne Änderung der Daten abgerechnet werden.";;;;;;;
 * ;"*Rezeptfehler ""Betragshorror"" bei §300 Pflegehilfsmittel Irrläufer: Rezept kann ohne Änderung der Daten abgerechnet werden.";;;;;;;
 * ;;;;;;;;
 * MATRIX;R1;R2;R3;R4;R5;R6;R7;R8
 * DIFF;0;0;0;0;>0;>0;>0;>0
 * PARAGRAPH;300;300;302;302;300;300;302;302
 * ISTHILFSMITTEL;Ja;Nein;Ja;Nein;Ja;Nein;Ja;Nein
 * ERGBEBNIS;OK;OK;OK;OK;WARNUNG;FEHLER;FEHLER;FEHLER
 * </pre>
 * 
 * @author Thomas Schneider / 2015
 */
@SuppressWarnings("rawtypes")
public class RuleDecisionTable<T> extends AbstractRule<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 982758388836072241L;
    /** contains the name, description and result vector - and additional variables */
    transient Map<String, Object> properties;
    /** the content of the decision table */
    transient Map<String, List<Condition<?>>> par;
    /**
     * caclulated table on all conditions compared to the given arguments. will be hold in memory for performance issues
     */
    transient byte[][] matchtable;

    public static final char PREFIX = '&';

    public RuleDecisionTable() {
    }

    public RuleDecisionTable(String name, String csvExpression, LinkedHashMap<String, ParType> parameter) {
    	super(name, saveCSV(name, csvExpression), parameter);
    }

	RuleDecisionTable(Map<String, Object> properties, Map<String, List<Condition<?>>> par) {
        super();
        this.name = (String) properties.get("name");
        setOperation((String) properties.get("operation"));
        this.properties = properties;
        this.par = par;
        checkConsistence(properties, par);
    }

    private static String saveCSV(String name, String csvExpression) {
        String path = ENV.get(Pool.class).getDirectory(RuleDecisionTable.class) + name;
        FileUtil.save(path, csvExpression);
        return path;
	}

    /**
     * check for overlapping conditions
     * <p/>
     * UNDER CONSTRUCTION
     * 
     * @param properties
     * @param par
     */
    private void checkConsistence(Map<String, Object> properties, Map<String, List<Condition<?>>> par) {
//        Map<String, Object> args = new HashMap<String, Object>();
//        for (String name : par.keySet()) {
//            for (Condition c : par.get(name)) {
//                //TODO: check all combinations...
//                args.put(name, c.operand2);
//                byte[][] mt = createMatchTable(args);
//                //check for more than one result
//                evalResult(mt, false);
//            }
//        }
    }

    @Override
    public String prefix() {
        return String.valueOf(PREFIX);
    }

    public T run(Map<String, Object> context, Object... extArgs) {
        //first, we create a matching table with 0 or 1
        T result = evalResult(createMatchTable(context));
        if (result == null && ENV.isModeStrict()) {
            throw new IllegalArgumentException("no value of given context matches any rule in decision table!\n\tcontext: " + context + "\n\tdecisiontable:\n\t" + par);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    byte[][] createMatchTable(Map<String, Object> args) {
        byte[][] mt = createEmptyMatchTable();
        int k = 0;
        List<Condition<?>> conditions;
        Comparable value;
        for (String name : par.keySet()) {
            conditions = par.get(name);
            value = (Comparable) args.get(name);
            for (int i = 0; i < conditions.size(); i++) {
                mt[k][i] = (byte) (conditions.get(i).isTrue(value) ? 1 : 0);
            }
            k++;
        }
        return mt;
    }

    /**
     * the first vector that is filled with 1 will be used as matching index for the result vector.
     * 
     * @param mt matching table
     * @return object from result collection
     */
    private T evalResult(byte[][] mt) {
        return evalResult(mt, true);
    }

    private T evalResult(byte[][] mt, boolean stopOnFirst) {
        boolean matched = false;
        for (int i = 0; i < mt[0].length; i++) {
            if (matches(mt, i)) {
                if (stopOnFirst)
                    return getResultVector().get(i);
                else {
                    if (matched)
                        throw new IllegalStateException(this + " is inconsistent at parameter index" + i);
                }
                matched = true;
            }
        }
        return null;
    }

    private boolean matches(byte[][] ba, int i) {
        for (int k = 0; k < ba.length; k++) {
            if (ba[k][i] != 1)
                return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<T> getResultVector() {
        Object[] resultVector = (Object[]) properties.get(DecisionTableInterpreter.KEY_RESULT);
        return (List<T>) (resultVector != null ? (List<T>) Arrays.asList(resultVector) : new LinkedList<>());
    }

    private byte[][] createEmptyMatchTable() {
        if (matchtable == null)
            matchtable = new byte[par.keySet().size()][getResultVector().size()];
        for (int i = 0; i < matchtable.length; i++) {
            for (int j = 0; j < matchtable[i].length; j++) {
                matchtable[i][j] = 0;
            }
        }
        return matchtable;
    }

    /**
     * transforms the current decision table to a tree. each odd level holds the parameter names, followed by its
     * conditions (the values of the table) in the next (even) level. So, the tree nodes are strings or conditions.
     * 
     * @return transformed decision table
     */
    //UNTESTED!   
    @SuppressWarnings("unchecked")
    public STree toTree() {
        Set<String> keys = par.keySet();
        STree tree = null, parent = null;
        for (String k : keys) {
            /*
             * create new tree node(s) and append all conditions to the new node(s).
             * the child map holds a set of keys, so identical keys will be removed.
             */
            if (parent == null)
                tree = new STree(k, parent, par.get(k).toArray());
            else {
                Collection<Tree> children = parent.values();
                for (Tree child : children) {
                    child.put(k.hashCode(), new STree(k, child, par.get(k)));
                }
            }
            parent = tree;
        }
        return (STree) tree.getRoot();
    }

//UNTESTED!   
    @SuppressWarnings("unchecked")
    public static <T> RuleDecisionTable<T> fromTree(STree<T> tree) {
        final Map<String, List<Condition>> par = new HashMap<String, List<Condition>>();
        HashMap<String, Object> properties = new HashMap<String, Object>();

        tree.transformTree(new ITransformer<STree<T>, STree<T>>() {
            @Override
            public STree<T> transform(STree<T> t) {
                if (t.getNode() instanceof String) {
                    Collection<Condition> current = par.get(t.getNode());
                    par.put(
                        (String) t.getNode(),
                        (List<Condition>) (current == null ? t.getChildren() : current
                            .addAll((Collection<? extends Condition>) t.getChildren())));
                } else {
                    //do nothing
                }
                return t;
            }
        });
        return new RuleDecisionTable(properties, par);
    }

    public static RuleDecisionTable fromCSV(String fileName) {
        return new DecisionTableInterpreter().scan(fileName, "\t");
    }

    @Override
    public String getOperation() {
        //don't load through super method
        return operation;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    @Commit
    protected void initDeserializing() {
        RuleDecisionTable fromCSV = fromCSV(getOperation());
        par = fromCSV.par;
        properties = fromCSV.properties;
        matchtable = fromCSV.matchtable;
        super.initDeserializing();
    }

    @Override
    public String toString() {
        return Util.toString(this.getClass(), "name=" + properties.get("name"));
    }
}

class DecisionTableInterpreter {
    /** keyword to start the decision table */
    static final String KEY_MATRIX = "matrix";
    /** keyword for the end of the parameter matrix. the property map will hold the expected results on this key. */
    static final String KEY_RESULT = "result";

    RuleDecisionTable<?> scan(String csv, String delimiter) {
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, List<Condition<?>>> par = new LinkedHashMap<String, List<Condition<?>>>();
        Scanner sc = null;
        try {
            sc = new Scanner(FileUtil.userDirFile(csv));
            sc.useDelimiter(delimiter);
            String key, lastKey = null, values[] = null;
            boolean withinMatrix = false;
            String line;
            while (sc.hasNextLine()) {
                line = sc.nextLine();
                key = StringUtil.substring(line, null, delimiter).toLowerCase();

                values = StringUtil.substring(line, delimiter, null).split(delimiter);
                //e.g. on a description there are more than one line...
                if (Util.isEmpty(key)) {
                    if (lastKey == null)
                        continue;
                    key = withinMatrix ? KEY_RESULT : lastKey;
                    withinMatrix = false;
                    values = CollectionUtil.concat((String[]) properties.get(key), values);
                } else if (!withinMatrix) {
                    withinMatrix = KEY_MATRIX.equals(key);
                }
                if (withinMatrix && !KEY_MATRIX.equals(key))
                    par.put(key, interpret(values));
                else
                    properties.put(key, values);
                lastKey = key;
            }
            //no KEY_RESULT found --> use the last line as result line.
            if (!properties.containsKey(KEY_RESULT)) {
                par.remove(lastKey);
                properties.put(KEY_RESULT, values);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (sc != null)
                sc.close();
        }
        if (properties.get("name") == null) {
            String name = StringUtil.substring(csv, "/", ".", true);
            properties.put("name", name);
            properties.put("operation", csv);
        }
        return createRule(properties, par);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private RuleDecisionTable createRule(Map<String, Object> properties, Map<String, List<Condition<?>>> par) {
        return new RuleDecisionTable(properties, par);
    }

    /**
     * extracts parameter referenced in conditions
     * 
     * @param conditions values of a csv-line
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<Condition<?>> interpret(String[] conditions) {
        final String OPEXP = "[^\\w\\s.,;]{1,2}";
        ArrayList<Condition<?>> conds = new ArrayList<Condition<?>>();
        String op, c;
        for (int i = 0; i < conditions.length; i++) {
            op = StringUtil.extract(conditions[i], OPEXP);
            if (Util.isEmpty(op))
                op = "=";
            c = StringUtil.substring(conditions[i], op, null);
            conds.add(new Condition(op, c));
        }
        return conds;
    }
}

class Condition<T extends Comparable<T>> {
    String op;
    T operand2;

    Condition(String op, T operand) {
        super();
        this.op = op;
        this.operand2 = operand;
    }

    public boolean isTrue(Comparable<T> comparable) {
        int c = comparable != null ? comparable.compareTo(operand2) : -1;
        return op.equals("=") ? c == 0 : op.equals("<") ? c < 0 : op.equals("<=") ? c <= 0 : op.equals(">") ? c > 0
            : op.equals(">=") ? c >= 0 : op.equals("!=") ? c != 0 : false;
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), op, operand2);
    }
}

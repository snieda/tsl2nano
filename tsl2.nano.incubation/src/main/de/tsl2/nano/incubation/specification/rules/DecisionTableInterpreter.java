package de.tsl2.nano.incubation.specification.rules;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

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
 * Beschreibung;"Rezeptfehler ""Betragshorror"" bei �302: Rezept kann ohne �nderung der Daten nicht abgerechnet werden.";;;;;;;
 * ;"Rezeptfehler ""Betragshorror"" bei �300, Rechnungstyp Arzneimittel oder Hilfsmittel: Rezept kann ohne �nderung der Daten nicht abgerechnet werden.";;;;;;;
 * ;"Rezeptfehler ""Betragshorror"" bei �300, Rechnungstyp Pflegehilfsmittel: Rezept kann ohne �nderung der Daten abgerechnet werden.";;;;;;;
 * ;"*Rezeptfehler ""Betragshorror"" bei �300 Pflegehilfsmittel Irrl�ufer: Rezept kann ohne �nderung der Daten abgerechnet werden.";;;;;;;
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
public class DecisionTableInterpreter {
    /** keyword to start the decision table */
    static final String KEY_MATRIX = "matrix";
    /** keyword for the end of the parameter matrix. the property map will hold the expected results on this key. */
    static final String KEY_RESULT = "result";

    Object scan(String csv, String delimiter) {
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, List<Condition<?>>> par = new LinkedHashMap<String, List<Condition<?>>>();
        Scanner sc = null;
        try {
            sc = new Scanner(new File(csv));
            sc.useDelimiter(delimiter);
            String key, lastKey = null, values[];
            boolean withinMatrix = false;
            for (String line = sc.nextLine(); sc.hasNext();) {
                key = StringUtil.substring(line, null, delimiter).toLowerCase();

                values = StringUtil.substring(line, delimiter, null).split(delimiter);
                //e.g. on a description there are more than one line...
                if (Util.isEmpty(key)) {
                    if (lastKey == null)
                        continue;
                    key = withinMatrix ? KEY_RESULT : lastKey;
                    withinMatrix = false;
                    values = CollectionUtil.concat((String[])properties.get(key), values);
                } else if (!withinMatrix) {
                    withinMatrix = KEY_MATRIX.equals(key);
                }
                if (withinMatrix)
                    par.put(key, interpret(values));
                else
                    properties.put(key, values);
                lastKey = key;
            }
            //no KEY_RESULT found --> use the last line as result line.
            if (!properties.containsKey(KEY_RESULT)) {
                values = (String[]) properties.remove(lastKey);
                properties.put(KEY_RESULT, values);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (sc != null)
                sc.close();
        }
        return createRule(properties, par);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object createRule(Map<String, Object> properties, Map<String, List<Condition<?>>> par) {
        return new DTRule(properties, par);
    }

    /**
     * extracts parameter referenced in conditions
     * 
     * @param conditions values of a csv-line
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<Condition<?>> interpret(String[] conditions) {
        final String OPEXP = "[^\\w]{1, 2}";
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
        int c = comparable.compareTo(operand2);
        return op.equals("=") ? c == 0 : op.equals("<") ? c < 0 : op.equals("<=") ? c <= 0 : op.equals(">") ? c > 0
            : op.equals(">=") ? c >= 0 : op.equals("!=") ? c != 0 : false;
    }
}

@SuppressWarnings("rawtypes")
class DTRule<T> {
    /** contains the name, description and result vector - and additional variables */
    Map<String, Object> properties;
    /** the content of the decision table */
    Map<String, List<Condition<?>>> par;
    /**
     * caclulated table on all conditions compared to the given arguments. will be hold in memory for performance issues
     */
    byte[][] matchtable;

    DTRule(Map<String, Object> properties, Map<String, List<Condition<?>>> par) {
        super();
        this.properties = properties;
        this.par = par;
        checkConsistence(properties, par);
    }

    /**
     * check for overlapping conditions
     * 
     * @param properties
     * @param par
     */
    private void checkConsistence(Map<String, Object> properties, Map<String, List<Condition<?>>> par) {
        Map<String, Object> args = new HashMap<String, Object>();
        for (String name : par.keySet()) {
            for (Condition c : par.get(name)) {
                //TODO: check all combinations...
                args.put(name, c.operand2);
                byte[][] mt = createMatchTable(args);
                //check for more than one result
                evalResult(mt, false);
            }
        }
    }

    public Object run(Map<String, Object> args) {
        //first, we create a matching table with 0 or 1
        return evalResult(createMatchTable(args));
    }

    @SuppressWarnings("unchecked")
    byte[][] createMatchTable(Map<String, Object> args) {
        byte[][] mt = createEmptyMatchTable();
        int k = 0;
        for (String name : par.keySet()) {
            List<Condition<?>> conditions = par.get(name);
            for (int i = 0; i < conditions.size(); i++) {
                mt[k++][i] = (byte) (conditions.get(i).isTrue((Comparable) args.get(name)) ? 1 : 0);
            }
        }
        return mt;
    }

    /**
     * the first vector that is filled with 1 will be used as matching index for the result vector.
     * 
     * @param mt matching table
     * @return object from result collection
     */
    private Object evalResult(byte[][] mt) {
        return evalResult(mt, true);
    }

    private Object evalResult(byte[][] mt, boolean stopOnFirst) {
        boolean matched = false;
        for (int i = 0; i < mt.length; i++) {
            if (matches(mt[i])) {
                if (stopOnFirst)
                    return getResultVector().get(i);
                else {
                    if (matched)
                        throw new IllegalStateException(this + " is inconsistent at parameter index" + i);
                }
            }
        }
        return null;
    }

    private boolean matches(byte[] ba) {
        for (int i = 0; i < ba.length; i++) {
            if (ba[i] != 1)
                return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<T> getResultVector() {
        return (List<T>) properties.get(DecisionTableInterpreter.KEY_RESULT);
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

    @Override
    public String toString() {
        return Util.toString(this.getClass(), "name=" + properties.get("name"));
    }
}
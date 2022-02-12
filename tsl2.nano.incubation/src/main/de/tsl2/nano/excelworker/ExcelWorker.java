package de.tsl2.nano.excelworker;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.script.PersistenceTool;

/**
 * Reads data lines from a given flat or CSV file to give each line to a worker
 * executing an action with parameters (given in line). The Excel/CSV sheet
 * has to have the following structure:
 * 
 * <pre>
 * 1. line: Title or Action definition line
 * 2. line: data headers
 * n. line: if exactly one column or starting with BLOCK: name of a block combining the following data lines
 *          else a data line with columns:
 *          	1. column: row identifier (has to match property "tsl2nano.excelworker.id.match"="\\d+")
 *          	following columns in any order, but:
 *          	if column-name: 'ACTION' -> name of action to be executed by the worker
 *          	if column-name starts with 'PAR' -> action parameter (given as type Object, on multiple: type Object[]) 
 *          	if column-value: 'ACTIONDEF' -> the following column value in line will be used as action definition:
 *          		if 'SQL' -> next column value is an sql statement to be used as action with query parameters. the 
 *                              connection is done through a persistence context (unit-name: EXCELWORKER) that has to
 *                              be in path 'META-INF/persistence.xml'
 *          		if 'CLS' -> full class name implementing the actions to be excecuted by the worker
 *          		if 'URL' -> url with placeholders to be executed
 *          		if 'CMD' -> system call with arguments
 *          		if 'PRN' -> (default) do nothing but print output to console
 * 
 * The expression in an ACTIONDEF cell (e.g.: !:URL:https://myserver.de/%1$s/show.html:!) may be  a formatted expression with placeholders like %s.
 * The placeholders are filled through the action parameters: {id, all-values-with-columnheaders-starting-with-PAR:}. The formatter is 
 * defined here: https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html
 * 
 * The action name will always converted to lowercase!
 * 
 * To execute the ExcelWorker the sheet has to be exported from Excel to a csv or any other delimited (default: \t) file.
 * You are able to configure some properties:
 * 
 * * tsl2nano.excelworker.tag.block="BLOCK"
 * * tsl2nano.excelworker.delimiter="\t"
 * * tsl2nano.excelworker.persistenceunit="EXCELWORKER"
 * * tsl2nano.excelworker.id.match"="\\d+"
 * * tsl2nano.excelworker.swallowtabs="false"
 * * tsl2nano.excelworker.dryrun="false"
 * * tsl2nano.excelworker.blocks.parallel="false"
 * 
 * start it with: ExcelWorker my-delimited-file.csv
 * </pre>
 * 
 * @author Thomas Schneider
 */
public class ExcelWorker implements Runnable {
	String file;
	private static final String BLOCK = System.getProperty("tsl2nano.excelworker.tag.block", "BLOCK");
	private static final String ACTION = System.getProperty("tsl2nano.excelworker.tag.action", "ACTION");
	private static final String PAR = System.getProperty("tsl2nano.excelworker.tag.par", "PAR:");
	private static final String ACTIONDEF = System.getProperty("tsl2nano.excelworker.tag.actiondef", "!:");
	private static final String ACTIONDEFEND = System.getProperty("tsl2nano.excelworker.tag.actiondef", ":!");
	private static final String DELIMITER = System.getProperty("tsl2nano.excelworker.delimiter", "\t");
	private static final String ID_MATCH = System.getProperty("tsl2nano.excelworker.id.match", "\\d+");
	private static final int ID_INDEX = Util.trY( () -> Integer.valueOf(System.getProperty("tsl2nano.excelworker.id.index", "0")));
	private static final Boolean SWALLOWTABS = Boolean.getBoolean("tsl2nano.excelworker.swallowtabs");
	private static final Boolean PARALLEL = Boolean.getBoolean("tsl2nano.excelworker.blocks.parallel");

	public ExcelWorker(String file) {
		this.file = file;
	}

	@Override
	public void run() {
		Collection<Object[]> blocks = Util.trY( () -> generateFromCSV(file));
		for (Object[] b : blocks) {
			runBlock(b);
		}
	}

	private void runBlock(Object[] b) {
		log("RUNNING: " + b[0]);
		if (PARALLEL)
			((Collection<Worker>)b[1]).parallelStream().forEach(w -> w.run());
		else
			((Collection<Worker>)b[1]).forEach(w -> w.run());
	}

	public static Collection<Object[]> generateFromCSV(String file) throws FileNotFoundException {
		log("======================================================");
		log("LOADING DATA  : " + file + "...");
		try (Scanner sc = new Scanner(new File(file))) {
			LinkedList<Object[]> alltests = new LinkedList<>();
			LinkedList<Worker> worker = new LinkedList<>();
			String title, header[], l, name = "", actionname;
			Action action;
			String[] args;
			int i = 0;
			int iaction = 1, ipars[] = new int[] {2};
			int bloecke = 0;
			String id;
			Map<String, Action> actions = new HashMap<>();
			if (sc.hasNextLine()) {
				title = sc.nextLine();
				findAndPutActionDef(title, actions);
			}
			if (sc.hasNextLine()) {
				header = sc.nextLine().split(DELIMITER);
				iaction = getIndexes(header, ACTION)[0];
				ipars = getIndexes(header, PAR);
			}
			log("\tCOLUMN-INDEXES: id=" + ID_INDEX + ", action=" + iaction + ", parameters=" + Arrays.toString(ipars));
			if (Boolean.getBoolean(Worker.DRYRUN))
				log("DRYRUN ACTIVATED ==> DOING ONLY CHECKS");
			log("======================================================\n");
			while (sc.hasNextLine()) {
				l = sc.nextLine();
				findAndPutActionDef(l, actions);
				i++;
				args = l.split(DELIMITER + (SWALLOWTABS ? "+": ""));
				if (args.length == 1 || args[0].startsWith(BLOCK)) {
					if (args[0].startsWith(BLOCK) || (worker.isEmpty() && name != null)) {
						if (!worker.isEmpty()) {
							alltests.add(new Object[] { name, worker });
							name = "";
							worker = new LinkedList<>();
						}
						name += " " + BLOCK + " " + ++bloecke;
						log("==> BEGIN: " + name);
					} else {
						name = args[0].toString();
					}
				} else if ((id = args[ID_INDEX]).matches(ID_MATCH)) {
					System.out.print(i + ": ");
					actionname = Worker.getActionName(args[iaction]);
					if (!Util.isEmpty(actionname)) {
						action = actions.get(actionname);
						worker.add(new Worker(action.type, action.expression, id, actionname,
								Worker.getActionParameter(id, args, ipars)));
					} else {
						log("IGNORING LINE: " + i);
					}
				}
			}
			alltests.add(new Object[] { name, worker });
			log("======================================================\n");
			System.out.println(alltests.size() + " BLOCKS WITH WORKERS CREATED (" + i + " LINES READ)\n\n");
			return alltests;
		}
	}

	private static int[] getIndexes(String[] header, String tag) {
		List<Integer> list = new LinkedList<>();
		for (int i = 0; i < header.length; i++) {
			if (header[i].equals(tag) || (tag.endsWith(":") && header[i].startsWith(tag)))
				list.add(i);
		}
		int[] indexes = new int[list.size()];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = list.get(i);
		}
		return indexes;
	}

	private static void findAndPutActionDef(String txt, Map<String, Action> actions) {
		if (txt.contains(ACTIONDEF)) {
			String name = StringUtil.substring(txt, ACTIONDEF, ":");
			ActionType actionType = ActionType.valueOf(StringUtil.substring(txt, name + ":", ":"));
			String expression = StringUtil.substring(txt, actionType + ":", ACTIONDEFEND);
			log("\tNEW ACTIONDEF : " + name + ":" + actionType + ":" + expression);
			actions.put(Worker.getActionName(name), new Action(actionType, expression));
		}
	}

	static void log(Object msg, Object... args) {
		System.out.println(args.length > 0 ? String.format(msg.toString(), args) : msg.toString());
	}

	public static void main(String[] args) {
		Util.trY(() -> new ExcelWorker(args[0]).run());
	}
}
enum ActionType { PRN, CLS, CMD, URL, SQL};
class Action {
	ActionType type; String expression;	
	Action(ActionType actionType, String expression) {
		this.type = actionType;
		this.expression = expression;
	}
}
class Worker implements Runnable {
	static final String DRYRUN = "tsl2nano.excelworker.dryrun";
	static final String PERSISTENCEUNIT = System.getProperty("tsl2nano.excelworker.persistenceunit", ExcelWorker.class.getSimpleName().toUpperCase());
	ActionType type;
	String expression;
	String id;
	String action;
	Object[] actionParameter;
	PersistenceTool p;

	public Worker(ActionType type, String expression, String id, String action, Object... actionParameter) {
		this.type = type;
		this.expression = expression;
		this.id = id;
		this.action = action;
		this.actionParameter = actionParameter;
		ExcelWorker.log(toString());
	}

	static String getActionName(String descriptor) {
		return descriptor.toLowerCase().trim();
	}

	static Object[] getActionParameter(String id, String args[], int[] ipars) {
		List<String> pars = new LinkedList<>();
		pars.add(id);
		for (int i = 0; i < ipars.length; i++) {
			if (args.length > ipars[i])
				pars.add(args[ipars[i]].trim());
		}
		return pars.toArray();
	}

	Worker with(PersistenceTool p) {
		this.p = p;
		return this;
	}

	@Override
	public void run() {
		String cmd = String.format(expression, actionParameter);
		ExcelWorker.log("STARTING " + this + " [%s:%s]", type, cmd);
		Object result = null;
		try {
			switch(type) {
			case PRN: ExcelWorker.log(this); break;
			case CLS: result = runClass(cmd); break;
			case SQL: result = runSQL(cmd); break;
			case URL: result = runURL(cmd); break;
			case CMD: result = runCMD(cmd); break;
			}
			ExcelWorker.log("\t<= RESULT: " + result);
		} catch (Exception ex) {
			if (Boolean.getBoolean(DRYRUN))
				ExcelWorker.log("ERROR: " + ex.toString());
		}
	}

	private Object runCMD(String cmd) {
		if (!Boolean.getBoolean(DRYRUN))
			return SystemUtil.execute(CollectionUtil.concatNew(new String[1 + actionParameter.length], new String[] {cmd}, actionParameter));
		else
			if (!new File(expression).canExecute())
				throw new IllegalArgumentException("NOT EXECUTABLE: " + cmd);
		return "<-- DRYRUN";
	}

	private Object runURL(String cmd) {
		if (!Boolean.getBoolean(DRYRUN))
			return NetUtil.getRest(cmd);
		return "<-- DRYRUN";
	}

	private Object runSQL(String cmd) {
		PersistenceTool pt = new PersistenceTool(PERSISTENCEUNIT);
		if (!Boolean.getBoolean(DRYRUN))
			return pt.execute(cmd, actionParameter);
		return "<-- DRYRUN";
	}

	private Object runClass(String cmd) {
		try {
			Method method = Thread.currentThread().getContextClassLoader().loadClass(cmd).getMethod(action, new Class[] {Object[].class});
			if (!Boolean.getBoolean(DRYRUN))
				return method.invoke(null, new Object[] {actionParameter});
			return "<-- DRYRUN";
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return String.format("%s => %s(%s)", id, action, Arrays.toString(actionParameter));
	}
}

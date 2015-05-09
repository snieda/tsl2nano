/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import static de.tsl2.nano.incubation.terminal.TextTerminal.BLOCK_BAR;
import static de.tsl2.nano.incubation.terminal.TextTerminal.SCREEN_HEIGHT;
import static de.tsl2.nano.incubation.terminal.TextTerminal.SCREEN_WIDTH;
import static de.tsl2.nano.incubation.terminal.TextTerminal.getTextFrame;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.Finished;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.classloader.NetworkClassLoader;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.XmlUtil;
import de.tsl2.nano.incubation.terminal.IItem.Type;
import de.tsl2.nano.incubation.terminal.item.Container;
import de.tsl2.nano.util.SchedulerUtil;

/**
 * Terminal showing a textual manual. The configuration can be read through an xml file (standard name:
 * {@link #DEFAULT_NAME}). For further informations, see {@link IItem}.
 * 
 * <pre>
 * Features:
 * - simple input and output on small screens
 * - input constraints check
 * - configuration over xml
 * - result will be written to a property map
 * - tree nodes can be selected through numbers or names
 * - batch mode possible
 * - macro recording and replaying
 * - simplified java method calls
 * - variable output sizes and styles
 * - workflow conditions: items are active if an optional condition is true
 * - if an item container (a tree) has only one visible item (perhaps filtered through conditions), it delegates directly to that item
 * - actions get the entire environment properties (including system properties) on calling run().
 * - sequential mode: if true, all tree items will be asked for in a sequential mode.
 * - show ascii-pictures (transformed from pixel-images) for an item (description must point to an image file)
 * - extends itself downloading required jars from network (if {@link #useNetworkExtension} ist true)
 * - schedule mode: starts a scheduler for an action
 * - selectors for files, content of files, class members etc.
 * 
 * </pre>
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Terminal implements IItemHandler, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -5767124822662015899L;
    transient private static final Log LOG;
    static {
        LogFactory.setPrintToConsole(false);
        LOG = LogFactory.getLog(Terminal.class);
    }
    /** used as file name */
    @Attribute
    String name = DEFAULT_NAME;
    /** utility to read user input */
    transient Scanner input;
    transient InputStream in;
    transient PrintStream out;
    @Attribute
    int width = SCREEN_WIDTH;
    @Attribute
    int height = SCREEN_HEIGHT;
    @Attribute
    int style = BLOCK_BAR;
    @Attribute
    boolean bars = true;
    /** item properties */
    transient Properties env;
    /** default: false. if true, on each terminal save, the terminals xml serialization file will be stored.  */
    @Attribute(required=false)
    boolean refreshConfig = false;
    
    /**
     * predefined variables (not changable through user input) copied to the {@link #env} but not saved in property
     * file. mostly technical definitions.
     */
    @ElementMap(entry = "definition", attribute = true, inline = true, keyType = String.class, key = "name", required = false, value = "value", valueType = Object.class)
    Map<String, Object> definitions;

    /** batch file name. the batch file contains input instructions (numbers or names) separated by '\n'. */
    @Element(required=false)
    String batch;

    /** base item - should be a Selection */
    @Element
    IItem root;

    /** useNetworkExtension */
    @Attribute
    boolean useNetworkExtension = true;

    /**
     * true, if macro recording was started through {@link #KEY_MACRO_RECORD} and not yet stoped with
     * {@link #KEY_MACRO_STOP}
     */
    transient boolean isRecording;

    /** if true, all tree items will be accessed directly and sequentially */
    @Attribute(required=false)
    boolean sequential = false;

    /** command identifier */
    static final String KEY_COMMAND = ":";
    /*
     * available commands
     */
    static final String KEY_HELP = "help";
    /** prints system informations */
    static final String KEY_INFO = "info";
    /** prints all system properties */
    static final String KEY_PROPERTIES = "properties";
    /** starts macro recording. user input will be stored to {@link #batch} and saved on terminal end. */
    static final String KEY_MACRO_RECORD = "record";
    /** stops macro recording */
    static final String KEY_MACRO_STOP = "stop";

    /** starts a scheduler for the given action */
    static final String KEY_SCHEDULE = "schedule";

    public static final String KEY_SEQUENTIAL0 = "sequential";

    static final String KEY_USENETWORKEXTENSION = "network";

    /** saves the current state to xml and property files */
    static final String KEY_SAVE = "save";
    /** quits the terminal */
    static final String KEY_QUIT = "quit";

    public static final String PREFIX = "terminal.";
    public static final String KEY_NAME = PREFIX + "name";
    public static final String KEY_WIDTH = PREFIX + "width";
    public static final String KEY_HEIGHT = PREFIX + "height";

    public static final String KEY_SEQUENTIAL = PREFIX + "sequential";

    /** default script file name */
    public static final String DEFAULT_NAME = PREFIX + "xml";

    static final String ASK_ENTER = ">>> PLEASE HIT ENTER FOR THE NEXT PAGE OR ENTER A SELECTION <<<";
    private static final String LOGO = "tsl2nano.logo.png";

    public Terminal() {
        initDeserialization();
    }

    public Terminal(IItem root) {
        this(root, System.in, System.out);
    }

    public Terminal(IItem root, InputStream in, PrintStream out) {
        this(root, in, out, SCREEN_WIDTH, SCREEN_HEIGHT, BLOCK_BAR, null);
    }

    public Terminal(IItem root, InputStream in, PrintStream out, int width, int height, int style) {
        this(root, in, out, width, height, style, null);
    }

    /**
     * constructor
     * 
     * @param root
     * @param input
     * @param in
     * @param out
     */
    public Terminal(IItem root,
            InputStream in,
            PrintStream out,
            int width,
            int height,
            int style,
            Map<String, Object> defintions) {
        super();
        this.root = root;
        this.in = in;
        this.out = out;
        this.width = width;
        this.height = height;
        this.style = style;
        this.definitions = defintions;
        this.env = createEnvironment(name, defintions);
    }

    static Properties createEnvironment(String name, Map definitions) {
        String p = name + ".properties";
        Properties env = new File(p).canRead() ? FileUtil.loadPropertiesFromFile(p) : new Properties();
        if (definitions != null) {
            env.putAll(definitions);
        }
        return env;
    }

    public static Terminal create(String file) {
        Terminal t = XmlUtil.loadXml(file, Terminal.class);
        t.name = file;
        return t;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            LOG.info("starting terminal " + name);
            input = new Scanner(in);
            prepareEnvironment(env, root);
//            //welcome screen
            if (!isInBatchMode()) {
                printAsciiImage(LOGO, new PrintWriter(out), width, height, true, bars);
                nextLine(in);
            }
            //if only one tree-item available, go to that item
            if (root instanceof Container) {
                root = ((Container) root).delegateToUniqueChild(root, in, out, env);
            }
            serve(root, in, out, env);
            shutdown();
        } catch (Finished ex) {
            shutdown();
        } catch (Exception ex) {
            ManagedException.forward(ex);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    public static void printAsciiImage(String name,
            PrintWriter out,
            int width,
            int height,
            boolean resource,
            boolean bars) {
        try {
            if (resource) {
                new AsciiImage(bars ? AsciiImage.BARS : AsciiImage.CHARS, AsciiImage.RGB).convertToAscii(
                    ImageIO.read(FileUtil.getResource(name)), out, width, height).flush();
            } else {
                new AsciiImage().convertToAscii(name, out, width, height).flush();
            }
        } catch (Exception e) {
            //it's only a logo, no problem (perhaps on android)
            LOG.error(e.toString());
        }
    }

    /**
     * see {@link #definitions}
     * 
     * @return Returns the definitions.
     */
    public Map<String, Object> getDefinitions() {
        return definitions;
    }

    /**
     * see {@link #definitions}
     * 
     * @param definitions The definitions to set.
     */
    public void setDefinitions(Map<String, Object> definitions) {
        this.definitions = definitions;
    }

    protected void shutdown() {
        save();
        String shutdownInfo =
            "\n|\n|\nSHUTDOWN TERMINAL!\n|\nsaved changes to\n" + name + "\nand\n " + name + ".properties";
        printScreen(shutdownInfo, out, null, true);
        LOG.info("terminal " + name + " ended");
    }

    protected void save() {
        if (refreshConfig || !new File(name).exists()) {
            XmlUtil.saveXml(name, this);
        }
        Set<Object> keys = env.keySet();
        //replace objects through their toString()
        Properties envCopy = new Properties();
        for (Object k : keys) {
            //pre defined variables are not content of item properties
            if (definitions != null && definitions.containsKey(k)) {
                continue;
            }
            Object v = env.get(k);
            envCopy.put(k, v instanceof String ? v : StringUtil.toString(v, -1));
        }
        FileUtil.saveProperties(name + ".properties", envCopy);
    }

    private void prepareEnvironment(Properties env, IItem root) {
        if (root.getType() == Type.Option) {
            return;
        }
        Object value = root.getValue();
        if (value != null) {
            env.put(root.getName(), value);
        }
        if (root.getType() == Type.Selector) {
            return;
        }
        if (root.getType().equals(Type.Container)) {
            List<IItem> childs = ((IContainer) root).getNodes(env);
            if (childs != null) {
                for (IItem c : childs) {
                    prepareEnvironment(env, c);
                }
            }
        }
        if (useNetworkExtension) {
            String classpath = System.getProperty("user.dir") + "/lib";
            new File(classpath).mkdirs();
            NetworkClassLoader.createAndRegister(classpath);
        }
        //to be accessible for actions
        System.setProperty(KEY_NAME, name);
        System.getProperties().put(KEY_WIDTH, width);
        System.getProperties().put(KEY_HEIGHT, height);
        System.getProperties().put(KEY_SEQUENTIAL, sequential);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getUserInterface() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String printScreen(IItem item, PrintStream out) {
        out.print(TextTerminal.getTextFrame(item.toString(), style, width, true));
        String question = item.ask(env);
        return printScreen(
            item.getDescription(env, false),
            out, question, false);
    }

    /**
     * {@inheritDoc}
     */
    public String printScreen(String screen, PrintStream out, String question, boolean center) {
        //split screens to max height
        String pagingInput;
        String s = screen;
        int lines = 0, page = 0, i = 0, l = -1;
        while ((i = s.indexOf("\n", l + 1)) < s.length() && i != -1) {
            if (i - l > width - 2) {
                i = l + width - 2;
            }
            if (++lines > height) {
                out.print(getTextFrame(s.substring(page, i), style, width, center));
                out.print(ASK_ENTER);
                page = i + 1;
                lines = 0;
                if (!isInBatchMode()) {
                    if ((pagingInput = nextLine(in)).length() > 0) {
                        return pagingInput;
                    }
                } else {
                    out.println();
                }
            }
            l = i;
        }
        //print the rest
        if (lines > 1 && lines <= height) {
            screen = s.substring(page, s.length());
            screen += StringUtil.fixString(height - lines, ' ').replace(" ", " \n");
            out.print(getTextFrame(screen, style, width, center));
        }
        out.print(question);
        return null;
    }

    public static String translate(String name) {
        return StringUtil.toFirstUpper(Messages.getStringOpt(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serve(IItem item, InputStream in, PrintStream out, Properties env) {
        try {
            String input = printScreen(item, out);
            if (input == null) {
                input = nextLine(in);
            }
            //to see the input in batch mode
            if (!Util.isEmpty(input) && input.startsWith(KEY_COMMAND)) {
                if (isCommand(input, KEY_HELP)) {
                    printScreen(getHelp(), out, ASK_ENTER, false);
                    nextLine(in);
                    printScreen(item.getDescription(env, true), out, "", false);
                } else if (isCommand(input, KEY_PROPERTIES)) {
                    System.getProperties().list(out);
                } else if (isCommand(input, KEY_INFO)) {
                    printScreen(ENV.createInfo(), out, "", false);
                } else if (isCommand(input, KEY_MACRO_RECORD)) {
                    isRecording = true;
                } else if (isCommand(input, KEY_MACRO_STOP)) {
                    isRecording = false;
                } else if (isCommand(input, KEY_SCHEDULE)) {
                    schedule(item, input, in, out, env);
                } else if (isCommand(input, KEY_SEQUENTIAL0)) {
                    sequential = !sequential;
                    prepareEnvironment(env, root);
                } else if (isCommand(input, KEY_USENETWORKEXTENSION)) {
                    useNetworkExtension = !useNetworkExtension;
                    prepareEnvironment(env, root);
                } else if (isCommand(input, KEY_SAVE)) {
                    save();
                } else if (isCommand(input, KEY_QUIT)) {
                    throw new Finished("terminal stopped");
                } else {
                    throw new IllegalArgumentException(input + " is not a known command!");
                }
                nextLine(in);
                serve(item, in, out, env);
            } else {
                IItem next = item.react(item, input, in, out, env);
                if (next != null) {
                    serve(next, in, out, env);
                }
            }
        } catch (Finished ex) {
            //terminal will be stopped
            throw ex;
        } catch (Exception ex) {
            //print only - no problem
            ex.printStackTrace(out);
            //do it again
            nextLine(in);
            serve(item, in, out, env);
        }
    }

    /**
     * schedule
     * 
     * @param item
     * @param input
     * @param in
     * @param out
     * @param env
     */
    private void schedule(final IItem item,
            final String input,
            InputStream in,
            final PrintStream out,
            final Properties env) {
        out.println("preparing scheduler on parameters (:schedule:command:delay:period:end), input=" + input);
        String[] args = input.split(":");
        int i = 2;
        final IItem action = ((Container) item).getNode(args[i++], env);
        long delay = Integer.valueOf(Util.value(args, i++, "1000"));
        long period = Integer.valueOf(Util.value(args, i++, "1000"));
        long end = Integer.valueOf(Util.value(args, i++, "3600000"));
        TimeUnit unit = TimeUnit.MICROSECONDS;
        out.println("starting scheduler for action " + action + ":\n"
            + "  delay : " + delay + " milliseconds\n"
            + "  period: " + period + " milliseconds\n"
            + "  end   : " + end + " milliseconds\n");
        SchedulerUtil.runAt(delay, period, end, unit, new Runnable() {
            @Override
            public void run() {
                InputStream in0 = createBatchStream("\n");
                action.react(item, input, in0, out, env);
            }
        });
    }

//    private void put(IItem item) {
//        if (item.getValue() != null)
//            env.put(item.getName(), item.getValue());
//        else
//            env.remove(item.getName());
//    }

    boolean isInBatchMode() {
        return isInBatchMode(in);
    }

    static boolean isInBatchMode(InputStream in) {
        return in != System.in;
    }

    /**
     * can be used to use a file as input (no user input possible). call this before you call {@link #run()}. this file
     * should have newline-separated entries that match the terminal structure.
     * 
     * @param batch The batch to set.
     */
    public void setBatch(File batch) {
        this.batch = batch.getPath();
        in = FileUtil.getFile(this.batch);
    }

    public void setBatch(String... cmds) {
        in = createBatchStream(cmds);
    }

    public static InputStream createBatchStream(String... cmds) {
        String concat = StringUtil.concat(new char[] { '\n' }, cmds);
        return ByteUtil.getInputStream(concat.getBytes());
    }

    private boolean isCommand(String input, String cmd) {
        return cmd.startsWith(StringUtil.substring(input.substring(1), null, KEY_COMMAND).toLowerCase());
    }

    /**
     * @return Returns the sequential.
     */
    public boolean isSequential() {
        return sequential;
    }

    /**
     * @param sequential The sequential to set.
     */
    public void setSequential(boolean sequential) {
        this.sequential = sequential;
    }

    
    /**
     * @return Returns the refreshConfig. see {@link #refreshConfig}
     */
    public boolean isRefreshConfig() {
        return refreshConfig;
    }

    /**
     * @param refreshConfig The refreshConfig to set. see {@link #refreshConfig}
     */
    public void setRefreshConfig(boolean refreshConfig) {
        this.refreshConfig = refreshConfig;
    }

    /**
     * nextLine
     * 
     * @param in
     * @return
     */
    private String nextLine(InputStream in) {
        String line = nextLine(input, in, out);
        //TODO: refactore to do the recording inside the base static nextLine(..)
        if (isRecording) {
            batch = batch == null ? line : batch + ", " + line;
        }
        return line;
    }

    public static String nextLine(InputStream in, PrintStream out) {
        return nextLine(new Scanner(in), in, out);
    }

    static String nextLine(Scanner scanner, InputStream in, PrintStream out) {
        String text = scanner.hasNextLine() ? scanner.nextLine() : null;
        if (isInBatchMode(in)) {
            out.println(text);
        }
        return text;
    }

    @Commit
    protected void initDeserialization() {
        env = createEnvironment(name, definitions);
        in = System.in;
        out = System.out;
    }

    public static final String getHelp() {
        return "The Terminal is configured through xml files with only four types of items.\n"
            + " (+) Tree    : holds childs of all types, but normally Options\n"
            + " (*) Input   : user input, has to be terminated with ENTER\n"
            + " (!) Action  : starts a command --> if terminated, the user has to hit ENTER\n"
            + " ( ) Option  : is a simple child of a Tree\n\n"
            + "The items can have the following properties:\n"
            + " x: changed or visited\n"
            + " §: duty (has to be visited)\n\n"
            + "You can leave an item with key ENTER, you can show this help typing ':help',\n"
            + "If you leave the entire menu with ENTER, a property file with the new values\n"
            + "will be written, if you hit Strg+c, the entire menu will be aborted. If you\n"
            + "type ':properties' you will see a list of all property values. :info will show\n"
            + " some system informations. :quit will stop the terminal save the property file.\n"
            + "':record' will record your actions to be saved as batch. ':stop' stops the macro.\n"
            + "To set reset an items value, type 'null' as value\n"
            + "It is possible to define workflow conditions, so items are not visible, if their\n"
            + " condition is negative.\n"
            + "If an item container (a tree) has only one visible item, that item will be activated."
            + "If you turn on 'sequence', the user doesn't have to enter each command number - all"
            + " items of a container will be asked sequentially."
            + "If you input the command :schedule:<item-no>[:delay][:period[:end]]] the item, which"
            + "has to be an action will be scheduled for the given milliseconds."
            + "You can set the mode 'useNetworkExtension' to true, if you want, that the terminal" +
            "downloads required (by action-definitions) jar-files itself";
    }

    public static void main(String[] args) {
//        if (args.length == 0) {
//            System.out.println("Please provide a file name as base for this terminal");
//            return;
//        }
        try {
            String name = args.length > 0 ? args[0] : DEFAULT_NAME;
            boolean admin = args.length > 1 && args[1].equals(TerminalAdmin.ADMIN) ? true : false;
            if (admin || !new File(name).exists()) {
                TerminalAdmin.create(name).run();
            } else {
                create(name).run();
            }
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }
}

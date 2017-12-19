package de.tsl2.nano.incubation.repeat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.Serializable;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.repeat.impl.AChange;
import de.tsl2.nano.incubation.repeat.impl.ACommand;
import de.tsl2.nano.incubation.repeat.impl.CommandManager;
import de.tsl2.nano.util.test.TypeBean;

public class RepeatTest {

    @BeforeClass
    public static void setUp() {
    	ENVTestPreparation.setUp("repeatable", false);
    }

    @AfterClass
    public static void tearDown() {
    	ENVTestPreparation.tearDown();
    }
    
    @Test
    public void testUndoRedo() throws Exception {
        /*
         * tests a simple regxp replacer
         * - item is a regexp
         * - old is the old text, found by regexp
         * - new is the new replaced text
         */
        StringBuilder text = new StringBuilder("A and B are not C");
        Command a = new Command(text, new AChange("(A[0-9]*)", "A", "A1"));
        Command b = new Command(text, new AChange("(B[0-9]*)", "B", "B1"));
        Command c = new Command(text, new AChange("(C[0-9]*)", "C", "C1"));
        undoRedo(a, b, c);
    }

    @Test
    public void testUndoRedoWithEntity() throws Exception {
        EBean tbean = new EBean();
        ECommand a = new ECommand(null, new AChange(null, null, tbean));
        ECommand b = new ECommand(tbean, new AChange("string", null, "astring"));
        ECommand c = new ECommand(tbean, new AChange("immutableInteger", null, 100));
        ECommand d = new ECommand(tbean, new AChange(null, tbean, null));
        undoRedo(b, c);

        //construction and deletion
        CommandManager commandManager = new CommandManager(10);
        assertEquals(a.getContext(), null);
        commandManager.doIt(a);
        assertEquals(a.getContext(), tbean);
        commandManager.doIt(d);
        assertEquals(d.getContext(), null);
        commandManager.undo();
        assertEquals(d.getContext(), tbean);
        commandManager.redo();
        assertEquals(d.getContext(), null);
    }

    public <CONTEXT> void undoRedo(ICommand<CONTEXT>... cmds) throws Exception {
        CommandManager cmdManager = new CommandManager();

        CONTEXT context = cmds[0].getContext();
        CONTEXT origin = BeanUtil.copy(context);
        log("origin : " + origin);

        cmdManager.doIt(cmds);

        CONTEXT changed = BeanUtil.copy(context);
        log("changed: " + changed);

        assertTrue(cmdManager.canUndo());
        assertFalse(cmdManager.canRedo());

        cmdManager.undo();
        cmdManager.undo();
        cmdManager.undo();

        assertFalse(cmdManager.canUndo());
        assertTrue(cmdManager.canRedo());

        log("text   : " + BeanUtil.copy(context));
        assertEquals(origin.toString(), context.toString());

        cmdManager.redo();
        cmdManager.redo();
        cmdManager.redo();

        assertTrue(cmdManager.canUndo());
        assertFalse(cmdManager.canRedo());

        log("re-done: " + context);
        assertEquals(changed.toString(), context.toString());
    }

    @Test
    public void testMacro() throws Exception {
        CommandManager cmdManager = new CommandManager();

        EBean tbean = new EBean();
        ECommand a = new ECommand(null, new AChange(null, null, tbean));
        ECommand b = new ECommand(tbean, new AChange("string", null, "astring"));
        ECommand c = new ECommand(tbean, new AChange("immutableInteger", null, 100));
        ECommand d = new ECommand(tbean, new AChange(null, tbean, null));

        cmdManager.getRecorder().record("test.record");
        cmdManager.doIt(a, b, c, d);
        cmdManager.getRecorder().stop();

        EBean mbean = new EBean();
        //now, redo that with on another context object --> macro replay!
        assertEquals(4, cmdManager.getRecorder().play("test.record", mbean));
        assertEquals(tbean, mbean);
    }

    static void log_(String msg) {
        System.out.print(msg);
    }

    static void log(String msg) {
        System.out.println(msg);
    }
}

/**
 * test command doing a simple text replacing
 */
class Command extends ACommand<StringBuilder> {
    private static final long serialVersionUID = 1L;

    public Command(StringBuilder context, IChange... changes) {
        super(context, changes);
    }

    @Override
    public void runWith(IChange... changes) {
        for (int i = 0; i < changes.length; i++) {
            String item = (String) changes[i].getItem();
            String old = StringUtil.toString(changes[i].getOld());
            String neW = StringUtil.toString(changes[i].getNew());
            StringUtil.extract(getContext(), item != null ? item : old, neW);
        }
    }

}

/**
 * test command doing a simple text replacing
 */
@SuppressWarnings({ "unchecked" })
class ECommand extends ACommand<Serializable> {
    private static final long serialVersionUID = 1L;

    public ECommand(Serializable context, IChange... changes) {
        super(context, changes);
    }

    @Override
    public void runWith(IChange... changes) {
        for (int i = 0; i < changes.length; i++) {
            if (changes[i].getItem() != null) {
                //while the context can change on item=null, we have to do it inside the loop
                Class<?> type = getContext().getClass();
                BeanClass<Serializable> b = (BeanClass<Serializable>) BeanClass.getBeanClass(type);
                b.setValue(getContext(), (String) changes[i].getItem(), changes[i].getNew());
            } else {
                setContext((Serializable) changes[i].getNew());
            }
        }
    }

}

class EBean extends TypeBean {
    /** serialVersionUID */
    private static final long serialVersionUID = 2662724418006121099L;

    @Override
    public boolean equals(Object obj) {
        return BeanUtil.equals(BeanUtil.serialize(this), BeanUtil.serialize(obj));
    }

    @Override
    public String toString() {
        return getString() + getImmutableInteger();//new String(BeanUtil.serialize(this));
    }

}

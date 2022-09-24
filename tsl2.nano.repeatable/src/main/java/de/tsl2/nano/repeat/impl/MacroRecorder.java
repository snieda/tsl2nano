package de.tsl2.nano.repeat.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.repeat.ICommand;
import de.tsl2.nano.repeat.IMacroManager;

/**
 * Simple implementation of {@link IMacroManager}
 * 
 * @param <CONTEXT>
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class MacroRecorder<CONTEXT> implements IMacroManager<CONTEXT> {
    Map<String, Deque<ICommand<?>>> macros;
    String recordingID;

    public MacroRecorder() {
        this(Integer.MAX_VALUE);
    }

    /**
     * constructor
     */
    public MacroRecorder(int capacity) {
        super();
        macros = new HashMap<String, Deque<ICommand<?>>>();
    }

    @Override
    public void record(String id, ICommand<CONTEXT>... m) {
        if (recordingID == null) {
            if (id == null) {
                throw new IllegalStateException(
                    "please call 'record' with a filled 'id' to start the recording process!");
            }
            recordingID = id;
        }
        macro().addAll(copy(m));
    }

    private Collection<? extends ICommand<?>> copy(ICommand<CONTEXT>[] m) {
        List<ICommand<CONTEXT>> copy = new ArrayList<ICommand<CONTEXT>>();
        for (int i = 0; i < m.length; i++) {
            copy.add(BeanUtil.copy(m[i]));
        }
        return copy;
    }

    private Deque<ICommand<?>> macro() {
        return macro(recordingID);
    }

    /**
     * macro
     * 
     * @return the current macro (the last record...if not stopped!)
     */
    private Deque<ICommand<?>> macro(String recordingID) {
        Deque<ICommand<?>> m = macros.get(recordingID);
        if (m == null) {
            m = new LinkedBlockingDeque<ICommand<?>>();
            macros.put(recordingID, m);
        }
        return m;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRecording() {
        return recordingID != null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int play(String id, CONTEXT context) {
        if (isRecording()) {
            throw new IllegalStateException("please stop macro recording first!");
        }
        Deque<ICommand<?>> m = macro(id);
        int size = m.size();
        for (ICommand cmd : m) {
            cmd.setContext(context);
            cmd.run();
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int stop() {
        int size = macro().size();
        recordingID = null;
        return size;
    }
}
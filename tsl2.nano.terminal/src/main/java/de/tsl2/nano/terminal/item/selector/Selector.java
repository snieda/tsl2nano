package de.tsl2.nano.terminal.item.selector;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.terminal.IContainer;
import de.tsl2.nano.terminal.IItem;
import de.tsl2.nano.terminal.item.AItem;
import de.tsl2.nano.terminal.item.Container;
import de.tsl2.nano.terminal.item.Option;

/**
 * Selector is a Container, holding only {@link Option}s, evaluated on runtime.
 * 
 * @param <T>
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class Selector<T> extends Container<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public Selector() {
        super();
    }

    public Selector(String name, String description) {
        super(name, description);
    }

    public Selector(String name, T value, String description) {
        super(name, description);
        this.value = value;
    }

    @Override
    public List<AItem<T>> getNodes(Map context) {
        if (nodes == null || nodes.size() == 0) {
            Properties props = initProperties(context);
            nodes = new LinkedList<AItem<T>>();
            createFirstNode(nodes, props);
            for (Object item : createItems(props)) {
                addOption(item);
            }
            createLastNode(nodes, props);
        }
        return nodes;
    }

    /**
     * to be overwritten - does nothing
     * @param nodes
     * @param props
     */
    protected void createFirstNode(List<AItem<T>> nodes, Properties props) {
    }

    /**
     * to be overwritten - does nothing
     * @param nodes
     * @param props
     */
    protected void createLastNode(List<AItem<T>> nodes, Properties props) {
    }

    protected Properties initProperties(Map context) {
        Properties props = new Properties();
        props.putAll(context);
        props.putAll(System.getProperties());
        return props;
    }

    /**
     * define your dynamic loaded items
     * 
     * @param props
     * 
     * @return list of items that will be wrapped into {@link Option}s.
     */
    abstract protected List<?> createItems(Map props);

    /**
     * creates a specific option holding the given item.
     * 
     * @param item to be an option. no generic type is defined to be flexible on extensions, working not directly on
     *            content objects.
     */
    protected void addOption(Object item) {
        final IItem<?> caller_ = this.getParent();
        Option option = new Option<T>(this, item.toString(), null, (T) item, null) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
                super.react(caller, input, in, out, env);
                return caller_;
            }

            @Override
            protected String getName(int fixlength, char filler) {
                //avoid translation ...
                String str = getPresentationPrefix() + name;
                return fixlength != -1 ? StringUtil.fixString(str, fixlength, filler, true) : str;
            }
        };
        nodes.add(option);
    }

    @Override
    public Type getType() {
        return Type.Selector;
    }
    
    /**
     * persistent parent of this selector.
     * @return the first item in hierarchy that is not a selector.
     */
    @Override
    public IContainer<T> getParent() {
        IContainer<T> p = super.getParent();
        return p instanceof Selector ? p.getParent() : p;
    }
    
   @Persist
    protected void initSerialization() {
        super.initDeserialization();
        if (nodes != null) {
            nodes.clear();
        }
    }

}
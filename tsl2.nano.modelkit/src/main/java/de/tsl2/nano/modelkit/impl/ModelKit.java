package de.tsl2.nano.modelkit.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import de.tsl2.nano.modelkit.Configured;
import de.tsl2.nano.modelkit.ExceptionHandler;
import de.tsl2.nano.modelkit.Identified;
import de.tsl2.nano.modelkit.ObjectUtil;
import de.tsl2.nano.modelkit.impl.ModelKitLoader.JsonToMapConverter;
import lombok.Getter;
import lombok.Setter;

/**
 * full model kit providing all elements to apply a function to a list of objects. a factory is provided
 * through #ModelKitLoader.
 */
@ApplicationScoped
@JsonPropertyOrder({ "name", "cron", "funcName", "description" })
public class ModelKit<T> extends AIdentified implements Function<List<T>, List<T>> {
    private static final Logger LOG = LoggerFactory.getLogger(ModelKit.class);
    private static boolean testMode;

    @JsonDeserialize(converter = JsonToMapConverter.class)
    Map<Class<? extends Identified>, List<? extends Identified>> env = new HashMap<>();

    @Getter @Setter
    private String cron;
    @Setter
    private String description;

    @Getter
    @Setter
    private String funcName;

    @JsonIgnore
    private String cronDescription;
    @JsonIgnore
    private static String[] logDebugFields;

    /** constructor is used internally on injection - but we have producers */
    ModelKit() {
        super(null);
    }

    public ModelKit(String name, String funcName, String cron, String description) {
        super(name);
        this.cron = cron;
        this.description = description;
        this.funcName = funcName;
        validate();
    }

    @Override
    public List<T> apply(List<T> items) {
        if (items.isEmpty()) {
            LOG.warn("the given list is empty - nothing to do!");
            return items;
        }
        long start = System.currentTimeMillis();
        before(items);
        final List<T> newItemList = new ArrayList<>(items.size());
        final List<T> passedItems = new ArrayList<>(items);

        int passes = getPassCount();
        for (int i = 0; i < passes; i++) {
            final int ii = i; //workaround to provide a final var in enclosing lambda
            forEachGroup(g -> newItemList.addAll(g.apply(ii, passedItems)));
            passedItems.addAll(newItemList);
            newItemList.clear();
        }

        after(newItemList);
        logDebug(newItemList, System.currentTimeMillis() - start);
        return newItemList;
    }

    private int getPassCount() {
        return get(Group.class).stream()
                .max((g1, g2) -> g1.getPassCount().compareTo(g2.getPassCount()))
                .get()
                .getPassCount();
    }

    /** to be implemented by extension */
    protected void before(List<T> items) {
    }

    /** to be implemented by extension */
    protected void after(List<T> items) {
    }

    static boolean isTestMode() {
        return testMode;
    }

    @Override
    public void validate() {
        isActiveNow();
        if (env.size() > 0) {
            List<Group> groups = get(Group.class);
            Objects.checkIndex(0, groups.size());
            if (!groups.stream().anyMatch(g -> g.getPassCount() > 0)) {
                throw new IllegalStateException(
                        "no group with any function found. at least one group must have a function to be applied!");
            }
            env.values().forEach(e -> e.forEach(c -> ((Configured) c).validate()));
        }
    }

    public String getDescription() {
        return description;
    }

    public void add(Identified... parts) {
        addIdentifiedArray(parts);
    }

    private void addIdentifiedArray(Identified... parts) {
        List<Identified> list = Arrays.asList(parts);
        env.put((Class<? extends Identified>) parts.getClass().getComponentType(), list);
        list.stream().forEach(i -> i.tagNames(this.name));
        list.stream().forEach(i -> ((Configured) i).setConfiguration(this));
    }

    public void add(Fact... parts) {
        Fact[] negations = new Fact[parts.length];
        for (int i = 0; i < negations.length; i++) {
            negations[i] = ((Fact) parts[i].clone()).setNegate();
        }
        addIdentifiedArray(Stream.concat(Arrays.stream(parts), Arrays.stream(negations)).toArray(Fact[]::new));
    }

    @Override
    public <I extends Identified> I get(String name, Class<I> type) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        List<I> elements = get(type);
        Objects.requireNonNull(elements,
                "configuration error: your model kit didn't declare any element of type " + type.getSimpleName()
                        + " for name: " + name);
        return Identified.get(elements, tag(this.name, name));
    }

    @Override
    public <I extends Identified> List<I> get(Class<I> type) {
        return (List<I>) env.get(type);
    }

    public List<?> getEnum(String definitionName) {
        return (List<?>) get(tag(name, definitionName), Def.class).getValue();
    }

    public <E extends AIdentified> E getPrevious(E element) {
        return getAt(element, -1);
    }

    public <E extends AIdentified> E getNext(E element) {
        return getAt(element, 1);
    }

    public <E extends AIdentified> E getAt(E element, int addIndex) {
        List<E> elements = (List<E>) get(element.getClass());
        int i = elements.indexOf(element);
        return i == -1 || i + addIndex < 0 || i + addIndex >= elements.size() ? null : elements.get(i + addIndex);
    }

    boolean isActiveNow() {
        return isActive(ZonedDateTime.now());
    }

    boolean isActive(ZonedDateTime time) {
        if (cron == null) {
            return true;
        }
        CronParser parser = getCronParser();
        ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(cron));
        Duration timeToNextExecution = executionTime.timeToNextExecution(time).orElseThrow();

        return timeToNextExecution.getSeconds() < 2;
    }

    private CronParser getCronParser() {
        return new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
    }

    private String cronDescription() {
        if (cronDescription == null) {
            cronDescription = cron != null ? CronDescriptor.instance().describe(getCronParser().parse(cron)) : "active on any time!";
        }
        return cronDescription;
    }

    /** convenience to crawl through owned groups (the type is only for compiler generic access) */
    public void forEachGroup(Consumer<Group<T>> c) {
        get(Group.class).stream().forEach(g -> c.accept(g));
    }

    public <T> void forEachGroupItem(List<T> items, Consumer<T> c) {
        get(Group.class).stream().forEach(g -> g.filter(items).forEach(i -> c.accept((T) i)));
    }

    public void forEachElement(Consumer<Identified> c) {
        env.values().forEach(e -> e.forEach(c));
    }

    public <T> String describe() {
        String chapter = "\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n";
        StringBuilder b = new StringBuilder(chapter + toString() + "\n");
        forEachGroup(g -> b.append("\t" + g.describe("\t") + "\n"));
        b.append(chapter);
        return b.toString();
    }

    public String describeResult() {
        StringBuilder b = new StringBuilder("\ncount of calls:\n");
        forEachElement(c -> b.append("\n\t" + c.getName() + ": " + ((Configured) c).getVisitorCount()));
        b.append("\n\n");
        return b.toString();
    }

    public static void enableDebugLog(String... logDebugFields) {
        ModelKit.logDebugFields = logDebugFields;
        testMode = true;
    }

    public void logDebug(List<?> items, long duration) {
        if (testMode || LOG.isDebugEnabled()) {
            LOG.info(
                    "\n" + name + " on " + items.size() + " items (time: " + duration + " msec)\n" +
                    describeResult() +
                            ObjectUtil.toString(items, logDebugFields));
        }
    }

    /** optional function to be called, if all configurations are done */
    void finalizeOnType() {
        if (testMode || LOG.isDebugEnabled()) {
            LOG.info(describe());
        }
    }

    // @Override
    // public boolean equals(Object obj) {
    //     // TODO: not performance optimized
    //     return super.equals(obj) && forEachElement(e -> ((ModelKit)obj).checkExistence(e.getName(), e.getClass()));
    // }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ": " + cronDescription() + ")";
    }

    public void register() {
        ModelKitLoader.register(this);
        finalizeOnType();
    }

    @ApplicationScoped
    @Produces
    @Named("Configured")
    @Default
    public static ModelKit getActiveModelKitNow() {
        return ModelKitLoader.getActiveModelKit(ZonedDateTime.now());
    }

    public static ModelKit getActiveModelKit(ZonedDateTime time) {
        return ModelKitLoader.getActiveModelKit(time);
    }

    public static void saveAsJSon(ModelKit... configs) {
        Arrays.stream(configs).forEach(c -> c.validate());
        new ModelKitTestLoader().test();

        ModelKitLoader.saveAsJSon(configs);
    }

    public void reset() {
        ModelKitLoader.reset();
    }

    public static void resetAndDelete() {
        ModelKitLoader.resetAndDelete();
    }
}

/**
 * loads all available model kits from json
 * <p/>
 * given by current date, only one model kit will be selected.
 * <p/>
 * TODO: configure load/save path for cloud environment
 */
class ModelKitLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ModelKitLoader.class);
    private static final String MODELKIT_JSON_JSON = "modelkit.json";

    /** hard coded modelkits include lambda implementations, to be reused on dynamic loaded model kits.  */
    private static Map<String, ModelKit> registeredHardConfigurations = new LinkedHashMap<>();
    /** loaded dynamic model kits */
    private static List<ModelKit> configurations;

    private ModelKitLoader() {
    }

    public static void register(ModelKit config) {
        registeredHardConfigurations.put(config.name, config);
    }

    public static <I extends Identified> I findRegistered(String kitName, String name, Class<I> type) {
        if (kitName.equals("*")) {
            for (ModelKit<?> config : registeredHardConfigurations.values()) {
                I ref = ExceptionHandler.trY(() -> config.get(name, type), IllegalStateException.class);
                if (ref != null) {
                    return ref;
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.warn("no configuration item found for config: " + kitName + "/" + name);
            }
            return null;
        } else {
            ModelKit<?> config = registeredHardConfigurations.get(kitName);
            if (config == null && ModelKit.isTestMode()) {
                throw new IllegalStateException("no registry entry found for configuration: " + kitName);
            }
            return config != null ? ExceptionHandler.trY(() -> config.get(name, type), IllegalStateException.class) : null;
        }
    }

     static ModelKit getActiveModelKit(ZonedDateTime time) {
        return getActiveModelKit(getConfigurations(), time);
    }

    static ModelKit getActiveModelKit(List<ModelKit> configs, ZonedDateTime time) {
        return configs.stream().filter(c -> c.isActive(time)).findFirst().orElseThrow();
    }

    private static List<ModelKit> getConfigurations() {
        if (configurations == null) {
            if (!new File(MODELKIT_JSON_JSON).exists()) {
                saveAsJSon(registeredHardConfigurations.values().toArray(new ModelKit[0]));
            }
            configurations = readFromJSon();
            configurations.forEach(c -> c.forEachElement(e -> ((Configured)e).setConfiguration(c)));
        }
        return configurations;
    }

    static List<ModelKit> readFromJSon() {
        LOG.info("loading configurations from " + MODELKIT_JSON_JSON);
        try {
            return createObjectMapper().readValue(new File(MODELKIT_JSON_JSON),
                new TypeReference<List<ModelKit>>() {
                });
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void saveAsJSon(ModelKit... configs) {
        LOG.info("saving " + MODELKIT_JSON_JSON + " on new configuration array");
        try {
            ObjectMapper mapper = createObjectMapper();
            mapper.writeValue(new File(MODELKIT_JSON_JSON), configs);
            reset();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        mapper.findAndRegisterModules();
        return mapper;
    }

    static void reset() {
        configurations = null;
    }
    static void resetAndDelete() {
        new File(MODELKIT_JSON_JSON).delete();
        reset();
    }

    public static class JsonToMapConverter extends StdConverter<Object, Map<Class, List<Identified>>> {
        @Override
        public Map<Class, List<Identified>> convert(Object value) {
            try {
                Map<String, List> map = (Map)value;
                Class type;
                Map<Class, List<Identified>> newMap = new LinkedHashMap<>();
                for (Map.Entry<String, List> e : map.entrySet()) {
                    newMap.put(type = Class.forName(e.getKey()), createTypedList(type, e.getValue()));
                }
                return newMap;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private List<Identified> createTypedList(Class type, List v) {
            List newList = new ArrayList<>(v.size());
            v.forEach(i -> newList.add(createIdentifiedObject(type, (Map) i)));
            return newList;
        }

        private Object createIdentifiedObject(Class type, Map<String, Object> args) {
            try {
                String name = (String)args.get("name");
                String[] path = name.split("\\.");

                String kitName = path.length > 1 ? path[0] : "*";
                kitName = kitName.startsWith("!") ? kitName.substring(1) : kitName;
                Identified hardRegistered = findRegistered(kitName, name, type);
                if (hardRegistered == null && ModelKit.isTestMode()) {
                    throw new IllegalStateException(name + " (" + type.getSimpleName() + ")  was not found in any registered configuration with name: " + kitName);
                }
                Object obj = hardRegistered != null ? hardRegistered : type.getDeclaredConstructor().newInstance();
                for (Map.Entry<String, Object> e : args.entrySet()) {
                    ObjectUtil.setValue(obj, e.getKey(), e.getValue());
                }
                return obj;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}

class ModelKitTestLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ModelKitTestLoader.class);
    public void test(ModelKit...kits) {
        List<Object> testItems = loadTestItems();
        List<String> warnings = new LinkedList<>();
        Arrays.stream(kits).forEach(c -> warnings.addAll(test(c, testItems)));
        if (!warnings.isEmpty()) {
            throw new IllegalStateException("The following configuration elements were missed: " + warnings);
        }
    }
    /**
     * tests against a sample list of items, load from json. checks, if all configuration elements were visisted and returns a
     * list of messages with non visited elements.
     */
    public List<String> test(ModelKit<?> kit, List<?> items) {
        LOG.info("doing a sorting test on new loaded configuration '" + kit.name + "' and " + items.size() + " items");
        List<?> sortedItems = kit.sort(items);
        if (sortedItems.size() != items.size()) {
            throw new IllegalStateException(
                "The groups of this sort configuration are overlapping or miss some items: given items: "
                    + items.size() + " <> sorted-items: " + sortedItems.size());
        }
        List<String> names = new LinkedList<>();
        kit.forEachElement(c -> {
            if (((Configured) c).getVisitorCount() > 0) {
                names.add(c.getName());
            }
        });
        return names;
    }
    private List<Object> loadTestItems() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("model-validation.json")) {
            return ModelKitLoader.createObjectMapper().readValue(in,
                new TypeReference<List<Object>>() {
                });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

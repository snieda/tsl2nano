package de.tsl2.nano.modelkit.impl;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tsl2.nano.modelkit.Configured;
import de.tsl2.nano.modelkit.Identified;
import de.tsl2.nano.modelkit.ObjectUtil;

/**
 * full model kit providing all elements to sort objects through a complex configurable algorithm. a factory is provided
 * through #ModelKitLoader.
 */
public class ModelKit extends AbstractIdentified {
    private static final Logger LOG = LoggerFactory.getLogger(ModelKit.class);
    private static boolean testMode;

    Map<Class<? extends Identified>, List<? extends Identified>> env = new HashMap<>();
    private String cron;
    private String description;

    @JsonIgnore
    private String cronDescription;

    public ModelKit(String name, String cron, String description) {
        super(name);
        this.cron = cron;
        this.description = description;
        validate();
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
            if (!groups.stream().anyMatch(g -> g.comparatorNames.size() > 0)) {
                throw new IllegalStateException("no group with comparators found. at least one group must have a comparator!");
            }
            env.values().forEach(e -> e.forEach(c -> ((Configured) c).validate()));
        }
    }

    /**
     * tests against a sample list of items, load from json. checks, if all configuration elements were visisted and returns a
     * list of messages with non visited elements.
     */
    public List<String> test(List<?> items) {
        // TODO: load sample items from json
        List<?> sortedItems = sort(items);
        if (sortedItems.size() != items.size()) {
            throw new IllegalStateException(
                "The groups of this model kit are overlapping or miss some items: given items: "
                    + items.size() + " <> sorted-items: " + sortedItems.size());
        }
        List<String> names = new LinkedList<>();
        forEachElement(c -> {
            if (((Configured) c).getVisitorCount() > 0) {
                names.add(c.getName());
            }
        });
        return names;
    }

    public String getDescription() {
        return description;
    }

    public void add(Identified... parts) {
        addIdentifiedArray(parts);
    }

    private void addIdentifiedArray(Identified... parts) {
        List<Identified> list = Arrays.asList(parts);
        env.put(parts[0].getClass(), list);
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
        return Identified.get(get(type), name);
    }

    @Override
    public <I extends Identified> List<I> get(Class<I> type) {
        return (List<I>) env.get(type);
    }

    public List<?> getEnum(String definitionName) {
        return (List<?>) get(definitionName, Definition.class).getValue();
    }

    public <E extends AbstractIdentified> E getPrevious(E element) {
        return getAt(element, -1);
    }

    public <E extends AbstractIdentified> E getNext(E element) {
        return getAt(element, 1);
    }

    public <E extends AbstractIdentified> E getAt(E element, int addIndex) {
        List<E> elements = (List<E>) get(element.getClass());
        int i = elements.indexOf(element);
        return i == -1 || i + addIndex < 0 || i + addIndex >= elements.size() ? null : elements.get(i + addIndex);
    }

    boolean isActiveNow() {
        return isActive(ZonedDateTime.now());
    }

    boolean isActive(ZonedDateTime time) {
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
            cronDescription = CronDescriptor.instance().describe(getCronParser().parse(cron));
        }
        return cronDescription;
    }

    /** convenience to crawl through owned groups (the type is only for compiler generic access) */
    public <T> void forEachGroup(Class<T> itemType, Consumer<Group<T>> c) {
        get(Group.class).stream().forEach(g -> c.accept(g));
    }

    public <T> void forEachGroupItem(List<T> items, Consumer<T> c) {
        get(Group.class).stream().forEach(g -> g.filter(items).forEach(i -> c.accept((T) i)));
    }

    public void forEachElement(Consumer<Identified> c) {
        env.values().forEach(e -> e.forEach(c));
    }

    public <T> String describe(Class<T> type) {
        String chapter = "\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n";
        StringBuilder b = new StringBuilder(chapter + toString() + "\n");
        forEachGroup(type, g -> b.append("\t" + g.describe("\t") + "\n"));
        b.append(chapter);
        return b.toString();
    }

    public String describeResult() {
        StringBuilder b = new StringBuilder("\ncount of calls:\n");
        forEachElement(c -> b.append("\n\t" + c.getName() + ": " + ((Configured) c).getVisitorCount()));
        b.append("\n\n");
        return b.toString();
    }

    public static void enableDebugLog() {
        testMode = true;
    }

    public void logDebug(List<?> items, long duration, String... fieldNames) {
        if (testMode || LOG.isDebugEnabled()) {
            LOG.info(
                "\n" + name + " sorted " + items.size() + " items (time: " + duration + " msec)\n" +
                    describeResult() +
                    ObjectUtil.toString(items, fieldNames));
        }
    }

    /** optional function to be called, if all configurations are done */
    public void finalizeOnType(Class<?> type) {
        if (testMode || LOG.isDebugEnabled()) {
            LOG.info(describe(type));
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

    public <T> List<T> sort(List<T> items, String... logDebugFields) {
        if (items.isEmpty()) {
            LOG.warn("the given list is empty - nothing to do!");
            return items;
        }
        long start = System.currentTimeMillis();
        Class<T> type = (Class<T>) items.iterator().next().getClass();
        final List<T> sortedItems = new ArrayList<>(items.size());

        forEachGroup(type, g -> sortedItems.addAll(g.sort(items)));

        logDebug(sortedItems, System.currentTimeMillis() - start, logDebugFields);
        return sortedItems;
    }

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

        // TODO: provide a list of test items
        ArrayList<?> testItems = new ArrayList<>();
        List<String> warnings = new LinkedList<>();
        Arrays.stream(configs).forEach(c -> warnings.addAll(c.test(testItems)));
        if (testMode && !warnings.isEmpty()) {
            throw new IllegalStateException("The following configuration elements were missed: " + warnings);
        }
        ModelKitLoader.saveAsJSon(configs);
    }

    public void reset() {
        ModelKitLoader.reset();
    }
}

/**
 * loads all available model kits from json. at the moment, there are two configurations (generated by implementations):
 * Standard and Sunday.
 * <p/>
 * given by current date, only one model kit will be selected.
 * <p/>
 * TODO: configure load/save path for cloud environment
 */
class ModelKitLoader {
    private static final String SORT_CONFIGURATION_JSON = "sort-configuration.json";

    private static List<ModelKit> configurations;

    private ModelKitLoader() {
    }

    static ModelKit getActiveModelKit(ZonedDateTime time) {
        return getActiveModelKit(getConfigurations(), time);
    }

    static ModelKit getActiveModelKit(List<ModelKit> configs, ZonedDateTime time) {
        return configs.stream().filter(c -> c.isActive(time)).findFirst().orElseThrow();
    }

    private static List<ModelKit> getConfigurations() {
        if (configurations == null) {
            configurations = readFromJSon();
        }
        return configurations;
    }

    static List<ModelKit> readFromJSon() {
        try {
            return createObjectMapper().readValue(new File(SORT_CONFIGURATION_JSON),
                new TypeReference<List<ModelKit>>() {
                });
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void saveAsJSon(ModelKit... configs) {
        try {
            ObjectMapper mapper = createObjectMapper();
            mapper.writeValue(new File(SORT_CONFIGURATION_JSON), configs);
            reset();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static ObjectMapper createObjectMapper() {
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
}

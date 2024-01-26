package de.tsl2.nano.serialize;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import de.tsl2.nano.filter.CollectionFilter;
import de.tsl2.nano.filter.Between;
import de.tsl2.nano.filter.In;
import de.tsl2.nano.filter.Equals;

public class ApplicationConfigurationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationConfigurationProvider.class);
    private static final String STAGE = System.getProperty("tsl2nano.app.stage", "test");
    public static final String BASE_PATH = System.getProperty("tsl2nano.app.config.basepath", "config/");
    private static final String FILE_NAME = System.getProperty("tsl2nano.app.filename", "application.yml");

    @PostConstruct
    void init() {
        // TODO: implement system
        // readConfiguration(ApplicationConfiguration.class, STAGE);
    }

    public <T> T readConfiguration(Class<T> type) {
        return readConfiguration(type, STAGE);
    }

    private <T> T readConfiguration(Class<T> type, String stage) {
        ObjectMapper mapper = createObjectMapper();

        try {
            String configFile = getConfigPath(stage);
            LOG.debug("reading application-configuration from {}", configFile);
            if (LOG.isDebugEnabled()) {
                printInfo(stage);
            }
            return mapper.readValue(new File(configFile), type);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER));
        mapper.findAndRegisterModules();
        mapper.registerSubtypes(getSubTypes().toArray(new Class[0]));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));

        // PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().build();
        // mapper.activateDefaultTyping(ptv); // default to using DefaultTyping.OBJECT_AND_NON_CONCRETE
        // mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

    private void printInfo(String stage) {
        try {
            String conf = new String(Files.readAllBytes(Paths.get(getConfigPath(stage))), "UTF-8");
            LOG.info("\n\n" + conf + "\n");
        } catch (IOException e) {
            LOG.error("couldn't print configuration info", e.toString());
        }
    }

    private String getConfigPath(String stage) {
        String configPath = BASE_PATH + FILE_NAME.replace("application.yml", "application-" + stage + ".yml");
        LOG.debug("using configuration path: " + configPath);
        return configPath;
    }

    public <T> void write(T appConfig) {
        write(appConfig, STAGE);
    }

    private <T> void write(T appConfig, String stage) {
        ObjectMapper mapper = createObjectMapper();
        try {
            String configPath = getConfigPath(stage);
            LOG.debug("writing application configuration to {}", configPath);
            mapper.writeValue(new File(configPath), appConfig);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    private Set<Class> getSubTypes() {
        // Set<Class> subTypes = Classes.getAllOfPackage(CollectionFilter.class);
        // subTypes.addAll(Arrays.asList(LocalDate.class, LocalDateTime.class, LocalTime.class, Double.class,
        //         Integer.class, String.class));
        // return subTypes;
        return new HashSet<>(Arrays.asList(Equals.class, In.class, Between.class));
    }

}

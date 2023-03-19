package de.tsl2.nano.modelkit;

import java.util.List;

import de.tsl2.nano.modelkit.impl.ModelKit;

/**
 * provides a mechanism to access the base configuration.
 */
public interface Configured {
    /** each artifact should be assigned to a configuration */
    void setConfiguration(ModelKit config);

    /** get all artifacts of given type from configuration */
    <T extends Identified> List<T> get(Class<T> type);

    /** get an name-identifable of given type from configuration */
    <T extends Identified> T get(String name, Class<T> type);

    /** check all named artifacts to be found inside configuration */
    void validate();

    /** provides a check mechanism for count of calls to each configured element */
    void visited(Object... explanation);

    long getVisitorCount();

    /** helper to check for existence in configuration */
    default <T extends Identified> void checkExistence(Class<T> type, List<String> names) {
        names.stream().forEach(c -> checkExistence(c, type));
    }

    /** helper to check for existence in configuration */
    default <T extends Identified> String checkExistence(String name, Class<T> type) {
        if (name != null) {
            get(name, type);
        }
        return name;
    }
}

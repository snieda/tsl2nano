package de.tsl2.nano.modelkit.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.tsl2.nano.modelkit.Configured;
import de.tsl2.nano.modelkit.Identified;
import lombok.Setter;

/**
 * base implementation to provide name-identifying and access to the base configuration.
 */
public abstract class AbstractIdentified implements Identified, Configured, Cloneable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIdentified.class);

    @Setter
    String name;
    @JsonIgnore
    ModelKit config;
    @JsonIgnore
    long countOfCalls;

    AbstractIdentified() {
    }

    public AbstractIdentified(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void visited(Object... explanation) {
        if (ModelKit.isTestMode() && explanation.length > 0) {
            LOG.info(Arrays.asList(explanation).toString());
        }
        countOfCalls++;
    }

    @Override
    public long getVisitorCount() {
        return countOfCalls;
    }

    @Override
    public boolean equals(Object obj) {
        return equals0(obj);
    }

    @Override
    public int hashCode() {
        return hashCode0();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public void setConfiguration(ModelKit config) {
        this.config = config;
        validate();
    }

    public void tagNames(String parent) {
        name = tag(parent, name);
    }

    protected static final String tag(String parent, String name) {
        if (name == null) {
            return null;
        } else if (parent == null) {
            return name;
        }
        boolean not = Fact.negate(name);
        String unsigned = not ? Fact.not(name) : name;
        String path = unsigned.startsWith(parent + ".") ? unsigned : parent + "." + unsigned;
        return not ? Fact.not(path) : path;
    }

    protected void tag(String parent, List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            names.set(i, tag(parent, names.get(i)));
        }
    }

    @Override
    public <T extends Identified> T get(String name, Class<T> type) {
        Objects.requireNonNull(config, "Please assign an instance of ModelKit to " + this.toString() + " before!");
        return config.get(name, type);
    }

    @Override
    public <T extends Identified> List<T> get(Class<T> type) {
        Objects.requireNonNull(config, "Please assign an instance of ModelKit to " + this.toString() + " before!");
        return config.get(type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getName();
    }
}

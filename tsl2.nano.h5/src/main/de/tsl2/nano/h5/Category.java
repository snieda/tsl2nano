package de.tsl2.nano.h5;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.Util;

public class Category {
 
    private Map<String, List<String>> categories;

    public Category(String propertyFile) {
        Properties catprops = FileUtil.loadProperties(propertyFile);
        categories = new HashMap<>();

        catprops.forEach((k, v) -> categories.put((String)k, split((String)v)));
    }
    List<String> split(String list) {
        return !Util.isEmpty(list) ? Arrays.asList(list.split(",")) : null;
    }

    public String get(String item) {
        if (categories.keySet().contains(item))
            return item;
        
        return categories.entrySet().stream()
            .filter( e -> e.getValue() != null && e.getValue().contains(item))
            .map(e -> e.getKey())
            .findFirst()
            .orElse(null);
    }
} 
 
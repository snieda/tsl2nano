/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 01.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.incubation.rules;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.execution.XmlUtil;
import de.tsl2.nano.log.LogFactory;

/**
 * Holds all defined rules. Reading existing rules from directory.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class RulePool {
    Log LOG = LogFactory.getLog(RulePool.class);
    Map<String, Rule<?>> rules;

    /**
     * constructor
     */
    public RulePool() {
        super();
        loadRules();
    }
    
    private void loadRules() {
        rules = new HashMap<String, Rule<?>>();
        String dirName = Environment.getConfigPath() + "rules/";
        File dir = new File(dirName);
        dir.mkdirs();
        File[] ruleFiles = dir.listFiles();
        Rule<?> rule;
        for (int i = 0; i < ruleFiles.length; i++) {
            try {
                rule = Environment.get(XmlUtil.class).loadXml(ruleFiles[i].getPath(), Rule.class);
                rules.put(rule.name, rule);
            } catch (Exception e) {
                LOG.error(e);
            }
        }
    }
    /**
     * getRule
     * @param name rule to find
     * @return rule or null
     */
    public Rule<?> getRule(String name) {
        return rules.get(name);
    }

    /**
     * adds the given rule to the pool
     * @param name rule name
     * @param rule rule to add
     */
    public void addRule(String name, Rule<BigDecimal> rule) {
        rules.put(name, rule);
    }
}

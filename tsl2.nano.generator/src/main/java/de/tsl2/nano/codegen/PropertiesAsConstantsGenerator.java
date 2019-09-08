/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 2, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.codegen;

import de.tsl2.nano.core.util.StringUtil;

/**
 * Generates an interface with integer constants (hashed keys or simple sequence) to be used to get resource values.
 * saves ram-space using integers instead of full string keys. provides type-safed access to property values.
 * <p/>
 * ON CONSTRUCTION:
 * 
 * <pre>
 * - (generate key/values to property file (from bean-attribute informations))
 * - load key/values from property file.
 * - map them into a Map[Integer, Object] where the integers are the hashes of the keys. 
 * - generate an interface for that property file with: 
 *   public static final Integer [key-as-constant-name]=[hash(key)];
 *   
 *   These constants can then be used to get the mapped values:
 *      myresourceBundle.getString([generated-interface].[key-name-as-constant-name]);
 * </pre>
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class PropertiesAsConstantsGenerator extends ClassGenerator {

    @Override
    protected String getDefaultDestinationFile(String modelFile) {
        modelFile = modelFile.replace('.', '/');
        modelFile = StringUtil.substring(modelFile, null, "/", true);
        return "src/gen/" + modelFile + ".java";
    }
}

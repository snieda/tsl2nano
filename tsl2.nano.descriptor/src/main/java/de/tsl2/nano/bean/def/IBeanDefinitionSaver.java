package de.tsl2.nano.bean.def;

import de.tsl2.nano.core.ENV;

/**
 * if implementation is found through {@link ENV#get(Class)}, it will be called by {@link BeanDefinition#saveDefinition()}
 * @author ts
 */
@FunctionalInterface
public interface IBeanDefinitionSaver {
	void saveResourceEntries(BeanDefinition def);
}

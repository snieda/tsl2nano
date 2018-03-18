/**
 * 
 */
package de.tsl2.nano.core.cls;

import java.lang.reflect.Method;

/**
 * @author Tom
 *
 */
interface IDefaultAttribute<T> extends IAttribute<T> {

    default int compareTo(IAttribute<T> o) {
        return getId().compareTo(o.getId());
    }

    @Override
    default String getId() {
        return getType().getSimpleName() + "." + getName();
    }

    @Override
    default boolean hasWriteAccess() {
        return true;
    }

    @Override
    default Method getAccessMethod() {
        return null;
    }

    @Override
    default boolean isVirtual() {
        return true;
    }

}

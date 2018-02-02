/**
 * 
 */
package de.tsl2.nano.core.cls;

import java.lang.reflect.Method;

/**
 * @author Tom
 *
 */
public interface IDefaultAttribute<T> extends IAttribute<T> {

    default int compareTo(IAttribute<T> o) {
        return getId().compareTo(o.getId());
    }

    @Override
    public default String getId() {
        return getType().getSimpleName() + "." + getName();
    }

    @Override
    public default boolean hasWriteAccess() {
        return true;
    }

    @Override
    public default Method getAccessMethod() {
        return null;
    }

    @Override
    public default boolean isVirtual() {
        return true;
    }

}

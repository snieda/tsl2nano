package de.tsl2.nano.core;

import java.util.function.Consumer;

import de.tsl2.nano.core.messaging.ChangeEvent;

/**
 * callback hook on ENV property changes
 * @author ts
 *
 * @param <T>
 */
public interface IEnvChangeListener extends Consumer<ChangeEvent> {
}

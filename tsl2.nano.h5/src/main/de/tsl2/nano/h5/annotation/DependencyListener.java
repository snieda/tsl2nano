package de.tsl2.nano.h5.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import de.tsl2.nano.annotation.extension.With;

@Retention(RUNTIME)
@Target(METHOD)
@With(DependencyListenerAnnotationFactory.class)
public @interface DependencyListener {
    String rule();
    ListenerType listenerType();
    String[] observables();
    
    enum ListenerType { SIMPLE, WEBSOCKET, BOTH };
}

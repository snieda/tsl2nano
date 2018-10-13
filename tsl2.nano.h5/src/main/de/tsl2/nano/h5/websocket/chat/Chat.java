package de.tsl2.nano.h5.websocket.chat;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface Chat {
    String receiver();
    String message() default ""; //-->valueExpression
    String attachment() default ""; // -->presentable.iconFromField
}

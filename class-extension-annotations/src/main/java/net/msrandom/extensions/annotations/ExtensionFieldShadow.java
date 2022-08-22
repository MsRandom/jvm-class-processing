package net.msrandom.extensions.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Implies that the following field is used to access a field from the {@link ClassExtension#value} class of the class extension that owns this field.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
@SuppressWarnings("unused")
public @interface ExtensionFieldShadow {
}

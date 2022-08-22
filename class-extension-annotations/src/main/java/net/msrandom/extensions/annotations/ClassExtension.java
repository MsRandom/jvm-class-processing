package net.msrandom.extensions.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker to let this extension be found by the class-processor, allowing it to extend the base class {@link ClassExtension#value}.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@ImplementsBaseElement
public @interface ClassExtension {
    /**
     * Chooses the class that needs to be extended, meaning all members will be copied to it.
     *
     * @return The class to be extended.
     */
    Class<?> value();
}

package net.msrandom.extensions.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Implies that the following element's implementation is just a placeholder to be replaced with an element-
 *  -potentially annotated with {@link ImplementedByExtension} owned by a {@link ClassExtension} class.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE})
public @interface ImplementedByExtension {
}

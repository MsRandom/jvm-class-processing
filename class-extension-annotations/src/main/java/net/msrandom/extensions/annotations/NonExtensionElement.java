package net.msrandom.extensions.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker to force the class-processor to ignore the annotated element despite being an element of a {@link ClassExtension} class.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
        ElementType.TYPE,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.CONSTRUCTOR,
        ElementType.ANNOTATION_TYPE,
        ElementType.PACKAGE
})
public @interface NonExtensionElement {
}

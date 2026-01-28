package org.myjtools.jexten;

import java.lang.annotation.*;

/**
 * The fields of an extension class that are annotated with <tt>Injected</tt>
 * and have a type of other extension points will be automatically assigned.
 * <p>
 * This feature provides a minimal inversion of control mechanism, restricted
 * to extensions managed by the {@link ExtensionManager}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Inject {

    /**
     * Name of a specific extension to be injected, in case of more than one
     * is available and the priority mechanism does not provide enough control.
     * Such extensions must inform the {@link Extension#name()} property.
     */
    String value() default "";

}

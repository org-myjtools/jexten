package org.myjtools.jexten;

import java.lang.annotation.*;


/**
 * This annotation allows to mark an interface or abstract class as an
 * extension point managed by the {@link ExtensionManager}.
 * <p>
 * In order to ensure compatibility between the extension point and its
 * extensions, it is important to maintain correctly the {@link #version()}
 * property. If you are intended to break backwards compatibility keeping the
 * same package and type name, increment the major part of the version in
 * order to avoid runtime errors. Otherwise, increment the minor part of the
 * version in order to state the previous methods are still valid.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExtensionPoint {

    String version() default "1.0";

}

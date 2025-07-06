package org.myjtools.jexten;

import java.lang.annotation.*;


/**
 * This annotation allows to mark a class as an extension managed by the
 * {@link ExtensionManager}.
 * <p>
 * Notice that any class not annotated with {@link Extension} will not be
 * managed in spite of implementing or extending the {@link ExtensionPoint}
 * class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Extension {


    /**
     * The qualified type name of the extension point that is extended.
     * <p>
     * If this field is not provided and the extension class implements directly
     * the extension point class, it will automatically infer the value as the
     * qualified name of the extension point class. Notice that, if the extension
     * point class uses generic parameters, the inference mechanism will not
     * work, so clients must provide the class directly in those cases.
     * <p>
     * Empty string is used as non-defined value
     */
    String extensionPoint() default "";


    /**
     * The minimum version of the extension point that is extended in form of
     * {@code <majorVersion>.<minorVersion>} .
     * <p>
     * If an incompatible version is used (that is, the major part of the version
     * is different), the extension manager will emit a warning and prevent the
     * extension from loading.
     * </p>
     */
    String extensionPointVersion() default "1.0";



    /**
     * org.myjtools.jexten.Priority used when extensions collide, the highest value have priority
     * over others.
     */
    Priority priority() default Priority.NORMAL;


    Scope scope() default Scope.LOCAL;


    /**
     * A custom extension loader that will be used to load the extension.
     * Generally, this is only necessary for extensions designed to be integrated
     * with some external IoC mechanism.
     */
    Class<? extends ExtensionLoader> loadedWith() default ExtensionLoader.class;


    /**
     * A specific name to identify this extension for injection purposes
     */
    String name() default "";
}

/**
 * Core module of the JExten extension framework.
 * <p>
 * This module provides the fundamental API for defining and discovering extensions
 * in a modular Java application. It includes:
 * <ul>
 *   <li>{@code @Extension} and {@code @ExtensionPoint} annotations for marking extension classes</li>
 *   <li>{@code ExtensionManager} for discovering and retrieving extension instances</li>
 *   <li>{@code Scope} and {@code Priority} enums for controlling extension lifecycle and ordering</li>
 *   <li>{@code InjectionProvider} and {@code ExtensionLoader} for custom dependency injection integration</li>
 * </ul>
 *
 * @see org.myjtools.jexten.ExtensionManager
 * @see org.myjtools.jexten.Extension
 * @see org.myjtools.jexten.ExtensionPoint
 */
module org.myjtools.jexten {

    // Public API: Contains all core annotations, interfaces, and classes
    // required by applications to define and consume extensions
    exports org.myjtools.jexten;

    // Logging support for internal diagnostics
    requires org.slf4j;
}

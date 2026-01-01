package org.myjtools.jexten;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.myjtools.jexten.internal.DefaultExtensionManager;


/**
 * Central interface for discovering and retrieving extensions in the JExten framework.
 * <p>
 * The ExtensionManager is responsible for:
 * <ul>
 *   <li>Discovering extensions annotated with {@link Extension} across module layers</li>
 *   <li>Instantiating extensions according to their {@link Scope}</li>
 *   <li>Managing extension lifecycle and caching</li>
 *   <li>Resolving extension priorities when multiple implementations exist</li>
 *   <li>Performing dependency injection on extension instances</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * ExtensionManager manager = ExtensionManager.create();
 *
 * // Get the highest priority extension
 * Optional<MyService> service = manager.getExtension(MyService.class);
 *
 * // Get all extensions
 * manager.getExtensions(MyService.class)
 *     .forEach(s -> s.execute());
 * }</pre>
 *
 * <h2>With Plugin Support</h2>
 * <pre>{@code
 * PluginManager pluginManager = new PluginManager(...);
 * ExtensionManager manager = ExtensionManager.create(pluginManager);
 * }</pre>
 *
 * @see Extension
 * @see ExtensionPoint
 * @see ModuleLayerProvider
 */
public interface ExtensionManager {


    /**
     * Creates a new ExtensionManager that discovers extensions from the specified
     * module layer provider.
     * <p>
     * Use this factory method when you need to discover extensions from custom
     * module layers, such as those created by a {@code PluginManager}.
     *
     * @param layerProvider the provider of module layers to scan for extensions
     * @return a new ExtensionManager instance
     */
    static ExtensionManager create(ModuleLayerProvider layerProvider) {
        return new DefaultExtensionManager(layerProvider);
    }


    /**
     * Creates a new ExtensionManager that discovers extensions from the boot module layer.
     * <p>
     * This is the simplest way to create an ExtensionManager when all extensions
     * are available in the application's boot layer (no dynamic plugins).
     *
     * @return a new ExtensionManager instance using the boot layer
     */
    static ExtensionManager create() {
        return new DefaultExtensionManager(ModuleLayerProvider.boot());
    }


    /**
     * Configures this ExtensionManager to use a custom injection provider for
     * resolving dependencies during extension instantiation.
     * <p>
     * This is useful for integrating with external IoC containers like Spring or Guice.
     *
     * @param injectionProvider the custom injection provider to use
     * @return this ExtensionManager instance for method chaining
     */
    ExtensionManager withInjectionProvider(InjectionProvider injectionProvider);


    /**
     * Retrieves the highest priority extension implementing the specified extension point.
     * <p>
     * Extensions are sorted by their {@link Priority} value, with {@link Priority#HIGHEST}
     * being selected first. If multiple extensions have the same priority, the selection
     * is deterministic but unspecified.
     *
     * @param <T> the type of the extension point
     * @param extensionPoint the extension point interface class
     * @return an Optional containing the highest priority extension, or empty if none found
     */
    <T> Optional<T> getExtension(Class<T> extensionPoint);


    /**
     * Retrieves the highest priority extension implementing the specified extension point
     * that matches the given filter.
     *
     * @param <T> the type of the extension point
     * @param extensionPoint the extension point interface class
     * @param filter a predicate to filter extension classes before selection
     * @return an Optional containing the highest priority matching extension, or empty if none found
     */
    <T> Optional<T> getExtension(Class<T> extensionPoint, Predicate<Class<?>> filter);


    /**
     * Retrieves an extension by its exact name as specified in {@link Extension#name()}.
     *
     * @param <T> the type of the extension point
     * @param extensionPoint the extension point interface class
     * @param name the exact name of the extension to retrieve
     * @return an Optional containing the named extension, or empty if not found
     */
    <T> Optional<T> getExtensionByName(Class<T> extensionPoint, String name);


    /**
     * Retrieves an extension whose name matches the given predicate.
     *
     * @param <T> the type of the extension point
     * @param extensionPoint the extension point interface class
     * @param name a predicate to match extension names
     * @return an Optional containing the first matching extension, or empty if none match
     */
    <T> Optional<T> getExtensionByName(Class<T> extensionPoint, Predicate<String> name);


    /**
     * Retrieves all extensions implementing the specified extension point.
     * <p>
     * The returned stream is ordered by {@link Priority}, with highest priority first.
     *
     * @param <T> the type of the extension point
     * @param extensionPoint the extension point interface class
     * @return a Stream of all extensions, ordered by priority (highest first)
     */
    <T> Stream<T> getExtensions(Class<T> extensionPoint);


    /**
     * Retrieves all extensions implementing the specified extension point
     * that match the given filter.
     *
     * @param <T> the type of the extension point
     * @param extensionPoint the extension point interface class
     * @param filter a predicate to filter extension classes
     * @return a Stream of matching extensions, ordered by priority (highest first)
     */
    <T> Stream<T> getExtensions(Class<T> extensionPoint, Predicate<Class<?>> filter);


    /**
     * Retrieves all extensions whose names match the given predicate.
     *
     * @param <T> the type of the extension point
     * @param extensionPoint the extension point interface class
     * @param filter a predicate to match extension names
     * @return a Stream of extensions with matching names, ordered by priority (highest first)
     */
    <T> Stream<T> getExtensionsByName(Class<T> extensionPoint, Predicate<String> filter);


    /**
     * Clears all cached extension instances.
     * <p>
     * This forces the ExtensionManager to re-discover and re-instantiate extensions
     * on the next request. Useful when module layers have been modified (e.g., plugins
     * added or removed).
     * <p>
     * Note: This does not affect {@link Scope#SINGLETON} instances that have already
     * been created and are managed globally.
     */
    void clear();

}

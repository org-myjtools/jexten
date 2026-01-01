// Copyright  (c) 2022 -  Luis Iñesta Gelabert  <luiinge@gmail.com>
package org.myjtools.jexten;

import java.util.Optional;
import java.util.ServiceLoader.Provider;

/**
 * This interface exposes a method to be used in order to load an extension.
 * The intended way for this is delegate the operation to a Java Platform
 * {@link Provider}, but custom implementations may use other mechanisms.
 * <p>
 * Clients are not required to implement this interface unless there is an
 * external IoC loading mechanism that have to be integrated with the {@link ExtensionManager}.
 * <p>
 * Custom implementations are bound to specific extensions by using the
 * {@link Extension#loadedWith()} annotation property.
 */
public interface ExtensionLoader {


    /**
     * Loads an extension instance from the given service provider.
     * <p>
     * This method is responsible for creating or retrieving an instance of the extension
     * based on the specified scope. The default implementation simply calls
     * {@link Provider#get()}, but custom implementations may integrate with external
     * dependency injection frameworks (such as Spring, Guice, or CDI) to obtain
     * properly configured instances.
     * <p>
     * Implementations should respect the scope semantics:
     * <ul>
     *   <li>{@link Scope#SINGLETON} - Return the same instance across all invocations</li>
     *   <li>{@link Scope#SESSION} - Return a cached instance within the current context</li>
     *   <li>{@link Scope#TRANSIENT} - Create a new instance for each invocation</li>
     * </ul>
     *
     * @param <T> the type of the extension to load
     * @param provider the service provider that can instantiate the extension class
     * @param scope the lifecycle scope that determines instance management behavior
     * @return an Optional containing the loaded extension instance, or empty if the
     *         extension could not be loaded (e.g., due to missing dependencies)
     */
    <T> Optional<T> load(Provider<T> provider, Scope scope);

}

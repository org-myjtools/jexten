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


    <T> Optional<T> load(Provider<T> provider, Scope scope);

}

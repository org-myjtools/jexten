// Copyright  (c) 2021 -  Luis Iñesta Gelabert  <luiinge@gmail.com>

package org.myjtools.jexten.internal;

import java.lang.reflect.*;
import java.util.stream.*;
import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.ServiceLoader.Provider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.myjtools.jexten.*;
import org.slf4j.*;


public class DefaultExtensionManager implements ExtensionManager {

	protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultExtensionManager.class);


	private final ModuleLayerProvider layerProvider;
	private static final Map<Class<?>, Object> singletons = new ConcurrentHashMap<>();
	private final Map<Class<?>, Object> locals = new ConcurrentHashMap<>();
	private final Set<Class<?>> invalidExtensions = ConcurrentHashMap.newKeySet();
	private final Set<Class<?>> validExtensions = ConcurrentHashMap.newKeySet();
	private final InjectionProvider injectionProvider;


	private DefaultExtensionManager(
		ModuleLayerProvider layerProvider,
		InjectionProvider injectionProvider
	) {
		this.layerProvider = layerProvider;
		this.injectionProvider = injectionProvider;
	}


	public DefaultExtensionManager(ModuleLayerProvider layerProvider) {
		this(layerProvider, InjectionProvider.empty());
	}


	@Override
	public ExtensionManager withInjectionProvider(InjectionProvider injectionProvider) {
		return new DefaultExtensionManager(layerProvider, injectionProvider);
	}


	@Override
	public <T> Optional<T> getExtension(Class<T> extensionPoint) {
		return getExtensions(extensionPoint).findFirst();
	}


	@Override
	public <T> Optional<T> getExtension(Class<T> extensionPoint, Predicate<Class<?>> filter) {
		return getExtensions(extensionPoint,filter).findFirst();
	}


	@Override
	public <T> Optional<T> getExtensionByName(Class<T> extensionPoint, String name) {
		return getExtensions(extensionPoint)
			.filter(it -> extensionOf(it).name().equals(name))
			.findFirst();
	}


	@Override
	public <T> Optional<T> getExtensionByName(Class<T> extensionPoint, Predicate<String> filter) {
		return getExtensions(extensionPoint)
			.filter(it -> filter.test(extensionOf(it).name()))
			.findFirst();
	}


	@Override
	public <T> Stream<T> getExtensions(Class<T> extensionPoint) {
		return getExtensions(extensionPoint, x->true);
	}


	@Override
	public <T> Stream<T> getExtensionsByName(Class<T> extensionPoint, Predicate<String> filter) {
		return getExtensions(extensionPoint, it -> filter.test(extensionOf(it).name()));
	}

	@Override
	public <T> Stream<T> getExtensions(Class<T> extensionPoint, Predicate<Class<?>> filter) {
		return getExtensions(
			extensionPoint,
			filter,
			new InjectionHandler(this,injectionProvider,layerProvider,LOGGER)
		);
	}


	@Override
	public void clear() {
		singletons.clear();
		validExtensions.clear();
		invalidExtensions.clear();
	}


	<T> Stream<T> getExtensions(
		Class<T> extensionPoint,
		Predicate<Class<?>> filter,
		InjectionHandler injection
	) {

		addUseDirective(extensionPoint);
		validateAnnotatedWith(extensionPoint, ExtensionPoint.class);

		Set<Provider<T>> candidates =  layerProvider
			.moduleLayers()
			.map(layer -> ServiceLoader.load(layer, extensionPoint))
			.flatMap(ServiceLoader::stream)
			.filter(provider -> filter.test(provider.type()))
			.filter(provider -> validateProvider(provider, extensionPoint))
			.collect(toSet());

		return candidates.stream()
			.sorted(this::comparePriority)
			.map(provider -> instantiate(extensionPoint, provider, injection))
			.flatMap(Optional::stream);
	}



	private void addUseDirective(Class<?> type) {
		Module thisModule = DefaultExtensionManager.class.getModule();
		try {
			// dynamically declaration of 'use' directive, otherwise it will cause an error
			thisModule.addUses(type);
		} catch (ServiceConfigurationError e) {
			LOGGER.error(
				"Cannot register 'use' directive of service {} into module {}",
				type,
				thisModule
			);
		}
	}


	private <T,E> Optional<E> instantiate(
		Class<T> extensionPoint,
		Provider<E> provider,
		InjectionHandler injection
	) {
		var extensionMetadata = extensionOf(provider);
		ExtensionLoader loader = null;
		// this is the default value, but it is only the interface, not a real implementation
		if (extensionMetadata.loadedWith() != ExtensionLoader.class) {
			loader = (ExtensionLoader) singletons.computeIfAbsent(
				extensionMetadata.loadedWith(),
				type->newInstance(type).orElse(null)
			);
		}
		var scope = extensionMetadata.scope();

		Optional<E> instance;

		if (loader != null) {
			instance = loader.load(provider, scope);
		} else {
			instance = switch (scope) {
				case SINGLETON -> singleton(provider.type());
				case LOCAL -> local(provider.type());
				case TRANSIENT -> newInstance(provider.type());
			};
		}

		return instance
			.map(extension -> injection.injectExtensions(extensionPoint, extension))
			.map(this::runPostConstructMethods);

	}



	private <T> Optional<T> newInstance(Class<? extends T> type) {
		try {
			//
			// Since the mechanism to detect implementations relies on the Java ServiceLoader,
			// we can't inject dependencies via constructors;
			// any attempt to declare a service implementation with no default constructor
			// will produce the following compiler error:
			// java: the service implementation does not have a default constructor
			//
			// There are ways to detect implementations without using the ServiceLoader; however,
			// in this library the injection is a convenience rather than a primary feature, so
			// we'll avoid adding more complexity to this part.
			//
			return Optional.of(type.getConstructor().newInstance());
		} catch (ReflectiveOperationException e) {
			LOGGER.error("Cannot instantiate class {} : {}", type.getCanonicalName(), e.toString());
			LOGGER.debug("{}",e,e);
		}
		return Optional.empty();
	}


	@SuppressWarnings("unchecked")
	private  <T> Optional<T> singleton(Class<? extends T> type) {
		T prototype = (T) singletons.computeIfAbsent(type, it->newInstance(it).orElse(null));
		return Optional.ofNullable(prototype);
	}


	@SuppressWarnings("unchecked")
	private  <T> Optional<T> local(Class<? extends T> type) {
		T prototype = (T) locals.computeIfAbsent(type, it->newInstance(it).orElse(null));
		return Optional.ofNullable(prototype);
	}


	private <T> T runPostConstructMethods(T extension) {
		var methods = Stream.of(extension.getClass().getMethods())
			.filter(method -> method.isAnnotationPresent(PostConstruct.class))
			.filter(method -> method.getParameterCount() == 0)
			.toList();
		for (var method : methods) {
			try {
				method.invoke(extension);
			} catch (IllegalAccessException e) {
				LOGGER.error("Cannot execute post construct method {}::{}  : {}",
					extension.getClass().getCanonicalName(),
					method.getName(),
					e.getMessage()
				);
				LOGGER.debug("{}",e,e);
			} catch (InvocationTargetException e) {
				LOGGER.error("Cannot execute post construct method {}::{}  : {}",
					extension.getClass().getCanonicalName(),
					method.getName(),
					e.getTargetException().getMessage()
				);
				LOGGER.debug("{}",e.getTargetException(),e.getTargetException());
			}
		}
		return extension;
	}


	@SuppressWarnings("unchecked")
	private <T> boolean validateProvider(Provider<T> provider, Class<T> extensionPoint) {
		Class<T> extension = (Class<T>) provider.type();
		if (validExtensions.contains(extension)) {
			return true;
		}
		if (invalidExtensions.contains(extension)) {
			return false;
		}
		try {
			validateAnnotatedWith(extension, Extension.class);
			var extensionMetadata = extension.getAnnotation(Extension.class);
			var extensionPointMetadata = extensionPoint.getAnnotation(ExtensionPoint.class);
			validateExtensionMetadata(extensionMetadata,extensionPointMetadata);
			validExtensions.add(extension);
			return true;
		} catch (Exception e) {
			LOGGER.warn(
				"Extension {} implementing {} is not valid and it will be ignored",
				extension.getCanonicalName(),
				extensionPoint.getCanonicalName()
			);
			LOGGER.warn(e.getMessage());
			LOGGER.debug("{}",e,e);
			invalidExtensions.add(extension);
			return false;
		}
	}


	private void validateExtensionMetadata(
		Extension extensionMetadata,
		ExtensionPoint extensionPointMetadata
	) throws IllegalArgumentException {
		var implementationVersion = Version.of(extensionMetadata.extensionPointVersion());
		var specificationVersion = Version.of(extensionPointMetadata.version());
		boolean compatible = specificationVersion.isCompatibleWith(implementationVersion);
		if (!compatible) {
			throw new IllegalArgumentException(String.format(
				"Extension point implementation version %s not compatible with specification version %s",
				implementationVersion,
				specificationVersion
			));
		}
	}


	private void validateAnnotatedWith(Class<?> type, Class<? extends Annotation> annotation) {
		if (!type.isAnnotationPresent(annotation)) {
			throw new IllegalArgumentException(String.format(
				"Class %s not annotated with %s",
				type.getCanonicalName(),
				annotation.getCanonicalName()
			));
		}
	}


	private <T> int comparePriority (Provider<T> providerA, Provider<T> providerB) {
		return extensionOf(providerA).priority().compareTo(extensionOf(providerB).priority());
	}



	private <T> Extension extensionOf(Provider<T> provider) {
		return provider.type().getAnnotation(Extension.class);
	}

	private Extension extensionOf(Object extension) {
		return extension.getClass().getAnnotation(Extension.class);
	}


}

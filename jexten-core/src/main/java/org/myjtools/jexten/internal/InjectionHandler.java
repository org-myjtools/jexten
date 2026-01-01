// Copyright  (c) 2021 -  Luis Iñesta Gelabert  <luiinge@gmail.com>

package org.myjtools.jexten.internal;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.myjtools.jexten.*;
import org.slf4j.Logger;

public class InjectionHandler {


    private static final Pattern GENERIC_NAME = Pattern.compile("[^<]+<([^>]+)>");

    private static final Set<Class<?>> COLLECTION_TYPES = Set.of(
        List.class,
        Set.class,
        Collection.class
    );


    private final ModuleLayerProvider layerProvider;


    public record InjectionRequest (
        Class<?> requestedType,
        Class<?> effectiveType,
        String name,
        boolean isGenericType,
        boolean isArray,
        boolean isCollectionType,
        boolean isExtensionPoint
    ) {}


    private final Logger logger;
    private final DefaultExtensionManager extensionManager;
    private final Map<Class<?>, Map<Class<?>,Object>> resolvedInstances = new HashMap<>();
    private final InjectionProvider externalInjectionProvider;

    public InjectionHandler(
        DefaultExtensionManager extensionManager,
        InjectionProvider externalInjectionProvider,
        ModuleLayerProvider layerProvider,
        Logger logger
    ) {
        this.extensionManager = extensionManager;
        this.externalInjectionProvider = externalInjectionProvider;
        this.layerProvider = layerProvider;
        this.logger = logger;
    }


    public <T,E> E injectExtensions(Class<T> extensionPoint, E extension) {
        addExtensionIfAbsent(extensionPoint, extension);
        Class<?> extensionClass = extension.getClass();
        Stream.<Class<?>>iterate(extensionClass, Objects::nonNull, Class::getSuperclass)
            .flatMap(this::getAllFields)
            .filter(field -> field.isAnnotationPresent(Inject.class))
            .forEach(field -> tryInjectExtensions(extension, field));
        return extension;
    }




    private void addExtensionIfAbsent(Class<?> extensionPoint, Object extension) {
        resolvedInstances
            .computeIfAbsent(extensionPoint, x->new HashMap<>())
            .putIfAbsent(extension.getClass(), extension);
    }


    private Object retrieveInjectableInstances(InjectionRequest request) {

        Collection<Object> retrievedValues;

        if (!request.isExtensionPoint()) {
            retrievedValues = externalInjectionProvider.provideInstancesFor(
                request.effectiveType,
                request.name
            ).toList();
        } else {
            var extensions = resolvedInstances.computeIfAbsent(request.effectiveType, x -> new HashMap<>());
            extensionManager
                .getExtensions(request.effectiveType, type -> !extensions.containsKey(type), this)
                .forEach(extension -> extensions.putIfAbsent(extension.getClass(), extension));
            retrievedValues = extensions.values().stream()
                .filter(it -> extensionMatchName(it, request.name))
                .toList();
        }

        Object results = retrievedValues;

        if (request.isArray) {
            var array = Array.newInstance(request.effectiveType, retrievedValues.size());
            AtomicInteger i = new AtomicInteger();
            retrievedValues.forEach(value -> Array.set(array, i.getAndIncrement(), value));
            results = array;
        } else if (request.isCollectionType) {
            if (request.requestedType == List.class || request.requestedType == Collection.class) {
                results = List.copyOf(retrievedValues);
            } else if (request.requestedType == Set.class) {
                results = Set.copyOf(retrievedValues);
            }
        } else if (request.isGenericType) {
            if (request.requestedType == Optional.class) {
                results = retrievedValues.stream().findFirst();
            } else {
                throw new IllegalArgumentException(
                    "Cannot instantiate value for generic type "+request.requestedType
                );
            }
        } else {
            results = retrievedValues.stream().findFirst().orElse(null);
        }
        return results;
    }




    private boolean extensionMatchName(Object extension, String name) {
        if (name.isEmpty()) return true;
        var annotation = extension.getClass().getAnnotation(Extension.class);
        return name.equals(annotation.name());
    }


    private <E> void tryInjectExtensions(E extension, Field field) {
        try {
            injectExtensionsInField(extension, field);
        } catch (Exception e) {
            logger.warn(
                "Cannot inject value into {}.{} : {}",
                extension.getClass().getCanonicalName(),
                field.getName(),
                e.getMessage()
            );
            logger.debug("{}", e, e);
            if (e instanceof InaccessibleObjectException) {
                logger.warn(
                    "Consider add the following to your module-info.java file:\n\topens {} to {};\n",
                    extension.getClass().getPackage().getName(),
                    ExtensionManager.class.getModule().getName()
                );
            }
        }
    }


    private <E> void injectExtensionsInField(E extension, Field field)
    throws IllegalAccessException, ClassNotFoundException {
        var injectionRequest = requestFromField(field);
        Object value = retrieveInjectableInstances(injectionRequest);
        if (!field.canAccess(extension)) {
            field.setAccessible(true);
        }
        field.set(extension, value);
    }



    private InjectionRequest requestFromField(Field field) throws ClassNotFoundException {
        var effectiveType = effectiveType(field.getType(), field.getGenericType());
        var annotation = field.getAnnotation(Inject.class);
        return new InjectionRequest(
            field.getType(),
            effectiveType,
            annotation == null ? effectiveType.getCanonicalName() : annotation.value(),
            !field.getType().isArray() && effectiveType != field.getType(),
            field.getType().isArray(),
            COLLECTION_TYPES.contains(field.getType()),
            effectiveType.isAnnotationPresent(ExtensionPoint.class)
        );
    }


    private Stream<Field> getAllFields(Class<?> type) {
        if (type == null) {
            return Stream.empty();
        }
        return Stream.concat(Stream.of(type.getDeclaredFields()),getAllFields(type.getSuperclass()));
    }


    private Class<?> effectiveType(Class<?> type, Type genericType) throws ClassNotFoundException {
        if (type.isArray()) {
            return type.componentType();
        }
        if (type == List.class || type == Set.class || type == Collection.class) {
            var genericTypeMatcher = GENERIC_NAME.matcher(genericType.getTypeName());
            if (genericTypeMatcher.matches()) {
                Class<?> clazz = layerProvider.getClass(genericTypeMatcher.group(1));
                if (clazz != null) {
                    return clazz;
                } else {
                    throw new ClassNotFoundException("Cannot find class for type "+genericTypeMatcher.group(1));
                }
            } else {
                throw new ClassNotFoundException("Raw use of parametrized type "+type.getSimpleName());
            }
        }
        return type;
    }




}

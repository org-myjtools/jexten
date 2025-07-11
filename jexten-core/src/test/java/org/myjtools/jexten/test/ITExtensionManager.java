package org.myjtools.jexten.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.myjtools.jexten.*;
import org.junit.jupiter.api.Test;
import org.myjtools.jexten.test.ext.*;

class ITExtensionManager {

    static {
        System.out.println(ITExtensionManager.class.getModule());
    }

    private final ExtensionManager extensionManager = ExtensionManager.create();

    @Test
    void canRetrieveASingleExtension() {
        assertThat(extensionManager.getExtension(SimpleExtensionPoint.class))
            .isNotEmpty()
            .containsInstanceOf(SimpleExtensionPoint.class);
    }


    @Test
    void canRetrieveMultipleExtensions() {
        assertThat(extensionManager.getExtensions(SimpleExtensionPoint.class))
            .anyMatch(it -> it.getClass() == SimpleExtension.class)
            .anyMatch(exactInstanceOf(AnotherSimpleExtension.class));
    }



    @Test
    void filterExtensionsWithIncorrectVersion() {
        assertThat(extensionManager.getExtensions(VersionedExtensionPoint.class))
            .hasSize(1)
            .allMatch(VersionedExtension_2_1.class::isInstance);
    }




    @Test
    void extensionsAreRetrievedInPriorityOrder() {
       Stream<Class<?>>  extensionClasses = extensionManager
           .getExtensions(PriorityExtensionPoint.class).map(Object::getClass);
       assertThat(extensionClasses)
           .hasSize(5)
           .containsExactly(
              HighestPriorityExtension.class,
              HigherPriorityExtension.class,
              NormalPriorityExtension.class,
              LowerPriorityExtension.class,
              LowestPriorityExtension.class
           );
    }


    @Test
    void scopeIsHonouredAlongMultipleCalls() {

        var singletonFirstCall = extensionManager
            .getExtension(SimpleExtensionPoint.class, classEqualTo(SingletonExtension.class))
            .orElseThrow();
        var singletonSecondCall = extensionManager
            .getExtension(SimpleExtensionPoint.class, classEqualTo(SingletonExtension.class))
            .orElseThrow();
        var transientFirstCall = extensionManager
            .getExtension(SimpleExtensionPoint.class, classEqualTo(TransientExtension.class))
            .orElseThrow();
        var transientSecondCall = extensionManager
            .getExtension(SimpleExtensionPoint.class, classEqualTo(TransientExtension.class))
            .orElseThrow();

        assertThat(singletonFirstCall).isSameAs(singletonSecondCall);
        assertThat(transientFirstCall).isNotSameAs(transientSecondCall);

    }


    @Test
    void canRetrieveExtensionUsingExternalLoader() {
        TestingExtensionLoader.lastExtensionLoaded = null;
        var extension = extensionManager
            .getExtension(SimpleExtensionPoint.class, classEqualTo(ExternallyLoadedExtension.class))
            .orElseThrow();
        assertThat(TestingExtensionLoader.lastExtensionLoaded).isSameAs(extension);
    }


    @Test
    void extensionsCanBeInjectedIntoFields() {
        InjectedFieldExtension extension = (InjectedFieldExtension) extensionManager
            .getExtension(SimpleExtensionPoint.class, classEqualTo(InjectedFieldExtension.class))
            .orElseThrow();
        assertThat(extension.injectedExtension()).isNotNull();
        assertThat(extension.injectedCollection()).isNotEmpty();
        assertThat(extension.injectedSet()).isNotEmpty();
        assertThat(extension.injectedList()).isNotEmpty();
        assertThat(extension.injectedArray()).isNotEmpty();
    }



    @Test
    void extensionInjectionAcceptsDependencyLoopsInFields() {
        var extension = (InjectedLoopExtension) extensionManager
            .getExtension(LoopedExtensionPoint.class).orElseThrow();
        assertThat(extension.loop).isSameAs(extension);
        assertThat(((InjectedLoopExtension)extension.loop).loop).isSameAs(extension);
    }



    @Test
    void postConstructExtensionIsCalledAfterInjection() {
        InjectedFieldExtension extension = (InjectedFieldExtension) extensionManager
            .getExtension(SimpleExtensionPoint.class, classEqualTo(InjectedFieldExtension.class))
            .orElseThrow();
        assertThat(extension.postConstructInjectedExtension()).isNotNull();
        assertThat(extension.postConstructInjectedExtension()).isSameAs(extension.injectedExtension());
    }


    @Test
    void externalObjectCanBeInjected() {
        ExternalInjection injection = ()->"stuff";
        var injectedExtensionManager = ExtensionManager.create(ModuleLayerProvider.boot())
            .withInjectionProvider(
                (type,name) -> type == ExternalInjection.class ? Stream.of(injection) : Stream.empty()
            );
        InjectedFieldExtension extension = (InjectedFieldExtension) injectedExtensionManager
            .getExtension(SimpleExtensionPoint.class, classEqualTo(InjectedFieldExtension.class))
            .orElseThrow();
        assertThat(extension.externalInjection().provideExternalStuff()).isEqualTo("stuff");
    }



    private <T> Predicate<? super T> exactInstanceOf(Class<?> type) {
        return it -> it.getClass() == type;
    }


    private Predicate<Class<?>> classEqualTo(Class<?> type) {
        return it -> it == type;
    }


}

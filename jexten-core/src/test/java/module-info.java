import org.myjtools.jexten.test.ext.*;
module org.myjtools.jexten.test {

    requires org.myjtools.jexten;
    requires transitive org.junit.jupiter.api;
    requires transitive org.junit.jupiter.engine;
    requires org.assertj.core;
    requires org.slf4j;
    requires org.slf4j.simple;

    exports org.myjtools.jexten.test to org.myjtools.jexten, org.junit.platform.commons;
    exports org.myjtools.jexten.test.ext to org.myjtools.jexten;
    opens org.myjtools.jexten.test.ext to org.myjtools.jexten;
    opens org.myjtools.jexten.test to org.junit.platform.commons;

    provides SimpleExtensionPoint with
        SimpleExtension,
        AnotherSimpleExtension,
        SingletonExtension,
        ExternallyLoadedExtension,
        InjectedFieldExtension,
        TransientExtension,
        OptionalInjectionExtension;

    provides VersionedExtensionPoint with
        VersionedExtension_1,
        VersionedExtension_2_1,
        VersionedExtension_3;


    provides PriorityExtensionPoint with
        LowerPriorityExtension,
        LowestPriorityExtension,
        NormalPriorityExtension,
        HighestPriorityExtension,
        HigherPriorityExtension;

    provides InjectableExtensionPoint with InjectedExtension, NamedExtension, AnotherNamedExtension;

    provides LoopedExtensionPoint with InjectedLoopExtension;



}
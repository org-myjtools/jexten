package org.myjtools.jexten;

import java.util.stream.Stream;

public interface InjectionProvider {

    static InjectionProvider empty () {
        return (type,name) -> Stream.empty();
    }

    Stream<Object> provideInstancesFor(Class<?> requestedType, String name);

}

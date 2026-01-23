package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;

@Extension
public class OptionalInjectionExtension implements SimpleExtensionPoint {

    @Inject("specific-extension")
    private InjectableExtensionPoint namedInjection;

    public InjectableExtensionPoint getNamedInjection() {
        return namedInjection;
    }

    @Override
    public String provideStuff() {
        return "OptionalInjectionExtension";
    }
}
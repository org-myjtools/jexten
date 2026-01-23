package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;

@Extension(name = "specific-extension")
public class NamedExtension implements InjectableExtensionPoint {

    public String getName() {
        return "specific-extension";
    }
}
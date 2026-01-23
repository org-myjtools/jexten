package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;

@Extension(name = "another-extension")
public class AnotherNamedExtension implements InjectableExtensionPoint {

    public String getName() {
        return "another-extension";
    }
}
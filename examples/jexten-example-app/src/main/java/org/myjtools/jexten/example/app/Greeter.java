package org.myjtools.jexten.example.app;

import org.myjtools.jexten.ExtensionPoint;

@ExtensionPoint
public interface Greeter {

    String greet(String name);

}

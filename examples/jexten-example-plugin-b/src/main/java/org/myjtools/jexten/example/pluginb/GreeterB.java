package org.myjtools.jexten.example.pluginb;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.example.app.Greeter;

@Extension
public class GreeterB implements Greeter {

    @Override
    public void greet(String name) {
        System.out.println("Hello, " + name + " from GreeterB!");
    }

}

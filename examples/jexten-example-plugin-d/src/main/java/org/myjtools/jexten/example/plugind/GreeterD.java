package org.myjtools.jexten.example.plugind;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.example.app.Greeter;

@Extension
public class GreeterD implements Greeter {

    @Override
    public void greet(String name) {
        System.out.println("Hello, " + name + " from GreeterD!");
    }

}

package org.myjtools.jexten.example.plugina;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.example.app.Greeter;

@Extension
public class GreeterA implements Greeter {

    @Override
    public String greet(String name) {
        return "Hello, " + name + " from GreeterA!";
    }

}

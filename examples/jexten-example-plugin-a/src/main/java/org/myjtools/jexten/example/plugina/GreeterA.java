package org.myjtools.jexten.example.plugina;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.example.app.Greeter;

@Extension
public class GreeterA implements Greeter {

    @Override
    public void greet(String name) {
        System.out.println("Hello, " + name + " from GreeterA!");
    }

}

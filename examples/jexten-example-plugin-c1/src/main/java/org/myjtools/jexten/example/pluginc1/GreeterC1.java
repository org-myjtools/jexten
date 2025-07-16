package org.myjtools.jexten.example.pluginc1;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.example.app.Greeter;
import org.myjtools.jexten.example.pluginc.LocaleGreeter;

import java.util.List;

@Extension
public class GreeterC1 implements LocaleGreeter {

    @Override
    public void greetInLanguage(String name) {
        System.out.println("Hello " + name + " from GreeterC1 (in english)!");;
    }

}

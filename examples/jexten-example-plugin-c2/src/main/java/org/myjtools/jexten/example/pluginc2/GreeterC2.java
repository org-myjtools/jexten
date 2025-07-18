package org.myjtools.jexten.example.pluginc2;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.example.app.Greeter;
import org.myjtools.jexten.example.pluginc.LocaleGreeter;

import java.util.List;

@Extension
public class GreeterC2 implements LocaleGreeter {

    @Override
    public void greetInLanguage(String name) {
        System.out.println("¡Hola, " + name + " desde GreeterC2 (en español)!");;
    }

}

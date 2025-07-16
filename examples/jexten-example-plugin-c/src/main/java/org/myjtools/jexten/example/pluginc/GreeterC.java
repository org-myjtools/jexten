package org.myjtools.jexten.example.pluginc;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.example.app.Greeter;

import java.util.List;

@Extension
public class GreeterC implements Greeter {

    @Inject
    List<LocaleGreeter> localeGreeters;

    @Override
    public void greet(String name) {
        localeGreeters.forEach(greet -> greet.greetInLanguage(name));
    }

}

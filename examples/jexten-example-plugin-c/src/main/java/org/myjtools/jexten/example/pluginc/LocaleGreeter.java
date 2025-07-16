package org.myjtools.jexten.example.pluginc;

import org.myjtools.jexten.ExtensionPoint;

@ExtensionPoint
public interface LocaleGreeter {

    void greetInLanguage(String name);

}

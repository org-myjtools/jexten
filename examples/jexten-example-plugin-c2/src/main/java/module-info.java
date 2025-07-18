module org.myjtools.jexten.example.pluginc2 {
    requires org.myjtools.jexten;
    requires org.myjtools.jexten.example.app;
    requires org.myjtools.jexten.example.pluginc;
    exports org.myjtools.jexten.example.pluginc2;
    opens org.myjtools.jexten.example.pluginc2 to org.myjtools.jexten,org.myjtools.jexten.example.pluginc;
    provides org.myjtools.jexten.example.pluginc.LocaleGreeter with org.myjtools.jexten.example.pluginc2.GreeterC2;
}
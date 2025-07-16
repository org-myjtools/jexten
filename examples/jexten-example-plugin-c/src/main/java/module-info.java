module org.myjtools.jexten.example.pluginc {
    requires org.myjtools.jexten;
    requires org.myjtools.jexten.example.app;
    exports org.myjtools.jexten.example.pluginc;
    opens org.myjtools.jexten.example.pluginc to org.myjtools.jexten;
    provides org.myjtools.jexten.example.app.Greeter with org.myjtools.jexten.example.pluginc.GreeterC;
    uses org.myjtools.jexten.example.pluginc.LocaleGreeter;
}
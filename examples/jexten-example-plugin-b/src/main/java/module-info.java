module org.myjtools.jexten.example.pluginb {
    requires org.myjtools.jexten;
    requires org.myjtools.jexten.example.app;
    exports org.myjtools.jexten.example.pluginb;
    opens org.myjtools.jexten.example.pluginb to org.myjtools.jexten;
    provides org.myjtools.jexten.example.app.Greeter with org.myjtools.jexten.example.pluginb.GreeterB;
}
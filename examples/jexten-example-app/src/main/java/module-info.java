module org.myjtools.jexten.example.app {
    requires org.myjtools.jexten;
    requires org.myjtools.jexten.plugin;
    exports org.myjtools.jexten.example.app;
    opens org.myjtools.jexten.example.app to org.myjtools.jexten;
    uses org.myjtools.jexten.example.app.Greeter;
}
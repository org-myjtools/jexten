module org.myjtools.jexten.example.plugina {
    requires org.myjtools.jexten;
    requires org.myjtools.jexten.example.app;
    exports org.myjtools.jexten.example.plugina;
    opens org.myjtools.jexten.example.plugina to org.myjtools.jexten;
    provides org.myjtools.jexten.example.app.Greeter with org.myjtools.jexten.example.plugina.GreeterA;
}
import org.myjtools.jexten.example.plugind.GreeterD;

module org.myjtools.jexten.example.plugind {
    requires org.myjtools.jexten;
    requires org.myjtools.jexten.example.app;
    exports org.myjtools.jexten.example.plugind;
    opens org.myjtools.jexten.example.plugind to org.myjtools.jexten;
    provides org.myjtools.jexten.example.app.Greeter with GreeterD;
}
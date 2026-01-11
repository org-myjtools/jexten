package org.myjtools.jexten.plugin;

import java.io.Serial;

public class PluginException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 2029625435486652147L;


    public static PluginException wrapper (Exception e) {
        if (e instanceof PluginException ex) {
            return ex;
        }
        return new PluginException(e);
    }


    public PluginException(String message, Object... args) {
        super(message.replace("{}", "%s").formatted(args));
    }


    private PluginException(Exception e) {
        super(e);
    }


    public PluginException(Exception e, String message, Object... args) {
        super(String.format(message.replace("{}", "%s"), args),e);
    }

}
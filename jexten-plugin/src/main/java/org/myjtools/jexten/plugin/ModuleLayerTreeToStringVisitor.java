package org.myjtools.jexten.plugin;

public class ModuleLayerTreeToStringVisitor implements ModuleLayerTreeVisitor {

    private final StringBuilder description = new StringBuilder();

    public static String toString(ModuleLayerTree tree) {
        var visitor = new ModuleLayerTreeToStringVisitor();
        tree.forEach(visitor);
        return visitor.description.toString();
    }

    protected void append(String text, Object...args) {
        description.append(text.formatted(args)).append("\n");
    }


    @Override
    public void enterLayer(ModuleLayer layer, PluginManifest plugin, int depth) {
        if (depth == 0) {
            append("[ ApplicationLayer ]");
        } else {
            String margin = "  "+"    ".repeat(depth-1);
            append(
                    "%s[ %s (%s version %s) ]",
                    margin,
                    plugin.name(),
                    plugin.id(),
                    plugin.version()
            );
        }
    }


    @Override
    public void exitLayer(ModuleLayer layer, PluginManifest plugin, int depth) {
        if (depth == 0) {
            append("[ ----------------- ]");
        } else {
            String margin = "  "+"    ".repeat(depth-1);
            String text = "%s (%s version %s)".formatted(
                    plugin.name(),
                    plugin.id(),
                    plugin.version()
            );
            append(
                    "%s[ %s ]",
                    margin,
                    "-".repeat(text.length())
            );
        }
    }

    @Override
    public void visitModule(ModuleLayer layer, PluginManifest plugin, int depth, Module module) {
        var margin = (depth == 0 ? "  ":"") + "    ".repeat(depth);
        append("%s- %s",margin,module.getName());
    }






}

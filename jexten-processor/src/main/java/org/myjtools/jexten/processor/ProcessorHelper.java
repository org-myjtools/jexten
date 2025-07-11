package org.myjtools.jexten.processor;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.*;
import javax.tools.*;
import javax.tools.Diagnostic.Kind;


class ProcessorHelper {

    private final Filer filer;
    private final Messager messager;
    private final Elements elementUtils;
    private final Types typeUtils;
    private final RoundEnvironment roundEnv;



    ProcessorHelper(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv) {
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.roundEnv = roundEnv;
    }


    void log(Kind kind, String message, Object... messageArgs) {
        messager.printMessage(
            kind,
            "[jexten] :: " + formatted(message, messageArgs)
        );
    }


    void log(Kind kind, Element element, String message, Object... messageArgs) {
        messager.printMessage(
            kind,
            "[jexten] [" + element + "] " + formatted(message, messageArgs)
        );
    }


    private String formatted(String message, Object[] messageArgs) {
        return message.replace("{}", "%s").formatted(messageArgs);
    }


    void writeMetaInfFile(String filename, List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        FileObject resourceFile = null;
        try {
            resourceFile = filer.createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "META-INF/"+filename
            );
            try (BufferedWriter writer = new BufferedWriter(resourceFile.openWriter())) {
                for (String line : lines) {
                    writer.append(line);
                    writer.newLine();
                }
            }
        } catch (IOException | IllegalStateException e) {
            log(Kind.ERROR, "Cannot write file {} : {}", resourceFile, e.toString());
        }
    }




    public Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> type) {
        return roundEnv.getElementsAnnotatedWith(type);
    }

    @SafeVarargs
    public final Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation>... types) {
        return roundEnv.getElementsAnnotatedWithAny(new HashSet<>(Arrays.asList(types)));
    }


    public ModuleElement getModuleOf(Element element) {
        return elementUtils.getModuleOf(element);
    }


    public Iterable<? extends TypeMirror> directSupertypes(TypeMirror type) {
        return typeUtils.directSupertypes(type);
    }


    public TypeElement getTypeElement(String name) {
        return elementUtils.getTypeElement(name);
    }

}

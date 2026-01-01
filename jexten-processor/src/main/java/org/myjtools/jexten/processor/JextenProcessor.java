package org.myjtools.jexten.processor;


import static java.util.stream.Collectors.joining;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.element.ModuleElement.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import org.myjtools.jexten.*;

/**
 * An extension processor that validate and publish the provided extensions
 */
@SupportedAnnotationTypes({ "org.myjtools.jexten.Extension", "org.myjtools.jexten.ExtensionPoint" })
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class JextenProcessor extends AbstractProcessor {

    public static final String MODULE = "org.myjtools.jexten";


    private ProcessorHelper helper;
    private Errors errors;
    private ProcessorValidator validator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        processingEnv.getMessager().printMessage(Kind.NOTE, "[jexten] :: JExten Processor initialized");
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (annotations.isEmpty()) {
            return true;
        }

        this.helper = new ProcessorHelper(processingEnv, roundEnv);
        this.errors = new Errors();
        this.validator = new ProcessorValidator(MODULE, helper, processingEnv, errors);

        helper.log(Kind.NOTE, "Processing annotations {}", annotations);

        if (!validator.validateModuleInfoExists()) {
            return false;
        }

        Map<String, List<String>> serviceImplementations = new LinkedHashMap<>();
        validateAndRegisterExtensions(serviceImplementations);
        validator.validateExtensionPoints();
        validateModule(serviceImplementations);
        showErrors();
        if (!errors.hasMessages()) {
            writeExtensionsFile(serviceImplementations);
            var extensionPoints = helper.getElementsAnnotatedWith(ExtensionPoint.class).stream()
                    .map(Object::toString)
                    .toList();
            writeExtensionPointsFile(extensionPoints);

        }
        return errors.hasMessages();
    }


    void validateAndRegisterExtensions(Map<String, List<String>> serviceImplementations) {
        for (Element extensionElement : helper.getElementsAnnotatedWith(Extension.class)) {
            if (validator.validateElementKindIsClass(extensionElement)) {
                validateAndRegisterExtension((TypeElement) extensionElement, serviceImplementations);
            }
        }
    }

    private void validateModule(Map<String, List<String>> serviceImplementations) {
        var elements = helper.getElementsAnnotatedWith(
            ExtensionPoint.class,
            Extension.class
        );
        if (elements.isEmpty()) {
            return;
        }
        var module = helper.getModuleOf(elements.iterator().next());

        var extensionPoints = helper.getElementsAnnotatedWith(ExtensionPoint.class);
        if (!extensionPoints.isEmpty()) {
            var opens = directives(module, OpensDirective.class);
            var exports = directives(module, ExportsDirective.class);
            var uses = directives(module, UsesDirective.class);
            for (var extensionPoint : extensionPoints) {
                validator.validateModuleExtensionPoint((TypeElement) extensionPoint, exports, uses, opens);
            }
        }

        var provides = directives(module, ProvidesDirective.class);
        var exports = directives(module, ExportsDirective.class);
        for (var entry : serviceImplementations.entrySet()) {
            validator.validateModuleExtension(entry.getKey(), entry.getValue(), provides, exports);
        }

    }





    private <T extends Directive> List<T> directives(ModuleElement module, Class<T> type) {
        return module.getDirectives().stream().filter(type::isInstance).map(type::cast).toList();
    }


    private void validateAndRegisterExtension(
        TypeElement extensionElement,
        Map<String, List<String>> serviceImplementations
    ) {

        boolean ignore;
        var extensionAnnotation = extensionElement.getAnnotation(Extension.class);

        ignore = !validator.validateVersionFormat(
            extensionAnnotation.extensionPointVersion(),
            extensionElement,
            "extensionPointVersion"
        );

        if (ignore) {
            return;
        }

        String extensionPointName = computeExtensionPointName(
            extensionElement,
            extensionAnnotation
        );
        String extensionName = extensionElement.getQualifiedName().toString();
        TypeElement extensionPointElement = helper.getTypeElement(extensionPointName);
        ExtensionInfo extensionInfo = new ExtensionInfo(
            extensionElement, extensionName, extensionPointElement, extensionPointName
        );

        ignore = !validator.validateExtensionPointClassExists(extensionInfo);
        ignore = ignore || !validator.validateExtensionPointAnnotation(extensionInfo);
        ignore = ignore || !validator.validateExtensionPointAssignableFromExtension(extensionInfo);

        if (!ignore) {
            serviceImplementations
                .computeIfAbsent(extensionPointName, x -> new ArrayList<>())
                .add(extensionName);
        }

    }





    private String computeExtensionPointName(
        TypeElement extensionClassElement,
        Extension extensionAnnotation
    ) {
        String extensionPointName = extensionAnnotation.extensionPoint();
        if (extensionPointName.isEmpty()) {
            for (TypeMirror implementedInterface : extensionClassElement.getInterfaces()) {
                extensionPointName = implementedInterface.toString();
                // remove the <..> part in case it is a generic class
                extensionPointName = extensionPointName.replaceAll("<[^>]*>", "");
            }
        }
        return extensionPointName;
    }




    private void writeExtensionPointsFile(List<String> extensionPoints) {
        helper.writeMetaInfFile(
            "extension-points",
            extensionPoints
        );
    }


    private void writeExtensionsFile(Map<String, List<String>> serviceImplementations) {
        helper.writeMetaInfFile(
            "extensions",
            serviceImplementations.entrySet().stream().map(
                e -> e.getKey()+"="+String.join(",", e.getValue())
            ).toList()
        );
    }









    private void showErrors() {
        if (errors.hasMessages()) {
            errors.messages().forEach((element,messages)->
                messages.forEach(it -> helper.log(Kind.ERROR, element,it))
            );
        }
        if (errors.hasFixes()) {
            String fixes = errors.fixes().collect(joining("\n\t","\n\t",""));
            helper.log(Kind.ERROR, "Try to apply the following fixes to the module-info.java file:");
            helper.log(Kind.ERROR, fixes);
        }
    }

}

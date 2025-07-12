package org.myjtools.jexten.processor;


import java.util.stream.*;
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

    static {
        System.out.println("Jexten Processor");
    }


    private record ExtensionInfo(
        TypeElement extensionElement,
        String extensionName,
        TypeElement extensionPointElement,
        String extensionPointName
    ) { }


    private ProcessorHelper helper;
    private Errors errors;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (annotations.isEmpty()) {
            return true;
        }

        this.helper = new ProcessorHelper(processingEnv, roundEnv);
        this.errors = new Errors();
        helper.log(Kind.NOTE, "Processing annotations {}", annotations);

        if (!validateModuleInfoExists()) {
            return false;
        }

        Map<String, List<String>> serviceImplementations = new LinkedHashMap<>();
        validateAndRegisterExtensions(serviceImplementations);
        validateExtensionPoints();
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





    private boolean validateModuleInfoExists() {
        for (var type : List.of(Extension.class, ExtensionPoint.class)) {
            for (Element element : helper.getElementsAnnotatedWith(type)) {
                var module = processingEnv.getElementUtils().getModuleOf(element);
                if (module == null || module.isUnnamed()) {
                    helper.log(
                        Kind.ERROR,
                        "Cannot find module definition. Ensure module-info.java exists"
                    );
                    return false;
                }
            }
        }
        return true;
    }


    private void validateAndRegisterExtensions(Map<String, List<String>> serviceImplementations) {
        for (Element extensionElement : helper.getElementsAnnotatedWith(Extension.class)) {
            if (validateElementKindIsClass(extensionElement)) {
                validateAndRegisterExtension(
                    (TypeElement) extensionElement,
                    serviceImplementations
                );
            }
        }
    }


    private void validateExtensionPoints() {
        var extensionPoints = helper.getElementsAnnotatedWith(ExtensionPoint.class);
        for (Element extensionPointElement : extensionPoints) {
            validateExtensionPoint(extensionPointElement);
        }
    }


    private void validateExtensionPoint(Element extensionPointElement) {
        if (extensionPointElement.getKind() != ElementKind.INTERFACE) {
            errors.addMessage(
                extensionPointElement,
                "@ExtensionPoint not valid for '{}' (only processed for interfaces)",
                extensionPointElement.getSimpleName()
            );
        } else {
            var annotation = extensionPointElement.getAnnotation(ExtensionPoint.class);
            validateVersionFormat(annotation.version(), extensionPointElement, "version");
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
                validateModuleExtensionPoint((TypeElement) extensionPoint, exports, uses, opens);
            }
        }

        var provides = directives(module, ProvidesDirective.class);
        var exports = directives(module, ExportsDirective.class);
        for (var entry : serviceImplementations.entrySet()) {
            validateModuleExtension(entry.getKey(), entry.getValue(), provides, exports);
        }

    }


    private void validateModuleExtensionPoint(
        TypeElement extensionPoint,
        List<ExportsDirective> exports,
        List<UsesDirective> uses,
        List<OpensDirective> opens
    ) {
        var extensionPointPackage = packageElementOf(extensionPoint);
        if (noPackageMatchExtensionPointPackage(exports, extensionPointPackage)) {
            errors.addMessage(
                extensionPointPackage,
                "Extension point package {} must be exported in the module-info.java file",
                extensionPointPackage
            );
            errors.addFix("exports {};",extensionPointPackage);
        }
        if (noOpensMatchExtensionPointPackage(opens, extensionPointPackage)) {
            errors.addMessage(
                extensionPointPackage,
                "Extension point package {} must be opened to {} in the module-info.java file",
                extensionPointPackage,
                    MODULE
            );
            errors.addFix("opens {} to {};",extensionPointPackage, MODULE);
        }
        if (extensionPointNotDeclaredInUses(uses, extensionPoint)) {
            errors.addMessage(
                extensionPoint,
                "Usage of extension point {} must be declared in the module-info.java file",
                extensionPoint.getQualifiedName()
            );
            errors.addFix("uses {};",extensionPoint.getQualifiedName());
        }
    }




    private boolean noPackageMatchExtensionPointPackage(
        List<ExportsDirective> exports,
        PackageElement extensionPointPackage
    ) {
        return exports
            .stream()
            .map(ExportsDirective::getPackage)
            .noneMatch(extensionPointPackage::equals);
    }


    private boolean noOpensMatchExtensionPointPackage(
        List<OpensDirective> opens,
        PackageElement extensionPointPackage
    ) {
        return opens
            .stream()
            .filter(it -> it.getPackage().equals(extensionPointPackage))
            .flatMap(it -> it.getTargetModules().stream())
            .noneMatch(it -> it.getQualifiedName().toString().equals(MODULE));
    }


    private boolean extensionPointNotDeclaredInUses(List<UsesDirective> uses, TypeElement extensionPoint) {
        return uses.stream()
            .map(it->it.getService().getQualifiedName())
            .noneMatch(extensionPoint.getQualifiedName()::equals);
    }



    private void validateModuleExtension(
        String extensionPoint,
        List<String> extensions,
        List<ProvidesDirective> provides,
        List<ExportsDirective> exports
    ) {

        var exportedPackages = exports
            .stream()
            .map(ExportsDirective::getPackage)
            .map(PackageElement::getQualifiedName)
            .map(Name::toString)
            .toList();

        var implementations = provides
            .stream()
            .filter(directive -> nameEquals(directive.getService(), extensionPoint))
            .flatMap(directive -> directive.getImplementations().stream())
            .map(TypeElement::getQualifiedName)
            .map(Object::toString)
            .toList();

        var nonDeclared = extensions
            .stream()
            .filter(extension -> !implementations.contains(extension))
            .toList();

        var nonExported = extensions
            .stream()
            .filter(extension -> !exportedPackages.contains(extension.substring(0,extension.lastIndexOf('.'))))
            .toList();

        if (!nonDeclared.isEmpty()) {
            for (String extension : nonDeclared) {
                errors.addMessage(                    helper.getTypeElement(extension),
                    "Extension {} implementing extension point {} must be declared in the module-info.java file",
                    extension,
                    extensionPoint
                );
            }
            var fix = new StringBuilder()
                .append("provides {} with ");
            if (implementations.isEmpty() && nonDeclared.size() == 1) {
                fix.append(extensions.getFirst()).append(";\n\n");
            } else {
                fix.append(Stream.concat(
                    implementations.stream(),
                    nonDeclared.stream()
                ).sorted().collect(joining(",\n", "\n", ";\n\n")));
            }
            errors.addFix(fix.toString(), extensionPoint);
        }

        nonExported.forEach(extension -> {
            String pack = extension.substring(0,extension.lastIndexOf('.'));
            errors.addMessage(
                helper.getTypeElement(extension),
                "Package {} containing extension {} must be exported in the module-info.java file",
                pack,
                extension
            );
            errors.addFix("exports {};", pack);
        });



    }


    private boolean nameEquals(TypeElement element, String name) {
        return element.getQualifiedName().contentEquals(name);
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

        ignore = !validateVersionFormat(
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

        ignore = !validateExtensionPointClassExists(extensionInfo);
        ignore = ignore || !validateExtensionPointAnnotation(extensionInfo);
        ignore = ignore || !validateExtensionPointAssignableFromExtension(extensionInfo);

        if (!ignore) {
            serviceImplementations
                .computeIfAbsent(extensionPointName, x -> new ArrayList<>())
                .add(extensionName);
        }

    }


    private boolean validateExtensionPointAssignableFromExtension(ExtensionInfo extensionInfo) {
        if (!isAssignable(
            extensionInfo.extensionElement.asType(),
            extensionInfo.extensionPointElement.asType()
        )) {
            helper.log(
                Kind.ERROR,
                extensionInfo.extensionElement,
                "{} must implement or extend the extension point type {}",
                extensionInfo.extensionName,
                extensionInfo.extensionPointName
            );
            return false;
        }
        return true;
    }


    private boolean validateExtensionPointAnnotation(ExtensionInfo extensionInfo) {
        if (extensionInfo.extensionPointElement.getAnnotation(ExtensionPoint.class) == null) {
            helper.log(
                Kind.ERROR,
                extensionInfo.extensionElement,
                "Expected extension point type '{}' is not annotated with @ExtensionPoint",
                extensionInfo.extensionPointName
            );
            return false;
        }
        return true;
    }


    private boolean validateExtensionPointClassExists(ExtensionInfo extensionInfo) {
        if (extensionInfo.extensionPointElement == null) {
            helper.log(
                Kind.ERROR,
                extensionInfo.extensionElement,
                "Cannot find extension point class '{}'",
                extensionInfo.extensionPointName
            );
            return false;
        }
        return true;
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


    boolean validateElementKindIsClass(Element element) {
        if (element.getKind() != ElementKind.CLASS) {
            helper.log(
                Kind.WARNING,
                element,
                "@Extension ignored for {} (only processed for classes)",
                element.getSimpleName()
            );
            return false;
        }
        return true;
    }


    private boolean validateVersionFormat(String version, Element element, String fieldName) {
        boolean valid = Version.validate(version);
        if (!valid) {
            helper.log(
                Kind.ERROR,
                element,
                "Content of field {} ('{}') must be in form of semantic version: '<major>.<minor>[.<patch>]'",
                fieldName,
                version
            );
        }
        return valid;
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

    private boolean isAssignable(TypeMirror type, TypeMirror typeTo) {
        if (nameWithoutGeneric(type).equals(nameWithoutGeneric(typeTo))) {
            return true;
        }
        for (TypeMirror superType : helper.directSupertypes(type)) {
            if (isAssignable(superType, typeTo)) {
                return true;
            }
        }
        return false;
    }


    private String nameWithoutGeneric(TypeMirror type) {
        int genericPosition = type.toString().indexOf('<');
        return genericPosition < 0 ? type.toString()
            : type.toString().substring(0, genericPosition);
    }


    private PackageElement packageElementOf(Element extensionPoint) {
        return processingEnv.getElementUtils().getPackageOf(extensionPoint);
    }


    private void showErrors() {
        if (errors.hasMessages()) {
            errors.messages().forEach((element,messages)->{
                messages.forEach(it -> helper.log(Kind.ERROR, element,it));
            });
        }
        if (errors.hasFixes()) {
            String fixes = errors.fixes().collect(joining("\n\t","\n\t",""));
            helper.log(Kind.ERROR, "Try to apply the following fixes to the module-info.java file:");
            helper.log(Kind.ERROR, fixes);
        }
    }

}

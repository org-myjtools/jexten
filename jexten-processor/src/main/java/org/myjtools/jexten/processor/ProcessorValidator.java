package org.myjtools.jexten.processor;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.jexten.Version;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.stream.Stream;
import static java.util.stream.Collectors.joining;

public class ProcessorValidator {

    private final ProcessorHelper helper;
    private final ProcessingEnvironment processingEnv;
    private final Errors errors;
    private final String moduleName;

    ProcessorValidator(
        String moduleName,
        ProcessorHelper helper,
        ProcessingEnvironment processingEnv,
        Errors errors
    ) {
        this.moduleName = moduleName;
        this.processingEnv = processingEnv;
        this.helper = helper;
        this.errors = errors;
    }


    boolean validateModuleInfoExists() {
        for (var type : List.of(Extension.class, ExtensionPoint.class)) {
            for (Element element : helper.getElementsAnnotatedWith(type)) {
                var module = processingEnv.getElementUtils().getModuleOf(element);
                if (module == null || module.isUnnamed()) {
                    helper.log(
                        Diagnostic.Kind.ERROR,
                        "Cannot find module definition. Ensure module-info.java exists"
                    );
                    return false;
                }
            }
        }
        return true;
    }


    void validateExtensionPoints() {
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

    void validateModuleExtensionPoint(
            TypeElement extensionPoint,
            List<ModuleElement.ExportsDirective> exports,
            List<ModuleElement.UsesDirective> uses,
            List<ModuleElement.OpensDirective> opens
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
                moduleName
            );
            errors.addFix("opens {} to {};",extensionPointPackage, moduleName);
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
        List<ModuleElement.ExportsDirective> exports,
        PackageElement extensionPointPackage
    ) {
        return exports
            .stream()
            .map(ModuleElement.ExportsDirective::getPackage)
            .noneMatch(extensionPointPackage::equals);
    }


    private boolean noOpensMatchExtensionPointPackage(
            List<ModuleElement.OpensDirective> opens,
            PackageElement extensionPointPackage
    ) {
        return opens
            .stream()
            .filter(it -> it.getPackage().equals(extensionPointPackage))
            .flatMap(it -> it.getTargetModules().stream())
            .noneMatch(it -> it.getQualifiedName().toString().equals(moduleName));
    }


    private boolean extensionPointNotDeclaredInUses(List<ModuleElement.UsesDirective> uses, TypeElement extensionPoint) {
        return uses.stream()
                .map(it->it.getService().getQualifiedName())
                .noneMatch(extensionPoint.getQualifiedName()::equals);
    }


    void validateModuleExtension(
            String extensionPoint,
            List<String> extensions,
            List<ModuleElement.ProvidesDirective> provides,
            List<ModuleElement.ExportsDirective> exports
    ) {

        var exportedPackages = exports
                .stream()
                .map(ModuleElement.ExportsDirective::getPackage)
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
                errors.addMessage(
                    helper.getTypeElement(extension),
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


    boolean validateExtensionPointAssignableFromExtension(ExtensionInfo extensionInfo) {
        if (!isAssignable(
            extensionInfo.extensionElement().asType(),
            extensionInfo.extensionPointElement().asType()
        )) {
            helper.log(
                Diagnostic.Kind.ERROR,
                extensionInfo.extensionElement(),
                "{} must implement or extend the extension point type {}",
                extensionInfo.extensionName(),
                extensionInfo.extensionPointName()
            );
            return false;
        }
        return true;
    }


    boolean validateExtensionPointAnnotation(ExtensionInfo extensionInfo) {
        if (extensionInfo.extensionPointElement().getAnnotation(ExtensionPoint.class) == null) {
            helper.log(
                Diagnostic.Kind.ERROR,
                extensionInfo.extensionElement(),
                "Expected extension point type '{}' is not annotated with @ExtensionPoint",
                extensionInfo.extensionPointName()
            );
            return false;
        }
        return true;
    }


    boolean validateExtensionPointClassExists(ExtensionInfo extensionInfo) {
        if (extensionInfo.extensionPointElement() == null) {
            helper.log(
                Diagnostic.Kind.ERROR,
                extensionInfo.extensionElement(),
                "Cannot find extension point class '{}'",
                extensionInfo.extensionPointName()
            );
            return false;
        }
        return true;
    }

    boolean validateElementKindIsClass(Element element) {
        if (element.getKind() != ElementKind.CLASS) {
            helper.log(
                Diagnostic.Kind.WARNING,
                element,
                "@Extension ignored for {} (only processed for classes)",
                element.getSimpleName()
            );
            return false;
        }
        return true;
    }


    boolean validateVersionFormat(String version, Element element, String fieldName) {
        boolean valid = Version.validate(version);
        if (!valid) {
            helper.log(
                Diagnostic.Kind.ERROR,
                element,
                "Content of field {} ('{}') must be in form of semantic version: '<major>.<minor>[.<patch>]'",
                fieldName,
                version
            );
        }
        return valid;
    }

    private boolean nameEquals(TypeElement element, String name) {
        return element.getQualifiedName().contentEquals(name);
    }

    private PackageElement packageElementOf(Element extensionPoint) {
        return processingEnv.getElementUtils().getPackageOf(extensionPoint);
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
}

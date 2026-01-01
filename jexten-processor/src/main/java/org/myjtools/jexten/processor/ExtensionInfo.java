package org.myjtools.jexten.processor;

import javax.lang.model.element.TypeElement;

record ExtensionInfo(
        TypeElement extensionElement,
        String extensionName,
        TypeElement extensionPointElement,
        String extensionPointName
) {
}

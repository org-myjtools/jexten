package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Priority;

@Extension(priority = Priority.HIGHER)
public class HigherPriorityExtension implements PriorityExtensionPoint {
}

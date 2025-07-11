// Copyright  (c) 2021 -  Luis Iñesta Gelabert  <luiinge@gmail.com>

package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Priority;

@Extension(priority = Priority.LOWER)
public class LowerPriorityExtension implements PriorityExtensionPoint {
}

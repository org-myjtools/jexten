package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.Scope;

@Extension(scope = Scope.SINGLETON)
public class InjectedLoopExtension implements LoopedExtensionPoint {

	@Inject
	public LoopedExtensionPoint loop;

}

package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.test.TestingExtensionLoader;

@Extension(loadedWith = TestingExtensionLoader.class)
public class ExternallyLoadedExtension implements SimpleExtensionPoint {

	@Override
	public String provideStuff() {
		return "Stuff from ExternallyLoadedExtension";
	}

}

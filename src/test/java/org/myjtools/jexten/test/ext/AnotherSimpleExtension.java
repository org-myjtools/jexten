package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;

@Extension
public class AnotherSimpleExtension implements SimpleExtensionPoint {

	@Override
	public String provideStuff() {
		return "Stuff from AnotherSimpleExtension";
	}

}

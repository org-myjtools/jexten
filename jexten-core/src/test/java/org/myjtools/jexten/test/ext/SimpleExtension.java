// Copyright  (c) 2022 -  Luis Iñesta Gelabert  <luiinge@gmail.com>

package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;

@Extension
public class SimpleExtension implements SimpleExtensionPoint {

	@Override
	public String provideStuff() {
		return "Stuff from SimpleExtension";
	}

}

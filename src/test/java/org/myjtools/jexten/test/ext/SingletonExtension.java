// Copyright  (c) 2022 -  Luis Iñesta Gelabert  <luiinge@gmail.com>

package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;

@Extension(scope = Scope.SINGLETON)
public class SingletonExtension implements SimpleExtensionPoint {

	@Override
	public String provideStuff() {
		return "Stuff from SingletonExtension";
	}

}

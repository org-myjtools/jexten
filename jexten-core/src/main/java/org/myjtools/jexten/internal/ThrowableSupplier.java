// Copyright  (c) 2022 -  Luis Iñesta Gelabert  <luiinge@gmail.com>

package org.myjtools.jexten.internal;


import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowableSupplier<T,E extends Exception> {

	static <T> ThrowableSupplier<T,RuntimeException> of(Supplier<T> supplier) {
		return supplier::get;
	}

	T get() throws E;

}

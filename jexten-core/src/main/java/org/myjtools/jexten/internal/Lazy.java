// Copyright  (c) 2022 -  Luis Iñesta Gelabert  <luiinge@gmail.com>

package org.myjtools.jexten.internal;

public class Lazy<T,E extends Exception> {

	public static <T,E extends Exception> Lazy<T,E> of (ThrowableSupplier<T,E> supplier) {
		return new Lazy<>(supplier);
	}


	private final ThrowableSupplier<T,E> supplier;
	private T instance;

	private Lazy(ThrowableSupplier<T, E> supplier) {
		this.supplier = supplier;
	}


	public T get() throws E {
		if (instance == null) {
			instance = supplier.get();
		}
		return instance;
	}


	public void reset() {
		instance = null;
	}

}

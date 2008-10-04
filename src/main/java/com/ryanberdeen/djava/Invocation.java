package com.ryanberdeen.djava;

import java.lang.reflect.Method;

public abstract class Invocation<T> {
	protected T target;
	protected Method method;
	protected Object[] arguments;

	Invocation(T target, Method method, Object[] arguments) {
		this.arguments = arguments;
		this.method = method;
		this.target = target;
	}

	public T getTarget() {
		return target;
	}

	public Method getMethod() {
		return method;
	}

	public Object[] getArguments() {
		return arguments;
	}
}

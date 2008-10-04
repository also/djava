package com.ryanberdeen.djava;

import java.lang.reflect.Method;

public class RemoteInvocation extends Invocation<RemoteObjectReference> {
	public RemoteInvocation(RemoteObjectReference target, Method method, Object[] arguments) {
		super(target, method, arguments);
	}

	public Integer getTargetId() {
		return target.getId();
	}

	public String getMethodName() {
		return method.getName();
	}

	public Class<?>[] getParameterTypes() {
		return method.getParameterTypes();
	}

	public boolean isAsynchronous() {
		return method.getAnnotation(Asynchronous.class) != null;
	}
}

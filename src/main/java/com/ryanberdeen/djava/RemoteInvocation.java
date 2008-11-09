/*
 * Copyright 2008 Ryan Berdeen.
 *
 * This file is part of dJava.
 *
 * dJava is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dJava is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with dJava.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ryanberdeen.djava;

import java.lang.reflect.Method;

public class RemoteInvocation {
	private RemoteObjectReference target;
	private Method method;
	private Object[] arguments;

	RemoteInvocation(RemoteObjectReference target, Method method, Object[] arguments) {
		this.arguments = arguments;
		this.method = method;
		this.target = target;
	}

	public Integer getTargetId() {
		return target.getId();
	}

	public String getMethodName() {
		return method.getName();
	}

	public Object[] getArguments() {
		return arguments;
	}

	public Class<?>[] getParameterTypes() {
		return method.getParameterTypes();
	}

	public boolean isAsynchronous() {
		return method.getAnnotation(Asynchronous.class) != null;
	}
}

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Stores values necessary to invoke a method on a target.
 *
 */
public class Invocation {
	private Object target;
	private Method method;
	private Object[] arguments;

	Invocation(Object target, Method method, Object[] arguments) {
		this.arguments = arguments;
		this.method = method;
		this.target = target;
	}

	public Object invoke() throws InvocationTargetException, IllegalAccessException {
		// TODO why is this necessary
		method.setAccessible(true);

		return method.invoke(target, arguments);
	}

	public Object getTarget() {
		return target;
	}

	public Method getMethod() {
		return method;
	}

	public Object[] getArguments() {
		return arguments;
	}
}

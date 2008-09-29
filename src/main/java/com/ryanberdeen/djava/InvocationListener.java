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

/** Listener interface for method invocation events.
 * @author rberdeen
 *
 * @param <T>
 */
public interface InvocationListener<T> {
	
	/** Called before the method is invoked.
	 * @param target target of the method call
	 * @param method method being called
	 * @param args arguments to the method
	 * @return an object to pass to afterMethodInvocation
	 */
	public T beforeMethodInvocation(Object target, Method method, Object[] args);
	
	/** Called after the method is invoked.
	 * @param before the return value of beforeMethodInvocation
	 * @param result the result of the method call
	 */
	public void afterMethodInvocation(T before, Object result);
}

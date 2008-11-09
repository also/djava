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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/** Stores values necessary to invoke a method on a target.
 *
 */
public abstract class LocalInvocation {
	private long requestingThreadId;
	private Long targetThreadId;
	protected DJavaConnection dJavaConnection;
	protected Integer targetId;
	protected String methodName;
	private Class<?>[] parameterTypes;
	protected Object[] argumentSpecifications;
	@SuppressWarnings("unchecked")
	private InvocationListener invocationListener;

	public LocalInvocation(DJavaConnection dJavaConnection, long requestingThreadId, Long targetThreadId, Integer targetId, String methodName, Class<?>[] parameterTypes, Object[] argumentSpecifications, InvocationListener<?> invocationListener) {
		this.dJavaConnection = dJavaConnection;
		this.requestingThreadId = requestingThreadId;
		this.targetThreadId = targetThreadId;
		this.argumentSpecifications = argumentSpecifications;
		this.methodName = methodName;
		this.targetId = targetId;
		this.parameterTypes = parameterTypes;
		this.invocationListener = invocationListener;
	}

	Long getTargetThreadId() {
		return targetThreadId;
	}

	void invoke() throws Throwable {
		Long outerRequestingThreadId = dJavaConnection.getRequestingThreadId();
		dJavaConnection.setRequestingThreadId(requestingThreadId);

		try {
			Object result;

			Object target = dJavaConnection.getTarget(targetId);
			if (target == null) {
				throw new TargetNotFoundException(targetId, methodName);
			}

			Class<?> targetClass = target.getClass();
			Method method = targetClass.getMethod(methodName, parameterTypes);

			Object[] arguments = buildArguments();

			// TODO why is this necessary
			method.setAccessible(true);

			try {
				if (invocationListener != null) {
					/* if the method is synchronized, the order in which it is entered is significant */
					if (Modifier.isSynchronized(method.getModifiers())) {
						/* make sure that events for target are logged in the order they actually happen */
						synchronized (target) {
							result = invokeAndNotify(target, method, arguments);
						}
					}
					else {
						/* if the method isn't synchronized, order doesn't matter */
						result = invokeAndNotify(target, method, arguments);
					}
				}
				else {
					result = method.invoke(target, arguments);
				}
			}
			catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof Exception) {
					handleThrowable(cause);
					return;
				}
				else {
					throw cause;
				}
			}

			Class<?> returnType = method.getReturnType();
			handleResult(toResponse(returnType, result));
		}
		catch (Exception ex) {
			handleInternalThrowable(ex);
		}
		catch (Throwable ex) {
			throw ex;
		}
		finally {
			dJavaConnection.setRequestingThreadId(outerRequestingThreadId);
		}
	}

	protected abstract void handleResult(Serializable result) throws Exception;
	protected abstract void handleThrowable(Throwable t) throws Exception;
	protected abstract void handleInternalThrowable(Throwable t);

	/** Invokes the method on the target, notifying the listener before and after.
	 */
	@SuppressWarnings("unchecked")
	private Object invokeAndNotify(Object target, Method method, Object[] arguments) throws InvocationTargetException, IllegalAccessException {
		Object before = invocationListener.beforeMethodInvocation(target, method, arguments);
		Object result = method.invoke(target, arguments);
		invocationListener.afterMethodInvocation(before, result);

		return result;
	}

	private Object[] buildArguments() {
		Object[] result = new Object[argumentSpecifications.length];
		for (int i = 0; i < result.length; i++) {
			if (argumentSpecifications[i] instanceof ObjectDescriptor) {
				// TODO
				result[i] = dJavaConnection.getProxy((ObjectDescriptor) argumentSpecifications[i]);
			}
			else if (argumentSpecifications[i] instanceof RemoteObjectReference) {
				result[i] = dJavaConnection.getTarget(((RemoteObjectReference) argumentSpecifications[i]).getId());
			}
			else {
				result[i] = argumentSpecifications[i];
			}
		}
		return result;
	}

	private Serializable toResponse(Class<?> returnType, Object result) throws Exception {
		if (result != null) {
			// TODO probably shouldn't serialize enums
			// TODO serialize wrapper types when method doesn't return primitive
			if (returnType == String.class || returnType.isPrimitive() || Enum.class.isAssignableFrom(returnType)) {
				return (Serializable) result;
			}
			else {
				return dJavaConnection.getObjectDescriptor(result);
			}
		}
		else {
			return null;
		}
	}
}

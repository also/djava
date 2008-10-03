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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.p2presenter.messaging.Connection;
import org.p2presenter.messaging.handler.RequestHandler;
import org.p2presenter.messaging.message.IncomingRequestMessage;
import org.p2presenter.messaging.message.OutgoingResponseMessage;

/** Handles invocation request messages.
 * @author rmberdeen
 *
 */
public class InvocationRequestHandler implements RequestHandler {
	public static final String PARAMETER_TYPES_HEADER_SEPARATOR = ",";
	public static final String METHOD_NAME_HEADER_NAME = "Method-Name";
	public static final String TARGET_PROXY_ID_HEADER_NAME = "Target-Proxy-Id";
	public static final String TARGET_NAME_HEADER_NAME = "Target-Name";
	public static final String PARAMETER_TYPES_HEADER_NAME = "Parameter-Types";
	public static final String CONTENT_TYPE = "application/x-java-serialized-object";
	public static final String ARGUMENT_COUNT_HEADER_NAME = "Argument-Count";

	private InvocationListener invocationListener;

	public void setInvocationListener(InvocationListener invocationListener) {
		this.invocationListener = invocationListener;
	}

	// TODO ensure required headers are set
	public OutgoingResponseMessage handleRequest(IncomingRequestMessage request) {
		OutgoingSerializedObjectResponseMessage response = new OutgoingSerializedObjectResponseMessage(request);


		try {
			try {
				ProxyCache proxyCache = ProxyCache.getProxyCache(request.getConnection(), request.getUri());
				Invocation invocation = parseMessage(request, proxyCache);

				Object result;

				// TODO unwrap invocation target exception

				if (invocationListener != null) {
					/* if the method is synchronized, the order in which it is entered is significant */
					if (Modifier.isSynchronized(invocation.getMethod().getModifiers())) {
						/* make sure that events for target are logged in the order they actually happen */
						synchronized (invocation.getTarget()) {
							result = invokeAndNotify(invocation);
						}
					}
					else {
						/* if the method isn't synchronized, order doesn't matter */
						result = invokeAndNotify(invocation);
					}
				}
				else {
					result = invocation.invoke();
				}

				if (result != null) {
					Class<?> returnType = invocation.getMethod().getReturnType();
					// TODO probably shouldn't serialize enums
					// TODO serialize wrapper types when method doesn't return primitive
					if (returnType == String.class || returnType.isPrimitive() || Enum.class.isAssignableFrom(returnType)) {
						response.setContentObject((Serializable) result);
					}
					else {
						response.setContentObject(proxyCache.getObjectDescriptor(result));
					}
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
				response.setStatus(500);
				response.setContentObject(ex);
				// TODO send the exception message if it can't be serialized
			}
		}
		catch (ObjectStreamException ex) {
			ex.printStackTrace();
			/*try {
				response.setStatus(500);
				response.setContentObject(ex);
				// TODO warn
			}
			catch (ObjectStreamException exx) {}*/
		}
		return response;
	}

	private Invocation parseMessage(IncomingRequestMessage request, ProxyCache proxyCache) throws ClassNotFoundException, NoSuchMethodException {
		String argumentCountHeader = request.getHeader(ARGUMENT_COUNT_HEADER_NAME);
		int argumentCount = Integer.parseInt(argumentCountHeader);

		Object[] args;
		Class<?>[] parameterTypes;
		if (argumentCount > 0) {
			args = new Object[argumentCount];

			ByteArrayInputStream bytes = new ByteArrayInputStream(request.getContent());

			try {
				ObjectInputStream in = new ObjectInputStream(bytes);
				for (int i = 0; i < args.length; i++) {
					args[i] = in.readObject();
					if (args[i] instanceof RemoteObjectReference) {
						if (args[i] instanceof ObjectDescriptor) {
							// TODO
							args[i] = proxyCache.getProxy(request.getConnection(), true, (ObjectDescriptor) args[i]);
						}
						else {
							args[i] = proxyCache.getTarget(((RemoteObjectReference) args[i]).getId());
						}
					}
				}
				in.close();
			}
			catch (IOException ex) {
				// an io exceptions should never happen while using a byte arry stream
				throw new Error(ex);
			}
			catch (ClassNotFoundException ex) {
				// FIXME
				throw new RuntimeException(ex);
			}

			String[] parameterTypeNames = request.getHeader(PARAMETER_TYPES_HEADER_NAME).split(PARAMETER_TYPES_HEADER_SEPARATOR);
			parameterTypes = new Class[parameterTypeNames.length];

			for (int i = 0; i < parameterTypes.length; i++) {
				parameterTypes[i] = Class.forName(parameterTypeNames[i]);
			}
		}
		else {
			args = new Object[0];
			parameterTypes = new Class[0];
		}

		Object target = proxyCache.getTarget(new Integer(request.getHeader(TARGET_PROXY_ID_HEADER_NAME)));

		Class<?> targetClass = target.getClass();
		Method method = targetClass.getMethod(request.getHeader(METHOD_NAME_HEADER_NAME), parameterTypes);

		return new Invocation(target, method, args);
	}

	/** Invokes the method on the target, notifying the listener before and after.
	 */
	private Object invokeAndNotify(Invocation invocation) throws Exception {
		Object before = invocationListener.beforeMethodInvocation(invocation.getTarget(), invocation.getMethod(), invocation.getArguments());
		Object result = invocation.invoke();
		invocationListener.afterMethodInvocation(before, result);

		return result;
	}
}

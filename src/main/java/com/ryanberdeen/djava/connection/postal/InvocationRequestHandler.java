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

package com.ryanberdeen.djava.connection.postal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.lang.reflect.Method;

import com.ryanberdeen.djava.InvocationListener;
import com.ryanberdeen.djava.LocalInvocation;
import com.ryanberdeen.djava.ObjectDescriptor;
import com.ryanberdeen.djava.RemoteObjectReference;
import com.ryanberdeen.djava.TargetNotFoundException;
import com.ryanberdeen.postal.handler.RequestHandler;
import com.ryanberdeen.postal.message.IncomingRequestMessage;
import com.ryanberdeen.postal.message.OutgoingResponseMessage;

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
				PostalDJavaConnection dJavaConnection = PostalDJavaConnection.getPostalDJavaConnection(request.getConnection(), request.getUri(), true);
				LocalInvocation localInvocation = parseMessage(request, dJavaConnection);

				response.setContentObject(dJavaConnection.invoke(localInvocation));
			}
			catch (Exception ex) {
				ex.printStackTrace();
				response.setStatus(500);
				response.setContentObject(ex);
				// TODO send the exception message if it can't be serialized
			}
			catch (Throwable t) {
				throw (Error) t;
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

	private LocalInvocation parseMessage(IncomingRequestMessage request, PostalDJavaConnection dJavaConnection) throws ClassNotFoundException, NoSuchMethodException {
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
							args[i] = dJavaConnection.getProxy((ObjectDescriptor) args[i]);
						}
						else {
							args[i] = dJavaConnection.getTarget(((RemoteObjectReference) args[i]).getId());
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

		Integer targetId = new Integer(request.getHeader(TARGET_PROXY_ID_HEADER_NAME));
		String methodName = request.getHeader(METHOD_NAME_HEADER_NAME);

		Object target = dJavaConnection.getTarget(targetId);
		if (target == null) {
			throw new TargetNotFoundException(targetId, methodName);
		}

		Class<?> targetClass = target.getClass();
		Method method = targetClass.getMethod(methodName, parameterTypes);

		return new LocalInvocation(target, method, args, invocationListener);
	}
}

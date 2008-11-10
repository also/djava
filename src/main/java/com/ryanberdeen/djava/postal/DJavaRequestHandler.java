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

package com.ryanberdeen.djava.postal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.ryanberdeen.djava.InvocationListener;
import com.ryanberdeen.djava.LocalInvocation;
import com.ryanberdeen.postal.handler.RequestHandler;
import com.ryanberdeen.postal.message.IncomingRequestMessage;
import com.ryanberdeen.postal.message.OutgoingResponseMessage;

/** Handles invocation request messages.
 * @author rmberdeen
 *
 */
public class DJavaRequestHandler implements RequestHandler {
	public static final String PARAMETER_TYPES_HEADER_SEPARATOR = ",";
	public static final String METHOD_NAME_HEADER_NAME = "Method-Name";
	public static final String TARGET_PROXY_ID_HEADER_NAME = "Target-Proxy-Id";
	public static final String TARGET_NAME_HEADER_NAME = "Target-Name";
	public static final String PARAMETER_TYPES_HEADER_NAME = "Parameter-Types";
	public static final String CONTENT_TYPE = "application/x-java-serialized-object";
	public static final String ARGUMENT_COUNT_HEADER_NAME = "Argument-Count";
	public static final String REQUESTING_THREAD_ID_HEADER_NAME = "Requesting-Thread-Id";
	public static final String TARGET_THREAD_ID_HEADER_NAME = "Target-Thread-Id";

	private InvocationListener<?> invocationListener;

	public void setInvocationListener(InvocationListener<?> invocationListener) {
		this.invocationListener = invocationListener;
	}

	// TODO ensure required headers are set
	public OutgoingResponseMessage handleRequest(IncomingRequestMessage request) throws IOException {
		String requestType = request.getRequestType();
		if (PostalDJavaConnection.REQUEST_INVOKE.equals(requestType)) {
			handleInvocationRequest(request);
		}
		else if (PostalDJavaConnection.REQUEST_FINALIZE.equals(requestType)) {
			getPostalDJavaConnection(request).removeLocalObject(Integer.parseInt(request.getContentAsString()));
		}
		else {
			// TODO send error response
		}
		return null;
	}

	private void handleInvocationRequest(IncomingRequestMessage request) {
		try {
			PostalDJavaConnection dJavaConnection = getPostalDJavaConnection(request);
			LocalInvocation localInvocation = parseMessage(request, dJavaConnection);

			dJavaConnection.invokeLocally(localInvocation);
		}
		catch (Exception ex) {
			// TODO
			ex.printStackTrace();

		}
		catch (Throwable t) {
			throw (Error) t;
		}
	}

	private PostalDJavaConnection getPostalDJavaConnection(IncomingRequestMessage request) {
		return PostalDJavaConnection.getPostalDJavaConnection(request.getConnection(), request.getUri(), true);
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
				}
				in.close();
			}
			catch (IOException ex) {
				// an io exception should never happen while using a byte array stream
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
		Long remoteThreadId = new Long(request.getHeader(REQUESTING_THREAD_ID_HEADER_NAME));
		Long targetThreadId = null;
		String targetThreadIdString = request.getHeader(TARGET_THREAD_ID_HEADER_NAME);
		if (targetThreadIdString != null) {
			targetThreadId = new Long(targetThreadIdString);
		}
		String methodName = request.getHeader(METHOD_NAME_HEADER_NAME);

		return new PostalLocalInvocation(request, dJavaConnection, remoteThreadId, targetThreadId, targetId, methodName, parameterTypes, args, invocationListener);
	}
}

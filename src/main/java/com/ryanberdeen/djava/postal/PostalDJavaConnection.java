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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.ryanberdeen.djava.DJavaConnection;
import com.ryanberdeen.djava.DJavaContext;
import com.ryanberdeen.djava.ObjectDescriptor;
import com.ryanberdeen.djava.RemoteInvocation;
import com.ryanberdeen.djava.RemoteObjectReference;
import com.ryanberdeen.postal.Connection;
import com.ryanberdeen.postal.message.IncomingResponseMessage;
import com.ryanberdeen.postal.message.OutgoingRequestMessage;
import com.ryanberdeen.postal.message.ResponseMessage;

public class PostalDJavaConnection extends DJavaConnection {
	public static final String REQUEST_INVOKE = "invoke";
	public static final String REQUEST_FINALIZE = "finalize";
	public static final String REQUEST_LOOKUP = "lookup";

	private static final String CONNECTION_ATTRIBUTE_PREFIX = DJavaContext.class.getName() + "dJavaContext.";

	private Connection connection;
	private String uri;

	public PostalDJavaConnection(Connection connection, String uri, boolean bidirectional) {
		super(bidirectional);
		this.connection = connection;
		this.uri = uri;
	}

	public Object invokeRemotely(RemoteInvocation invocation) throws Throwable {
		OutgoingRequestMessage request = new OutgoingRequestMessage(connection, REQUEST_INVOKE, uri);
		request.setHeader(DJavaRequestHandler.REQUESTING_THREAD_ID_HEADER_NAME, String.valueOf(Thread.currentThread().getId()));
		request.setHeader(DJavaRequestHandler.TARGET_PROXY_ID_HEADER_NAME, String.valueOf(invocation.getTargetId()));
		Long targetThreadId = getRequestingThreadId();
		if (targetThreadId != null) {
			request.setHeader(DJavaRequestHandler.TARGET_THREAD_ID_HEADER_NAME, String.valueOf(targetThreadId));
		}

		request.setHeader(DJavaRequestHandler.METHOD_NAME_HEADER_NAME, invocation.getMethodName());
		Class<?>[] parameterTypes = invocation.getParameterTypes();
		StringBuilder parameterTypesStringBuidler = new StringBuilder();
		if (parameterTypes.length > 0) {
			parameterTypesStringBuidler.append(parameterTypes[0].getName());
			for (int i = 1; i < parameterTypes.length; i++) {
				parameterTypesStringBuidler.append(',');
				parameterTypesStringBuidler.append(parameterTypes[i].getName());
			}
		}
		request.setHeader(DJavaRequestHandler.PARAMETER_TYPES_HEADER_NAME, parameterTypesStringBuidler.toString());

		Object[] args = invocation.getArguments();
		if (args != null) {
			request.setHeader(DJavaRequestHandler.ARGUMENT_COUNT_HEADER_NAME, String.valueOf(args.length));
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try {
				ObjectOutputStream out = new ObjectOutputStream(bytes);

				for (int i = 0; i < args.length; i++) {
					Object argument = args[i];
					argument = dJavaContext.toOutgoing(argument, parameterTypes[i]);
					out.writeObject(argument);
				}

				out.close();
			}
			catch (IOException ex) {
				// shouldn't happen
				throw new Error(ex);
			}

			request.setContent(bytes.toByteArray(), DJavaRequestHandler.CONTENT_TYPE);
		}
		else {
			request.setHeader(DJavaRequestHandler.ARGUMENT_COUNT_HEADER_NAME, "0");
		}

		ResponseMessage response;

		// TODO check allowed exceptions
		Future<IncomingResponseMessage> futureResponse = connection.sendRequest(request);
		if (invocation.isAsynchronous()) {
			return null;
		}
		else {
			response = dJavaContext.awaitResponse(futureResponse);

			// TODO check for null response (connection closed)

			if (response.getStatus() == 200) {
				return getObjectContent(response);
			}
			else {
				Throwable throwable = (Throwable) getObjectContent(response);
				if (throwable != null) {
					throw throwable;
				}
				else {
					// TODO throw unchecked exception
					throw new RemoteException("Remote error " + response.getStatus() + ": " + response.getContentAsString());
				}
			}
		}
	}

	public void sendResponse(OutgoingSerializedObjectResponseMessage response) {
		connection.sendResponse(response);
	}

	private Object getObjectContent(ResponseMessage response) throws Exception {
		if (DJavaRequestHandler.CONTENT_TYPE.equals(response.getContentType())) {
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(response.getContent()));

			return dJavaContext.fromResponse(this, in.readObject());
		}
		else {
			return null;
		}
	}

	@Override
	public ObjectDescriptor lookUpRemoteObject(String name) {
		OutgoingRequestMessage request = new OutgoingRequestMessage(connection, REQUEST_LOOKUP, uri);
		request.setContent(name);
		IncomingResponseMessage response = connection.sendRequestAndAwaitResponseUninterruptibly(request);
		try {
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(response.getContent()));
			return (ObjectDescriptor) in.readObject();
		}
		catch (Exception ex) {
			return null;
		}
	}

	public void finalizeRemotely(RemoteObjectReference remoteObjectReference) {
		OutgoingRequestMessage request = new OutgoingRequestMessage(connection, REQUEST_FINALIZE, uri);
		request.setContent(String.valueOf(remoteObjectReference.getId()));
		connection.sendRequestAndIgnoreResponse(request);
	}

	public static PostalDJavaConnection getPostalDJavaConnection(Connection connection, String uri, boolean bidirectional) {
		return connection.getAttribute(CONNECTION_ATTRIBUTE_PREFIX  + uri, new CreatePostalDJavaConnection(connection, uri, bidirectional));
	}

	public static class CreatePostalDJavaConnection implements Callable<PostalDJavaConnection> {
		private Connection connection;
		private String uri;
		private boolean bidirectional;

		public CreatePostalDJavaConnection(Connection connection, String uri, boolean bidirectional) {
			this.connection = connection;
			this.uri = uri;
			this.bidirectional = bidirectional;
		}

		public PostalDJavaConnection call() {
			return new PostalDJavaConnection(connection, uri, bidirectional);
		}
	}
}

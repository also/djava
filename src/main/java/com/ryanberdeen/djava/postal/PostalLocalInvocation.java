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

import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.Serializable;

import com.ryanberdeen.djava.DJavaConnection;
import com.ryanberdeen.djava.InvocationListener;
import com.ryanberdeen.djava.LocalInvocation;
import com.ryanberdeen.postal.message.IncomingRequestMessage;

public class PostalLocalInvocation extends LocalInvocation {
	private IncomingRequestMessage request;

	public PostalLocalInvocation(IncomingRequestMessage request, DJavaConnection connection, long requestingThreadId, Long targetThreadId, Integer targetId, String methodName, Class<?>[] parameterTypes, Object[] arguments, InvocationListener<?> invocationListener) {
		super(connection, requestingThreadId, targetThreadId, targetId, methodName, parameterTypes, arguments, invocationListener);
		this.request = request;
	}

	@Override
	protected void handleResult(Serializable result) throws InvalidClassException, NotSerializableException {
		OutgoingSerializedObjectResponseMessage response = new OutgoingSerializedObjectResponseMessage(request);
		response.setContentObject(result);
		sendResponse(response);
	}

	@Override
	protected void handleThrowable(Throwable t) throws Exception {
		t.printStackTrace();
		OutgoingSerializedObjectResponseMessage response = new OutgoingSerializedObjectResponseMessage(request);
		response.setStatus(500);
		response.setContentObject(t);
		sendResponse(response);
	}

	@Override
	protected void handleInternalThrowable(Throwable t) {
		t.printStackTrace();
		try {
			OutgoingSerializedObjectResponseMessage response = new OutgoingSerializedObjectResponseMessage(request);
			response.setStatus(500);
			response.setContentObject(t);
			sendResponse(response);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void sendResponse(OutgoingSerializedObjectResponseMessage response) {
		((PostalDJavaConnection) dJavaConnection).sendResponse(response);
	}
}

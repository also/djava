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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.concurrent.Future;


@SuppressWarnings("unused")
class RemoteObjectInvocationHandler implements InvocationHandler {
	private DJavaConnection connection;
	private RemoteObjectReference remoteObjectReference;

	public RemoteObjectInvocationHandler(DJavaConnection connection, RemoteObjectReference remoteObjectReference) {
		this.connection = connection;
		this.remoteObjectReference = remoteObjectReference;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("getRemoteObjectReference") && method.getParameterTypes().length == 0) {
			return remoteObjectReference;
		}
		return connection.invokeRemotely(new RemoteInvocation(remoteObjectReference, method, args));
	}
}

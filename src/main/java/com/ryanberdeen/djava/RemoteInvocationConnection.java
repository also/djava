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

import java.lang.reflect.Proxy;

import org.p2presenter.messaging.LocalConnection;

public class RemoteInvocationConnection {
	private LocalConnection connection;
	private String uri;
	boolean bidirectional = false;
	
	public RemoteInvocationConnection(LocalConnection connection, String uri) {
		this.connection = connection;
		this.uri = uri;
	}
	
	public RemoteInvocationConnection(LocalConnection connection, String uri, boolean bidirectional) {
		this(connection, uri);
		this.bidirectional = bidirectional;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T proxy(Class<T> interfaceClass, RemoteObjectReference remoteProxyReference) {
		return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] {interfaceClass, RemoteInvocationProxy.class}, new RemoteObjectInvocationHandler(connection, uri, bidirectional, remoteProxyReference));
	}
	
	public LocalConnection getConnection() {
		return connection;
	}
}

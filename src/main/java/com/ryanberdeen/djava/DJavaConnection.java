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

public abstract class DJavaConnection {
	protected DJavaContext dJavaContext;
	private ThreadLocal<Long> requestingThreadId;

	public DJavaConnection(boolean bidirectional) {
		dJavaContext = new DJavaContext(bidirectional);
		requestingThreadId = new ThreadLocal<Long>();
	}

	public abstract Object invokeRemotely(RemoteInvocation invocation) throws Throwable;

	public RemoteInvocationProxy getProxy(ObjectDescriptor objectDescriptor) {
		return dJavaContext.getProxy(this, objectDescriptor);
	}

	public Object getTarget(Integer id) {
		return dJavaContext.getTarget(id);
	}

	public void invokeLocally(LocalInvocation localInvocation) {
		dJavaContext.invokeLocally(localInvocation);
	}

	public ObjectDescriptor getObjectDescriptor(Object toProxy) throws Exception {
		return dJavaContext.getObjectDescriptor(toProxy);
	}

	@SuppressWarnings("unchecked")
	public <T> T proxy(Class<T> interfaceClass, Integer id) {
		Class[] classes = new Class[] {interfaceClass};

		ObjectDescriptor objectDescriptor = new ObjectDescriptor(classes, id);
		return (T) dJavaContext.getProxy(this, objectDescriptor);
	}

	public Long getRequestingThreadId() {
		return requestingThreadId.get();
	}

	void setRequestingThreadId(Long requestingThreadId) {
		this.requestingThreadId.set(requestingThreadId);
	}
}

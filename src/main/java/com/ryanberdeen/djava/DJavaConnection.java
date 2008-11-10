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

/** A connection to a remote system capable of handling dJava method invocations.
 *
 */
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

	public ObjectDescriptor getObjectDescriptor(Object toProxy) {
		return dJavaContext.getObjectDescriptor(toProxy);
	}

	/** Returns a proxy object that forwards method invocations to the remote object specified.
	 * @param <T> the interface of the proxied object
	 * @param interfaceClass the interface to proxy
	 * @param id the remote id of the object
	 * @return an object implementing the specified interface
	 */
	@SuppressWarnings("unchecked")
	public <T> T proxy(Class<T> interfaceClass, Integer id) {
		Class[] classes = new Class[] {interfaceClass};

		ObjectDescriptor objectDescriptor = new ObjectDescriptor(classes, id);
		return (T) dJavaContext.getProxy(this, objectDescriptor);
	}

	public Object proxy(String name) {
		ObjectDescriptor objectDescriptor = lookUpRemoteObject(name);
		return objectDescriptor != null ? dJavaContext.getProxy(this, objectDescriptor) : null;
	}

	public abstract ObjectDescriptor lookUpRemoteObject(String name);

	public Long getRequestingThreadId() {
		return requestingThreadId.get();
	}

	void setRequestingThreadId(Long requestingThreadId) {
		this.requestingThreadId.set(requestingThreadId);
	}

	void finalizeRemoteObjectReference(RemoteObjectReference remoteObjectReference) {
		dJavaContext.removeProxyReference(remoteObjectReference);
		finalizeRemotely(remoteObjectReference);
	}

	protected abstract void finalizeRemotely(RemoteObjectReference remoteObjectReference);

	public void removeLocalObject(int id) {
		dJavaContext.removeLocalObject(id);
	}

	public ObjectDescriptor getNamedObjectDescriptor(String name) {
		return dJavaContext.getNamedObjectDescriptor(name);
	}
}

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
import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import com.ryanberdeen.djava.connection.DJavaConnection;

public class ProxyCache {
	private boolean bidirectional;
	private Integer proxyNumber = 1;
	private HashMap<Object, ObjectDescriptor> objectDescriptors = new HashMap<Object, ObjectDescriptor>();

	/** Maps from id to target*/
	private HashMap<Integer, Object> localObjects = new HashMap<Integer, Object>();

	private HashMap<RemoteObjectReference, WeakReference<RemoteInvocationProxy>> proxyReferences = new HashMap<RemoteObjectReference, WeakReference<RemoteInvocationProxy>>();

	private HashMap<Long, Thread> waitingThreads;
	private HashMap<Long, LocalInvocation> pendingInvocations;

	public ProxyCache(boolean bidirectional) {
		this.bidirectional = bidirectional;
	}

	public Object getTarget(Integer id) {
		return localObjects.get(id);
	}

	public ObjectDescriptor getObjectDescriptor(Object toProxy) throws Exception {
		synchronized (objectDescriptors) {
			ObjectDescriptor objectDescriptor = objectDescriptors.get(toProxy);

			if (objectDescriptor == null) {
				Class<?>[] interfaces = toProxy.getClass().getInterfaces();
				objectDescriptor = new ObjectDescriptor(interfaces, proxyNumber++);
				objectDescriptors.put(toProxy, objectDescriptor);
				localObjects.put(objectDescriptor.getId(), toProxy);
			}

			return objectDescriptor;
		}
	}

	public RemoteInvocationProxy getProxy(DJavaConnection connection, ObjectDescriptor objectDescriptor) {
		synchronized (proxyReferences) {
			RemoteInvocationProxy proxy = null;
			WeakReference<RemoteInvocationProxy> remoteProxyReference = proxyReferences.get(objectDescriptor);
			if (remoteProxyReference != null) {
				proxy = remoteProxyReference.get();
			}
			if (proxy == null) {
				Class<?>[] interfaceClasses = new Class[objectDescriptor.getProxiedClasses().length + 1];
				System.arraycopy(objectDescriptor.getProxiedClasses(), 0, interfaceClasses, 1, objectDescriptor.getProxiedClasses().length);
				interfaceClasses[0] = RemoteInvocationProxy.class;
				RemoteObjectInvocationHandler handler = new RemoteObjectInvocationHandler(connection, new RemoteObjectReference(objectDescriptor));
				proxy = (RemoteInvocationProxy) Proxy.newProxyInstance(interfaceClasses[0].getClassLoader(), interfaceClasses, handler);
				proxyReferences.put(objectDescriptor.getRemoteObjectReference(), new WeakReference<RemoteInvocationProxy>(proxy));
			}

			return proxy;
		}
	}

	public void registerWaitingThread() {
		Thread thread = Thread.currentThread();
		waitingThreads.put(thread.getId(), thread);
	}

	/** Performs the method invocations requested of the current thread. */
	public void performInvocations() {

	}

	public Serializable invoke(LocalInvocation localInvocation) throws Throwable {
		Object result = localInvocation.invoke();

		if (result != null) {
			Class<?> returnType = localInvocation.getReturnType();
			// TODO probably shouldn't serialize enums
			// TODO serialize wrapper types when method doesn't return primitive
			if (returnType == String.class || returnType.isPrimitive() || Enum.class.isAssignableFrom(returnType)) {
				return (Serializable) result;
			}
			else {
				return getObjectDescriptor(result);
			}
		}
		else {
			return null;
		}
	}

	public Object toOutgoing(Object object, Class<?> parameterType) throws Exception {
		if (object instanceof RemoteInvocationProxy) {
			// send a reference to the remote object
			return ((RemoteInvocationProxy) object).getRemoteObjectReference();
		}
		else if (bidirectional && parameterType.isInterface()) {
			// send a reference to the local object
			return getObjectDescriptor(object);
		}
		else {
			return object;
		}
	}

	/** Converts an incoming object to one that can be used for a return value. */
	public Object fromResponse(DJavaConnection connection, Object result) {
		if (result instanceof ObjectDescriptor) {
			return getProxy(connection, (ObjectDescriptor) result);
		}
		else {
			return result;
		}
	}
}

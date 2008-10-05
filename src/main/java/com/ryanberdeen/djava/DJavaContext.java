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

import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.ryanberdeen.djava.connection.DJavaConnection;

public class DJavaContext {
	private boolean bidirectional;
	private Integer proxyNumber = 1;
	private HashMap<Object, ObjectDescriptor> objectDescriptors = new HashMap<Object, ObjectDescriptor>();

	/** Maps from id to target*/
	private HashMap<Integer, Object> localObjects = new HashMap<Integer, Object>();

	private HashMap<RemoteObjectReference, WeakReference<RemoteInvocationProxy>> proxyReferences = new HashMap<RemoteObjectReference, WeakReference<RemoteInvocationProxy>>();

	private ConcurrentHashMap<Long, WaitingThread> waitingThreads = new ConcurrentHashMap<Long, WaitingThread>();

	public DJavaContext(boolean bidirectional) {
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

	public void invokeLocally(LocalInvocation localInvocation) {
		Long targetThreadId = localInvocation.getTargetThreadId();
		if (targetThreadId != null) {
			WaitingThread waitingThread = waitingThreads.get(targetThreadId);
			if (waitingThread != null) {
				waitingThread.setPendingInvocation(localInvocation);
				return;
			}
		}
		invoke(localInvocation);
	}

	public void invoke(LocalInvocation localInvocation) {
		try {
			localInvocation.invoke();
		}
		catch (Error e) {
			// TODO handle fatal error
			throw e;
		}
		catch (RuntimeException e) {
			// TODO handle possibly non-fatal error
			throw e;
		}
		catch (Throwable t) {
			// TODO handle fatal error
			t.printStackTrace();
			throw new RuntimeException(t);
		}
	}

	private WaitingThread registerWaitingThread() {
		Thread thread = Thread.currentThread();
		long id = thread.getId();
		WaitingThread waitingThread = waitingThreads.get(id);
		if (waitingThread == null) {
			waitingThread = new WaitingThread(thread);
			waitingThreads.put(id, waitingThread);
		}
		waitingThread.incrementDepth();
		return waitingThread;
	}

	public void unregisterWaitingThread(WaitingThread waitingThread) {
		waitingThread.decrementDepth();
		if (waitingThread.getDepth() == 0) {
			waitingThreads.remove(waitingThread.getId());
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

	public <T> T awaitResponse(Future<T> futureResponse) throws ExecutionException {
		T response;
		WaitingThread waitingThread = registerWaitingThread();
		try {
			for (;;) {
				try {
					response = futureResponse.get();
					break;
				}
				catch (InterruptedException ex) {
					waitingThread.performInvocation(this);
				}
			}
		}
		finally {
			unregisterWaitingThread(waitingThread);
		}

		return response;
	}
}

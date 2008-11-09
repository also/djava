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

class WaitingThread {
	private Thread thread;
	private int depth = 0;
	private LocalInvocation pendingInvocation;

	public WaitingThread(Thread thread) {
		this.thread = thread;
	}

	public long getId() {
		return thread.getId();
	}

	public void incrementDepth() {
		depth++;
	}

	public void decrementDepth() {
		depth--;
	}

	public int getDepth() {
		return depth;
	}

	public synchronized void setPendingInvocation(LocalInvocation pendingInvocation) {
		this.pendingInvocation = pendingInvocation;
		thread.interrupt();
	}

	public synchronized void performInvocation(DJavaContext djJavaContext) {
		if (pendingInvocation != null) {
			djJavaContext.invoke(pendingInvocation);
			pendingInvocation = null;
		}
	}
}

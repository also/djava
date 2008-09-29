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

public class ObjectDescriptor extends RemoteObjectReference implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Class<?>[] proxiedClasses;
	
	public ObjectDescriptor(Class<?>[] proxiedClasses, int id) {
		super(id);
		this.proxiedClasses = proxiedClasses;
	}
	
	public Class<?>[] getProxiedClasses() {
		return proxiedClasses;
	}
	
	public RemoteObjectReference getRemoteObjectReference() {
		return new RemoteObjectReference(getId());
	}
}

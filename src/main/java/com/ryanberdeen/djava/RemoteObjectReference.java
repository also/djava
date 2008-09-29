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

public class RemoteObjectReference implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int id;

	public RemoteObjectReference(int id) {
		this.id = id;
	}
	
	public RemoteObjectReference(RemoteObjectReference that) {
		this.id = that.id;
	}

	public int getId() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof RemoteObjectReference && ((RemoteObjectReference) obj).id == id;
	}
	
	@Override
	public int hashCode() {
		// TODO not a very good hash
		return id;
	}
}

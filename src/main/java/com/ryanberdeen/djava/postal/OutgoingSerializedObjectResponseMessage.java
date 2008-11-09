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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.ryanberdeen.postal.message.OutgoingResponseMessage;
import com.ryanberdeen.postal.message.RequestMessage;

public class OutgoingSerializedObjectResponseMessage extends OutgoingResponseMessage {
	public OutgoingSerializedObjectResponseMessage(RequestMessage inResponseToMessage) {
		super(inResponseToMessage);
	}

	public void setContentObject(Serializable content) throws InvalidClassException, NotSerializableException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out = new ObjectOutputStream(bytes);
			out.writeObject(content);
			out.close();

			setContent(bytes.toByteArray(), InvocationRequestHandler.CONTENT_TYPE);
		}
		catch (InvalidClassException ex) {
			throw ex;
		}
		catch (NotSerializableException ex) {
			throw ex;
		}
		catch (IOException ex) {
			throw new Error(ex);
		}
	}
}

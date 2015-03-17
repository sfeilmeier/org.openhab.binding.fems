/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.internal.io;

import org.bulldog.core.gpio.DigitalInput;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.types.State;

public class IODigitalInput implements IO {

	private DigitalInput pin;
	
	public IODigitalInput(DigitalInput pin) {
		this.pin = pin;
	}
	
	@Override
	public State getState() {
		if(pin.read().getBooleanValue()) {
			return OpenClosedType.OPEN;
		} else {
			return OpenClosedType.CLOSED;
		}
	}
}

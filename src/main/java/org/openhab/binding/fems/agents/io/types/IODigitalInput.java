/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.agents.io.types;

import org.bulldog.core.gpio.DigitalInput;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.types.State;

public class IODigitalInput implements IO {

	private DigitalInput pin;
	
	public IODigitalInput(DigitalInput pin) {
		this.pin = pin;
		pin.setTeardownOnShutdown(true);
	}
	
	@Override
	public State getState() {
		if(pin.read().getBooleanValue()) {
			return OpenClosedType.OPEN;
		} else {
			return OpenClosedType.CLOSED;
		}
	}

	@Override
	public void dispose() {
		// nothing to do here
	}

	@Override
	public boolean sendToOnlineMonitoring() {
		return true;
	}
}

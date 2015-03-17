/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.internal.io;

import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.fems.tools.FEMSDisplayAgent;

public class IOLcd implements IOOutput {
	private int row;
	
	public IOLcd(int row) {
		this.row = row;
	}

	@Override
	public State getState() {
		return StringType.EMPTY;
	}

	@Override
	public void handleCommand(Command command) {
		if(command instanceof StringType) {
			StringType cmd = (StringType) command;
			switch(row) {
			case 1: // first line: set SOC (together with current time)
				FEMSDisplayAgent.getFEMSDisplay().offerFirstRow(cmd.toString());
				break;
			case 2: // second line: set full text
				FEMSDisplayAgent.getFEMSDisplay().offer(cmd.toString(), true);
				break;
			}
		}
	}
}

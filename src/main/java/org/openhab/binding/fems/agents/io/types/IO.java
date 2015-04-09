/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.agents.io.types;

import org.eclipse.smarthome.core.types.State;

public interface IO {
	public State getState();
	
	public void dispose();
	
	/**
	 * Whether to send this IO data to the online monitoring or not
	 * 
	 * @return true: yes, send; false: no, don't send
	 */
	public boolean sendToOnlineMonitoring();
}

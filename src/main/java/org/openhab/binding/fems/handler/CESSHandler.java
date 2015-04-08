/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.handler;

import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.fems.internal.essprotocol.CESSProtocolFactory;
import org.openhab.binding.fems.internal.essprotocol.ESSProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CESSHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * 
 * @author Stefan Feilmeier - Initial contribution
 */
public class CESSHandler extends ESSHandler {
    private Logger logger = LoggerFactory.getLogger(CESSHandler.class);

	public CESSHandler(Thing thing) {
		super(thing);
	}
	
	@Override
	public void initialize() {
		logger.info("Initializing FEMS CESS handler.");
		super.initialize();
	}
	
	protected ESSProtocol getProtocol() {
		return CESSProtocolFactory.getProtocol(modbusDevice, unitid);
	}
}

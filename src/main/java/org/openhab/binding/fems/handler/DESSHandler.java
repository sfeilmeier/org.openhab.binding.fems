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
import org.openhab.binding.fems.internal.essprotocol.DESSProtocolFactory;
import org.openhab.binding.fems.internal.essprotocol.ESSProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DESSHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * 
 * @author Stefan Feilmeier - Initial contribution
 */
public class DESSHandler extends ESSHandler {
    private Logger logger = LoggerFactory.getLogger(DESSHandler.class);

	public DESSHandler(Thing thing) {
		super(thing);
	}
	
	@Override
	public void initialize() {
		logger.info("Initializing FEMS DESS handler.");
		super.initialize();
	}
	
	protected ESSProtocol getProtocol() {
		return DESSProtocolFactory.getProtocol(modbusDevice, unitid);
	}
}

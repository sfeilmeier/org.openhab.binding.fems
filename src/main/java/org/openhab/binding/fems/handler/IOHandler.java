/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.fems.Constants;
import org.openhab.binding.fems.agents.io.message.IOAgentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOHandler extends BaseThingHandler implements IOAgentListener {
	private Logger logger = LoggerFactory.getLogger(IOHandler.class);
	
	public IOHandler(Thing thing) {
		super(thing);
	}

	@Override
	public void initialize() {
		super.initialize();
	}
		
	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		Constants.IO_AGENT.handleCommand(channelUID.getId(), command);
	}
	
	@Override
	public void handleUpdate(ChannelUID channelUID, State newState) {
		logger.info("handleUpdate " + channelUID.toString() + newState.toString());
		super.handleUpdate(channelUID, newState);
	}

	@Override
	public void ioUpdate(String id, State state) {
		updateState(new ChannelUID(getThing().getUID(), id), state);
	}
	
}
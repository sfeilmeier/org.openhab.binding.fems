/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.handler;

import java.util.List;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.fems.agents.ess.message.ESSAgentListener;
import org.openhab.binding.fems.internal.essprotocol.modbus.BitWordElement;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElement;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElementRange;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusItem;
import org.openhab.binding.fems.internal.essprotocol.modbus.OnOffBitItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESSHandler extends BaseThingHandler implements ESSAgentListener {
	private final Logger logger = LoggerFactory.getLogger(ESSHandler.class);
	
	public ESSHandler(Thing thing) {
		super(thing);
	}
		
	@Override
	public void initialize() {
		super.initialize();
	}
	
	public void essUpdate(List<ModbusElementRange> wordRanges) {
		for (ModbusElementRange wordRange : wordRanges) {
			for (ModbusElement word : wordRange.getWords()) {
				if(word instanceof ModbusItem) {
					ModbusItem item = (ModbusItem)word;
					updateState(new ChannelUID(getThing().getUID(), item.getName()), item.getState());
				} else if (word instanceof BitWordElement) {
					BitWordElement bitWord = (BitWordElement)word;
					for (OnOffBitItem bitItem : bitWord.getBitItems()) {
						// logger.info("Switch " + bitWord.getName() + "_" + bitItem.getName());
						updateState(new ChannelUID(getThing().getUID(), bitWord.getName() + "_" + bitItem.getName()), bitItem.getState());
					} 
				}
			}
		}
	}
	
	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		logger.info("handleCommand()");
		logger.info("ChannelUID: " + channelUID);
		logger.info("Command: " + command);
	}
}

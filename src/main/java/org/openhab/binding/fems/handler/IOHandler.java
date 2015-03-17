/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.handler;

import java.util.HashMap;

import org.bulldog.beagleboneblack.BBBNames;
import org.bulldog.core.Edge;
import org.bulldog.core.gpio.DigitalInput;
import org.bulldog.core.gpio.DigitalOutput;
import org.bulldog.core.gpio.Pwm;
import org.bulldog.core.gpio.event.InterruptEventArgs;
import org.bulldog.core.gpio.event.InterruptListener;
import org.bulldog.core.platform.Board;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.fems.FEMSBindingConstants;
import org.openhab.binding.fems.internal.io.IO;
import org.openhab.binding.fems.internal.io.IOAnalogOutput;
import org.openhab.binding.fems.internal.io.IOAnalogOutput.DIVIDE;
import org.openhab.binding.fems.internal.io.IODigitalInput;
import org.openhab.binding.fems.internal.io.IODigitalOutput;
import org.openhab.binding.fems.internal.io.IOLcd;
import org.openhab.binding.fems.internal.io.IOOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.fems.scheduler.agents.OnlineMonitoring.OnlineMonitoringAgentMessage.DataMessage;
import de.fenecon.fems.scheduler.agents.OnlineMonitoring.OnlineMonitoringAgentMessage.DataMessageContentType;

public class IOHandler extends BaseThingHandler {
	private Logger logger = LoggerFactory.getLogger(IOHandler.class);
	
	private HashMap<String, IO> pins = null;
	
	public IOHandler(Thing thing) {
		super(thing);
	}
	
	private class DigitalInputInterrupt implements InterruptListener {
		private String id;
		public DigitalInputInterrupt(String id) {
			this.id = id;
		}
		@Override
		public void interruptRequest(InterruptEventArgs args) {
			if(args.getEdge() == Edge.Rising) {
				updateState(new ChannelUID(getThing().getUID(), id), OpenClosedType.OPEN);
			} else {
				updateState(new ChannelUID(getThing().getUID(), id), OpenClosedType.CLOSED);
			}
		}
	};

	@Override
	public void initialize() {			
		Board bbb = FEMSBindingConstants.bbb;
		pins = new HashMap<String, IO>();
		
		// LCD Display
		pins.put("LCD_1", new IOLcd(1));
		pins.put("LCD_2", new IOLcd(2));
		
		// Relay Outputs
		pins.put("RelayOutput_1", new IODigitalOutput(bbb.getPin(BBBNames.P8_12).as(DigitalOutput.class)));
		pins.put("RelayOutput_2", new IODigitalOutput(bbb.getPin(BBBNames.P8_11).as(DigitalOutput.class)));
		pins.put("RelayOutput_3", new IODigitalOutput(bbb.getPin(BBBNames.P8_16).as(DigitalOutput.class)));
		pins.put("RelayOutput_4", new IODigitalOutput(bbb.getPin(BBBNames.P8_15).as(DigitalOutput.class)));
		
		// Analog Outputs
		pins.put("AnalogOutput_1", new IOAnalogOutput(
				bbb.getPin(BBBNames.EHRPWM1A_P9_14).as(Pwm.class),
				bbb.getPin(BBBNames.P9_28).as(DigitalOutput.class), DIVIDE.VOLTAGE));
		pins.put("AnalogOutput_2", new IOAnalogOutput(
				bbb.getPin(BBBNames.EHRPWM1B_P9_16).as(Pwm.class),
				bbb.getPin(BBBNames.P9_29).as(DigitalOutput.class), DIVIDE.VOLTAGE));
		pins.put("AnalogOutput_3", new IOAnalogOutput(
				bbb.getPin(BBBNames.EHRPWM2A_P8_19).as(Pwm.class),
				bbb.getPin(BBBNames.P9_30).as(DigitalOutput.class), DIVIDE.VOLTAGE));
		pins.put("AnalogOutput_4", new IOAnalogOutput(
				bbb.getPin(BBBNames.EHRPWM2B_P8_13).as(Pwm.class),
				bbb.getPin(BBBNames.P9_31).as(DigitalOutput.class), DIVIDE.VOLTAGE));
		
		// Digital Inputs
		DigitalInput di1 = bbb.getPin(BBBNames.P9_42).as(DigitalInput.class);
		di1.addInterruptListener(new DigitalInputInterrupt("DigitalInput_1"));
		di1.enableInterrupts();
		pins.put("DigitalInput_1", new IODigitalInput(di1));

		DigitalInput di2 = bbb.getPin(BBBNames.P9_27).as(DigitalInput.class);
		di2.addInterruptListener(new DigitalInputInterrupt("DigitalInput_2"));
		di2.enableInterrupts();
		pins.put("DigitalInput_2", new IODigitalInput(di2));
		
		DigitalInput di3 = bbb.getPin(BBBNames.P9_41).as(DigitalInput.class);
		di3.addInterruptListener(new DigitalInputInterrupt("DigitalInput_3"));
		di3.enableInterrupts();
		pins.put("DigitalInput_3", new IODigitalInput(di3));
		
		DigitalInput di4 = bbb.getPin(BBBNames.P9_25).as(DigitalInput.class);
		di4.addInterruptListener(new DigitalInputInterrupt("DigitalInput_4"));
		di4.enableInterrupts();
		pins.put("DigitalInput_4", new IODigitalInput(di4));

		// TODO: Analogue Inputs
		// TODO: This is not working for some reason. Maybe try to delay initialization (updateState) a bit
		for (String id : pins.keySet()) {
			IO io = pins.get(id);
			State state = io.getState();
			updateState(new ChannelUID(getThing().getUID(), id), state);
		}
		
		/* turn off all outputs on shutdown */ 
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
				for (String id : pins.keySet()) {
					IO io = pins.get(id);
					logger.info("Turn off " + id);
					if(io instanceof IOOutput) {
						IOOutput output = (IOOutput)io;
						output.handleCommand(OnOffType.OFF);
					}
				}
            }
        });
		
		super.initialize();
	}
		
	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		IO io = pins.get(channelUID.getId());
		if(io instanceof IOOutput) {
			IOOutput ioOutput = (IOOutput)io;
			ioOutput.handleCommand(command);
			
			DataMessage message = new DataMessage(DataMessageContentType.IO);
			message.states.put(channelUID.getId(), io.getState());
			FEMSBindingConstants.onlineMonitoringAgent.message(message);
		} else {
			logger.error("This is not an IO Output: " + channelUID.getId());
		}
	}
	
	@Override
	public void handleUpdate(ChannelUID channelUID, State newState) {
		logger.info("handleUpdate " + channelUID.toString() + newState.toString());
		super.handleUpdate(channelUID, newState);
	}
}

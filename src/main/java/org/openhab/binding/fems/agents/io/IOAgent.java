package org.openhab.binding.fems.agents.io;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bulldog.beagleboneblack.BBBNames;
import org.bulldog.core.gpio.DigitalIO;
import org.bulldog.core.gpio.DigitalOutput;
import org.bulldog.core.gpio.Pwm;
import org.bulldog.core.platform.Board;
import org.bulldog.core.platform.Platform;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.fems.Constants;
import org.openhab.binding.fems.agents.Agent;
import org.openhab.binding.fems.agents.Message;
import org.openhab.binding.fems.agents.io.message.HandleCommandMessage;
import org.openhab.binding.fems.agents.io.message.IOAgentListener;
import org.openhab.binding.fems.agents.io.message.ListenerMessage;
import org.openhab.binding.fems.agents.io.message.UpdateAllStatesMessage;
import org.openhab.binding.fems.agents.io.message.UpdateLcdTimeMessage;
import org.openhab.binding.fems.agents.io.types.IO;
import org.openhab.binding.fems.agents.io.types.IOAnalogOutput;
import org.openhab.binding.fems.agents.io.types.IOAnalogOutputCtrlVolt;
import org.openhab.binding.fems.agents.io.types.IODigitalOutput;
import org.openhab.binding.fems.agents.io.types.IOLcd;
import org.openhab.binding.fems.agents.io.types.IOLcdBacklight;
import org.openhab.binding.fems.agents.io.types.IOLcdRow;
import org.openhab.binding.fems.agents.io.types.IOOutput;
import org.openhab.binding.fems.agents.io.types.IOUserLed;
import org.openhab.binding.fems.agents.onlinemonitoring.message.DataMessage.MethodType;
import org.openhab.binding.fems.tools.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOAgent extends Agent {
	public final Board BBB = Platform.createBoard(); // BeagleBone Black hardware layer
	private Map<String, IO> ios = null;
	private final List<IOAgentListener> listeners = new LinkedList<IOAgentListener>();
	private final Logger logger = LoggerFactory.getLogger(IOAgent.class);
	private IOLcd lcd = null;
	
	/*private class DigitalInputInterrupt implements InterruptListener {
		private final String id;
		private final IODigitalInput io;
		public DigitalInputInterrupt(String id, IODigitalInput io) {
			this.id = id;
			this.io = io;
		}
		@Override
		public void interruptRequest(InterruptEventArgs args) {
			updateState(id, io);
			//if(args.getEdge() == Edge.Rising) {
			//	updateState(id, OpenClosedType.OPEN);
			//} else {
			//	updateState(id, OpenClosedType.CLOSED);
			//}
		}
	}; */
	
	/**
	 * {@inheritDoc}
	 */
	public IOAgent(String name) {
		super(name);
	}
	
	@Override
	public void foreverLoop(Message message) {	
		if(message instanceof ListenerMessage) {
			IOAgentListener listener = ((ListenerMessage)message).getListener();
			listeners.add(listener);
			/*Causes java.lang.IllegalStateException
			 * for (String id : ios.keySet()) {
				listener.ioUpdate(id, ios.get(id).getState());
			}*/
			// try again in a few seconds, because on startup it might not work directly:
			Executors.newScheduledThreadPool(1).schedule(new Runnable() {
				@Override
				public void run() {
					handle(new UpdateAllStatesMessage());
				}
			}, 5, TimeUnit.SECONDS);
			
		} else if (message instanceof HandleCommandMessage) {
			// handle a command for an IO Output
			HandleCommandMessage m = (HandleCommandMessage)message;
			if(ios.containsKey(m.getId())) {
				IO io = ios.get(m.getId());
				if(io instanceof IOOutput) {
					((IOOutput)io).handleCommand(m.getCommand());
					updateState(m.getId(), io);
				} else {
					logger.warn("IO interface is not an Output: " + m.getCommand());
				}
				

			} else {
				logger.warn("No matching IO interface found: " + m.getId());
			}
			
		} else if (message instanceof UpdateLcdTimeMessage) {
			lcd.updateLcdTime();
			
		} else if (message instanceof UpdateAllStatesMessage) {
			for(IOAgentListener listener : listeners) {
				for (String id : ios.keySet()) {
					listener.ioUpdate(id, ios.get(id).getState());
				}
			}
		}
	}

	/*
	 * Initialize IOs
	 */
	public void init() {
		ios = new HashMap<String, IO>();
		synchronized (BBB) {
			// LCD Display
			lcd = new IOLcd(
					BBB.getPin(BBBNames.P9_15).as(DigitalIO.class),  //rs pin 
					BBB.getPin(BBBNames.P9_23).as(DigitalIO.class),  //rw pin
					BBB.getPin(BBBNames.P9_12).as(DigitalIO.class),  //enable pin
					BBB.getPin(BBBNames.P8_30).as(DigitalIO.class),  //db 4
					BBB.getPin(BBBNames.P8_28).as(DigitalIO.class),  //db 5
					BBB.getPin(BBBNames.P8_29).as(DigitalIO.class),  //db 6
					BBB.getPin(BBBNames.P8_27).as(DigitalIO.class)); //db 7
			ios.put(Constants.LCD_1, new IOLcdRow(lcd, Constants.LCD_1));
			ios.put(Constants.LCD_2, new IOLcdRow(lcd, Constants.LCD_2));
			ios.put(Constants.LCD_Backlight, new IOLcdBacklight(Constants.LCD_Backlight, OnOffType.OFF, BBB.getPin(BBBNames.P9_22).as(Pwm.class)));
				
			// BeagleBone User LEDs
			ios.put(Constants.UserLED_1, new IOUserLed(Constants.UserLED_1, OnOffType.OFF));
			ios.put(Constants.UserLED_2, new IOUserLed(Constants.UserLED_2, OnOffType.OFF));
			ios.put(Constants.UserLED_3, new IOUserLed(Constants.UserLED_3, OnOffType.OFF));
			ios.put(Constants.UserLED_4, new IOUserLed(Constants.UserLED_4, OnOffType.OFF));
			
			// TODO: RGB-LED
			
			// Relay Outputs
			ios.put(Constants.RelayOutput_1, new IODigitalOutput(BBB.getPin(BBBNames.P8_12).as(DigitalOutput.class), OnOffType.OFF));
			ios.put(Constants.RelayOutput_2, new IODigitalOutput(BBB.getPin(BBBNames.P8_11).as(DigitalOutput.class), OnOffType.OFF));
			ios.put(Constants.RelayOutput_3, new IODigitalOutput(BBB.getPin(BBBNames.P8_16).as(DigitalOutput.class), OnOffType.OFF));
			ios.put(Constants.RelayOutput_4, new IODigitalOutput(BBB.getPin(BBBNames.P8_15).as(DigitalOutput.class), OnOffType.OFF));
			
			// Analog Outputs
			Map<String, IOAnalogOutputCtrlVolt> aovs = new HashMap<String, IOAnalogOutputCtrlVolt>();
			aovs.put(Constants.AnalogOutput_1_Volt, new IOAnalogOutputCtrlVolt(BBB.getPin(BBBNames.P9_28).as(DigitalOutput.class), OnOffType.ON));
			aovs.put(Constants.AnalogOutput_2_Volt, new IOAnalogOutputCtrlVolt(BBB.getPin(BBBNames.P9_29).as(DigitalOutput.class), OnOffType.ON));
			aovs.put(Constants.AnalogOutput_3_Volt, new IOAnalogOutputCtrlVolt(BBB.getPin(BBBNames.P9_30).as(DigitalOutput.class), OnOffType.ON));
			aovs.put(Constants.AnalogOutput_4_Volt, new IOAnalogOutputCtrlVolt(BBB.getPin(BBBNames.P9_31).as(DigitalOutput.class), OnOffType.ON));
			ios.putAll(aovs);

			try {
				ios.put(Constants.AnalogOutput_1, new IOAnalogOutput(
						BBB.getPin(BBBNames.EHRPWM1A_P9_14).as(Pwm.class), OnOffType.OFF, aovs.get(Constants.AnalogOutput_1_Volt)));
			} catch (RuntimeException e) { logger.error(e.getMessage()); }
			
			try {
				ios.put(Constants.AnalogOutput_2, new IOAnalogOutput(
						BBB.getPin(BBBNames.EHRPWM1B_P9_16).as(Pwm.class), OnOffType.OFF, aovs.get(Constants.AnalogOutput_2_Volt)));
			} catch (RuntimeException e) { logger.error(e.getMessage()); }
			try {
				ios.put(Constants.AnalogOutput_3, new IOAnalogOutput(
						BBB.getPin(BBBNames.EHRPWM2A_P8_19).as(Pwm.class), OnOffType.OFF, aovs.get(Constants.AnalogOutput_3_Volt)));
			} catch (RuntimeException e) { logger.error(e.getMessage()); }
			try {
				ios.put(Constants.AnalogOutput_4, new IOAnalogOutput(
					BBB.getPin(BBBNames.EHRPWM2B_P8_13).as(Pwm.class), OnOffType.OFF, aovs.get(Constants.AnalogOutput_4_Volt)));
			} catch (RuntimeException e) { logger.error(e.getMessage()); }
			
			/* TODO: Deactivated for now, because it's causing "epoll failed! Interrupted system call"
			// Digital Inputs
			DigitalInput di1 = BBB.getPin(BBBNames.P9_42).as(DigitalInput.class);
			IODigitalInput dio1 = new IODigitalInput(di1);
			di1.addInterruptListener(new DigitalInputInterrupt(Constants.DigitalInput_1, dio1));
			di1.enableInterrupts();
			ios.put(Constants.DigitalInput_1, dio1);

			DigitalInput di2 = BBB.getPin(BBBNames.P9_27).as(DigitalInput.class);
			IODigitalInput dio2 = new IODigitalInput(di2);
			di2.addInterruptListener(new DigitalInputInterrupt(Constants.DigitalInput_2, dio2));
			di2.enableInterrupts();
			ios.put(Constants.DigitalInput_2, dio2);
			
			DigitalInput di3 = BBB.getPin(BBBNames.P9_41).as(DigitalInput.class);
			IODigitalInput dio3 = new IODigitalInput(di3);
			di3.addInterruptListener(new DigitalInputInterrupt(Constants.DigitalInput_3, dio3));
			di3.enableInterrupts();
			ios.put(Constants.DigitalInput_3, dio3);
			
			DigitalInput di4 = BBB.getPin(BBBNames.P9_25).as(DigitalInput.class);
			IODigitalInput dio4 = new IODigitalInput(di4);
			di4.addInterruptListener(new DigitalInputInterrupt(Constants.DigitalInput_4, dio4));
			di4.enableInterrupts();
			ios.put(Constants.DigitalInput_4, dio4);
			*/
			
			// TODO: Analogue Inputs

			// update all states (but actually no listener is registered till now most likely)
			for (Entry<String, IO> entry : ios.entrySet()) {
				updateState(entry.getKey(), entry.getValue());
			}
		}
	}
	
	@Override
	protected void dispose() {
		// dispose all IOs
		for (Entry<String, IO> entry : ios.entrySet()) {
			entry.getValue().dispose();
			updateState(entry.getKey(), entry.getValue());
		}
		// dispose lcd
		if(lcd != null) lcd.dispose();
		super.dispose();
	}
	
	private void updateState(String id, IO io) {
		org.eclipse.smarthome.core.types.State state = io.getState();
		if(io.sendToOnlineMonitoring()) {
			Map<String, org.eclipse.smarthome.core.types.State> states = new HashMap<String, org.eclipse.smarthome.core.types.State>();
			states.put(id, state);
			Constants.ONLINE_MONITORING_AGENT.sendData(
				MethodType.IO,
				Tools.convertStatesForMessage(states));	
		}
		for(IOAgentListener listener : listeners) {
			listener.ioUpdate(id, state);
		}
	}

	public void addListener(IOAgentListener listener) {
		handle(new ListenerMessage(listener));
	}
	
	public void handleCommand(String id, Command command) {
		handle(new HandleCommandMessage(id, command));
	}
	
	public void updateTime() {
		handle(new UpdateLcdTimeMessage());
	}
	
	/* Helper function to set text on LCD row two
	 */
	public void setLcdText(String value) {
		setLcdText(false, value); // default: second row
	}
	public void setLcdText(boolean firstRow, String value) {
		if(firstRow) {
			handleCommand(Constants.LCD_1, new StringType(value));
		} else {
			handleCommand(Constants.LCD_2, new StringType(value));
		}
	}
}

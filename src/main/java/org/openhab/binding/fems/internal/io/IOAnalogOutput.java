/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.internal.io;

import org.bulldog.core.gpio.DigitalOutput;
import org.bulldog.core.gpio.Pwm;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;

public class IOAnalogOutput implements IOOutput {
	//private Logger logger = LoggerFactory.getLogger(IOAnalogOutput.class);
	public enum DIVIDE {
		AMPERE, VOLTAGE
	}
	
	public static final double FREQUENCY = 5000; // 5 kHz
	
	private Pwm pwmPin;
	private DigitalOutput divideCtrlPin;
	private DIVIDE divide;
	
	public IOAnalogOutput(Pwm pwmPin, DigitalOutput divideCtrlPin, DIVIDE divide) {
		this.pwmPin = pwmPin;
		this.divideCtrlPin = divideCtrlPin;
		this.divide = divide;
	}

	@Override
	public State getState() {
		double duty = pwmPin.getDuty();
		return toPercentType(duty);
	}

	@Override
	public void handleCommand(Command command) {
		if(command instanceof OnOffType) {
			OnOffType cmd = (OnOffType) command;
			switch(cmd) {
			case ON:
				setAnalogOutput(1);
				break;
			case OFF:
				setAnalogOutput(0);
				break;
			}
		} else if (command instanceof IncreaseDecreaseType) {
			IncreaseDecreaseType cmd = (IncreaseDecreaseType) command;
			int currentValue = toPercentType( pwmPin.getDuty() ).intValue();
			switch(cmd) {
			case INCREASE:
				setAnalogOutput(toDuty(new PercentType(currentValue + 1)));
				break;
			case DECREASE:
				setAnalogOutput(toDuty(new PercentType(currentValue - 1)));
				break;
			}			
		} else if (command instanceof PercentType) {
			PercentType cmd = (PercentType) command;
			setAnalogOutput(toDuty(cmd));
		}
	}
	
	private PercentType toPercentType(double duty) {
		if(duty >= 1) {
			return PercentType.HUNDRED;
		} else if(duty <= 0){
			return PercentType.ZERO;
		} else {
			return new PercentType((int)Math.round(duty * 100));
		}	
	}
	
	private double toDuty(PercentType percent) {
		return(percent.doubleValue() / 100);
	}
	
	private void setAnalogOutput(double duty) {
		if(divide == DIVIDE.AMPERE) {
			divideCtrlPin.low();			
		} else {
			divideCtrlPin.high();
		}
		pwmPin.setFrequency(FREQUENCY);
		pwmPin.setDuty(duty);
		pwmPin.enable();		
	}
}

package org.openhab.binding.fems.agents.io.types;

import org.bulldog.core.gpio.DigitalOutput;
import org.eclipse.smarthome.core.library.types.OnOffType;

/**
 * ON: control VOLTAGE
 * OFF: control AMPERE
 * 
 * @author Stefan Feilmeier
 */
public class IOAnalogOutputCtrlVolt extends IODigitalOutput {

	public IOAnalogOutputCtrlVolt(DigitalOutput pin, OnOffType init) {
		super(pin, init);
	}
}

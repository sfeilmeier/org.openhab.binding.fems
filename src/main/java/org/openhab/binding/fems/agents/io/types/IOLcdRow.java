package org.openhab.binding.fems.agents.io.types;

import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.fems.Constants;

public class IOLcdRow implements IOOutput {
	private final int row;
	
	private final IOLcd lcd;
	private StringType value = new StringType("");
	
	public IOLcdRow(IOLcd lcd, String id) {
		this.lcd = lcd;
		switch (id) {
		case Constants.LCD_1:
			this.row = 0;
			break;
		case Constants.LCD_2:
		default:
			this.row = 1;
			break;
		}
	}
	
	@Override
	public State getState() {
		return value;
	}

	@Override
	public void handleCommand(Command command) {
		if(command instanceof StringType) {
			lcd.handleCommand(row, (StringType)command);
		}
	}

	@Override
	public void dispose() {
		// nothing to do here
	}

	@Override
	public boolean sendToOnlineMonitoring() {
		return false;
	}
}

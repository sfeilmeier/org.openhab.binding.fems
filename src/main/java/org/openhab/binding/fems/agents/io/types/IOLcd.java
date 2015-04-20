/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.agents.io.types;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bulldog.core.gpio.DigitalIO;
import org.bulldog.devices.lcd.HD44780Compatible;
import org.bulldog.devices.lcd.LcdFont;
import org.bulldog.devices.lcd.LcdMode;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.fems.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOLcd {
	private final HD44780Compatible lcd;
	private final static Logger logger = LoggerFactory.getLogger(IOLcd.class);
	
	private String firstRow = "";
	private String secondRow = ""; 
	
	private class UpdateLcdTime implements Runnable {
		public void run() {
			Constants.IO_AGENT.updateTime();
		}
	}
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final UpdateLcdTime updateLcdTime = new UpdateLcdTime();
	private ScheduledFuture<?> scheduledFuture = null;
	
	public IOLcd(DigitalIO rs, DigitalIO rw,
			DigitalIO enable, DigitalIO db4, DigitalIO db5, DigitalIO db6, DigitalIO db7) {
		lcd = new HD44780Compatible(rs, rw, enable, db4, db5, db6, db7);
		lcd.setMode(LcdMode.Display2x16, LcdFont.Font_5x8);
        lcd.blinkCursor(false);
        lcd.showCursor(false);
        lcd.clear(); // turn off by default
        scheduledFuture = scheduler.scheduleAtFixedRate(updateLcdTime, 0, 1, TimeUnit.SECONDS);
	}
	
	public void updateLcdTime() {
		handleCommand(0, RefreshType.REFRESH);
	}
	
	public void handleCommand(int row, Command command) {
		String lastValue = "";
		String newValue = "";
		if(command instanceof StringType) {
			String commandString = ((StringType)command).toString();
			if(row == 0) {
				newValue = commandString;
				lastValue = firstRow;
			} else {
				String dateString = Constants.SHORT_TIME_FORMAT.format(new Date());
				newValue = (dateString.length() + commandString.length() > 16) ? commandString : dateString + " " + commandString;
				lastValue = secondRow;
			}
		}
		if(row == 0 && newValue.isEmpty()) newValue = "FEMS";
		
		if(newValue.compareTo(lastValue) != 0 || command instanceof RefreshType) {
			if(row == 0) {
				String dateString = Constants.LONG_TIME_FORMAT.format(new Date());
				newValue = (dateString.length() + newValue.length() > 16)
						? String.format("%1$-16s", newValue)
						: String.format("%-7s %-8s", newValue, dateString);
				firstRow = newValue;
			} else {
				secondRow = newValue;
				Constants.IO_AGENT.handleCommand("LCD_Backlight", OnOffType.ON);
			}
			lcd.writeAt(row, 0, String.format("%1$-16s", newValue));
		}
	}
	
	public void dispose() {
		logger.info("Shutdown: clear LCD");
		if(scheduledFuture != null) {
			scheduledFuture.cancel(false);
		}
		lcd.clear();
	}
	
	public String getFirstRow() {
		return firstRow;
	}
	
	public String getSecondRow() {
		return secondRow;
	}
}

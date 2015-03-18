/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.bulldog.beagleboneblack.BBBNames;
import org.bulldog.core.gpio.DigitalIO;
import org.bulldog.core.gpio.DigitalOutput;
import org.bulldog.core.gpio.Pwm;
import org.bulldog.core.io.PinIOGroup;
import org.bulldog.core.platform.Board;
import org.bulldog.devices.lcd.HD44780Compatible;
import org.bulldog.devices.lcd.HD44780Mode;
import org.bulldog.devices.lcd.Lcd;
import org.bulldog.devices.lcd.LcdFont;
import org.bulldog.devices.lcd.LcdMode;
import org.openhab.binding.fems.FEMSBindingConstants;
import org.openhab.binding.fems.internal.io.IOAnalogOutput;

public class FEMSDisplayAgent extends Thread {
	private final static double backlightOnValue = 0.70;
	private final static double backlightOffValue = 0.20;
	private final static int backlightSeconds = 120;
	private final static SimpleDateFormat longTimeFormat = new SimpleDateFormat("HH:mm:ss");
    private final static SimpleDateFormat shortTimeFormat = new SimpleDateFormat("HH:mm");
    
	private static ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
	private static FEMSDisplayAgent femsDisplayAgent = null;
	
    public static FEMSDisplayAgent getFEMSDisplay() {
    	if(femsDisplayAgent == null) {
    		femsDisplayAgent = new FEMSDisplayAgent();	
    		femsDisplayAgent.start();
    	}
    	return femsDisplayAgent;
    }
    
    private volatile boolean stop = false; 
    private volatile String offeredFirstRow = "";
    public void stopAgent() {
    	stop = true;
    }
    public void offerFirstRow(String text) {
    	offeredFirstRow = text;
    }

	@Override
	public void run() {
		/* LCD Display */
    	Board bbb = FEMSBindingConstants.BBB;
        PinIOGroup ioGroup = new PinIOGroup(bbb.getPin(BBBNames.P9_12).as(DigitalIO.class),  //enable pin
    		bbb.getPin(BBBNames.P8_30).as(DigitalIO.class),  //db 4
    		bbb.getPin(BBBNames.P8_28).as(DigitalIO.class),  //db 5
    		bbb.getPin(BBBNames.P8_29).as(DigitalIO.class),  //db 6
    		bbb.getPin(BBBNames.P8_27).as(DigitalIO.class)   //db 7
		);
        final Lcd lcd = new HD44780Compatible(bbb.getPin(BBBNames.P9_15).as(DigitalOutput.class), //rs pin
            bbb.getPin(BBBNames.P9_23).as(DigitalOutput.class), //rw pin
            ioGroup,
            HD44780Mode.FourBit);
        lcd.setMode(LcdMode.Display2x16, LcdFont.Font_5x8);
        lcd.blinkCursor(false);
        lcd.showCursor(false);
        /* LCD Dimm */
        final Pwm backlight = bbb.getPin(BBBNames.P9_22).as(Pwm.class);
    	backlight.setFrequency(IOAnalogOutput.FREQUENCY);
    	backlight.setDuty(0);
    	backlight.enable();
    	/* Turn LCD off on shutdown */
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	lcd.clear();
            	backlight.setDuty(0);
            }
        });
    	/* Forever-Loop */
    	String firstRow = "", lastFirstRow = "";
    	String secondRow = "", lastSecondRow = "";
    	Date now = null;
    	Date backlightTurnedOnDate = null;
    	double backlightValue = 0;
    	try {
	    	while(!stop) {
	    		/* write lcd */
	    		now = new Date();
	    		if(offeredFirstRow.isEmpty()) offeredFirstRow = "FEMS";
	    		firstRow = String.format("%-7s %-8s", offeredFirstRow, longTimeFormat.format(now.getTime()));
	    		secondRow = queue.poll();
	    		if(firstRow != lastFirstRow) {
	    			lcd.writeAt(0, 0, firstRow);
	    		}
	    		if(secondRow != lastSecondRow && secondRow != null) {
	    			lcd.writeAt(1, 0, secondRow);
	    			backlightValue = backlightOnValue;
	    			backlight.setDuty(backlightOnValue);
	    			backlightTurnedOnDate = now;
	    		}
	    		/* slowly decrease backlight */
	    		if(backlightTurnedOnDate != null) {
		    		if(TimeUnit.MILLISECONDS.toSeconds(now.getTime() - backlightTurnedOnDate.getTime()) > backlightSeconds) {
		    			backlightValue -= 0.05;
		    			if(backlightValue <= backlightOffValue) {
		    				backlightTurnedOnDate = null;
		    				backlight.setDuty(backlightOffValue);
		    			} else {
		    				backlight.setDuty(backlightValue);
		    			}
		    		}
	    		}
	    		/* prepare next run */
	    		lastFirstRow = firstRow;
	    		lastSecondRow = secondRow;
	    		Thread.sleep(100);
	    	}
		} catch (InterruptedException e) { ; }
    	FEMSDisplayAgent.femsDisplayAgent = null;
	}

	public void offer(String text, boolean withTime) {
    	if(withTime) {
    		text = shortTimeFormat.format(new Date().getTime()) + " " + text;
    	}
		queue.offer(String.format("%1$-16s", text));
	}
}
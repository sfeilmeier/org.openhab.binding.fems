package org.openhab.binding.fems.agents.io.types;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bulldog.core.gpio.Pwm;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.fems.Constants;

public class IOLcdBacklight extends IOAnalogOutput {
	public final static float BACKLIGHT_MAX_VALUE = 0.70f;
	public final static float BACKLIGHT_MIN_VALUE = 0.20f;
	public final static int BACKLIGHT_SECONDS = 120;
	private final String myId;
	
	private class TurnBacklightOff implements Runnable {
		public void run() {
			Constants.IO_AGENT.handleCommand(myId, OnOffType.OFF);
		}
	}
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final TurnBacklightOff turnBacklightOff = new TurnBacklightOff(); 
	private ScheduledFuture<?> scheduledFuture = null;
	
	/**
	 * 
	 * @param myId required, to schedule myself correctly via IO_AGENT
	 * @param pwmPin
	 */
	public IOLcdBacklight(String myId, Command initState, Pwm pwmPin) {
		super(pwmPin, initState, null);
		this.myId = myId;
	}
	
	@Override
	protected void setAnalogOutput(float duty) {
		if(duty > BACKLIGHT_MAX_VALUE) {
			duty = BACKLIGHT_MAX_VALUE;
		} else if(duty < BACKLIGHT_MIN_VALUE) {
			duty = BACKLIGHT_MIN_VALUE;
		}
		if(duty != BACKLIGHT_MIN_VALUE) {
			// stop earlier schedulings
			if(scheduledFuture != null) {
				scheduledFuture.cancel(false);
			}
			// schedule turning backlight off after BACKLIGHT_SECONDS
			scheduledFuture = scheduler.schedule(turnBacklightOff, BACKLIGHT_SECONDS, TimeUnit.SECONDS);
		}
		super.setAnalogOutput(duty);
	}
	
	@Override
	public boolean sendToOnlineMonitoring() {
		return false;
	}
}

/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.handler;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.wimpi.modbus.ModbusException;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.fems.FEMSBindingConstants;
import org.openhab.binding.fems.internal.essprotocol.ESSProtocol;
import org.openhab.binding.fems.internal.essprotocol.modbus.BitWordElement;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElement;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElementRange;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusItem;
import org.openhab.binding.fems.internal.essprotocol.modbus.OnOffBitItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.fems.agents.OnlineMonitoring.Message.DataMessage;

public abstract class ESSHandler extends BaseThingHandler {
	private Logger logger = LoggerFactory.getLogger(ESSHandler.class);
	
	public ESSHandler(Thing thing) {
		super(thing);
	}
	
	protected final int refresh = 60; // refresh every minute as default 
	protected int unitid = 100;
	protected String modbusDevice = "ttyUSB*";
	protected ESSProtocol protocol;
	
	ScheduledFuture<?> refreshJob;
	
	@Override
	public void dispose() {
		super.dispose();
		refreshJob.cancel(true);
		
		protocol.dispose();
	}
	
	protected abstract ESSProtocol getProtocol();
	
	@Override
	public void initialize() {	
		logger.info("initialize");
		// Read configuration
		Configuration config = getThing().getConfiguration();	
		try {
			String unitidString = (String)config.get("unitid");
			if(unitidString != null) {
				unitid = Integer.parseInt(unitidString);
			}
			logger.info("Set Unit-ID to " + unitid);
		} catch(Exception e) { /* let's ignore it and go for the default */ }

		protocol = getProtocol();
		
		// Start refresh service
		logger.info("startAutomaticRefresh");
		startAutomaticRefresh();
		
		super.initialize();
	}
	
	private void startAutomaticRefresh() {
		final Runnable runnable = new Runnable() {
			private int totalWaitTime = 0; // counts the total waited time in seconds for ModbusErrors
			public void run() {
				try {
					protocol.updateData();
					for (ModbusElementRange wordRange : protocol.getWordRanges()) {
						for (ModbusElement word : wordRange.getWords()) {
							if(word instanceof ModbusItem) {
								ModbusItem item = (ModbusItem)word;
//								logger.info("Number " + item.getName());
								updateState(new ChannelUID(getThing().getUID(), item.getName()), item.getState());
							} else if (word instanceof BitWordElement) {
								BitWordElement bitWord = (BitWordElement)word;
								for (OnOffBitItem bitItem : bitWord.getBitItems()) {
//									logger.info("Switch " + bitWord.getName() + "_" + bitItem.getName());
									updateState(new ChannelUID(getThing().getUID(), bitWord.getName() + "_" + bitItem.getName()), bitItem.getState());
								} 
							}
						}
					}
					
					// no error happened: kick the watchdog
				    Runtime.getRuntime().exec("/bin/systemd-notify WATCHDOG=1");
					
					// prepare data to be transfered
				    HashMap<String, Object> params = new HashMap<String, Object>(); 
					try {
						NetworkInterface n = NetworkInterface.getByName("eth0");
						Enumeration<InetAddress> ee = n.getInetAddresses();
						while (ee.hasMoreElements()) {
							InetAddress i = (InetAddress) ee.nextElement();
							if(i instanceof Inet4Address) {
								params.put("ipv4", i.getHostAddress()); // local ipv4 address
					        }
					    }
					} catch (SocketException e) { /* no IP-Address - ignore */ }
					
					DataMessage message = protocol.getDataMessage(params);
					FEMSBindingConstants.ONLINE_MONITORING_AGENT.sendData(message);
					
		        	totalWaitTime = 0;
				} catch(ModbusException e) {
					// modbus error: Try again within a timespan of 10 to 30 seconds:
					int min = 10; int max = 30;
					try {
						int waitTime = (int)(Math.random() * (max - min) + min);
						totalWaitTime += waitTime;
						if(totalWaitTime > refresh) {
							logger.info("Not waiting anymore... hoping for next regular run");
							totalWaitTime = 0;
						} else {
							logger.info("Try again in " + waitTime + " seconds");
							protocol.closeSerialConnection();
							Thread.sleep((int)(Math.random() * (max - min) + min)*1000);
							run();
						}
					} catch (InterruptedException e1) { ; }
					
				} catch(Exception e) {
					logger.error("Exception occurred during execution: {}", e.getMessage());
					e.printStackTrace();
					protocol.dispose();
				}
			}
		};
		refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, refresh, TimeUnit.SECONDS);
	}
	
	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		logger.info("handleCommand()");
		logger.info("ChannelUID: " + channelUID);
		logger.info("Command: " + command);
	    if (command instanceof RefreshType) {
	        try {
	        	protocol.updateData();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
	        logger.info("TODO");
	        // TODO updateState(channelUID, getModbusData(channelUID.getId()));
	    }
	}
}

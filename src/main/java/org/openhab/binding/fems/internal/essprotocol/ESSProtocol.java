/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.internal.essprotocol;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.msg.ExceptionResponse;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;

import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.fems.Constants;
import org.openhab.binding.fems.agents.onlinemonitoring.message.DataMessage;
import org.openhab.binding.fems.agents.onlinemonitoring.message.DataMessage.MethodType;
import org.openhab.binding.fems.internal.essprotocol.modbus.BitWordElement;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElement;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElementRange;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusItem;
import org.openhab.binding.fems.internal.essprotocol.modbus.OnOffBitItem;
import org.openhab.binding.fems.tools.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ESSProtocol {
	private Logger logger = LoggerFactory.getLogger(ESSProtocol.class);
	
	private static SerialConnection serialConnection = null; // only use within serialConnectionLock synchronized block!
	private static Object serialConnectionLock = new Object();
	protected ArrayList<ModbusElementRange> wordRanges;
	
	protected String modbusDevice;
	protected int unitid;
	
    public ESSProtocol(String modbusDevice, int unitid, ArrayList<ModbusElementRange> wordRanges) {
		this.wordRanges = wordRanges;
		this.modbusDevice = modbusDevice;
		this.unitid = unitid;
	}
    
	public void dispose() {
		for (ModbusElementRange wordRange : wordRanges) {
			wordRange.dispose();
		}
		synchronized (serialConnectionLock) {
			if(serialConnection!=null) {
				if(serialConnection.isOpen()) {
					serialConnection.close();
				}
				serialConnection = null;
			}			
		}
	}
	
	public ArrayList<ModbusElementRange> getWordRanges() {
		return wordRanges;
	}
	
	public void updateData() throws Exception {
		SerialConnection serialCon = getSerialConnection();
		synchronized (serialConnectionLock) {
			for (ModbusElementRange wordRange : wordRanges) {
				ModbusSerialTransaction trans = wordRange.getModbusSerialTransaction(serialCon, unitid);
				trans.setRetries(1);
				trans.execute();
				ModbusResponse res = trans.getResponse();
				
				if (res instanceof ReadMultipleRegistersResponse) {
					wordRange.updateData((ReadMultipleRegistersResponse)res);
		    	} else if (res instanceof ExceptionResponse) {
		    		throw new Exception("Modbus exception response:" + ((ExceptionResponse)res).getExceptionCode());
		    	} else {
		    		throw new Exception("Undefined Modbus response");
		    	}
				Thread.sleep(100);
			}		
		}
	}
	
	public abstract DataMessage getDataMessage(Map<String, Object> params);
	
	protected DataMessage getDataMessage(MethodType contentType, Map<String, Object> params) {
		HashMap<String, State> states = new HashMap<String, State>();
		for (ModbusElementRange wordRange : getWordRanges()) {
			for (ModbusElement word : wordRange.getWords()) {
				if(word instanceof ModbusItem) {
					ModbusItem item = (ModbusItem)word;
					states.put(item.getName(), item.getState());
				} else if (word instanceof BitWordElement) {
					BitWordElement bitWord = (BitWordElement)word;
					for (OnOffBitItem bitItem : bitWord.getBitItems()) {
						states.put(bitWord.getName() + "_" + bitItem.getName(), bitItem.getState());
					} 
				}
			}
		}
		return new DataMessage(contentType, 
				Tools.convertStatesForMessage(states), 
				params);
	}
	
	public void closeSerialConnection() {
		synchronized (serialConnectionLock) {
			if(serialConnection!=null) {
				serialConnection.close();
			}
		}
	}
	
	public SerialConnection getSerialConnection() throws Exception {
		synchronized (serialConnectionLock) {
			if(serialConnection==null) {
				// find first matching device
				String portName = "/dev/ttyUSB0"; // if no file found: use default
				try (DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get("/dev"), modbusDevice)) {
				    for(Path file : files) {
				    	portName = file.toAbsolutePath().toString();
				    	logger.info("Set modbus portname: " + portName);
				    }
				} catch(Exception e) {
					logger.info("Error trying to find " + modbusDevice + ": " + e.getMessage());
					e.printStackTrace();
				}
				SerialParameters params = new SerialParameters();
				params.setPortName(portName);
				params.setBaudRate(getBaudrate());
				params.setDatabits(8);
				params.setParity("None");
				params.setStopbits(1);
				params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
				params.setEcho(false);
				params.setReceiveTimeout(Constants.MODBUS_TIMEOUT);
				ESSProtocol.serialConnection = new SerialConnection(params);
			}
			if(!ESSProtocol.serialConnection.isOpen()) {
				ESSProtocol.serialConnection.open();
			}
			return serialConnection;
		}
	}
	
	protected abstract int getBaudrate();
}

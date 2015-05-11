/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems;

import java.text.SimpleDateFormat;
import java.util.Collection;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.fems.agents.ess.ESSAgent;
import org.openhab.binding.fems.agents.io.IOAgent;
import org.openhab.binding.fems.agents.onlinemonitoring.OnlineMonitoringAgent;
import org.openhab.binding.fems.agents.onlinemonitoring.OnlineMonitoringCacheAgent;

import com.google.common.collect.Lists;

/**
 * The {@link FEMSBinding} class defines common constants, which are 
 * used across the whole binding.
 * 
 * @author Stefan Feilmeier - Initial contribution
 */
public class Constants {

    public static final String BINDING_ID = "fems";
    
    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_CESS = new ThingTypeUID(BINDING_ID, "cess");
    public final static ThingTypeUID THING_TYPE_DESS = new ThingTypeUID(BINDING_ID, "dess");
    public final static ThingTypeUID THING_TYPE_WEATHER = new ThingTypeUID(BINDING_ID, "weather");
    public final static ThingTypeUID THING_TYPE_IO = new ThingTypeUID(BINDING_ID, "io");
    
    // Collection of Thing Type UIDs
    public final static Collection<ThingTypeUID> SUPPORTED_THING_TYPES = 
    		Lists.newArrayList(THING_TYPE_CESS, THING_TYPE_DESS, THING_TYPE_WEATHER, THING_TYPE_IO);   
    
    // Date and Time formats
    public final static SimpleDateFormat SHORT_TIME_FORMAT = new SimpleDateFormat("HH:mm");
    public final static SimpleDateFormat LONG_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public final static SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    
    // Agents
    public final static OnlineMonitoringCacheAgent ONLINE_MONITORING_CACHE_AGENT = 
    		new OnlineMonitoringCacheAgent("Online-Monitoring Cache");
    public final static OnlineMonitoringAgent ONLINE_MONITORING_AGENT = 
    		new OnlineMonitoringAgent("Online-Monitoring", ONLINE_MONITORING_CACHE_AGENT);
    public final static IOAgent IO_AGENT =
    		new IOAgent("IO");
    public final static ESSAgent ESS_AGENT =
    		new ESSAgent("ESS");
    
    // IO Items
	public final static String LCD_1 = "LCD_1";
	public final static String LCD_2 = "LCD_2";
	public final static String LCD_Backlight = "LCD_Backlight";
	public final static String UserLED_1 = "UserLED_1";
	public final static String UserLED_2 = "UserLED_2";
	public final static String UserLED_3 = "UserLED_3";
	public final static String UserLED_4 = "UserLED_4";
	public final static String RelayOutput_1 = "RelayOutput_1";
	public final static String RelayOutput_2 = "RelayOutput_2";
	public final static String RelayOutput_3 = "RelayOutput_3";
	public final static String RelayOutput_4 = "RelayOutput_4";
	public final static String AnalogOutput_1_Volt = "AnalogOutput_1_Volt";
	public final static String AnalogOutput_2_Volt = "AnalogOutput_2_Volt";
	public final static String AnalogOutput_3_Volt = "AnalogOutput_3_Volt";
	public final static String AnalogOutput_4_Volt = "AnalogOutput_4_Volt";
	public final static String AnalogOutput_1 = "AnalogOutput_1";
	public final static String AnalogOutput_2 = "AnalogOutput_2";
	public final static String AnalogOutput_3 = "AnalogOutput_3";
	public final static String AnalogOutput_4 = "AnalogOutput_4";
	public final static String DigitalInput_1 = "DigitalInput_1";
	public final static String DigitalInput_2 = "DigitalInput_2";
	public final static String DigitalInput_3 = "DigitalInput_3";
	public final static String DigitalInput_4 = "DigitalInput_4";
	
	public final static int MODBUS_TIMEOUT = 3000;
	public final static int MODBUS_TRANS_DELAY_MS = 1000;
	public final static int MODBUS_RETRIES = 10;
	public final static String MODBUS_DEVICE = "ttyUSB*";
	public final static int MODBUS_UNITID_DESS = 4;
	public final static int MODBUS_UNITID_CESS = 100;
	public final static int ESS_PERIOD = 60; // in seconds
}

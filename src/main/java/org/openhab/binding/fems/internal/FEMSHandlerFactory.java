/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.internal;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.fems.FEMSBindingConstants;
import org.openhab.binding.fems.handler.CESSHandler;
import org.openhab.binding.fems.handler.DESSHandler;
import org.openhab.binding.fems.handler.IOHandler;
import org.openhab.binding.fems.handler.WeatherHandler;
import org.openhab.binding.fems.tools.FEMSDisplayAgent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.fems.scheduler.agents.OnlineMonitoring.OnlineMonitoringAgentMessage.ApikeyMessage;
import de.fenecon.fems.scheduler.agents.OnlineMonitoring.OnlineMonitoringAgentMessage.SystemMessage;

/**
 * The {@link FEMSHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Stefan Feilmeier - Initial contribution
 */
public class FEMSHandlerFactory extends BaseThingHandlerFactory {
	private Logger logger = LoggerFactory.getLogger(FEMSHandlerFactory.class); 
    
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return FEMSBindingConstants.SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected void activate(ComponentContext componentContext) {
    	super.activate(componentContext);
    
    	logger.info("activate");
	
		// remove old RS485 lock file
		try {
			Files.deleteIfExists(Paths.get("/var/lock/LCK..ttyUSB0"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	
    	// Turn 4. UserLED on
    	try {
			Files.write(Paths.get("/sys/class/leds/beaglebone:green:usr3/brightness"), "1".getBytes());
		} catch (IOException e1) { logger.error(e1.getMessage()); }
    	
		// LCD Display
    	try {
			NetworkInterface n = NetworkInterface.getByName("eth0");
			Enumeration<InetAddress> ee = n.getInetAddresses();
			while (ee.hasMoreElements()) {
				InetAddress i = (InetAddress) ee.nextElement();
				if(i instanceof Inet4Address) {
					FEMSDisplayAgent.getFEMSDisplay().offer(" " + i.getHostAddress(), false);
		        }
		    }
    	} catch (SocketException e) { /* no IP-Address - ignore */ }
 
		// read FEMS properties from /etc/fems
		Properties properties = new Properties();
		BufferedInputStream stream = null;
		try {
			stream = new BufferedInputStream(new FileInputStream("/etc/fems"));
			properties.load(stream);
			if(stream != null) stream.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		// start OnlineMonitoringAgent
		FEMSBindingConstants.onlineMonitoringAgent.start();
		FEMSBindingConstants.onlineMonitoringAgent.message(new ApikeyMessage(properties.getProperty("apikey")));

		// send init message to FEMS Online-Monitoring
		FEMSBindingConstants.onlineMonitoringAgent.message(new SystemMessage("openHAB started"));
	};

	@Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(FEMSBindingConstants.THING_TYPE_CESS)) {
            return new CESSHandler(thing);
        } else if (thingTypeUID.equals(FEMSBindingConstants.THING_TYPE_DESS)) {
        	return new DESSHandler(thing);
        } else if (thingTypeUID.equals(FEMSBindingConstants.THING_TYPE_WEATHER)) {
        	return new WeatherHandler(thing);
        } else if (thingTypeUID.equals(FEMSBindingConstants.THING_TYPE_IO)) {
        	return new IOHandler(thing);
        }
        return null;
    }
}

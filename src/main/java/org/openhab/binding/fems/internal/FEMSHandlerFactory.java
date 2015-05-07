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
import java.net.InetAddress;
import java.util.Properties;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.fems.Constants;
import org.openhab.binding.fems.handler.ESSHandler;
import org.openhab.binding.fems.handler.IOHandler;
import org.openhab.binding.fems.handler.WeatherHandler;
import org.openhab.binding.fems.internal.essprotocol.CESSProtocolFactory;
import org.openhab.binding.fems.internal.essprotocol.DESSProtocolFactory;
import org.openhab.binding.fems.internal.essprotocol.ESSProtocol;
import org.openhab.binding.fems.tools.FEMSInit;
import org.openhab.binding.fems.tools.Tools;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        return Constants.SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected void activate(ComponentContext componentContext) {
    	super.activate(componentContext);
 
    	logger.info("Activated FEMS Binding");
 
		// read FEMS properties from /etc/fems
		Properties properties = new Properties();
		BufferedInputStream stream = null;
		String apikey = null;
		String ess = "dess";
		boolean debug = false;
		try {
			stream = new BufferedInputStream(new FileInputStream("/etc/fems"));
			properties.load(stream);
			apikey = properties.getProperty("apikey");
			ess = properties.getProperty("ess", "dess");
			debug = Boolean.parseBoolean(properties.getProperty("debug", "false"));
			if(stream != null) stream.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
    	
		ESSProtocol protocol;
		if(ess.equals("cess")) {
			protocol = CESSProtocolFactory.getProtocol();
		} else {
			protocol = DESSProtocolFactory.getProtocol();
		}
		
		// start Agents
		Constants.IO_AGENT.start();
		Constants.ONLINE_MONITORING_AGENT.setApikey(apikey);
		Constants.ONLINE_MONITORING_AGENT.start();
		Constants.ONLINE_MONITORING_CACHE_AGENT.setApikey(apikey);
		Constants.ONLINE_MONITORING_CACHE_AGENT.start();	
		Constants.ESS_AGENT.setProtocol(protocol);
		Constants.ESS_AGENT.start();
		
		// initialize FEMS
		FEMSInit femsInit = new FEMSInit();
		Constants.ESS_AGENT.addListener(femsInit);
		femsInit.init(ess, debug);

    	// LCD Display
		InetAddress i = Tools.getIPaddress();
		if(i != null) {
			Constants.IO_AGENT.setLcdText(" " + i.getHostAddress());
		}
	};

	@Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(Constants.THING_TYPE_CESS) || thingTypeUID.equals(Constants.THING_TYPE_DESS)) {
        	ESSHandler essHandler = new ESSHandler(thing);
        	Constants.ESS_AGENT.addListener(essHandler);
			Constants.IO_AGENT.handleCommand(Constants.UserLED_4, OnOffType.ON); // Turn 4. UserLED on
            return essHandler;
        } else if (thingTypeUID.equals(Constants.THING_TYPE_WEATHER)) {
        	return new WeatherHandler(thing);
        } else if (thingTypeUID.equals(Constants.THING_TYPE_IO)) {
        	IOHandler ioHandler = new IOHandler(thing);
        	Constants.IO_AGENT.addListener(ioHandler);
        	return ioHandler;
        }
        return null;
    }
	
	// TODO: implement unregister methods, so that we don't need to restart completely all the time
}


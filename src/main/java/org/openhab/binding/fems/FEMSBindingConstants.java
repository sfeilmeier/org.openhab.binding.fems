/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems;

import java.util.Collection;



import org.bulldog.core.platform.Board;
import org.bulldog.core.platform.Platform;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.Lists;

import de.fenecon.fems.agents.OnlineMonitoring.OnlineMonitoringAgent;

/**
 * The {@link FEMSBinding} class defines common constants, which are 
 * used across the whole binding.
 * 
 * @author Stefan Feilmeier - Initial contribution
 */
public class FEMSBindingConstants {

    public static final String BINDING_ID = "fems";
    
    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_CESS = new ThingTypeUID(BINDING_ID, "cess");
    public final static ThingTypeUID THING_TYPE_DESS = new ThingTypeUID(BINDING_ID, "dess");
    public final static ThingTypeUID THING_TYPE_WEATHER = new ThingTypeUID(BINDING_ID, "weather");
    public final static ThingTypeUID THING_TYPE_IO = new ThingTypeUID(BINDING_ID, "io");
    
    // Collection of Thing Type UIDs
    public final static Collection<ThingTypeUID> SUPPORTED_THING_TYPES = 
    		Lists.newArrayList(THING_TYPE_CESS, THING_TYPE_DESS, THING_TYPE_WEATHER, THING_TYPE_IO);
    
    // BeagleBone Black hardware layer
    public final static Board BBB = Platform.createBoard();
    
    // Agents
    public final static OnlineMonitoringAgent ONLINE_MONITORING_AGENT = new OnlineMonitoringAgent("Online-Monitoring");
}

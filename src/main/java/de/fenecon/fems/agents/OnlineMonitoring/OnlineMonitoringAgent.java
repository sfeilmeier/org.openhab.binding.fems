package de.fenecon.fems.agents.OnlineMonitoring;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONObject;
import org.openhab.binding.fems.tools.FEMSYaler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.fems.agents.Message;
import de.fenecon.fems.agents.OnlineMonitoring.Message.DataMessage;
import de.fenecon.fems.agents.OnlineMonitoring.Message.DataMessage.ContentType;
import de.fenecon.fems.agents.OnlineMonitoring.Message.SystemMessage;

public class OnlineMonitoringAgent extends OnlineMonitoringAbstractAgent {
	private Logger logger = LoggerFactory.getLogger(OnlineMonitoringAgent.class);
	
	private final OnlineMonitoringCacheAgent cacheAgent;
	
	/**
	 * {@inheritDoc}
	 */
	public OnlineMonitoringAgent(String name, OnlineMonitoringCacheAgent cacheAgent) {
		super(name);
		this.cacheAgent = cacheAgent;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void foreverLoop() throws InterruptedException {
		Message message = messages.poll(); // wait for new message
		if(message == null) return;
		
		if(message instanceof DataMessage) {
			JSONObject json = ((DataMessage)message).convertToJson();
			json = prepareForSending(json);
			try {
				
				JSONObject retJson = sendToOnlineMonitoring(json);
				handleRetJson(retJson);
			} catch (IOException e) {
				cacheAgent.sendLater(json);
			}
		}
	}
	
	/** Send data */
	public void sendData(ContentType content, HashMap<String, org.eclipse.smarthome.core.types.State> states, 
			JSONObject data) {
		message(new DataMessage(content, states, data));
	}
	public void sendData(ContentType content, HashMap<String, org.eclipse.smarthome.core.types.State> states) {
		message(new DataMessage(content, states, null));
	}
	public void sendData(DataMessage message) {
		message(message);
	}
	
	/** Send system message */
	public void sendSystemMessage(String text) {
		message(new SystemMessage(text));
	}
	
	/** Handle return JSON */
	private void handleRetJson(JSONObject json) {
		try {
			// activate or deactivate yaler tunnel
	    	if(json != null && json.has("yaler")) {
	    		String relayDomain = json.getString("yaler");
	    		FEMSYaler.getFEMSYaler().activateTunnel(relayDomain);
	    	} else {
	    		FEMSYaler.getFEMSYaler().deactivateTunnel();
	    	}		
		} catch (Exception e) {
			logger.warn("Error handling server command: " + e.getMessage());
		}
		// TODO: Handle City-ID from Server
    	/*if(json != null && json.has("cityid")) {
    		int newCityid = json.getInt("cityid");
			if(newCityid != cityid) {
				logger.info("Set cityid to " + newCityid + " on behalf of FEMS server");
				cityid = newCityid;
				run(); // send again
			}
    	}*/
	}
	
	/** Prepare JSON for sending */
	protected JSONObject prepareForSending(JSONObject json) {
		json.put("version", OnlineMonitoringAbstractAgent.PROTOCOL_VERSION);
		json.put("apikey", apikey);
		return json;
	}
}

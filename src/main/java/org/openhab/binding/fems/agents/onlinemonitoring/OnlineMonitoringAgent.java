package org.openhab.binding.fems.agents.onlinemonitoring;

import java.io.IOException;
import java.util.Map;

import org.openhab.binding.fems.agents.Message;
import org.openhab.binding.fems.agents.onlinemonitoring.message.DataMessage;
import org.openhab.binding.fems.agents.onlinemonitoring.message.SystemMessage;
import org.openhab.binding.fems.agents.onlinemonitoring.message.DataMessage.MethodType;
import org.openhab.binding.fems.tools.FEMSYaler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

public class OnlineMonitoringAgent extends OnlineMonitoringAbstractAgent {
	private final static Logger logger = LoggerFactory.getLogger(OnlineMonitoringAgent.class);
	
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
	public void foreverLoop(Message message) throws InterruptedException {	
		if(message instanceof DataMessage) {
			JSONRPC2Request request = ((DataMessage)message).getJsonRpcRequest();
			request = prepareForSending(request);
			try {
				Map<?, ?> response = sendToOnlineMonitoring(request);
				handleResponse(response);
			} catch (IOException | JSONRPC2SessionException e) {
				cacheAgent.sendLater(request);
			}
		}
	}
	
	/** Send data, taking current time as timestamp
	 * 
	 * be aware, that the states need to be transformed to be valid for JSON/InfluxDB:
	 * Use FEMSBindingTools.convertStatesForMessage(states)
	 * @param method
	 * @param states
	 * @param params
	 */
	 
	public void sendData(MethodType method, Map<String, Object> states, 
			Map<String, Object> params) {
		handle(new DataMessage(method, states, params));
	}
	
	/** be aware, that the states need to be transformed to be valid for JSON/InfluxDB:
	 * Use FEMSBindingTools.convertStatesForMessage(states)
	 * @param method
	 * @param states
	 */
	public void sendData(MethodType method, Map<String, Object> states) {
		handle(new DataMessage(method, states, null));
	}
	public void sendData(DataMessage message) {
		handle(message);
	}
	
	/** Send system message */
	public void sendSystemMessage(String text) {
		handle(new SystemMessage(text));
	}
	
	/** Handle return JSON */
	private void handleResponse(Map<?, ?> response) {
		if(response == null) return;
		try {
			if(response.containsKey("yaler") && response.get("yaler") instanceof String) {
				String relayDomain = (String)response.get("yaler");
				if(FEMSYaler.getFEMSYaler().activateTunnel(relayDomain)) {
					this.sendSystemMessage("Yalertunnel is now activated");
				};
			} else {
				if(FEMSYaler.getFEMSYaler().deactivateTunnel()) {
					this.sendSystemMessage("Yalertunnel is now deactivated");
				};
			}
		} catch (Exception e) {
			logger.error("Unable to handle response for yaler: " + e.getMessage());
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
	
	/** Prepare JSONRPC2Request for sending */
	protected JSONRPC2Request prepareForSending(JSONRPC2Request request) {
		request.getNamedParams().put("apikey", apikey);
		return request;
	}
}

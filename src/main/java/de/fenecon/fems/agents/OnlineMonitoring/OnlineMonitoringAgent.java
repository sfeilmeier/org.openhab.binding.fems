package de.fenecon.fems.agents.OnlineMonitoring;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.openhab.binding.fems.tools.FEMSYaler;
import org.openhab.binding.fems.tools.JSONCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.fems.agents.Agent;
import de.fenecon.fems.agents.Message;
import de.fenecon.fems.agents.OnlineMonitoring.Message.ApikeyMessage;
import de.fenecon.fems.agents.OnlineMonitoring.Message.DataMessage;
import de.fenecon.fems.agents.OnlineMonitoring.Message.DataMessage.ContentType;
import de.fenecon.fems.agents.OnlineMonitoring.Message.SystemMessage;

public class OnlineMonitoringAgent extends Agent {
	private static Logger logger = LoggerFactory.getLogger(OnlineMonitoringAgent.class);
	
	protected static final String ONLINE_MONITORING_URL = "https://fenecon.de/femsmonitor";
	protected static final int PROTOCOL_VERSION = 1;
	
	private static JSONCache jsonCache = new JSONCache();
	private String apikey = null;
	
	/**
	 * {@inheritDoc}
	 */
	public OnlineMonitoringAgent(String name) {
		super(name);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void foreverLoop() throws InterruptedException {
		logger.info("New ForeverLoop");
		// wait for new message
		Message message = messages.poll();
		if(message == null) return;
		
		if(message instanceof ApikeyMessage) {
			this.apikey = ((ApikeyMessage)message).getApikey();
			logger.info("Apikey was set");
			
		} else if(message instanceof DataMessage) {
			JSONObject json = prepareDataMessage((DataMessage)message);
			JSONObject retJson = sendToOnlineMonitoring(json);
			handleRetJson(retJson);
		}
		
		// handle cached messages
		// TODO: Handle caching completely in separate CachingAgent; otherwise the following code. 
		// - might block everything for a while
		// - is only executed when a message arrived at all
		while(!jsonCache.isEmpty()) {
			JSONObject json = jsonCache.pop();
			logger.info("Trying to send cached data"
					+ (json.has("timestamp") ? " from " + new Date(json.getLong("timestamp")) : ""));
			JSONObject retJson = sendToOnlineMonitoring(json);
			handleRetJson(retJson);
		}
	}
	
	/** Set apikey */
	public void setApikey(String apikey) {
		message(new ApikeyMessage(apikey));
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
	
	/** Prepare DataMessage for sending */
	private JSONObject prepareDataMessage(DataMessage message) {
		JSONObject json = message.convertToJson();
		json.put("version", PROTOCOL_VERSION);
		json.put("apikey", apikey);
		return json;
	}
	
	/**
	 * Send message to online-monitoring 
	 */
	private JSONObject sendToOnlineMonitoring(JSONObject json) {
		// was apikey set? otherwise send to cache
		if(this.apikey == null) {
			logger.info("No apikey - caching data");
			//TODO: bug: we would cache a json without apikey... can never be sent! Solve with MapDB
			//jsonCache.push(json);
			return null;
		}
		
		// send json to server
		HttpsURLConnection con;
		URL url;
		JSONObject retJson = null;
		try {
			url = new URL(ONLINE_MONITORING_URL);
			con = (HttpsURLConnection)url.openConnection();
			
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type","application/json"); 
			con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;Windows98;DigExt)"); 
			con.setDoOutput(true); 
			con.setDoInput(true);
			
			DataOutputStream output = new DataOutputStream(con.getOutputStream());  
			try {
				output.writeBytes(json.toString());
			} finally {
				output.close();
			}

			String content = (json.has("content") ? json.getString("content") : "unknown");
			if(con.getResponseCode() == 200) {
				logger.info("Successfully sent " + content
						+ "-data; server answered: " + con.getResponseMessage());
			} else {
				throw new Exception(content + "-data; server response: " + con.getResponseCode());
			}
		} catch (Exception e) {
			logger.info("Error while sending: " + e.getMessage() + "; will try again later");
			jsonCache.push(json);
			return retJson;
		}
		
		// read reply from server
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine = in.readLine();
            if(inputLine != null) {
	        	retJson = new JSONObject(inputLine);
	        }
        } catch (IOException e) {
        	logger.info("Error while reading server reply: " + e.getMessage());
		} finally {
        	if(in != null) try { in.close(); } catch (IOException e) { }
        }
		return retJson;
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
}

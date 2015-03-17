package de.fenecon.fems.scheduler.agents.OnlineMonitoring;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.json.JSONObject;
import org.openhab.binding.fems.tools.FEMSYaler;
import org.openhab.binding.fems.tools.JSONCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.fems.scheduler.agents.OnlineMonitoring.OnlineMonitoringAgentMessage.ApikeyMessage;
import de.fenecon.fems.scheduler.agents.OnlineMonitoring.OnlineMonitoringAgentMessage.DataMessage;
import de.fenecon.fems.scheduler.tools.Agent;
import de.fenecon.fems.scheduler.tools.Message;

public class OnlineMonitoringAgent extends Agent {
	private static Logger logger = LoggerFactory.getLogger(OnlineMonitoringAgent.class);
	private static final String femsmonitorUrl = "https://fenecon.de/femsmonitor";
	public static final int protocolVersion = 1;
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
			this.apikey = ((ApikeyMessage)message).apikey;
			logger.info("Apikey was set");
			
		} else if(message instanceof DataMessage) {
			JSONObject json = convertToJson((DataMessage)message);
			JSONObject retJson = sendJson(json);
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
			JSONObject retJson = sendJson(json);
			handleRetJson(retJson);
		}
	}
	
	/** Convert state data from HashMap to JSON */
	private JSONObject convertToJson(DataMessage message) {
		JSONObject json = new JSONObject();
		json.put("version", protocolVersion);
		json.put("apikey", apikey);
		json.put("timestamp", message.timestamp.getTime());
		json.put("content", message.content.toString());
		for(Object keyObj : message.data.keySet()) {
			if(keyObj instanceof String) {
				String key = (String)keyObj;
				json.put(key, message.data.get(key));
			}
		}
		// convert eclipse smarthome states to json types
		JSONObject statesJson = new JSONObject(); 
		for (String key : message.states.keySet()) {
			org.eclipse.smarthome.core.types.State state = message.states.get(key);
			if(state instanceof OnOffType) {
				if((OnOffType)state == OnOffType.ON) {
					statesJson.put(key, 1);
				} else {
					statesJson.put(key, 0);
				}
			} else if (state instanceof UnDefType) {
				statesJson.put(key, JSONObject.NULL);
			} else if (state instanceof StringType) {
				statesJson.put(key, state.toString());
			} else if (state instanceof DecimalType) {
				DecimalType stateDecimal = (DecimalType)state;
				statesJson.put(key, stateDecimal.toBigDecimal());
			} else {
				statesJson.put(key, state.toString());
			}
		}
		json.put("states", statesJson);		
		return json;
	}
	
	/** Send message to online-monitoring */
	private JSONObject sendJson(JSONObject json) {
		JSONObject retJson = null;
		// was apikey set? otherwise send to cache
		if(this.apikey == null) {
			logger.info("No apikey - caching data");
			jsonCache.push(json);
			return retJson;
		}
		// send json to server
		HttpsURLConnection con;
		URL url;
		try {
			url = new URL(femsmonitorUrl);
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
		// read from server
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

package de.fenecon.fems.agents.OnlineMonitoring;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.fems.agents.Agent;

public abstract class OnlineMonitoringAbstractAgent extends Agent {
	private Logger logger = LoggerFactory.getLogger(OnlineMonitoringAbstractAgent.class);
	
	protected static final URL ONLINE_MONITORING_URL = makeUrl("https://fenecon.de/femsmonitor");
	private static URL makeUrl(String urlString) {
	    try {
	        return new java.net.URL(urlString);
	    } catch (java.net.MalformedURLException e) {
	        return null;
	    }
	}
	
	protected static final int PROTOCOL_VERSION = 1;
	
	protected volatile String apikey = null;
	
	/**
	 * {@inheritDoc}
	 */
	public OnlineMonitoringAbstractAgent(String name) {
		super(name);
	}
	
	/** Set apikey */
	public void setApikey(String apikey) {
		logger.info("Set Apikey");
		this.apikey = apikey;
	}
	
	/**
	 * Send message to online-monitoring 
	 */
	protected JSONObject sendToOnlineMonitoring(JSONObject json) throws IOException {
		// was apikey set? otherwise send to cache
		if(this.apikey == null) {
			logger.info("No apikey - caching data");
			//TODO: bug: we would cache a json without apikey... can never be sent! Solve with MapDB
			//jsonCache.push(json);
			throw new IOException("No apikey");
		}
		
		// send json to server
		HttpsURLConnection con;
		JSONObject retJson = null;
		con = (HttpsURLConnection)ONLINE_MONITORING_URL.openConnection();
			
		try { con.setRequestMethod("POST");	} catch (ProtocolException e1) { }
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
			throw new IOException(content + "-data; server response: " + con.getResponseCode());
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
}

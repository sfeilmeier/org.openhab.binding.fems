package de.fenecon.fems.agents.OnlineMonitoring;

import java.io.IOException;
import java.util.Date;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.fems.tools.JSONCache;

public class OnlineMonitoringCacheAgent extends OnlineMonitoringAbstractAgent {
	private Logger logger = LoggerFactory.getLogger(OnlineMonitoringCacheAgent.class);
	
	private JSONCache jsonCache = null;
	
	/**
	 * {@inheritDoc}
	 */
	public OnlineMonitoringCacheAgent(String name) {
		super(name);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		if(!getJsonCache().isEmpty()) { // initialize JSON Cache
			lock.release(); // immediately start sending if we have cached messages
		}
		super.run();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void foreverLoop() throws InterruptedException {
		Thread.sleep(1000); // wait for 1 second
		if(!getJsonCache().isEmpty()) {
			JSONObject json = jsonCache.pop();
			logger.info("Trying to send cached data"
					+ (json.has("timestamp") ? " from " + new Date(json.getLong("timestamp")) : ""));
			try {
				sendToOnlineMonitoring(json); // ignoring return message for cached messages
			} catch (IOException e) {
				sendLater(json);
			}
			if(!getJsonCache().isEmpty()) {
				lock.release();
			}
		}
	}
	
	/**
	 * Add a message to the Cache Agent
	 * @param message
	 */
	public void sendLater(JSONObject json) {
		getJsonCache().push(json);
		lock.release();
	}
	
	/** make sure we have a valid JSON Cache object */
	private JSONCache getJsonCache() {
		if(jsonCache == null) {
			jsonCache = new JSONCache();
		}
		return jsonCache;
	}
}

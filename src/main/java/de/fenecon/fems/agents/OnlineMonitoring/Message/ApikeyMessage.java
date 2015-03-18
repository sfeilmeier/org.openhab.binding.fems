package de.fenecon.fems.agents.OnlineMonitoring.Message;

/**
 *  Set the apikey for FENECON Online-Monitoring 
 */
public class ApikeyMessage extends Message {
	private final String apikey;
	
	public ApikeyMessage(String apikey) {
		this.apikey = apikey;
	}
	
	public String getApikey() {
		return apikey;
	}
}

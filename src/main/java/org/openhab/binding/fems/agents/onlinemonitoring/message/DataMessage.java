package org.openhab.binding.fems.agents.onlinemonitoring.message;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.fems.agents.Message;
import org.openhab.binding.fems.tools.FEMSYaler;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;

/**
 * Message with data to be sent to Online-Monitoring
 */
public class DataMessage extends Message {
	/* General types of DataMessages */
	public static enum MethodType {
	    COMMERCIAL("commercial"),
	    PRO("pro"),
	    IO("io"),
	    SYSTEM("system"),
	    WEATHER("weather");

	    private final String text;
	    private MethodType(final String text) {
	        this.text = text;
	    }
	    @Override
	    public String toString() {
	        return text;
	    }
	}
	
	protected final JSONRPC2Request request;
	protected final static int JSON_RPC_ID = 0;
	
	public DataMessage(MethodType method, Map<String, Object> states, Map<String, Object> params) {
		this(new Date(), method, states, params);
	}
	
	public DataMessage(Date timestamp, MethodType method, 
			Map<String, Object> states, Map<String, Object> params) {
		HashMap<String, Object> newParams = new HashMap<String, Object>();
		newParams.put("timestamp", timestamp.getTime()/1000);
		if(params != null) {
			newParams.putAll(params);
		}
		if(states != null) {
			newParams.put("states", states);
		}
		newParams.put("yaler", FEMSYaler.getFEMSYaler().isActive());
		request = new JSONRPC2Request(method.toString(), newParams, JSON_RPC_ID);
	}
	
	public JSONRPC2Request getJsonRpcRequest() {
		return request;
	}
}

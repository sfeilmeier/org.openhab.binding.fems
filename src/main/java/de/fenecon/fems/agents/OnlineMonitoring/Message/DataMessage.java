package de.fenecon.fems.agents.OnlineMonitoring.Message;

import java.util.Date;
import java.util.HashMap;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.json.JSONObject;
import org.openhab.binding.fems.tools.FEMSYaler;

/**
 * Message with data to be sent to Online-Monitoring
 */
public class DataMessage extends Message {
	/* General types of DataMessages */
	public static enum ContentType {
	    CESS("cess"),
	    DESS("dess"),
	    IO("io"),
	    SYSTEM("system"),
	    WEATHER("weather");

	    private final String text;
	    private ContentType(final String text) {
	        this.text = text;
	    }
	    @Override
	    public String toString() {
	        return text;
	    }
	}
	
	protected final Date timestamp;
	protected final ContentType content;
	protected final HashMap<String, State> states;
	protected final JSONObject data;
	
	public DataMessage(ContentType content, HashMap<String, State> states, JSONObject data) {
		this(new Date(), content, states, data);
	}
	
	public DataMessage(final Date timestamp, final ContentType content, 
			final HashMap<String, State> states, final JSONObject data) {
		this.timestamp = timestamp;
		this.content = content;
		this.states = states;
		this.data = data;
		//TODO: more elegant solution for yaler
		data.put("yaler", FEMSYaler.getFEMSYaler().isActive());
	}
	
	/** Convert state data from HashMap to JSON */
	public JSONObject convertToJson() {
		JSONObject json = new JSONObject();
		json.put("timestamp", timestamp.getTime());
		json.put("content", content.toString());
		for(Object keyObj : data.keySet()) {
			if(keyObj instanceof String) {
				String key = (String)keyObj;
				json.put(key, data.get(key));
			}
		}
		// convert eclipse smarthome states to json types
		JSONObject statesJson = new JSONObject(); 
		for (String key : states.keySet()) {
			org.eclipse.smarthome.core.types.State state = states.get(key);
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
}

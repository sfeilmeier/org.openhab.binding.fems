package de.fenecon.fems.scheduler.agents.OnlineMonitoring;

import java.util.Date;
import java.util.HashMap;

import org.json.JSONObject;
import org.openhab.binding.fems.tools.FEMSYaler;

import de.fenecon.fems.scheduler.tools.Message;

public abstract class OnlineMonitoringAgentMessage {
	/* Set the apikey for FENECON Online-Monitoring */
	public static class ApikeyMessage extends Message {
		public final String apikey;
		public ApikeyMessage(String apikey) {
			this.apikey = apikey;
		}
	}
	
	/* General types of DataMessages */
	public static enum DataMessageContentType {
	    CESS("cess"),
	    DESS("dess"),
	    IO("io"),
	    SYSTEM("system"),
	    WEATHER("weather");

	    private final String text;
	    private DataMessageContentType(final String text) {
	        this.text = text;
	    }
	    @Override
	    public String toString() {
	        return text;
	    }
	}
	
	/* Message with data to be sent to Online-Monitoring */
	public static class DataMessage extends Message {
		public final Date timestamp;
		public final DataMessageContentType content;
		public final HashMap<String, org.eclipse.smarthome.core.types.State> states = new HashMap<String, org.eclipse.smarthome.core.types.State>();
		public final JSONObject data = new JSONObject();
		public DataMessage(DataMessageContentType content) {
			this(new Date(), content);
		}
		public DataMessage(Date timestamp, DataMessageContentType content) {
			this.timestamp = timestamp;
			this.content = content;
			//TODO: more elegant solution for yaler
			data.put("yaler", FEMSYaler.getFEMSYaler().isActive());
		}
	}
	
	/* Simple Systemmessage */
	public static class SystemMessage extends DataMessage {
		public SystemMessage(String text) {
			super(DataMessageContentType.SYSTEM);
			this.data.put("system", text);
		}
	}
}

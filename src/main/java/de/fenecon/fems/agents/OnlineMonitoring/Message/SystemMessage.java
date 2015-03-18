package de.fenecon.fems.agents.OnlineMonitoring.Message;

import org.json.JSONObject;

import de.fenecon.fems.agents.OnlineMonitoring.Message.DataMessage;

/*
 * Simple Systemmessage-Helper
 */
public class SystemMessage extends DataMessage {
	public SystemMessage(String text) {
		super(ContentType.SYSTEM,
				null,
				new JSONObject().put("system", text));
	}
}

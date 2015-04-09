package org.openhab.binding.fems.agents.onlinemonitoring.message;

import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.fems.agents.onlinemonitoring.message.DataMessage;

/*
 * Simple Systemmessage-Helper
 */
public class SystemMessage extends DataMessage {
	public SystemMessage(String text) {
		super(MethodType.SYSTEM,
				null,
				generateParamsMap(text));
	}
	
	private static Map<String, Object> generateParamsMap(String text) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("text", text);
		return params;
	}
}

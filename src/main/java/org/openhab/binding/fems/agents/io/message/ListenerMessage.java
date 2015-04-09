package org.openhab.binding.fems.agents.io.message;

import org.openhab.binding.fems.agents.Message;

/**
 * Message with new IOAgentListener
 */
public class ListenerMessage extends Message {
	private final IOAgentListener listener;
	
	public ListenerMessage(IOAgentListener listener) {
		this.listener = listener;
	}
	
	public IOAgentListener getListener() {
		return listener;
	}
}

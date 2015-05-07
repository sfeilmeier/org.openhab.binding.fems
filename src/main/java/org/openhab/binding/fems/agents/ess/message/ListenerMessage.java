package org.openhab.binding.fems.agents.ess.message;

import org.openhab.binding.fems.agents.Message;

/**
 * Message with new ESSAgentListener
 */
public class ListenerMessage extends Message {
	private final ESSAgentListener listener;
	
	public ListenerMessage(ESSAgentListener listener) {
		this.listener = listener;
	}
	
	public ESSAgentListener getListener() {
		return listener;
	}
}

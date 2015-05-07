package org.openhab.binding.fems.agents.ess.message;

import org.openhab.binding.fems.agents.Message;
import org.openhab.binding.fems.internal.essprotocol.ESSProtocol;

public class ProtocolMessage extends Message {
	private final ESSProtocol protocol;
	
	public ProtocolMessage(ESSProtocol protocol) {
		this.protocol = protocol;
	}
	
	public ESSProtocol getProtocol() {
		return protocol;
	}
}

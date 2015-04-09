package org.openhab.binding.fems.agents.io.message;

import org.eclipse.smarthome.core.types.State;

public interface IOAgentListener {

	void ioUpdate(String id, State state);

}

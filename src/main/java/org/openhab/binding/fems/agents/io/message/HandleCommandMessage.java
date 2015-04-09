package org.openhab.binding.fems.agents.io.message;

import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.fems.agents.Message;

public class HandleCommandMessage extends Message {
	private final String id;
	private final Command command;
	
	public HandleCommandMessage(String id, Command command) {
		this.id = id;
		this.command = command;
	}

	public String getId() {
		return id;
	}

	public Command getCommand() {
		return command;
	}
	
	@Override
	public String toString() {
		return "HandleCommandMessage(" + id + "," + command + ")";
	}
}

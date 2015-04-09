package org.openhab.binding.fems.agents;

public abstract class Message {
	protected final Agent sender;
	
	public Message() {
		if(Thread.currentThread() instanceof Agent) {
			sender = (Agent)(Thread.currentThread());
		} else {
			sender = null;
		}
	}
	
	/** Get the sender agent of the message; null if it was not an agent
	 * 
	 * @return sender agent or null
	 */
	public Agent getSender() {
		return sender;
	}
	
	public String getSenderName() {
		if(sender != null) {
			return sender.getName();
		} else {
			return "(unknown)";
		}
	}
}

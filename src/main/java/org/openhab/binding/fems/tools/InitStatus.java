package org.openhab.binding.fems.tools;

/**
 * Creates a string in the form "---" up to "XXX"
 * 1st char: IP
 * 2nd char: Internet
 * 3rd char: Modbus
 * 
 * @author Stefan Feilmeier
 */
public class InitStatus {
	private boolean changed = false;
	private boolean ip = false;
	private boolean internet = false;
	private boolean modbus = false;
	
	public boolean getChanged() {
		return changed;
	}
	public void setIp(boolean ip) {
		this.ip = ip;
		changed = true;
	}
	public boolean getInternet() {
		return internet;
	}
	public void setInternet(boolean internet) {
		this.internet = internet;
		changed = true;
	}
	public void setModbus(boolean modbus) {
		this.modbus = modbus;
		changed = true;
	}
	public String toString() {
		changed = false;
		return (ip ? "X" : "-") + (internet ? "X" : "-") + (modbus ? "X" : "-");
	}
}
package org.openhab.binding.fems.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log {
	private String logText = null;
	private final Logger logger; 
	
	public Log(@SuppressWarnings("rawtypes") Class clazz) {
		logger = LoggerFactory.getLogger(clazz);
	}
	
	private void addLog(String text) {
		if(logText == null) {
			logText = text;
		} else {
			logText += "\n" + text;
		}		
	}
	
	public void info(String text) {
		logger.info(text);
		addLog(text);
	}
	
	public void error(String text) {
		logger.error(text);
		addLog("ERROR: " + text);
	}
	
	public String getLog() {
		return logText;
	}
}

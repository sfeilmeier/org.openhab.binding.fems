package org.openhab.binding.fems.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.fems.Constants;
import org.openhab.binding.fems.agents.ess.message.ESSAgentListener;
import org.openhab.binding.fems.exceptions.FEMSException;
import org.openhab.binding.fems.exceptions.IPException;
import org.openhab.binding.fems.exceptions.InternetException;
import org.openhab.binding.fems.exceptions.RS485Exception;
import org.openhab.binding.fems.internal.FEMSHandlerFactory;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElement;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElementRange;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusItem;

public class FEMSInit implements ESSAgentListener {
	private volatile List<ModbusElementRange> wordRanges = null;
	
	public void init(String ess, boolean debug) {
		int returnCode = 0;
		Log log = new Log(FEMSHandlerFactory.class);
		try {
			log.info("FEMS Initialization");
		
			// init LCD display
			Constants.IO_AGENT.setLcdText(true, "FEMS Selbsttest");
			
			// Init runtime variables
			Runtime rt = Runtime.getRuntime();
			Process proc;
			InitStatus initStatus = new InitStatus();
			
			try {			
				// check for valid ip address
				InetAddress ip = Tools.getIPaddress();
				if(ip == null) {
			        try {
						proc = rt.exec("/sbin/dhclient eth0");
						proc.waitFor();
						ip = Tools.getIPaddress(); /* try again */
						if(ip == null) { /* still no IP */
							throw new IPException();
						}
					} catch (IOException | InterruptedException e) {
						throw new IPException(e.getMessage());
					}
				}
				log.info("IP: " + ip.getHostAddress());
				initStatus.setIp(true);
				Constants.IO_AGENT.setLcdText(initStatus + " IP ok      ");
				Constants.IO_AGENT.handleCommand(Constants.UserLED_1, OnOffType.ON);
		
				// check time
				if(Tools.isDateValid()) { /* date is valid, so we check internet access only */
					log.info("Date ok: " + Constants.LONG_DATE_FORMAT.format(new Date()));
					try {
						URL url = new URL("https://fenecon.de");
						URLConnection con = url.openConnection();
						con.setConnectTimeout(5000);
						con.getContent();
					} catch (IOException e) {
						throw new InternetException(e.getMessage());
					}	
				} else {
					log.info("Date not ok: " + Constants.LONG_DATE_FORMAT.format(new Date()));
					try {
						proc = rt.exec("/usr/sbin/ntpdate -b -u fenecon.de 0.pool.ntp.org 1.pool.ntp.org 2.pool.ntp.org 3.pool.ntp.org");
						proc.waitFor();
						if(!Tools.isDateValid()) {
							// try one more time
							proc = rt.exec("/usr/sbin/ntpdate -b -u fenecon.de 0.pool.ntp.org 1.pool.ntp.org 2.pool.ntp.org 3.pool.ntp.org");
							proc.waitFor();						
							if(!Tools.isDateValid()) {
								throw new InternetException("Wrong Date: " + Constants.LONG_DATE_FORMAT.format(new Date()));
							}
						}
						log.info("Date now ok: " + Constants.LONG_DATE_FORMAT.format(new Date()));
					} catch (IOException | InterruptedException e) {
						throw new InternetException(e.getMessage());
					}
				}
				log.info("Internet access is available");
				initStatus.setInternet(true);
				Constants.IO_AGENT.setLcdText(initStatus + " Internet ok");
				Constants.IO_AGENT.handleCommand(Constants.UserLED_2, OnOffType.ON);
						
				// start system update
				log.info("Start system update");
				try {
					proc = rt.exec("/usr/bin/fems-autoupdate");
				} catch (IOException e) {
					log.error(e.getMessage());
				}
				
				// test modbus
				String searchString = "BSMU_Battery_Stack_Overall_SOC";
				if(ess.equals("cess")) {
					searchString = "Battery_string_1_SOC";
				}
				if(!debug) { 
					State soc = null;
					for(int i=0; i<20; i++) {
						if(wordRanges != null) {
							outerloop: for (ModbusElementRange wordRange : wordRanges) {
								for (ModbusElement word : wordRange.getWords()) {
									if(word instanceof ModbusItem) {
										ModbusItem item = (ModbusItem)word;
										if(item.getName().equals(searchString)) {
											soc = item.getState();
											break outerloop;
										}
									}
								}
							}
						} else {
							log.info("No modbus data received yet - waiting");
							Thread.sleep(5000);
						}
					}
					if(soc != null) {
						log.info("Modbus OK - SOC: " + soc.toString());
						initStatus.setModbus(true);
						Constants.IO_AGENT.setLcdText(initStatus + " RS485 ok   ");
						Constants.IO_AGENT.handleCommand(Constants.UserLED_3, OnOffType.ON);
					} else {
						throw new RS485Exception();
					}
				} else { // if we are in debug mode: ignore RS485-errors
					log.info("Debug mode: ignore RS485-Error");
				}
				
				// Exit message
				log.info("Finished without error");
				Constants.IO_AGENT.setLcdText(initStatus + "  erfolgreich");
				
				//TODO: remove; not necessary anymore; announce systemd finished
				/*log.info("Announce systemd: ready");
				try {
					proc = rt.exec("/bin/systemd-notify --ready");
					proc.waitFor();
				} catch (IOException | InterruptedException e) {
					log.error(e.getMessage());
				}*/
			} catch (FEMSException e) {
				log.error(e.getMessage());
				log.error("Finished with error");
				Constants.IO_AGENT.setLcdText(initStatus + " " + e.getMessage());
				returnCode = 1;
			}
			
			// Check if Yaler is active
			if(FEMSYaler.getFEMSYaler().isActive()) {
				log.info("Yaler is activated");
			} else {
				log.info("Yaler is deactivated");
			}
			
			log.info("openHAB started");
			
			// Send message
			Constants.ONLINE_MONITORING_AGENT.sendSystemMessage(log.getLog());
			
			Constants.IO_AGENT.handleCommand(Constants.UserLED_4, OnOffType.ON);
			
		} catch (Throwable e) { // Catch everything else
			returnCode = 2;
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			log.error("Critical error: " + sw.toString());
			e.printStackTrace();
			Constants.ONLINE_MONITORING_AGENT.sendSystemMessage(log.getLog()); // try to send log
		}
		
		if(returnCode != 0) {
			log.error("Waiting...");
			try { Thread.sleep(5000); } catch (InterruptedException e) { }
			System.exit(returnCode);
		}
	}

	@Override
	public void essUpdate(List<ModbusElementRange> wordRanges) {
		this.wordRanges = wordRanges;
	}
}

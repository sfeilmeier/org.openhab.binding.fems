/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.msg.ExceptionResponse;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openhab.binding.fems.exceptions.FEMSException;
import org.openhab.binding.fems.exceptions.IPException;
import org.openhab.binding.fems.exceptions.InternetException;
import org.openhab.binding.fems.exceptions.RS485Exception;
import org.openhab.binding.fems.tools.FEMSYaler;
import org.openhab.binding.fems.tools.InitStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FEMSCore {
	private final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	private final static Logger logger = LoggerFactory.getLogger(FEMSCore.class); 
	
	private static String apikey;
	private static String ess;
	private static boolean debug;
	
	public static void main(String[] args) {
		// read FEMS properties from /etc/fems
		Properties properties = new Properties();
		BufferedInputStream stream = null;
		try {
			stream = new BufferedInputStream(new FileInputStream("/etc/fems"));
			properties.load(stream);
			apikey = properties.getProperty("apikey");
			ess = properties.getProperty("ess", "dess");
			debug = Boolean.parseBoolean(properties.getProperty("debug", "false"));
			if(stream != null) stream.close();
		} catch (IOException e) {
			logError(e.getMessage());
		}

		// handle commandline parameters		
		Options options = new Options();
		options.addOption("h", "help", false, "");
		options.addOption(null, "init", false, "Initialize system");
		options.addOption(null, "aout", true, "Set Analog Output: ID,%");
		options.addOption(null, "lcd-text", true, "Set LCD-Text");
		options.addOption(null, "lcd-backlight", true, "Set LCD-Backlight in %");
		options.addOption(null, "rs485", false, "Test RS485 connection");
		options.addOption("d", "debug", false, "Enable debug logging");
		
		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
			if(cmd.hasOption("debug")) {
				// TODO: Enable debug logging
				//ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
			    //root.setLevel(ch.qos.logback.classic.Level.DEBUG);			
			}
			if(cmd.hasOption("init")) {
				init();
			} else if (args.length == 0) {
				help(options);
			} else if (cmd.hasOption("rs485")) {
				testRs485();
			} else {
				// start IO Agent
				Constants.IO_AGENT.start();
				
				if(cmd.hasOption("aout")) {
					setAnalogOutput(cmd.getOptionValue("aout"));
				} else if(cmd.hasOption("lcd-text")) {
					logger.info("LCD-Text");
					setLcdText(cmd.getOptionValue("lcd-text"));
				} else if(cmd.hasOption("lcd-backlight")) {
					setLcdBrightness(Integer.parseInt(cmd.getOptionValue("lcd-backlight")));
			    } else {
			    	help(options);
				}
				try { Thread.sleep(10000); } catch (InterruptedException e) { e.printStackTrace(); }
				//logger.debug("Interrupt IO Agent");
				//Constants.IO_AGENT.interrupt();
			}
		} catch (ParseException e) {
			help(options);
		}
		
		System.exit(0);
	}

	private static String logText = null;
	private static void logInfo(String text) {
		System.out.println(text);
		if(logText == null) {
			FEMSCore.logText = text;
		} else {
			FEMSCore.logText += "\n" + text;
		}
	}
	private static void logError(String text) {
		System.out.println("ERROR: " + text);
		if(logText == null) {
			FEMSCore.logText = "ERROR: " + text;
		} else {
			FEMSCore.logText += "\nERROR: " + text;
		}
	}

	/**
	 * Show all commandline options
	 * @param options
	 */
	private static void help(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "FemsTester", options );		
	}
	
	/**
	 * Checks if the current system date is valid
	 * @return
	 */
	private static boolean isDateValid() {
		Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR);  
		if(year < 2014 || year > 2025) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Checks if modbus connection to storage system is working
	 * @param ess "dess" or "cess"
	 * @return
	 */
	private static boolean isModbusWorking(String ess) {
		// remove old lock file
		try {
			if(Files.deleteIfExists(Paths.get("/var/lock/LCK..ttyUSB0"))) {
				logInfo("Deleted old lock file");
			}
		} catch (IOException e) {
			logError("Error deleting old lock file: " + e.getMessage());
			e.printStackTrace();
		}
		
		// find first matching device
		String modbusDevice = "ttyUSB*";
		String portName = "/dev/ttyUSB0"; // if no file found: use default
		try (DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get("/dev"), modbusDevice)) {
		    for(Path file : files) {
		    	portName = file.toAbsolutePath().toString();
		    	logInfo("Set modbus portname: " + portName);
		    }
		} catch(Exception e) {
			logInfo("Error trying to find " + modbusDevice + ": " + e.getMessage());
			e.printStackTrace();
		}

		// default: DESS 
		int baudRate = 9600;
		int socAddress = 10143;
		int unit = 4;
		if(ess.compareTo("cess")==0) {
			baudRate = 19200;
			socAddress = 0x1402;
			unit = 100;
		}
		SerialParameters params = new SerialParameters();
		params.setPortName(portName);
		params.setBaudRate(baudRate);
		params.setDatabits(8);
		params.setParity("None");
		params.setStopbits(1);
		params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
		params.setEcho(false);
		params.setReceiveTimeout(Constants.MODBUS_TIMEOUT);
		logInfo("Timeout: " + params.getReceiveTimeout());
		SerialConnection serialConnection = new SerialConnection(params);
		try {
			serialConnection.open();
		} catch (Exception e) {
			logError("Modbus connection error: " + e.getMessage());
			serialConnection.close();
			return false;
		}
		ModbusSerialTransaction modbusSerialTransaction = null;
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(socAddress, 1);
		req.setUnitID(unit);
		req.setHeadless();	
		modbusSerialTransaction = new ModbusSerialTransaction(serialConnection);
		modbusSerialTransaction.setRequest(req);
		modbusSerialTransaction.setRetries(1);
		try {
			modbusSerialTransaction.execute();
		} catch (ModbusException e) {
			logError("Modbus execution error: " + e.getMessage());
			serialConnection.close();
			return false;
		}
		ModbusResponse res = modbusSerialTransaction.getResponse();
		serialConnection.close();
		
		if (res instanceof ReadMultipleRegistersResponse) {
			return true;
    	} else if (res instanceof ExceptionResponse) {
    		logError("Modbus read error: " + ((ExceptionResponse)res).getExceptionCode());
    	} else {
    		logError("Modbus read undefined response");
    	}
		return false;
	}
	
	/**
	 * Gets an IPv4 network address
	 * @return
	 */
	private static InetAddress getIPaddress() {
    	try {
			NetworkInterface n = NetworkInterface.getByName("eth0");
			Enumeration<InetAddress> ee = n.getInetAddresses();
			while (ee.hasMoreElements()) {
				InetAddress i = (InetAddress) ee.nextElement();
				if(i instanceof Inet4Address) {
					return i;
		        }
		    }
    	} catch (SocketException e) { /* no IP-Address */ }
    	return null; 
	}
	
	/**
	 * Initialize FEMS/FEMSmonitor system
	 */
	private static void init() {
		int returnCode = 0; 
		try {
			logInfo("FEMS Initialization");
			
			// start IO Agent
			Constants.IO_AGENT.start();
			
			// init LCD display
			Constants.IO_AGENT.setLcdText(true, "FEMS Selbsttest");
			
			// Init runtime variables
			Runtime rt = Runtime.getRuntime();
			Process proc;
			InitStatus initStatus = new InitStatus();
			
			try {
				// check for valid ip address
				InetAddress ip = getIPaddress();
				if(ip == null) {
			        try {
						proc = rt.exec("/sbin/dhclient eth0");
						proc.waitFor();
						ip = getIPaddress(); /* try again */
						if(ip == null) { /* still no IP */
							throw new IPException();
						}
					} catch (IOException | InterruptedException e) {
						throw new IPException(e.getMessage());
					}
				}
				logInfo("IP: " + ip.getHostAddress());
				initStatus.setIp(true);
				Constants.IO_AGENT.setLcdText(initStatus + " IP ok      ");
				Constants.IO_AGENT.handleCommand(Constants.UserLED_1, OnOffType.ON);
		
				// check time
				if(isDateValid()) { /* date is valid, so we check internet access only */
					logInfo("Date ok: " + dateFormat.format(new Date()));
					try {
						URL url = new URL("https://fenecon.de");
						URLConnection con = url.openConnection();
						con.setConnectTimeout(5000);
						con.getContent();
					} catch (IOException e) {
						throw new InternetException(e.getMessage());
					}	
				} else {
					logInfo("Date not ok: " + dateFormat.format(new Date()));
					try {
						proc = rt.exec("/usr/sbin/ntpdate -b -u fenecon.de 0.pool.ntp.org 1.pool.ntp.org 2.pool.ntp.org 3.pool.ntp.org");
						proc.waitFor();
						if(!isDateValid()) {
							// try one more time
							proc = rt.exec("/usr/sbin/ntpdate -b -u fenecon.de 0.pool.ntp.org 1.pool.ntp.org 2.pool.ntp.org 3.pool.ntp.org");
							proc.waitFor();						
							if(!isDateValid()) {
								throw new InternetException("Wrong Date: " + dateFormat.format(new Date()));
							}
						}
						logInfo("Date now ok: " + dateFormat.format(new Date()));
					} catch (IOException | InterruptedException e) {
						throw new InternetException(e.getMessage());
					}
				}
				logInfo("Internet access is available");
				initStatus.setInternet(true);
				Constants.IO_AGENT.setLcdText(initStatus + " Internet ok");
				Constants.IO_AGENT.handleCommand(Constants.UserLED_2, OnOffType.ON);
						
				// test modbus
				if(isModbusWorking(ess)) {
					logInfo("Modbus is ok");
					initStatus.setModbus(true);
					Constants.IO_AGENT.setLcdText(initStatus + " RS485 ok   ");
					Constants.IO_AGENT.handleCommand(Constants.UserLED_3, OnOffType.ON);
				} else {	
					if(debug) { // if we are in debug mode: ignore RS485-errors
						logInfo("Ignore RS485-Error");
					} else {
						throw new RS485Exception();
					}
				}
				
				// Exit message
				logInfo("Finished without error");
				Constants.IO_AGENT.setLcdText(initStatus + "  erfolgreich");
				
				// announce systemd finished
				logInfo("Announce systemd: ready");
				try {
					proc = rt.exec("/bin/systemd-notify --ready");
					proc.waitFor();
				} catch (IOException | InterruptedException e) {
					logError(e.getMessage());
				}
			} catch (FEMSException e) {
				logError(e.getMessage());
				logError("Finished with error");
				Constants.IO_AGENT.setLcdText(initStatus + " " + e.getMessage());
				returnCode = 1;
			}
			
			// Check if Yaler is active
			if(FEMSYaler.getFEMSYaler().isActive()) {
				logInfo("Yaler is activated");
			} else {
				logInfo("Yaler is deactivated");
			}
			
			// Send message
			if(apikey == null) {
				logError("Apikey is not available");
			} else {
				// start Agents
				Constants.ONLINE_MONITORING_AGENT.setApikey(apikey);
				Constants.ONLINE_MONITORING_AGENT.start();
				Constants.ONLINE_MONITORING_CACHE_AGENT.setApikey(apikey);
				Constants.ONLINE_MONITORING_CACHE_AGENT.start();
				
				Constants.ONLINE_MONITORING_AGENT.sendSystemMessage(logText);
			}
			
			// start system update
			logInfo("Start system update");
			try {
				proc = rt.exec("/usr/bin/fems-autoupdate");
				proc.waitFor();
			} catch (IOException | InterruptedException e) {
				logError(e.getMessage());
			}
			
			Constants.IO_AGENT.handleCommand(Constants.UserLED_4, OnOffType.ON);
			
		} catch (Throwable e) { // Catch everything else
			returnCode = 2;
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			logError("Critical error: " + sw.toString());
			e.printStackTrace();
			Constants.ONLINE_MONITORING_AGENT.sendSystemMessage(logText); // try to send log
		}

		try {
			Thread.sleep(2000);  // give the agents some time to try sending
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Stop agents
		Constants.ONLINE_MONITORING_AGENT.interrupt();
		try { Constants.ONLINE_MONITORING_AGENT.join(); } catch (InterruptedException e) { e.printStackTrace();	}
		Constants.ONLINE_MONITORING_CACHE_AGENT.interrupt();
		try { Constants.ONLINE_MONITORING_CACHE_AGENT.join();  } catch (InterruptedException e) { e.printStackTrace();	}
		Constants.IO_AGENT.interrupt();
		try { Constants.IO_AGENT.join();  } catch (InterruptedException e) { e.printStackTrace();	}
		
		// Exit
		System.exit(returnCode);
	}
	
	/** Set Analog Output
	 */
	private static void setAnalogOutput(String cmd) {
		logInfo("Analog Output: " + cmd);
		String[] cmds = cmd.split(",");
		
		try {
			if(cmds.length < 2)	throw new Exception("Missing parameters");
			// parse ID of analog output
			int id = Integer.parseInt(cmds[0]);
			logInfo("No: " + id);
			String aout;
			switch(id) {
			case 1:
				aout = "AnalogOutput_1";
				break;
			case 2:
				aout = "AnalogOutput_2";
				break;
			case 3:
				aout = "AnalogOutput_3";
				break;
			case 4:
				aout = "AnalogOutput_4";
				break;
			default:
				throw new Exception("ID must be between 1 and 4");
			}
			// parse percent/duty
			logInfo("Duty: " + cmds[1]);
			// set analog output
			Constants.IO_AGENT.handleCommand(aout, new PercentType(cmds[1]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** Set LCD-Text
	 */
	private static void setLcdText(String text) {
		logInfo("LCD-Text: " + text);
		Constants.IO_AGENT.setLcdText(true, text.substring(0, text.length() > 16 ? 16 : text.length()));
		if(text.length() > 15) {
			Constants.IO_AGENT.setLcdText(false, text.substring(16, text.length() > 32 ? 32 : text.length()));
		}
	}
	
	/** Set LCD-Text
	 */
	private static void setLcdBrightness(int percent) {
		logInfo("LCD-Brightness: " + percent + " %");
		Constants.IO_AGENT.handleCommand("LCD_Backlight", new PercentType(percent));
	}	
	
	/** Test RS485 connection to storage system
	 */
	private static void testRs485() {
		if(isModbusWorking(ess)) {
			logInfo("RS485-Modbus connection to " + ess + "-system is working");
		} else {
			logInfo("RS485-Modbus connection to " + ess + "-system is NOT working");
		}
	}
}

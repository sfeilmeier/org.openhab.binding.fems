package org.openhab.binding.fems.agents.ess;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.wimpi.modbus.ModbusException;

import org.openhab.binding.fems.Constants;
import org.openhab.binding.fems.agents.Agent;
import org.openhab.binding.fems.agents.Message;
import org.openhab.binding.fems.agents.ess.message.ESSAgentListener;
import org.openhab.binding.fems.agents.ess.message.ListenerMessage;
import org.openhab.binding.fems.agents.ess.message.ProtocolMessage;
import org.openhab.binding.fems.agents.ess.message.TriggerMessage;
import org.openhab.binding.fems.agents.onlinemonitoring.message.DataMessage;
import org.openhab.binding.fems.internal.essprotocol.ESSProtocol;
import org.openhab.binding.fems.tools.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESSAgent extends Agent {
	private final Logger logger = LoggerFactory.getLogger(ESSAgent.class);
	private final List<ESSAgentListener> listeners = new LinkedList<ESSAgentListener>();

	private class TriggerESSAgent implements Runnable {
		public void run() {
			handle(new TriggerMessage());
		}
	}
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final TriggerESSAgent triggerESSAgent = new TriggerESSAgent();
	private ScheduledFuture<?> scheduledFuture = null;
	private ESSProtocol protocol = null;
	
	/**
	 * {@inheritDoc}
	 */
	public ESSAgent(String name) {
		super(name);
	}
	
	@Override
	protected void init() {
		scheduledFuture = scheduler.scheduleAtFixedRate(triggerESSAgent, 0, Constants.ESS_PERIOD, TimeUnit.SECONDS);
	};
	
	@Override
	protected void dispose() {
		if(scheduledFuture != null) {
			scheduledFuture.cancel(false);
		}
		if(protocol != null) {
			protocol.dispose();
		}
		super.dispose();
	}

	@Override
	public void foreverLoop(Message message) throws InterruptedException {
		if(message instanceof ListenerMessage) {
			ESSAgentListener listener = ((ListenerMessage)message).getListener();
			listeners.add(listener);
		
		} else if (message instanceof ProtocolMessage) {
			protocol = ((ProtocolMessage)message).getProtocol();
			
		} else if (message instanceof TriggerMessage) {
			if(protocol != null) {
				try {
					// update data from storage system
					protocol.updateData();
				
					// no error happened: kick the watchdog
				    Runtime.getRuntime().exec("/bin/systemd-notify WATCHDOG=1");
				    
					// prepare data to be transfered
				    HashMap<String, Object> params = new HashMap<String, Object>();
				    InetAddress i = Tools.getIPaddress();
				    if(i != null) {
						params.put("ipv4", i.getHostAddress()); // local ipv4 address
			        }
					
					DataMessage dataMessage = protocol.getDataMessage(params);
					Constants.ONLINE_MONITORING_AGENT.sendData(dataMessage);
				    
					// update listeners
					for(ESSAgentListener listener : listeners) {
						listener.essUpdate(protocol.getWordRanges());
					}
				} catch(ModbusException e) {
					// modbus error: Try again within a timespan of 5 to 30 seconds:
					int min = 5; int max = 30;
					int waitTime = (int)(Math.random() * (max - min) + min);
					logger.info("Try again in " + waitTime + " seconds");
					protocol.closeSerialConnection();
					Thread.sleep((int)(Math.random() * (max - min) + min)*1000);
					if(messages.isEmpty()) { //only trigger, if not already triggered
						handle(new TriggerMessage());
					}
				} catch(Exception e) {
					logger.error("Exception occurred during execution: {}", e.getMessage());
					e.printStackTrace();
					protocol.dispose();
				}
			}
		}
	}

	public void setProtocol(ESSProtocol protocol) {
		handle(new ProtocolMessage(protocol));
	}
	
	public void addListener(ESSAgentListener listener) {
		handle(new ListenerMessage(listener));
	}
}

/**
 * Copyright (c) 2015 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.agents;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "Agent" is the base entity for all agents.
 * 
 * @author Stefan Feilmeier
 */
public abstract class Agent extends Thread {
	private static Logger logger = LoggerFactory.getLogger(Agent.class);
	protected final ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
	protected final Semaphore lock = new Semaphore(0);
	
	/**
	 * Create and start a new agent
	 */
	public Agent() {
	}
	
	/**
	 * Create and start a new agent
	 * 
	 * @param agentName Name of the agent
	 */
	public Agent(String name) {
		this();
		this.setName(name);
		
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	interrupt();
            	dispose();
            }
        });
	}
	
	/**
	 * Send a message to be handled by this agent
	 * (This method is thread-safe)
	 * 
	 * @param handle
	 */
	protected void handle(Message message) {
		messages.add(message);
		lock.release();
	}
	
	/**
	 * The main execution control of the agent 
	 */
	@Override
	public void run() {
		logger.debug("Agent started");
		init();
		while(true) {
			try {
				if(messages.isEmpty()) lock.acquire(); // wait for new message
				Message message = messages.poll(); 
				if(message == null) continue;
				if(message instanceof InterruptMessage) {
					break;
				}
				foreverLoop(message);
			} catch (InterruptedException e) {	
				logger.debug("ForeverLoop interrupted");
				handle(new InterruptMessage());
			}
		}
		dispose();
		logger.debug("ForeverLoop ended");
	}

	/**
	 * This method is called when the agent starts, before it is entering the ForeverLoop
	 */
	protected void init() {
		
	}
	
	/**
	 * This method is called just before the agent stops
	 */
	protected void dispose() {
		
	}

	/**
	 * This method is called over and over again after lock is released
	 * 
	 * @throws InterruptedException
	 */
	public abstract void foreverLoop(Message message) throws InterruptedException;
}

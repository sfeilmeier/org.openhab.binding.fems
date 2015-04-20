/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.internal.essprotocol.modbus;

import java.math.BigDecimal;

import net.wimpi.modbus.procimg.Register;

import org.eclipse.smarthome.core.library.types.DecimalType;

public class DecimalWordItem extends ModbusItem implements ModbusWordElement {
	//private Logger logger = LoggerFactory.getLogger(DecimalWordItem.class);
	private double multiplier = 1.;
	private int delta = 0;
	
	public DecimalWordItem(String name) {
		this(name, 1.);
	}
	
	public DecimalWordItem(String name, double multiplier) {
		super(name);
		this.multiplier = multiplier;
	}
	
	public DecimalWordItem(String name, double multiplier, int delta) {
		super(name);
		this.multiplier = multiplier;
		this.delta = delta;
	}

	@Override
	public void updateData(Register register) {
		setState(new DecimalType( new BigDecimal((register.getValue() - delta) * multiplier).setScale(2, BigDecimal.ROUND_HALF_UP) ) );
	}
}

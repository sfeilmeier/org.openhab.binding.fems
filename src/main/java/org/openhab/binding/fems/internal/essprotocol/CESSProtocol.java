/**
 * Copyright (c) 2014 Stefan Feilmeier <stefan.feilmeier@fenecon.de>.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fems.internal.essprotocol;

import java.util.ArrayList;
import java.util.Map;

import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElementRange;

import de.fenecon.fems.agents.OnlineMonitoring.Message.DataMessage;
import de.fenecon.fems.agents.OnlineMonitoring.Message.DataMessage.MethodType;

public class CESSProtocol extends ESSProtocol {
	public CESSProtocol(String modbusinterface, int unitid,
			ArrayList<ModbusElementRange> wordRanges) {
		super(modbusinterface, unitid, wordRanges);
	}

	@Override
	protected int getBaudrate() {
		return 19200;
	}

	@Override
	public DataMessage getDataMessage(Map<String, Object> params) {
		return getDataMessage(MethodType.COMMERCIAL, params);
	};
}

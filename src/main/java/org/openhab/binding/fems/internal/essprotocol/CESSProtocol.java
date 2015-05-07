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

import org.openhab.binding.fems.Constants;
import org.openhab.binding.fems.agents.onlinemonitoring.message.DataMessage;
import org.openhab.binding.fems.agents.onlinemonitoring.message.DataMessage.MethodType;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElementRange;

public class CESSProtocol extends ESSProtocol {
	public CESSProtocol(ArrayList<ModbusElementRange> wordRanges) {
		super(Constants.MODBUS_UNITID_CESS, wordRanges);
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

package org.openhab.binding.fems.agents.ess.message;

import java.util.List;
import org.openhab.binding.fems.internal.essprotocol.modbus.ModbusElementRange;

public interface ESSAgentListener {
	
	public void essUpdate(List<ModbusElementRange> wordRanges);
}

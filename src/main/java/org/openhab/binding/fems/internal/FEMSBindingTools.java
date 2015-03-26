package org.openhab.binding.fems.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;

public class FEMSBindingTools {
	/** Convert Eclipse Smarthome State to objects that JSON Smart can handle:
	 *		JSON		Java
	 *		true|false	java.lang.Boolean
	 *		number		java.lang.Number
	 *		string		java.lang.String
	 *		array		java.util.List
	 *		object		java.util.Map
	 *		null		null
	 * */
	public static Map<String, Object> convertStatesForMessage(Map<String, State> states) {
		if(states == null) return null;
		HashMap<String, Object> newStates = new HashMap<String, Object>();
		for (String key : states.keySet()) {
			State state = states.get(key);
			if(state instanceof OnOffType) {
				if((OnOffType)state == OnOffType.ON) {
					newStates.put(key, new Integer(1)); // would be boolean, but it is not properly handled by InfluxDB
				} else {
					newStates.put(key, new Integer(0));
				}
			} else if (state instanceof UnDefType) {
				newStates.put(key, null);
			} else if (state instanceof StringType) {
				newStates.put(key, state.toString());
			} else if (state instanceof DecimalType) {
				DecimalType stateDecimal = (DecimalType)state;
				newStates.put(key, stateDecimal.toBigDecimal());
			} else {
				newStates.put(key, state.toString());
			}
		}
		return newStates;
	}
}

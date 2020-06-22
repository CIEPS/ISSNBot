package org.issn.issnbot.listeners;

import java.util.LinkedHashMap;
import java.util.Map;

import org.issn.issnbot.listeners.IssnBotListener.PropertyStatus;

public class SerialResult {

	private Map<Integer, PropertyStatus> statuses;
	private Map<Integer, PropertyStatus> previousValuesStatuses;
	
	public SerialResult() {
		// LinkedHashMap keeps the order of insertion
		statuses = new LinkedHashMap<>();
		previousValuesStatuses = new LinkedHashMap<>(); 
	}

	public Map<Integer, PropertyStatus> getStatuses() {
		return statuses;
	}

	public void setStatuses(Map<Integer, PropertyStatus> statuses) {
		this.statuses = statuses;
	}

	public Map<Integer, PropertyStatus> getPreviousValuesStatuses() {
		return previousValuesStatuses;
	}

	public void setPreviousValuesStatuses(Map<Integer, PropertyStatus> previousValuesStatuses) {
		this.previousValuesStatuses = previousValuesStatuses;
	}	
	
	
}

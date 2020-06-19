package org.issn.issnbot.listeners;

import java.io.File;
import java.util.Map;

import org.issn.issnbot.WikidataUpdateStatus;

public interface IssnBotListener {

	public void start(File inputFile);

	public void stop();
	
	public void startBatch(String batchId);
	
	public void stopBatch(String batchId);
	
	public void startSerial(String issnl, String wikidataId);
	
	public void successSerial(String issnl, String wikidataId, Map<Integer, PropertyStatus> statuses);
	
	public void errorSerial(String issnl, String wikidataId);
	
	public class PropertyStatus {
		public WikidataUpdateStatus status;
		public String precision;
		
		public PropertyStatus(WikidataUpdateStatus status) {
			super();
			this.status = status;
		}

		public PropertyStatus(WikidataUpdateStatus status, String precision) {
			super();
			this.status = status;
			this.precision = precision;
		}
	}
	
}

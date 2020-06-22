package org.issn.issnbot.listeners;

import java.io.File;
import java.util.Map;

import org.issn.issnbot.WikidataUpdateStatus;
import org.issn.issnbot.model.SerialEntry;

public interface IssnBotListener {

	public void start(File inputFile);

	public void stop();
	
	public void startBatch(String batchId);
	
	public void stopBatch(String batchId);
	
	public void startSerial(SerialEntry entry);
	
	public void successSerial(SerialEntry entry, SerialResult result);
	
	public void errorSerial(SerialEntry entry, String message);
	
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

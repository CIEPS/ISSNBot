package org.issn.issnbot.inconsistencies;

import java.io.File;
import java.util.Map;

import org.issn.issnbot.WikidataUpdateStatus;
import org.issn.issnbot.model.SerialEntry;

public interface InconsistencyCheckerListener {

	public void start(File inputFile);

	public void stop();
	
	public void startFile(String fileName);
	
	public void stopFile(String fileName);
	
	public void startSerial(SerialEntry entry);
	
	public void scoreSerial(SerialEntry entry, double score);
	
	public void errorSerial(SerialEntry entry, String message);

	
}

package org.issn.issnbot.listeners;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.issn.issnbot.WikidataUpdateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssnBotOutputListener implements IssnBotListener {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	protected File outputDirectory;
	protected UpdateStatusesCSVWriter updateWriter;
	
	
	
	public IssnBotOutputListener(File outputDirectory) {
		super();
		this.outputDirectory = outputDirectory;
	}

	@Override
	public void start(File inputFile) {
		// nothing
	}

	@Override
	public void stop() {
		// nothing
	}

	@Override
	public void startBatch(String batchId) throws RuntimeException {
		// nothing
		try {
			File outputFile = new File(this.outputDirectory, batchId+"_output.csv");
			FileOutputStream os = new FileOutputStream(outputFile);
			OutputStreamWriter writer = new OutputStreamWriter(os);
			
			this.updateWriter = new UpdateStatusesCSVWriter(writer);
			
			// writeHeader
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void stopBatch(String batchId) {
		try {
			this.updateWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startSerial(String issnl, String wikidataId) {
		// nothing
	}

	@Override
	public void successSerial(String issnl, String wikidataId, Map<Integer, PropertyStatus> statuses) {
		try {
			this.updateWriter.success(issnl, wikidataId, statuses);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void errorSerial(String issnl, String wikidataId) {
		try {
			this.updateWriter.error(issnl, wikidataId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

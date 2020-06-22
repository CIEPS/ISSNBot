package org.issn.issnbot.listeners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.issn.issnbot.model.SerialEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssnBotOutputListener implements IssnBotListener {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	protected File outputDirectory;
	protected File errorDirectory;
	protected UpdateStatusesCSVWriter updateWriter;
	
	protected Writer errorWriter;
	
	public IssnBotOutputListener(File outputDirectory, File errorDirectory) {
		super();
		this.outputDirectory = outputDirectory;
		this.errorDirectory = errorDirectory;
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
			
			File errorFile = new File(this.errorDirectory, batchId+"_error.csv");
			FileOutputStream errorOs = new FileOutputStream(errorFile);
			this.errorWriter = new OutputStreamWriter(errorOs);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void stopBatch(String batchId) {
		try {
			this.updateWriter.close();
			this.errorWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startSerial(SerialEntry entry) {
		// nothing
	}

	@Override
	public void successSerial(SerialEntry entry, SerialResult result) {
		try {
			this.updateWriter.success(entry.getIssnL(), entry.getWikidataId(), result.getStatuses(), result.getPreviousValuesStatuses());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void errorSerial(SerialEntry entry, String message) {
		try {
			this.updateWriter.error(entry.getIssnL(), entry.getWikidataId(), message);
			this.errorWriter.write(entry.getRecord());
			this.errorWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

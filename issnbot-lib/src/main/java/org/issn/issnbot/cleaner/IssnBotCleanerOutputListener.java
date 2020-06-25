package org.issn.issnbot.cleaner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.issn.issnbot.listeners.IssnBotListener.PropertyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssnBotCleanerOutputListener implements IssnBotCleanerListener {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	protected File outputDirectory;
	protected IssnBotCleanerStatusesCSVWriter updateWriter;
	
	public IssnBotCleanerOutputListener(File outputDirectory) {
		super();
		this.outputDirectory = outputDirectory;
	}

	@Override
	public void start() {
		try {
			File outputFile = new File(this.outputDirectory, "clean_output.csv");
			FileOutputStream os = new FileOutputStream(outputFile);
			OutputStreamWriter writer = new OutputStreamWriter(os);
			
			this.updateWriter = new IssnBotCleanerStatusesCSVWriter(writer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		// nothing
	}


	@Override
	public void startItem(String qid) {
		// nothing
	}

	@Override
	public void successItem(String qid, boolean untouched, Map<Integer, PropertyStatus> statuses) {
		try {
			this.updateWriter.success(qid, untouched?"UNTOUCHED":"SUCCESS", statuses);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void errorItem(String qid, String message) {
		try {
			this.updateWriter.error(qid, message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

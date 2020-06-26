package org.issn.issnbot.listeners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.read.CSVSerialEntryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssnBotOutputListener implements IssnBotListener {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	protected File outputDirectory;
	protected File errorDirectory;
	protected UpdateStatusesCSVWriter updateWriter;
	
	protected Writer dataErrorWriter;
	protected Writer apiErrorWriter;
	
	protected transient boolean wroteApiErroWriterHeaderLine = false;
	
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
	public void startFile(String fileName) throws RuntimeException {
		// nothing
		try {
			File outputFile = new File(this.outputDirectory, fileName+"_output.csv");
			FileOutputStream os = new FileOutputStream(outputFile);
			OutputStreamWriter writer = new OutputStreamWriter(os);
			
			this.updateWriter = new UpdateStatusesCSVWriter(writer);
			
			File dataErrorFile = new File(this.errorDirectory, fileName+"_error_data.csv");
			FileOutputStream dataErrorOs = new FileOutputStream(dataErrorFile);
			this.dataErrorWriter = new OutputStreamWriter(dataErrorOs);
			
			File apiErrorFile = new File(this.errorDirectory, fileName+"_error_api.csv");
			FileOutputStream apiErrorOs = new FileOutputStream(apiErrorFile);
			this.apiErrorWriter = new OutputStreamWriter(apiErrorOs);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void stopFile(String fileName) {
		try {
			this.updateWriter.close();
			this.apiErrorWriter.close();
			this.dataErrorWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startSerial(SerialEntry entry) {
		// nothing
	}

	@Override
	public void successSerial(SerialEntry entry, boolean noUpdate, SerialResult result) {
		try {
			this.updateWriter.success(entry.getIssnL(), entry.getWikidataId(), noUpdate?"UNTOUCHED":"SUCCESS", result.getStatuses(), result.getPreviousValuesStatuses());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void errorSerial(SerialEntry entry, boolean apiError, String message) {
		try {
			this.updateWriter.error(entry.getIssnL(), entry.getWikidataId(), apiError, message);
			if(apiError) {
				if(!wroteApiErroWriterHeaderLine) {
					this.writeApiErrorWriterErrorLine();
				}
				this.apiErrorWriter.write(entry.getRecord());
				this.apiErrorWriter.flush();
			} else {
				this.dataErrorWriter.write(entry.getRecord());
				this.dataErrorWriter.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void writeApiErrorWriterErrorLine() throws IOException {
		this.apiErrorWriter.write(Arrays.asList(CSVSerialEntryReader.COLUMN.values()).stream().map(v -> v.getHeader()).collect(Collectors.joining(",")));
		this.apiErrorWriter.flush();
		wroteApiErroWriterHeaderLine = true;
	}
	
}

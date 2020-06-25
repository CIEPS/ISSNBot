package org.issn.issnbot.inconsistencies;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.read.CSVSerialEntryReader;
import org.issn.issnbot.read.SerialEntryReadException;
import org.issn.issnbot.read.SerialEntryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

/**
 * This is an empty class for now, "reserved for future potential use".
 * @author thomas
 *
 */
public class InconsistencyChecker {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	private List<InconsistencyCheckerListener> listeners = new ArrayList<InconsistencyCheckerListener>();
	
	private transient String currentFileName;
	
	private SerialDataProviderIfc dataProvider;
	
	public InconsistencyChecker(SerialDataProviderIfc dataProvider) {
		super();
		this.dataProvider = dataProvider;
	}

	public void checkInconsistencies(File inputFolder) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {
		
		// notify start
		listeners.forEach(l -> l.start(inputFolder));

		// recurse in subdirs
		for (File anInputFile : FileUtils.listFiles(inputFolder, new String[] {"csv", "xls", "tsv"}, true)) {			
			// keep track of current file name to put in edit messages
			this.currentFileName = anInputFile.getName();
			// init CSV reader
			SerialEntryReader reader = new CSVSerialEntryReader(new FileInputStream(anInputFile));
			// process the file
			this.checkInconsistencies(reader, anInputFile.getName());
		}

		// notify stop
		listeners.forEach(l -> l.stop());
	}
	
	public void checkInconsistencies(SerialEntryReader reader, String filename) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {

		// notify start file
		listeners.stream().forEach(l -> l.startFile(filename));

		List<SerialEntry> entries = reader.read();
		log.debug("Checking inconsistencies in "+entries.size()+" entries");
		
		List<SerialEntry> currentBatch = new ArrayList<>();
		for (SerialEntry serialEntry : entries) {
			checkInconsistencies(serialEntry);
		}

		// notify end file
		listeners.stream().forEach(l -> l.stopFile(filename));
	}
	
	public void checkInconsistencies(SerialEntry entry) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {
		// notify score
		listeners.stream().forEach(l -> l.scoreSerial(entry, 1.0d));
	}

	public List<InconsistencyCheckerListener> getListeners() {
		return listeners;
	}

	public void setListeners(List<InconsistencyCheckerListener> listeners) {
		this.listeners = listeners;
	}
	
}

package org.issn.issnbot.read;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.issn.issnbot.model.SerialEntry;

public interface SerialEntryReader {

	public List<SerialEntry> read() throws IOException, SerialEntryReadException;
	
}

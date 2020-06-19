package org.issn.issnbot.listeners;

import java.io.IOException;
import java.util.Map;

import org.issn.issnbot.WikidataUpdateStatus;

public interface UpdateStatusesWriter {

	public void success(String issnl, String qid, Map<Integer, IssnBotListener.PropertyStatus> updateStatus) throws IOException;
	
	public void error(String issnl, String qid) throws IOException;
	
	public void close() throws IOException;
}

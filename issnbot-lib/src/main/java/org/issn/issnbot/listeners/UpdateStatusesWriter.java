package org.issn.issnbot.listeners;

import java.io.IOException;
import java.util.Map;

import org.issn.issnbot.WikidataUpdateStatus;

public interface UpdateStatusesWriter {

	public void success(
			String issnl,
			String qid,
			String code,
			Map<Integer, IssnBotListener.PropertyStatus> updateStatus,
			Map<Integer, IssnBotListener.PropertyStatus> previousValueUpdateStatuses
	) throws IOException;
	
	public void error(String issnl, String qid, boolean apiError, String message) throws IOException;
	
	public void close() throws IOException;
}

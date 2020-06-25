package org.issn.issnbot.cleaner;

import java.util.Map;

import org.issn.issnbot.listeners.IssnBotListener.PropertyStatus;

public interface IssnBotCleanerListener {

	public void start();

	public void stop();
	
	public void startItem(String qid);
	
	public void successItem(String qid, boolean untouched, Map<Integer, PropertyStatus> statuses);
	
	public void errorItem(String qid, String message);

	
}

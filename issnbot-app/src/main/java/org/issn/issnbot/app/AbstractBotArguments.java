package org.issn.issnbot.app;

import com.beust.jcommander.Parameter;

public class AbstractBotArguments {
	
	
	/***
	 * Default constructor required
	 */
	public AbstractBotArguments() {
		super();
	}
	
	@Parameter(
			names = { "login" },
			description = "Wikidata login to use. Defaults to 'IssnBot'. Password must be provided in the 'password' System property",			
			required = false
	)
	protected String login = "IssnBot";
	
	@Parameter(
			names = { "maxLag" },
			description = "Sets the maxLag parameter on Wikidata connection in seconds. Defaults to 5. : \"make the client wait until the replication lag is less than the specified value\". See http://wikidata.github.io/Wikidata-Toolkit/org/wikidata/wdtk/wikibaseapi/WikibaseDataEditor.html#getMaxLag()."
					+ "Current maxLag can be seen at https://www.wikidata.org/w/api.php?action=query&titles=MediaWiki&format=json&maxlag=-1",			
			required = false
	)
	protected Integer maxLag = null;
	
	@Parameter(
			names = { "maxLagBackoffFactor" },
			description = "Sets the maxLagBackoffFactor parameter on Wikidata connection. Defaults to 1.5. Multiplicator of wait time between each attempt. See http://wikidata.github.io/Wikidata-Toolkit/org/wikidata/wdtk/wikibaseapi/WikibaseDataEditor.html#setMaxLagBackOffFactor(double)",			
			required = false
	)
	protected Double maxLagBackoffFactor = null;
	
	@Parameter(
			names = { "maxLagMaxRetries" },
			description = "Sets the maxLagmaxRetries parameter on Wikidata connection. Defaults to 14. Number of retries when server is overloaded. See http://wikidata.github.io/Wikidata-Toolkit/org/wikidata/wdtk/wikibaseapi/WikibaseDataEditor.html#setMaxLagMaxRetries(int)",			
			required = false
	)
	protected Integer maxLagMaxRetries = null;
	
	@Parameter(
			names = { "maxLagFirstWaitTime" },
			description = "Sets the maxLagFirstWaitTime parameter on Wikidata connection in milliseconds. Defaults to 1000. See http://wikidata.github.io/Wikidata-Toolkit/org/wikidata/wdtk/wikibaseapi/WikibaseDataEditor.html#setMaxLagFirstWaitTime(int)",			
			required = false
	)
	protected Integer maxLagFirstWaitTime = null;
	
	@Parameter(
			names = { "pauseBetweenEdits" },
			description = "Ensures the bot waits a given number of milliseconds between each call to the API. Wikidata Java API ensures an _average_ time between edits, defaulting to 2000ms, but computed in a window of 9 edits, and calls may be rejected before that. Set this value lower than 2000. Defaults to 1000.",			
			required = false
	)
	protected Integer pauseBetweenEdits = 1000;
	
	@Parameter(
			names = { "batchId" },
			description = "Sets a batchId so that the updates made in multiple runs of the bot are tracked in a single batchID so they can be rollbacked together. If not provided, a default batchId is set. batchId should be uninformative hexadecimal hash, see https://www.wikidata.org/wiki/Wikidata:Edit_groups/Adding_a_tool#For_custom_bots"		
	)
	protected String batchId = null;
	

	public String getLogin() {
		return login;
	}

	public Integer getMaxLag() {
		return maxLag;
	}

	public Double getMaxLagBackoffFactor() {
		return maxLagBackoffFactor;
	}

	public Integer getMaxLagMaxRetries() {
		return maxLagMaxRetries;
	}

	public Integer getMaxLagFirstWaitTime() {
		return maxLagFirstWaitTime;
	}

	public String getBatchId() {
		return batchId;
	}

	public Integer getPauseBetweenEdits() {
		return pauseBetweenEdits;
	}
	
	
}

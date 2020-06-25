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
			description = "Sets the maxLag parameter on Wikidata connection in seconds. Defaults to 5. : \"make the client wait until the replication lag is less than the specified value\". See http://wikidata.github.io/Wikidata-Toolkit/org/wikidata/wdtk/wikibaseapi/WikibaseDataEditor.html#getMaxLag()",			
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
	
	
}

package org.issn.issnbot;

import java.util.Arrays;
import java.util.HashSet;

import org.issn.issnbot.model.WikidataIssnModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

public class AbstractWikidataBot {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	protected String agentName;
	
	protected String login;
	protected String password;
	
	protected Integer wikidata_maxLag = null;
	protected Integer wikidata_maxLagMaxRetries = null;
	protected Double  wikidata_maxLagBackoffFactor = null;
	protected Integer wikidata_maxLagFirstWaitTime = null;
	
	protected transient WikibaseDataEditor wbde;
	protected transient WikibaseDataFetcher wbdf;
	
	protected boolean dryRun = true;
	
	public AbstractWikidataBot(String agentName, String login, String password) {
		super();
		this.agentName = agentName;
		this.login = login;
		this.password = password;
	}
	
	public void initConnection() throws LoginFailedException {
		// Always set your User-Agent to the name of your application:
		WebResourceFetcherImpl.setUserAgent(this.agentName);

		BasicApiConnection connection = BasicApiConnection.getWikidataApiConnection();
		// Login -- required for operations on real wikis:
		connection.login(login, password);

		this.wbde = new WikibaseDataEditor(connection, WikidataIssnModel.WIKIDATA_IRI);
		this.wbde.setEditAsBot(true);
		if(this.wikidata_maxLag != null) {
			this.wbde.setMaxLag(this.wikidata_maxLag);
		}
		if(this.wikidata_maxLagFirstWaitTime != null) {
			this.wbde.setMaxLagFirstWaitTime(this.wikidata_maxLagFirstWaitTime);
		}
		if(this.wikidata_maxLagBackoffFactor != null) {
			this.wbde.setMaxLagBackOffFactor(this.wikidata_maxLagBackoffFactor);
		}
		if(this.wikidata_maxLagMaxRetries != null) {
			this.wbde.setMaxLagMaxRetries(this.wikidata_maxLagMaxRetries);
		}
		log.info("Wikidata connection parameters : maxLag="+this.wbde.getMaxLag()+" seconds, firstWaitTime="+this.wbde.getMaxLagFirstWaitTime()+", backoffFactor="+this.wbde.getMaxLagBackOffFactor()+", maxRetries="+this.wbde.getMaxLagMaxRetries());

		this.wbdf = new WikibaseDataFetcher(connection, Datamodel.SITE_WIKIDATA);
		// we don't care about site links
		this.wbdf.getFilter().setSiteLinkFilter(new HashSet<String>());
		this.wbdf.getFilter().setPropertyFilter(new HashSet<PropertyIdValue>(Arrays.asList(new PropertyIdValue[] {
				WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID),
				WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID),
				WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSNL_PROPERTY_ID),
				WikidataIssnModel.toWikidataProperty(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID),
				WikidataIssnModel.toWikidataProperty(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID),
				WikidataIssnModel.toWikidataProperty(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID)
		})));		
	}


	public Integer getWikidata_maxLag() {
		return wikidata_maxLag;
	}


	public void setWikidata_maxLag(Integer wikidata_maxLag) {
		this.wikidata_maxLag = wikidata_maxLag;
	}


	public Integer getWikidata_maxLagMaxRetries() {
		return wikidata_maxLagMaxRetries;
	}


	public void setWikidata_maxLagMaxRetries(Integer wikidata_maxLagMaxRetries) {
		this.wikidata_maxLagMaxRetries = wikidata_maxLagMaxRetries;
	}


	public Double getWikidata_maxLagBackoffFactor() {
		return wikidata_maxLagBackoffFactor;
	}


	public void setWikidata_maxLagBackoffFactor(Double wikidata_maxLagBackoffFactor) {
		this.wikidata_maxLagBackoffFactor = wikidata_maxLagBackoffFactor;
	}


	public Integer getWikidata_maxLagFirstWaitTime() {
		return wikidata_maxLagFirstWaitTime;
	}


	public void setWikidata_maxLagFirstWaitTime(Integer wikidata_maxLagFirstWaitTime) {
		this.wikidata_maxLagFirstWaitTime = wikidata_maxLagFirstWaitTime;
	}


	public String getAgentName() {
		return agentName;
	}


	public String getLogin() {
		return login;
	}


	public String getPassword() {
		return password;
	}
	
	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}
	
}

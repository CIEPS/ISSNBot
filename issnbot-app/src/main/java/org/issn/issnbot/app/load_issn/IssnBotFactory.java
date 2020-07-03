package org.issn.issnbot.app.load_issn;

import java.util.Map;

import org.issn.issnbot.IssnBot;
import org.issn.issnbot.listeners.IssnBotOutputListener;
import org.issn.issnbot.listeners.IssnBotReportListener;
import org.issn.issnbot.providers.PropertiesLanguageCodesProvider;
import org.issn.issnbot.providers.PropertiesLanguageIdProvider;
import org.issn.issnbot.providers.WikidataDistributionFormatProvider;
import org.issn.issnbot.providers.WikidataLanguageCodesProvider;
import org.issn.issnbot.providers.WikidataSparqlCountryIdProvider;
import org.issn.issnbot.providers.WikidataSparqlLanguageIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;

public class IssnBotFactory {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	private ArgumentsLoadIssn args;
	
	public IssnBotFactory(ArgumentsLoadIssn args) {
		super();
		this.args = args;
	}



	public IssnBot build() throws LoginFailedException {
		
		log.info("Init Language IDs map...");
		// WikidataSparqlLanguageIdProvider langIdProvider = new WikidataSparqlLanguageIdProvider();
		PropertiesLanguageIdProvider langIdProvider = new PropertiesLanguageIdProvider();
		log.info("Init Country codes map...");
		WikidataSparqlCountryIdProvider countryIdProvier = new WikidataSparqlCountryIdProvider();
		log.info("Init Language codes map...");
		// WikidataLanguageCodesProvider languageCodes = new WikidataLanguageCodesProvider();
		Map<String, String> languageCodes = new PropertiesLanguageCodesProvider().getLanguageCodes();
		log.info("Init Distribution formats map...");
		WikidataDistributionFormatProvider distributionFormats = new WikidataDistributionFormatProvider();
		
		// output language maps in log folder
		
		
		IssnBot bot = new IssnBot(
				this.args.getLogin(),
				System.getProperty("password"),
				langIdProvider,
				countryIdProvier,
				languageCodes,
				distributionFormats
		);
		
		if(this.args.getLimit() != null) {
			bot.setLimit(this.args.getLimit());
		}
		
		bot.setWikidata_maxLag(args.getMaxLag());
		bot.setWikidata_maxLagFirstWaitTime(args.getMaxLagFirstWaitTime());
		bot.setWikidata_maxLagBackoffFactor(args.getMaxLagBackoffFactor());
		bot.setWikidata_maxLagMaxRetries(args.getMaxLagMaxRetries());
		bot.setWikidata_averageTimePerEdit(args.getAverageTimePerEdit());
		
		bot.getListeners().add(new IssnBotReportListener());
		bot.getListeners().add(new IssnBotOutputListener(args.getOutput(), args.getError()));
		
		bot.setDryRun(!args.isUpdate());
		
		if(args.getBatchId() != null) {
			bot.setBatchId(args.getBatchId());
		}
		
		if(args.getPauseBetweenEdits() != null) {
			bot.setPauseBetweenEdits(args.getPauseBetweenEdits());
		}
		
		bot.initConnection();
		
		return bot;
	}
	
}

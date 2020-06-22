package org.issn.issnbot.app.load_issn;

import org.issn.issnbot.IssnBot;
import org.issn.issnbot.listeners.IssnBotOutputListener;
import org.issn.issnbot.listeners.IssnBotReportListener;
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
		WikidataSparqlLanguageIdProvider langIdProvider = new WikidataSparqlLanguageIdProvider();
		log.info("Init Country codes map...");
		WikidataSparqlCountryIdProvider countryIdProvier = new WikidataSparqlCountryIdProvider();
		log.info("Init Language codes map...");
		WikidataLanguageCodesProvider languageCodes = new WikidataLanguageCodesProvider();
		log.info("Init Distribution formats map...");
		WikidataDistributionFormatProvider distributionFormats = new WikidataDistributionFormatProvider();
		
		
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
		
		bot.getListeners().add(new IssnBotReportListener());
		bot.getListeners().add(new IssnBotOutputListener(args.getOutput(), args.getError()));
		
		bot.setDryRun(!args.isUpdate());
		
		bot.initConnection();
		
		return bot;
	}
	
}

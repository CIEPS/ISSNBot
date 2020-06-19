package org.issn.issnbot.app.load_issn;

import org.issn.issnbot.IssnBot;
import org.issn.issnbot.app.CommandIfc;
import org.issn.issnbot.listeners.IssnBotOutputListener;
import org.issn.issnbot.listeners.IssnBotReportListener;
import org.issn.issnbot.providers.WikidataDistributionFormatProvider;
import org.issn.issnbot.providers.WikidataLanguageCodesProvider;
import org.issn.issnbot.providers.WikidataSparqlCountryIdProvider;
import org.issn.issnbot.providers.WikidataSparqlLanguageIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadIssn implements CommandIfc {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	@Override
	public void execute(Object o) {
		log.info("Running command : "+this.getClass().getSimpleName());
		
		if(System.getProperty("password") == null) {
			String message = "Please provide the Wikidata password to use in the 'password' System property e.g. java -Dpassword=xxxx -jar ...";
			System.out.println(message);
			log.error(message);
			System.exit(-1);
		}
		
		long start = System.currentTimeMillis();
		ArgumentsLoadIssn args = (ArgumentsLoadIssn)o;
		log.info("  Input folder : {}", args.getInput().getAbsolutePath());
		log.info("  Output folder : {}", args.getOutput().getAbsolutePath());
		log.info("  Error folder : {}", args.getError().getAbsolutePath());		
		
		// check if everything exists
		exitIf(!args.getInput().exists(), "Provided input folder '"+args.getInput()+"' does not exist, cannot proceed." );
		if( !args.getOutput().exists() ) {
			args.getOutput().mkdirs();
		}
		if( !args.getError().exists() ) {
			args.getError().mkdirs();
		}
		
		// check if input folder is not empty
		exitIf(!args.getInput().isDirectory() , "Provided input parameter '"+args.getInput()+"' is not a directory cannot proceed.");
		exitIf( args.getInput().list().length == 0, "Provided input folder '"+args.getInput()+"' is empty, cannot proceed." );
		
		try {
			
			log.info("Init Language IDs map...");
			WikidataSparqlLanguageIdProvider langIdProvider = new WikidataSparqlLanguageIdProvider();
			log.info("Init Country codes map...");
			WikidataSparqlCountryIdProvider countryIdProvier = new WikidataSparqlCountryIdProvider();
			log.info("Init Language codes map...");
			WikidataLanguageCodesProvider languageCodes = new WikidataLanguageCodesProvider();
			log.info("Init Distribution formats map...");
			WikidataDistributionFormatProvider distributionFormats = new WikidataDistributionFormatProvider();
			
			log.info("Running IssnBot...");
			
			IssnBot bot = new IssnBot(
					args.login,
					System.getProperty("password"),
					langIdProvider,
					countryIdProvier,
					languageCodes,
					distributionFormats
			);
			
			bot.getListeners().add(new IssnBotReportListener());
			bot.getListeners().add(new IssnBotOutputListener(args.getOutput()));
			
			bot.setDryRun(!args.isUpdate());
			
			bot.processFolder(args.getInput());
			
			log.info("Command : "+this.getClass().getSimpleName()+" finished successfully in {} ms", (System.currentTimeMillis() - start));
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
			System.exit(-1);
		}
		
	}
	
	private void exitIf(boolean test, String message) {
		if(test) {
			System.out.println(message);
			log.error(message);
			System.exit(-1);
		}
	}

}

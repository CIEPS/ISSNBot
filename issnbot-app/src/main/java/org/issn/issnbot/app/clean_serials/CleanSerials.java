package org.issn.issnbot.app.clean_serials;

import java.util.stream.Collectors;

import org.issn.issnbot.app.CommandIfc;
import org.issn.issnbot.cleaner.IssnBotCleaner;
import org.issn.issnbot.cleaner.IssnBotCleanerOutputListener;
import org.issn.issnbot.cleaner.ListEntriesToCleanProvider;
import org.issn.issnbot.cleaner.TitleBasedEntriesToCleanProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;

public class CleanSerials implements CommandIfc {

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
		ArgumentsCleanSerials args = (ArgumentsCleanSerials)o;
		log.info("  Output folder : {}", args.getOutput().getAbsolutePath());		
		
		try {
			
			IssnBotCleaner bot = new IssnBotCleaner(args.getLogin(), System.getProperty("password"));
			
			bot.getListeners().add(new IssnBotCleanerOutputListener(args.getOutput()));
			
			bot.setWikidata_maxLag(args.getMaxLag());
			bot.setWikidata_maxLagFirstWaitTime(args.getMaxLagFirstWaitTime());
			bot.setWikidata_maxLagBackoffFactor(args.getMaxLagBackoffFactor());
			bot.setWikidata_maxLagMaxRetries(args.getMaxLagMaxRetries());
			
			bot.setDryRun(!args.isUpdate());
			bot.initConnection();
			
			if(args.getBatchId() != null) {
				bot.setBatchId(args.getBatchId());
			}
			
			if(args.getPauseBetweenEdits() != null) {
				bot.setPauseBetweenEdits(args.getPauseBetweenEdits());
			}
			
			// run cleaning
			if(args.getQid() != null && !args.getQid().isEmpty() ) {
				bot.clean(new ListEntriesToCleanProvider(args.getQid().stream().map(id -> Datamodel.makeWikidataItemIdValue(id)).collect(Collectors.toList())));
			} else {
				bot.clean(new TitleBasedEntriesToCleanProvider());
			}
			
			
			log.info("Command : "+this.getClass().getSimpleName()+" finished successfully in {} ms", (System.currentTimeMillis() - start));
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
			System.exit(-1);
		}
		
	}

}

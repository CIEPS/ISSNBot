package org.issn.issnbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.issn.issnbot.listeners.IssnBotListener;
import org.issn.issnbot.listeners.IssnBotReportListener;
import org.issn.issnbot.model.IssnValue;
import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.model.WikidataIssnModel;
import org.issn.issnbot.providers.PropertiesLanguageCodesProvider;
import org.issn.issnbot.providers.WikidataDistributionFormatProvider;
import org.issn.issnbot.providers.WikidataIdProviderIfc;
import org.issn.issnbot.providers.WikidataLanguageCodesProvider;
import org.issn.issnbot.providers.WikidataSparqlCountryIdProvider;
import org.issn.issnbot.providers.WikidataSparqlLanguageIdProvider;
import org.issn.issnbot.read.CSVSerialEntryReader;
import org.issn.issnbot.read.SerialEntryReadException;
import org.issn.issnbot.read.SerialEntryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

public class IssnBot extends AbstractWikidataBot {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	public static final SimpleDateFormat MESSAGE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
	public static final String AGENT_NAME = "ISSN Bot";

	private String editSummary = null;

	

	private WikidataIdProviderIfc languageIdProvider;
	private WikidataIdProviderIfc countryIdProvider;
	private Map<String, String> languageCodes;
	private WikidataIdProviderIfc formatsIdProvider;

	private List<IssnBotListener> listeners = new ArrayList<IssnBotListener>();

	private int limit = -1;	
	
	private transient Date runDate;
	private transient String currentFileName;
	
	private transient int serialsProcessed = 0;
	
	private transient Map<String, EntityDocument> readAheadCache = new HashMap<>();
	
	private transient long lastEditTimeMillis = -1;
	
	private int maxNbRetries = 5;
	
	public IssnBot(
			String login,
			String password,
			WikidataIdProviderIfc languageIdProvider,
			WikidataIdProviderIfc countryIdProvider,
			Map<String, String> languageCodes,
			WikidataIdProviderIfc formatsIdProvider
	) {

		super(AGENT_NAME, login, password);
		
		this.languageIdProvider = languageIdProvider;
		this.countryIdProvider = countryIdProvider;
		this.languageCodes = languageCodes;
		this.formatsIdProvider = formatsIdProvider;
	}


	public void processFolder(File inputFolder) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {

		// save the date at which bot runs
		this.runDate = new Date();
		
		// notify start
		listeners.forEach(l -> l.start(inputFolder));

		if(this.batchId == null) {
			// assign batch ID - each run gets its own batch ID
			this.batchId = Long.toString((new Random()).nextLong(), 16).replace("-", "");
			log.info("Initialized batch identifier to : "+this.batchId);
		}
		
		// recurse in subdirs
		for (File anInputFile : FileUtils.listFiles(inputFolder, new String[] {"csv", "xls", "tsv"}, true)) {				
			// keep track of current file name to put in edit messages
			this.currentFileName = anInputFile.getName();
			// init CSV reader
			SerialEntryReader reader = new CSVSerialEntryReader(new FileInputStream(anInputFile));
			// process the file
			this.process(reader.read(), anInputFile.getName());
			// if we have reached the limit, stop
			if(limit > 0 && serialsProcessed == limit) {
				break;
			}
		}

		// notify stop
		listeners.forEach(l -> l.stop());
	}
	
	public void readAhead(List<String> entityIds) throws MediaWikiApiErrorException, IOException {
		this.readAheadCache = wbdf.getEntityDocuments(entityIds);
	}

	public void process(List<SerialEntry> entries, String filename) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {

		// notify start file
		listeners.stream().forEach(l -> l.startFile(filename));

		log.debug("Processing "+entries.size()+" entries in batch ID "+batchId);
		if(this.dryRun) {
			log.debug("Operating in dry run, no real updates will take place in Wikidata");
		} else {
			log.warn("This is a real run, updates will take place in Wikidata !");
		}

		int BATCH_SIZE = 50;
		
		List<SerialEntry> currentBatch = new ArrayList<>();
		for (SerialEntry serialEntry : entries) {
			serialsProcessed++;
			currentBatch.add(serialEntry);
			if(currentBatch.size() == BATCH_SIZE) {
				processBatch(currentBatch);
				currentBatch = new ArrayList<>();
				log.info("Processed "+this.serialsProcessed+" / "+entries.size());
			}
			
			if(limit > 0 && serialsProcessed == limit) {
				log.info("Reached limit ("+this.limit+"), stop");
				break;
			}
		}
		
		// process last part
		if(currentBatch.size() > 0) {
			processBatch(currentBatch);
			currentBatch = new ArrayList<>();
			log.info("Processed "+this.serialsProcessed+" / "+entries.size());
		}

		// notify end file
		listeners.stream().forEach(l -> l.stopFile(filename));
	}
	
	public void processBatch(List<SerialEntry> currentBatch) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {
		this.readAhead(currentBatch.stream().map(e -> e.getWikidataId()).collect(Collectors.toList()));
		for (SerialEntry serialEntry : currentBatch) {
			processSerial(serialEntry, 0);
		}
		
	}

	public void processSerial(SerialEntry entry, int nbRetries) {
		log.debug("Processing row "+entry.getRecordNumber()+" : "+entry.getIssnL());

		// notify serial
		listeners.stream().forEach(l -> l.startSerial(entry));

		try {

			// ItemIdValue entityId = ItemIdValue.NULL; // used when creating new items
			// ItemIdValue entityId = Datamodel.makeWikidataItemIdValue("Q84025820");
			ItemIdValue entityId = Datamodel.makeWikidataItemIdValue(entry.getWikidataId());
			
			
			// check availability of language code
			if(this.languageCodes.get(entry.getLang().getValue()) == null) {
				throw new IssnBotException(entry.getIssnL()+" - Language code "+entry.getLang().getValue()+" is not an iso6392 code associated to a Wikimedia code in Wikidata.");
			}
			
			// Fetch the entity data from Wikidata : this was read in the readAhead cache
			ItemDocument currentItem = (ItemDocument)this.readAheadCache.get(entry.getWikidataId());
			if(currentItem == null) {
				// if called directly (not in a batch), call API directly
				currentItem = (ItemDocument)wbdf.getEntityDocument(entry.getWikidataId());
			}
			log.trace(currentItem.toString());

			WikidataSerial wikidataSerial = new WikidataSerial(currentItem, languageCodes);

			List<Statement> statementsToAdd = new ArrayList<>();
			List<Statement> statementsToDelete = new ArrayList<>();

			// process ISSN-L statement
			wikidataSerial.updateIssnLStatement(entry).ifPresent(statementsToAdd::add);
			
			// synchronize Label and Aliases
			List<MonolingualTextValue> addLabels = wikidataSerial.getLabelsToAdd(entry);
			List<MonolingualTextValue> addAliases = wikidataSerial.getAliasesToAdd(entry, !addLabels.isEmpty());
			
			
			// process title statement
			wikidataSerial.updateTitle(entry).ifPresent(statementsToAdd::add);

			// process language statement
			wikidataSerial.updateLanguage(entry, this.languageIdProvider).ifPresent(statementsToAdd::add);

			// process place of publication statement
			wikidataSerial.updatePlaceOfPublicationStatement(entry, this.countryIdProvider).stream().forEach(statementsToAdd::add);
			
			// process official websites statements
			wikidataSerial.updateOfficialWebsiteStatements(entry).stream().forEach(statementsToAdd::add);
			
			// update all 4 ISSNs
			wikidataSerial.updateIssn1Statement(entry, this.formatsIdProvider).ifPresent(statementsToAdd::add);
			wikidataSerial.updateIssn2Statement(entry, this.formatsIdProvider).ifPresent(statementsToAdd::add);
			wikidataSerial.updateIssn3Statement(entry, this.formatsIdProvider).ifPresent(statementsToAdd::add);
			wikidataSerial.updateIssn4Statement(entry, this.formatsIdProvider).ifPresent(statementsToAdd::add);
			// compute statements to delete, if necessary
			wikidataSerial.getUnknownIssnStatementsToDelete(entry).stream().forEach(statementsToDelete::add);
			
			// process cancelled ISSNs
			wikidataSerial.updateCancelledIssnStatements(entry).stream().forEach(statementsToAdd::add);
			// compute statements to delete, if necessary
			wikidataSerial.getUnknownCancelledIssnStatementsToDelete(entry).stream().forEach(statementsToDelete::add);
			
				
			// if nothing to do, don't do anything
			if(
					addLabels.isEmpty()
					&&
					addAliases.isEmpty()
					&&
					statementsToAdd.isEmpty()
					&&
					statementsToDelete.isEmpty()
			) {
				// notify untouched
				log.debug("No modification to do, don't call API.");
				listeners.stream().forEach(l -> l.successSerial(entry, true, wikidataSerial.getResult()));
			} else {
				
				if(!this.dryRun ) {
					
					
					String batchIdentifier = "([[:toollabs:editgroups/b/ISSNBot/" + this.batchId + "|details]])";
					String currentEditSummary = (this.editSummary != null)?this.editSummary:AGENT_NAME+" synch ISSN-L "+entry.getIssnL()+" from "+this.currentFileName+" at "+MESSAGE_DATE_FORMAT.format(this.runDate);
					
					try {
						
						// pause if needed
						long now = System.currentTimeMillis();
						if(this.pauseBetweenEdits > 0 && this.lastEditTimeMillis > 0 && ((now - this.lastEditTimeMillis) < this.pauseBetweenEdits)) {
							try {
								long pauseTime = this.pauseBetweenEdits - (now - this.lastEditTimeMillis);
								log.debug("Pausing for "+pauseTime+" millis...");
							    Thread.sleep(pauseTime);
							} catch(InterruptedException ex) {
							    Thread.currentThread().interrupt();
							}
						}
						
						log.debug("Calling API to update "+entry.getWikidataId()+"...");
						log.debug("Added Statements "+statementsToAdd.toString()+"...");
						
						ItemDocument newItemDocument = wbde.updateTermsStatements(
								entityId,
								// addLabels,
								addLabels,
								// addDescriptions,
								Collections.emptyList(),
								// addAliases,
								addAliases,
								// deleteAliases
								Collections.emptyList(),
								// statements to add
								statementsToAdd,
								// statements to delete						
								statementsToDelete,
								// summary
								currentEditSummary+" | "+batchIdentifier,
								// tags
								Collections.emptyList()
						);
						this.lastEditTimeMillis = System.currentTimeMillis();
					} catch (Exception e) {
						this.lastEditTimeMillis = System.currentTimeMillis();
						log.error(e.getMessage(),e);	
						if(nbRetries >= this.maxNbRetries) {		
							// notify error
							log.error("Reached maximum number of retries, notifying error.");
							listeners.stream().forEach(l -> l.errorSerial(entry, true, e.getMessage()));
							return;
						} else {
							int updatedNbRetries = nbRetries++;
							log.info("Got an API exception, retrying for "+updatedNbRetries+" time on a maximum of "+this.maxNbRetries+" times");
							processSerial(entry, updatedNbRetries);
						}						
					}
					
					log.debug("API called successfully");
				
				}
				
				// notify success
				listeners.stream().forEach(l -> l.successSerial(entry, false, wikidataSerial.getResult()));
			}

		} catch(Exception e) {
			log.error(e.getMessage(),e);			
			// notify error
			listeners.stream().forEach(l -> l.errorSerial(entry, false, e.getMessage()));
		}

	}
	



	public String getEditSummary() {
		return editSummary;
	}

	public void setEditSummary(String editSummary) {
		this.editSummary = editSummary;
	}

	public List<IssnBotListener> getListeners() {
		return listeners;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public static void main(String...args) {		
		try {
			IssnBot agent = new IssnBot(
					args[0],
					args[1],
					new WikidataSparqlLanguageIdProvider(),
					new WikidataSparqlCountryIdProvider(),
					new PropertiesLanguageCodesProvider().getLanguageCodes(),
					new WikidataDistributionFormatProvider()
					);
			agent.getListeners().add(new IssnBotReportListener());
			agent.setDryRun(false);
			agent.setEditSummary("ISSN Bot test - See https://www.wikidata.org/wiki/Wikidata_talk:WikiProject_Periodicals#Data_donation_from_ISSN_Register_-_Feedback_welcome");
			
			agent.setWikidata_maxLag(8);
			agent.initConnection();
			
			SerialEntry aSerial = new SerialEntry();
			aSerial.setIssnL("2037-1136");
			aSerial.setWikidataId("Q84025820");
			// Le Monde
			// aSerial.setWikidataId("Q12461");

			aSerial.setIssns(Arrays.asList(new IssnValue[] {
					// online
					new IssnValue("2037-1136", "Medicina subacquea e iperbarica (Online)", "ta"),
					// printed
					new IssnValue("2035-8911", "Medicina subacquea e iperbarica (Testo stampato)", "ta")
			}));
			// aSerial.setTitle(aSerial.new ValueWithReference("Medicina subacquea e iperbarica", "2037-1136"));
			// aSerial.setLang(aSerial.new ValueWithReference("ita", "2037-1136"));
			aSerial.setTitle(aSerial.new ValueWithReference("mis title", "2037-1136"));
			aSerial.setLang(aSerial.new ValueWithReference("mis", "2037-1136"));
			// aSerial.setUrls(aSerial.new ValuesWithReference(Arrays.asList(new String[] { "http://portal.issn.org" }), "2037-1136"));
			// aSerial.setCountry(aSerial.new ValueWithReference("ITA", "2037-1136"));

			// agent.processSerial(aSerial);
			
			
			Statement s	=
					StatementBuilder
						.forSubjectAndProperty(Datamodel.makeWikidataItemIdValue("Q84025820"), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID))
						.withValue(Datamodel.makeMonolingualTextValue("mis test", "mis"))
						.build();
			
			agent.wbde.setMaxLag(22);
			agent.wbde.setMaxLagMaxRetries(3);
			System.out.println("Calling API...");
			agent.wbde.updateTermsStatements(
					Datamodel.makeWikidataItemIdValue("Q84025820"),
					// addLabels,
					Collections.emptyList(),
					// addDescriptions,
					Collections.emptyList(),
					// addAliases,
					Collections.emptyList(),
					// deleteAliases
					Collections.emptyList(),
					// statements to add
					Collections.singletonList(s),
					// statements to delete						
					Collections.emptyList(),
					// summary
					"mul/mis test",
					// tags
					Collections.emptyList()
			);
			System.out.println("Done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

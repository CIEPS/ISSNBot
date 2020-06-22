package org.issn.issnbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.issn.issnbot.listeners.IssnBotListener;
import org.issn.issnbot.listeners.IssnBotReportListener;
import org.issn.issnbot.model.IssnValue;
import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.model.SerialEntry.ValuesWithReference;
import org.issn.issnbot.model.WikidataIssnModel;
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
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

public class IssnBot {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	public static final String AGENT_NAME = "ISSN Bot";

	private WikibaseDataEditor wbde;
	private WikibaseDataFetcher wbdf;

	private String batchIdentifier;

	private String editSummary = null;

	private boolean dryRun = true;

	private String login;
	private String password;
	private WikidataIdProviderIfc languageIdProvider;
	private WikidataIdProviderIfc countryIdProvider;
	private WikidataLanguageCodesProvider languageCodes;
	private WikidataIdProviderIfc formatsIdProvider;
	
	private Integer wikidata_maxLag = null;
	private Integer wikidata_maxLagMaxRetries = null;
	private Double  wikidata_maxLagBackoffFactor = null;
	private Integer wikidata_maxLagFirstWaitTime = null;

	private List<IssnBotListener> listeners = new ArrayList<IssnBotListener>();

	private int limit = -1;	
	private transient int serialsProcessed = 0;
	
	private transient Map<String, EntityDocument> readAheadCache = new HashMap<>();
	
	
	
	public IssnBot(
			String login,
			String password,
			WikidataIdProviderIfc languageIdProvider,
			WikidataIdProviderIfc countryIdProvider,
			WikidataLanguageCodesProvider languageCodes,
			WikidataIdProviderIfc formatsIdProvider
	) {

		this.login = login;
		this.password = password;
		this.languageIdProvider = languageIdProvider;
		this.countryIdProvider = countryIdProvider;
		this.languageCodes = languageCodes;
		this.formatsIdProvider = formatsIdProvider;
	}
	
	public void initConnection() throws LoginFailedException {
		// Always set your User-Agent to the name of your application:
		WebResourceFetcherImpl.setUserAgent(AGENT_NAME);

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

	public void initBatch(String batchId) {
		// init a batch identifier
		// String batchId = Long.toString((new Random()).nextLong(), 16).replace("-", "");
		this.batchIdentifier = "([[:toollabs:editgroups/b/CB/" + batchId + "|details]])";
		log.info("Initialized batch identifier to : "+this.batchIdentifier);
	}


	public void processFolder(File inputFolder) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {

		// notify start
		listeners.forEach(l -> l.start(inputFolder));

		// recurse in subdirs
		for (File anInputFile : FileUtils.listFiles(inputFolder, new String[] {"csv", "xls", "tsv"}, true)) {
			SerialEntryReader reader = new CSVSerialEntryReader(new FileInputStream(anInputFile));
			this.process(reader, anInputFile.getName());
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

	public void process(SerialEntryReader reader, String id) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {

		// notify start batch
		listeners.stream().forEach(l -> l.startBatch(id));

		this.initBatch(id);
		List<SerialEntry> entries = reader.read();
		log.debug("Processing "+entries.size()+" entries in batch ID "+id);
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
		}

		// notify end batch
		listeners.stream().forEach(l -> l.stopBatch(id));
	}
	
	public void processBatch(List<SerialEntry> currentBatch) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {
		this.readAhead(currentBatch.stream().map(e -> e.getWikidataId()).collect(Collectors.toList()));
		for (SerialEntry serialEntry : currentBatch) {
			processSerial(serialEntry);
		}
		
	}

	public void processSerial(SerialEntry entry) throws IOException, MediaWikiApiErrorException {
		log.debug("Processing row "+entry.getRecordNumber()+" : "+entry.getIssnL());

		// notify serial
		listeners.stream().forEach(l -> l.startSerial(entry));

		try {

			// ItemIdValue entityId = ItemIdValue.NULL; // used when creating new items
			// ItemIdValue entityId = Datamodel.makeWikidataItemIdValue("Q84025820");
			ItemIdValue entityId = Datamodel.makeWikidataItemIdValue(entry.getWikidataId());
			
			
			// check availability of language code
			if(this.languageCodes.getWikimediaCode(entry.getLang().getValue()) == null) {
				throw new IssnBotException(entry.getIssnL()+" - Language code "+entry.getLang().getValue()+" is not an iso6392 code associated to a Wikimedia code in Wikidata, or mul or mis.");
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
			
			// create the ISSN statements
			//		entry.getIssns().stream().forEach(value -> {
			//			// the main value with the ISSN String
			//			Statement s = StatementBuilder
			//					.forSubjectAndProperty(entityId, WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID))
			//					.withValue(Datamodel.makeStringValue(value.getIssn()))
			//					// key title qualifier
			//					.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.NAMED_AS_PROPERTY_ID), Datamodel.makeStringValue(value.getKeyTitle()))
			//					// distribution format qualifier
			//					.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID), value.getDistributionFormat())
			//					.build();
			//
			//			statementsToAdd.add(s);
			//		});



			// create the labels
			// entry.getLabelsPerLanguageCode().entrySet().stream().forEach(label -> {
			//	builder.withLabel(label.getValue(), label.getKey());
			//	// .withLabel("Wikidata Toolkit test", "en")
			// });

			// ItemDocument itemDocument = builder.build();


			if(!this.dryRun ) {
				log.debug("Calling API to update "+entry.getWikidataId()+"...");
				log.debug("Added Statements "+statementsToAdd.toString()+"...");
				
				String currentEditSummary = (this.editSummary != null)?this.editSummary:AGENT_NAME+" synchronizing ISSN-L "+entry.getIssnL();
				
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
						currentEditSummary+" | "+this.batchIdentifier,
						// tags
						Collections.emptyList()
				);
				
				// to create document
				// ItemDocument newItemDocument = wbde.createItemDocument(itemDocument,"ISSN import test. See https://www.wikidata.org/wiki/Wikidata_talk:WikiProject_Periodicals#Data_donation_from_ISSN_Register_-_Feedback_welcome", Collections.emptyList());
					
//				wbde.editItemDocument(
//						newID,
//						// clear : yes / no
//						false,
//						(this.editSummary != null)?this.editSummary+" | "+this.batchIdentifier:this.batchIdentifier,
//								Collections.emptyList()
//				);
				
				log.debug("API called successfully");
			}
			
			// NOTE : there is no merging of statements here, if the ISSN already exists but with no qualifiers, new statements are created
			// the existing statements should be updated

			//		ItemDocument newItemDocument = wbde.updateStatements(
			//				entityId,
			//				// statements to add
			//				statementsToAdd,
			//				// statements to delete
			//				Collections.emptyList(),
			//				// summary
			//				(this.editSummary != null)?this.editSummary+" | "+this.batchIdentifier:this.batchIdentifier,
			//				// tags
			//				Collections.emptyList()
			//		);
			//		System.out.println(newItemDocument);

			// notify success
			listeners.stream().forEach(l -> l.successSerial(entry, wikidataSerial.getResult()));

		} catch(Exception e) {
			log.error(e.getMessage(),e);			
			// notify error
			listeners.stream().forEach(l -> l.errorSerial(entry, e.getMessage()));
		}

	}

	public String getEditSummary() {
		return editSummary;
	}

	public void setEditSummary(String editSummary) {
		this.editSummary = editSummary;
	}

	public String getBatchIdentifier() {
		return batchIdentifier;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public List<IssnBotListener> getListeners() {
		return listeners;
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
					new WikidataLanguageCodesProvider(),
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

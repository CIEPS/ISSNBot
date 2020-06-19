package org.issn.issnbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.issn.issnbot.listeners.IssnBotListener;
import org.issn.issnbot.listeners.IssnBotReportListener;
import org.issn.issnbot.model.IssnValue;
import org.issn.issnbot.model.SerialEntry;
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

	private WikidataIdProviderIfc languageIdProvider;
	private WikidataIdProviderIfc countryIdProvider;
	private WikidataLanguageCodesProvider languageCodes;
	private WikidataIdProviderIfc formatsIdProvider;

	private List<IssnBotListener> listeners = new ArrayList<IssnBotListener>();

	public IssnBot(
			String login,
			String password,
			WikidataIdProviderIfc languageIdProvider,
			WikidataIdProviderIfc countryIdProvider,
			WikidataLanguageCodesProvider languageCodes,
			WikidataIdProviderIfc formatsIdProvider
			) throws LoginFailedException {

		this.languageIdProvider = languageIdProvider;
		this.countryIdProvider = countryIdProvider;
		this.languageCodes = languageCodes;
		this.formatsIdProvider = formatsIdProvider;

		// Always set your User-Agent to the name of your application:
		WebResourceFetcherImpl.setUserAgent(AGENT_NAME);

		BasicApiConnection connection = BasicApiConnection.getWikidataApiConnection();
		// Login -- required for operations on real wikis:
		connection.login(login, password);

		this.wbde = new WikibaseDataEditor(connection, WikidataIssnModel.WIKIDATA_IRI);
		this.wbde.setEditAsBot(true);
		this.wbde.setMaxLag(6);
		log.debug("maxlag parameter is "+this.wbde.getMaxLag()+" seconds");

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
		this.batchIdentifier = "([[:toollabs:editgroups/b/ISSNBot/" + batchId + "|details]])";
		log.debug("Initialized batch identifier to : "+this.batchIdentifier);
	}


	public void processFolder(File inputFolder) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {

		// notify start
		listeners.forEach(l -> l.start(inputFolder));

		// recurse in subdirs
		for (File anInputFile : FileUtils.listFiles(inputFolder, new String[] {"csv"}, true)) {
			this.process(new CSVSerialEntryReader(new FileInputStream(anInputFile)), anInputFile.getName());
		}

		// notify stop
		listeners.forEach(l -> l.stop());
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

		for (SerialEntry serialEntry : entries) {
			processSerial(serialEntry);
		}

		// notify end batch
		listeners.stream().forEach(l -> l.stopBatch(id));
	}

	public void processSerial(SerialEntry entry) throws IOException, MediaWikiApiErrorException {
		log.debug("Processing Wikidata entry : "+entry.getWikidataId());

		// notify serial
		listeners.stream().forEach(l -> l.startSerial(entry.getIssnL(), entry.getWikidataId()));

		// ItemIdValue entityId = ItemIdValue.NULL; // used when creating new items
		// ItemIdValue entityId = Datamodel.makeWikidataItemIdValue("Q84025820");
		ItemIdValue entityId = Datamodel.makeWikidataItemIdValue(entry.getWikidataId());

		try {			
			// check availability of language code
			if(this.languageCodes.getWikimediaCode(entry.getLang().getValue()) == null) {
				throw new IssnBotException("Language code "+entry.getLang().getValue()+" is not an iso6392 code associated to a Wikimedia code in Wikidata.");
			}
			
			// Fetch the entity data from Wikidata
			ItemDocument currentItem = (ItemDocument)wbdf.getEntityDocument(entry.getWikidataId());
			log.debug(currentItem.toString());

			WikidataSerial wikidataSerial = new WikidataSerial(currentItem, languageCodes);

			List<Statement> statementsToAdd = new ArrayList<>();

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
			wikidataSerial.updatePlaceOfPublicationStatement(entry, this.countryIdProvider).ifPresent(statementsToAdd::add);
			
			// process official websites statements
			wikidataSerial.updateOfficialWebsiteStatements(entry).stream().forEach(statementsToAdd::add);
			
			// update all 4 ISSNs
			wikidataSerial.updateIssn1Statement(entry, this.formatsIdProvider).ifPresent(statementsToAdd::add);
			wikidataSerial.updateIssn2Statement(entry, this.formatsIdProvider).ifPresent(statementsToAdd::add);
			wikidataSerial.updateIssn3Statement(entry, this.formatsIdProvider).ifPresent(statementsToAdd::add);
			wikidataSerial.updateIssn4Statement(entry, this.formatsIdProvider).ifPresent(statementsToAdd::add);
			
			
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
						Collections.emptyList(),
						// summary
						(this.editSummary != null)?this.editSummary+" | "+this.batchIdentifier:this.batchIdentifier,
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
			listeners.stream().forEach(l -> l.successSerial(entry.getIssnL(), entry.getWikidataId(), wikidataSerial.getUpdateStatuses()));

		} catch(Exception e) {
			log.error(e.getMessage(),e);			
			// notify error
			listeners.stream().forEach(l -> l.errorSerial(entry.getIssnL(), entry.getWikidataId()));
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

			SerialEntry aSerial = new SerialEntry();
			aSerial.setIssnL("2037-1136");
			aSerial.setWikidataId("Q84025820");
			// Le Monde
			// aSerial.setWikidataId("Q12461");

			aSerial.setIssns(Arrays.asList(new IssnValue[] {
					// online
					new IssnValue("2037-1136", "Medicina subacquea e iperbarica (Online)", "cr"),
					// printed
					new IssnValue("2035-8911", "Medicina subacquea e iperbarica (Testo stampato)", "ta")
			}));
			aSerial.setTitle(aSerial.new ValueWithReference("Medicina subacquea e iperbarica", "2037-1136"));
			aSerial.setLang(aSerial.new ValueWithReference("ita", "2037-1136"));

			agent.processSerial(aSerial);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

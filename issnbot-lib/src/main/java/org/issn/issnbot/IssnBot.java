package org.issn.issnbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.issn.issnbot.model.IssnValue;
import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.model.WikidataIssnModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ReferenceBuilder;
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
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
	
	public IssnBot(
			String login,
			String password
	) throws LoginFailedException {
		
		// Always set your User-Agent to the name of your application:
		WebResourceFetcherImpl
				.setUserAgent(AGENT_NAME);
		
		BasicApiConnection connection = BasicApiConnection.getWikidataApiConnection();
		// Login -- required for operations on real wikis:
		connection.login(login, password);
		
		this.wbde = new WikibaseDataEditor(connection, WikidataIssnModel.WIKIDATA_IRI);
		this.wbde.setEditAsBot(true);
		log.debug("Default maxlag is "+this.wbde.getMaxLag()+" seconds");
		
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
		
		// init a batch identifier
		String batchId = Long.toString((new Random()).nextLong(), 16).replace("-", "");
		this.batchIdentifier = "([[:toollabs:editgroups/b/ISSNBot/" + batchId + "|details]])";
		log.debug("Initialized batch identifier to : "+this.batchIdentifier);
	}
	
	public void updateSerialItem(SerialEntry entry) throws IOException, MediaWikiApiErrorException {
		log.debug("Updating Wikidata entry : "+entry.getWikidataId());
		
		// ItemIdValue entityId = ItemIdValue.NULL; // used when creating new items
		// ItemIdValue entityId = Datamodel.makeWikidataItemIdValue("Q84025820");
		ItemIdValue entityId = Datamodel.makeWikidataItemIdValue(entry.getWikidataId());
		
		// Fetch the entity data from Wikidata
		EntityDocument currentItem = wbdf.getEntityDocument(entry.getWikidataId());
		System.out.println(currentItem);
		
		WikidataSerial wikidataSerial = new WikidataSerial(currentItem);
		
		List<Statement> statementsToAdd = new ArrayList<>();
		
		// create the title statement
		// TODO
		wikidataSerial.buildTitleUpdateStatement(entry).ifPresent(s -> statementsToAdd.add(s));
		
		
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
		
		System.out.println("Calling API...");
		// to create document
		// ItemDocument newItemDocument = wbde.createItemDocument(itemDocument,"ISSN import test. See https://www.wikidata.org/wiki/Wikidata_talk:WikiProject_Periodicals#Data_donation_from_ISSN_Register_-_Feedback_welcome", Collections.emptyList());
		
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
		
		System.out.println("Done");
		
		
		
		
//		ItemDocument newItemDocument = wbde.editItemDocument(
//				itemDocument,
//				false,
//				"ISSN import test. See https://www.wikidata.org/wiki/Wikidata_talk:WikiProject_Periodicals#Data_donation_from_ISSN_Register_-_Feedback_welcome",
//				Collections.emptyList()
//		);
//		ItemIdValue newItemId = newItemDocument.getEntityId();
//		
//		System.out.println("*** Successfully created a new item "
//				+ newItemId.getId()
//				+ " (see https://test.wikidata.org/w/index.php?title="
//				+ newItemId.getId() + "&oldid="
//				+ newItemDocument.getRevisionId() + " for this version)");
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

	public static void main(String...args) {		
		try {
			IssnBot agent = new IssnBot(args[0], args[1]);
			agent.setEditSummary("ISSN Bot test - See https://www.wikidata.org/wiki/Wikidata_talk:WikiProject_Periodicals#Data_donation_from_ISSN_Register_-_Feedback_welcome");
			
			SerialEntry aSerial = new SerialEntry();
			aSerial.setWikidataId("Q84025820");
			// Le Monde
			// aSerial.setWikidataId("Q12461");
			
			aSerial.setIssns(Arrays.asList(new IssnValue[] {
				// online
				new IssnValue("2037-1136", "Medicina subacquea e iperbarica (Online)", Datamodel.makeWikidataItemIdValue("Q1714118")),
				// printed
				new IssnValue("2035-8911", "Medicina subacquea e iperbarica (Testo stampato)", Datamodel.makeWikidataItemIdValue("Q1261026"))
			}));
			aSerial.setTitle(aSerial.new ValueWithReference("Medicina subacquea e iperbarica.", "2037-1136"));
			aSerial.setLang(aSerial.new ValueWithReference("it", "2037-1136"));
			
			agent.updateSerialItem(aSerial);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

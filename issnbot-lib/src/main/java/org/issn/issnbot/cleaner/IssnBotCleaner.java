package org.issn.issnbot.cleaner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.issn.issnbot.AbstractWikidataBot;
import org.issn.issnbot.IssnBot;
import org.issn.issnbot.SerialItemDocument;
import org.issn.issnbot.WikidataUpdateStatus;
import org.issn.issnbot.listeners.IssnBotListener.PropertyStatus;
import org.issn.issnbot.model.WikidataIssnModel;
import org.issn.issnbot.read.SerialEntryReadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

public class IssnBotCleaner extends AbstractWikidataBot {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	private List<IssnBotCleanerListener> listeners = new ArrayList<IssnBotCleanerListener>();
	
	private transient Map<String, EntityDocument> readAheadCache = new HashMap<>();
	
	private transient long lastEditTimeMillis = -1;
	
	private int maxNbRetries = 5;
	
	private transient int nbSerialsProcessed = 0;
	
	public IssnBotCleaner(String login, String password) {
		super(IssnBot.AGENT_NAME, login, password);
	}
	
	public void clean(EntriesToCleanProvider provider) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {

		// notify start
		listeners.stream().forEach(l -> l.start());
		
		// generate batch ID
		this.batchId = Long.toString((new Random()).nextLong(), 16).replace("-", "");
		log.info("Initialized batch identifier to : "+this.batchId);

		List<ItemIdValue> entriesToClean = provider.getEntriesToClean();
		log.debug("Cleaning "+entriesToClean.size()+" entries");
		
		
		int BATCH_SIZE = 50;
		
		List<ItemIdValue> currentBatch = new ArrayList<>();
		
		for (ItemIdValue anEntryToClean : entriesToClean) {
			currentBatch.add(anEntryToClean);
			if(currentBatch.size() == BATCH_SIZE) {
				processBatch(currentBatch);
				currentBatch = new ArrayList<>();
				log.info("Processed "+this.nbSerialsProcessed+" / "+entriesToClean.size());
			}
		}
		
		// process last part
		if(currentBatch.size() > 0) {
			processBatch(currentBatch);
			currentBatch = new ArrayList<>();
			log.info("Processed "+this.nbSerialsProcessed+" / "+entriesToClean.size());
		}

		// notify stop
		listeners.stream().forEach(l -> l.stop());
	}
	
	public void processBatch(List<ItemIdValue> currentBatch) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {
		this.readAhead(currentBatch);
		for (ItemIdValue anEntryToClean : currentBatch) {
			clean(anEntryToClean.getId(), 0);
		}
	}
	
	public void readAhead(List<ItemIdValue> entityIds) throws MediaWikiApiErrorException, IOException {
		this.readAheadCache = wbdf.getEntityDocuments(entityIds.stream().map(e -> e.getId()).collect(Collectors.toList()));
	}
	
	public void clean(String qid, int nbRetries) throws IOException, MediaWikiApiErrorException, SerialEntryReadException {
		// notify start item
		listeners.stream().forEach(l -> l.startItem(qid));
		
		ItemDocument currentItem = (ItemDocument)this.readAheadCache.get(qid);
		if(currentItem == null) {
			// if called directly (not in a batch), call API directly
			currentItem = (ItemDocument)wbdf.getEntityDocument(qid);
		}
		log.trace(currentItem.toString());

		SerialItemDocument serialItemDocument = new SerialItemDocument(currentItem);
		
		Map<Integer, PropertyStatus> statuses = new HashMap<>();
		
		List<Statement> statementsToDelete = new ArrayList<>();
		findStatementsToClean(serialItemDocument, WikidataIssnModel.TITLE_PROPERTY_ID, statuses).stream().forEach(statementsToDelete::add);
		findStatementsToClean(serialItemDocument, WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID, statuses).stream().forEach(statementsToDelete::add);
		findStatementsToClean(serialItemDocument, WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, statuses).stream().forEach(statementsToDelete::add);
		findStatementsToClean(serialItemDocument, WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID, statuses).stream().forEach(statementsToDelete::add);
		findStatementsToClean(serialItemDocument, WikidataIssnModel.ISSNL_PROPERTY_ID, statuses).stream().forEach(statementsToDelete::add);
		// don't delete ISSN property
		// can't delete labels or aliases
		
		// if nothing to do, don't do anything
		if(
				statementsToDelete.isEmpty()
		) {
			// notify untouched
			log.debug("No modification to do, don't call API.");
			// notify success item
			listeners.stream().forEach(l -> l.successItem(qid, true, statuses));
		} else {
			
			if(!this.dryRun ) {
				log.debug("Calling API to update "+qid+"...");

				String batchIdentifier = "([[:toollabs:editgroups/b/ISSNBot/" + this.batchId + "|details]])";
				String currentEditSummary = IssnBot.AGENT_NAME+" cleaned entry because an ISSN used as a reference is not present on the item anymore";
				
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
					
					ItemDocument newItemDocument = wbde.updateTermsStatements(
							Datamodel.makeWikidataItemIdValue(qid),
							// addLabels,
							Collections.emptyList(),
							// addDescriptions,
							Collections.emptyList(),
							// addAliases,
							Collections.emptyList(),
							// deleteAliases
							Collections.emptyList(),
							// statements to add
							Collections.emptyList(),
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
						listeners.stream().forEach(l -> l.errorItem(qid, e.getMessage()));
						return;
					} else {
						int updatedNbRetries = ++nbRetries;
						log.info("Got an API exception, retrying for "+updatedNbRetries+" time on a maximum of "+this.maxNbRetries+" times");
						clean(qid, updatedNbRetries);
					}
				}
				
				log.debug("API called successfully");
			
			}
			
			// notify success
			listeners.stream().forEach(l -> l.successItem(qid, false, statuses));
			
			// increment count
			this.nbSerialsProcessed++;
		}
	}
	
	public List<Statement> findStatementsToClean(SerialItemDocument serial, int propertyId, Map<Integer, PropertyStatus> statuses) {
		List<Statement> toDelete = serial.findStatementsWithIssnReference(propertyId);
		if(!toDelete.isEmpty()) {
			log.debug(serial.getEntityId().getId()+" - P"+propertyId+" : DELETE");
			statuses.put(propertyId, new PropertyStatus(WikidataUpdateStatus.PREVIOUS_VALUE_DELETED));
		} else {
			log.debug(serial.getEntityId().getId()+" - P"+propertyId+" : NONE");
			statuses.put(propertyId, new PropertyStatus(WikidataUpdateStatus.PREVIOUS_VALUE_NONE));
		}
		return toDelete;
	}

	public List<IssnBotCleanerListener> getListeners() {
		return listeners;
	}

	public void setListeners(List<IssnBotCleanerListener> listeners) {
		this.listeners = listeners;
	}
	
	public static void main(String...args) throws Exception {
		IssnBotCleaner agent = new IssnBotCleaner(
				args[0],
				args[1]
		);
		agent.setDryRun(false);

		agent.setWikidata_maxLag(20);
		agent.initConnection();
		
		agent.clean(new TitleBasedEntriesToCleanProvider());
	}
	
}

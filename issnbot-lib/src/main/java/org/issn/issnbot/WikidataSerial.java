package org.issn.issnbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.issn.issnbot.listeners.IssnBotListener.PropertyStatus;
import org.issn.issnbot.listeners.SerialResult;
import org.issn.issnbot.model.IssnValue;
import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.model.WikidataIssnModel;
import org.issn.issnbot.providers.WikidataIdProviderIfc;
import org.issn.issnbot.providers.WikidataLanguageCodesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ReferenceBuilder;
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

public class WikidataSerial {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	private SerialItemDocument serialItem;
	
	
	private SerialResult result = new SerialResult();

	private WikidataLanguageCodesProvider languageCodes;
	
	public WikidataSerial(EntityDocument entityDocument, WikidataLanguageCodesProvider languageCodes) {
		super();
		this.serialItem = new SerialItemDocument(entityDocument);
		this.languageCodes = languageCodes;
	}
	
	
	
	public Optional<Statement> updateTitle(SerialEntry serial)
	throws IssnBotException {
		String title = serial.getTitle().getValue();
		String langCode = serial.getLang().getValue();
		String referenceIssn = serial.getTitle().getReference();
		
		// availability of language code was checked before
		String wikimediaLangCode = this.languageCodes.getWikimediaCode(langCode);
		
		// find existing title statement
		Optional<Statement> existingStatement = serialItem.findTitleStatement(serial.getTitle().getValue(), wikimediaLangCode);
		
		StatementBuilder sb = null;
		if(!existingStatement.isPresent()) {		
			// value is not here, let's check if a title is present with a different value but with proper references
			List<Statement> existingTitlesWithIssnReference = this.serialItem.findStatementsWithIssnReference(WikidataIssnModel.TITLE_PROPERTY_ID);
			if(existingTitlesWithIssnReference.isEmpty()) {
				// nothing, let's create the value
				log.debug(serial.getIssnL()+" - Title : CREATE");
				notify(WikidataIssnModel.TITLE_PROPERTY_ID, WikidataUpdateStatus.CREATED);
				sb = StatementBuilder
						.forSubjectAndProperty(serialItem.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID))
						.withValue(Datamodel.makeMonolingualTextValue(title, wikimediaLangCode))
						.withReference(buildStatementReference(referenceIssn));
			} else if(existingTitlesWithIssnReference.size() == 1) {
				log.debug(serial.getIssnL()+" - Title : UPDATE");
				notify(WikidataIssnModel.TITLE_PROPERTY_ID, WikidataUpdateStatus.UPDATED);
				// if there is an existing title statement with an ISSN reference but a different value, overwrite it
				sb = copyWithoutValue(existingTitlesWithIssnReference.get(0))
						.withValue(Datamodel.makeMonolingualTextValue(title, wikimediaLangCode));
			} else {
				// multiple existing title values with an ISSN reference ? don't know what to do
				throw new IssnBotException(serial.getIssnL()+" - Found multiple titles (P"+WikidataIssnModel.TITLE_PROPERTY_ID+") with an ISSN reference on "+this.serialItem.getEntityId());
			}			
			
		} else if(!SerialItemDocument.hasIssnReference(existingStatement.get())) {
			log.debug(serial.getIssnL()+" - Title : ADD REFERENCE");
			notify(WikidataIssnModel.TITLE_PROPERTY_ID, WikidataUpdateStatus.ADDED_REFERENCE);
			
			// if existing title does not have ISSN references, add them
			sb = copy(existingStatement.get()).withReference(buildStatementReference(referenceIssn));
			
		} else if(!SerialItemDocument.hasIssnReferenceValue(existingStatement.get(), referenceIssn)) {
			log.debug(serial.getIssnL()+" - Title : UPDATE REFERENCE");
			notify(WikidataIssnModel.TITLE_PROPERTY_ID, WikidataUpdateStatus.UPDATE_REFERENCE);
			// the title with the same value exists, it has an ISSN reference, but not with the same ISSN number, so update it
			sb = copyWithoutIssnReference(existingStatement.get()).withReference(buildStatementReference(referenceIssn));
		} else {
			log.debug(serial.getIssnL()+" - Title : NOTHING");
			notify(WikidataIssnModel.TITLE_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		}
		
		return Optional.ofNullable((sb != null)?sb.build():null);
	}
	
	public Optional<Statement> updateLanguage(SerialEntry serial, WikidataIdProviderIfc langIdProvider) 
	throws IssnBotException {
		String langCode = serial.getLang().getValue();
		String referenceIssn = serial.getLang().getReference();
		
		ItemIdValue langValue = langIdProvider.getWikidataId(langCode);
		if(langValue == null) {
			throw new IssnBotException(serial.getIssnL()+" - Unable to find corresponding wikidata entry for language code '"+langCode+"'");
		}
		
		// availability of language code was checked before
		String wikimediaLangCode = this.languageCodes.getWikimediaCode(langCode);
		
		// find existing statement
		Optional<Statement> existingStatement = serialItem.findTitleStatement(serial.getTitle().getValue(), wikimediaLangCode);
		
		StatementBuilder sb = null;
		if(!existingStatement.isPresent()) {		
			// value is not here, let's check if a statement is present with a different value but with proper references
			List<Statement> existingTitlesWithIssnReference = this.serialItem.findStatementsWithIssnReference(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID);
			if(existingTitlesWithIssnReference.isEmpty()) {
				// nothing, let's create the value
				log.debug(serial.getIssnL()+" - Language : CREATE");
				notify(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID, WikidataUpdateStatus.CREATED);
				sb = StatementBuilder
						.forSubjectAndProperty(serialItem.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID))
						.withValue(langValue)
						.withReference(WikidataSerial.buildStatementReference(referenceIssn));
			} else if(existingTitlesWithIssnReference.size() == 1) {
				log.debug(serial.getIssnL()+" - Language : UPDATE");
				notify(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID, WikidataUpdateStatus.UPDATED);
				// if there is an existing statement with an ISSN reference but a different value, overwrite it
				sb = copyWithoutValue(existingTitlesWithIssnReference.get(0))
						.withValue(langValue);
			} else {
				// multiple existing values with an ISSN reference ? don't know what to do
				throw new IssnBotException(serial.getIssnL()+" - Found multiple languages (P"+WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID+") with an ISSN reference on "+this.serialItem.getEntityId());
			}			
			
		} else if(!SerialItemDocument.hasIssnReference(existingStatement.get())) {
			log.debug(serial.getIssnL()+" - Language : ADD REFERENCE");
			notify(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID, WikidataUpdateStatus.ADDED_REFERENCE);
			
			// if existing statement does not have ISSN references, add them
			sb = copy(existingStatement.get()).withReference(buildStatementReference(referenceIssn));
			
		} else if(!SerialItemDocument.hasIssnReferenceValue(existingStatement.get(), referenceIssn)) {
			log.debug(serial.getIssnL()+" - Language : UPDATE REFERENCE");
			notify(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID, WikidataUpdateStatus.UPDATE_REFERENCE);
			// the statement with the same value exists, it has an ISSN reference, but not with the same ISSN number, so update it
			sb = copyWithoutIssnReference(existingStatement.get()).withReference(buildStatementReference(referenceIssn));
		} else {
			log.debug(serial.getIssnL()+" - Language : NOTHING");
			notify(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		}
		
		return Optional.ofNullable((sb != null)?sb.build():null);
		
	}
	
	public List<Statement> updatePlaceOfPublicationStatement(SerialEntry serial, WikidataIdProviderIfc countryIdProvider) 
	throws IssnBotException {
		
		List<Statement> result = new ArrayList<Statement>();
		
		// if not set, skip
		if(serial.getCountry() == null || serial.getCountry().getValue() == null) {
			notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.EMPTY);
			return Collections.emptyList();
		}
		
		String countryCode = serial.getCountry().getValue();
		String referenceIssn = serial.getCountry().getReference();
		
		// check availability of country
		ItemIdValue countryValue = countryIdProvider.getWikidataId(countryCode);
		if(countryValue == null) {
			throw new IssnBotException(serial.getIssnL()+" - Unable to find corresponding wikidata entry for country code '"+countryCode+"'");
		}
		
		StatementBuilder sb = null;
		Optional<Statement> existingPlaceOfPublicationStatement = serialItem.findPlaceOfPublicationStatement(countryValue);
		
		if(!existingPlaceOfPublicationStatement.isPresent()) {
			log.debug(serial.getIssnL()+" - Place of Publication : CREATE");
			notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.CREATED);

			sb = StatementBuilder
					.forSubjectAndProperty(serialItem.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID))
					.withValue(countryValue)
					.withReference(WikidataSerial.buildStatementReference(referenceIssn));
			
			// if a previous value was known, we should deprecated it
			List<Statement> statementsWithReference = serialItem.findStatementsWithIssnReference(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID);
			if(!statementsWithReference.isEmpty()) {				
				// all these statements need to be deprecated
				boolean atLeastOneDeprecation = false;
				for (Statement oldStatement : statementsWithReference) {
					if(oldStatement.getRank() != StatementRank.DEPRECATED) {
						result.add(copy(oldStatement).withRank(StatementRank.DEPRECATED).build());
						atLeastOneDeprecation = true;
					}
				}
				
				if(atLeastOneDeprecation) {
					log.debug(serial.getIssnL()+" - Place of Publication previous value : DEPRECATE");
					notifyPreviousValueAction(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.PREVIOUS_VALUE_DEPRECATED);
				} else {
					log.debug(serial.getIssnL()+" - Place of Publication previous value : UNTOUCHED");
					notifyPreviousValueAction(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.PREVIOUS_VALUE_UNTOUCHED);
				}
			} else {
				log.debug(serial.getIssnL()+" - Place of Publication previous value : NONE");
				notifyPreviousValueAction(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.PREVIOUS_VALUE_NONE);
			}
			
		} else if(!SerialItemDocument.hasIssnReference(existingPlaceOfPublicationStatement.get())) {
			log.debug(serial.getIssnL()+" - Place of Publication : ADD REFERENCE");
			notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.ADDED_REFERENCE);
			
			// if existing statement does not have ISSN references, add them
			sb = copy(existingPlaceOfPublicationStatement.get()).withReference(buildStatementReference(referenceIssn));	

		} else if(!SerialItemDocument.hasIssnReferenceValue(existingPlaceOfPublicationStatement.get(), referenceIssn)) {
			log.debug(serial.getIssnL()+" - Place of Publication : UPDATE REFERENCE");
			notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.UPDATE_REFERENCE);
			// the statement with the same value exists, it has an ISSN reference, but not with the same ISSN number, so update it
			sb = copyWithoutIssnReference(existingPlaceOfPublicationStatement.get()).withReference(buildStatementReference(referenceIssn));
		} else {
			log.debug(serial.getIssnL()+" - Place of Publication : NOTHING");
			notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		}
		
		if(sb != null) {
			result.add(sb.build());
		}
		return result;
	}
	
	public List<Statement> updateOfficialWebsiteStatements(SerialEntry serial) 
	throws IssnBotException {			
		
		List<Statement> result = new ArrayList<Statement>();
		WikidataUpdateStatus status = WikidataUpdateStatus.NOTHING;
		List<WikidataUpdateStatus> statuses = new ArrayList<>();
		if(serial.getUrls() != null && serial.getUrls().getValues() != null) {
			for (String aWebsite : serial.getUrls().getValues()) {
				
				StatementBuilder sb = null;
				Optional<Statement> existingWebsiteStatement = serialItem.findOfficialWebsiteStatement(aWebsite);
				
				if(!existingWebsiteStatement.isPresent()) {
					log.debug(serial.getIssnL()+" - Official Website : CREATE");
					statuses.add(WikidataUpdateStatus.CREATED);
					status = (status == WikidataUpdateStatus.NOTHING || status == WikidataUpdateStatus.CREATED)?WikidataUpdateStatus.CREATED:WikidataUpdateStatus.MIXED;

					sb = StatementBuilder
							.forSubjectAndProperty(serialItem.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID))
							.withValue(Datamodel.makeStringValue(aWebsite))
							.withReference(WikidataSerial.buildStatementReference(serial.getUrls().getReference()));
					
				} else if(!SerialItemDocument.hasIssnReference(existingWebsiteStatement.get())) {
					log.debug(serial.getIssnL()+" - Official Website : ADD REFERENCE");
					statuses.add(WikidataUpdateStatus.ADDED_REFERENCE);
					status = (status == WikidataUpdateStatus.NOTHING || status == WikidataUpdateStatus.ADDED_REFERENCE)?WikidataUpdateStatus.ADDED_REFERENCE:WikidataUpdateStatus.MIXED;
					
					// if existing statement does not have ISSN references, add them
					sb = copy(existingWebsiteStatement.get()).withReference(WikidataSerial.buildStatementReference(serial.getUrls().getReference()));
					
				} else if(!SerialItemDocument.hasIssnReferenceValue(existingWebsiteStatement.get(), serial.getUrls().getReference())) {
					log.debug(serial.getIssnL()+" - Official Website : UPDATE REFERENCE");
					statuses.add(WikidataUpdateStatus.UPDATE_REFERENCE);
					// the statement with the same value exists, it has an ISSN reference, but not with the same ISSN number, so update it
					sb = copyWithoutIssnReference(existingWebsiteStatement.get()).withReference(buildStatementReference(serial.getUrls().getReference()));
					status = (status == WikidataUpdateStatus.NOTHING || status == WikidataUpdateStatus.UPDATE_REFERENCE)?WikidataUpdateStatus.UPDATE_REFERENCE:WikidataUpdateStatus.MIXED;
					
				} else {
					log.debug(serial.getIssnL()+" - Official Website : NOTHING");
					statuses.add(WikidataUpdateStatus.NOTHING);
				}
				
				if(sb != null) {
					result.add(sb.build());
				}
				
			}			
		}
		
		notify(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID, status, statuses.stream().map(s -> s.name()).collect(Collectors.joining(" ")));
		
		
		// deprecate every existing value with a reference that is not in the values
		boolean atLeastOneDeprecation = false;
		for (Statement anExistingStatement : this.serialItem.getOfficialWebsiteStatements()) {
			if(
					SerialItemDocument.hasIssnReference(anExistingStatement)
					&&
					!serial.hasOfficialWebsite(((StringValue)anExistingStatement.getValue()).getString())
					&&
					anExistingStatement.getRank() != StatementRank.DEPRECATED
			) {
				// deprecate it					
				result.add(copy(anExistingStatement).withRank(StatementRank.DEPRECATED).build());
				atLeastOneDeprecation = true;
			}
		}
		
		if(atLeastOneDeprecation) {
			log.debug(serial.getIssnL()+" - Official Website previous value : DEPRECATE");
			notifyPreviousValueAction(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID, WikidataUpdateStatus.PREVIOUS_VALUE_DEPRECATED);
		} else if (this.serialItem.getOfficialWebsiteStatements().size() > 0){
			log.debug(serial.getIssnL()+" - Official Website previous value : UNTOUCHED");
			notifyPreviousValueAction(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID, WikidataUpdateStatus.PREVIOUS_VALUE_UNTOUCHED);
		} else {
			log.debug(serial.getIssnL()+" - Official Website previous value : NONE");
			notifyPreviousValueAction(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID, WikidataUpdateStatus.PREVIOUS_VALUE_NONE);
		}		
		
		return result;
	}
	
	public Optional<Statement> updateIssnLStatement(SerialEntry serial) 
	throws IssnBotException {		
		
		StatementBuilder sb = null;
		Optional<Statement> existingIssnLStatement = serialItem.findIssnLStatement(serial.getIssnL());
		
		if(serialItem.getIssnLStatements().size() > 1) {
			throw new IssnBotException(serial.getIssnL()+" - Found "+serialItem.getIssnLStatements().size()+" ISSN-L values on "+serialItem.getEntityId().getId());
		}
		
		if(!existingIssnLStatement.isPresent()) {
			log.debug(serial.getIssnL()+" - IssnL : CREATE");
			notify(WikidataIssnModel.ISSNL_PROPERTY_ID, WikidataUpdateStatus.CREATED);

			sb = StatementBuilder
					.forSubjectAndProperty(serialItem.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSNL_PROPERTY_ID))
					.withValue(Datamodel.makeStringValue(serial.getIssnL()));

		} else {
			log.debug(serial.getIssnL()+" - IssnL : NOTHING");
			notify(WikidataIssnModel.ISSNL_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		}		
		
		return Optional.ofNullable((sb != null)?sb.build():null);
	}
	
	public Optional<Statement> updateIssn1Statement(SerialEntry serial, WikidataIdProviderIfc formatProvider) 
	throws IssnBotException {		
		return updateSingleIssnStatement(serial.getIssnL(), (serial.getIssns().size() > 0)?serial.getIssns().get(0):null, 1, WikidataIssnModel.FAKE_ISSN1_PROPERTY_ID, formatProvider);
	}
	
	public Optional<Statement> updateIssn2Statement(SerialEntry serial, WikidataIdProviderIfc formatProvider) 
	throws IssnBotException {		
		return updateSingleIssnStatement(serial.getIssnL(), (serial.getIssns().size() > 1)?serial.getIssns().get(1):null, 2, WikidataIssnModel.FAKE_ISSN2_PROPERTY_ID, formatProvider);
	}
	
	public Optional<Statement> updateIssn3Statement(SerialEntry serial, WikidataIdProviderIfc formatProvider) 
	throws IssnBotException {		
		return updateSingleIssnStatement(serial.getIssnL(), (serial.getIssns().size() > 2)?serial.getIssns().get(2):null, 3, WikidataIssnModel.FAKE_ISSN3_PROPERTY_ID, formatProvider);
	}
	
	public Optional<Statement> updateIssn4Statement(SerialEntry serial, WikidataIdProviderIfc formatProvider) 
	throws IssnBotException {		
		
		return updateSingleIssnStatement(serial.getIssnL(), (serial.getIssns().size() > 3)?serial.getIssns().get(3):null, 4, WikidataIssnModel.FAKE_ISSN4_PROPERTY_ID, formatProvider);
	}
	
	private Optional<Statement> updateSingleIssnStatement(String issnl, IssnValue issnValue, int issnIndex, int fakePropId, WikidataIdProviderIfc formatProvider)
	throws IssnBotException {		
		
		// if unsert, notify and return
		if(issnValue == null) {
			notify(fakePropId, WikidataUpdateStatus.EMPTY);
			return Optional.empty();
		}
		
		StatementBuilder sb = null;
		Optional<Statement> existingIssnStatement = this.serialItem.findIssnStatement(issnValue.getIssn());
		
		// check availability of format
		ItemIdValue formatValue = formatProvider.getWikidataId(issnValue.getDistributionFormat());
		if(formatValue == null) {
			throw new IssnBotException(issnl+" - Unable to find corresponding wikidata entry for distribution format '"+issnValue.getDistributionFormat()+"'");
		}
		
		
		if(!existingIssnStatement.isPresent()) {
			log.debug(issnl+" - ISSN"+issnIndex+" : CREATE");
			notify(fakePropId, WikidataUpdateStatus.CREATED);
			
			sb = StatementBuilder
				.forSubjectAndProperty(serialItem.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID))
				.withValue(Datamodel.makeStringValue(issnValue.getIssn()))
				// key title qualifier
				.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.NAMED_AS_PROPERTY_ID), Datamodel.makeStringValue(issnValue.getKeyTitle()))
				// distribution format qualifier
				.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID), formatProvider.getWikidataId(issnValue.getDistributionFormat()));
			
		} else if(
				!SerialItemDocument.hasNamedAsAndFormatQualifiers(existingIssnStatement.get())
		) {			
			log.debug(issnl+" - ISSN"+issnIndex+" : Added Qualifiers");
			notify(fakePropId, WikidataUpdateStatus.ADDED_QUALIFIER);
			
			// in case one of the 2 exists, make sure we don't copy any of the 2
			sb = copyWithoutIssnQualifiers(existingIssnStatement.get());
			
			// key title qualifier
			sb.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.NAMED_AS_PROPERTY_ID), Datamodel.makeStringValue(issnValue.getKeyTitle()));
			
			// distribution format qualifier
			sb.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID), formatProvider.getWikidataId(issnValue.getDistributionFormat()));

		} else if(
				!SerialItemDocument.hasNamedAsQualifierValue(existingIssnStatement.get(), issnValue.getKeyTitle())
				||
				!SerialItemDocument.hasFormatQualifierValue(existingIssnStatement.get(), formatValue)
		) {
			log.debug(issnl+" - ISSN"+issnIndex+" : Updated Qualifiers");
			notify(fakePropId, WikidataUpdateStatus.UPDATED_QUALIFIER);
			
			// always update both even in case one value is OK
			sb = copyWithoutIssnQualifiers(existingIssnStatement.get());
			
			// key title qualifier
			sb.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.NAMED_AS_PROPERTY_ID), Datamodel.makeStringValue(issnValue.getKeyTitle()));
			
			// distribution format qualifier
			sb.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID), formatProvider.getWikidataId(issnValue.getDistributionFormat()));
		} else {
			log.debug(issnl+" - ISSN"+issnIndex+" : NOTHING");
			notify(fakePropId, WikidataUpdateStatus.NOTHING);
		}	
		
		return Optional.ofNullable((sb != null)?sb.build():null);
	}
	
	public List<Statement> getUnknownIssnStatementsToDelete(SerialEntry serial) 
	throws IssnBotException {		
		
		List<Statement> result = new ArrayList<Statement>();
		
		boolean atLeastOneDeletion = false;		
		for (Statement anExistingStatement : this.serialItem.getIssnStatements()) {
			// not present in our data, and has the proper qualifiers
			// which indicate it is 99% sure it comes from our data
			// kill it.
			if(
					!serial.hasIssn(((StringValue)anExistingStatement.getValue()).getString())
					&&
					SerialItemDocument.hasNamedAsAndFormatQualifiers(anExistingStatement)
			) {
				// kill it					
				result.add(copy(anExistingStatement).build());
				atLeastOneDeletion = true;
			}
		}
		
		if(atLeastOneDeletion) {
			log.debug(serial.getIssnL()+" - ISSN previous value : DELETE");
			notifyPreviousValueAction(WikidataIssnModel.ISSN_PROPERTY_ID, WikidataUpdateStatus.PREVIOUS_VALUE_DELETED);
		} else if (this.serialItem.getOfficialWebsiteStatements().size() > 0){
			log.debug(serial.getIssnL()+" - ISSN previous value : UNTOUCHED");
			notifyPreviousValueAction(WikidataIssnModel.ISSN_PROPERTY_ID, WikidataUpdateStatus.PREVIOUS_VALUE_UNTOUCHED);
		} else {
			log.debug(serial.getIssnL()+" - ISSN previous value : NONE");
			notifyPreviousValueAction(WikidataIssnModel.ISSN_PROPERTY_ID, WikidataUpdateStatus.PREVIOUS_VALUE_NONE);
		}
		
		return result;
	}
	
	public List<Statement> updateCancelledIssnStatements(SerialEntry serial) 
	throws IssnBotException {				
		List<Statement> result = new ArrayList<Statement>();
		WikidataUpdateStatus status = null;
		List<WikidataUpdateStatus> statuses = new ArrayList<>();
		
		if(serial.getCancelledIssns() != null && serial.getCancelledIssns() != null) {
			for (String aCancelledIssn : serial.getCancelledIssns()) {
				
				StatementBuilder sb = null;
				Optional<Statement> existingCancelledIssnStatement = serialItem.findCancelledIssnStatement(aCancelledIssn);
				
				if(!existingCancelledIssnStatement.isPresent()) {
					log.debug(serial.getIssnL()+" - Cancelled ISSN : CREATE");
					statuses.add(WikidataUpdateStatus.CREATED);
					status = (status == null || status == WikidataUpdateStatus.CREATED)?WikidataUpdateStatus.CREATED:WikidataUpdateStatus.MIXED;

					sb = StatementBuilder
							.forSubjectAndProperty(serialItem.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID))
							.withValue(Datamodel.makeStringValue(aCancelledIssn))
							.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.REASON_FOR_DEPRECATION_PROPERTY_ID), WikidataIssnModel.INCORRECT_IDENTIFER_VALUE)
							.withRank(StatementRank.DEPRECATED);

				} else if(serialItem.findIssnStatement(aCancelledIssn).isPresent()) {
					// find plain ISSN statements that are actually cancelled, in that case deprecate them
					log.debug(serial.getIssnL()+" - Cancelled ISSN : SET DEPRECATED");
					statuses.add(WikidataUpdateStatus.SET_DEPRECATED);
					
					sb = copy(serialItem.findIssnStatement(aCancelledIssn).get())
							.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.REASON_FOR_DEPRECATION_PROPERTY_ID), WikidataIssnModel.INCORRECT_IDENTIFER_VALUE)
							.withRank(StatementRank.DEPRECATED);
				} else {
					log.debug(serial.getIssnL()+" - Cancelled ISSN : NOTHING");
					statuses.add(WikidataUpdateStatus.NOTHING);
					status = (status == null || status == WikidataUpdateStatus.NOTHING)?WikidataUpdateStatus.NOTHING:WikidataUpdateStatus.MIXED;
				}
				
				if(sb != null) {
					result.add(sb.build());
				}
				
			}
		}
		
		notifyPreviousValueAction(WikidataIssnModel.FAKE_CANCELLED_ISSN_PROPERTY, status, statuses.stream().map(s -> s.name()).collect(Collectors.joining(" ")));
		
		
		return result;
	}
	
	public List<Statement> getUnknownCancelledIssnStatementsToDelete(SerialEntry serial) 
	throws IssnBotException {		
		
		List<Statement> result = new ArrayList<Statement>();
		
		boolean atLeastOneDeletion = false;		
		for (Statement anExistingStatement : this.serialItem.getCancelledIssnStatements()) {
			// not present in our data
			// kill it.
			if(
					!serial.hasCancelledIssn(((StringValue)anExistingStatement.getValue()).getString())
			) {
				// kill it					
				result.add(copy(anExistingStatement).build());
				atLeastOneDeletion = true;
			}
		}
		
		if(atLeastOneDeletion) {
			log.debug(serial.getIssnL()+" - Cancelled ISSN previous value : DELETE");
			notifyPreviousValueAction(WikidataIssnModel.FAKE_CANCELLED_ISSN_PROPERTY, WikidataUpdateStatus.PREVIOUS_VALUE_DELETED);
		} else if (this.serialItem.getCancelledIssnStatements().size() > 0){
			log.debug(serial.getIssnL()+" - Cancelled ISSN previous value : UNTOUCHED");
			notifyPreviousValueAction(WikidataIssnModel.FAKE_CANCELLED_ISSN_PROPERTY, WikidataUpdateStatus.PREVIOUS_VALUE_UNTOUCHED);
		} else {
			log.debug(serial.getIssnL()+" - Cancelled ISSN previous value : NONE");
			notifyPreviousValueAction(WikidataIssnModel.FAKE_CANCELLED_ISSN_PROPERTY, WikidataUpdateStatus.PREVIOUS_VALUE_NONE);
		}
		
		return result;
	}
	
	
	public List<MonolingualTextValue> getLabelsToAdd(SerialEntry serial)
	throws IssnBotException {

		// availability of language code was checked before
		String wikimediaLangCode = this.languageCodes.getWikimediaCode(serial.getLang().getValue());
		
		// mis and mul are not available for labels/aliases, just skip in this case
		if(wikimediaLangCode.equals("mis") || wikimediaLangCode.equals("mul") || wikimediaLangCode.equals("und")) {
			log.debug(serial.getIssnL()+" - Label : NOTHING");
			notify(WikidataIssnModel.FAKE_LABEL_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
			return Collections.emptyList();
		}
		
		// if the ISSN title does not exist neither as a label not as an alias, add it as a label
		if(
				this.serialItem.getItemDocument().findLabel(wikimediaLangCode) == null
				&&
				this.serialItem.findAlias(serial.getTitle().getValue(), wikimediaLangCode).isEmpty()
		) {
			// no label in this language, and the label does not exists as an alias, then add it as a label
			log.debug(serial.getIssnL()+" - Label : CREATE");
			notify(WikidataIssnModel.FAKE_LABEL_PROPERTY_ID, WikidataUpdateStatus.CREATED);
			return Collections.singletonList(Datamodel.makeMonolingualTextValue(serial.getTitle().getValue(), wikimediaLangCode));
		}
		
		log.debug(serial.getIssnL()+" - Label : NOTHING");
		notify(WikidataIssnModel.FAKE_LABEL_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		
		return Collections.emptyList();
	}
	
	public List<MonolingualTextValue> getAliasesToAdd(SerialEntry serial, boolean alreadyAddedLabel) {
		
		// availability of language code was checked before
		String wikimediaLangCode = this.languageCodes.getWikimediaCode(serial.getLang().getValue());
		
		// mis and mul are not available for labels/aliases, just skip in this case
		if(wikimediaLangCode.equals("mis") || wikimediaLangCode.equals("mul") || wikimediaLangCode.equals("und")) {
			log.debug(serial.getIssnL()+" - Alias : NOTHING");
			notify(WikidataIssnModel.FAKE_ALIAS_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
			return Collections.emptyList();
		}
		
		if(
				!alreadyAddedLabel
				&&
				!serialItem.hasLabel(serial.getTitle().getValue(), wikimediaLangCode)
				&&
				serialItem.findAlias(serial.getTitle().getValue(), wikimediaLangCode).isEmpty()
		) {
			// string does not exist as a label, and does not exist as an alias
			log.debug(serial.getIssnL()+" - Alias : CREATE");
			notify(WikidataIssnModel.FAKE_ALIAS_PROPERTY_ID, WikidataUpdateStatus.CREATED);
			return Collections.singletonList(Datamodel.makeMonolingualTextValue(serial.getTitle().getValue(), wikimediaLangCode));
		}

		log.debug(serial.getIssnL()+" - Alias : NOTHING");
		notify(WikidataIssnModel.FAKE_ALIAS_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		return Collections.emptyList();
	}
	
	private void notify(int property, WikidataUpdateStatus status ) {
		this.result.getStatuses().put(property, new PropertyStatus(status));
	}
	
	private void notify(int property, WikidataUpdateStatus status, String precision) {
		this.result.getStatuses().put(property, new PropertyStatus(status, precision));
	}
	
	private void notifyPreviousValueAction(int property, WikidataUpdateStatus status ) {
		this.result.getPreviousValuesStatuses().put(property, new PropertyStatus(status));
	}
	
	private void notifyPreviousValueAction(int property, WikidataUpdateStatus status, String precision) {
		this.result.getPreviousValuesStatuses().put(property, new PropertyStatus(status, precision));
	}
	
	public SerialResult getResult() {
		return result;
	}

	private static Reference buildStatementReference(String issn) {
		Reference reference = ReferenceBuilder.newInstance()
				.withPropertyValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.STATED_IN_PROPERTY_ID), WikidataIssnModel.ISSN_REGISTER_VALUE)
				.withPropertyValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID), Datamodel.makeStringValue(issn))
				.build();
		
		return reference;
	}
	
	private static StatementBuilder copy(Statement s) {
		return StatementBuilder
				.forSubjectAndProperty(s.getSubject(), s.getMainSnak().getPropertyId())
				.withId(s.getStatementId())
				.withValue(s.getValue())
				.withQualifiers(s.getQualifiers())
				.withReferences(s.getReferences());
	}
	
	private static StatementBuilder copyWithoutIssnReference(Statement s) {
		return StatementBuilder
				.forSubjectAndProperty(s.getSubject(), s.getMainSnak().getPropertyId())
				.withId(s.getStatementId())
				.withValue(s.getValue())
				.withQualifiers(s.getQualifiers())
				.withReferences(s.getReferences().stream().filter(r -> !SerialItemDocument.isIssnReference(r)).collect(Collectors.toList()));
	}
	
	private static StatementBuilder copyWithoutValue(Statement s) {
		return StatementBuilder
				.forSubjectAndProperty(s.getSubject(), s.getMainSnak().getPropertyId())
				.withId(s.getStatementId())
				.withQualifiers(s.getQualifiers())
				.withReferences(s.getReferences());
	}
	
	private static StatementBuilder copyWithoutIssnQualifiers(Statement s) {
		return StatementBuilder
				.forSubjectAndProperty(s.getSubject(), s.getMainSnak().getPropertyId())
				.withId(s.getStatementId())
				.withValue(s.getValue())
				.withQualifiers(s.getQualifiers().stream().filter(q -> 
					!q.getProperty().getId().equals("P"+WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID)
					&&
					!q.getProperty().getId().equals("P"+WikidataIssnModel.NAMED_AS_PROPERTY_ID)
				).collect(Collectors.toList()))
				.withReferences(s.getReferences());
	}
	

	
}

package org.issn.issnbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.issn.issnbot.listeners.IssnBotListener;
import org.issn.issnbot.listeners.IssnBotListener.PropertyStatus;
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
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

public class WikidataSerial {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	private ItemDocument itemDocument;
	
	// LinkedHashMap keeps the order of insertion
	private Map<Integer, IssnBotListener.PropertyStatus> updateStatuses = new LinkedHashMap<>();

	private WikidataLanguageCodesProvider languageCodes;
	
	public WikidataSerial(EntityDocument entityDocument, WikidataLanguageCodesProvider languageCodes) {
		super();
		this.itemDocument = ((ItemDocument)entityDocument);
		this.languageCodes = languageCodes;
	}
	
	
	
	public Optional<Statement> updateTitle(SerialEntry serial)
	throws IssnBotException {
		log.debug(serial.getIssnL()+" - Update Title Statement");
		String title = serial.getTitle().getValue();
		String langCode = serial.getLang().getValue();
		String referenceIssn = serial.getTitle().getReference();
		
		// availability of language code was checked before
		String wikimediaLangCode = this.languageCodes.getWikimediaCode(langCode);
		
		StatementBuilder sb = null;
		Optional<Statement> existingTitleStatement = findTitleStatement(serial.getTitle().getValue(), wikimediaLangCode);
		
		if(!existingTitleStatement.isPresent()) {
			log.debug("Title : CREATE");
			notify(WikidataIssnModel.TITLE_PROPERTY_ID, WikidataUpdateStatus.CREATED);
			// si un même titre existe avec la même valeur (= même chaine + même langue)
			// alors la référence sera ajoutée dessus
			sb = StatementBuilder
					.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID))
					.withValue(Datamodel.makeMonolingualTextValue(title, wikimediaLangCode))
					.withReference(this.buildStatementReference(referenceIssn));
			
		} else if(!hasIssnReference(existingTitleStatement.get())) {
			log.debug("Title : ADD REFERENCE");
			notify(WikidataIssnModel.TITLE_PROPERTY_ID, WikidataUpdateStatus.ADDED_REFERENCE);
			
			// if existing title does not have ISSN references, add them
			sb = StatementBuilder
					.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID))
					.withId(existingTitleStatement.get().getStatementId())
					.withReference(this.buildStatementReference(referenceIssn));			
						
			
			// if the language code if different, silently overwrite it when adding references
//			if( !((MonolingualTextValue)existingTitleStatement.get().getValue()).getLanguageCode().equals(langCode) ) {
//				log.debug("Title : ADDED REFERENCE AND UPDATE LANGUAGE from "+(MonolingualTextValue)existingTitleStatement.get().getValue()+" to "+langCode);
//				notify(WikidataIssnModel.TITLE_PROPERTY_ID, WikidataUpdateStatus.ADDED_REFERENCE_AND_UPDATED_LANGUAGE);
//				sb.withValue(Datamodel.makeMonolingualTextValue(title, langCode));
//			}
			
		} else if(!hasIssnReferenceValue(existingTitleStatement.get(), referenceIssn)) {
			log.debug("Title : UPDATE REFERENCE - CURRENTLY NOT IMPLEMENTED");
			notify(WikidataIssnModel.TITLE_PROPERTY_ID, WikidataUpdateStatus.UPDATE_REFERENCE_NOT_IMPLEMENTED);
		} else {
			log.debug("Title : NOTHING");
			notify(WikidataIssnModel.TITLE_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		}
		
		return Optional.ofNullable((sb != null)?sb.build():null);
	}
	
	public Optional<Statement> updateLanguage(SerialEntry serial, WikidataIdProviderIfc langIdProvider) 
	throws IssnBotException {
		log.debug(serial.getIssnL()+" - Update Language Statement");
		String langCode = serial.getLang().getValue();
		String referenceIssn = serial.getLang().getReference();
		
		ItemIdValue langValue = langIdProvider.getWikidataId(langCode);
		if(langValue == null) {
			throw new IssnBotException("Unable to find corresponding wikidata entry for language code '"+langCode+"'");
		}
		
		StatementBuilder sb = null;
		Optional<Statement> existingLanguageStatement = findLanguageStatement(langValue);
		
		if(!existingLanguageStatement.isPresent()) {
			log.debug("Language : CREATE");
			notify(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID, WikidataUpdateStatus.CREATED);

			sb = StatementBuilder
					.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID))
					.withValue(langValue)
					.withReference(this.buildStatementReference(referenceIssn));
		} else if(!hasIssnReference(existingLanguageStatement.get())) {
			log.debug("Language : ADD REFERENCE");
			notify(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID, WikidataUpdateStatus.ADDED_REFERENCE);
			
			// if existing statement does not have ISSN references, add them
			sb = StatementBuilder
					.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID))
					.withId(existingLanguageStatement.get().getStatementId())
					.withReference(this.buildStatementReference(referenceIssn));	

		} else if(!hasIssnReferenceValue(existingLanguageStatement.get(), referenceIssn)) {
			log.debug("Language : UPDATE REFERENCE - CURRENTLY NOT IMPLEMENTED");
			notify(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID, WikidataUpdateStatus.UPDATE_REFERENCE_NOT_IMPLEMENTED);
		} else {
			log.debug("Language : NOTHING");
			notify(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		}
		
		
		return Optional.ofNullable((sb != null)?sb.build():null);
	}
	
	public Optional<Statement> updatePlaceOfPublicationStatement(SerialEntry serial, WikidataIdProviderIfc countryIdProvider) 
	throws IssnBotException {
		// if not set, skip
		if(serial.getCountry() == null || serial.getCountry().getValue() == null) {
			log.debug("Place of Publication : EMPTY");
			notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.EMPTY);
			return Optional.empty();
		}
		
		log.debug(serial.getIssnL()+" - Build Place of Publication Statement");
		String countryCode = serial.getCountry().getValue();
		String referenceIssn = serial.getCountry().getReference();
		
		// check availability of country
		ItemIdValue countryValue = countryIdProvider.getWikidataId(countryCode);
		if(countryValue == null) {
			throw new IssnBotException("Unable to find corresponding wikidata entry for country code '"+countryCode+"'");
		}
		
		StatementBuilder sb = null;
		Optional<Statement> existingPlaceOfPublicationStatement = findPlaceOfPublicationStatement(countryValue);
		
		if(!existingPlaceOfPublicationStatement.isPresent()) {
			log.debug("Place of Publication : CREATE");
			notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.CREATED);

			sb = StatementBuilder
					.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID))
					.withValue(countryValue)
					.withReference(this.buildStatementReference(referenceIssn));
			
			// if a previous value was known, we should deprecated it
			List<Statement> statementsWithReference = findStatementsWithReference(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID);
			if(!statementsWithReference.isEmpty()) {
				log.debug("Place of Publication : CREATE AND DEPRECATE OLDER VALUES");
				notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.CREATED_AND_DEPRECATE_OLDER_VALUES);
				
				// all these statements need to be deprecated and added an end time qualifier
				// TODO
			}
			
		} else if(!hasIssnReference(existingPlaceOfPublicationStatement.get())) {
			log.debug("Place of Publication : ADD REFERENCE");
			notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.ADDED_REFERENCE);
			
			// if existing statement does not have ISSN references, add them
			sb = StatementBuilder
					.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID))
					.withId(existingPlaceOfPublicationStatement.get().getStatementId())
					.withReference(this.buildStatementReference(referenceIssn));	

		} else if(!hasIssnReferenceValue(existingPlaceOfPublicationStatement.get(), referenceIssn)) {
			log.debug("Place of Publication : UPDATE REFERENCE - CURRENTLY NOT IMPLEMENTED");
			notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.UPDATE_REFERENCE_NOT_IMPLEMENTED);
		} else {
			log.debug("Place of Publication : NOTHING");
			notify(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		}
		
		
		return Optional.ofNullable((sb != null)?sb.build():null);
	}
	
	public List<Statement> updateOfficialWebsiteStatements(SerialEntry serial) 
	throws IssnBotException {		
		log.debug(serial.getIssnL()+" - Build Official Websites Statement");		
		
		List<Statement> result = new ArrayList<Statement>();
		WikidataUpdateStatus status = WikidataUpdateStatus.NOTHING;
		List<WikidataUpdateStatus> statuses = new ArrayList<>();
		if(serial.getUrls() != null && serial.getUrls().getValues() != null) {
			for (String aWebsite : serial.getUrls().getValues()) {
				
				StatementBuilder sb = null;
				Optional<Statement> existingWebsiteStatement = findOfficialWebsiteStatement(aWebsite);
				
				if(!existingWebsiteStatement.isPresent()) {
					log.debug("Official Website : CREATE");
					statuses.add(WikidataUpdateStatus.CREATED);
					status = (status == WikidataUpdateStatus.NOTHING || status == WikidataUpdateStatus.CREATED)?WikidataUpdateStatus.CREATED:WikidataUpdateStatus.MIXED;

					sb = StatementBuilder
							.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID))
							.withValue(Datamodel.makeStringValue(aWebsite))
							.withReference(this.buildStatementReference(serial.getUrls().getReference()));
					
					// if a previous value was known, we should deprecated it
					List<Statement> statementsWithReference = findStatementsWithReference(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID);
					if(!statementsWithReference.isEmpty()) {
						log.debug("Official Website : CREATE AND DEPRECATE OLDER VALUES");
						statuses.add(WikidataUpdateStatus.CREATED_AND_DEPRECATE_OLDER_VALUES);
						status = (status == WikidataUpdateStatus.NOTHING || status == WikidataUpdateStatus.CREATED_AND_DEPRECATE_OLDER_VALUES)?WikidataUpdateStatus.CREATED_AND_DEPRECATE_OLDER_VALUES:WikidataUpdateStatus.MIXED;
						
						// all these statements need to be deprecated and added an end time qualifier
						// TODO
					}
					
				} else if(!hasIssnReference(existingWebsiteStatement.get())) {
					log.debug("Official Website : ADD REFERENCE");
					statuses.add(WikidataUpdateStatus.ADDED_REFERENCE);
					status = (status == WikidataUpdateStatus.NOTHING || status == WikidataUpdateStatus.ADDED_REFERENCE)?WikidataUpdateStatus.ADDED_REFERENCE:WikidataUpdateStatus.MIXED;
					
					// if existing statement does not have ISSN references, add them
					sb = StatementBuilder
							.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID))
							.withId(existingWebsiteStatement.get().getStatementId())
							.withReference(this.buildStatementReference(serial.getUrls().getReference()));	
				} else if(!hasIssnReferenceValue(existingWebsiteStatement.get(), serial.getUrls().getReference())) {
					log.debug("Official Website : UPDATE REFERENCE - CURRENTLY NOT IMPLEMENTED");
					statuses.add(WikidataUpdateStatus.UPDATE_REFERENCE_NOT_IMPLEMENTED);
					status = (status == WikidataUpdateStatus.NOTHING || status == WikidataUpdateStatus.UPDATE_REFERENCE_NOT_IMPLEMENTED)?WikidataUpdateStatus.UPDATE_REFERENCE_NOT_IMPLEMENTED:WikidataUpdateStatus.MIXED;
				} else {
					log.debug("Official Website : NOTHING");
					statuses.add(WikidataUpdateStatus.NOTHING);
				}
				
				if(sb != null) {
					result.add(sb.build());
				}
				
			}			
		}
		
		notify(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID, status, statuses.stream().map(s -> s.name()).collect(Collectors.joining(" ")));
		return result;
	}
	
	public Optional<Statement> updateIssnLStatement(SerialEntry serial) 
	throws IssnBotException {		
		log.debug(serial.getIssnL()+" - Build ISSN-L Statement");
		
		StatementBuilder sb = null;
		Optional<Statement> existingIssnLStatement = findIssnLStatement(serial.getIssnL());
		
		if(!existingIssnLStatement.isPresent()) {
			log.debug("IssnL : CREATE");
			notify(WikidataIssnModel.ISSNL_PROPERTY_ID, WikidataUpdateStatus.CREATED);

			sb = StatementBuilder
					.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSNL_PROPERTY_ID))
					.withValue(Datamodel.makeStringValue(serial.getIssnL()));

		} else {
			log.debug("IssnL : NOTHING");
			notify(WikidataIssnModel.ISSNL_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		}		
		
		return Optional.ofNullable((sb != null)?sb.build():null);
	}
	
	public Optional<Statement> updateIssn1Statement(SerialEntry serial, WikidataIdProviderIfc formatProvider) 
	throws IssnBotException {		
		log.debug(serial.getIssnL()+" - Build ISSN1 Statement");
		if(serial.getIssns().size() > 0) {
			return updateSingleIssnStatement(serial.getIssns().get(0), 1, WikidataIssnModel.FAKE_ISSN1_PROPERTY_ID, formatProvider);
		} else {
			return Optional.empty();
		}
	}
	
	public Optional<Statement> updateIssn2Statement(SerialEntry serial, WikidataIdProviderIfc formatProvider) 
	throws IssnBotException {		
		log.debug(serial.getIssnL()+" - Build ISSN2 Statement");
		if(serial.getIssns().size() > 1) {
			return updateSingleIssnStatement(serial.getIssns().get(1), 2, WikidataIssnModel.FAKE_ISSN2_PROPERTY_ID, formatProvider);
		} else {
			return Optional.empty();
		}
	}
	
	public Optional<Statement> updateIssn3Statement(SerialEntry serial, WikidataIdProviderIfc formatProvider) 
	throws IssnBotException {		
		log.debug(serial.getIssnL()+" - Build ISSN3 Statement");
		if(serial.getIssns().size() > 2) {
			return updateSingleIssnStatement(serial.getIssns().get(2), 3, WikidataIssnModel.FAKE_ISSN3_PROPERTY_ID, formatProvider);
		} else {
			return Optional.empty();
		}
	}
	
	public Optional<Statement> updateIssn4Statement(SerialEntry serial, WikidataIdProviderIfc formatProvider) 
	throws IssnBotException {		
		log.debug(serial.getIssnL()+" - Build ISSN4 Statement");
		if(serial.getIssns().size() > 3) {
			return updateSingleIssnStatement(serial.getIssns().get(3), 4, WikidataIssnModel.FAKE_ISSN4_PROPERTY_ID, formatProvider);
		} else {
			return Optional.empty();
		}
	}
	
	private Optional<Statement> updateSingleIssnStatement(IssnValue issnValue, int issnIndex, int fakePropId, WikidataIdProviderIfc formatProvider)
	throws IssnBotException {		
		StatementBuilder sb = null;
		Optional<Statement> existingIssnStatement = findIssnStatement(issnValue.getIssn());
		
		// check availability of format
		ItemIdValue formatValue = formatProvider.getWikidataId(issnValue.getDistributionFormat());
		if(formatValue == null) {
			throw new IssnBotException("Unable to find corresponding wikidata entry for distribution format '"+issnValue.getDistributionFormat()+"'");
		}
		
		
		if(!existingIssnStatement.isPresent()) {
			log.debug("ISSN"+issnIndex+" : CREATE");
			notify(fakePropId, WikidataUpdateStatus.CREATED);

			sb = StatementBuilder
				.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID))
				.withValue(Datamodel.makeStringValue(issnValue.getIssn()))
				// key title qualifier
				.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.NAMED_AS_PROPERTY_ID), Datamodel.makeStringValue(issnValue.getKeyTitle()))
				// distribution format qualifier
				.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID), formatProvider.getWikidataId(issnValue.getDistributionFormat()));
			
		} else {
			
			if(
					!hasQualifier(existingIssnStatement.get(), WikidataIssnModel.NAMED_AS_PROPERTY_ID)
					|| 
					!hasQualifier(existingIssnStatement.get(), WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID)
			) {
				
				sb = StatementBuilder
						.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID))
						.withId(existingIssnStatement.get().getStatementId())
						// as the matching is done on value + qualifiers, we need to specify the value again
						.withValue(Datamodel.makeStringValue(issnValue.getIssn()));
				
				log.debug("ISSN"+issnIndex+" : Update Qualifiers");
				notify(fakePropId, WikidataUpdateStatus.ADDED_QUALIFIER);
				
				if(
						!hasQualifier(existingIssnStatement.get(), WikidataIssnModel.NAMED_AS_PROPERTY_ID)
				) {
					// key title qualifier
					sb.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.NAMED_AS_PROPERTY_ID), Datamodel.makeStringValue(issnValue.getKeyTitle()));
				}
				// TODO : already has a qualifier but with a different value ?
				
				if(
						!hasQualifier(existingIssnStatement.get(), WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID)
				) {
					// distribution format qualifier
					sb.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID), formatProvider.getWikidataId(issnValue.getDistributionFormat()));
				}
				// TODO : already has a qualifier but with a different value ?
			} else {
				log.debug("ISSN"+issnIndex+" : Nothing");
				notify(fakePropId, WikidataUpdateStatus.NOTHING);
			}			
		}	
		
		return Optional.ofNullable((sb != null)?sb.build():null);
	}
	
	
	public List<MonolingualTextValue> getLabelsToAdd(SerialEntry serial)
	throws IssnBotException {

		// availability of language code was checked before
		String wikimediaLangCode = this.languageCodes.getWikimediaCode(serial.getLang().getValue());
		
		// if the ISSN title does not exist neither as a label not as an alias, add it as a label
		if(
				this.itemDocument.findLabel(wikimediaLangCode) == null
				&&
				findAlias(serial.getTitle().getValue(), wikimediaLangCode).isEmpty()
		) {
			// no label in this language, and the label does not exists as an alias, then add it as a label
			log.debug("Label : CREATED");
			notify(WikidataIssnModel.FAKE_LABEL_PROPERTY_ID, WikidataUpdateStatus.CREATED);
			return Collections.singletonList(Datamodel.makeMonolingualTextValue(serial.getTitle().getValue(), wikimediaLangCode));
		}
		
		log.debug("Label : NOTHING");
		notify(WikidataIssnModel.FAKE_LABEL_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		
		return Collections.emptyList();
	}
	
	public List<MonolingualTextValue> getAliasesToAdd(SerialEntry serial, boolean alreadyAddedLabel) {
		
		// availability of language code was checked before
		String wikimediaLangCode = this.languageCodes.getWikimediaCode(serial.getLang().getValue());
		
		if(
				!alreadyAddedLabel
				&&
				!hasLabel(serial.getTitle().getValue(), wikimediaLangCode)
				&&
				findAlias(serial.getTitle().getValue(), wikimediaLangCode).isEmpty()
		) {
			// string does not exist as a label, and does not exist as an alias
			log.debug("Alias : CREATED");
			notify(WikidataIssnModel.FAKE_ALIAS_PROPERTY_ID, WikidataUpdateStatus.CREATED);
			return Collections.singletonList(Datamodel.makeMonolingualTextValue(serial.getTitle().getValue(), wikimediaLangCode));
		}

		log.debug("Alias : NOTHING");
		notify(WikidataIssnModel.FAKE_ALIAS_PROPERTY_ID, WikidataUpdateStatus.NOTHING);
		return Collections.emptyList();
	}
	
	private boolean hasLabel(String title, String lang) {
		
		return (
				this.itemDocument.getLabels().get(lang) != null
				&&
				this.itemDocument.getLabels().get(lang).getText().equals(title)
		);
	}
	
	private Optional<MonolingualTextValue> findAlias(String title, String lang) {
		if(this.itemDocument.getAliases().get(lang) != null) {
			for (MonolingualTextValue aValue : this.itemDocument.getAliases().get(lang)) {
				if(aValue.equals(title)) {
					return Optional.of(aValue);
				}
			}
		}
		return Optional.empty();
	}

	private Optional<Statement> findTitleStatement(String title, String langCode) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on string value and language code only, not on qualifiers
		return statements.stream().filter(s -> ((MonolingualTextValue)s.getValue()).getText().equals(title) && ((MonolingualTextValue)s.getValue()).getLanguageCode().equals(langCode)).findFirst();
	}
	
	private Optional<Statement> findLanguageStatement(ItemIdValue lang) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		return statements.stream().filter(s -> ((ItemIdValue)s.getValue()).getId().equals(lang.getId())).findFirst();
	}
	
	private Optional<Statement> findPlaceOfPublicationStatement(ItemIdValue country) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		return statements.stream().filter(s -> ((ItemIdValue)s.getValue()).getId().equals(country.getId())).findFirst();
	}
	
	private Optional<Statement> findIssnLStatement(String issnL) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSNL_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on the ISSN-L only, not on qualifiers
		return statements.stream().filter(s -> ((StringValue)s.getValue()).getString().equals(issnL)).findFirst();
	}
	
	private Optional<Statement> findOfficialWebsiteStatement(String website) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// assume this is a string value
		return statements.stream().filter(s -> ((StringValue)s.getValue()).getString().equals(website)).findFirst();
	}
	
	private Optional<Statement> findIssnStatement(String issn) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on the ISSN only, not on qualifiers
		return statements.stream().filter(s -> ((StringValue)s.getValue()).getString().equals(issn)).findFirst();
	}
	
	private List<Statement> findStatementsWithReference(int propertyId) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(propertyId));
		if(statements == null) {
			return Collections.emptyList();
		}
		return statements.stream().filter(s -> hasIssnReference(s)).collect(Collectors.toList());
	}
	
	private void notify(int property, WikidataUpdateStatus status ) {
		this.updateStatuses.put(property, new PropertyStatus(status));
	}
	
	private void notify(int property, WikidataUpdateStatus status, String precision) {
		this.updateStatuses.put(property, new PropertyStatus(status, precision));
	}
	
	public Map<Integer, PropertyStatus> getUpdateStatuses() {
		return updateStatuses;
	}
	
//	public List<Statement> getIssnStatements() {
//		return this.serial.getIssns().stream().map(issn -> {
//			
//			
//			// the main value with the ISSN String
//			Statement s = StatementBuilder
//					.forSubjectAndProperty(entityId, WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID))
//					.withValue(Datamodel.makeStringValue(issn.getIssn()))
//					// key title qualifier
//					.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.NAMED_AS_PROPERTY_ID), Datamodel.makeStringValue(issn.getKeyTitle()))
//					// distribution format qualifier
//					.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID), issn.getDistributionFormat())
//					.build();
//
//			return s;
//		}).collect(Collectors.toList());
//	}
	
	public ItemDocument getItemDocument() {
		return itemDocument;
	}

	private Reference buildStatementReference(String issn) {
		Reference reference = ReferenceBuilder.newInstance()
				.withPropertyValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.STATED_IN_PROPERTY_ID), WikidataIssnModel.ISSN_REGISTER_VALUE)
				.withPropertyValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID), Datamodel.makeStringValue(issn))
				.build();
		
		return reference;
	}

	private boolean hasIssnReference(Statement s) {
		return s.getReferences().stream().anyMatch(r -> {
			return 
					r.getSnakGroups().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+WikidataIssnModel.STATED_IN_PROPERTY_ID))
					&&
					r.getSnakGroups().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+WikidataIssnModel.ISSN_PROPERTY_ID))
			;
		});
	}
	
	private boolean hasQualifier(Statement s, int qualifierProperty) {
		return s.getQualifiers().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+qualifierProperty));
	}
	
	private boolean hasIssnReferenceValue(Statement s, String issn) {
		return s.getReferences().stream().anyMatch(r -> {
			return 
					r.getSnakGroups().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+WikidataIssnModel.STATED_IN_PROPERTY_ID))
					&&
					r.getSnakGroups().stream().anyMatch(sg -> {
						return
						sg.getProperty().getId().equals("P"+WikidataIssnModel.ISSN_PROPERTY_ID)
						&&
						sg.getSnaks().stream().anyMatch(snak -> 
							(snak instanceof ValueSnak)
							&&
							(((ValueSnak)snak).getValue() instanceof StringValue)
							&&
							((StringValue)((ValueSnak)snak).getValue()).getString().equals(issn)
						);
					})
			;
		});
	}
	
}

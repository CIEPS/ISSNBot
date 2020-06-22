package org.issn.issnbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.issn.issnbot.model.WikidataIssnModel;
import org.issn.issnbot.providers.WikidataLanguageCodesProvider;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

public class SerialItemDocument {

	private ItemDocument itemDocument;
	
	public SerialItemDocument(EntityDocument entityDocument) {
		this.itemDocument = ((ItemDocument)entityDocument);
	}
	
	public boolean hasLabel(String title, String lang) {
		
		return (
				this.itemDocument.getLabels().get(lang) != null
				&&
				this.itemDocument.getLabels().get(lang).getText().equals(title)
		);
	}
	
	public Optional<MonolingualTextValue> findAlias(String title, String lang) {
		if(this.itemDocument.getAliases().get(lang) != null) {
			for (MonolingualTextValue aValue : this.itemDocument.getAliases().get(lang)) {
				if(aValue.equals(title)) {
					return Optional.of(aValue);
				}
			}
		}
		return Optional.empty();
	}

	public Optional<Statement> findTitleStatement(String title, String langCode) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on string value and language code only, not on qualifiers
		// and we compare ignoring case
		return statements.stream().filter(
				s -> 
					((MonolingualTextValue)s.getValue()).getText().equalsIgnoreCase(title)
					&&
					((MonolingualTextValue)s.getValue()).getLanguageCode().equals(langCode)
		).findFirst();
	}
	
	public Optional<Statement> findLanguageStatement(ItemIdValue lang) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		return statements.stream().filter(s -> ((ItemIdValue)s.getValue()).getId().equals(lang.getId())).findFirst();
	}
	
	public Optional<Statement> findPlaceOfPublicationStatement(ItemIdValue country) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		return statements.stream().filter(s -> ((ItemIdValue)s.getValue()).getId().equals(country.getId())).findFirst();
	}
	
	public Optional<Statement> findIssnLStatement(String issnL) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSNL_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on the ISSN-L only, not on qualifiers
		return statements.stream().filter(s -> ((StringValue)s.getValue()).getString().equals(issnL)).findFirst();
	}
	
	public List<Statement> getIssnLStatements() {
		StatementGroup existingStatements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSNL_PROPERTY_ID));
		if(existingStatements != null) {
			return existingStatements.getStatements();
		} else {
			return new ArrayList<Statement>();
		}
	}
	
	public Optional<Statement> findOfficialWebsiteStatement(String website) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// assume this is a string value
		return statements.stream().filter(s -> ((StringValue)s.getValue()).getString().equals(website)).findFirst();
	}
	
	public List<Statement> getOfficialWebsiteStatements() {
		StatementGroup existingStatements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID));
		if(existingStatements != null) {
			return existingStatements.getStatements();
		} else {
			return new ArrayList<Statement>();
		}
	}
	
	public Optional<Statement> findIssnStatement(String issn) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on the ISSN only, not on qualifiers
		return statements.stream().filter(s -> ((StringValue)s.getValue()).getString().equals(issn)).findFirst();
	}
	
	public List<Statement> getIssnStatements() {
		StatementGroup existingStatements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID));
		if(existingStatements != null) {
			return existingStatements.getStatements();
		} else {
			return new ArrayList<Statement>();
		}
	}
	
	public Optional<Statement> findCancelledIssnStatement(String cancelledIssn) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on the ISSN value, and the rank
		return statements.stream().filter(s -> s.getRank() == StatementRank.DEPRECATED && ((StringValue)s.getValue()).getString().equals(cancelledIssn)).findFirst();
	}
	
	public List<Statement> getCancelledIssnStatements() {
		StatementGroup existingStatements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID));
		if(existingStatements != null) {
			// return ISSN statements marked as deprecated with proper qualifier
			return existingStatements.getStatements().stream().filter(s -> 
				s.getRank()== StatementRank.DEPRECATED
				&&
				hasReasonForDeprecationQualifierValue(s, WikidataIssnModel.INCORRECT_IDENTIFER_VALUE)
			).collect(Collectors.toList());
		} else {
			return new ArrayList<Statement>();
		}
	}
	
	public List<Statement> findStatementsWithIssnReference(int propertyId) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(propertyId));
		if(statements == null) {
			return Collections.emptyList();
		}
		return statements.stream().filter(s -> hasIssnReference(s)).collect(Collectors.toList());
	}
	
	public static boolean hasIssnReference(Statement s) {
		return s.getReferences().stream().anyMatch(r -> {
			return 
					r.getSnakGroups().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+WikidataIssnModel.STATED_IN_PROPERTY_ID))
					&&
					r.getSnakGroups().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+WikidataIssnModel.ISSN_PROPERTY_ID))
			;
		});
	}
	
	public static boolean isIssnReference(Reference r) {
		return
				r.getSnakGroups().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+WikidataIssnModel.STATED_IN_PROPERTY_ID))
				&&
				r.getSnakGroups().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+WikidataIssnModel.ISSN_PROPERTY_ID))
				;
	}

	
	public static boolean hasNamedAsAndFormatQualifiers(Statement s) {
		return				
		SerialItemDocument.hasQualifier(s, WikidataIssnModel.NAMED_AS_PROPERTY_ID)
		&&
		SerialItemDocument.hasQualifier(s, WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID)
		;
	}
	
	
	public static boolean hasQualifier(Statement s, int qualifierProperty) {
		return s.getQualifiers().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+qualifierProperty));
	}
	
	public static boolean hasNamedAsQualifierValue(Statement s, String keyTitle) {
		return s.getQualifiers().stream().anyMatch(sg -> 
			sg.getProperty().getId().equals("P"+WikidataIssnModel.NAMED_AS_PROPERTY_ID)
			&&
			sg.stream().anyMatch(snak -> 
				(snak instanceof ValueSnak)
				&&
				(((ValueSnak)snak).getValue() instanceof StringValue)
				&&
				((StringValue)((ValueSnak)snak).getValue()).getString().equals(keyTitle)
			)
		);
	}
	
	public static boolean hasFormatQualifierValue(Statement s, ItemIdValue value) {
		return s.getQualifiers().stream().anyMatch(sg -> 
			sg.getProperty().getId().equals("P"+WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID)
			&&
			sg.stream().anyMatch(snak -> 
				(snak instanceof ValueSnak)
				&&
				(((ValueSnak)snak).getValue() instanceof ItemIdValue)
				&&
				((ItemIdValue)((ValueSnak)snak).getValue()).getId().equals(value.getId())
			)
		);
	}
	
	public static boolean hasReasonForDeprecationQualifierValue(Statement s, ItemIdValue value) {
		return s.getQualifiers().stream().anyMatch(sg -> 
			sg.getProperty().getId().equals("P"+WikidataIssnModel.REASON_FOR_DEPRECATION_PROPERTY_ID)
			&&
			sg.stream().anyMatch(snak -> 
				(snak instanceof ValueSnak)
				&&
				(((ValueSnak)snak).getValue() instanceof ItemIdValue)
				&&
				((ItemIdValue)((ValueSnak)snak).getValue()).getId().equals(value.getId())
			)
		);
	}
	
	public static boolean hasIssnReferenceValue(Statement s, String issn) {
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
	
	public ItemIdValue getEntityId() {
		return itemDocument.getEntityId();
	}

	public ItemDocument getItemDocument() {
		return itemDocument;
	}
	
	
}

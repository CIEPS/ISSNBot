package org.issn.issnbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.issn.issnbot.model.WikidataIssnModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * A wrapper around a Wikidata ItemDocument that provides convenience methods to access/read
 * the necessary properties for the bot.
 * 
 * @author thomas
 *
 */
public class SerialItemDocument {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	private ItemDocument itemDocument;
	
	public SerialItemDocument(EntityDocument entityDocument) {
		this.itemDocument = ((ItemDocument)entityDocument);
	}
	
	/**
	 * Checks if the ItemDocument has the given label in the given language code (case-insensitive)
	 * 
	 * @param title
	 * @param lang
	 * @return
	 */
	public boolean hasLabel(String title, String lang) {
		
		return (
				this.itemDocument.getLabels().get(lang) != null
				&&
				this.itemDocument.getLabels().get(lang).getText().equalsIgnoreCase(title)
		);
	}
	
	/**
	 * Searches for the alias with the given value (case-insensitive) in the given language.
	 * @param title
	 * @param lang
	 * @return
	 */
	public Optional<MonolingualTextValue> findAlias(String title, String lang) {
		if(this.itemDocument.getAliases().get(lang) != null) {
			for (MonolingualTextValue aValue : this.itemDocument.getAliases().get(lang)) {
				if(aValue.getText().equalsIgnoreCase(title)) {
					return Optional.of(aValue);
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Searches a title statement with the given value (case-insensitive) in the given language code
	 * @param title
	 * @param langCode
	 * @return
	 */
	public Optional<Statement> findTitleStatement(String title, String langCode) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on string value and language code only, not on qualifiers
		// and we compare ignoring case
		return statements.stream().filter(
				s -> {
					log.debug("Comparing "+((MonolingualTextValue)s.getValue()).getText()+"@"+((MonolingualTextValue)s.getValue()).getLanguageCode()+" vs. "+title+"@"+langCode);
					return
					((MonolingualTextValue)s.getValue()).getText().equalsIgnoreCase(title)
					&&
					((MonolingualTextValue)s.getValue()).getLanguageCode().equals(langCode)
					;
				}
		).findFirst();
	}
	
	/**
	 * Searches for a language statement with the given value
	 * @param lang
	 * @return
	 */
	public Optional<Statement> findLanguageStatement(ItemIdValue lang) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		return statements.stream().filter(s -> ((ItemIdValue)s.getValue()).getId().equals(lang.getId())).findFirst();
	}
	
	/**
	 * Searches for a place of publication statement with the given value
	 * @param country
	 * @return
	 */
	public Optional<Statement> findPlaceOfPublicationStatement(ItemIdValue country) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		return statements.stream().filter(s -> ((ItemIdValue)s.getValue()).getId().equals(country.getId())).findFirst();
	}
	
	/**
	 * Searches for an ISSN-L statement with the given value
	 * @param issnL
	 * @return
	 */
	public Optional<Statement> findIssnLStatement(String issnL) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSNL_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on the ISSN-L only, not on qualifiers
		return statements.stream().filter(s -> ((StringValue)s.getValue()).getString().equals(issnL)).findFirst();
	}
	
	/**
	 * Searches for an official website statement with the given value, ignoring a potential final '/' character.
	 * @param website
	 * @return
	 */
	public Optional<Statement> findOfficialWebsiteStatement(String website) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// careful sometimes we can have a NoValue value
		// also check the same value without final "/"
		return statements.stream().filter(s -> {
			return	
				(s.getValue() instanceof StringValue)
				&&
				(
						((StringValue)s.getValue()).getString().equals(website)
						||
						(
								((StringValue)s.getValue()).getString().endsWith("/")
								&&
								((StringValue)s.getValue()).getString().substring(0, ((StringValue)s.getValue()).getString().length()-1).equals(website)
						)
						||
						(
								website.endsWith("/")
								&&
								((StringValue)s.getValue()).getString().equals(website.substring(0, website.length()-1))
						)
				);
		}
		).findFirst();
	}
	
	/**
	 * Searches for an ISSN statement with the given value
	 * @param issn
	 * @return
	 */
	public Optional<Statement> findIssnStatement(String issn) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on the ISSN only, not on qualifiers
		return statements.stream().filter(s -> ((StringValue)s.getValue()).getString().equals(issn)).findFirst();
	}
	
	/**
	 * Searches for an ISSN statement with a deprecated rank and with the given value
	 * @param cancelledIssn
	 * @return
	 */
	public Optional<Statement> findCancelledIssnStatement(String cancelledIssn) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID));
		if(statements == null) {
			return Optional.empty();
		}
		// we compare on the ISSN value, and the rank
		return statements.stream().filter(s -> s.getRank() == StatementRank.DEPRECATED && ((StringValue)s.getValue()).getString().equals(cancelledIssn)).findFirst();
	}
	
	/**
	 * Lists all statements of the given property, and returns an empty list if there are none.
	 * @param property
	 * @return
	 */
	public List<Statement> getStatements(int property) {
		StatementGroup existingStatements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(property));
		if(existingStatements != null) {
			return existingStatements.getStatements();
		} else {
			return new ArrayList<Statement>();
		}
	}
	
	/**
	 * Lists all the ISSN-L statements, and returns an empty list if there are none.
	 * @return
	 */
	public List<Statement> getIssnLStatements() {
		return getStatements(WikidataIssnModel.ISSNL_PROPERTY_ID);
	}
	
	/**
	 * Lists all official websites statements, and returns an empty list if there are none.
	 * @return
	 */
	public List<Statement> getOfficialWebsiteStatements() {
		return getStatements(WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID);
	}
	
	/**
	 * Lists all ISSN statements, and returns an empty list if there are none.
	 * @return
	 */
	public List<Statement> getIssnStatements() {
		return getStatements(WikidataIssnModel.ISSN_PROPERTY_ID);
	}	

	/**
	 * Lists all ISSN statements that have a deprecated rank with a reason for deprecation = INCORRECT_IDENTIFER_VALUE
	 * @return
	 */
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
	
	/**
	 * Lists all statements of the given property having a proper ISSN reference
	 * @param propertyId
	 * @return
	 */
	public List<Statement> findStatementsWithIssnReference(int propertyId) {
		StatementGroup statements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(propertyId));
		if(statements == null) {
			return Collections.emptyList();
		}
		return statements.stream().filter(s -> hasIssnReference(s)).collect(Collectors.toList());
	}	
	
	
	/**
	 * Tests if a statement has a proper ISSN reference
	 * @param s
	 * @return
	 */
	public static boolean hasIssnReference(Statement s) {
		return s.getReferences().stream().anyMatch(r -> isIssnReference(r));
	}
	
	/**
	 * Tests if a reference is a proper ISSN reference, that is a reference with property "Stated In" and property "ISSN" 
	 * @param r
	 * @return
	 */
	public static boolean isIssnReference(Reference r) {
		return
				r.getSnakGroups().stream().anyMatch(sg -> 
					sg.getProperty().getId().equals("P"+WikidataIssnModel.STATED_IN_PROPERTY_ID)
					&&
					sg.getSnaks().stream().anyMatch(snak -> 
						(snak instanceof ValueSnak)
						&&
						(((ValueSnak)snak).getValue() instanceof ItemIdValue)
						&&
						((ItemIdValue)((ValueSnak)snak).getValue()).getId().equals(WikidataIssnModel.ISSN_REGISTER_VALUE.getId())
					)
				)
				&&
				r.getSnakGroups().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+WikidataIssnModel.ISSN_PROPERTY_ID))
				;
	}

	/**
	 * Tests if a statement has both "Named As" and "Distribution Format" qualifiers (that is an ISSN statement with these qualifiers)
	 * @param s
	 * @return
	 */
	public static boolean hasNamedAsAndFormatQualifiers(Statement s) {
		return				
		SerialItemDocument.hasQualifier(s, WikidataIssnModel.NAMED_AS_PROPERTY_ID)
		&&
		SerialItemDocument.hasQualifier(s, WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID)
		;
	}
	
	/**
	 * Tests if a statement has the given qualifier property
	 * 
	 * @param s
	 * @param qualifierProperty
	 * @return
	 */
	public static boolean hasQualifier(Statement s, int qualifierProperty) {
		return s.getQualifiers().stream().anyMatch(sg -> sg.getProperty().getId().equals("P"+qualifierProperty));
	}
	
	/**
	 * Tests if a statement has the given value in a "Named as" qualifier
	 * @param s
	 * @param keyTitle
	 * @return
	 */
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
	
	/**
	 * Tests if a statement has the given value in a "Distribution format" qualifier
	 * @param s
	 * @param value
	 * @return
	 */
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
	
	/**
	 * Tests if a statement has the given value in a "Reason for deprecation" qualifier
	 * @param s
	 * @param value
	 * @return
	 */
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
	
	/**
	 * Tests if a statement has a proper ISSN reference with the given ISSN value
	 * @param s
	 * @param issn
	 * @return
	 */
	public static boolean hasIssnReferenceValue(Statement s, String issn) {
		return s.getReferences().stream().anyMatch(r -> {
			return 
					isIssnReference(r)
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
	
	public static void main(String...strings) {
		String TEST = "http://budabester-zeitung/";
		System.out.println(TEST.substring(0, TEST.length()-1));
	}
	
	
}

package org.issn.issnbot;

import java.util.Optional;

import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.model.WikidataIssnModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ReferenceBuilder;
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

public class WikidataSerial {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	private ItemDocument itemDocument;

	public WikidataSerial(EntityDocument entityDocument) {
		super();
		this.itemDocument = ((ItemDocument)entityDocument);
	}
	
	public Optional<Statement> buildTitleUpdateStatement(SerialEntry serial) {
		log.debug("Building Title UpdateStatement");
		String title = serial.getTitle().getValue();
		String langCode = serial.getLang().getValue();
		String referenceIssn = serial.getTitle().getReference();
		
		Statement s = null;
		Optional<Statement> existingTitleStatement = findTitleStatement(serial.getTitle().getValue());
		
		if(!existingTitleStatement.isPresent()) {
			log.debug("Title : CREATE");
			// si un même titre existe avec la même valeur (= même chaine + même langue)
			// alors la référence sera ajoutée dessus
			s = StatementBuilder
					.forSubjectAndProperty(itemDocument.getEntityId(), WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID))
					.withValue(Datamodel.makeMonolingualTextValue(title, langCode))
					.withReference(this.buildStatementReference(referenceIssn))
					.build();
		} else if(!hasIssnReference(existingTitleStatement.get())) {
			log.debug("Title : UPDATE REFERENCES");
			// if existing title does not have ISSN references, add them			
			s = existingTitleStatement.get();
			s.getReferences().add(this.buildStatementReference(referenceIssn));			
		} else {
			log.debug("Title : NOTHING");
		}
		
		return Optional.ofNullable(s);
	}

	public Optional<Statement> findTitleStatement(String title) {
		StatementGroup titleStatements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID));
		// we compare on string value only, not on language and not on qualifiers
		return titleStatements.stream().filter(s -> ((MonolingualTextValue)s.getValue()).getText().equals(title)).findFirst();
	}
	
	public Optional<Statement> findIssnStatement(String issn) {
		StatementGroup issnStatements = this.itemDocument.findStatementGroup(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID));
		// we compare on the ISSN only, not on qualifiers
		return issnStatements.stream().filter(s -> ((StringValue)s.getValue()).getString().equals(issn)).findFirst();
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

	
}

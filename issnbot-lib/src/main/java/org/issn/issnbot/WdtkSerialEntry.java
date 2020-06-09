package org.issn.issnbot;

import java.util.List;
import java.util.stream.Collectors;

import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.model.WikidataIssnModel;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ReferenceBuilder;
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public class WdtkSerialEntry {

	private SerialEntry serial;
	private ItemIdValue entityId;
	
	public WdtkSerialEntry(SerialEntry serial) {
		super();
		this.serial = serial;
		
		this.entityId = Datamodel.makeWikidataItemIdValue(this.serial.getWikidataId());
	}

	public SerialEntry getSerial() {
		return serial;
	}
	
	public Statement getTitleStatement() {
		String title = serial.getTitle().getValue();
		String langCode = serial.getLang().getValue();
		String referenceIssn = serial.getTitle().getReference();
		
		// si un même titre existe avec la même valeur (= même chaine + même langue)
		// alors la référence sera ajoutée dessus
		Statement s = StatementBuilder
				.forSubjectAndProperty(entityId, WikidataIssnModel.toWikidataProperty(WikidataIssnModel.TITLE_PROPERTY_ID))
				.withValue(Datamodel.makeMonolingualTextValue(title, langCode))
				.withReference(this.buildStatementReference(referenceIssn))
				.build();
		
		return s;
	}
	
	public Statement getIssnLStatement() {
		Statement s = StatementBuilder
				.forSubjectAndProperty(entityId, WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSNL_PROPERTY_ID))
				.withValue(Datamodel.makeStringValue(serial.getIssnL()))
				.build();

		return s;
	}
	
	public List<Statement> getIssnStatements() {
		return this.serial.getIssns().stream().map(issn -> {
			
			
			// the main value with the ISSN String
			Statement s = StatementBuilder
					.forSubjectAndProperty(entityId, WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID))
					.withValue(Datamodel.makeStringValue(issn.getIssn()))
					// key title qualifier
					.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.NAMED_AS_PROPERTY_ID), Datamodel.makeStringValue(issn.getKeyTitle()))
					// distribution format qualifier
					.withQualifierValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.DISTRIBUTION_FORMAT_PROPERTY_ID), issn.getDistributionFormat())
					.build();

			return s;
		}).collect(Collectors.toList());
	}
	
	
	private Reference buildStatementReference(String issn) {
		Reference reference = ReferenceBuilder.newInstance()
				.withPropertyValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.STATED_IN_PROPERTY_ID), WikidataIssnModel.ISSN_REGISTER_VALUE)
				.withPropertyValue(WikidataIssnModel.toWikidataProperty(WikidataIssnModel.ISSN_PROPERTY_ID), Datamodel.makeStringValue(issn))
				.build();
		
		return reference;
	}
	
}

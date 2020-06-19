package org.issn.issnbot.model;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.ItemIdValueImpl;
import org.wikidata.wdtk.datamodel.implementation.PropertyIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

/**
 * Final class declaring constants for everything needed for the ISSN Bot.
 * @author thomas
 *
 */
public final class WikidataIssnModel {

	public static final String WIKIDATA_SPARQL_ENDPOINT = "http://query.wikidata.org/sparql";
	
	public static final String WIKIDATA_IRI = "http://www.wikidata.org/entity/";
	
	public static final int ISSN_PROPERTY_ID = 236;
	
	public static final int NAMED_AS_PROPERTY_ID = 1810;
	
	public static final int DISTRIBUTION_FORMAT_PROPERTY_ID = 437;
	
	public static final int ISSNL_PROPERTY_ID = 7363;

	public static final int REASON_FOR_DEPRECATION_PROPERTY_ID = 2241;
	
	public static final int TITLE_PROPERTY_ID = 1476;
	
	public static final int LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID = 407;
	
	public static final int OFFICIAL_WEBSITE_PROPERTY_ID = 856;
	
	public static final int PLACE_OF_PUBLICATION_PROPERTY_ID = 291;
	
	public static final int STATED_IN_PROPERTY_ID = 248;
	
	public static final int FAKE_LABEL_PROPERTY_ID = 1;
	
	public static final int FAKE_ALIAS_PROPERTY_ID = 2;
	
	public static final int FAKE_ISSN1_PROPERTY_ID = ISSN_PROPERTY_ID;
	public static final int FAKE_ISSN2_PROPERTY_ID = ISSN_PROPERTY_ID*10;
	public static final int FAKE_ISSN3_PROPERTY_ID = ISSN_PROPERTY_ID*100;
	public static final int FAKE_ISSN4_PROPERTY_ID = ISSN_PROPERTY_ID*1000;
	
	// this is needed to fetch the country codes
	public static final int ISO_3166_1_ALPHA_3_PROPERTY_ID = 298;
	
	// this is needed to fetch the language codes
	public static final int ISO_639_2_PROPERTY_ID = 219;
	
	public static final int ISO_639_1_PROPERTY_ID = 218;
	
	public static final int WIKIMEDIA_LANGUAGE_CODE_PROPERTY_ID = 424;
	
	// this is Q-ID
	public static final ItemIdValue WITHDRAWN_IDENTIFER_VALUE = Datamodel.makeWikidataItemIdValue("Q"+21441764);
	
	// this is Q-ID
	public static final ItemIdValue ISSN_REGISTER_VALUE = Datamodel.makeWikidataItemIdValue("Q"+70460099);
	
	public static PropertyIdValue toWikidataProperty(int propertyId) {
		return new PropertyIdValueImpl("P"+propertyId, WIKIDATA_IRI);
	}

}

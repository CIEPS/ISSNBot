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
	
	// this is Q-ID
	public static final ItemIdValue WITHDRAWN_IDENTIFER_VALUE = Datamodel.makeWikidataItemIdValue("Q"+21441764);
	
	// this is Q-ID
	public static final ItemIdValue ISSN_REGISTER_VALUE = Datamodel.makeWikidataItemIdValue("Q"+70460099);
	
	public static PropertyIdValue toWikidataProperty(int propertyId) {
		return new PropertyIdValueImpl("P"+propertyId, WIKIDATA_IRI);
	}

}

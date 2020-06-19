package org.issn.issnbot.providers;

import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public interface WikidataIdProviderIfc {

	public ItemIdValue getWikidataId(String code);
	
	
}

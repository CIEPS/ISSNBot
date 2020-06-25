package org.issn.issnbot.cleaner;

import java.util.List;

import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public interface EntriesToCleanProvider {

	public List<ItemIdValue> getEntriesToClean();
	
}

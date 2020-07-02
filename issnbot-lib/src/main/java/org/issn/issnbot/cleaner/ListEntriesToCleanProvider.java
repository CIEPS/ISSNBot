package org.issn.issnbot.cleaner;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class ListEntriesToCleanProvider implements EntriesToCleanProvider {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private List<ItemIdValue> entriesToClean;

	public ListEntriesToCleanProvider(List<ItemIdValue> entriesToClean) {
		this.entriesToClean = entriesToClean;
	}

	public List<ItemIdValue> getEntriesToClean() {
		return entriesToClean;
	}

	public void setEntriesToClean(List<ItemIdValue> entriesToClean) {
		this.entriesToClean = entriesToClean;
	}

}

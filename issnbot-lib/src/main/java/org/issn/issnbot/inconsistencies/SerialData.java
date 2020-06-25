package org.issn.issnbot.inconsistencies;

import java.util.List;

import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

public class SerialData {

	private String uri;
	private ItemIdValue country;
	private List<MonolingualTextValue> titles;
	
	public SerialData(String uri) {
		super();
		this.uri = uri;
	}

	public ItemIdValue getCountry() {
		return country;
	}

	public void setCountry(ItemIdValue country) {
		this.country = country;
	}

	public List<MonolingualTextValue> getTitles() {
		return titles;
	}

	public void setTitles(List<MonolingualTextValue> titles) {
		this.titles = titles;
	}

	public String getUri() {
		return uri;
	}
	
}

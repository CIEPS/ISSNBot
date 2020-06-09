package org.issn.issnbot.model;

import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class IssnValue {

	private String issn;
	private String keyTitle;
	private ItemIdValue distributionFormat;
	
	
	
	public IssnValue(String issn, String keyTitle, ItemIdValue distributionFormat) {
		super();
		this.issn = issn;
		this.keyTitle = keyTitle;
		this.distributionFormat = distributionFormat;
	}
	
	public String getIssn() {
		return issn;
	}
	public void setIssn(String issn) {
		this.issn = issn;
	}
	public String getKeyTitle() {
		return keyTitle;
	}
	public void setKeyTitle(String keyTitle) {
		this.keyTitle = keyTitle;
	}
	public ItemIdValue getDistributionFormat() {
		return distributionFormat;
	}
	public void setDistributionFormat(ItemIdValue distributionFormat) {
		this.distributionFormat = distributionFormat;
	}
	
}

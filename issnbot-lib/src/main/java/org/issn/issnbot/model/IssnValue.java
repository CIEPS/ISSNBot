package org.issn.issnbot.model;

public class IssnValue {

	private String issn;
	private String keyTitle;
	private String distributionFormat;
	
	
	
	public IssnValue(String issn, String keyTitle, String distributionFormat) {
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
	public String getDistributionFormat() {
		return distributionFormat;
	}
	public void setDistributionFormat(String distributionFormat) {
		this.distributionFormat = distributionFormat;
	}
	
}

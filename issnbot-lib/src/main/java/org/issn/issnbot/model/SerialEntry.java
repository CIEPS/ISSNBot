package org.issn.issnbot.model;

import java.util.List;
import java.util.Map;

public class SerialEntry {

	private String issnL;
	private String wikidataId;
	private ValueWithReference title;
	private ValueWithReference lang;
	private ValueWithReference country;
	private	List<IssnValue> issns;
	
	
	public ValueWithReference getLang() {
		return lang;
	}
	public void setLang(ValueWithReference lang) {
		this.lang = lang;
	}
	public ValueWithReference getTitle() {
		return title;
	}
	public void setTitle(ValueWithReference title) {
		this.title = title;
	}
	public ValueWithReference getCountry() {
		return country;
	}
	public void setCountry(ValueWithReference country) {
		this.country = country;
	}
	public List<IssnValue> getIssns() {
		return issns;
	}
	public void setIssns(List<IssnValue> issns) {
		this.issns = issns;
	}
	public String getIssnL() {
		return issnL;
	}
	public void setIssnL(String issnL) {
		this.issnL = issnL;
	}
	public String getWikidataId() {
		return wikidataId;
	}
	/**
	 * A Q-ID e.g. Q123456
	 * @param wikidataId
	 */
	public void setWikidataId(String wikidataId) {
		this.wikidataId = wikidataId;
	}
	
	public class ValueWithReference {
		private String value;
		private String reference;
		
		public ValueWithReference(String value, String reference) {
			super();
			this.value = value;
			this.reference = reference;
		}
		
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public String getReference() {
			return reference;
		}
		public void setReference(String reference) {
			this.reference = reference;
		}
		
	}
}

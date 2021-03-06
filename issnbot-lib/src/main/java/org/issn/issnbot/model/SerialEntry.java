package org.issn.issnbot.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SerialEntry {

	private String issnL;
	private String wikidataId;
	private ValueWithReference title;
	private ValueWithReference lang;
	private ValueWithReference country;
	private ValuesWithReference urls;
	private	List<IssnValue> issns = new ArrayList<IssnValue>();
	private List<String> cancelledIssns = new ArrayList<String>();
	
	// complete original CSV record
	private String record;
	
	private long recordNumber;
	
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
	public List<String> getCancelledIssns() {
		return cancelledIssns;
	}
	public void setCancelledIssns(List<String> cancelledIssns) {
		this.cancelledIssns = cancelledIssns;
	}
	/**
	 * A Q-ID e.g. Q123456
	 * @param wikidataId
	 */
	public void setWikidataId(String wikidataId) {
		this.wikidataId = wikidataId;
	}

	public ValuesWithReference getUrls() {
		return urls;
	}
	public void setUrls(ValuesWithReference urls) {
		this.urls = urls;
	}
	
	

	public String getRecord() {
		return record;
	}
	public void setRecord(String record) {
		this.record = record;
	}
	
	public long getRecordNumber() {
		return recordNumber;
	}
	public void setRecordNumber(long recordNumber) {
		this.recordNumber = recordNumber;
	}
	
	
	public boolean hasOfficialWebsite(String value) {
		if(this.urls != null && this.urls.values != null) {
			return this.urls.values.stream().anyMatch(v -> v.equals(value));
		} else {
			return false;
		}
	}
	
	public boolean hasIssn(String value) {
		if(this.issns != null) {
			return this.issns.stream().anyMatch(issn -> issn.getIssn().equals(value));
		} else {
			return false;
		}
	}
	
	public boolean hasCancelledIssn(String value) {
		if(this.cancelledIssns != null) {
			return this.cancelledIssns.contains(value);
		} else {
			return false;
		}
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
	
	public class ValuesWithReference {
		private List<String> values;
		private String reference;
		
		public ValuesWithReference(List<String> values, String reference) {
			super();
			this.values = values;
			this.reference = reference;
		}
		
		public List<String> getValues() {
			return values;
		}
		public void setValues(List<String> values) {
			this.values = values;
		}
		public String getReference() {
			return reference;
		}
		public void setReference(String reference) {
			this.reference = reference;
		}		
	}
}

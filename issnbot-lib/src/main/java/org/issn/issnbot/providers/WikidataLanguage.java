package org.issn.issnbot.providers;

public class WikidataLanguage {

	private int qid;
	private String wikimediaCode;
	private String alpha3Code;
	private String alpha2Code;
	
	
	public WikidataLanguage(int qid, String wikimediaCode, String alpha3Code, String alpha2Code) {
		super();
		this.qid = qid;
		this.wikimediaCode = wikimediaCode;
		this.alpha3Code = alpha3Code;
		this.alpha2Code = alpha2Code;
	}
	
	public int getQid() {
		return qid;
	}
	public void setQid(int qid) {
		this.qid = qid;
	}
	public String getWikimediaCode() {
		return wikimediaCode;
	}
	public void setWikimediaCode(String wikimediaCode) {
		this.wikimediaCode = wikimediaCode;
	}
	public String getAlpha3Code() {
		return alpha3Code;
	}
	public void setAlpha3Code(String alpha3Code) {
		this.alpha3Code = alpha3Code;
	}
	public String getAlpha2Code() {
		return alpha2Code;
	}
	public void setAlpha2Code(String alpha2Code) {
		this.alpha2Code = alpha2Code;
	}
	
	
	
}

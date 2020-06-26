package org.issn.issnbot.providers;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.issn.issnbot.model.WikidataIssnModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikidataLanguageCodesProvider {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private Map<String, String> languageCodes;

	public WikidataLanguageCodesProvider() {
		this.initCache();
	}

	public String getWikimediaCode(String alpha3Code) {
		if(languageCodes.containsKey(alpha3Code) ) {
			return languageCodes.get(alpha3Code);
		} else {
			return null;
		}
	}

	private void initCache() {

		this.languageCodes = new HashMap<>();

		// hardcode value for mis and mul and und
		this.languageCodes.put("mis", "mis");
		this.languageCodes.put("mul", "mul");
		this.languageCodes.put("und", "und");
		
		Repository repo = new SPARQLRepository(WikidataIssnModel.WIKIDATA_SPARQL_ENDPOINT);

		try (RepositoryConnection conn = repo.getConnection()) {
			String queryString = "PREFIX wdt: <http://www.wikidata.org/prop/direct/>"+"\n"
					+ "SELECT DISTINCT ?iso6392 ?wikimediaCode"+"\n"
					+ "WHERE {"+"\n"
					+ "  ?qid wdt:P"+WikidataIssnModel.WIKIMEDIA_LANGUAGE_CODE_PROPERTY_ID+" ?wikimediaCode ."+"\n"
					+ "  ?qid wdt:P"+WikidataIssnModel.ISO_639_2_PROPERTY_ID+" ?iso6392 ."+"\n"
					+ "} ORDER BY ?wikimediaCode";
			log.debug("Issuing SPARQL \n"+queryString);
			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) {  // iterate over the result
					BindingSet bindingSet = result.next();

					String alpha3 = bindingSet.getValue("iso6392").stringValue();
					String wikimediaCode = bindingSet.getValue("wikimediaCode").stringValue();
					
					log.debug("Populated wikimedia language code cache with "+alpha3+" => "+wikimediaCode);
					
					// double check for duplicate codes
					// mapping could be duplicated, e.g. "gre" / "el" appears twice
					if(languageCodes.containsKey(alpha3) && !languageCodes.get(alpha3).equals(wikimediaCode)) {
						log.error("Found duplicate ISO6392 -> wikimedia code mapping for '"+alpha3+"': "+languageCodes.get(alpha3)+" and "+wikimediaCode);
					}
					
					this.languageCodes.put(alpha3, wikimediaCode);
				}
			}
		}
		
		log.debug("Language code cache contains "+this.languageCodes.size()+" language codes.");
		log.info("Wikimedia language codes map : \n"+this.languageCodes.entrySet().stream().map(e -> e.getKey()+"="+e.getValue()).collect(Collectors.joining("\n")));

	}

	public Map<String, String> getLanguageCodes() {
		return languageCodes;
	}
	
}

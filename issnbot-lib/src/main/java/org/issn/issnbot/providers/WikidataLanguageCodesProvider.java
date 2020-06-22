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

	private Map<String, WikidataLanguage> languageCodes;

	public WikidataLanguageCodesProvider() {
		this.initCache();
	}

	public String getWikimediaCode(String alpha3Code) {
		if(languageCodes.containsKey(alpha3Code) ) {
			return languageCodes.get(alpha3Code).getWikimediaCode();
		} else {
			return null;
		}
	}
	
	public WikidataLanguage getLanguage(String alpha3Code) {
		return languageCodes.get(alpha3Code); 
	}

	private void initCache() {

		this.languageCodes = new HashMap<>();

		// hardcode value for mis and mul
		this.languageCodes.put("mis", new WikidataLanguage(22283016, "mis", "mis", null));
		this.languageCodes.put("mul", new WikidataLanguage(-1, "mul", "mul", null));
		
		Repository repo = new SPARQLRepository(WikidataIssnModel.WIKIDATA_SPARQL_ENDPOINT);

		try (RepositoryConnection conn = repo.getConnection()) {
			String queryString = "PREFIX wdt: <http://www.wikidata.org/prop/direct/>"+"\n"
					+ "SELECT ?qid ?wikimediaCode ?iso6392 ?iso6391"+"\n"
					+ "WHERE {"+"\n"
					+ "  ?qid wdt:P"+WikidataIssnModel.WIKIMEDIA_LANGUAGE_CODE_PROPERTY_ID+" ?wikimediaCode ."+"\n"
					+ "  ?qid wdt:P"+WikidataIssnModel.ISO_639_2_PROPERTY_ID+" ?iso6392 ."+"\n"
					+ "  OPTIONAL { ?qid wdt:P"+WikidataIssnModel.ISO_639_1_PROPERTY_ID+" ?iso6391 }"+"\n"
					+ "} ORDER BY ?wikimediaCode";
			log.debug("Issuing SPARQL \n"+queryString);
			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) {  // iterate over the result
					BindingSet bindingSet = result.next();
					Integer qid = Integer.parseInt(bindingSet.getValue("qid").stringValue().substring(WikidataIssnModel.WIKIDATA_IRI.length() + 1));
					String wikimediaCode = bindingSet.getValue("wikimediaCode").stringValue();
					String alpha3 = bindingSet.getValue("iso6392").stringValue();
					String alpha2 = (bindingSet.hasBinding("iso6391"))?bindingSet.getValue("iso6391").stringValue():null;
					log.debug("Populated language code cache with "+alpha3+" => "+alpha2);
					if(alpha2 == null) {
						log.info("Language code "+alpha3+" does not have iso-639-2 equivalent code ");
					}
					this.languageCodes.put(alpha3, new WikidataLanguage(qid, wikimediaCode, alpha3, alpha2));
				}
			}
		}
		
		log.debug("Language code cache contains "+this.languageCodes.size()+" language codes.");
		log.info("Language map : \n"+this.languageCodes.entrySet().stream().map(e -> e.getKey()+" => "+e.getValue().getWikimediaCode()).collect(Collectors.joining("\n")));

	}
}

package org.issn.issnbot.providers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.issn.issnbot.model.WikidataIssnModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class WikidataSparqlCountryIdProvider implements WikidataIdProviderIfc {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private Map<String, ItemIdValue> countryIdCache;

	public WikidataSparqlCountryIdProvider() {
		this.initCache();
	}

	@Override
	public ItemIdValue getWikidataId(String code) {
		return countryIdCache.get(code);
	}

	private void initCache() {

		this.countryIdCache = new HashMap<>();

		Repository repo = new SPARQLRepository(WikidataIssnModel.WIKIDATA_SPARQL_ENDPOINT);

		try (RepositoryConnection conn = repo.getConnection()) {
			String queryString = "SELECT ?iso31661_alpha3 ?qid WHERE { ?qid <http://www.wikidata.org/prop/direct/P"+WikidataIssnModel.ISO_3166_1_ALPHA_3_PROPERTY_ID+"> ?iso31661_alpha3 } ORDER BY ?iso31661_alpha3";
			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) {  // iterate over the result
					BindingSet bindingSet = result.next();
					String code = bindingSet.getValue("iso31661_alpha3").stringValue();
					String qid = bindingSet.getValue("qid").stringValue().substring(WikidataIssnModel.WIKIDATA_IRI.length());
					log.debug("Populated country ID cache with "+code+" => "+qid);
					this.countryIdCache.put(code, Datamodel.makeWikidataItemIdValue(qid));
				}
			}
		}
		
		log.debug("Country ID cache contains "+this.countryIdCache.size()+" language code entries.");

	}
}

package org.issn.issnbot.providers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.Value;
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

public class WikidataSparqlLanguageIdProvider implements WikidataIdProviderIfc {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private Map<String, ItemIdValue> languageIdCache;

	public WikidataSparqlLanguageIdProvider() {
		this.initCache();
	}

	@Override
	public ItemIdValue getWikidataId(String languageCode) {
		return languageIdCache.get(languageCode);
	}

	private void initCache() {

		this.languageIdCache = new HashMap<>();

		Repository repo = new SPARQLRepository(WikidataIssnModel.WIKIDATA_SPARQL_ENDPOINT);

		try (RepositoryConnection conn = repo.getConnection()) {
			String queryString = "SELECT ?iso6392 ?qid WHERE { ?qid <http://www.wikidata.org/prop/direct/P"+WikidataIssnModel.ISO_639_2_PROPERTY_ID+"> ?iso6392 } ORDER BY ?iso6392";
			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
			log.debug("Issuing SPARQL \n"+queryString);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) {  // iterate over the result
					BindingSet bindingSet = result.next();
					String code = bindingSet.getValue("iso6392").stringValue();
					String qid = bindingSet.getValue("qid").stringValue().substring(WikidataIssnModel.WIKIDATA_IRI.length());
					log.debug("Populated language ID cache with "+code+" => "+qid);
					this.languageIdCache.put(code, Datamodel.makeWikidataItemIdValue(qid));
				}
			}
		}
		
		log.debug("Language ID cache contains "+this.languageIdCache.size()+" language code entries.");

	}
}

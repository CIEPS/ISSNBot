package org.issn.issnbot.cleaner;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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

public class TitleBasedEntriesToCleanProvider implements EntriesToCleanProvider {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private Map<ItemIdValue, String> entriesToClean;

	public TitleBasedEntriesToCleanProvider() {
		this.initCache();
	}

	public List<ItemIdValue> getEntriesToClean() {
		return new ArrayList<ItemIdValue>(entriesToClean.keySet());
	}

	private void initCache() {

		this.entriesToClean = new HashMap<>();
		
		Repository repo = new SPARQLRepository(WikidataIssnModel.WIKIDATA_SPARQL_ENDPOINT);

		try (RepositoryConnection conn = repo.getConnection()) {
			String queryString;
			try (Scanner scanner = new Scanner(this.getClass().getResourceAsStream("TitleBasedEntriesToCleanProvider_query.rq"), StandardCharsets.UTF_8.name())) {
				queryString = scanner.useDelimiter("\\A").next();
		    }
			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
			log.debug("Issuing SPARQL \n"+queryString);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) {  // iterate over the result
					BindingSet bindingSet = result.next();
					String serial = bindingSet.getValue("serial").stringValue().substring(WikidataIssnModel.WIKIDATA_IRI.length());
					String issn = bindingSet.getValue("titleSourceIssn").stringValue();
					log.debug("Found serial to be cleaned "+serial+" because ISSN "+issn+" is not present anymore.");
					this.entriesToClean.put(Datamodel.makeWikidataItemIdValue(serial), issn);
				}
			}
		}
		
		log.debug("Found "+this.entriesToClean.size()+" entries to clean.");

	}
}

package org.issn.issnbot.providers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class PropertiesLanguageIdProvider implements WikidataIdProviderIfc {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private Map<String, ItemIdValue> languageIdCache;

	public PropertiesLanguageIdProvider() {
		this.initCache();
	}

	@Override
	public ItemIdValue getWikidataId(String languageCode) {
		return languageIdCache.get(languageCode);
	}

	private void initCache() {

		this.languageIdCache = new HashMap<>();

		Properties properties;
		try {
			properties = new Properties();
			properties.load(this.getClass().getResourceAsStream("languageIds_mapping.properties"));
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		properties.entrySet().stream().forEach(e -> {
			if(e.getValue() != null && !e.getValue().equals("")) {
				languageIdCache.put((String)e.getKey(), Datamodel.makeWikidataItemIdValue((String)e.getValue()));
			}
		});
		
		log.debug("Language ID cache contains "+this.languageIdCache.size()+" entries.");
		log.info("Language ID cache : \n"+this.languageIdCache.entrySet().stream().map(e -> e.getKey()+"="+e.getValue().getId()).collect(Collectors.joining("\n")));

	}
}

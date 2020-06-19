package org.issn.issnbot.providers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class WikidataDistributionFormatProvider implements WikidataIdProviderIfc {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private Map<String, ItemIdValue> distributionFormatCache;

	public WikidataDistributionFormatProvider() {
		this.initCache();
	}

	@Override
	public ItemIdValue getWikidataId(String code) {
		return distributionFormatCache.get(code);
	}

	private void initCache() {

		this.distributionFormatCache = new HashMap<>();
		
		Properties distributionFormats;
		try {
			distributionFormats = new Properties();
			distributionFormats.load(this.getClass().getResourceAsStream("distributionFormats_mapping.properties"));
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		distributionFormats.entrySet().stream().forEach(e -> {
			if(e.getValue() != null && !e.getValue().equals("")) {
				distributionFormatCache.put((String)e.getKey(), Datamodel.makeWikidataItemIdValue((String)e.getValue()));
			}
		});
		
		log.debug("Distribution format cache contains "+this.distributionFormatCache.size()+" language code entries.");

	}
}

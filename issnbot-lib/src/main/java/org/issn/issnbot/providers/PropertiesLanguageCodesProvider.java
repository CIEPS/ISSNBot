package org.issn.issnbot.providers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesLanguageCodesProvider {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private Map<String, String> languageCodes;

	public PropertiesLanguageCodesProvider() {
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

		Properties properties;
		try {
			properties = new Properties();
			properties.load(this.getClass().getResourceAsStream("languageCodes_mapping.properties"));
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		properties.entrySet().stream().forEach(e -> {
			if(e.getValue() != null && !e.getValue().equals("")) {
				languageCodes.put((String)e.getKey(), (String)e.getValue());
			}
		});
		
		log.debug("Language code cache contains "+this.languageCodes.size()+" language codes.");
		log.info("Wikimedia language codes map : \n"+this.languageCodes.entrySet().stream().map(e -> e.getKey()+"="+e.getValue()).collect(Collectors.joining("\n")));

	}
	
	public Map<String, String> getLanguageCodes() {
		return languageCodes;
	}
}

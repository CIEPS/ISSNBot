package org.issn.issnbot;

import org.issn.issnbot.providers.WikidataSparqlLanguageIdProvider;
import org.junit.Assert;
import org.junit.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class WikidataSparqlLanguageIdProviderTest {

	@Test
	public void test() {
		WikidataSparqlLanguageIdProvider provider = new WikidataSparqlLanguageIdProvider();
		ItemIdValue qid = provider.getWikidataId("fre");
		Assert.assertTrue(qid.getId().equals("Q150"));
	}

}

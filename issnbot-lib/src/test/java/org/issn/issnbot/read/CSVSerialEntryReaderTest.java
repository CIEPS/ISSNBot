package org.issn.issnbot.read;

import java.io.InputStream;
import java.util.List;

import org.issn.issnbot.model.SerialEntry;
import org.junit.Assert;
import org.junit.Test;

public class CSVSerialEntryReaderTest {

	@Test
	public void test() throws Exception {
		CSVSerialEntryReader reader = new CSVSerialEntryReader(this.getClass().getResourceAsStream("basic_test.csv"));
		List<SerialEntry> entries = reader.read();
		
		Assert.assertTrue(entries.size() == 1);
	}

}

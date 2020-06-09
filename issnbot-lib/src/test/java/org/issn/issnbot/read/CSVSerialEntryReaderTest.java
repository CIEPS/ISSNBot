package org.issn.issnbot.read;

import java.io.InputStream;
import java.util.List;

import org.issn.issnbot.model.SerialEntry;
import org.junit.Assert;
import org.junit.Test;

public class CSVSerialEntryReaderTest {

	@Test
	public void test() throws Exception {
		CSVSerialEntryReader reader = new CSVSerialEntryReader();
		InputStream input = this.getClass().getResourceAsStream("basic_test.csv");
		List<SerialEntry> entries = reader.read(input);
		
		Assert.assertTrue(entries.size() == 1);
	}

}

package org.issn.issnbot.read;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.model.SerialEntry.ValueWithReference;

public class CSVSerialEntryReader implements SerialEntryReader {

	enum COLUMN {		
		
		ISSN_L(0),
		WIKIDATA_QID(1),
		TITLE(2),
		TITLE_REF_ISSN(3),
		LANG(4),
		LANG_REF_ISSN(5),
		COUNTRY(6),
		COUNTRY_REF_ISSN(7),
		URLS(8),
		URLS_REF_ISSN(9),
		ISSN_1(10),
		ISSN_1_KEY_TITLE(11),
		ISSN_1_FORMAT(12),
		ISSN_2(13),
		ISSN_2_KEY_TITLE(14),
		ISSN_2_FORMAT(15),
		ISSN_3(16),
		ISSN_3_KEY_TITLE(17),
		ISSN_3_FORMAT(18),
		ISSN_4(19),
		ISSN_4_KEY_TITLE(20),
		ISSN_4_FORMAT(21),
		CANCELLED_ISSN_1(22),
		CANCELLED_ISSN_2(23),
		;
		
		private int index;

		private COLUMN(int index) {
			this.index = index;
		}

		public int getIndex() {
			return index;
		}
	
	}
	
	@Override
	public List<SerialEntry> read(InputStream input) throws IOException, SerialEntryReadException {
		
		List<SerialEntry> result = new ArrayList<SerialEntry>();
		
		CSVParser csvParser = CSVParser.parse(input, Charset.forName("UTF-8"), CSVFormat.DEFAULT.withFirstRecordAsHeader());
		CSVRecordParser recordParser = new CSVRecordParser();
		for (CSVRecord csvRecord : csvParser) {
			result.add(recordParser.parse(csvRecord));
		}
		
		return result;
	}
	
	class CSVRecordParser {
		
		public SerialEntry parse(CSVRecord record) {
			SerialEntry entry = new SerialEntry();
			
			entry.setIssnL(record.get(COLUMN.ISSN_L.getIndex()));
			entry.setWikidataId(record.get(COLUMN.WIKIDATA_QID.getIndex()));
			entry.setTitle(entry.new ValueWithReference(record.get(COLUMN.TITLE.getIndex()), record.get(COLUMN.TITLE_REF_ISSN.getIndex())));
			
			return entry;
		}
	}

}

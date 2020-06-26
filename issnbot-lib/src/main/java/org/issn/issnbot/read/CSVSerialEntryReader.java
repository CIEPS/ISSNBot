package org.issn.issnbot.read;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.issn.issnbot.model.IssnValue;
import org.issn.issnbot.model.SerialEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVSerialEntryReader implements SerialEntryReader {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	/*
	 * ISSN -L	Wiki-ID	Label (titre)	ISSN ref titre	Code ISO de langue	ISSN ref langue	ISO pays	ISSN ref pays	URLs	
	 * ISSN ref URLs	ISSN1	keytitle1	format1	ISSN2	keytitle2	format2	ISSN3	keytitle3	format3	ISSN4	keytitle4	format4	cancelled ISSN

	 */
	public enum COLUMN {		
		
		ISSN_L(0, "ISSN-L"),
		WIKIDATA_QID(1, "Wiki-ID"),
		TITLE(2, "Label (titre)"),
		TITLE_REF_ISSN(3, "ISSN ref titre"),
		LANG(4, "Code ISO de langue"),
		LANG_REF_ISSN(5, "ISSN ref langue"),
		COUNTRY(6, "ISO pays"),
		COUNTRY_REF_ISSN(7, "ISSN ref pays"),
		URLS(8, "URLs"),
		URLS_REF_ISSN(9, "ISSN ref URLs"),
		ISSN_1(10, "ISSN1"),
		ISSN_1_KEY_TITLE(11, "keytitle1"),
		ISSN_1_FORMAT(12, "format1"),
		ISSN_2(13, "ISSN2"),
		ISSN_2_KEY_TITLE(14, "keytitle2"),
		ISSN_2_FORMAT(15, "format2"),
		ISSN_3(16, "ISSN3"),
		ISSN_3_KEY_TITLE(17, "keytitle3"),
		ISSN_3_FORMAT(18, "format3"),
		ISSN_4(19, "ISSN4"),
		ISSN_4_KEY_TITLE(20, "keytitle4"),
		ISSN_4_FORMAT(21, "format4"),
		CANCELLED_ISSNS(22, "cancelled ISSN"),
		;
		
		private int index;
		private String header;

		private COLUMN(int index, String header) {
			this.index = index;
			this.header = header;
		}

		public int getIndex() {
			return index;
		}
		
		public String getHeader() {
			return header;
		}
	
	}
	
	protected InputStream input;
	
	protected CSVFormat csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter('\t');
	
	public CSVSerialEntryReader(InputStream input) {
		super();
		this.input = input;
	}

	@Override
	public List<SerialEntry> read() throws IOException, SerialEntryReadException {
		
		List<SerialEntry> result = new ArrayList<SerialEntry>();
		
		CSVParser csvParser = CSVParser.parse(input, Charset.forName("UTF-8"), this.csvFormat);
		
		CSVRecordParser recordParser = new CSVRecordParser();
		for (CSVRecord csvRecord : csvParser) {
			result.add(recordParser.parse(csvRecord));
		}
		
		return result;
	}

	class CSVRecordParser {
		
		public SerialEntry parse(CSVRecord record) {
			SerialEntry entry = new SerialEntry();
			
			entry.setRecordNumber(record.getRecordNumber());
			
			// 0 : ISSN-L
			entry.setIssnL(record.get(COLUMN.ISSN_L.getIndex()));
			// 1 : QID
			entry.setWikidataId(record.get(COLUMN.WIKIDATA_QID.getIndex()));
			// 2 and 3 : Title and reference ISSN
			entry.setTitle(entry.new ValueWithReference(record.get(COLUMN.TITLE.getIndex()).trim(), record.get(COLUMN.TITLE_REF_ISSN.getIndex())));
			// 4 and 5 : lang and reference ISSN
			entry.setLang(entry.new ValueWithReference(record.get(COLUMN.LANG.getIndex()), record.get(COLUMN.LANG_REF_ISSN.getIndex())));
			// 6 and 7 : country and reference ISSN
			entry.setCountry(entry.new ValueWithReference(record.get(COLUMN.COUNTRY.getIndex()), record.get(COLUMN.COUNTRY_REF_ISSN.getIndex())));
			// 8 and 9 : URLs, separated by spaces
			if(record.isSet(COLUMN.URLS.getIndex()) && !record.get(COLUMN.URLS.getIndex()).equals("")) {
				entry.setUrls(entry.new ValuesWithReference(
						Arrays.asList(record.get(COLUMN.URLS.getIndex()).split(" ")),
						record.get(COLUMN.URLS_REF_ISSN.getIndex()))
				);
			}
			// 10, 11, 12 : ISSN 1
			if(record.isSet(COLUMN.ISSN_1.getIndex()) && !record.get(COLUMN.ISSN_1.getIndex()).equals("")) {
				entry.getIssns().add(new IssnValue(record.get(COLUMN.ISSN_1.getIndex()), record.get(COLUMN.ISSN_1_KEY_TITLE.getIndex()).trim(), record.get(COLUMN.ISSN_1_FORMAT.getIndex())));
			}
			// 13, 14, 15 : ISSN 2
			if(record.isSet(COLUMN.ISSN_2.getIndex()) && !record.get(COLUMN.ISSN_2.getIndex()).equals("")) {
				entry.getIssns().add(new IssnValue(record.get(COLUMN.ISSN_2.getIndex()), record.get(COLUMN.ISSN_2_KEY_TITLE.getIndex()).trim(), record.get(COLUMN.ISSN_2_FORMAT.getIndex())));
			}
			// 16, 17, 18 : ISSN 3
			if(record.isSet(COLUMN.ISSN_3.getIndex()) && !record.get(COLUMN.ISSN_3.getIndex()).equals("")) {
				entry.getIssns().add(new IssnValue(record.get(COLUMN.ISSN_3.getIndex()), record.get(COLUMN.ISSN_3_KEY_TITLE.getIndex()).trim(), record.get(COLUMN.ISSN_3_FORMAT.getIndex())));
			}
			// 19, 20, 21 : ISSN 4
			if(record.isSet(COLUMN.ISSN_4.getIndex()) && !record.get(COLUMN.ISSN_4.getIndex()).equals("")) {
				entry.getIssns().add(new IssnValue(record.get(COLUMN.ISSN_4.getIndex()), record.get(COLUMN.ISSN_4_KEY_TITLE.getIndex()).trim(), record.get(COLUMN.ISSN_4_FORMAT.getIndex())));
			}
			// 22 and 23 : cancelled ISSNs
			if(record.isSet(COLUMN.CANCELLED_ISSNS.getIndex()) && !record.get(COLUMN.CANCELLED_ISSNS.getIndex()).equals("")) {
				entry.setCancelledIssns(
						Arrays.asList(record.get(COLUMN.CANCELLED_ISSNS.getIndex()).split(" "))
				);
			}			
			
			// log.debug("Read CSV entry "+entry.getIssnL()+", title is "+entry.getTitle().getValue());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try(CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out), csvFormat)) {
				printer.printRecord(record);
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
			// store raw record line to be outputted in error folder in case of error
			entry.setRecord(new String(out.toByteArray()));
			
			return entry;
		}
	}

}

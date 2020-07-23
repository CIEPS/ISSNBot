package org.issn.issnbot.cleaner;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.issn.issnbot.listeners.IssnBotListener.PropertyStatus;
import org.issn.issnbot.model.WikidataIssnModel;

public class IssnBotCleanerStatusesCSVWriter {

	protected Appendable output;
	
	protected CSVPrinter printer;
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public IssnBotCleanerStatusesCSVWriter(Appendable output) throws IOException {
		super();
		this.output = output;
		
		this.printer = new CSVPrinter(output, CSVFormat.DEFAULT);
		
		// write header
		printer.print("Time");
		printer.print("Wikidata QID");
		printer.print("Status");
		printer.print("Message");
		printer.print("Title (P"+WikidataIssnModel.TITLE_PROPERTY_ID+")");
		printer.print("Language (P"+WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID+")");
		printer.print("Country of origin (P"+WikidataIssnModel.COUNTRY_OF_ORIGIN_PROPERTY_ID+")");
		printer.print("Official Website (P"+WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID+")");
		printer.print("ISSN-L (P"+WikidataIssnModel.ISSNL_PROPERTY_ID+")");
		printer.println();
	}

	public void success(
			String qid,
			String status,
			Map<Integer, PropertyStatus> updateStatuses
	) throws IOException {
		printer.print(sdf.format(new Date()));
		printer.print(qid);
		printer.print(status);
		// empty message when success
		printer.print("");
		for (Map.Entry<Integer, PropertyStatus> aPropStatus : updateStatuses.entrySet()) {
			printer.print(aPropStatus.getValue().status+((aPropStatus.getValue().precision != null)?" ("+aPropStatus.getValue().precision+")":""));
		}
		printer.println();
	}
	
	public void error(String qid, String message) throws IOException {
		printer.print(sdf.format(new Date()));
		printer.print(qid);
		printer.print("ERROR");
		printer.print(message);
		printer.println();
	}
	
	public void close() throws IOException {
		this.printer.close(true);
	}

}

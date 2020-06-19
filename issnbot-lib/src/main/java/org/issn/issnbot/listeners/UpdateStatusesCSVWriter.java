package org.issn.issnbot.listeners;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.issn.issnbot.WikidataUpdateStatus;
import org.issn.issnbot.model.WikidataIssnModel;

public class UpdateStatusesCSVWriter implements UpdateStatusesWriter {

	protected Appendable output;
	
	protected CSVPrinter printer;
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public UpdateStatusesCSVWriter(Appendable output) throws IOException {
		super();
		this.output = output;
		
		this.printer = new CSVPrinter(output, CSVFormat.DEFAULT);
		
		// write header
		printer.print("Time");
		printer.print("ISSN-L");
		printer.print("Wikidata QID");
		printer.print("Status");
		printer.print("ISSN-L (P"+WikidataIssnModel.ISSNL_PROPERTY_ID+")");
		printer.print("Label");
		printer.print("Alias");
		printer.print("Title (P"+WikidataIssnModel.TITLE_PROPERTY_ID+")");
		printer.print("Language (P"+WikidataIssnModel.LANGUAGE_OF_WORK_OR_NAME_PROPERTY_ID+")");
		printer.print("Place of Publication (P"+WikidataIssnModel.PLACE_OF_PUBLICATION_PROPERTY_ID+")");
		printer.print("Official Website (P"+WikidataIssnModel.OFFICIAL_WEBSITE_PROPERTY_ID+")");
		printer.print("ISSN1 (P"+WikidataIssnModel.ISSN_PROPERTY_ID+")");
		printer.print("ISSN2 (P"+WikidataIssnModel.ISSN_PROPERTY_ID+")");
		printer.print("ISSN3 (P"+WikidataIssnModel.ISSN_PROPERTY_ID+")");
		printer.print("ISSN4 (P"+WikidataIssnModel.ISSN_PROPERTY_ID+")");
		printer.println();
	}

	@Override
	public void success(String issnl, String qid, Map<Integer, IssnBotListener.PropertyStatus> updateStatuses) throws IOException {
		printer.print(sdf.format(new Date()));
		printer.print(issnl);
		printer.print(qid);
		printer.print("SUCCESS");
		for (Map.Entry<Integer, IssnBotListener.PropertyStatus> aPropStatus : updateStatuses.entrySet()) {
			printer.print(aPropStatus.getValue().status+((aPropStatus.getValue().precision != null)?" ("+aPropStatus.getValue().precision+")":""));
		}
		printer.println();
	}
	
	@Override
	public void error(String issnl, String qid) throws IOException {
		printer.print(sdf.format(new Date()));
		printer.print(issnl);
		printer.print(qid);
		printer.print("ERROR");
		printer.println();
	}
	
	public void close() throws IOException {
		this.printer.close(true);
	}

}

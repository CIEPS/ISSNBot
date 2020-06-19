package org.issn.issnbot.listeners;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.issn.issnbot.WikidataUpdateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssnBotReportListener implements IssnBotListener {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	private Date startTime;
	private Date endTime;
	private long nbFilesInput;
	private long nbSerialsInput;
	protected long nbBatches = 0;
	protected long nbSuccess = 0;
	protected long nbErrors = 0;
	
	protected Map<WikidataUpdateStatus, Long> countByStatuses = new HashMap<>();
	
	private List<String> serialsInError = new ArrayList<String>();
	
	
	
	public IssnBotReportListener() {
		// init our status map
		for (WikidataUpdateStatus aStatus : WikidataUpdateStatus.values()) {
			this.countByStatuses.put(aStatus, 0L);
		}
	}

	@Override
	public void start(File inputFile) {
		this.nbFilesInput = inputFile.listFiles().length;
		this.startTime = new Date();
	}

	@Override
	public void stop() {
		this.endTime = new Date();
		log.info(this.printReport());
	}

	@Override
	public void startBatch(String batchId) {
		// nothing
	}

	@Override
	public void stopBatch(String batchId) {
		nbBatches++;
	}

	@Override
	public void startSerial(String issnl, String wikidataId) {
		nbSerialsInput++;
	}

	@Override
	public void successSerial(String issnl, String wikidataId, Map<Integer, IssnBotListener.PropertyStatus> statuses) {
		nbSuccess++;
		
		// populate status map
		for (Map.Entry<Integer, IssnBotListener.PropertyStatus> anEntry : statuses.entrySet()) {
			Long value = this.countByStatuses.get(anEntry.getValue().status);
			this.countByStatuses.put(anEntry.getValue().status, ++value);
		}		
	}

	@Override
	public void errorSerial(String issnl, String wikidataId) {
		nbErrors++;
		this.serialsInError.add(issnl+" / "+wikidataId);
	}
	
	public String printReport() {
		StringBuffer sb = new StringBuffer();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long duration = this.endTime.getTime() - this.startTime.getTime();
		long durationInSeconds = duration / 1000;
		sb.append("\n");
		sb.append("\n");
		sb.append("--- ISSN Bot Report ---"+"\n");
		sb.append("- Number of files to process: "+this.nbFilesInput+"\n");
		sb.append("- Number of serials processed: "+this.nbSerialsInput+"\n");
		sb.append("- Number of serials in ERROR  : "+this.nbErrors+"\n");
		sb.append("- Number of serials in success: "+this.nbSuccess+"\n");
		sb.append("\n");
		for (WikidataUpdateStatus aStatus : WikidataUpdateStatus.values()) {
			sb.append("- Number of statements "+aStatus+": "+this.countByStatuses.get(aStatus)+"\n");
		}
		sb.append("\n");
		sb.append("List of serials in errors: "+"\n");
		if(this.serialsInError.isEmpty()) {
			sb.append("  None !"+"\n");
		} else {
			for (int j = 0; j < serialsInError.size(); j++) {
				sb.append("  "+serialsInError.get(j)+"\n");
				if(j == 100) {
					sb.append("  "+"... (showing only 100 values)"+"\n");
				}
			}
		}
		
		sb.append("\n");
		sb.append("Process took "+String.format("%d:%02d:%02d", durationInSeconds / 3600, (durationInSeconds % 3600) / 60, (durationInSeconds % 60))+" (started at "+sdf.format(this.startTime)+", ended at "+sdf.format(this.endTime)+")"+"\n");
		return sb.toString();
	}

}

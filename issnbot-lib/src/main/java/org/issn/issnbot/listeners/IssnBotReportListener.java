package org.issn.issnbot.listeners;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.issn.issnbot.WikidataUpdateStatus;
import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.model.WikidataIssnModel;
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
	
	protected Map<Integer, Long> creationByProperties = new HashMap<>();
	
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
	public void startSerial(SerialEntry entry) {
		nbSerialsInput++;
	}

	@Override
	public void successSerial(SerialEntry entry, SerialResult result) {
		nbSuccess++;
		
		// populate status map
		for (Map.Entry<Integer, IssnBotListener.PropertyStatus> anEntry : result.getStatuses().entrySet()) {
			Long value = this.countByStatuses.get(anEntry.getValue().status);
			this.countByStatuses.put(anEntry.getValue().status, ++value);
			
			// count creation by properties
			if(anEntry.getValue().status == WikidataUpdateStatus.CREATED) {
				if(!this.creationByProperties.containsKey(anEntry.getKey()) ) {
					this.creationByProperties.put(anEntry.getKey(), 0L);
				}
				Long tempVal = this.creationByProperties.get(anEntry.getKey());
				this.creationByProperties.put(anEntry.getKey(), ++tempVal);
			}
		}	
		for (Map.Entry<Integer, IssnBotListener.PropertyStatus> anEntry : result.getPreviousValuesStatuses().entrySet()) {
			Long value = this.countByStatuses.get(anEntry.getValue().status);
			this.countByStatuses.put(anEntry.getValue().status, ++value);
		}	
	}

	@Override
	public void errorSerial(SerialEntry entry, String message) {
		nbErrors++;
		this.serialsInError.add(entry.getIssnL()+" / "+entry.getWikidataId());
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
		for (Map.Entry<Integer, Long> anEntry : this.creationByProperties.entrySet()) {
			if(anEntry.getKey().equals(WikidataIssnModel.FAKE_LABEL_PROPERTY_ID)) {
				sb.append("- Number of creation of labels: "+anEntry.getValue()+"\n");
			} else if(anEntry.getKey().equals(WikidataIssnModel.FAKE_ALIAS_PROPERTY_ID)) {
				sb.append("- Number of creation of aliases: "+anEntry.getValue()+"\n");
			} else if(anEntry.getKey().equals(WikidataIssnModel.FAKE_CANCELLED_ISSN_PROPERTY)) {
				sb.append("- Number of creation of cancelled ISSNs: "+anEntry.getValue()+"\n");
			} else {
				sb.append("- Number of creation of P"+anEntry.getKey()+": "+anEntry.getValue()+"\n");
			}
		}
		sb.append("\n");
		for (WikidataUpdateStatus aStatus : WikidataUpdateStatus.values()) {
			sb.append("- Number of statements "+aStatus+": "+this.countByStatuses.get(aStatus)+"\n");
		}
		// sb.append("- Number of statements TOTAL: "+this.countByStatuses.entrySet().stream().mapToLong(e -> e.getValue()).sum()+"\n");
		sb.append("\n");
		sb.append("List of serials in errors: "+"\n");
		if(this.serialsInError.isEmpty()) {
			sb.append("  None !"+"\n");
		} else {
			for (int j = 0; j < serialsInError.size(); j++) {
				sb.append("  "+serialsInError.get(j)+"\n");
				if(j == 10) {
					sb.append("  "+"... (showing only 10 values)"+"\n");
					break;
				}
			}
		}
		
		sb.append("\n");
		sb.append("Average time per entry = "+(String.format("%03d", (duration/this.nbSerialsInput)))+" milliseconds"+"\n");
		sb.append("Process took "+String.format("%d:%02d:%02d", durationInSeconds / 3600, (durationInSeconds % 3600) / 60, (durationInSeconds % 60))+" (started at "+sdf.format(this.startTime)+", ended at "+sdf.format(this.endTime)+")"+"\n");
		
		return sb.toString();
	}

}

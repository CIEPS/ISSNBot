package org.issn.issnbot.app.load_issn;

import org.issn.issnbot.app.CommandIfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadIssn implements CommandIfc {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	@Override
	public void execute(Object o) {
		log.info("Running command : "+this.getClass().getSimpleName());
	
	}

}

package org.issn.issnbot.app.version;

import org.issn.issnbot.app.CommandIfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Version implements CommandIfc {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	@Override
	public void execute(Object o) {
		System.out.println(this.getClass().getPackage().getImplementationTitle()+", version "+this.getClass().getPackage().getImplementationVersion());
	}

}

package org.issn.issnbot.app.clean_serials;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.issn.issnbot.app.AbstractBotArguments;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

@Parameters(
		commandDescription = "Clean previously imported data on items where the ISSN value is no more present or has been modified",
		separators = "="
)
public class ArgumentsCleanSerials extends AbstractBotArguments {
	
	
	/***
	 * Default constructor required
	 */
	public ArgumentsCleanSerials() {
		super();
	}

	@Parameter(
			names = { "output" },
			description = "Path to output directory. Defaults to 'output'",			
			converter = FileConverter.class
	)
	protected File output = new File("output");
	
	@Parameter(
			names = { "update" },
			description = "Send updates to Wikidata. Defaults to false. If set to true, real updates will be made in Wikidata, otherwise only the comparison report is generated.",
			required = false
	)
	protected boolean update = false;
	
	@Parameter(
			names = { "qid" },
			description = "A Wikidata qid to clean, e.g. 'Q123456'. The parameter can be repeated any number of times. If provided, only these list of QIDs will be cleaned."
	)
	protected List<String> qid = new ArrayList<>();

	public File getOutput() {
		return output;
	}

	public boolean isUpdate() {
		return update;
	}

	public List<String> getQid() {
		return qid;
	}
	
	
}

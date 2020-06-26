package org.issn.issnbot.app.clean_serials;

import java.io.File;

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

	public File getOutput() {
		return output;
	}

	public boolean isUpdate() {
		return update;
	}
	
	
}

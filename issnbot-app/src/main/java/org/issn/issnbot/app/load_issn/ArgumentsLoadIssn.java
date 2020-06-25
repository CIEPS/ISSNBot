package org.issn.issnbot.app.load_issn;

import java.io.File;

import org.issn.issnbot.app.AbstractBotArguments;
import org.issn.issnbot.app.ExistingFileValidator;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

@Parameters(
		commandDescription = "Load ISSN data to Wikidata",
		separators = "="
)
public class ArgumentsLoadIssn extends AbstractBotArguments {
	
	
	/***
	 * Default constructor required
	 */
	public ArgumentsLoadIssn() {
		super();
	}
	
	@Parameter(
			names = { "input" },
			description = "Path to input directory. Defaults to 'input'.",			
			converter = FileConverter.class,
			validateWith = ExistingFileValidator.class
	)
	protected File input = new File("input");

	@Parameter(
			names = { "output" },
			description = "Path to output directory. Defaults to 'output'",			
			converter = FileConverter.class
	)
	protected File output = new File("output");

	@Parameter(
			names = { "error" },
			description = "Path to error directory. Defaults to 'error'",			
			converter = FileConverter.class
	)
	protected File error = new File("error");
	
	@Parameter(
			names = { "update" },
			description = "Send updates to Wikidata. Defaults to false. If set to true, real updates will be made in Wikidata, otherwise only the comparison report is generated.",
			required = false
	)
	protected boolean update = false;
	
	@Parameter(
			names = { "limit" },
			description = "Limit the number of lines to be processed. Defaults to -1, which means no limit",			
			required = false
	)
	protected Integer limit = null;

	public File getInput() {
		return input;
	}

	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	public File getOutput() {
		return output;
	}

	public File getError() {
		return error;
	}

	public Integer getLimit() {
		return limit;
	}
	
}

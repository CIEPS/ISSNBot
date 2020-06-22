package org.issn.issnbot.app.load_issn;

import java.io.File;

import org.issn.issnbot.app.ExistingFileValidator;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

@Parameters(
		commandDescription = "Load ISSN data to Wikidata",
		separators = "="
)
public class ArgumentsLoadIssn {
	
	
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
			names = { "login" },
			description = "Wikidata login to use. Defaults to 'IssnBot'. Password must be provided in the 'password' System property",			
			required = false
	)
	protected String login = "IssnBot";
	
	@Parameter(
			names = { "maxLag" },
			description = "Sets the maxLag parameter on Wikidata connection in seconds. Defaults to 5. : \"make the client wait until the replication lag is less than the specified value\". See http://wikidata.github.io/Wikidata-Toolkit/org/wikidata/wdtk/wikibaseapi/WikibaseDataEditor.html#getMaxLag()",			
			required = false
	)
	protected Integer maxLag = null;
	
	@Parameter(
			names = { "maxLagBackoffFactor" },
			description = "Sets the maxLagBackoffFactor parameter on Wikidata connection. Defaults to 1.5. Multiplicator of wait time between each attempt. See http://wikidata.github.io/Wikidata-Toolkit/org/wikidata/wdtk/wikibaseapi/WikibaseDataEditor.html#setMaxLagBackOffFactor(double)",			
			required = false
	)
	protected Double maxLagBackoffFactor = null;
	
	@Parameter(
			names = { "maxLagMaxRetries" },
			description = "Sets the maxLagmaxRetries parameter on Wikidata connection. Defaults to 14. Number of retries when server is overloaded. See http://wikidata.github.io/Wikidata-Toolkit/org/wikidata/wdtk/wikibaseapi/WikibaseDataEditor.html#setMaxLagMaxRetries(int)",			
			required = false
	)
	protected Integer maxLagMaxRetries = null;
	
	@Parameter(
			names = { "maxLagFirstWaitTime" },
			description = "Sets the maxLagFirstWaitTime parameter on Wikidata connection in milliseconds. Defaults to 1000. See http://wikidata.github.io/Wikidata-Toolkit/org/wikidata/wdtk/wikibaseapi/WikibaseDataEditor.html#setMaxLagFirstWaitTime(int)",			
			required = false
	)
	protected Integer maxLagFirstWaitTime = null;
	
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

	public String getLogin() {
		return login;
	}

	public Integer getMaxLag() {
		return maxLag;
	}

	public Double getMaxLagBackoffFactor() {
		return maxLagBackoffFactor;
	}

	public Integer getMaxLagMaxRetries() {
		return maxLagMaxRetries;
	}

	public Integer getMaxLagFirstWaitTime() {
		return maxLagFirstWaitTime;
	}

	public Integer getLimit() {
		return limit;
	}
	
	
	
}

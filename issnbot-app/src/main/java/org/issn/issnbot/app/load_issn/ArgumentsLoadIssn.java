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
			description = "Path to input file",			
			converter = FileConverter.class,
			required = false,
			validateWith = ExistingFileValidator.class
	)
	protected File input = new File("input");
}

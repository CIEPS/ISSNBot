package org.issn.issnbot.app;

import org.issn.issnbot.app.clean_serials.ArgumentsCleanSerials;
import org.issn.issnbot.app.clean_serials.CleanSerials;
import org.issn.issnbot.app.load_issn.ArgumentsLoadIssn;
import org.issn.issnbot.app.load_issn.LoadIssn;
import org.issn.issnbot.app.version.ArgumentsVersion;
import org.issn.issnbot.app.version.Version;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.ParameterException;

import ch.qos.logback.classic.util.ContextInitializer;

public class Main {

	enum COMMAND {		
		
		LOAD_ISSN(new ArgumentsLoadIssn(), new LoadIssn()),
		CLEAN_SERIALS(new ArgumentsCleanSerials(), new CleanSerials()),
		VERSION(new ArgumentsVersion(), new Version()),
		;
		
		private CommandIfc command;
		private Object arguments;

		private COMMAND(Object arguments, CommandIfc command) {
			this.command = command;
			this.arguments = arguments;
		}

		public CommandIfc getCommand() {
			return command;
		}

		public Object getArguments() {
			return arguments;
		}		
	}
	
	private void run(String[] args) {
		ArgumentsMain main = new ArgumentsMain();
		JCommander jc = new JCommander(main);
		
		for (COMMAND aCOMMAND : COMMAND.values()) {
			jc.addCommand(aCOMMAND.name().toLowerCase(), aCOMMAND.getArguments());
		}
		// add a help command
		jc.addCommand("help", new ArgumentsHelp());
		
		try {
			jc.parse(args);
		// a mettre avant ParameterException car c'est une sous-exception
		} catch (MissingCommandException e) {
			// if no command was found, exit with usage message and error code
			System.err.println("ERROR : "+"Unkwown command.");
			jc.usage();
			System.exit(-1);
		} catch (ParameterException e) {
			System.err.println("ERROR : "+e.getMessage());
			if(jc.getParsedCommand() != null) {
				jc.usage();
			} else {
				jc.usage();
			}
			System.exit(-1);
		} 
		
		// configure logging
		if(main.getLog() != null) {
			System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, main.getLog().getAbsolutePath());
		}

		// if help was requested, print it and exit with a normal code
		if(main.isHelp()) {
			jc.usage();
			System.exit(0);
		}
		
		if(main.isVersion()) {
			COMMAND.VERSION.getCommand().execute(COMMAND.VERSION.getArguments());
			System.exit(0);
		}
		
		// if no command was found (0 parameters passed in command line)
		// exit with usage message and error code
		if(jc.getParsedCommand() == null) {
			System.err.println("No command found.");
			jc.usage();
			System.exit(-1);
		}
		
		if(jc.getParsedCommand().equals("help")) {
			jc.usage();
			System.exit(0);
		}
		
		// executes the command with the associated arguments
		COMMAND.valueOf(jc.getParsedCommand().toUpperCase()).getCommand().execute(
				COMMAND.valueOf(jc.getParsedCommand().toUpperCase()).getArguments()
		);
	}
	
	public static void main(String[] args) {
		Main me = new Main();
		me.run(args);
	}
	
	
}

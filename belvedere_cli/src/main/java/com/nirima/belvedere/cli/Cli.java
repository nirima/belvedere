package com.nirima.belvedere.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static org.kohsuke.args4j.OptionHandlerFilter.ALL;

public class Cli {



    public static void main(String[] args) throws IOException {
        new Cli().doMain(args);
    }

    @Argument(required=true,index=0,metaVar="action",usage="subcommands, e.g., {search|modify|delete}",handler= SubCommandHandler.class)
    @SubCommands({
            @SubCommand(name="convert",impl=ConvertCommand.class),
            @SubCommand(name="reverse",impl=ReverseCommand.class),
            @SubCommand(name="validate",impl=ValidateCommand.class),
    })
    protected Command action;
    

    public void doMain(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            // parse the arguments.
            parser.parseArgument(args);

            // you can parse additional arguments if you want.
            // parser.parseArgument("more","args");

            // after parsing arguments, you should check
            // if enough arguments are given.
           // if( arguments.isEmpty() )
            //    throw new CmdLineException(parser,"No argument is given");

            // Convert it
            //DSLExec dsl = new DSLExec(getClass().getResource("test.api"));

            action.execute();


        } catch( CmdLineException e ) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java belvedere [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println("  Example: java belvedere"+parser.printExample(ALL));

            return;
        }

    }

    public void convert() throws MalformedURLException {

    }

    public void reverse() throws JsonProcessingException, MalformedURLException {

        
    }
}

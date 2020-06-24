package com.nirima.belvedere.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

public abstract class Command {

    protected boolean readFromStdin = false;

    @Option(name="-f", aliases="--file", usage="Fully qualified path and name of file.")
    protected File in;
    
    @Option(name = "-", usage = "Read input from stdin instead of from a file.")
    public void setReadFromStdin(boolean readFromStdin) throws CmdLineException {
        if (in != null) {
            throw new CmdLineException("Don't use the - option (for reading from stdin) when you also specify an input file.");
        }
        this.readFromStdin = readFromStdin;
    }
    @Option(name="-o",usage="output to this file",metaVar="OUTPUT")
    protected File out = null;

    public abstract void execute() throws IOException;
}

/*
Copyright 2014 Google Inc. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.google.cloud.genomics.benchmarker;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;


public class CommandLine {

  CmdLineParser parser;

  @Argument
  public List<String> remainingArgs = new ArrayList<String>();

  @Option(name = "--help",
      usage = "display this help message")
  public boolean help = false;

  @Option(name = "--port",
      metaVar = "<port>",
      usage = "set the port for this server")
  public Integer port = 9000;

  public CommandLine(String[] args) throws CmdLineException {
    parser = new CmdLineParser(this);
    parser.parseArgument(args);
  }

  public boolean showHelp() {
    return help;
  }

  public void printHelp(Appendable out) throws IOException {
    out.append(getUsage());
  }

  public String getUsage() {
    StringWriter sw = new StringWriter();
    sw.append("Usage: GenomicsBenchmarker [flags...]\n");
    parser.printUsage(sw, null);
    return sw.toString();
  }

}

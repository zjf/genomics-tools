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
package com.google.cloud.genomics.localrepo;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLineArguments {

  private static final Option DATASET_OPTION =
      new Option("d", "dataset", true, "Dataset in <id>:<path> form");
  private static final Options OPTIONS = new Options().addOption(DATASET_OPTION);
  private static final Parser PARSER = new GnuParser();
  private static final String REGEX = "^([\\p{Alpha}$_][\\p{Alnum}$_]*):(.+)$";
  private static final Pattern PATTERN = Pattern.compile(REGEX);

  public static CommandLineArguments parse(String... args) {
    try {
      CommandLine commandLine = PARSER.parse(OPTIONS, args);
      String opt = DATASET_OPTION.getOpt();
      ImmutableCollection.Builder<DatasetDirectory> datasets = ImmutableSet.builder();
      if (commandLine.hasOption(opt)) {
        for (String value : commandLine.getOptionValues(opt)) {
          Matcher matcher = PATTERN.matcher(value);
          if (matcher.matches()) {
            datasets.add(DatasetDirectory.create(matcher.group(1), matcher.group(2)));
          } else {
            throw new IllegalArgumentException(String.format(
                "Flag \"%s\": \"%s\" does not match regex \"%s\"",
                DATASET_OPTION.getLongOpt(),
                value,
                REGEX));
          }
        }
      }
      return new CommandLineArguments(datasets.build());
    } catch (ParseException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  public Collection<DatasetDirectory> getDatasets() {
    return datasets;
  }

  private CommandLineArguments(Collection<DatasetDirectory> datasets) {
    this.datasets = datasets;
  }

  private final Collection<DatasetDirectory> datasets;
}

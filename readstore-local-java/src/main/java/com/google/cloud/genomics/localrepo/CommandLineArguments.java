/*
 *Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.genomics.localrepo;

import com.google.cloud.genomics.localrepo.util.Suppliers;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandLineArguments {

  private enum Flag {

    DATASET(
        'd',
        "dataset",
        "A dataset in <id>:<directory> form",
        "^([\\p{Alpha}$_][\\p{Alnum}$_]*):(.+)$",
        results -> results.stream()
            .collect(Collectors.toMap(result -> result.group(1), result -> result.group(2)))),

    PORT(
        'p',
        "port",
        "The port to run the server on",
        "^(\\p{Digit}+)$",
        results -> {
          switch (results.size()) {
            case 0:
              return Optional.empty();
            case 1:
              return Optional.of(Integer.parseInt(results.get(0).group(1)));
            default:
              throw new IllegalArgumentException("Flag 'port' can only appear once");
          }
        });

    private static final Supplier<Options> OPTIONS = Suppliers.memoize(() -> {
      Options options = new Options();
      for (Flag flag : values()) {
        options.addOption(flag.option);
      }
      return options;
    });

    private static final Parser PARSER = new GnuParser();

    static CommandLine parse(String[] args) {
      try {
        return PARSER.parse(OPTIONS.get(), args);
      } catch (ParseException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      }
    }

    private final String longOpt;
    private final char opt;
    private final Option option;
    private final Pattern pattern;
    private final String regex;
    private final Function<List<MatchResult>, Object> getValue;

    private Flag(
        char opt,
        String longOpt,
        String description,
        String regex,
        Function<List<MatchResult>, Object> getValue) {
      this.option =
          new Option(Character.toString(this.opt = opt), this.longOpt = longOpt, true, description);
      this.pattern = Pattern.compile(this.regex = regex);
      this.getValue = getValue;
    }

    final Object getValue(CommandLine commandLine) {
      List<MatchResult> results = new ArrayList<>();
      if (commandLine.hasOption(opt)) {
        for (String value : commandLine.getOptionValues(opt)) {
          Matcher matcher = pattern.matcher(value);
          if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format(
                "Flag '%s': \"%s\" doesn't match regex \"%s\"",
                longOpt,
                value,
                regex));
          }
          results.add(matcher);
        }
      }
      return getValue.apply(results);
    }
  }

  @SuppressWarnings("unchecked")
  public static CommandLineArguments parse(String... args) {
    CommandLine commandLine = Flag.parse(args);
    return new CommandLineArguments(
        (Optional<Integer>) Flag.PORT.getValue(commandLine),
        (Map<String, String>) Flag.DATASET.getValue(commandLine));
  }

  private final Map<String, String> datasets;
  private final Optional<Integer> port;

  private CommandLineArguments(Optional<Integer> port, Map<String, String> datasets) {
    this.port = port;
    this.datasets = datasets;
  }

  public Map<String, String> getDatasets() {
    return datasets;
  }

  public Optional<Integer> getPort() {
    return port;
  }
}

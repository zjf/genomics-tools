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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLineArguments {

  private enum Flag {

    DATASET(
        'd',
        "dataset",
        "A dataset in <id>:<directory> form",
        "^([\\p{Alpha}$_][\\p{Alnum}$_]*):(.+)$") {
      @Override Object getValue(List<MatchResult> results) {
        return Maps.transformValues(FluentIterable.from(results).uniqueIndex(GROUP_1), GROUP_2);
      }
    },

    PORT(
        'p',
        "port",
        "The port to run the server on",
        "^(\\p{Digit}+)$") {
      @Override Object getValue(List<MatchResult> results) {
        switch (results.size()) {
          case 0:
            return Optional.<Integer>absent();
          case 1:
            return Optional.of(Integer.parseInt(GROUP_1.apply(Iterables.getOnlyElement(results))));
          default:
            throw new IllegalArgumentException("Flag 'port' can only appear once");
        }
      }
    };

    private static final Function<MatchResult, String> GROUP_1 = groupFunction(1);
    private static final Function<MatchResult, String> GROUP_2 = groupFunction(2);

    private static final Supplier<Options> OPTIONS = Suppliers.memoize(
        new Supplier<Options>() {
          @Override public Options get() {
            Options options = new Options();
            for (Flag flag : values()) {
              options.addOption(flag.option);
            }
            return options;
          }
        });

    private static final Parser PARSER = new GnuParser();

    private static Function<MatchResult, String> groupFunction(final int group) {
      return
          new Function<MatchResult, String>() {
            @Override public String apply(MatchResult result) {
              return result.group(group);
            }
          };
    }

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

    private Flag(char opt, String longOpt, String description, String regex) {
      option =
          new Option(Character.toString(this.opt = opt), this.longOpt = longOpt, true, description);
      pattern = Pattern.compile(this.regex = regex);
    }

    final Object getValue(CommandLine commandLine) {
      ImmutableList.Builder<MatchResult> results = ImmutableList.builder();
      if (commandLine.hasOption(opt)) {
        for (String value : commandLine.getOptionValues(opt)) {
          Matcher matcher = pattern.matcher(value);
          if (!matcher.matches()) {
            throw new IllegalArgumentException(
                String.format("Flag '%s': \"%s\" doesn't match regex \"%s\"", longOpt, value, regex));
          }
          results.add(matcher);
        }
      }
      return getValue(results.build());
    }

    abstract Object getValue(List<MatchResult> results);
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

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

import com.google.cloud.genomics.localrepo.dto.Dataset;
import com.google.cloud.genomics.localrepo.dto.Read;
import com.google.cloud.genomics.localrepo.dto.Readset;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class Backend {

  public static Backend create(Collection<DatasetDirectory> datasets) {
    return new Backend(
        FluentIterable.from(datasets).uniqueIndex(DatasetDirectory.GET_DATASET_ID),
        union(FluentIterable.from(datasets).transform(DatasetDirectory.GET_READSETS)));
  }

  private static <X, Y> Map<X, Y> union(Iterable<Map<X, Y>> maps) {
    ImmutableMap.Builder<X, Y> union = ImmutableMap.builder();
    for (Map<X, Y> map : maps) {
      union.putAll(map);
    }
    return union.build();
  }

  private static final Function<DatasetDirectory, Set<String>> GET_READSET_IDS =
      new Function<DatasetDirectory, Set<String>>() {
        @Override public Set<String> apply(DatasetDirectory dataset) {
          return dataset.getReadsets().keySet();
        }
      };

  private final Function<String, Set<String>> getReadsetIds;
  private final Map<String, DatasetDirectory> datasets;
  private final Map<String, BamFilesReadset> readsets;
  private final Map<String, String> readsetIdsBySample;

  private Backend(
      final Map<String, DatasetDirectory> datasets,
      Map<String, BamFilesReadset> readsets) {
    this.datasets = datasets;
    this.readsets = readsets;
    this.getReadsetIds =
        new Function<String, Set<String>>() {
          @Override public Set<String> apply(String datasetId) {
            return Optional.fromNullable(datasets.get(datasetId))
                .transform(GET_READSET_IDS)
                .or(Optional.of(Collections.<String>emptySet()))
                .get();
          }
        };
    this.readsetIdsBySample = Maps.transformValues(
        FluentIterable.from(readsets.values()).uniqueIndex(BamFilesReadset.GET_SAMPLE),
        BamFilesReadset.GET_READSET_ID);
  }

  public Optional<Dataset> getDataset(String datasetId) {
    return Optional.fromNullable(datasets.get(datasetId)).transform(DatasetDirectory.GET_DATASET);
  }

  public Optional<Readset> getReadset(String readsetId) {
    return Optional.fromNullable(readsets.get(readsetId)).transform(BamFilesReadset.GET_READSET);
  }

  public FluentIterable<Dataset> listDatasets() {
    return FluentIterable.from(datasets.values()).transform(DatasetDirectory.GET_DATASET);
  }

  public <X> X searchReads(
      Collection<String> datasetIds,
      final Collection<String> readsetIds,
      String sequenceName,
      Optional<Long> start,
      Optional<Long> end,
      Function<FluentIterable<Read>, X> callback) {
    Query.Builder builder = Query.builder(readsetIdsBySample, readsetIds, sequenceName);
    if (start.isPresent()) {
      builder.setStart(start.get().intValue());
    }
    if (end.isPresent()) {
      builder.setEnd(end.get().intValue());
    }
    return builder.build(callback).search(FluentIterable
        .from(datasetIds.isEmpty() ? datasets.keySet() : datasetIds)
        .transformAndConcat(readsetIds.isEmpty()
            ? getReadsetIds
            : new Function<String, Collection<String>>() {
                @Override public Collection<String> apply(String input) {
                  return readsetIds;
                }
              })
        .transform(Functions.forMap(readsets))
        .transformAndConcat(BamFilesReadset.GET_BAM_FILES));
  }

  public FluentIterable<Readset> searchReadsets(Collection<String> datasetIds) {
    return FluentIterable.from(readsets.values())
        .filter(datasetIds.isEmpty()
            ? Predicates.<BamFilesReadset>alwaysTrue()
            : Predicates.compose(Predicates.in(datasetIds), BamFilesReadset.GET_DATASET_ID))
        .transform(BamFilesReadset.GET_READSET);
  }
}

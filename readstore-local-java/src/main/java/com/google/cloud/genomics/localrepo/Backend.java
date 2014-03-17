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
import com.google.cloud.genomics.localrepo.dto.Readset;
import com.google.cloud.genomics.localrepo.dto.SearchReadsRequest;
import com.google.cloud.genomics.localrepo.dto.SearchReadsResponse;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Map;

public class Backend {

  public static Backend create(Collection<DatasetDirectory> datasets, int pageSize) {
    return new Backend(
        FluentIterable.from(datasets).uniqueIndex(DatasetDirectory.GET_DATASET_ID),
        union(FluentIterable.from(datasets).transform(DatasetDirectory.GET_READSETS)),
        pageSize);
  }

  private static <X, Y> Map<X, Y> union(Iterable<Map<X, Y>> maps) {
    ImmutableMap.Builder<X, Y> union = ImmutableMap.builder();
    for (Map<X, Y> map : maps) {
      union.putAll(map);
    }
    return union.build();
  }

  private final Map<String, DatasetDirectory> datasets;
  private final Map<String, BamFilesReadset> readsets;
  private final QueryEngine queryEngine;

  private Backend(
      final Map<String, DatasetDirectory> datasets,
      Map<String, BamFilesReadset> readsets,
      int pageSize) {
    this.datasets = datasets;
    this.readsets = readsets;
    this.queryEngine = QueryEngine.create(datasets, readsets, pageSize);
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

  public SearchReadsResponse searchReads(SearchReadsRequest request) {
    return queryEngine.searchReads(request);
  }

  public FluentIterable<Readset> searchReadsets(Collection<String> datasetIds) {
    return FluentIterable.from(readsets.values())
        .filter(datasetIds.isEmpty()
            ? Predicates.<BamFilesReadset>alwaysTrue()
            : Predicates.compose(Predicates.in(datasetIds), BamFilesReadset.GET_DATASET_ID))
        .transform(BamFilesReadset.GET_READSET);
  }
}

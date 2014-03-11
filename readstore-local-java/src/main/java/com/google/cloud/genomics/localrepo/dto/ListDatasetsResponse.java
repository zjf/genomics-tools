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
package com.google.cloud.genomics.localrepo.dto;

import com.google.cloud.genomics.localrepo.DataTransferObject;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class ListDatasetsResponse extends DataTransferObject {

  @JsonCreator public static ListDatasetsResponse create(
      @JsonProperty("datasets") List<Dataset> datasets,
      @JsonProperty("nextPageToken") String nextPageToken) {
    return new ListDatasetsResponse(datasets, nextPageToken);
  }

  private final List<Dataset> datasets;
  private final String nextPageToken;

  private ListDatasetsResponse(List<Dataset> datasets, String nextPageToken) {
    this.datasets = datasets;
    this.nextPageToken = nextPageToken;
  }

  public List<Dataset> getDatasets() {
    return datasets;
  }

  public String getNextPageToken() {
    return nextPageToken;
  }
}

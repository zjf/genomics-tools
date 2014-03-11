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

public class SearchReadsetsRequest extends DataTransferObject {

  @JsonCreator public static SearchReadsetsRequest create(
      @JsonProperty("datasetIds") List<String> datasetIds,
      @JsonProperty("pageToken") String pageToken) {
    return new SearchReadsetsRequest(datasetIds, pageToken);
  }

  private final List<String> datasetIds;
  private final String pageToken;

  private SearchReadsetsRequest(
      List<String> datasetIds,
      String pageToken) {
    this.datasetIds = datasetIds;
    this.pageToken = pageToken;
  }

  public List<String> getDatasetIds() {
    return datasetIds;
  }

  public String getPageToken() {
    return pageToken;
  }
}

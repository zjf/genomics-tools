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

public class SearchReadsRequest extends DataTransferObject {

  private static final ReflectiveHashCodeAndEquals<SearchReadsRequest> HASH_CODE_AND_EQUALS =
      ReflectiveHashCodeAndEquals.create(SearchReadsRequest.class);

  @JsonCreator public static SearchReadsRequest create(
      @JsonProperty("datasetIds") List<String> datasetIds,
      @JsonProperty("readsetIds") List<String> readsetIds,
      @JsonProperty("sequenceName") String sequenceName,
      @JsonProperty("sequenceStart") Long sequenceStart,
      @JsonProperty("sequenceEnd") Long sequenceEnd,
      @JsonProperty("pageToken") String pageToken) {
    return new SearchReadsRequest(
        datasetIds,
        readsetIds,
        sequenceName,
        sequenceStart,
        sequenceEnd,
        pageToken);
  }

  private final List<String> datasetIds;
  private final String pageToken;
  private final List<String> readsetIds;
  private final Long sequenceEnd;
  private final String sequenceName;
  private final Long sequenceStart;

  private SearchReadsRequest(
      List<String> datasetIds,
      List<String> readsetIds,
      String sequenceName,
      Long sequenceStart,
      Long sequenceEnd,
      String pageToken) {
    this.datasetIds = datasetIds;
    this.readsetIds = readsetIds;
    this.sequenceName = sequenceName;
    this.sequenceStart = sequenceStart;
    this.sequenceEnd = sequenceEnd;
    this.pageToken = pageToken;
  }

  @Override public boolean equals(Object obj) {
    return HASH_CODE_AND_EQUALS.equals(this, obj);
  }

  public List<String> getDatasetIds() {
    return datasetIds;
  }

  public String getPageToken() {
    return pageToken;
  }

  public List<String> getReadsetIds() {
    return readsetIds;
  }

  public Long getSequenceEnd() {
    return sequenceEnd;
  }

  public String getSequenceName() {
    return sequenceName;
  }

  public Long getSequenceStart() {
    return sequenceStart;
  }

  @Override public int hashCode() {
    return HASH_CODE_AND_EQUALS.hashCode(this);
  }
}

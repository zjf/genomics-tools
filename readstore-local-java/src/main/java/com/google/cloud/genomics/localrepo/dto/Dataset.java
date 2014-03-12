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

public class Dataset extends DataTransferObject {

  private static final ReflectiveHashCodeAndEquals<Dataset> HASH_CODE_AND_EQUALS =
      ReflectiveHashCodeAndEquals.create(Dataset.class);

  @JsonCreator public static Dataset create(
      @JsonProperty("id") String id,
      @JsonProperty("projectId") long projectId,
      @JsonProperty("isPublic") boolean isPublic) {
    return new Dataset(id, projectId, isPublic);
  }

  private final String id;
  private final boolean isPublic;
  private final long projectId;

  private Dataset(String id, long projectId, boolean isPublic) {
    this.id = id;
    this.projectId = projectId;
    this.isPublic = isPublic;
  }

  @Override public boolean equals(Object obj) {
    return HASH_CODE_AND_EQUALS.equals(this, obj);
  }

  public String getId() {
    return id;
  }

  public long getProjectId() {
    return projectId;
  }

  @Override public int hashCode() {
    return HASH_CODE_AND_EQUALS.hashCode(this);
  }

  public boolean isIsPublic() {
    return isPublic;
  }
}

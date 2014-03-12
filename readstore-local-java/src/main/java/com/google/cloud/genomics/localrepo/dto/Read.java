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

import java.util.Map;

public class Read extends DataTransferObject {

  private static final ReflectiveHashCodeAndEquals<Read> HASH_CODE_AND_EQUALS =
      ReflectiveHashCodeAndEquals.create(Read.class);

  @JsonCreator public static Read create(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("readsetId") String readsetId,
      @JsonProperty("flags") int flags,
      @JsonProperty("referenceSequenceName") String referenceSequenceName,
      @JsonProperty("position") Integer position,
      @JsonProperty("mappingQuality") Integer mappingQuality,
      @JsonProperty("cigar") String cigar,
      @JsonProperty("mateReferenceSequenceName") String mateReferenceSequenceName,
      @JsonProperty("matePosition") Integer matePosition,
      @JsonProperty("templateLength") Integer templateLength,
      @JsonProperty("originalBases") String originalBases,
      @JsonProperty("alignedBases") String alignedBases,
      @JsonProperty("baseQuality") String baseQuality,
      @JsonProperty("tags") Map<String, String> tags) {
    return new Read(
        id,
        name,
        readsetId,
        flags,
        referenceSequenceName,
        position,
        mappingQuality,
        cigar,
        mateReferenceSequenceName,
        matePosition,
        templateLength,
        originalBases,
        alignedBases,
        baseQuality,
        tags);
  }

  private final String alignedBases;
  private final String baseQuality;
  private final String cigar;
  private final int flags;
  private final String id;
  private final Integer mappingQuality;
  private final Integer matePosition;
  private final String mateReferenceSequenceName;
  private final String name;
  private final String originalBases;
  private final Integer position;
  private final String readsetId;
  private final String referenceSequenceName;
  private final Map<String, String> tags;
  private final Integer templateLength;

  private Read(
      String id,
      String name,
      String readsetId,
      int flags,
      String referenceSequenceName,
      Integer position,
      Integer mappingQuality,
      String cigar,
      String mateReferenceSequenceName,
      Integer matePosition,
      Integer templateLength,
      String originalBases,
      String alignedBases,
      String baseQuality,
      Map<String, String> tags) {
    this.id = id;
    this.name = name;
    this.readsetId = readsetId;
    this.flags = flags;
    this.referenceSequenceName = referenceSequenceName;
    this.position = position;
    this.mappingQuality = mappingQuality;
    this.cigar = cigar;
    this.mateReferenceSequenceName = mateReferenceSequenceName;
    this.matePosition = matePosition;
    this.templateLength = templateLength;
    this.originalBases = originalBases;
    this.alignedBases = alignedBases;
    this.baseQuality = baseQuality;
    this.tags = tags;
  }

  @Override public boolean equals(Object obj) {
    return HASH_CODE_AND_EQUALS.equals(this, obj);
  }

  public String getAlignedBases() {
    return alignedBases;
  }

  public String getBaseQuality() {
    return baseQuality;
  }

  public String getCigar() {
    return cigar;
  }

  public int getFlags() {
    return flags;
  }

  public String getId() {
    return id;
  }

  public Integer getMappingQuality() {
    return mappingQuality;
  }

  public Integer getMatePosition() {
    return matePosition;
  }

  public String getMateReferenceSequenceName() {
    return mateReferenceSequenceName;
  }

  public String getName() {
    return name;
  }

  public String getOriginalBases() {
    return originalBases;
  }

  public Integer getPosition() {
    return position;
  }

  public String getReadsetId() {
    return readsetId;
  }

  public String getReferenceSequenceName() {
    return referenceSequenceName;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public Integer getTemplateLength() {
    return templateLength;
  }

  @Override public int hashCode() {
    return HASH_CODE_AND_EQUALS.hashCode(this);
  }
}

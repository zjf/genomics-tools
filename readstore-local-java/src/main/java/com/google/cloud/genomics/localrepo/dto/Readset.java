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
import com.google.common.base.Function;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class Readset extends DataTransferObject {

  public static class FileData extends DataTransferObject {

    public static class Header extends DataTransferObject {

      private static final ReflectiveHashCodeAndEquals<Header> HASH_CODE_AND_EQUALS =
          ReflectiveHashCodeAndEquals.create(Header.class);

      @JsonCreator public static Header create(
          @JsonProperty("version") String version,
          @JsonProperty("sortingOrder") String sortingOrder) {
        return new Header(version, sortingOrder);
      }

      private final String sortingOrder;
      private final String version;

      private Header(
          String version,
          String sortingOrder) {
        this.version = version;
        this.sortingOrder = sortingOrder;
      }

      @Override public boolean equals(Object obj) {
        return HASH_CODE_AND_EQUALS.equals(this, obj);
      }

      public String getSortingOrder() {
        return sortingOrder;
      }

      public String getVersion() {
        return version;
      }

      @Override public int hashCode() {
        return HASH_CODE_AND_EQUALS.hashCode(this);
      }
    }

    public static class Program extends DataTransferObject {

      private static final ReflectiveHashCodeAndEquals<Program> HASH_CODE_AND_EQUALS =
          ReflectiveHashCodeAndEquals.create(Program.class);

      @JsonCreator public static Program create(
          @JsonProperty("id") String id,
          @JsonProperty("name") String name,
          @JsonProperty("commandLine") String commandLine,
          @JsonProperty("prevProgramId") String prevProgramId,
          @JsonProperty("version") String version) {
        return new Program(id, name, commandLine, prevProgramId, version);
      }

      private final String commandLine;
      private final String id;
      private final String name;
      private final String prevProgramId;
      private final String version;

      private Program(
          String id,
          String name,
          String commandLine,
          String prevProgramId,
          String version) {
        this.id = id;
        this.name = name;
        this.commandLine = commandLine;
        this.prevProgramId = prevProgramId;
        this.version = version;
      }

      @Override public boolean equals(Object obj) {
        return HASH_CODE_AND_EQUALS.equals(this, obj);
      }

      public String getCommandLine() {
        return commandLine;
      }

      public String getId() {
        return id;
      }

      public String getName() {
        return name;
      }

      public String getPrevProgramId() {
        return prevProgramId;
      }

      public String getVersion() {
        return version;
      }

      @Override public int hashCode() {
        return HASH_CODE_AND_EQUALS.hashCode(this);
      }
    }

    public static class ReadGroup extends DataTransferObject {

      public static final Function<ReadGroup, String> GET_ID =
          new Function<ReadGroup, String>() {
            @Override public String apply(ReadGroup readGroup) {
              return readGroup.getId();
            }
          };

      private static final ReflectiveHashCodeAndEquals<ReadGroup> HASH_CODE_AND_EQUALS =
          ReflectiveHashCodeAndEquals.create(ReadGroup.class);

      @JsonCreator public static ReadGroup create(
          @JsonProperty("id") String id,
          @JsonProperty("sequencingCenterName") String sequencingCenterName,
          @JsonProperty("description") String description,
          @JsonProperty("date") String date,
          @JsonProperty("flowOrder") String flowOrder,
          @JsonProperty("keySequence") String keySequence,
          @JsonProperty("library") String library,
          @JsonProperty("processingProgram") String processingProgram,
          @JsonProperty("predictedInsertSize") Integer predictedInsertSize,
          @JsonProperty("sequencingTechnology") String sequencingTechnology,
          @JsonProperty("platformUnit") String platformUnit,
          @JsonProperty("sample") String sample) {
        return new ReadGroup(
            id,
            sequencingCenterName,
            description,
            date,
            flowOrder,
            keySequence,
            library,
            processingProgram,
            predictedInsertSize,
            sequencingTechnology,
            platformUnit,
            sample);
      }

      private final String date;
      private final String description;
      private final String flowOrder;
      private final String id;
      private final String keySequence;
      private final String library;
      private final String platformUnit;
      private final Integer predictedInsertSize;
      private final String processingProgram;
      private final String sample;
      private final String sequencingCenterName;
      private final String sequencingTechnology;

      private ReadGroup(
          String id,
          String sequencingCenterName,
          String description,
          String date,
          String flowOrder,
          String keySequence,
          String library,
          String processingProgram,
          Integer predictedInsertSize,
          String sequencingTechnology,
          String platformUnit,
          String sample) {
        this.id = id;
        this.sequencingCenterName = sequencingCenterName;
        this.description = description;
        this.date = date;
        this.flowOrder = flowOrder;
        this.keySequence = keySequence;
        this.library = library;
        this.processingProgram = processingProgram;
        this.predictedInsertSize = predictedInsertSize;
        this.sequencingTechnology = sequencingTechnology;
        this.platformUnit = platformUnit;
        this.sample = sample;
      }

      @Override public boolean equals(Object obj) {
        return HASH_CODE_AND_EQUALS.equals(this, obj);
      }

      public String getDate() {
        return date;
      }

      public String getDescription() {
        return description;
      }

      public String getFlowOrder() {
        return flowOrder;
      }

      public String getId() {
        return id;
      }

      public String getKeySequence() {
        return keySequence;
      }

      public String getLibrary() {
        return library;
      }

      public String getPlatformUnit() {
        return platformUnit;
      }

      public Integer getPredictedInsertSize() {
        return predictedInsertSize;
      }

      public String getProcessingProgram() {
        return processingProgram;
      }

      public String getSample() {
        return sample;
      }

      public String getSequencingCenterName() {
        return sequencingCenterName;
      }

      public String getSequencingTechnology() {
        return sequencingTechnology;
      }

      @Override public int hashCode() {
        return HASH_CODE_AND_EQUALS.hashCode(this);
      }
    }

    public static class RefSequence extends DataTransferObject {

      private static final ReflectiveHashCodeAndEquals<RefSequence> HASH_CODE_AND_EQUALS =
          ReflectiveHashCodeAndEquals.create(RefSequence.class);

      @JsonCreator public static RefSequence create(
          @JsonProperty("name") String name,
          @JsonProperty("length") int length,
          @JsonProperty("assemblyId") String assemblyId,
          @JsonProperty("md5Checksum") String md5Checksum,
          @JsonProperty("species") String species,
          @JsonProperty("uri") String uri) {
        return new RefSequence(name, length, assemblyId, md5Checksum, species, uri);
      }

      private final String assemblyId;
      private final int length;
      private final String md5Checksum;
      private final String name;
      private final String species;
      private final String uri;

      private RefSequence(
          String name,
          int length,
          String assemblyId,
          String md5Checksum,
          String species,
          String uri) {
        this.name = name;
        this.length = length;
        this.assemblyId = assemblyId;
        this.md5Checksum = md5Checksum;
        this.species = species;
        this.uri = uri;
      }

      @Override public boolean equals(Object obj) {
        return HASH_CODE_AND_EQUALS.equals(this, obj);
      }

      public String getAssemblyId() {
        return assemblyId;
      }

      public int getLength() {
        return length;
      }

      public String getMd5Checksum() {
        return md5Checksum;
      }

      public String getName() {
        return name;
      }

      public String getSpecies() {
        return species;
      }

      public String getUri() {
        return uri;
      }

      @Override public int hashCode() {
        return HASH_CODE_AND_EQUALS.hashCode(this);
      }
    }

    private static final ReflectiveHashCodeAndEquals<FileData> HASH_CODE_AND_EQUALS =
        ReflectiveHashCodeAndEquals.create(FileData.class);

    @JsonCreator public static FileData create(
        @JsonProperty("fileUri") String fileUri,
        @JsonProperty("headers") List<Header> headers,
        @JsonProperty("refSequences") List<RefSequence> refSequences,
        @JsonProperty("readGroups") List<ReadGroup> readGroups,
        @JsonProperty("programs") List<Program> programs,
        @JsonProperty("comments") List<String> comments) {
      return new FileData(fileUri, headers, refSequences, readGroups, programs, comments);
    }

    private final List<String> comments;
    private final String fileUri;
    private final List<Header> headers;
    private final List<Program> programs;
    private final List<ReadGroup> readGroups;
    private final List<RefSequence> refSequences;

    private FileData(
        String fileUri,
        List<Header> headers,
        List<RefSequence> refSequences,
        List<ReadGroup> readGroups,
        List<Program> programs,
        List<String> comments) {
      this.fileUri = fileUri;
      this.headers = headers;
      this.refSequences = refSequences;
      this.readGroups = readGroups;
      this.programs = programs;
      this.comments = comments;
    }

    @Override public boolean equals(Object obj) {
      return HASH_CODE_AND_EQUALS.equals(this, obj);
    }

    public List<String> getComments() {
      return comments;
    }

    public String getFileUri() {
      return fileUri;
    }

    public List<Header> getHeaders() {
      return headers;
    }

    public List<Program> getPrograms() {
      return programs;
    }

    public List<ReadGroup> getReadGroups() {
      return readGroups;
    }

    public List<RefSequence> getRefSequences() {
      return refSequences;
    }

    @Override public int hashCode() {
      return HASH_CODE_AND_EQUALS.hashCode(this);
    }
  }

  private static final ReflectiveHashCodeAndEquals<Readset> HASH_CODE_AND_EQUALS =
      ReflectiveHashCodeAndEquals.create(Readset.class);

  @JsonCreator public static Readset create(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("datasetId") String datasetId,
      @JsonProperty("created") long created,
      @JsonProperty("fileData") List<FileData> fileData) {
    return new Readset(id, name, datasetId, created, fileData);
  }

  private final long created;
  private final String datasetId;
  private final List<FileData> fileData;
  private final String id;
  private final String name;

  private Readset(
      String id,
      String name,
      String datasetId,
      long created,
      List<FileData> fileData) {
    this.id = id;
    this.name = name;
    this.datasetId = datasetId;
    this.created = created;
    this.fileData = fileData;
  }

  @Override public boolean equals(Object obj) {
    return HASH_CODE_AND_EQUALS.equals(this, obj);
  }

  public long getCreated() {
    return created;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public List<FileData> getFileData() {
    return fileData;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override public int hashCode() {
    return HASH_CODE_AND_EQUALS.hashCode(this);
  }
}

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

import static org.junit.Assert.assertEquals;

import com.google.cloud.genomics.localrepo.dto.Dataset;
import com.google.cloud.genomics.localrepo.dto.ListDatasetsResponse;
import com.google.cloud.genomics.localrepo.dto.Read;
import com.google.cloud.genomics.localrepo.dto.Readset;
import com.google.cloud.genomics.localrepo.dto.SearchReadsRequest;
import com.google.cloud.genomics.localrepo.dto.SearchReadsResponse;
import com.google.cloud.genomics.localrepo.dto.SearchReadsetsRequest;
import com.google.cloud.genomics.localrepo.dto.SearchReadsetsResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.joda.time.DateTimeUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;

@RunWith(JUnit4.class)
public class ServerTest {

  private static final String
      DATASET_ID = "datasetId";
  private static final DatasetDirectory
      DATASET_DIR = DatasetDirectory.create(DATASET_ID, "testdata");
  private static final Dataset
      DATASET = DATASET_DIR.getDataset();
  private static final String
      USER_DIR = System.getProperty("user.dir");
  private static final List<Readset.FileData.Header>
      HEADERS = Arrays.asList(createHeader("1.0", "coordinate"));
  private static final List<Readset.FileData.RefSequence>
      REF_SEQUENCES = Arrays.asList(createRefSequence("reference", 10));
  private static final Readset.FileData.ReadGroup
      READ_GROUP_1 = createReadGroup("readgroup1", "sample1"),
      READ_GROUP_2 = createReadGroup("readgroup2", "sample1"),
      READ_GROUP_3 = createReadGroup("readgroup3", "sample2"),
      READ_GROUP_4 = createReadGroup("readgroup4", "sample3"),
      READ_GROUP_5 = createReadGroup("readgroup5", "sample3");
  private static final List<Readset.FileData>
      FILE_DATA = Arrays.asList(
          Readset.FileData.create(
              String.format("file://%s/testdata/bam1.bam", USER_DIR),
              HEADERS,
              REF_SEQUENCES,
              Arrays.asList(READ_GROUP_1, READ_GROUP_2, READ_GROUP_3, READ_GROUP_4),
              Collections.<Readset.FileData.Program>emptyList(),
              Collections.<String>emptyList()),
          Readset.FileData.create(
              String.format("file://%s/testdata/bam2.bam", USER_DIR),
              HEADERS,
              REF_SEQUENCES,
              Arrays.asList(READ_GROUP_2, READ_GROUP_3, READ_GROUP_4, READ_GROUP_5),
              Collections.<Readset.FileData.Program>emptyList(),
              Collections.<String>emptyList()));
  private static final long
      NOW = System.currentTimeMillis();
  private static final Map<Readset, SearchReadsResponse>
      SEARCH_READS_RESPONSES = ImmutableMap.of(
          createReadset("1", "sample1"),
          SearchReadsResponse.create(
              Arrays.asList(
                  createRead("read1", "1", 0, "reference", 1, "2M", "readgroup1"),
                  createRead("read5", "1", 0, "reference", 2, "2M", "readgroup2"),
                  createRead("read2", "1", 0, "reference", 3, "2M", "readgroup2")),
              null),
          createReadset("2", "sample2"),
          SearchReadsResponse.create(
              Arrays.asList(
                  createRead("read6", "2", 0, "reference", 4, "2M", "readgroup3"),
                  createRead("read3", "2", 0, "reference", 5, "2M", "readgroup3")),
              null),
          createReadset("3", "sample3"),
          SearchReadsResponse.create(
              Arrays.asList(
                  createRead("read7", "3", 0, "reference", 6, "2M", "readgroup4"),
                  createRead("read4", "3", 0, "reference", 7, "2M", "readgroup4"),
                  createRead("read8", "3", 0, "reference", 8, "2M", "readgroup5")),
              null));
  private static final List<Readset> READSETS =
      ImmutableList.copyOf(SEARCH_READS_RESPONSES.keySet());

  @ClassRule
  public static final WebTarget
      TARGET = WebTarget.create(Server.builder().setDatasets(DATASET_DIR).build());

  @BeforeClass
  public static void setUp() {
    DateTimeUtils.setCurrentMillisFixed(NOW);
  }

  @Test
  public void testGetDataset() {
    assertEquals(
        DATASET,
        TARGET.path(String.format("/datasets/%s", DATASET.getId()))
            .request()
            .get(Dataset.class));
  }

  @Test
  public void testGetReadset() {
    for (Readset readset : READSETS) {
      assertEquals(
          readset,
          TARGET.path(String.format("/readsets/%s", readset.getId())).request().get(Readset.class));
    }
  }

  @Test
  public void testListDatasets() {
    assertEquals(
        ListDatasetsResponse.create(Arrays.asList(DATASET), null),
        TARGET.path("/datasets")
            .queryParam("projectId", 0)
            .request()
            .get(ListDatasetsResponse.class));
  }

  @Test
  public void testSearchReads() {
    for (Map.Entry<Readset, SearchReadsResponse> entry : SEARCH_READS_RESPONSES.entrySet()) {
      assertEquals(
          entry.getValue(),
          TARGET.path("/reads/search")
              .request()
              .post(
                  Entity.json(SearchReadsRequest.create(
                      Collections.<String>emptyList(),
                      Collections.singletonList(entry.getKey().getId()),
                      "reference",
                      null,
                      null,
                      null)),
                  SearchReadsResponse.class));
    }
  }

  @Test
  public void testSearchReadsets() {
    assertEquals(
        SearchReadsetsResponse.create(READSETS, null),
        TARGET.path("/readsets/search")
            .request()
            .post(
                Entity.json(SearchReadsetsRequest.create(Arrays.<String>asList(), null)),
                SearchReadsetsResponse.class));
  }

  private static Readset createReadset(String id, String sample) {
    return Readset.create(id, sample, DATASET_ID, NOW, FILE_DATA);
  }

  private static Readset.FileData.Header createHeader(String version, String sortingOrder) {
    return Readset.FileData.Header.create(version, sortingOrder);
  }

  private static Readset.FileData.RefSequence createRefSequence(String name, int length) {
    return Readset.FileData.RefSequence.create(name, length, null, null, null, null);
  }

  private static Readset.FileData.ReadGroup createReadGroup(String id, String sample) {
    return Readset.FileData.ReadGroup.create(
        id, null, null, null, null, null, null, null, null, null, null, sample);
  }

  private static Read createRead(
      String name,
      String readsetId,
      int flags,
      String referenceSequenceName,
      int position,
      String cigar,
      String readGroup) {
    return Read.create(
        name,
        name,
        readsetId,
        flags,
        referenceSequenceName,
        position,
        null,
        cigar,
        null,
        null,
        null,
        null,
        null,
        null,
        ImmutableMap.of("RG", readGroup));
  }
}

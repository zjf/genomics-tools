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
package com.google.cloud.genomics.api.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.genomics.Genomics;
import com.google.api.services.genomics.GenomicsRequest;
import com.google.cloud.genomics.api.client.GenomicsSample;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.IllegalArgumentException;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class GenomicsSampleTest {
  private static Genomics GENOMICS;

  @BeforeClass
  public static void setUp() throws Exception {
    GENOMICS = new Genomics(GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(), null);
  }

  @Test
  public void testImportReadsets() throws Exception {
    CommandLine cl = new CommandLine();

    try {
      GenomicsSample.importReadsets(cl, null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected - dataset_id required
    }

    cl.datasetId = "dataset";
    try {
      GenomicsSample.importReadsets(cl, null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected - bam_file required
    }

    cl.bamFiles = Arrays.asList("file");
    assertNotNull(GenomicsSample.importReadsets(cl, GENOMICS));
  }

  @Test
  public void testSearchReadsets() throws Exception {
    CommandLine cl = new CommandLine();

    try {
      GenomicsSample.searchReadsets(cl, null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected - dataset_ids required
    }

    cl.datasetIds = Arrays.asList("dataset");
    assertNotNull(GenomicsSample.searchReadsets(cl, GENOMICS));
  }

  @Test
  public void testGetReadset() throws Exception {
    CommandLine cl = new CommandLine();

    try {
      GenomicsSample.getReadset(cl, null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected - readset_id required
    }

    cl.readsetId = "readset";
    Genomics.Readsets.Get request = GenomicsSample.getReadset(cl, GENOMICS);
    assertEquals("readset", request.getReadsetId());
  }

  @Test
  public void testGetJob() throws Exception {
    CommandLine cl = new CommandLine();

    try {
      GenomicsSample.getJob(cl, null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected - job_id required
    }

    cl.jobId = "job";
    Genomics.Jobs.Get request = GenomicsSample.getJob(cl, GENOMICS);
    assertEquals("job", request.getJobId());
  }

  @Test
  public void testSearchReads() throws Exception {
    CommandLine cl = new CommandLine();
    // An empty request is ok
    assertNotNull(GenomicsSample.searchReads(cl, GENOMICS));

    cl.sequenceName = "1";
    try {
      GenomicsSample.searchReads(cl, null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected - sequence start and end required
    }

    cl.sequenceStart = 5;
    try {
      GenomicsSample.searchReads(cl, null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected - sequence end required
    }

    cl.sequenceEnd = 6;
    assertNotNull(GenomicsSample.searchReads(cl, GENOMICS));
  }


}
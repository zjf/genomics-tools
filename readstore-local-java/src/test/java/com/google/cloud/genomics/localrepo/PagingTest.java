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
import static org.junit.Assert.assertNull;

import com.google.cloud.genomics.localrepo.dto.Read;
import com.google.cloud.genomics.localrepo.dto.SearchReadsRequest;
import com.google.cloud.genomics.localrepo.dto.SearchReadsResponse;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;

import javax.ws.rs.client.Entity;

public class PagingTest extends BaseTest {

  @ClassRule
  public static final WebTarget
      TARGET = WebTarget.create(Server.builder().setDatasets(DATASET_DIR).setPageSize(1).build());

  private static final Function<Read, String> GET_NAME =
      new Function<Read, String>() {
        @Override public String apply(Read read) {
          return read.getName();
        }
      };

  @Test
  public void testSearchReadsPaging() {
    ImmutableList.Builder<String> names = ImmutableList.builder();
    SearchReadsRequest request1 =
        SearchReadsRequest.create(null, null, "reference", null, null, null);
    SearchReadsResponse response1 = TARGET.path("/reads/search").request()
        .post(Entity.json(request1), SearchReadsResponse.class);
    names.addAll(FluentIterable.from(response1.getReads()).transform(GET_NAME));
    SearchReadsRequest request2 = SearchReadsRequest.create(null,
        null,
        "reference",
        null,
        null,
        response1.getNextPageToken());
    SearchReadsResponse response2 = TARGET.path("/reads/search").request()
        .post(Entity.json(request2), SearchReadsResponse.class);
    names.addAll(FluentIterable.from(response2.getReads()).transform(GET_NAME));
    SearchReadsRequest request3 = SearchReadsRequest.create(null,
        null,
        "reference",
        null,
        null,
        response2.getNextPageToken());
    SearchReadsResponse response3 = TARGET.path("/reads/search").request()
        .post(Entity.json(request3), SearchReadsResponse.class);
    names.addAll(FluentIterable.from(response3.getReads()).transform(GET_NAME));
    SearchReadsRequest request4 = SearchReadsRequest.create(null,
        null,
        "reference",
        null,
        null,
        response3.getNextPageToken());
    SearchReadsResponse response4 = TARGET.path("/reads/search").request()
        .post(Entity.json(request4), SearchReadsResponse.class);
    names.addAll(FluentIterable.from(response4.getReads()).transform(GET_NAME));
    SearchReadsRequest request5 = SearchReadsRequest.create(null,
        null,
        "reference",
        null,
        null,
        response4.getNextPageToken());
    SearchReadsResponse response5 = TARGET.path("/reads/search").request()
        .post(Entity.json(request5), SearchReadsResponse.class);
    names.addAll(FluentIterable.from(response5.getReads()).transform(GET_NAME));
    SearchReadsRequest request6 = SearchReadsRequest.create(null,
        null,
        "reference",
        null,
        null,
        response5.getNextPageToken());
    SearchReadsResponse response6 = TARGET.path("/reads/search").request()
        .post(Entity.json(request6), SearchReadsResponse.class);
    names.addAll(FluentIterable.from(response6.getReads()).transform(GET_NAME));
    SearchReadsRequest request7 = SearchReadsRequest.create(null,
        null,
        "reference",
        null,
        null,
        response6.getNextPageToken());
    SearchReadsResponse response7 = TARGET.path("/reads/search").request()
        .post(Entity.json(request7), SearchReadsResponse.class);
    names.addAll(FluentIterable.from(response7.getReads()).transform(GET_NAME));
    SearchReadsRequest request8 = SearchReadsRequest.create(null,
        null,
        "reference",
        null,
        null,
        response7.getNextPageToken());
    SearchReadsResponse response8 = TARGET.path("/reads/search").request()
        .post(Entity.json(request8), SearchReadsResponse.class);
    names.addAll(FluentIterable.from(response8.getReads()).transform(GET_NAME));
    assertEquals(
        Arrays.asList("read1", "read5", "read2", "read6", "read3", "read7", "read4", "read8"),
        names.build());
    assertNull(response8.getNextPageToken());
  }
}

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

import com.google.cloud.genomics.localrepo.dto.SearchReadsRequest;
import com.google.cloud.genomics.localrepo.dto.SearchReadsResponse;

import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.Entity;

public class PagingTest extends BaseTest {

  @ClassRule
  public static final WebTarget
      TARGET = WebTarget.create(Server.builder().setDatasets(DATASET_DIR).setPageSize(1).build());

  @Test
  public void testSearchReadsPaging() {
    List<String> names = new ArrayList<>();
    SearchReadsRequest request1 = createSearchReadsRequest(null);
    SearchReadsResponse response1 = createSearchReadsResponse(request1, names);
    SearchReadsRequest request2 = createSearchReadsRequest(response1);
    SearchReadsResponse response2 = createSearchReadsResponse(request2, names);
    SearchReadsRequest request3 = createSearchReadsRequest(response2);
    SearchReadsResponse response3 = createSearchReadsResponse(request3, names);
    SearchReadsRequest request4 = createSearchReadsRequest(response3);
    SearchReadsResponse response4 = createSearchReadsResponse(request4, names);
    SearchReadsRequest request5 = createSearchReadsRequest(response4);
    SearchReadsResponse response5 = createSearchReadsResponse(request5, names);
    SearchReadsRequest request6 = createSearchReadsRequest(response5);
    SearchReadsResponse response6 = createSearchReadsResponse(request6, names);
    SearchReadsRequest request7 = createSearchReadsRequest(response6);
    SearchReadsResponse response7 = createSearchReadsResponse(request7, names);
    SearchReadsRequest request8 = createSearchReadsRequest(response7);
    SearchReadsResponse response8 = createSearchReadsResponse(request8, names);
    assertEquals(
        Arrays.asList("read1", "read5", "read2", "read6", "read3", "read7", "read4", "read8"),
        names);
    assertNull(response8.getNextPageToken());
  }

  private static SearchReadsRequest createSearchReadsRequest(SearchReadsResponse response) {
    return SearchReadsRequest.create(null, null, "reference", null, null,
        null == response ? null : response.getNextPageToken());
  }

  private static SearchReadsResponse createSearchReadsResponse(SearchReadsRequest request,
      List<String> names) {
    SearchReadsResponse response =
        TARGET.path("/reads/search").request()
            .post(Entity.json(request), SearchReadsResponse.class);
    response.getReads().stream().map(read -> read.getName()).forEach(names::add);
    return response;
  }
}

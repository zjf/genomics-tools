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

import com.google.cloud.genomics.localrepo.dto.Read;
import com.google.cloud.genomics.localrepo.dto.SearchReadsRequest;
import com.google.cloud.genomics.localrepo.dto.SearchReadsResponse;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/reads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class Reads extends BaseResource {

  private static class Page<X> {

    private static final int PAGE_SIZE = 256;

    static <X> Page<X> nextPage(Iterable<X> stream, int page) {
      Iterator<X> iterator = stream.iterator();
      ImmutableList.Builder<X> objects = ImmutableList.builder();
      for (int i = 0; iterator.hasNext() && i < PAGE_SIZE * (1 + page); ++i) {
        X next = iterator.next();
        if (PAGE_SIZE * page <= i) {
          objects.add(next);
        }
      }
      return new Page<>(
          objects.build(),
          iterator.hasNext() ? Optional.of(page + 1) : Optional.<Integer>absent());
    }

    final Optional<Integer> nextPage;
    final List<X> objects;

    private Page(List<X> objects, Optional<Integer> nextPage) {
      this.objects = objects;
      this.nextPage = nextPage;
    }
  }

  private final Backend backend;

  @Inject
  public Reads(Backend backend) {
    this.backend = backend;
  }

  @POST
  @Path("/search")
  public Response search(final SearchReadsRequest request) {
    List<String> datasetIds = request.getDatasetIds();
    List<String> readsetIds = request.getReadsetIds();
    return datasetIds.isEmpty() || readsetIds.isEmpty()
        ? Response
            .ok(backend.searchReads(
                datasetIds,
                readsetIds,
                request.getSequenceName(),
                Optional.fromNullable(request.getSequenceStart()),
                Optional.fromNullable(request.getSequenceEnd()),
                new Function<FluentIterable<Read>, SearchReadsResponse>() {
                  @Override public SearchReadsResponse apply(FluentIterable<Read> reads) {
                    Page<Read> page = Page.nextPage(
                        reads,
                        Integer.parseInt(Optional.fromNullable(request.getPageToken()).or("0")));
                    return SearchReadsResponse.create(
                        page.objects,
                        page.nextPage.isPresent() ? String.valueOf(page.nextPage.get()) : null);
                  }
                }))
            .build()
        : BAD_REQUEST;
  }
}

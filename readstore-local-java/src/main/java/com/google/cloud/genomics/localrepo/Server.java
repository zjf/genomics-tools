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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class Server {

  public static final class Builder {

    private static final String DEFAULT_PATH = "";
    private static final int DEFAULT_PORT = 8080;
    private static final Collection<DatasetDirectory> DEFAULT_DATASETS = Collections.emptyList();

    private String path = DEFAULT_PATH;
    private int port = DEFAULT_PORT;
    private Collection<DatasetDirectory> datasets = DEFAULT_DATASETS;

    private Builder() {}

    public Server build() {
      return new Server(port, path, datasets);
    }

    @Override
    public boolean equals(Object obj) {
      if (null != obj && Builder.class == obj.getClass()) {
        Builder rhs = (Builder) obj;
        return Objects.equals(path, rhs.path)
            && port == rhs.port
            && datasets.equals(rhs.datasets);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(port, path, datasets);
    }

    public Builder setDatasets(DatasetDirectory... datasets) {
      return setDatasets(Arrays.asList(datasets));
    }

    public Builder setDatasets(Collection<DatasetDirectory> datasets) {
      this.datasets = datasets;
      return this;
    }

    public Builder setPath(String path) {
      this.path = path;
      return this;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    @Override
    public String toString() {
      return String.format("path = \"%s\", port = %d", path, port);
    }
  }

  private static final Function<Map.Entry<String, String>, DatasetDirectory> CREATE_DATASET =
      new Function<Map.Entry<String, String>, DatasetDirectory>() {
        @Override public DatasetDirectory apply(Map.Entry<String, String> entry) {
          return DatasetDirectory.create(entry.getKey(), entry.getValue());
        }
      };

  public static Builder builder() {
    return new Builder();
  }

  public static void main(String[] args) throws Exception {
    CommandLineArguments cmdLine = CommandLineArguments.parse(args);
    builder()
        .setPort(cmdLine.getPort())
        .setDatasets(
            FluentIterable.from(cmdLine.getDatasets().entrySet())
                .transform(CREATE_DATASET)
                .toList())
        .build()
        .start();
    Thread.currentThread().join();
  }

  private final HttpServer server;
  private final URI uri;

  private Server(int port, String path, final Collection<DatasetDirectory> datasets) {
    server = GrizzlyHttpServerFactory.createHttpServer(
        uri = URI.create(String.format("http://localhost:%d/%s", port, path)),
        new ResourceConfig()
            .register(Datasets.class)
            .register(Reads.class)
            .register(Readsets.class)
            .register(
                new AbstractBinder() {
                  @Override protected void configure() {
                    bind(Backend.create(datasets));
                  }
                })
            .register(
                new JacksonJaxbJsonProvider() {{
                  setMapper(DataTransferObject.OBJECT_MAPPER);
                }}),
        false);
  }

  @Override
  public boolean equals(Object obj) {
    if (null != obj && Server.class == obj.getClass()) {
      Server rhs = (Server) obj;
      return Objects.equals(server, rhs.server)
          && Objects.equals(uri, rhs.uri);
    }
    return false;
  }

  public URI getURI() {
    return uri;
  }

  @Override
  public int hashCode() {
    return Objects.hash(server, uri);
  }

  public Server start() throws IOException {
    server.start();
    return this;
  }

  public Server stop() {
    server.shutdownNow();
    return this;
  }

  @Override
  public String toString() {
    return String.format("server = %s, uri = %s", server, uri);
  }
}

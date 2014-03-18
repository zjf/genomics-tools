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
package com.google.cloud.genomics.benchmarker;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

public final class Server {
  public static void main(String[] args) throws Exception {
    CommandLine cmdLine = new CommandLine(args);

    if (cmdLine.showHelp()) {
      cmdLine.printHelp(System.err);
      return;
    }
    new Server(cmdLine.port).start();
    Thread.currentThread().join();
  }

  private final HttpServer server;

  private Server(int port) {
    server = GrizzlyHttpServerFactory.createHttpServer(
        URI.create(String.format("http://localhost:%d", port)),
        new ResourceConfig(),
        false);

    server.getServerConfiguration().addHttpHandler(
        new StaticHttpHandler("static/"), "/static");
  }

  public Server start() throws IOException {
    server.start();
    return this;
  }
}

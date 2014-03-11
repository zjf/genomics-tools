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

import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

public class WebTarget extends ExternalResource implements javax.ws.rs.client.WebTarget {

  public static WebTarget create(Server server) {
    return create(
        server,
        ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(server.getURI()));
  }

  private static WebTarget create(Server server, javax.ws.rs.client.WebTarget delegate) {
    return new WebTarget(server, delegate);
  }

  private final javax.ws.rs.client.WebTarget delegate;
  private final Server server;

  private WebTarget(Server server, javax.ws.rs.client.WebTarget delegate) {
    this.server = server;
    this.delegate = delegate;
  }

  @Override
  protected void after() {
    server.stop();
  }

  @Override
  protected void before() throws IOException {
    server.start();
  }

  @Override
  public Configuration getConfiguration() {
    return delegate.getConfiguration();
  }

  @Override
  public URI getUri() {
    return delegate.getUri();
  }

  @Override
  public UriBuilder getUriBuilder() {
    return delegate.getUriBuilder();
  }

  @Override
  public WebTarget matrixParam(String name, Object... values) {
    return create(server, delegate.matrixParam(name, values));
  }

  @Override
  public WebTarget path(String path) {
    return create(server, delegate.path(path));
  }

  @Override
  public WebTarget property(String name, Object value) {
    return create(server, delegate.property(name, value));
  }

  @Override
  public WebTarget queryParam(String name, Object... values) {
    return create(server, delegate.queryParam(name, values));
  }

  @Override
  public WebTarget register(Class<?> componentClass) {
    return create(server, delegate.register(componentClass));
  }

  @Override
  public WebTarget register(Class<?> componentClass, Class<?>... contracts) {
    return create(server, delegate.register(componentClass, contracts));
  }

  @Override
  public WebTarget register(Class<?> componentClass, int priority) {
    return create(server, delegate.register(componentClass, priority));
  }

  @Override
  public WebTarget register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
    return create(server, delegate.register(componentClass, contracts));
  }

  @Override
  public WebTarget register(Object component) {
    return create(server, delegate.register(component));
  }

  @Override
  public WebTarget register(Object component, Class<?>... contracts) {
    return create(server, delegate.register(component, contracts));
  }

  @Override
  public WebTarget register(Object component, int priority) {
    return create(server, delegate.register(component, priority));
  }

  @Override
  public WebTarget register(Object component, Map<Class<?>, Integer> contracts) {
    return create(server, delegate.register(component, contracts));
  }

  @Override
  public Builder request() {
    return delegate.request();
  }

  @Override
  public Builder request(MediaType... acceptedResponseTypes) {
    return delegate.request(acceptedResponseTypes);
  }

  @Override
  public Builder request(String... acceptedResponseTypes) {
    return delegate.request(acceptedResponseTypes);
  }

  @Override
  public WebTarget resolveTemplate(String name, Object value) {
    return create(server, delegate.resolveTemplate(name, value));
  }

  @Override
  public WebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
    return create(server, delegate.resolveTemplate(name, value, encodeSlashInPath));
  }

  @Override
  public WebTarget resolveTemplateFromEncoded(String name, Object value) {
    return create(server, delegate.resolveTemplateFromEncoded(name, value));
  }

  @Override
  public WebTarget resolveTemplates(Map<String, Object> templateValues) {
    return create(server, delegate.resolveTemplates(templateValues));
  }

  @Override
  public WebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) {
    return create(server, delegate.resolveTemplates(templateValues, encodeSlashInPath));
  }

  @Override
  public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
    return create(server, delegate.resolveTemplatesFromEncoded(templateValues));
  }
}

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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.genomics.Genomics;
import com.google.api.services.genomics.GenomicsRequest;
import com.google.api.services.genomics.model.ImportReadsetsRequest;
import com.google.api.services.genomics.model.SearchReadsRequest;
import com.google.api.services.genomics.model.SearchReadsetsRequest;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * Genomics Java client sample application.
 */
public class GenomicsSample {
  private static final String APPLICATION_NAME = "Google-GenomicsSample/1.0";
  private static final java.io.File DATA_STORE_DIR =
      new java.io.File(System.getProperty("user.home"), ".store/genomics_java_client");
  private static final String DEVSTORAGE_SCOPE =
      "https://www.googleapis.com/auth/devstorage.read_write";
  private static final String GENOMICS_SCOPE = "https://www.googleapis.com/auth/genomics";
  private static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  private static FileDataStoreFactory dataStoreFactory;
  private static NetHttpTransport httpTransport;
  private static CommandLine cmdLine;

  private static GoogleClientSecrets loadClientSecrets(String clientSecretsFilename) {
    File f = new File(clientSecretsFilename);
    if (f.exists()) {
      try {
        InputStream inputStream = new FileInputStream(new File(clientSecretsFilename));
        return GoogleClientSecrets.load(JSON_FACTORY,
            new InputStreamReader(inputStream));
      } catch (Exception e) {
        System.err.println("Could not load client_secrets.json");
      }
    } else {
      System.err.println("Client secrets file " + clientSecretsFilename + " does not exist."
          + "  Visit https://developers.google.com/genomics to learn how"
          + " to install a client_secrets.json file.  If you have installed a client_secrets.json"
          + " in a specific location, use --client_secrets_filename <path>/client_secrets.json.");
    }
    return null;
  }

  private static Genomics buildService(final Credential credential) {
    return new Genomics.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .setRootUrl(cmdLine.rootUrl)
        .setHttpRequestInitializer(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
              credential.initialize(httpRequest);
              httpRequest.setReadTimeout(60000); // 60 seconds
            }
          }).build();
  }

  private static Credential authorize() throws Exception {
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, JSON_FACTORY, loadClientSecrets(cmdLine.clientSecretsFilename),
        Arrays.asList(DEVSTORAGE_SCOPE, GENOMICS_SCOPE, EMAIL_SCOPE)).setDataStoreFactory(dataStoreFactory)
        .build();
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

  public static void main(String[] args) {
    try {
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
      cmdLine = new CommandLine(args);

      // Authorization
      Credential credential = authorize();

      // Show help
      assertOrDie(!cmdLine.showHelp(), "");

      // Make sure request_type is specified
      assertOrDie(cmdLine.remainingArgs.size() == 1,
          "Must specify a request_type\n");

      // Route to appropriate request method
      String requestType = cmdLine.remainingArgs.get(0);
      switch (requestType) {
        case "help":
          cmdLine.printHelp("", System.err);
          break;
        default:
          Genomics genomics = buildService(credential);
          try {
            executeAndPrint(getRequest(cmdLine, genomics, requestType));
          } catch (IllegalArgumentException e) {
            cmdLine.printHelp(e.getMessage() + "\n", System.err);
            System.exit(0);
          }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private static GenomicsRequest getRequest(CommandLine cmdLine, Genomics genomics, String requestType)
      throws IOException, IllegalArgumentException {
    switch (requestType) {
      case "importreadsets":
        return importReadsets(cmdLine, genomics);
      case "searchreadsets":
        return searchReadsets(cmdLine, genomics);
      case "getreadset":
        return getReadset(cmdLine, genomics);
      case "getjob":
        return getJob(cmdLine, genomics);
      case "searchreads":
        return searchReads(cmdLine, genomics);
      default:
        List<String> validRequestTypes = Arrays.asList("help", "importreadsets",
            "searchreadsets", "getreadset", "getjob", "searchreads");
        throw new IllegalArgumentException("request_type must be one of: " + validRequestTypes + "\n");
    }
  }

  private static void assertOrDie(boolean condition, String headline) throws IOException {
    if (!condition) {
      cmdLine.printHelp(headline, System.err);
      System.exit(0);
    }
  }

  private static void assertOrThrow(boolean condition, String headline) throws IllegalArgumentException {
    if (!condition) {
      throw new IllegalArgumentException(headline);
    }
  }

  private static void executeAndPrint(GenomicsRequest<?> req) throws IOException {
    if (!cmdLine.fields.isEmpty()) {
      req.setFields(cmdLine.fields);
    }
    System.out.println("result: " + req.execute());
  }

  static Genomics.Readsets.GenomicsImport importReadsets(CommandLine cmdLine, Genomics genomics)
      throws IOException, IllegalArgumentException {
    assertOrThrow(!cmdLine.datasetId.isEmpty(), "Must specify a dataset_id\n");
    assertOrThrow(cmdLine.bamFiles.size() > 0, "Must specify at least one BAM file\n");

    ImportReadsetsRequest content = new ImportReadsetsRequest()
        .setDatasetId(cmdLine.datasetId)
        .setSourceUris(cmdLine.bamFiles);
    return genomics.readsets().genomicsImport(content);
  }

  static Genomics.Readsets.Search searchReadsets(CommandLine cmdLine, Genomics genomics)
      throws IOException, IllegalArgumentException {
    assertOrThrow(!cmdLine.datasetIds.isEmpty(), "Currently, dataset_ids is required. " +
        "This requirement will go away in the future.");

    SearchReadsetsRequest content = new SearchReadsetsRequest().setDatasetIds(cmdLine.datasetIds);
    return genomics.readsets().search(content);
  }

  static Genomics.Readsets.Get getReadset(CommandLine cmdLine, Genomics genomics)
      throws IOException, IllegalArgumentException {
    assertOrThrow(!cmdLine.readsetId.isEmpty(), "Must specify a readset_id");
    return genomics.readsets().get(cmdLine.readsetId);
  }

  static Genomics.Jobs.Get getJob(CommandLine cmdLine, Genomics genomics)
      throws IOException, IllegalArgumentException {
    assertOrThrow(!cmdLine.jobId.isEmpty(), "Must specify a job_id");
    return genomics.jobs().get(cmdLine.jobId);
  }

  static Genomics.Reads.Search searchReads(CommandLine cmdLine, Genomics genomics)
      throws IOException, IllegalArgumentException {
    SearchReadsRequest content = new SearchReadsRequest()
        .setReadsetIds(cmdLine.readsetIds)
        .setPageToken(cmdLine.pageToken);

    // Range parameters must all be specified or none.
    if (!cmdLine.sequenceName.isEmpty() || cmdLine.sequenceStart > 0 || cmdLine.sequenceEnd > 0) {
      assertOrThrow(!cmdLine.sequenceName.isEmpty(), "Must specify a sequence_name");
      assertOrThrow(cmdLine.sequenceStart > 0, "sequence_start must be greater than 0");
      // getting this far implies target_start is greater than 0
      assertOrThrow(cmdLine.sequenceEnd >= cmdLine.sequenceStart,
          "sequence_end must be greater than sequence_start");

      content
          .setSequenceName(cmdLine.sequenceName)
          .setSequenceStart(BigInteger.valueOf(cmdLine.sequenceStart))
          .setSequenceEnd(BigInteger.valueOf(cmdLine.sequenceEnd));
    }
    return genomics.reads().search(content);
  }
}

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
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.java6.auth.oauth2.GooglePromptReceiver;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.genomics.Genomics;
import com.google.api.services.genomics.model.GetJobResponse;
import com.google.api.services.genomics.model.GetReadsetResponse;
import com.google.api.services.genomics.model.ImportReadsetsRequest;
import com.google.api.services.genomics.model.ImportReadsetsResponse;
import com.google.api.services.genomics.model.ListReadsRequest;
import com.google.api.services.genomics.model.ListReadsResponse;
import com.google.api.services.genomics.model.ListReadsetsRequest;
import com.google.api.services.genomics.model.ListReadsetsResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Genomics Java client sample application.
 */
public class GenomicsSample {

  private static Genomics genomics;
  private static GoogleClientSecrets clientSecrets = null;
  private static final String APPLICATION_NAME = "Google-GenomicsSample/1.0";
  private static NetHttpTransport httpTransport;
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static CommandLine cmdLine;
  private static final String DEVSTORAGE_SCOPE =
      "https://www.googleapis.com/auth/devstorage.read_write";
  private static final String GENOMICS_SCOPE = "https://www.googleapis.com/auth/genomics";
  private static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";
  private static final String TOKEN_PROPERTIES_FILENAME = "genomics.token.properties";
  private static GoogleAuthorizationCodeFlow flow = null;

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
          + "  Visit https://developers.google.com/genomics/v1beta/quickstart to learn how"
          + " to install a client_secrets.json file.  If you have installed a client_secrets.json"
          + " in a specific location, use --client_secrets_filename <path>/client_secrets.json.");
    }
    return null;
  }

  public static GoogleCredential createCredentialWithRefreshToken(TokenResponse tokenResponse) {
    return new GoogleCredential.Builder()
        .setTransport(httpTransport)
        .setJsonFactory(JSON_FACTORY)
        .setClientSecrets(clientSecrets)
        .build()
        .setFromTokenResponse(tokenResponse);
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

  private static File getGoogleCloudDotDirectory() {
    return new File(System.getProperty("user.home"), ".google_cloud");
  }

  private static void createDotGoogleCloudDirectory() {
      Set<PosixFilePermission> perms =
          PosixFilePermissions.fromString("rwx------");
      FileAttribute<Set<PosixFilePermission>> attr =
          PosixFilePermissions.asFileAttribute(perms);
      Path path = Paths.get(getGoogleCloudDotDirectory().getPath());
      if (!Files.exists(path)) {
        try {
          Files.createDirectory(path, attr);
        } catch (FileNotFoundException e) {
          System.err.println("Failed to create $HOME/.google_cloud");
          System.exit(1);
        } catch (IOException e) {
          System.err.println("Failed to create $HOME/.google_cloud");
          System.exit(1);
        }
      }  // TODO(user): ensure directory exists with correct permissions.
  }

  private static GoogleAuthorizationCodeFlow getFlow() {
    if (flow == null) {
      flow = new GoogleAuthorizationCodeFlow.Builder(
          httpTransport,
          JSON_FACTORY,
          clientSecrets,
          Arrays.asList(DEVSTORAGE_SCOPE, GENOMICS_SCOPE, EMAIL_SCOPE))
          .setAccessType("offline")
          .setApprovalPrompt("force")
          .build();
    }
    return flow;
  }


  private static Credential exchangeCode() throws IOException  {
    GoogleAuthorizationCodeFlow flow = getFlow();
    return new AuthorizationCodeInstalledApp(flow, new GooglePromptReceiver())
          .authorize(System.getProperty("user.name"));
  }

  private static void storeRefreshToken(String refreshToken) {
    Properties properties = new Properties();
    properties.setProperty("refreshtoken", refreshToken);

    // Create the custom permissions attribute.
    Set<PosixFilePermission> perms =
        PosixFilePermissions.fromString("rw-------");
    FileAttribute<Set<PosixFilePermission>> attr =
        PosixFilePermissions.asFileAttribute(perms);
    Path path = Paths.get(getGoogleCloudDotDirectory().getPath(), "/" + TOKEN_PROPERTIES_FILENAME);
    try {
      Files.createFile(path, attr);
    } catch (IOException e) {
      System.err.println("Failed to create token file " + path);
    }
    try {
      properties.store(Files.newOutputStream(path), null);
    } catch (IOException e) {
      System.err.println("Failed storing token properties to file " + path);
    }
  }

  private static String loadRefreshToken() {
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(getGoogleCloudDotDirectory().getPath() +
              "/" + TOKEN_PROPERTIES_FILENAME));
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
    }
    return (String) properties.get("refreshtoken");
  }

  private static boolean deleteRefreshToken() {
    String filename = getGoogleCloudDotDirectory().getPath() + "/" + TOKEN_PROPERTIES_FILENAME;
    File f = new File(filename);
    return f.exists() && f.delete();
  }

  private static long parseProjectId(final String projectIdString) {
    // convert String ProjectId argument to Long
    return Long.parseLong(projectIdString);
  }

  private static Credential authenticate() throws IOException {
    // Attempt to load client secrets
    clientSecrets = loadClientSecrets(cmdLine.clientSecretsFilename);
    if (clientSecrets == null) {
      System.exit(1);
    }

    // Attempt to Load existing Refresh Token
    @Nullable String storedRefreshToken = loadRefreshToken();

    // Check to see if the an existing refresh token was loaded.
    // If so, create a credential and call refreshToken() to get a new
    // access token.
    Credential credential;
    if (storedRefreshToken != null) {
      // Request a new Access token using the refresh token.
      credential = createCredentialWithRefreshToken(
          new TokenResponse().setRefreshToken(storedRefreshToken));
      credential.refreshToken();
    } else {
      // If there is no refresh token (or token.properties file), start the OAuth
      // authorization flow.
      // Exchange the auth code for an access token and refesh token
      credential = exchangeCode();

      // Store the refresh token for future use.
      storeRefreshToken(credential.getRefreshToken());
    }
    return credential;
  }

  private static void createGenomicsClient() throws IOException {
    genomics = buildService(authenticate());
  }

  public static void main(String[] args) {
    try {
      createDotGoogleCloudDirectory();

      cmdLine = new CommandLine(args);

      // Show help
      assertOrDie(!cmdLine.showHelp());

      // Make sure request_type is specified
      assertOrDie(cmdLine.remainingArgs.size() == 1,
          "Must specify a request_type\n");

      httpTransport = GoogleNetHttpTransport.newTrustedTransport();

      // Route to appropriate request method
      List<String> validRequestTypes = Arrays.asList(
          "auth", "help", "importreadsets", "listreadsets", "getreadset", "getjob", "listreads");
      String requestType = cmdLine.remainingArgs.get(0);
      switch(requestType) {
        case "help":
          cmdLine.printHelp("", System.err);
          break;
        case "auth":
          deleteRefreshToken();
          authenticate();
          break;
        default:
          createGenomicsClient();
          switch(requestType) {
            case "importreadsets":
              importReadsets();
              break;
            case "listreadsets":
              listReadsets();
              break;
            case "getreadset":
              getReadset();
              break;
            case "getjob":
              getJob();
              break;
            case "listreads":
              listReads();
              break;
            default:
              cmdLine.printHelp("request_type must be one of: " + validRequestTypes + "\n",
                  System.err);
              return;
          }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private static void assertOrDie(boolean condition, String headline) throws IOException {
    if (!condition) {
      cmdLine.printHelp(headline, System.err);
      System.exit(0);
    }
  }

  private static void assertOrDie(boolean condition) throws IOException {
    assertOrDie(condition, "");
  }

  private static void importReadsets() throws IOException {
    // validate the command line
    assertOrDie(!cmdLine.datasetId.isEmpty(), "Must specify a dataset_id\n");
    assertOrDie(cmdLine.bamFiles.size() > 0, "Must specify at least one BAM file\n");

    // Create request
    ImportReadsetsRequest content = new ImportReadsetsRequest()
        .setDatasetId(cmdLine.datasetId)
        .setSourceUris(cmdLine.bamFiles);
    // Invoke import and get response
    ImportReadsetsResponse result = genomics.readsets().genomicsImport(content).execute();
    System.out.println("result: " + result);
  }

  private static void listReadsets() throws IOException {
    // validate the command line
    assertOrDie(!cmdLine.datasetIds.isEmpty(), "Currently, dataset_ids is required. " +
        "This requirement will go away in the future.\n");

    ListReadsetsRequest content = new ListReadsetsRequest().setDatasetIds(cmdLine.datasetIds);
    ListReadsetsResponse result = genomics.readsets().search(content)
        .setFields(cmdLine.fields)
        .execute();
    System.out.println("result: " + result);
  }

  private static void getReadset() throws IOException {
    // validate the command line
    assertOrDie(!cmdLine.readsetId.isEmpty(), "Must specify a readset_id\n");

    GetReadsetResponse result = genomics.readsets().get(cmdLine.readsetId)
        .setFields(cmdLine.fields)
        .execute();
    System.out.println("result: " + result);
  }

  private static void getJob() throws IOException {
    // validate the command line
    assertOrDie(!cmdLine.projectId.isEmpty(), "Currently, project_id is required. " +
        "This requirement will go away in the future.\n");
    assertOrDie(!cmdLine.jobId.isEmpty(), "Must specify a job_id\n");

    // Create request
    long projectId = parseProjectId(cmdLine.projectId);
    GetJobResponse result = genomics.jobs().get(cmdLine.jobId)
        .setFields(cmdLine.fields)
        .setProjectId(projectId)
        .execute();
    System.out.println("result: " + result);
  }

  private static void listReads() throws IOException {
    // Create request.
    ListReadsRequest content = new ListReadsRequest()
        .setReadsetIds(cmdLine.readsetIds)
        .setPageToken(cmdLine.pageToken);

    // Range parameters must all be specified or none.
    if (!cmdLine.sequenceName.isEmpty() || cmdLine.sequenceStart > 0 || cmdLine.sequenceEnd > 0) {
      assertOrDie(!cmdLine.sequenceName.isEmpty(), "Must specify a sequence_name\n");
      assertOrDie(cmdLine.sequenceStart > 0, "sequence_start must be greater than 0\n");
      // getting this far implies target_start is greater than 0
      assertOrDie(cmdLine.sequenceEnd >= cmdLine.sequenceStart,
          "sequence_end must be greater than sequence_start\n");

      content
          .setSequenceName(cmdLine.sequenceName)
          .setSequenceStart(BigInteger.valueOf(cmdLine.sequenceStart.intValue()))
          .setSequenceEnd(BigInteger.valueOf(cmdLine.sequenceEnd.intValue()));
    }

    // Invoke query and get response
    ListReadsResponse result = genomics.reads().search(content)
        .setFields(cmdLine.fields)
        .execute();
    System.out.println("result: " + result);
  }
}

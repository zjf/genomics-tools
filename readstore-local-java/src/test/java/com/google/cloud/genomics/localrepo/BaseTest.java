package com.google.cloud.genomics.localrepo;

abstract class BaseTest {

  static final String DATASET_ID = "datasetId";
  static final DatasetDirectory DATASET_DIR = DatasetDirectory.create(DATASET_ID, "testdata");
}

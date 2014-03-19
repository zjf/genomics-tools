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

import com.google.cloud.genomics.localrepo.BamFile.IndexedBamFile;
import com.google.cloud.genomics.localrepo.dto.Readset;
import com.google.cloud.genomics.localrepo.util.Suppliers;

import net.sf.picard.sam.SamFileHeaderMerger;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;

import org.joda.time.DateTimeUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BamFilesReadset {

  public static BamFilesReadset create(
      String readsetId,
      String sample,
      String datasetId,
      Set<IndexedBamFile> bamFiles) {
    return new BamFilesReadset(readsetId, sample, datasetId, bamFiles);
  }

  private final Set<IndexedBamFile> bamFiles;
  private final String datasetId;
  private final String readsetId;
  private final String sample;
  private final Supplier<Readset> readset;
  private final Supplier<SAMFileHeader> header;

  private BamFilesReadset(String readsetId, String sample, String datasetId,
      Set<IndexedBamFile> bamFiles) {
    this.readsetId = readsetId;
    this.sample = sample;
    this.datasetId = datasetId;
    this.bamFiles = bamFiles;
    this.readset =
        Suppliers.memoize(() -> Readset.create(
            getReadsetId(),
            getSample(),
            getDatasetId(),
            DateTimeUtils.currentTimeMillis(),
            getBamFiles()
                .stream()
                .map(BamFile::getFileData)
                .collect(
                    Collectors.toCollection(() -> new ArrayList<>(new TreeSet<>(Comparator
                        .comparing(Readset.FileData::getFileUri)))))));
    this.header =
        Suppliers.memoize(() -> new SamFileHeaderMerger(SortOrder.coordinate, bamFiles.stream()
            .map(BamFile::getHeader).collect(Collectors.toList()), true).getMergedHeader());
  }

  @Override public boolean equals(Object obj) {
    if (null != obj && BamFilesReadset.class == obj.getClass()) {
      BamFilesReadset rhs = (BamFilesReadset) obj;
      return Objects.equals(getReadsetId(), rhs.getReadsetId())
          && Objects.equals(getSample(), rhs.getSample())
          && Objects.equals(getDatasetId(), rhs.getDatasetId())
          && Objects.equals(getBamFiles(), rhs.getBamFiles());
    }
    return false;
  }

  public Set<IndexedBamFile> getBamFiles() {
    return bamFiles;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public Readset getReadset() {
    return readset.get();
  }

  public String getReadsetId() {
    return readsetId;
  }

  public String getSample() {
    return sample;
  }

  @Override public int hashCode() {
    return Objects.hash(
        getReadsetId(),
        getSample(),
        getDatasetId(),
        getBamFiles());
  }

  @Override public String toString() {
    return String.format(
        "readsetId: %s sample: %s datasetId: %s bamFiles: %s",
        getReadset(),
        getSample(),
        getDatasetId(),
        getBamFiles());
  }

  SAMFileHeader getHeader() {
    return header.get();
  }
}

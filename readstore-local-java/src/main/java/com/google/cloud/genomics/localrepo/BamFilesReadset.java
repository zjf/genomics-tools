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
import com.google.cloud.genomics.localrepo.dto.Readset.FileData;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;

import net.sf.picard.sam.SamFileHeaderMerger;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;

import org.joda.time.DateTimeUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

public class BamFilesReadset {

  public static final Function<BamFilesReadset, Set<IndexedBamFile>> GET_BAM_FILES =
      new Function<BamFilesReadset, Set<IndexedBamFile>>() {
        @Override public Set<IndexedBamFile> apply(BamFilesReadset bamFile) {
          return bamFile.getBamFiles();
        }
      };

  public static final Function<BamFilesReadset, String> GET_DATASET_ID =
      new Function<BamFilesReadset, String>() {
        @Override public String apply(BamFilesReadset readset) {
          return readset.getDatasetId();
        }
      };

  public static final Function<BamFilesReadset, Readset> GET_READSET =
      new Function<BamFilesReadset, Readset>() {
        @Override public Readset apply(BamFilesReadset readset) {
          return readset.getReadset();
        }
      };

  public static final Function<BamFilesReadset, String> GET_READSET_ID =
      new Function<BamFilesReadset, String>() {
        @Override public String apply(BamFilesReadset readset) {
          return readset.getReadsetId();
        }
      };

  public static final Function<BamFilesReadset, String> GET_SAMPLE =
      new Function<BamFilesReadset, String>() {
        @Override public String apply(BamFilesReadset readset) {
          return readset.getSample();
        }
      };

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

  private final Supplier<Readset> readset = Suppliers.memoize(
      new Supplier<Readset>() {
        @Override public Readset get() {
          return Readset.create(
              getReadsetId(),
              getSample(),
              getDatasetId(),
              DateTimeUtils.currentTimeMillis(),
              FluentIterable.from(getBamFiles()).transform(BamFile.GET_FILE_DATA).toSortedList(new Comparator<FileData>() {
                @Override public int compare(FileData f1, FileData f2) {
                  return f1.getFileUri().compareTo(f2.getFileUri());
                }
              }));
        }
      });

  private final Supplier<SAMFileHeader> header = Suppliers.memoize(
      new Supplier<SAMFileHeader>() {
        @Override
        public SAMFileHeader get() {
          Collection<SAMFileHeader> headers =
              FluentIterable.from(bamFiles).transform(BamFile.GET_HEADER).toList();
          return new SamFileHeaderMerger(SortOrder.coordinate, headers, true).getMergedHeader();
        }
      });

  private BamFilesReadset(
      String readsetId,
      String sample,
      String datasetId,
      Set<IndexedBamFile> bamFiles) {
    this.readsetId = readsetId;
    this.sample = sample;
    this.datasetId = datasetId;
    this.bamFiles = bamFiles;
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

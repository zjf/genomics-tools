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

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.cloud.genomics.localrepo.BamFile.IndexedBamFile;
import com.google.cloud.genomics.localrepo.dto.Dataset;
import com.google.cloud.genomics.localrepo.dto.Readset;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

public class DatasetDirectory {

  private static class ReadGroupInfo {

    static ReadGroupInfo create(IndexedBamFile bamFile, String sample) {
      return new ReadGroupInfo(bamFile, sample);
    }

    private final IndexedBamFile bamFile;
    private final String sample;

    private ReadGroupInfo(IndexedBamFile bamFile, String sample) {
      this.bamFile = bamFile;
      this.sample = sample;
    }

    IndexedBamFile getBamFile() {
      return bamFile;
    }

    String getSample() {
      return sample;
    }
  }

  private static final Function<File, Iterable<BamFile>> CREATE_BAM_FILE =
      applyAsSet(BamFile.CREATE);

  private static final Function<BamFile, Iterable<IndexedBamFile>> CREATE_INDEXED_BAM_FILE =
      applyAsSet(IndexedBamFile.CREATE);

  private static final Function<IndexedBamFile, Iterable<ReadGroupInfo>> CREATE_READ_GROUP_INFOS =
      new Function<IndexedBamFile, Iterable<ReadGroupInfo>>() {
        @Override public Iterable<ReadGroupInfo> apply(final IndexedBamFile bamFile) {
          return FluentIterable.from(bamFile.getFileData().getReadGroups())
              .transform(
                  new Function<Readset.FileData.ReadGroup, ReadGroupInfo>() {
                    @Override public ReadGroupInfo apply(Readset.FileData.ReadGroup readGroup) {
                      return ReadGroupInfo.create(bamFile, readGroup.getSample());
                    }
                  });
        }
      };

  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  private static final Supplier<String> READSET_ID_GENERATOR =
      new Supplier<String>() {

        private final AtomicLong readsetId = new AtomicLong(0);

        @Override public String get() {
          return Long.toString(readsetId.incrementAndGet());
        }
      };

  private static <X, Y> Function<X, Iterable<Y>> applyAsSet(
      Function<X, Optional<Y>> function) {
    return input -> function.apply(input).asSet();
  }

  public static DatasetDirectory create(String datasetId, String directory) {
    return create(Dataset.create(datasetId, 0L, true), FILE_SYSTEM.getPath(directory));
  }

  public static DatasetDirectory create(Dataset dataset, Path directory) {
    return new DatasetDirectory(dataset, directory);
  }

  private static <X> Function<X, FluentIterable<X>> dfs(
      final Function<X, Iterable<X>> getChildren) {
    return
        new Function<X, FluentIterable<X>>() {
          @Override public FluentIterable<X> apply(final X initial) {
            return
                new FluentIterable<X>() {
                  @Override public Iterator<X> iterator() {
                    return
                        new AbstractIterator<X>() {

                          private final Queue<X> stack = createStack();

                          @Override protected X computeNext() {
                            X next = stack.poll();
                            if (null == next) {
                              return endOfData();
                            }
                            for (X child : getChildren.apply(next)) {
                              stack.offer(child);
                            }
                            return next;
                          }

                          private Queue<X> createStack() {
                            Queue<X> stack = Collections.asLifoQueue(Queues.<X>newArrayDeque());
                            stack.offer(initial);
                            return stack;
                          }
                        };
                  }
                };
          }
        };
  }

  private final Maps.EntryTransformer<String, Set<IndexedBamFile>, BamFilesReadset> createReadset =
      new Maps.EntryTransformer<String, Set<IndexedBamFile>, BamFilesReadset>() {
        @Override
        public BamFilesReadset transformEntry(String sample, Set<IndexedBamFile> bamFiles) {
          return BamFilesReadset.create(
              READSET_ID_GENERATOR.get(),
              sample,
              getDataset().getId(),
              bamFiles);
        }
      };

  private final Dataset dataset;
  private final Path directory;

  private final Supplier<Map<String, BamFilesReadset>> readsets = Suppliers.memoize(
      new Supplier<Map<String, BamFilesReadset>>() {
        @Override public Map<String, BamFilesReadset> get() {
          return FluentIterable
              .from(Maps
                  .transformEntries(
                      Maps.transformValues(
                          FluentIterable.from(Collections.singletonList(getDirectory()))
                              .transformAndConcat(dfs(path -> {
                                try {
                                  return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ? Files.newDirectoryStream(path)
                                      : Collections.<Path>emptyList();
                                } catch (Exception e) {
                                  throw Throwables.propagate(e);
                                }
                              }))
                              .transform(path -> path.toFile())
                              .transformAndConcat(CREATE_BAM_FILE)
                              .transformAndConcat(CREATE_INDEXED_BAM_FILE)
                              .transformAndConcat(CREATE_READ_GROUP_INFOS)
                              .index(info -> info.getSample())
                              .asMap(),
                          infos -> infos.stream().map(info -> info.getBamFile()).collect(Collectors.toSet())),
                      createReadset)
                  .values())
              .uniqueIndex(BamFilesReadset::getReadsetId);
        }
      });

  private DatasetDirectory(Dataset dataset, Path directory) {
    this.dataset = dataset;
    this.directory = directory;
  }

  @Override public boolean equals(Object obj) {
    if (null != obj && DatasetDirectory.class == obj.getClass()) {
      DatasetDirectory rhs = (DatasetDirectory) obj;
      return Objects.equals(getDataset(), rhs.getDataset())
          && Objects.equals(getDirectory(), rhs.getDirectory());
    }
    return false;
  }

  public Dataset getDataset() {
    return dataset;
  }

  public Path getDirectory() {
    return directory;
  }

  public Map<String, BamFilesReadset> getReadsets() {
    return readsets.get();
  }

  @Override public int hashCode() {
    return Objects.hash(getDataset(), getDirectory());
  }

  @Override public String toString() {
    return String.format("dataset: %s directory: %s", getDataset(), getDirectory());
  }
}

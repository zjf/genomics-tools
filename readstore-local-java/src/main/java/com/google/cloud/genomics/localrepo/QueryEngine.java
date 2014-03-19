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
import com.google.cloud.genomics.localrepo.dto.Read;
import com.google.cloud.genomics.localrepo.dto.SearchReadsRequest;
import com.google.cloud.genomics.localrepo.dto.SearchReadsResponse;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.PeekingIterator;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.SAMRecordComparator;
import net.sf.samtools.SAMRecordCoordinateComparator;
import net.sf.samtools.SAMRecordIterator;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class QueryEngine {

  private static class SAMRecordWithSkip implements Comparable<SAMRecordWithSkip> {

    private static final SAMRecordComparator COMPARATOR = new SAMRecordCoordinateComparator();

    final SAMRecord record;
    final int skip;

    SAMRecordWithSkip(SAMRecord record, int skip) {
      this.record = record;
      this.skip = skip;
    }

    @Override public int compareTo(SAMRecordWithSkip rhs) {
      int coordinateComparison = COMPARATOR.compare(record, rhs.record);
      return 0 == coordinateComparison ? skip - rhs.skip : coordinateComparison;
    }
  }

  private static final Function<FluentIterable<SAMRecord>, Iterable<SAMRecordWithSkip>> ADD_SKIPS =
      new Function<FluentIterable<SAMRecord>, Iterable<SAMRecordWithSkip>>() {
        @Override public Iterable<SAMRecordWithSkip> apply(FluentIterable<SAMRecord> stream) {
          return stream.transform(
              new Function<SAMRecord, SAMRecordWithSkip>() {

                private int skip = 0;

                @Override public SAMRecordWithSkip apply(SAMRecord record) {
                  return new SAMRecordWithSkip(record, skip++);
                }
              });
        }
      };

  private static final Function<DatasetDirectory, Set<String>> GET_READSET_IDS =
      new Function<DatasetDirectory, Set<String>>() {
        @Override public Set<String> apply(DatasetDirectory dataset) {
          return dataset.getReadsets().keySet();
        }
      };

  private static final Function<SAMRecord, String> GET_SAMPLE =
      new Function<SAMRecord, String>() {
        @Override public String apply(SAMRecord record) {
          return record.getReadGroup().getSample();
        }
      };

  private static final Logger LOGGER = Logger.getLogger(QueryEngine.class.getName());

  private static final Function<Long, Integer> LONG_TO_INT =
      new Function<Long, Integer>() {
        @Override public Integer apply(Long l) {
          return l.intValue();
        }
      };

  private static final Equivalence<SAMRecord> SAME_REFEREENCE_AND_START =
      new Equivalence<SAMRecord>() {

        @Override protected boolean doEquivalent(SAMRecord lhs, SAMRecord rhs) {
          return Objects.equals(lhs.getReferenceIndex(), rhs.getReferenceIndex())
              && Objects.equals(lhs.getAlignmentStart(), rhs.getAlignmentStart());
        }

        @Override protected int doHash(SAMRecord record) {
          return Objects.hash(record.getReferenceIndex(), record.getAlignmentStart());
        }
      };

  public static QueryEngine create(
      final Map<String, DatasetDirectory> datasets,
      final Map<String, BamFilesReadset> readsets,
      int pageSize) {
    return new QueryEngine(datasets, readsets, pageSize);
  }

  private static int toInt(Long l) {
    return Optional.fromNullable(l).transform(LONG_TO_INT).or(0);
  }

  private final Map<String, DatasetDirectory> datasets;
  private final Function<File, IndexedBamFile> getBamFile;
  private final Function<String, Set<String>> getReadsetIds;
  private final Map<String, BamFilesReadset> readsets;
  private final Map<String, String> readsetIdsBySample;
  private final int pageSize;

  private QueryEngine(
      final Map<String, DatasetDirectory> datasets,
      final Map<String, BamFilesReadset> readsets,
      int pageSize) {
    this.datasets = datasets;
    this.readsets = readsets;
    this.getReadsetIds =
        new Function<String, Set<String>>() {
          @Override public Set<String> apply(String datasetId) {
            return Optional.fromNullable(datasets.get(datasetId))
                .transform(GET_READSET_IDS)
                .or(Collections.<String>emptySet());
          }
        };
    this.getBamFile = Functions.forMap(FluentIterable
        .from(FluentIterable
            .from(readsets.values())
            .transformAndConcat(BamFilesReadset.GET_BAM_FILES)
            .toSet())
        .uniqueIndex(BamFile.GET_FILE));
    this.readsetIdsBySample = Maps.transformValues(
        FluentIterable.from(readsets.values()).uniqueIndex(BamFilesReadset.GET_SAMPLE),
        BamFilesReadset.GET_READSET_ID);
    this.pageSize = pageSize;
  }

  private QueryDescriptor createQueryDescriptor(SearchReadsRequest request) {
    String pageToken = request.getPageToken();
    if (null == pageToken) {
      List<String> datasetIds = request.getDatasetIds();
      final List<String> readsetIds = request.getReadsetIds();
      return QueryDescriptor.create(
          ImmutableMap.copyOf(Maps.transformValues(
              FluentIterable
                  .from(getReadsets(datasetIds, readsetIds)
                      .transformAndConcat(BamFilesReadset.GET_BAM_FILES)
                      .transform(BamFile.GET_FILE)
                      .toSet())
                  .uniqueIndex(Functions.<File>identity()),
              Functions.constant(QueryDescriptor.Start.create(
                  request.getSequenceName(),
                  toInt(request.getSequenceStart()),
                  0)))),
          toInt(request.getSequenceEnd()));
    }
    return QueryDescriptor.fromPageToken(pageToken);
  }

  private FluentIterable<BamFilesReadset> getReadsets(
      List<String> datasetIds,
      final List<String> readsetIds) {
    return FluentIterable
        .from(datasetIds.isEmpty() ? datasets.keySet() : datasetIds)
        .transformAndConcat(readsetIds.isEmpty() ? getReadsetIds :
            new Function<String, Collection<String>>() {
              @Override public Collection<String> apply(String input) {
                return readsetIds;
              }
            })
        .transform(Functions.forMap(readsets));
  }

  private static final Predicate<Iterator<?>> ITERATOR_HAS_NEXT =
      new Predicate<Iterator<?>>() {
        @Override public boolean apply(Iterator<?> iterator) {
          return iterator.hasNext();
        }
      };

  private SearchReadsResponse searchReads(
      Map<File, PeekingIterator<SAMRecordWithSkip>> iterators,
      final int end,
      Predicate<SAMRecord> readsetFilter) {
    ImmutableList.Builder<Read> reads = ImmutableList.builder();
    for (Iterator<SAMRecordWithSkip> iterator = Iterators.limit(
        Iterators.mergeSorted(iterators.values(), Ordering.<SAMRecordWithSkip>natural()), pageSize);
        iterator.hasNext();) {
      SAMRecord record = iterator.next().record;
      if (readsetFilter.apply(record)) {
        reads.add(read(record));
      }
    }
    Map<File, PeekingIterator<SAMRecordWithSkip>> nonEmptyIterators =
        Maps.filterValues(iterators, ITERATOR_HAS_NEXT);
    return SearchReadsResponse.create(
        reads.build(),
        nonEmptyIterators.isEmpty()
            ? null
            : QueryDescriptor
                .create(
                    ImmutableMap.copyOf(Maps.transformValues(nonEmptyIterators,
                        new Function<PeekingIterator<SAMRecordWithSkip>, QueryDescriptor.Start>() {
                          @Override
                          public QueryDescriptor.Start apply(
                              PeekingIterator<SAMRecordWithSkip> iterator) {
                            SAMRecordWithSkip peek = iterator.peek();
                            SAMRecord record = peek.record;
                            return QueryDescriptor.Start.create(record.getReferenceName(),
                                record.getAlignmentStart(), peek.skip);
                          }
                        })),
                    end)
                .toPageToken());
  }

  private static final Function<SAMTagAndValue, String> GET_TAG =
      new Function<SAMTagAndValue, String>() {
        @Override public String apply(SAMTagAndValue samTagAndValue) {
          return samTagAndValue.tag;
        }
      };

  private static final Function<SAMTagAndValue, String> GET_VALUE = Functions.compose(
      Functions.toStringFunction(),
      new Function<SAMTagAndValue, Object>() {
        @Override public Object apply(SAMTagAndValue samTagAndValue) {
          return samTagAndValue.value;
        }
      });

  private Read read(SAMRecord record) {
    return Read.create(
        toRead(record.getReadName()),
        toRead(record.getReadName()),
        readsetIdsBySample.get(record.getReadGroup().getSample()),
        record.getFlags(),
        toRead(record.getReferenceName()),
        toRead(record.getAlignmentStart(), 0),
        toRead(record.getMappingQuality(), 255),
        toRead(record.getCigarString()),
        toRead(record.getMateReferenceName()),
        toRead(record.getMateAlignmentStart(), 0),
        toRead(record.getInferredInsertSize(), 0),
        toRead(record.getReadString()),
        null,
        toRead(record.getBaseQualityString()),
        Maps.transformValues(
            FluentIterable.from(record.getAttributes()).uniqueIndex(GET_TAG),
            GET_VALUE));
  }

  private static String toRead(String value) {
    return "*".equals(value) ? null : value;
  }

  private static Integer toRead(int value, int specialValue) {
    return specialValue == value ? null : value;
  }

  private SearchReadsResponse searchReads(
      final QueryDescriptor descriptor,
      final Predicate<SAMRecord> readsetFilter) {
    abstract class RecursiveProcessor<X, Y, Z> {

      abstract void close(Y value);

      abstract Y open(X key);

      final Z process(Iterable<X> keys) {
        return process(keys.iterator(), ImmutableMap.<X, Y>builder());
      }

      private Z process(Iterator<X> iterator, ImmutableMap.Builder<X, Y> map) {
        if (iterator.hasNext()) {
          X key = iterator.next();
          Y value = null;
          try {
            return process(iterator, map.put(key, value = open(key)));
          } finally {
            if (null != value) {
              try {
                close(value);
              } catch (Exception e) {
                LOGGER.warning(e.getMessage());
              }
            }
          }
        }
        return process(map.build());
      }

      abstract Z process(Map<X, Y> map);
    }
    return new RecursiveProcessor<Map.Entry<File, QueryDescriptor.Start>, SAMFileReader,
        SearchReadsResponse>() {

      @Override
      void close(SAMFileReader reader) {
        reader.close();
      }

      @Override
      SAMFileReader open(Map.Entry<File, QueryDescriptor.Start> entry) {
        return getBamFile.apply(entry.getKey()).open();
      }

      @Override
      SearchReadsResponse process(
          Map<Map.Entry<File, QueryDescriptor.Start>, SAMFileReader> map) {
        final int end = descriptor.getEnd();
        return new RecursiveProcessor<
            Map.Entry<Map.Entry<File, QueryDescriptor.Start>, SAMFileReader>, SAMRecordIterator,
            SearchReadsResponse>() {

          @Override
          void close(SAMRecordIterator iterator) {
            iterator.close();
          }

          @Override
          SAMRecordIterator open(
              Map.Entry<Map.Entry<File, QueryDescriptor.Start>, SAMFileReader> entry) {
            QueryDescriptor.Start interval = entry.getKey().getValue();
            SAMRecordIterator iterator = entry.getValue().queryOverlapping(
                interval.getSequence(),
                interval.getStart(),
                end);
            int skip = interval.getSkip();
            for (int i = 0; iterator.hasNext() && i < skip; ++i) {
              iterator.next();
            }
            return iterator;
          }

          private <X> FluentIterable<FluentIterable<X>> partition(final Iterator<X> iterator,
              final Equivalence<? super X> equivalence) {
            return new FluentIterable<FluentIterable<X>>() {
              @Override
              public Iterator<FluentIterable<X>> iterator() {
                return new AbstractIterator<FluentIterable<X>>() {

                  private final PeekingIterator<X> delegate = Iterators.peekingIterator(iterator);

                  @Override
                  protected FluentIterable<X> computeNext() {
                    if (delegate.hasNext()) {
                      X first = delegate.next();
                      ImmutableList.Builder<X> list = ImmutableList.<X>builder().add(first);
                      for (Predicate<? super X> p = equivalence.equivalentTo(first);
                          delegate.hasNext() && p.apply(delegate.peek());
                          list.add(delegate.next()));
                      return FluentIterable.from(list.build());
                    }
                    return endOfData();
                  }
                };
              }
            };
          }

          @Override
          SearchReadsResponse process(Map<
              Map.Entry<Map.Entry<File, QueryDescriptor.Start>, SAMFileReader>,
              SAMRecordIterator> map) {
            ImmutableMap.Builder<File, PeekingIterator<SAMRecordWithSkip>> iterators =
                ImmutableMap.builder();
            for (Map.Entry<Map.Entry<Map.Entry<File, QueryDescriptor.Start>, SAMFileReader>,
                SAMRecordIterator> entry : map.entrySet()) {
              iterators.put(entry.getKey().getKey().getKey(), Iterators.peekingIterator(
                  partition(entry.getValue(), SAME_REFEREENCE_AND_START)
                  .transformAndConcat(ADD_SKIPS).iterator()));
            }
            return searchReads(iterators.build(), end, readsetFilter);
          }
        }.process(map.entrySet());
      }
    }.process(descriptor.getStarts().entrySet());
  }

  public SearchReadsResponse searchReads(SearchReadsRequest request) {
    return searchReads(createQueryDescriptor(request), Predicates.compose(Predicates.in(
        getReadsets(request.getDatasetIds(), request.getReadsetIds())
        .transform(BamFilesReadset.GET_READSET_ID).toSet()),
        Functions.compose(Functions.forMap(readsetIdsBySample), GET_SAMPLE)));
  }
}

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
import com.google.cloud.genomics.localrepo.util.Functions;
import com.google.cloud.genomics.localrepo.util.Maps;
import com.google.cloud.genomics.localrepo.util.Predicates;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordComparator;
import net.sf.samtools.SAMRecordCoordinateComparator;
import net.sf.samtools.SAMRecordIterator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

  private static final Function<Stream<SAMRecord>, Stream<SAMRecordWithSkip>> ADD_SKIPS =
      stream -> stream.map(
          new Function<SAMRecord, SAMRecordWithSkip>() {

            private int skip = 0;

            @Override public SAMRecordWithSkip apply(SAMRecord record) {
              return new SAMRecordWithSkip(record, skip++);
            }
          });

  private static final Logger LOGGER = Logger.getLogger(QueryEngine.class.getName());

  public static QueryEngine create(
      final Map<String, DatasetDirectory> datasets,
      final Map<String, BamFilesReadset> readsets,
      int pageSize) {
    return new QueryEngine(datasets, readsets, pageSize);
  }

  private static int toInt(Long l) {
    return Optional.ofNullable(l).map(x -> x.intValue()).orElse(0);
  }

  private final Map<String, DatasetDirectory> datasets;
  private final Function<File, IndexedBamFile> getBamFile;
  private final Function<String, Stream<String>> getReadsetIds;
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
        datasetId -> Optional.ofNullable(datasets.get(datasetId))
            .map(dataset -> dataset.getReadsets().keySet()).orElse(Collections.<String>emptySet())
            .stream();
    this.getBamFile =
        Functions.forMap(readsets.values().stream().flatMap(flatMap(BamFilesReadset::getBamFiles))
            .collect(Collectors.toSet()).stream()
            .collect(Collectors.toMap(BamFile::getFile, Function.identity())));
    this.readsetIdsBySample =
        readsets.values().stream()
            .collect(Collectors.toMap(BamFilesReadset::getSample, BamFilesReadset::getReadsetId));
    this.pageSize = pageSize;
  }

  private static <X, Y> Function<X, Stream<Y>> flatMap(
      Function<? super X, ? extends Iterable<Y>> function) {
    return x -> StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(function.apply(x).iterator(), Spliterator.IMMUTABLE),
        false);
  }

  private QueryDescriptor createQueryDescriptor(SearchReadsRequest request) {
    String pageToken = request.getPageToken();
    if (null == pageToken) {
      List<String> datasetIds = request.getDatasetIds();
      final List<String> readsetIds = request.getReadsetIds();
      return QueryDescriptor.create(
          new HashMap<>(getReadsets(datasetIds, readsetIds)
              .flatMap(flatMap(BamFilesReadset::getBamFiles))
              .map(BamFile::getFile)
              .collect(Collectors.toSet())
              .stream()
              .collect(
                  Collectors.toMap(Function.identity(), Functions.constant(QueryDescriptor.Start
                      .create(request.getSequenceName(), toInt(request.getSequenceStart()), 0))))),
          toInt(request.getSequenceEnd()));
    }
    return QueryDescriptor.fromPageToken(pageToken);
  }

  private Stream<BamFilesReadset> getReadsets(
      List<String> datasetIds,
      final List<String> readsetIds) {
    return (datasetIds.isEmpty() ? datasets.keySet() : datasetIds).stream()
        .flatMap(readsetIds.isEmpty() ? getReadsetIds : input -> readsetIds.stream()).map(Functions.forMap(readsets));
  }

  private SearchReadsResponse searchReads(
      Map<File, PeekingIterator<SAMRecordWithSkip>> iterators,
      final int end,
      Predicate<SAMRecord> readsetFilter) {
    List<Read> reads = new ArrayList<>();
    for (Iterator<SAMRecordWithSkip> iterator = Iterators.limit(
        Iterators.mergeSorted(iterators.values(), Comparator.naturalOrder()), pageSize);
        iterator.hasNext();) {
      SAMRecord record = iterator.next().record;
      if (readsetFilter.test(record)) {
        reads.add(read(record));
      }
    }
    Map<File, PeekingIterator<SAMRecordWithSkip>> nonEmptyIterators =
        Maps.filterValues(iterators, iterator -> iterator.hasNext());
    return SearchReadsResponse.create(
        reads,
        nonEmptyIterators.isEmpty()
            ? null
            : QueryDescriptor
                .create(
                    new HashMap<>(Maps.transformValues(nonEmptyIterators,
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
        record
            .getAttributes()
            .stream()
            .collect(
                Collectors.toMap(attribute -> attribute.tag,
                    attribute -> attribute.value.toString())));
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
        return process(keys.iterator(), new HashMap<>());
      }

      private Z process(Iterator<X> iterator, Map<X, Y> map) {
        if (iterator.hasNext()) {
          X key = iterator.next();
          Y value = null;
          try {
            map.put(key, value = open(key));
            return process(iterator, map);
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
        return process(map);
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

          private <Y, X extends Y> Stream<Stream<X>> partition(final Iterator<X> iterator,
              final BiPredicate<Y, Y> equivalence) {
            return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<Stream<X>>(
                    Long.MAX_VALUE, Spliterator.IMMUTABLE) {

                  private final PeekingIterator<X> delegate = Iterators.peekingIterator(iterator);

                  @Override public boolean tryAdvance(Consumer<? super Stream<X>> action) {
                    if (delegate.hasNext()) {
                      List<X> list = new ArrayList<>();
                      X first = delegate.next();
                      for (list.add(first); delegate.hasNext()
                          && equivalence.test(first, delegate.peek()); list.add(delegate.next()));
                      action.accept(list.stream());
                      return true;
                    }
                    return false;
                  }
                },
                false);
          }

          @Override
          SearchReadsResponse process(Map<
              Map.Entry<Map.Entry<File, QueryDescriptor.Start>, SAMFileReader>,
              SAMRecordIterator> map) {
            Map<File, PeekingIterator<SAMRecordWithSkip>> iterators =
                new HashMap<>();
            for (Map.Entry<Map.Entry<Map.Entry<File, QueryDescriptor.Start>, SAMFileReader>,
                SAMRecordIterator> entry : map.entrySet()) {
              iterators.put(entry.getKey().getKey().getKey(), Iterators.peekingIterator(partition(
                  entry.getValue(),
                  (lhs, rhs) -> Objects.equals(lhs.getReferenceIndex(), rhs.getReferenceIndex())
                      && Objects.equals(lhs.getAlignmentStart(), rhs.getAlignmentStart()))
                  .map(ADD_SKIPS).flatMap(Function.identity()).iterator()));
            }
            return searchReads(iterators, end, readsetFilter);
          }
        }.process(map.entrySet());
      }
    }.process(descriptor.getStarts().entrySet());
  }

  public SearchReadsResponse searchReads(SearchReadsRequest request) {
    return searchReads(
        createQueryDescriptor(request),
        Predicates.compose(
            Predicates.in(getReadsets(request.getDatasetIds(), request.getReadsetIds()).map(
                BamFilesReadset::getReadsetId).collect(Collectors.toSet())),
            Functions.forMap(readsetIdsBySample).compose(
                record -> record.getReadGroup().getSample())));
  }
}

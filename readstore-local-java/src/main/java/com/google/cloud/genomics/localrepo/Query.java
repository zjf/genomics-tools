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
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import net.sf.picard.sam.MergingSamRecordIterator;
import net.sf.picard.sam.SamFileHeaderMerger;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.util.CloseableIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class Query<X> {

  public static class Builder {

    private int end = 0;
    private final Map<String, String> readsetIdsBySample;
    private final String sequenceName;
    private int start = 0;
    private final Collection<String> readsetIds;

    private Builder(
        Map<String, String> readsetIdsBySample,
        Collection<String> readsetIds,
        String sequenceName) {
      this.readsetIdsBySample = readsetIdsBySample;
      this.sequenceName = sequenceName;
      this.readsetIds = readsetIds;
    }

    public <X> Query<X> build(Function<FluentIterable<Read>, X> callback) {
      return new Query<>(readsetIdsBySample, readsetIds, sequenceName, start, end, callback);
    }

    public Builder setEnd(int end) {
      this.end = end;
      return this;
    }

    public Builder setStart(int start) {
      this.start = start;
      return this;
    }
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

  public static Builder builder(
      Map<String, String> readsetIdsBySample,
      Collection<String> readsetIds,
      String sequenceName) {
    return new Builder(readsetIdsBySample, readsetIds, sequenceName);
  }

  private final Function<FluentIterable<Read>, X> callback;

  private final Function<SAMRecord, Read> createRead =
      new Function<SAMRecord, Read>() {

        @Override public Read apply(SAMRecord record) {
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

        private String toRead(String value) {
          return "*".equals(value) ? null : value;
        }

        private Integer toRead(int value, int specialValue) {
          return specialValue == value ? null : value;
        }
      };

  private final int end;
  private final Map<String, String> readsetIdsBySample;
  private final String sequenceName;
  private final int start;
  private final Predicate<SAMRecord> desiredReadset;

  private Query(
      Map<String, String> readsetIdsBySample,
      Collection<String> readsetIds,
      String sequenceName,
      int start,
      int end,
      Function<FluentIterable<Read>, X> callback) {
    this.readsetIdsBySample = readsetIdsBySample;
    this.sequenceName = sequenceName;
    this.start = start;
    this.end = end;
    this.callback = callback;
    this.desiredReadset = Predicates.compose(
        Predicates.in(readsetIds),
        Functions.compose(
            Functions.forMap(readsetIdsBySample),
            new Function<SAMRecord, String>() {
              @Override public String apply(SAMRecord record) {
                return record.getReadGroup().getSample();
              }
            }));
  }

  public X search(Iterable<IndexedBamFile> bamFiles) {
    return search(
        bamFiles.iterator(),
        ImmutableList.<SAMFileReader>builder(),
        ImmutableList.<SAMFileHeader>builder());
  }

  private X search(
      Iterator<IndexedBamFile> bamFiles,
      ImmutableCollection.Builder<SAMFileReader> readers,
      ImmutableCollection.Builder<SAMFileHeader> headers) {
    if (bamFiles.hasNext()) {
      try (SAMFileReader reader = bamFiles.next().open()) {
        return search(bamFiles, readers.add(reader), headers.add(reader.getFileHeader()));
      }
    }
    return search(
        headers.build(),
        readers.build().iterator(),
        ImmutableMap.<SAMFileReader, CloseableIterator<SAMRecord>>builder());
  }

  private X search(
      final Collection<SAMFileHeader> headers,
      Iterator<SAMFileReader> readers,
      final ImmutableMap.Builder<SAMFileReader, CloseableIterator<SAMRecord>> iterators) {
    if (readers.hasNext()) {
      SAMRecordIterator iterator = null;
      try {
        SAMFileReader reader = readers.next();
        return search(
            headers,
            readers,
            iterators.put(reader, iterator = reader.query(sequenceName, start, end, true)));
      } finally {
        if (null != iterator) {
          iterator.close();
        }
      }
    }
    return callback.apply(
        new FluentIterable<SAMRecord>() {
          @Override public Iterator<SAMRecord> iterator() {
            return new MergingSamRecordIterator(
                new SamFileHeaderMerger(SortOrder.coordinate, headers, true),
                iterators.build(),
                true);
          }
        }.filter(desiredReadset).transform(createRead));
  }
}

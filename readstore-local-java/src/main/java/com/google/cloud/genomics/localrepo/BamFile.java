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

import com.google.cloud.genomics.localrepo.dto.Readset;
import com.google.cloud.genomics.localrepo.util.Suppliers;
import com.google.common.base.Optional;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMSequenceRecord;

import java.io.File;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BamFile {

  public static class IndexedBamFile extends BamFile {

    public static Optional<IndexedBamFile> create(BamFile bamFile) {
      File file = bamFile.getFile();
      String path = file.getAbsolutePath();
      File index = new File(String.format("%s.bai", path));
      if (isReadableFile(index)) {
        return Optional.of(new IndexedBamFile(file, index));
      }
      LOGGER.warning(String.format("BAM file \"%s\" has no index", path));
      return Optional.<IndexedBamFile>absent();
    }

    private final File index;

    private IndexedBamFile(File file, File index) {
      super(file);
      this.index = index;
    }

    @Override SAMFileReader createReader() {
      return new SAMFileReader(file, index);
    }
  }

  public static Optional<BamFile> create(File file) {
    return isReadableFile(file) && file.getName().endsWith(".bam")
        ? Optional.of(new BamFile(file))
        : Optional.<BamFile>absent();
  }

  private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance();

  private static final Logger LOGGER = Logger.getLogger(BamFile.class.getName());

  private static boolean isReadableFile(File file) {
    return file.isFile() && file.canRead();
  }

  final File file;

  private final Supplier<SAMFileHeader> header = Suppliers.memoize(
      new Supplier<SAMFileHeader>() {
        @Override public SAMFileHeader get() {
          try (SAMFileReader reader = open()) {
            return reader.getFileHeader();
          }
        }
      });

  private final Supplier<Readset.FileData> fileData = Suppliers.memoize(Suppliers.compose(
      new Function<SAMFileHeader, Readset.FileData>() {
        @Override public Readset.FileData apply(SAMFileHeader fileHeader) {
        return Readset.FileData.create(
            String.format("file://%s", file.getAbsolutePath()),
            Arrays.asList(Readset.FileData.Header.create(
                fileHeader.getVersion(),
                fileHeader.getSortOrder().toString())),
            fileHeader.getSequenceDictionary().getSequences().stream()
                .map(record -> Readset.FileData.RefSequence.create(
                    record.getSequenceName(),
                    record.getSequenceLength(),
                    record.getAssembly(),
                    record.getAttribute(SAMSequenceRecord.MD5_TAG),
                    record.getSpecies(),
                    record.getAttribute(SAMSequenceRecord.URI_TAG)))
                .collect(Collectors.toList()),
            fileHeader.getReadGroups().stream()
                .map(record -> {
                  Date runDate = record.getRunDate();
                  return Readset.FileData.ReadGroup.create(record.getId(), record.getSequencingCenter(),
                      record.getDescription(), null == runDate ? null : DATE_FORMAT.format(runDate),
                      record.getFlowOrder(), record.getKeySequence(), record.getLibrary(),
                      record.getAttribute("PG"), record.getPredictedMedianInsertSize(), record.getPlatform(),
                      record.getPlatformUnit(), record.getSample());
                })
                .collect(Collectors.toList()),
            fileHeader.getProgramRecords().stream()
                .map(record -> Readset.FileData.Program.create(
                    record.getId(),
                    record.getProgramName(),
                    record.getCommandLine(),
                    record.getPreviousProgramGroupId(),
                    record.getProgramVersion()))
                .collect(Collectors.toList()),
            fileHeader.getComments());
        }
      },
      header));

  private BamFile(final File bamFile) {
    file = bamFile;
  }

  SAMFileReader createReader() {
    return new SAMFileReader(file);
  }

  @Override public final boolean equals(Object obj) {
    return null != obj
        && BamFile.class.isAssignableFrom(obj.getClass())
        && Objects.equals(getFile(), ((BamFile) obj).getFile());
  }

  public final File getFile() {
    return file;
  }

  public final Readset.FileData getFileData() {
    return fileData.get();
  }

  SAMFileHeader getHeader() {
    return header.get();
  }

  @Override public final int hashCode() {
    return getFile().hashCode();
  }

  final SAMFileReader open() {
    SAMFileReader reader = createReader();
    reader.setValidationStringency(ValidationStringency.SILENT);
    return reader;
  }

  @Override public final String toString() {
    return getFile().toString();
  }
}

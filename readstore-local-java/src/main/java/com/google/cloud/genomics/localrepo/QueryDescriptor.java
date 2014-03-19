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

import com.google.common.base.Throwables;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.DatatypeConverter;

public final class QueryDescriptor implements Serializable {

  public static final class Start implements Serializable {

    public static Start create(String sequence, int start, int skip) {
      return new Start(sequence, start, skip);
    }

    private final String sequence;
    private final int skip;
    private final int start;

    private Start(String sequence, int start, int skip) {
      this.sequence = sequence;
      this.start = start;
      this.skip = skip;
    }

    @Override
    public boolean equals(Object obj) {
      if (null != obj && Start.class == obj.getClass()) {
        Start rhs = (Start) obj;
        return Objects.equals(getSequence(), rhs.getSequence())
            && Objects.equals(getStart(), rhs.getStart())
            && Objects.equals(getSkip(), rhs.getSkip());
      }
      return false;
    }

    public String getSequence() {
      return sequence;
    }

    public int getSkip() {
      return skip;
    }

    public int getStart() {
      return start;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getSequence(), getStart(), getSkip());
    }

    @Override
    public String toString() {
      return String.format("(%s, %d, %d, %d)", getSequence(), getStart(), getSkip());
    }
  }

  public static <M extends Map<File, Start> & Serializable> QueryDescriptor create(
      M starts, int end) {
    return new QueryDescriptor(starts, end);
  }

  public static QueryDescriptor fromPageToken(String pageToken) {
    try {
      return (QueryDescriptor) new ObjectInputStream(
          new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(pageToken))).readObject();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private final Map<File, Start> starts;
  private final int end;

  private QueryDescriptor(Map<File, Start> intervals, int end) {
    this.starts = intervals;
    this.end = end;
  }

  @Override
  public boolean equals(Object obj) {
    if (null != obj && QueryDescriptor.class == obj.getClass()) {
      QueryDescriptor rhs = (QueryDescriptor) obj;
      return Objects.equals(getStarts(), rhs.getStarts())
          && Objects.equals(getEnd(), rhs.getEnd());
    }
    return false;
  }

  public Map<File, Start> getStarts() {
    return starts;
  }

  public int getEnd() {
    return end;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getStarts(), getEnd());
  }

  public String toPageToken() {
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      new ObjectOutputStream(buffer).writeObject(this);
      return DatatypeConverter.printBase64Binary(buffer.toByteArray());
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString() {
    return String.format("starts: %s end: %s", getStarts(), getEnd());
  }
}

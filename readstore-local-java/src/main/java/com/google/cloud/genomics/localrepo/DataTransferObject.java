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

import static java.beans.Introspector.getBeanInfo;

import com.google.common.base.Throwables;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public abstract class DataTransferObject {

  static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().setSerializationInclusion(Inclusion.NON_NULL);

  private static final ObjectWriter OBJECT_WRITER =
      OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

  @Override public final boolean equals(Object obj) {
    Class<?> clazz = getClass();
    if (null != obj && clazz == obj.getClass()) {
      try {
        for (PropertyDescriptor descriptor : getBeanInfo(clazz).getPropertyDescriptors()) {
          Method method = descriptor.getReadMethod();
          if (!Objects.equals(method.invoke(this), method.invoke(obj))) {
            return false;
          }
        }
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
      return true;
    }
    return false;
  }

  @Override public final int hashCode() {
    try {
      Collection<Object> values = new ArrayList<>();
      for (PropertyDescriptor descriptor : getBeanInfo(getClass()).getPropertyDescriptors()) {
        values.add(descriptor.getReadMethod().invoke(this));
      }
      return Objects.hash(values.toArray());
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override public final String toString() {
    try {
      return OBJECT_WRITER.writeValueAsString(this);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}

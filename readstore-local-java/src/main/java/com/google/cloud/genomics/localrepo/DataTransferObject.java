/*
 *Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.genomics.localrepo;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.module.SimpleModule;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

public abstract class DataTransferObject {

  protected static class ReflectiveHashCodeAndEquals<T extends DataTransferObject> {

    public static <T extends DataTransferObject> ReflectiveHashCodeAndEquals<T> create(
        Class<T> clazz) {
      try {
        ImmutableList.Builder<Method> accessors = ImmutableList.builder();
        for (PropertyDescriptor descriptor :
            Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
          accessors.add(descriptor.getReadMethod());
        }
        return new ReflectiveHashCodeAndEquals<>(clazz, accessors.build());
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    private static Object invoke(Method accessor, Object target) {
      try {
        return accessor.invoke(target);
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    private final List<Method> accessors;
    private final Class<T> clazz;

    private ReflectiveHashCodeAndEquals(Class<T> clazz, List<Method> accessors) {
      this.clazz = clazz;
      this.accessors = accessors;
    }

    public boolean equals(T lhs, Object rhs) {
      if (null != rhs && clazz == rhs.getClass()) {
        for (Method accessor : accessors) {
          if (!Objects.equals(invoke(accessor, lhs), invoke(accessor, rhs))) {
            return false;
          }
        }
        return true;
      }
      return false;
    }

    public int hashCode(T obj) {
      Object[] values = new Object[accessors.size()];
      for (
          ListIterator<Method> iterator = accessors.listIterator();
          iterator.hasNext();
          values[iterator.nextIndex()] = invoke(iterator.next(), obj));
      return Objects.hash(values);
    }
  }

  static final ObjectMapper OBJECT_MAPPER;

  static {
    (OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(Inclusion.NON_NULL))
        .registerModule(new SimpleModule("test", new Version(1, 0, 0, null))
            .addDeserializer(Map.class,
                new JsonDeserializer<Map<?, ?>>() {

                  @Override public Map<?, ?> deserialize(JsonParser jp, DeserializationContext ctxt)
                      throws IOException, JsonProcessingException {
                    return jp.readValueAs(HashMap.class);
                  }

                  @Override public Map<?, ?> getNullValue() {
                    return Collections.EMPTY_MAP;
                  }
                })
            .addDeserializer(List.class,
                new JsonDeserializer<List<?>>() {

                  @Override public List<?> deserialize(JsonParser jp, DeserializationContext ctxt)
                      throws IOException, JsonProcessingException {
                    return jp.readValueAs(ArrayList.class);
                  }

                  @Override public List<?> getNullValue() {
                    return Collections.EMPTY_LIST;
                  }
                }));
  }

  private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

  @Override public abstract boolean equals(Object obj);

  @Override public abstract int hashCode();

  @Override public final String toString() {
    try {
      return OBJECT_WRITER.writeValueAsString(this);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}

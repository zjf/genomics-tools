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
package com.google.cloud.genomics.localrepo.util;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Maps {

  public static <X, Y> Map<X, Y> filterValues(Map<X, Y> unfiltered,
      final Predicate<? super Y> valuePredicate) {
    return com.google.common.collect.Maps.filterValues(unfiltered, valuePredicate::test);
  }

  public static <X, Y, Z> Map<X, Z> transformValues(Map<X, Y> fromMap,
      Function<? super Y, Z> function) {
    return com.google.common.collect.Maps.transformValues(fromMap, function::apply);
  }

  private Maps() {}
}

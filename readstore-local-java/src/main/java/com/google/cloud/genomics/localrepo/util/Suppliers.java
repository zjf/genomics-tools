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

import java.util.function.Function;
import java.util.function.Supplier;

public final class Suppliers {

  public static <F, T> Supplier<T> compose(Function<? super F, T> function, Supplier<F> supplier) {
    return com.google.common.base.Suppliers.compose(function::apply, supplier::get)::get;
  }

  public static <X> Supplier<X> memoize(final Supplier<X> supplier) {
    return com.google.common.base.Suppliers.memoize(supplier::get)::get;
  }

  private Suppliers() {}
}

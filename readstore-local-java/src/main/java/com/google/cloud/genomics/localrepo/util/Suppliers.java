package com.google.cloud.genomics.localrepo.util;

import java.util.function.Supplier;

public final class Suppliers {

  public static <X> Supplier<X> memoize(final Supplier<X> supplier) {
    return com.google.common.base.Suppliers.memoize(supplier::get)::get;
  }

  private Suppliers() {}
}

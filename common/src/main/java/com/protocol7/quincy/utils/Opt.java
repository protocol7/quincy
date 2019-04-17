package com.protocol7.quincy.utils;

import java.util.Optional;

public class Opt {
  public static String toString(Optional<?> opt) {
    if (opt.isPresent()) {
      return "[" + opt.get().toString() + "]";
    } else {
      return "[]";
    }
  }

  public static String toStringBytes(Optional<byte[]> opt) {
    if (opt.isPresent()) {
      return "[" + Hex.hex(opt.get()) + "]";
    } else {
      return "[]";
    }
  }
}

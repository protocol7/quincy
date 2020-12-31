package com.protocol7.quincy.protocol;

import com.google.common.base.Preconditions;
import com.protocol7.quincy.Varint;

public class PacketNumber {

  public static long parse(final byte[] b) {
    if (b.length < 1 || b.length > 4) {
      throw new IllegalArgumentException("Invalid packet buffer length");
    }

    long number = 0;
    for (int i = 0; i < b.length; i++) {
      number = number << 8 | (b[i] & 0xFF);
    }
    return number;
  }

  public static final long MIN = 0;
  public static final long MAX = 0xFFFFFFFFL;

  public static long validate(final long number) {
    Preconditions.checkArgument(number >= 0);
    Preconditions.checkArgument(number <= Varint.MAX);
    return number;
  }

  public static long next(final long number) {
    return number + 1;
  }

  private static int getLength(final long number) {
    if (number < MIN) {
      throw new IllegalArgumentException("number too small");
    } else if (number <= 0xFF) {
      return 1;
    } else if (number <= 0xFFFF) {
      return 2;
    } else if (number <= 0xFFFFFF) {
      return 3;
    } else if (number <= 0xFFFFFFFFL) {
      return 4;
    } else {
      throw new IllegalArgumentException("number too large");
    }
  }

  public static byte[] write(final long number) {
    if (number < MIN || number > MAX) {
      throw new IllegalArgumentException("Invalid number");
    }

    final int length = getLength(number);
    final byte[] b = new byte[length];
    for (int j = length; j > 0; j--) {
      b[length - j] = (byte) ((number >> (8 * (j - 1))) & 0xFF);
    }
    return b;
  }

  private PacketNumber() {}
}

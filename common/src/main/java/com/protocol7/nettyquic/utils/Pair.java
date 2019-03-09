package com.protocol7.nettyquic.utils;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class Pair<F, S> {

  private final F first;
  private final S second;

  public Pair(final F first, final S second) {
    this.first = requireNonNull(first);
    this.second = requireNonNull(second);
  }

  public F getFirst() {
    return first;
  }

  public S getSecond() {
    return second;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Pair<?, ?> pair = (Pair<?, ?>) o;
    return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, second);
  }
}

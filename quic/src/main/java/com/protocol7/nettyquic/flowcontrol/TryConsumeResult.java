package com.protocol7.nettyquic.flowcontrol;

public class TryConsumeResult {
  private final boolean success;
  private final long connectionOffset;
  private final long connectionMax;
  private final long streamOffset;
  private final long streamMax;

  public TryConsumeResult(
      final boolean success,
      final long connectionOffset,
      final long connectionMax,
      final long streamOffset,
      final long streamMax) {
    this.success = success;
    this.connectionOffset = connectionOffset;
    this.connectionMax = connectionMax;
    this.streamOffset = streamOffset;
    this.streamMax = streamMax;
  }

  public boolean isSuccess() {
    return success;
  }

  public long getConnectionOffset() {
    return connectionOffset;
  }

  public long getConnectionMax() {
    return connectionMax;
  }

  public long getStreamOffset() {
    return streamOffset;
  }

  public long getStreamMax() {
    return streamMax;
  }
}

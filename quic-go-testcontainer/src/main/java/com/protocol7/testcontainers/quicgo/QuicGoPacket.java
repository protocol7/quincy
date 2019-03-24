package com.protocol7.testcontainers.quicgo;

import java.util.Arrays;
import java.util.Objects;

public class QuicGoPacket {

  public final boolean inbound;
  public final boolean longHeader;

  public final String type;

  public final byte[] destinationConnectionId;
  public final byte[] sourceConnectionId;

  public final long packetNumber;

  public QuicGoPacket(
      final boolean inbound,
      final boolean longHeader,
      final String type,
      final byte[] destinationConnectionId,
      final byte[] sourceConnectionId,
      final long packetNumber) {
    this.inbound = inbound;
    this.longHeader = longHeader;
    this.type = type;
    this.destinationConnectionId = destinationConnectionId;
    this.sourceConnectionId = sourceConnectionId;
    this.packetNumber = packetNumber;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final QuicGoPacket that = (QuicGoPacket) o;
    return inbound == that.inbound
        && longHeader == that.longHeader
        && packetNumber == that.packetNumber
        && Objects.equals(type, that.type)
        && Arrays.equals(destinationConnectionId, that.destinationConnectionId)
        && Arrays.equals(sourceConnectionId, that.sourceConnectionId);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(inbound, longHeader, type, packetNumber);
    result = 31 * result + Arrays.hashCode(destinationConnectionId);
    result = 31 * result + Arrays.hashCode(sourceConnectionId);
    return result;
  }

  @Override
  public String toString() {
    return "QuicGoPacket{"
        + "inbound="
        + inbound
        + ", longHeader="
        + longHeader
        + ", type='"
        + type
        + '\''
        + ", destinationConnectionId="
        + destinationConnectionId
        + ", sourceConnectionId="
        + sourceConnectionId
        + ", packetNumber="
        + packetNumber
        + '}';
  }
}

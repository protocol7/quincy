package com.protocol7.nettyquick.tls.extensions;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Shorts;
import java.util.EnumSet;

public enum TransportParameterType {

  ORIGINAL_CONNECTION_ID(0x0000),
  IDLE_TIMEOUT(0x0001),
  STATELESS_RESET_TOKEN(0x0002),
  MAX_PACKET_SIZE(0x0003),
  INITIAL_MAX_DATA(0x0004),
  INITIAL_MAX_STREAM_DATA_BIDI_LOCAL(0x0005),
  INITIAL_MAX_STREAM_DATA_BIDI_REMOTE(0x0006),
  INITIAL_MAX_STREAM_DATA_UNI(0x0007),
  INITIAL_MAX_BIDI_STREAMS(0x0008),
  INITIAL_MAX_UNI_STREAMS(0x0009),
  ACK_DELAY_EXPONENT(0x000a),
  MAX_ACK_DELAY(0x000b),
  DISABLE_MIGRATION(0x000c),
  PREFERRED_ADDRESS(0x000d);

  public static TransportParameterType fromValue(byte[] value) {
    Preconditions.checkArgument(value.length == 2);

    return fromValue(value[0] << 8 | value[1]);
  }

  public static TransportParameterType fromValue(int value) {
    for (TransportParameterType tp : EnumSet.allOf(TransportParameterType.class)) {
      if (tp.value == value) {
        return tp;
      }
    }
    throw new IllegalArgumentException("Unknown value: " + value);
  }

  private final short value;

  TransportParameterType(int value) {
    this.value = (short) value;
  }

  public int getValue() {
    return value;
  }

  public byte[] asBytes() {
    return Shorts.toByteArray(value);
  }
}

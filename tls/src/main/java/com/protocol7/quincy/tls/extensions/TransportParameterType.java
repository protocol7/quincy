package com.protocol7.quincy.tls.extensions;

import com.google.common.base.Preconditions;
import java.util.EnumSet;

public enum TransportParameterType {
  ORIGINAL_DESTINATION_CONNECTION_ID(0x0000),
  MAX_IDLE_TIMEOUT(0x0001),
  STATELESS_RESET_TOKEN(0x0002),
  MAX_UDP_PACKET_SIZE(0x0003),
  INITIAL_MAX_DATA(0x0004),
  INITIAL_MAX_STREAM_DATA_BIDI_LOCAL(0x0005),
  INITIAL_MAX_STREAM_DATA_BIDI_REMOTE(0x0006),
  INITIAL_MAX_STREAM_DATA_UNI(0x0007),
  INITIAL_MAX_STREAMS_BIDI(0x0008),
  INITIAL_MAX_STREAMS_UNI(0x0009),
  ACK_DELAY_EXPONENT(0x000a),
  MAX_ACK_DELAY(0x000b),
  DISABLE_ACTIVE_MIGRATION(0x000c),
  PREFERRED_ADDRESS(0x000d),
  ACTIVE_CONNECTION_ID_LIMIT(0x000e),
  INITIAL_SOURCE_CONNECTION_ID(0x000f),
  RETRY_SOURCE_CONNECTION_ID(0x0010);

  public static TransportParameterType fromValue(final byte[] value) {
    Preconditions.checkArgument(value.length == 2);

    return fromValue(value[0] << 8 | value[1]);
  }

  public static TransportParameterType fromValue(final int value) {
    for (final TransportParameterType tp : EnumSet.allOf(TransportParameterType.class)) {
      if (tp.value == value) {
        return tp;
      }
    }
    throw new IllegalArgumentException("Unknown value: " + value);
  }

  private final short value;

  TransportParameterType(final int value) {
    this.value = (short) value;
  }

  public int getValue() {
    return value;
  }
}

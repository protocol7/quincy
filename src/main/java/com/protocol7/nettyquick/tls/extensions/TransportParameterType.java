package com.protocol7.nettyquick.tls.extensions;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Shorts;

import java.util.EnumSet;

public enum TransportParameterType {

    initial_max_stream_data_bidi_local(0),
    initial_max_data(1),
    initial_max_bidi_streams(2),
    idle_timeout(3),
    preferred_address(4),
    max_packet_size(5),
    stateless_reset_token(6),
    ack_delay_exponent(7),
    initial_max_uni_streams(8),
    disable_migration(9),
    initial_max_stream_data_bidi_remote(10),
    initial_max_stream_data_uni(11),
    max_ack_delay(12),
    original_connection_id(13);

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
        this.value = (short)value;
    }

    public int getValue() {
        return value;
    }

    public byte[] asBytes() {
        return Shorts.toByteArray(value);
    }
}
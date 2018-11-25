package com.protocol7.nettyquick.protocol;

public enum TransportError {

    NO_ERROR(0x0),
    INTERNAL_ERROR (0x1),
    SERVER_BUSY (0x2),
    FLOW_CONTROL_ERROR (0x3),
    STREAM_ID_ERROR (0x4),
    STREAM_STATE_ERROR (0x5),
    FINAL_OFFSET_ERROR (0x6),
    FRAME_ENCODING_ERROR (0x7),
    TRANSPORT_PARAMETER_ERROR (0x8),
    VERSION_NEGOTIATION_ERROR (0x9),
    PROTOCOL_VIOLATION (0xA);

    private final int value;

    TransportError(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

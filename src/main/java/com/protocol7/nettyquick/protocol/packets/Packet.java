package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

import java.util.Optional;

public interface Packet {

    int PACKET_TYPE_MASK = 0b10000000;

    static Packet parse(ByteBuf bb, Header.LastPacketNumberProvider lastAcked) {
        byte firstByte = bb.getByte(bb.readerIndex());

        if ((PACKET_TYPE_MASK & firstByte) == PACKET_TYPE_MASK) {
            // Long header packet
            if (firstByte == InitialPacket.MARKER) {
                return InitialPacket.parse(bb);
            }
            return null;
        } else {
            // short header packet
            return null;
        }
    }

    Packet addFrame(Frame frame);

    void write(ByteBuf bb);

    PacketNumber getPacketNumber();

    Optional<ConnectionId> getSourceConnectionId();
    Optional<ConnectionId> getDestinationConnectionId();

    PacketType getPacketType();

    Payload getPayload();
}

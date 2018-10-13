package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

import java.util.Optional;

public interface Packet {

    int PACKET_TYPE_MASK = 0b10000000;

    static Packet parse(ByteBuf bb, LastPacketNumber lastAcked) {
        bb.markReaderIndex();
        int firstByte = bb.readByte() & 0xFF;

        if ((PACKET_TYPE_MASK & firstByte) == PACKET_TYPE_MASK) {
            // Long header packet

            // might be a ver neg packet, so we must check the version
            Version version = Version.read(bb);
            bb.resetReaderIndex();

            if (version == Version.VERSION_NEGOTIATION) {
                return VersionNegotiationPacket.parse(bb);
            } else if (firstByte == InitialPacket.MARKER) {
                return InitialPacket.parse(bb);
            } else if (firstByte == HandshakePacket.MARKER) {
                return HandshakePacket.parse(bb);
            } else {
                throw new RuntimeException("Unknown long header packet");
            }
        } else {
            // short header packet
            bb.resetReaderIndex();
            return ShortPacket.parse(bb, lastAcked);
        }
    }

    void write(ByteBuf bb);

    Optional<ConnectionId> getSourceConnectionId();
    Optional<ConnectionId> getDestinationConnectionId();
}

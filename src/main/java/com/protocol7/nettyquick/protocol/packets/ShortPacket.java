package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

import java.util.Optional;

public class ShortPacket implements Packet {

    public static ShortPacket parse(ByteBuf bb, LastPacketNumber lastAcked) {
        return new ShortPacket(ShortHeader.parse(bb, lastAcked));
    }

    private final ShortHeader header;

    public ShortPacket(ShortHeader header) {
        this.header = header;
    }

    @Override
    public Packet addFrame(Frame frame) {
        return new ShortPacket(ShortHeader.addFrame(header, frame));
    }

    @Override
    public void write(ByteBuf bb) {
        header.write(bb);
    }

    @Override
    public PacketNumber getPacketNumber() {
        return header.getPacketNumber();
    }

    @Override
    public Optional<ConnectionId> getSourceConnectionId() {
        return Optional.empty();
    }

    @Override
    public Optional<ConnectionId> getDestinationConnectionId() {
        return header.getDestinationConnectionId();
    }

    @Override
    public Payload getPayload() {
        return header.getPayload();
    }
}

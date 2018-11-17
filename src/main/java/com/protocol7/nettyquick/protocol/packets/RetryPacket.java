package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.utils.Hex;
import com.protocol7.nettyquick.utils.Opt;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;

import java.util.Optional;

public class RetryPacket implements Packet {

    public static int MARKER = 0x80 | PacketType.Retry.getType();

    public static RetryPacket parse(ByteBuf bb) {
        bb.readByte(); // TODO verify marker
        Version version = Version.read(bb);

        int cil = bb.readByte() & 0xFF;
        int dcil = ConnectionId.firstLength(cil);
        int scil = ConnectionId.lastLength(cil);

        Optional<ConnectionId> destConnId = ConnectionId.readOptional(dcil, bb);
        Optional<ConnectionId> srcConnId = ConnectionId.readOptional(scil, bb);

        int odcil = ConnectionId.lastLength(bb.readByte() & 0xFF);
        ConnectionId orgConnId = ConnectionId.readOptional(odcil, bb).get();

        byte[] retryToken = new byte[bb.readableBytes()];
        bb.readBytes(retryToken);

        return new RetryPacket(version, destConnId, srcConnId, orgConnId, retryToken);
    }

    private final Version version;
    private final Optional<ConnectionId> destinationConnectionId;
    private final Optional<ConnectionId> sourceConnectionId;
    private final ConnectionId originalConnectionId;
    private final byte[] retryToken;

    public RetryPacket(Version version, Optional<ConnectionId> destinationConnectionId,
                       Optional<ConnectionId> sourceConnectionId,
                       ConnectionId originalConnectionId,
                       byte[] retryToken) {
        this.version = version;
        this.destinationConnectionId = destinationConnectionId;
        this.sourceConnectionId = sourceConnectionId;
        this.originalConnectionId = originalConnectionId;
        this.retryToken = retryToken;
    }

    public Optional<ConnectionId> getDestinationConnectionId() {
        return destinationConnectionId;
    }

    @Override
    public void write(ByteBuf bb, AEAD aead) {
        int marker = 0b10000000 | PacketType.Retry.getType();
        bb.writeByte(marker);

        version.write(bb);

        bb.writeByte(ConnectionId.joinLenghts(destinationConnectionId, sourceConnectionId));
        if (destinationConnectionId.isPresent()) {
            destinationConnectionId.get().write(bb);
        }
        if (sourceConnectionId.isPresent()) {
            sourceConnectionId.get().write(bb);
        }

        int odcil = Rnd.rndInt() & 0xF0; // lower 4 bits must be 0
        odcil |= ((originalConnectionId.getLength() - 3) & 0b1111);
        bb.writeByte(odcil);

        originalConnectionId.write(bb);

        bb.writeBytes(retryToken);
    }

    public Optional<ConnectionId> getSourceConnectionId() {
        return sourceConnectionId;
    }

    public ConnectionId getOriginalConnectionId() {
        return originalConnectionId;
    }

    public byte[] getRetryToken() {
        return retryToken;
    }

    @Override
    public String toString() {
        return "RetryPacket{" +
                "version=" + version +
                ", destinationConnectionId=" + Opt.toString(destinationConnectionId) +
                ", sourceConnectionId=" + Opt.toString(sourceConnectionId) +
                ", originalConnectionId=" + originalConnectionId +
                ", retryToken=" + Hex.hex(retryToken) +
                '}';
    }
}

package com.protocol7.nettyquick.protocol;

import java.util.Optional;

import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

public class ShortHeader implements Header {

  public static ShortHeader parse(ByteBuf bb, LastPacketNumber lastAckedProvider) {
    byte firstByte = bb.readByte();

    boolean keyPhase = (firstByte & 0x40) == 0x40;

    Optional<ConnectionId> connId = Optional.of(ConnectionId.read(12, bb)); // TODO how to determine length?
    PacketNumber lastAcked = lastAckedProvider.getLastAcked(connId.get());
    PacketNumber packetNumber = PacketNumber.read(bb);
    ProtectedPayload payload = ProtectedPayload.parse(bb);

    return new ShortHeader(keyPhase,
                           connId,
                           packetNumber,
                           payload);
  }

  public static ShortHeader addFrame(ShortHeader packet, Frame frame) {
    return new ShortHeader(packet.keyPhase,
                           packet.connectionId,
                           packet.packetNumber,
                           packet.payload.addFrame(frame));
  }

  private final boolean keyPhase;
  private final Optional<ConnectionId> connectionId;
  private final PacketNumber packetNumber;
  private final ProtectedPayload payload;

  public ShortHeader(final boolean keyPhase, final Optional<ConnectionId> connectionId, final PacketNumber packetNumber, final ProtectedPayload payload) {
    this.keyPhase = keyPhase;
    this.connectionId = connectionId;
    this.packetNumber = packetNumber;
    this.payload = payload;
  }

  public boolean isKeyPhase() {
    return keyPhase;
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
    return connectionId;
  }

  public PacketNumber getPacketNumber() {
    return packetNumber;
  }

  public ProtectedPayload getPayload() {
    return payload;
  }

  public void write(ByteBuf bb) {
    byte b = 0;
    if (keyPhase) {
      b = (byte)(b | 0x40);
    }
    b = (byte)(b | 0x20); // constant
    b = (byte)(b | 0x10); // constant
    bb.writeByte(b);

    connectionId.get().write(bb);
    packetNumber.write(bb);
    payload.write(bb);
  }

  @Override
  public String toString() {
    return "ShortHeader{" +
            ", connectionId=" + connectionId +
            ", packetNumber=" + packetNumber +
            ", payload=" + payload +
            '}';
  }
}

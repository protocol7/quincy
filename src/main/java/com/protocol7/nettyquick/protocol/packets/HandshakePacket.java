package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.AEADProvider;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class HandshakePacket implements FullPacket {

  public static int MARKER = 0x80 | PacketType.Handshake.getType();

  public static HandshakePacket create(
      Optional<ConnectionId> destConnectionId,
      Optional<ConnectionId> srcConnectionId,
      PacketNumber packetNumber,
      Version version,
      Frame... frames) {
    return create(destConnectionId, srcConnectionId, packetNumber, version, Arrays.asList(frames));
  }

  public static HandshakePacket create(
      Optional<ConnectionId> destConnectionId,
      Optional<ConnectionId> srcConnectionId,
      PacketNumber packetNumber,
      Version version,
      List<Frame> frames) {
    Payload payload = new Payload(frames);
    return new HandshakePacket(
        new LongHeader(
            PacketType.Handshake,
            destConnectionId,
            srcConnectionId,
            version,
            packetNumber,
            payload));
  }

  public static HandshakePacket parse(ByteBuf bb, AEADProvider aeadProvider) {
    // TODO validate marker

    LongHeader header = LongHeader.parse(bb, true, aeadProvider);
    return new HandshakePacket(header);
  }

  private final LongHeader header;

  private HandshakePacket(LongHeader header) {
    this.header = header;
  }

  @Override
  public PacketType getType() {
    return PacketType.Handshake;
  }

  @Override
  public Packet addFrame(Frame frame) {
    return new HandshakePacket(LongHeader.addFrame(header, frame));
  }

  @Override
  public void write(ByteBuf bb, AEAD aead) {
    header.write(bb, aead);
  }

  @Override
  public PacketNumber getPacketNumber() {
    return header.getPacketNumber();
  }

  @Override
  public Optional<ConnectionId> getSourceConnectionId() {
    return header.getSourceConnectionId();
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
    return header.getDestinationConnectionId();
  }

  @Override
  public Payload getPayload() {
    return header.getPayload();
  }

  @Override
  public String toString() {
    return "HandshakePacket{" + "header=" + header + '}';
  }
}

package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.AEADProvider;
import io.netty.buffer.ByteBuf;
import java.util.Optional;

public class ShortPacket implements FullPacket {

  public static ShortPacket parse(
      ByteBuf bb, LastPacketNumber lastAcked, AEADProvider aead, int connidLength) {
    return new ShortPacket(ShortHeader.parse(bb, lastAcked, aead, connidLength));
  }

  private final ShortHeader header;

  public ShortPacket(ShortHeader header) {
    this.header = header;
  }

  @Override
  public PacketType getType() {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Packet addFrame(Frame frame) {
    return new ShortPacket(ShortHeader.addFrame(header, frame));
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

  @Override
  public String toString() {
    return "ShortPacket{" + "header=" + header + '}';
  }
}

package com.protocol7.nettyquick.protocol.packets;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.LongHeader;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.UnprotectedPayload;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

import java.util.Optional;

public class HandshakePacket implements Packet {

  public static HandshakePacket create(Optional<ConnectionId> destConnectionId,
                                       Optional<ConnectionId> srcConnectionId,
                                  PacketNumber packetNumber,
                                  Version version) { // TODO take crypto params
    UnprotectedPayload payload = new UnprotectedPayload(Lists.newArrayList()); // TODO random stream and ack frames
    return new HandshakePacket(new LongHeader(PacketType.Handshake,
            destConnectionId,
            srcConnectionId,
            version,
            packetNumber,
            payload));
  }

  private final LongHeader header;

  public HandshakePacket(LongHeader header) {
    this.header = header;
  }

  @Override
  public Packet addFrame(Frame frame) {
    return new HandshakePacket(LongHeader.addFrame(header, frame));
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
    return header.getSourceConnectionId();
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
    return header.getDestinationConnectionId();
  }

  @Override
  public PacketType getPacketType() {
    return header.getPacketType();
  }

  @Override
  public UnprotectedPayload getPayload() {
    return header.getPayload();
  }
}

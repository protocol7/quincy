package com.protocol7.nettyquick.protocol.packets;

import java.util.List;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.LongPacket;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.PaddingFrame;

public class InitialPacket extends LongPacket {

  public static InitialPacket create(ConnectionId connectionId) { // TODO take crypto params
    Version version = Version.CURRENT;
    PacketNumber packetNumber = PacketNumber.random();

    List<Frame> frames = Lists.newArrayList();
    int length = 0; // TODO handshake stream frame
    while (length < 1200) {
      frames.add(PaddingFrame.INSTANCE);
      length += PaddingFrame.INSTANCE.getLength();
    }
    Payload payload = new Payload(frames);
    return new InitialPacket(connectionId, version, packetNumber, payload);
  }

  private InitialPacket(final ConnectionId connectionId, final Version version, final PacketNumber packetNumber, final Payload payload) {
    super(PacketType.Initial, connectionId, version, packetNumber, payload);
  }
}

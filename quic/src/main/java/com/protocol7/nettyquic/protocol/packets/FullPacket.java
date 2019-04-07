package com.protocol7.nettyquic.protocol.packets;

import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.Payload;
import com.protocol7.nettyquic.protocol.frames.Frame;

public interface FullPacket extends Packet {

  PacketType getType();

  FullPacket addFrame(Frame frame);

  PacketNumber getPacketNumber();

  Payload getPayload();
}

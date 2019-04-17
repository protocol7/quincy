package com.protocol7.quincy.protocol.packets;

import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Payload;
import com.protocol7.quincy.protocol.frames.Frame;

public interface FullPacket extends Packet {

  PacketType getType();

  FullPacket addFrame(Frame frame);

  PacketNumber getPacketNumber();

  Payload getPayload();
}

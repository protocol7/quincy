package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.frames.Frame;

public interface FullPacket extends Packet {

    PacketType getType();

    Packet addFrame(Frame frame);

    PacketNumber getPacketNumber();

    Payload getPayload();

}

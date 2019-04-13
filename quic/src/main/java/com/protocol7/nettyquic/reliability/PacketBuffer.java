package com.protocol7.nettyquic.reliability;

import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PacketBuffer {

  private final Map<PacketNumber, Packet> buffer = new ConcurrentHashMap<>();

  public void put(final FullPacket packet) {
    buffer.put(packet.getPacketNumber(), packet);
  }

  public boolean remove(final PacketNumber pn) {
    return buffer.remove(pn) != null;
  }

  public boolean contains(final PacketNumber packetNumber) {
    return buffer.containsKey(packetNumber);
  }

  public boolean isEmpty() {
    return buffer.isEmpty();
  }
}

package com.protocol7.nettyquic.reliability;

import static java.util.Objects.requireNonNull;

import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PacketBuffer {

  private final Map<PacketNumber, Packet> buffer = new ConcurrentHashMap<>();

  public void put(final FullPacket packet) {
    requireNonNull(packet);
    buffer.put(packet.getPacketNumber(), packet);
  }

  public boolean remove(final PacketNumber packetNumber) {
    requireNonNull(packetNumber);
    return buffer.remove(packetNumber) != null;
  }

  public boolean contains(final PacketNumber packetNumber) {
    requireNonNull(packetNumber);
    return buffer.containsKey(packetNumber);
  }

  public boolean isEmpty() {
    return buffer.isEmpty();
  }
}

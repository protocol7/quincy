package com.protocol7.quincy.netty;

import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.protocol.packets.Packet;

public class InternalQuicPacket {

  private final Connection connection;
  private final Packet packet;

  public InternalQuicPacket(final Connection connection, final Packet packet) {
    this.connection = connection;
    this.packet = packet;
  }

  public Connection getConnection() {
    return connection;
  }

  public Packet getPacket() {
    return packet;
  }
}

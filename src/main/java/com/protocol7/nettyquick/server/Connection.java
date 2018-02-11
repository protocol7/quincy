package com.protocol7.nettyquick.server;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.parser.PacketParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

public class Connection {

  public static Connection create(StreamHandler handler, Channel channel, InetSocketAddress clientAddress) {
    return new Connection(ConnectionId.create(), handler, channel, clientAddress);
  }

  private final ConnectionId connId;
  private final StreamHandler handler;
  private final Channel channel;
  private final InetSocketAddress clientAddress;
  private final AtomicReference<Version> version = new AtomicReference<>(Version.DRAFT_09);
  private final AtomicReference<PacketNumber> lastPacketNumber = new AtomicReference<>(new PacketNumber(2));
  private final ServerStreams streams = new ServerStreams();
  private final PacketParser packetParser = new PacketParser();

  public Connection(final ConnectionId connId, final StreamHandler handler, final Channel channel, final InetSocketAddress clientAddress) {
    this.connId = connId;
    this.handler = handler;
    this.channel = channel;
    this.clientAddress = clientAddress;
  }

  public ConnectionId getConnectionId() {
    return connId;
  }

  public Version getVersion() {
    return version.get();
  }

  public void sendPacket(Packet p) {
    ByteBuf bb = packetParser.serialize(p);
    channel.writeAndFlush(new DatagramPacket(bb, clientAddress)).syncUninterruptibly().awaitUninterruptibly(); // TODO fix
    System.out.println("c sent packet to " + clientAddress);

  }

  public void onPacket(Packet packet) {
    // TODO if stream frame
    ServerStream stream = streams.getOrCreate(new StreamId(1), this, handler); // TODO get stream ID from frame
    stream.onData(new byte[0]);
  }

  public PacketNumber nextPacketNumber() {
    return lastPacketNumber.getAndUpdate(packetNumber -> packetNumber.next());
  }
}

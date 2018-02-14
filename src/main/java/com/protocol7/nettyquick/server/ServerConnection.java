package com.protocol7.nettyquick.server;

import java.net.InetSocketAddress;
import java.util.Optional;
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

public class ServerConnection {

  public static ServerConnection create(StreamHandler handler, Channel channel, InetSocketAddress clientAddress) {
    return new ServerConnection(handler, channel, clientAddress);
  }

  private Optional<ConnectionId> connectionId = Optional.empty();
  private final StreamHandler handler;
  private final Channel channel;
  private final InetSocketAddress clientAddress;
  private final AtomicReference<Version> version = new AtomicReference<>(Version.DRAFT_09);
  private final AtomicReference<PacketNumber> lastPacketNumber = new AtomicReference<>(new PacketNumber(2));
  private final ServerStreams streams = new ServerStreams();
  private final PacketParser packetParser = new PacketParser();
  private final ServerStateMachine stateMachine;

  public ServerConnection(final StreamHandler handler, final Channel channel, final InetSocketAddress clientAddress) {
    this.handler = handler;
    this.channel = channel;
    this.clientAddress = clientAddress;
    this.stateMachine = new ServerStateMachine(this);
  }

  public Optional<ConnectionId> getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(ConnectionId connectionId) {
    this.connectionId = Optional.of(connectionId);
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
    stateMachine.processPacket(packet);
  }

  public ServerStream getOrCreateStream(StreamId streamId) {
    return streams.getOrCreate(streamId, this, handler);
  }


  public PacketNumber nextPacketNumber() {
    return lastPacketNumber.getAndUpdate(packetNumber -> packetNumber.next());
  }
}

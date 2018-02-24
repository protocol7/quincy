package com.protocol7.nettyquick.server;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.protocol7.nettyquick.Connection;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketBuffer;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.parser.PacketParser;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.streams.Streams;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerConnection implements Connection {

  public static ServerConnection create(StreamListener handler, Channel channel, InetSocketAddress clientAddress) {
    return new ServerConnection(handler, channel, clientAddress);
  }

  private final Logger log = LoggerFactory.getLogger(ServerConnection.class);

  private Optional<ConnectionId> connectionId = Optional.empty();
  private final StreamListener handler;
  private final Channel channel;
  private final InetSocketAddress clientAddress;
  private final AtomicReference<Version> version = new AtomicReference<>(Version.DRAFT_09);
  private final AtomicReference<PacketNumber> lastPacketNumber = new AtomicReference<>(PacketNumber.MIN);
  private final Streams streams = new Streams();
  private final PacketParser packetParser = new PacketParser();
  private final ServerStateMachine stateMachine;
  private final PacketBuffer packetBuffer;

  public ServerConnection(final StreamListener handler, final Channel channel, final InetSocketAddress clientAddress) {
    this.handler = handler;
    this.channel = channel;
    this.clientAddress = clientAddress;
    this.stateMachine = new ServerStateMachine(this);
    this.packetBuffer = new PacketBuffer(this, this::sendPacketUnbuffered);
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
    packetBuffer.send(p);
  }

  private void sendPacketUnbuffered(Packet packet) {
    ByteBuf bb = packetParser.serialize(packet);
    channel.writeAndFlush(new DatagramPacket(bb, clientAddress)).syncUninterruptibly().awaitUninterruptibly(); // TODO fix
    log.debug("c sent packet to " + clientAddress);

    log.debug("Server sent {}", packet);
  }

  public void onPacket(Packet packet) {
    log.debug("Server got {}", packet);

    lastPacketNumber.getAndAccumulate(packet.getPacketNumber(), (pn1, pn2) -> pn1.compareTo(pn2) > 0 ? pn1 : pn2);

    log.debug("Update packet number {}", lastPacketNumber.get());

    packetBuffer.onPacket(packet); // TODO connection ID is not set yet for initial packet so will be acknowdgeled with incorrect conn ID
    stateMachine.processPacket(packet);
  }

  public Stream getOrCreateStream(StreamId streamId) {
    return streams.getOrCreate(streamId, this, handler);
  }


  public PacketNumber nextPacketNumber() {
    return lastPacketNumber.updateAndGet(packetNumber -> packetNumber.next());
  }
}

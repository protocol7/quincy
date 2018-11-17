package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.protocol.packets.ShortPacket;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.streams.Streams;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.NullAEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ServerConnection implements Connection {

  public static ServerConnection create(StreamListener handler,
                                        Channel channel,
                                        InetSocketAddress clientAddress,
                                        ConnectionId srcConnId) {
    return new ServerConnection(handler, channel, clientAddress, srcConnId);
  }

  private final Logger log = LoggerFactory.getLogger(ServerConnection.class);

  private Optional<ConnectionId> destConnectionId = Optional.empty();
  private final Optional<ConnectionId> srcConnectionId;
  private final StreamListener handler;
  private final Channel channel;
  private final InetSocketAddress clientAddress;
  private final AtomicReference<Version> version = new AtomicReference<>(Version.CURRENT);
  private final AtomicReference<PacketNumber> sendPacketNumber = new AtomicReference<>(PacketNumber.MIN);
  private final Streams streams;
  private final ServerStateMachine stateMachine;
  private final PacketBuffer packetBuffer;

  private final AEAD initialAead;

  public ServerConnection(final StreamListener handler,
                          final Channel channel,
                          final InetSocketAddress clientAddress,
                          final ConnectionId srcConnId) {
    this.handler = handler;
    this.channel = channel;
    this.clientAddress = clientAddress;
    this.stateMachine = new ServerStateMachine(this);
    this.streams = new Streams(this);
    this.packetBuffer = new PacketBuffer(this, this::sendPacketUnbuffered, this.streams);

    this.srcConnectionId = Optional.of(srcConnId);

    this.initialAead = NullAEAD.create(srcConnId, true);
  }

  public Optional<ConnectionId> getDestinationConnectionId() {
    return destConnectionId;
  }
  public Optional<ConnectionId> getSourceConnectionId() {
    return srcConnectionId;
  }

  public void setDestinationConnectionId(ConnectionId destConnectionId) {
    this.destConnectionId = Optional.of(destConnectionId);
  }

  public Version getVersion() {
    return version.get();
  }

  public Packet sendPacket(Packet p) {
    packetBuffer.send(p, initialAead);
    return p;
  }

  public FullPacket sendPacket(Frame... frames) {
    return (FullPacket)sendPacket(new ShortPacket(new ShortHeader(false,
                                  getDestinationConnectionId(),
                                  nextSendPacketNumber(),
                                  new Payload(frames))));
  }

  private void sendPacketUnbuffered(Packet packet) {
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, initialAead);
    channel.writeAndFlush(new DatagramPacket(bb, clientAddress)).syncUninterruptibly().awaitUninterruptibly(); // TODO fix
    log.debug("Server sent {}", packet);
  }

  public void onPacket(Packet packet) {
    log.debug("Server got {}", packet);

    packetBuffer.onPacket(packet, initialAead); // TODO connection ID is not set yet for initial packet so will be acknowdgeled with incorrect conn ID
    stateMachine.processPacket(packet);
  }

  @Override
  public AEAD getAEAD(PacketType packetType) {
    if (packetType == PacketType.Initial) {
      return initialAead;
    } else if (packetType == PacketType.Handshake) {
      throw new RuntimeException("Not implemented");
    } else {
      throw new RuntimeException("Not implemented");
    }
  }

  public Stream getOrCreateStream(StreamId streamId) {
    return streams.getOrCreate(streamId, handler);
  }

  public PacketNumber lastAckedPacketNumber() {
    return packetBuffer.getLargestAcked();
  }

  public PacketNumber nextSendPacketNumber() {
    return sendPacketNumber.updateAndGet(packetNumber -> packetNumber.next());
  }
}

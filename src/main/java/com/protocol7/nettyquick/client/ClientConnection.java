package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.protocol.packets.ShortPacket;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.streams.Streams;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ClientConnection implements Connection {

  private final Logger log = LoggerFactory.getLogger(ClientConnection.class);

  private final Optional<ConnectionId> destConnectionId = Optional.empty();
  private final Optional<ConnectionId> srcConnectionId;
  private final EventExecutorGroup group;
  private final Channel channel;
  private final InetSocketAddress serverAddress;
  private final StreamListener streamListener;

  private final AtomicReference<Version> version = new AtomicReference<>(Version.CURRENT);
  private final AtomicReference<PacketNumber> sendPacketNumber = new AtomicReference<>(new PacketNumber(1)); // TODO fix
  private final PacketBuffer packetBuffer;
  private final ClientStateMachine stateMachine;

  private final Streams streams;

  public ClientConnection(final ConnectionId srcConnectionId, final NioEventLoopGroup group, final Channel channel, final InetSocketAddress serverAddress, final StreamListener streamListener) {
    this.srcConnectionId = Optional.ofNullable(srcConnectionId);
    this.group = group;
    this.channel = channel;
    this.serverAddress = serverAddress;
    this.streamListener = streamListener;
    this.stateMachine = new ClientStateMachine(this);
    this.streams = new Streams(this);
    this.packetBuffer = new PacketBuffer(this, this::sendPacketUnbuffered, this.streams);

  }

  public Future<Void> handshake() {
    MDC.put("actor", "client");
    return stateMachine.handshake();
  }

  public Packet sendPacket(Packet p) {
    packetBuffer.send(p);
    return p;
  }

  public Packet sendPacket(Frame... frames) {
    return sendPacket(new ShortPacket(new ShortHeader(false,
                               false,
                               PacketType.Four_octets,
                               getDestinationConnectionId(),
                               nextSendPacketNumber(),
                               new Payload(frames))));
  }

  @Override
  public Optional<ConnectionId> getSourceConnectionId() {
    return Optional.empty();
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
    return Optional.empty();
  }

  private void sendPacketUnbuffered(Packet packet) {
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb);
    channel.writeAndFlush(new DatagramPacket(bb, serverAddress)).syncUninterruptibly().awaitUninterruptibly(); // TODO fix
    log.debug("Client sent {}", packet);
  }

  public void onPacket(Packet packet) {
    log.debug("Client got {}", packet);

    packetBuffer.onPacket(packet);
    stateMachine.processPacket(packet);
  }

  public Version getVersion() {
    return version.get();
  }

  public PacketNumber lastAckedPacketNumber() {
    return packetBuffer.getLargestAcked();
  }

  public PacketNumber nextSendPacketNumber() {
    return sendPacketNumber.updateAndGet(packetNumber -> packetNumber.next());
  }

  public Stream openStream() {
    return streams.openStream(true, true, streamListener);
  }

  public Stream getOrCreateStream(StreamId streamId) {
    return streams.getOrCreate(streamId, streamListener);
  }

  public Future<?> close() {
    return group.shutdownGracefully();
  }
}

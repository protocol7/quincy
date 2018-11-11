package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.protocol.packets.ShortPacket;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.streams.Streams;
import com.protocol7.nettyquick.tls.AEAD;
import com.protocol7.nettyquick.tls.NullAEAD;
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

  private Optional<ConnectionId> destConnectionId;
  private Optional<ConnectionId> srcConnectionId = Optional.empty();
  private final EventExecutorGroup group;
  private final Channel channel;
  private final InetSocketAddress serverAddress;
  private final StreamListener streamListener;

  private final AtomicReference<Version> version = new AtomicReference<>(Version.CURRENT);
  private final AtomicReference<PacketNumber> sendPacketNumber = new AtomicReference<>(new PacketNumber(1)); // TODO fix
  private final PacketBuffer packetBuffer;
  private final ClientStateMachine stateMachine;

  private final Streams streams;

  private AEAD initialAead;
  private AEAD handshakeAead;

  public ClientConnection(final ConnectionId destConnectionId,
                          final NioEventLoopGroup group,
                          final Channel channel,
                          final InetSocketAddress serverAddress,
                          final StreamListener streamListener) {
    this.destConnectionId = Optional.ofNullable(destConnectionId);
    this.group = group;
    this.channel = channel;
    this.serverAddress = serverAddress;
    this.streamListener = streamListener;
    this.stateMachine = new ClientStateMachine(this);
    this.streams = new Streams(this);
    this.packetBuffer = new PacketBuffer(this, this::sendPacketUnbuffered, this.streams);

    initAEAD();
  }

  private void initAEAD() {
    this.initialAead = NullAEAD.create(destConnectionId.get(), true);

    System.out.println("Using AEAD: " + initialAead);
  }

  public Future<Void> handshake() {
    MDC.put("actor", "client");
    return stateMachine.handshake();
  }

  public Packet sendPacket(Packet p) {
    packetBuffer.send(p, initialAead);
    return p;
  }

  public FullPacket sendPacket(Frame... frames) {
    return (FullPacket) sendPacket(new ShortPacket(new ShortHeader(false,
                               getDestinationConnectionId(),
                               nextSendPacketNumber(),
                               new ProtectedPayload(frames))));
  }

  @Override
  public Optional<ConnectionId> getSourceConnectionId() {
    return srcConnectionId;
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
    return destConnectionId;
  }

  public void setSourceConnectionId(Optional<ConnectionId> srcConnId) {
    this.srcConnectionId = srcConnId;
  }

  public void setDestinationConnectionId(Optional<ConnectionId> destConnId) {
    this.destConnectionId = destConnId;
    initAEAD();
  }

  private void sendPacketUnbuffered(Packet packet) {
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, initialAead);
    channel.writeAndFlush(new DatagramPacket(bb, serverAddress)).syncUninterruptibly().awaitUninterruptibly(); // TODO fix
    log.debug("Client sent {}", packet);
  }

  public void onPacket(Packet packet) {
    log.debug("Client got {}", packet);

    packetBuffer.onPacket(packet, initialAead);
    stateMachine.processPacket(packet);
  }

  @Override
  public AEAD getAEAD(PacketType packetType) {
    if (packetType == PacketType.Initial) {
      return initialAead;
    } else if (packetType == PacketType.Handshake) {
      return handshakeAead;
    } else {
      throw new RuntimeException("Not implemented");
    }
  }

  public void setHandshakeAead(AEAD handshakeAead) {
    this.handshakeAead = handshakeAead;
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

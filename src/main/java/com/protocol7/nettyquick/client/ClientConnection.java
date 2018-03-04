package com.protocol7.nettyquick.client;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketBuffer;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.ShortPacket;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.Frame;
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

public class ClientConnection implements Connection {

  private final Logger log = LoggerFactory.getLogger(ClientConnection.class);

  private final ConnectionId connectionId;
  private final EventExecutorGroup group;
  private final Channel channel;
  private final InetSocketAddress serverAddress;
  private final StreamListener streamListener;

  private final AtomicReference<Version> version = new AtomicReference<>(Version.DRAFT_09);
  private final AtomicReference<PacketNumber> sendPacketNumber = new AtomicReference<>(new PacketNumber(1)); // TODO fix
  private final PacketBuffer packetBuffer;
  private final ClientStateMachine stateMachine;

  private final Streams streams;

  public ClientConnection(final ConnectionId connectionId, final NioEventLoopGroup group, final Channel channel, final InetSocketAddress serverAddress, final StreamListener streamListener) {
    this.connectionId = connectionId;
    this.group = group;
    this.channel = channel;
    this.serverAddress = serverAddress;
    this.streamListener = streamListener;
    this.stateMachine = new ClientStateMachine(this);
    this.packetBuffer = new PacketBuffer(this, this::sendPacketUnbuffered);
    this.streams = new Streams(this);
  }

  public Future<Void> handshake() {
    MDC.put("actor", "client");
    return stateMachine.handshake();
  }

  public void sendPacket(Packet p) {
    packetBuffer.send(p);
  }

  public void sendPacket(Frame... frames) {
    sendPacket(new ShortPacket(false,
                               false,
                               PacketType.Four_octets,
                               getConnectionId(),
                               nextSendPacketNumber(),
                               new Payload(frames)));
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

  public Optional<ConnectionId> getConnectionId() {
    return Optional.of(connectionId);
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
    StreamId streamId = StreamId.random();
    return streams.getOrCreate(streamId, streamListener);
  }

  public Stream getOrCreateStream(StreamId streamId) {
    return streams.getOrCreate(streamId, streamListener);
  }

  public Future<?> close() {
    return group.shutdownGracefully();
  }
}

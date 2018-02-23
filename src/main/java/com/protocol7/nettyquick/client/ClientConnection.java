package com.protocol7.nettyquick.client;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.protocol7.nettyquick.Connection;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.parser.PacketParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientConnection implements Connection {

  private final Logger log = LoggerFactory.getLogger(ClientConnection.class);

  private final ConnectionId connectionId;
  private final NioEventLoopGroup group;
  private final Channel channel;
  private final InetSocketAddress serverAddress;
  private final StreamListener streamListener;

  private final AtomicReference<Version> version = new AtomicReference<>(Version.DRAFT_09);
  private final AtomicReference<PacketNumber> lastPacketNumber = new AtomicReference<>(new PacketNumber(1)); // TODO fix
  private final PacketParser packetParser = new PacketParser();
  private final ClientStateMachine stateMachine;

  private final ClientStreams streams = new ClientStreams();

  public ClientConnection(final ConnectionId connectionId, final NioEventLoopGroup group, final Channel channel, final InetSocketAddress serverAddress, final StreamListener streamListener) {
    this.connectionId = connectionId;
    this.group = group;
    this.channel = channel;
    this.serverAddress = serverAddress;
    this.streamListener = streamListener;
    this.stateMachine = new ClientStateMachine(this);
  }

  public Future<Void> handshake() {
    return stateMachine.handshake();
  }

  public void sendPacket(Packet p) {
    ByteBuf bb = packetParser.serialize(p);
    channel.writeAndFlush(new DatagramPacket(bb, serverAddress)).syncUninterruptibly().awaitUninterruptibly(); // TODO fix
    System.out.println("c sent packet to " + serverAddress);
  }

  public void onPacket(Packet p) {
    stateMachine.processPacket(p);
  }

  public Optional<ConnectionId> getConnectionId() {
    return Optional.of(connectionId);
  }

  public Version getVersion() {
    return version.get();
  }

  public PacketNumber nextPacketNumber() {
    return lastPacketNumber.getAndUpdate(packetNumber -> packetNumber.next());
  }

  public ClientStream openStream() {
    StreamId streamId = StreamId.create();
    return streams.getOrCreate(streamId, this, streamListener);
  }

  public ClientStream getOrCreateStream(StreamId streamId) {
    return streams.getOrCreate(streamId, this, streamListener);
  }

  public Future<?> close() {
    return group.shutdownGracefully();
  }
}

package com.protocol7.nettyquick.client;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;
import com.protocol7.nettyquick.protocol.parser.PacketParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

public class ClientConnection {

  private final ConnectionId connectionId;
  private final Channel channel;
  private final InetSocketAddress serverAddress;
  private final StreamListener streamListener;

  private final AtomicReference<Version> version = new AtomicReference<>(Version.DRAFT_09);
  private final AtomicReference<PacketNumber> lastPacketNumber = new AtomicReference<>(new PacketNumber(1)); // TODO fix
  private final PacketParser packetParser = new PacketParser();

  private final ClientStreams streams = new ClientStreams();

  public ClientConnection(final ConnectionId connectionId, final Channel channel, final InetSocketAddress serverAddress, final StreamListener streamListener) {
    this.connectionId = connectionId;
    this.channel = channel;
    this.serverAddress = serverAddress;
    this.streamListener = streamListener;
  }

  public void sendPacket(Packet p) {
    ByteBuf bb = packetParser.serialize(p);
    channel.writeAndFlush(new DatagramPacket(bb, serverAddress)).syncUninterruptibly().awaitUninterruptibly(); // TODO fix
    System.out.println("c sent packet to " + serverAddress);
  }

  public void onPacket(Packet p) {
    for (Frame frame : p.getPayload().getFrames()) {
      if (frame instanceof StreamFrame) {
        StreamFrame sf = (StreamFrame) frame;

        ClientStream stream = streams.getOrCreate(sf.getStreamId(), this, streamListener); // TODO get stream ID from frame
        stream.onData(sf.getData());
      }
    }
  }

  public ConnectionId getConnectionId() {
    return connectionId;
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

  public void close() {
    channel.close().syncUninterruptibly().awaitUninterruptibly();
  }
}

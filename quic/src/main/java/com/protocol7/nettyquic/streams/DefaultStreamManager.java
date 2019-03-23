package com.protocol7.nettyquic.streams;

import com.protocol7.nettyquic.connection.FrameSender;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.frames.AckBlock;
import com.protocol7.nettyquic.protocol.frames.AckFrame;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.ResetStreamFrame;
import com.protocol7.nettyquic.protocol.frames.StreamFrame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;

public class DefaultStreamManager implements StreamManager {

  private final Streams streams;
  private final StreamListener listener;

  public DefaultStreamManager(final FrameSender frameSender, final StreamListener listener) {
    this.streams = new Streams(frameSender);
    this.listener = listener;
  }

  @Override
  public void beforeSendPacket(final Packet packet, final FrameSender sender) {}

  @Override
  public void onReceivePacket(final FullPacket packet, final FrameSender sender) {
    for (Frame frame : packet.getPayload().getFrames()) {
      if (frame instanceof StreamFrame) {
        final StreamFrame sf = (StreamFrame) frame;

        Stream stream = streams.getOrCreate(sf.getStreamId(), listener);
        stream.onData(sf.getOffset(), sf.isFin(), sf.getData());
      } else if (frame instanceof ResetStreamFrame) {
        final ResetStreamFrame rsf = (ResetStreamFrame) frame;
        final Stream stream = streams.getOrCreate(rsf.getStreamId(), listener);
        stream.onReset(rsf.getApplicationErrorCode(), rsf.getOffset());
      } else if (frame instanceof AckFrame) {
        AckFrame af = (AckFrame) frame;
        af.getBlocks().stream().forEach(this::handleAcks);
      }
    }
  }

  private void handleAcks(AckBlock block) {
    // TODO optimize
    long smallest = block.getSmallest().asLong();
    long largest = block.getLargest().asLong();
    for (long i = smallest; i <= largest; i++) {
      streams.onAck(new PacketNumber(i));
    }
  }

  @Override
  public Stream openStream(final boolean client, final boolean bidirectional) {
    return streams.openStream(client, bidirectional, listener);
  }
}

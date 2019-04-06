package com.protocol7.nettyquic.flowcontrol;

import com.google.common.annotations.VisibleForTesting;
import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.protocol.StreamId;
import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.DataBlockedFrame;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.FrameType;
import com.protocol7.nettyquic.protocol.frames.MaxDataFrame;
import com.protocol7.nettyquic.protocol.frames.MaxStreamDataFrame;
import com.protocol7.nettyquic.protocol.frames.StreamDataBlockedFrame;
import com.protocol7.nettyquic.protocol.frames.StreamFrame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultFlowControlHandler implements FlowControlHandler {

  private final FlowControlCounter receiveCounter;
  private final FlowControlCounter sendCounter;
  private final AtomicBoolean connectionBlocked = new AtomicBoolean(false);
  private final Set<StreamId> blockedStreams = new HashSet<>();

  public DefaultFlowControlHandler(final long connectionMaxBytes, final long streamMaxBytes) {
    receiveCounter = new FlowControlCounter(connectionMaxBytes, streamMaxBytes);
    sendCounter = new FlowControlCounter(connectionMaxBytes, streamMaxBytes);
  }

  @Override
  public void beforeSendPacket(final Packet packet, final PipelineContext ctx) {
    if (packet instanceof FullPacket) {
      FullPacket fullPacket = (FullPacket) packet;
      for (Frame frame : fullPacket.getPayload().getFrames()) {
        if (frame.getType() == FrameType.STREAM) {
          StreamFrame sf = (StreamFrame) frame;

          if (!tryConsume(sf.getStreamId(), sf.getOffset() + sf.getData().length, ctx)) {
            throw new IllegalStateException("Stream or connection blocked");
          }
        }
      }
    }

    ctx.next(packet);
  }

  @VisibleForTesting
  protected boolean tryConsume(final StreamId sid, final long offset, final PipelineContext ctx) {
    final TryConsumeResult result = sendCounter.tryConsume(sid, offset);

    if (result.isSuccess()) {
      return true;
    } else {
      final List<Frame> frames = new ArrayList<>();
      if (result.getConnectionOffset() > result.getConnectionMax() && !connectionBlocked.get()) {
        frames.add(new DataBlockedFrame(result.getConnectionMax()));
        connectionBlocked.set(true);
      }
      if (result.getStreamOffset() > result.getStreamMax() && !blockedStreams.contains(sid)) {
        frames.add(new StreamDataBlockedFrame(sid, result.getStreamMax()));
        blockedStreams.add(sid);
      }
      if (!frames.isEmpty()) {
        ctx.send(frames.toArray(new Frame[0]));
      }
      return false;
    }
  }

  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    if (packet instanceof FullPacket) {
      FullPacket fp = (FullPacket) packet;
      // listen for flow control frames
      for (final Frame frame : fp.getPayload().getFrames()) {
        if (frame.getType() == FrameType.MAX_STREAM_DATA) {
          final MaxStreamDataFrame msd = (MaxStreamDataFrame) frame;
          sendCounter.setStreamMaxBytes(msd.getStreamId(), msd.getMaxStreamData());
          blockedStreams.remove(msd.getStreamId());
        } else if (frame.getType() == FrameType.MAX_DATA) {
          final MaxDataFrame mdf = (MaxDataFrame) frame;
          sendCounter.setConnectionMaxBytes(mdf.getMaxData());
          connectionBlocked.set(false);
        } else if (frame.getType() == FrameType.STREAM) {
          final StreamFrame sf = (StreamFrame) frame;
          final StreamId sid = sf.getStreamId();
          final TryConsumeResult result =
              receiveCounter.tryConsume(sid, sf.getOffset() + sf.getData().length);

          if (result.isSuccess()) {
            final List<Frame> frames = new ArrayList<>();
            if (1.0 * result.getConnectionOffset() / result.getConnectionMax() > 0.5) {
              final long newMax = receiveCounter.increaseConnectionMax();
              frames.add(new MaxDataFrame(newMax));
            }
            if (1.0 * result.getStreamOffset() / result.getStreamMax() > 0.5) {
              final long newMax = receiveCounter.increaseStreamMax(sid);
              frames.add(new MaxStreamDataFrame(sid, newMax));
            }

            if (!frames.isEmpty()) {
              ctx.send(frames.toArray(new Frame[0]));
            }
          } else {
            ctx.closeConnection(
                TransportError.FLOW_CONTROL_ERROR, FrameType.STREAM, "Flow control error");
          }
        }
      }
    }

    ctx.next(packet);
  }
}

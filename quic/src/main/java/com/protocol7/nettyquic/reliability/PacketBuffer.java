package com.protocol7.nettyquic.reliability;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Queues;
import com.protocol7.nettyquic.FrameSender;
import com.protocol7.nettyquic.InboundHandler;
import com.protocol7.nettyquic.OutboundHandler;
import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.connection.State;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.frames.AckBlock;
import com.protocol7.nettyquic.protocol.frames.AckFrame;
import com.protocol7.nettyquic.protocol.packets.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO resends
public class PacketBuffer implements InboundHandler, OutboundHandler {

  private final Logger log = LoggerFactory.getLogger(PacketBuffer.class);

  private final Map<PacketNumber, Packet> buffer = new ConcurrentHashMap<>();
  private final BlockingQueue<PacketNumber> ackQueue = Queues.newArrayBlockingQueue(1000);
  private final AtomicReference<PacketNumber> largestAcked =
      new AtomicReference<>(PacketNumber.MIN);

  @VisibleForTesting
  protected Map<PacketNumber, Packet> getBuffer() {
    return buffer;
  }

  @Override
  public void beforeSendPacket(final Packet packet, final PipelineContext ctx) {
    if (packet instanceof FullPacket) {
      FullPacket fp = (FullPacket) packet;
      buffer.put(fp.getPacketNumber(), packet);
      log.debug("Buffered packet {}", ((FullPacket) packet).getPacketNumber());

      List<AckBlock> ackBlocks = drainAcks(ackQueue);
      if (!ackBlocks.isEmpty()) {
        // add to packet
        AckFrame ackFrame = new AckFrame(123, ackBlocks);
        ctx.next(fp.addFrame(ackFrame));
      } else {
        ctx.next(packet);
      }
    } else {
      ctx.next(packet);
    }
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    if (packet instanceof FullPacket && !(packet instanceof InitialPacket)) {
      if (ctx.getState() != State.Started) {
        ackQueue.add(((FullPacket) packet).getPacketNumber());
        log.debug("Acked packet {}", ((FullPacket) packet).getPacketNumber());

        handleAcks(packet);

        if (shouldFlush(packet)) {
          log.debug("Directly acking packet");
          flushAcks(ctx);
        }
      }
    }

    ctx.next(packet);
  }

  private boolean shouldFlush(Packet packet) {
    if (packet instanceof InitialPacket || packet instanceof HandshakePacket) {
      return false;
    } else if (packet instanceof FullPacket && acksOnly((FullPacket) packet)) {
      return false;
    } else {
      return true;
    }
  }

  private void handleAcks(Packet packet) {
    if (packet instanceof FullPacket) {
      ((FullPacket) packet)
          .getPayload()
          .getFrames()
          .stream()
          .filter(frame -> frame instanceof AckFrame)
          .forEach(frame -> handleAcks((AckFrame) frame));
    }
  }

  private void handleAcks(AckFrame frame) {
    frame.getBlocks().forEach(this::handleAcks);
  }

  private void handleAcks(AckBlock block) {
    // TODO optimize
    long smallest = block.getSmallest().asLong();
    long largest = block.getLargest().asLong();
    for (long i = smallest; i <= largest; i++) {
      PacketNumber pn = new PacketNumber(i);
      if (buffer.remove(pn) != null) {
        log.debug("Acked packet {}", pn);
        largestAcked.getAndAccumulate(pn, PacketNumber::max);
      }
    }
  }

  private void flushAcks(FrameSender sender) {
    List<AckBlock> blocks = drainAcks(ackQueue);
    if (!blocks.isEmpty()) {
      AckFrame ackFrame = new AckFrame(123, blocks);
      sender.send(ackFrame);

      log.debug("Flushed acks {}", blocks);
    }
  }

  // TODO break out and test directly
  private List<AckBlock> drainAcks(BlockingQueue<PacketNumber> queue) {
    List<PacketNumber> pns = new ArrayList<>();
    ackQueue.drainTo(pns);
    if (pns.isEmpty()) {
      return Collections.emptyList();
    }

    List<Long> pnsLong =
        pns.stream().map(packetNumber -> packetNumber.asLong()).collect(Collectors.toList());
    Collections.sort(pnsLong);

    List<AckBlock> blocks = new ArrayList<>();
    long lower = -1;
    long upper = -1;
    for (long pn : pnsLong) {
      if (lower == -1) {
        lower = pn;
        upper = pn;
      } else {
        if (pn > upper + 1) {
          blocks.add(AckBlock.fromLongs(lower, upper));
          lower = pn;
          upper = pn;
        } else {
          upper++;
        }
      }
    }
    blocks.add(AckBlock.fromLongs(lower, upper));

    return blocks;
  }

  private boolean acksOnly(FullPacket packet) {
    return packet.getPayload().getFrames().stream().allMatch(frame -> frame instanceof AckFrame);
  }
}

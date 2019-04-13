package com.protocol7.nettyquic.reliability;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

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
import com.protocol7.nettyquic.utils.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
  private final BlockingQueue<Pair<Long, Long>> ackQueue = Queues.newArrayBlockingQueue(1000);
  private final AtomicReference<PacketNumber> largestAcked =
      new AtomicReference<>(PacketNumber.MIN);
  private final AckDelay ackDelay;

  public PacketBuffer(final AckDelay ackDelay) {
    this.ackDelay = ackDelay;
  }

  @VisibleForTesting
  protected Map<PacketNumber, Packet> getBuffer() {
    return buffer;
  }

  @Override
  public void beforeSendPacket(final Packet packet, final PipelineContext ctx) {
    requireNonNull(packet);
    requireNonNull(ctx);

    if (packet instanceof FullPacket) {
      FullPacket fp = (FullPacket) packet;
      buffer.put(fp.getPacketNumber(), packet);

      final Pair<List<AckBlock>, Long> drained = drainAcks();
      final List<AckBlock> ackBlocks = drained.getFirst();
      if (!ackBlocks.isEmpty()) {
        // add to packet
        long delay = ackDelay.calculate(drained.getSecond(), NANOSECONDS);
        final AckFrame ackFrame = new AckFrame(delay, ackBlocks);
        fp = fp.addFrame(ackFrame);
      }

      ctx.next(fp);
    } else {
      ctx.next(packet);
    }
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    requireNonNull(packet);
    requireNonNull(ctx);

    if (packet instanceof FullPacket && !(packet instanceof InitialPacket)) {
      FullPacket fp = (FullPacket) packet;
      if (ctx.getState() != State.Started) {
        ackQueue.add(new Pair<>(fp.getPacketNumber().asLong(), ackDelay.time()));
        log.debug("Acked packet {}", fp.getPacketNumber());

        handleAcks(packet);

        if (shouldFlush(packet)) {
          log.debug("Directly acking packet");
          flushAcks(ctx);
        }
      }
    }

    ctx.next(packet);
  }

  private boolean shouldFlush(final Packet packet) {
    if (packet instanceof InitialPacket || packet instanceof HandshakePacket) {
      return false;
    } else if (packet instanceof FullPacket && acksOnly((FullPacket) packet)) {
      return false;
    } else {
      return true;
    }
  }

  private void handleAcks(final Packet packet) {
    if (packet instanceof FullPacket) {
      ((FullPacket) packet)
          .getPayload()
          .getFrames()
          .stream()
          .filter(frame -> frame instanceof AckFrame)
          .forEach(frame -> handleAcks((AckFrame) frame));
    }
  }

  private void handleAcks(final AckFrame frame) {
    frame.getBlocks().forEach(this::handleAcks);
  }

  private void handleAcks(final AckBlock block) {
    // TODO optimize
    final long smallest = block.getSmallest().asLong();
    final long largest = block.getLargest().asLong();
    for (long i = smallest; i <= largest; i++) {
      final PacketNumber pn = new PacketNumber(i);
      if (buffer.remove(pn) != null) {
        log.debug("Acked packet {}", pn);
        largestAcked.getAndAccumulate(pn, PacketNumber::max);
      }
    }
  }

  private void flushAcks(final FrameSender sender) {
    final Pair<List<AckBlock>, Long> drained = drainAcks();
    List<AckBlock> blocks = drained.getFirst();
    if (!blocks.isEmpty()) {
      long delay = ackDelay.calculate(drained.getSecond(), NANOSECONDS);
      final AckFrame ackFrame = new AckFrame(delay, blocks);
      sender.send(ackFrame);

      log.debug("Flushed acks {}", blocks);
    }
  }

  // TODO break out and test directly
  private Pair<List<AckBlock>, Long> drainAcks() {
    final List<Pair<Long, Long>> pns = new ArrayList<>();
    ackQueue.drainTo(pns);
    if (pns.isEmpty()) {
      return new Pair<>(Collections.emptyList(), 0L);
    }

    long largestPnQueueTime =
        pns.stream().max(Comparator.comparingLong(Pair::getFirst)).get().getSecond();

    final List<Long> pnsLong =
        pns.stream().map(pair -> pair.getFirst()).collect(Collectors.toList());
    Collections.sort(pnsLong);

    final List<AckBlock> blocks = new ArrayList<>();
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

    return new Pair<>(blocks, ackDelay.delay(largestPnQueueTime));
  }

  private static boolean acksOnly(final FullPacket packet) {
    return packet.getPayload().getFrames().stream().allMatch(frame -> frame instanceof AckFrame);
  }
}

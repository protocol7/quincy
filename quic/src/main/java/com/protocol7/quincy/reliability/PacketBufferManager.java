package com.protocol7.quincy.reliability;

import static com.protocol7.quincy.protocol.packets.Packet.getEncryptionLevel;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.VisibleForTesting;
import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.OutboundHandler;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.protocol.frames.AckFrame;
import com.protocol7.quincy.protocol.frames.AckRange;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.packets.*;
import com.protocol7.quincy.reliability.AckQueue.Entry;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.utils.Pair;
import com.protocol7.quincy.utils.Ticker;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketBufferManager implements InboundHandler, OutboundHandler {

  private static final long RESEND_DELAY = 10;

  private final Logger log = LoggerFactory.getLogger(PacketBufferManager.class);

  private final PacketBuffer initialBuffer;
  private final PacketBuffer handshakeBuffer;
  private final PacketBuffer buffer;
  private final AckQueue ackQueue = new AckQueue();
  private final AtomicReference<Long> largestAcked = new AtomicReference<>(0L);
  private final AckDelay ackDelay;
  private final FrameSender frameSender;

  public PacketBufferManager(
      final AckDelay ackDelay,
      final FrameSender frameSender,
      final Timer timer,
      final Ticker ticker) {
    this.ackDelay = requireNonNull(ackDelay);
    this.frameSender = frameSender;

    initialBuffer = new PacketBuffer(ticker);
    handshakeBuffer = new PacketBuffer(ticker);
    buffer = new PacketBuffer(ticker);

    final TimerTask task =
        new TimerTask() {
          @Override
          public void run(final Timeout timeout) {
            resend();
            timeout.timer().newTimeout(this, RESEND_DELAY, MILLISECONDS);
          }
        };

    timer.newTimeout(task, RESEND_DELAY, MILLISECONDS);

    // TODO cancel timeout when connection closes
  }

  public void resend() {
    final Collection<Frame> toResend = buffer.drainSince(1000, MILLISECONDS);
    toResend.stream().forEach(f -> frameSender.send(EncryptionLevel.OneRtt, f));
  }

  @Override
  public void beforeSendPacket(final Packet packet, final PipelineContext ctx) {
    requireNonNull(packet);
    requireNonNull(ctx);

    if (packet instanceof FullPacket) {
      FullPacket fp = (FullPacket) packet;
      buffer(fp);

      final Pair<List<AckRange>, Long> drained = drainAcks(getEncryptionLevel(fp));
      final List<AckRange> ackBlocks = drained.getFirst();
      if (!ackBlocks.isEmpty()) {
        // add to packet
        final long delay = ackDelay.calculate(drained.getSecond(), NANOSECONDS);
        final AckFrame ackFrame = new AckFrame(delay, ackBlocks);
        fp = fp.addFrame(ackFrame);
      }

      ctx.next(fp);
    } else {
      ctx.next(packet);
    }
  }

  private void buffer(final FullPacket packet) {
    final EncryptionLevel level = getEncryptionLevel(packet);
    if (level == EncryptionLevel.Initial) {
      initialBuffer.put(packet);
    } else if (level == EncryptionLevel.Handshake) {
      handshakeBuffer.put(packet);
    } else {
      buffer.put(packet);
    }
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    requireNonNull(packet);
    requireNonNull(ctx);

    if (packet instanceof FullPacket) {
      if (packet instanceof HandshakePacket) {
        // implicitly ack all initial packets
        initialBuffer.clear();
      } else if (packet instanceof ShortPacket) {
        // implicitly ack all handshake packets
        handshakeBuffer.clear();
      }

      final FullPacket fp = (FullPacket) packet;
      ackQueue.add(fp, ackDelay.time());
      log.debug("Packet queued for acking {}", fp.getPacketNumber());

      handleAcks(packet);

      if (shouldFlush(packet)) {
        log.debug("Directly acking packet");
        final EncryptionLevel level = getEncryptionLevel(packet);
        flushAcks(level, ctx);
      }
    }

    ctx.next(packet);
  }

  private boolean shouldFlush(final Packet packet) {
    // if (packet instanceof InitialPacket || packet instanceof HandshakePacket) {
    //  return true;
    // } else
    if (packet instanceof FullPacket && acksOnly((FullPacket) packet)) {
      return false;
    } else {
      return true;
    }
  }

  private void handleAcks(final Packet packet) {
    if (packet instanceof FullPacket) {
      final EncryptionLevel level = getEncryptionLevel(packet);

      ((FullPacket) packet)
          .getPayload()
          .getFrames()
          .stream()
          .filter(frame -> frame instanceof AckFrame)
          .forEach(frame -> handleAcks((AckFrame) frame, level));
    }
  }

  private void handleAcks(final AckFrame frame, final EncryptionLevel level) {
    frame.getRanges().forEach(b -> handleAcks(b, level));
  }

  private void handleAcks(final AckRange range, final EncryptionLevel level) {
    // TODO optimize
    final long smallest = range.getSmallest();
    final long largest = range.getLargest();
    for (long pn = smallest; pn <= largest; pn++) {
      if (ack(pn, level)) {
        log.debug("Acked packet {} at level {}", pn, level);
        largestAcked.getAndAccumulate(pn, Math::max);
      }
    }
  }

  private boolean ack(final long pn, final EncryptionLevel level) {
    if (level == EncryptionLevel.Initial) {
      return initialBuffer.remove(pn);
    } else if (level == EncryptionLevel.Handshake) {
      return handshakeBuffer.remove(pn);
    } else {
      return buffer.remove(pn);
    }
  }

  private void flushAcks(final EncryptionLevel level, final FrameSender sender) {
    System.out.println("Flushing acks at " + level);

    final Pair<List<AckRange>, Long> drained = drainAcks(level);
    final List<AckRange> ranges = drained.getFirst();
    if (!ranges.isEmpty()) {
      final long delay = ackDelay.calculate(drained.getSecond(), NANOSECONDS);
      final AckFrame ackFrame = new AckFrame(delay, ranges);
      sender.send(level, ackFrame);

      log.debug("Flushed acks {}", ranges);
    }
  }

  // TODO break out and test directly
  private Pair<List<AckRange>, Long> drainAcks(final EncryptionLevel level) {
    final Collection<Entry> pns = ackQueue.drain(level);
    if (pns.isEmpty()) {
      return Pair.of(Collections.emptyList(), 0L);
    }

    final long largestPnQueueTime =
        pns.stream().max(Comparator.comparingLong(Entry::getPacketNumber)).get().getTimestamp();

    final List<Long> pnsLong =
        pns.stream().map(Entry::getPacketNumber).sorted().collect(Collectors.toList());

    final List<AckRange> ranges = new ArrayList<>();
    long lower = -1;
    long upper = -1;
    for (final long pn : pnsLong) {
      if (lower == -1) {
        lower = pn;
        upper = pn;
      } else {
        if (pn > upper + 1) {
          ranges.add(new AckRange(lower, upper));
          lower = pn;
          upper = pn;
        } else {
          upper++;
        }
      }
    }
    ranges.add(new AckRange(lower, upper));

    return Pair.of(ranges, ackDelay.delay(largestPnQueueTime));
  }

  private static boolean acksOnly(final FullPacket packet) {
    return packet.getPayload().getFrames().stream().allMatch(frame -> frame instanceof AckFrame);
  }

  @VisibleForTesting
  protected PacketBuffer getBuffer() {
    return buffer;
  }

  @VisibleForTesting
  protected PacketBuffer getInitialBuffer() {
    return initialBuffer;
  }

  @VisibleForTesting
  protected PacketBuffer getHandshakeBuffer() {
    return handshakeBuffer;
  }
}

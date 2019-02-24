package com.protocol7.nettyquic.protocol;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Queues;
import com.protocol7.nettyquic.connection.Connection;
import com.protocol7.nettyquic.connection.Sender;
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
public class PacketBuffer {

  public interface AckListener {
    void onAck(PacketNumber pn);
  }

  private final Logger log = LoggerFactory.getLogger(PacketBuffer.class);

  private final Map<PacketNumber, Packet> buffer = new ConcurrentHashMap<>();
  private final BlockingQueue<PacketNumber> ackQueue = Queues.newArrayBlockingQueue(1000);
  private final AtomicReference<PacketNumber> largestAcked =
      new AtomicReference<>(PacketNumber.MIN);
  private final Connection connection;
  private final Sender sender;
  private final AckListener ackListener;

  public PacketBuffer(
      final Connection connection, final Sender sender, final AckListener ackListener) {
    this.connection = connection;
    this.sender = sender;
    this.ackListener = ackListener;
  }

  @VisibleForTesting
  protected Map<PacketNumber, Packet> getBuffer() {
    return buffer;
  }

  public PacketNumber getLargestAcked() {
    return largestAcked.get();
  }

  public void send(Packet packet) {
    if (packet instanceof FullPacket) {
      List<AckBlock> ackBlocks = drainAcks(ackQueue);
      if (!ackBlocks.isEmpty()) {
        // add to packet
        AckFrame ackFrame = new AckFrame(123, ackBlocks);
        sendImpl(((FullPacket) packet).addFrame(ackFrame));
      } else {
        sendImpl(packet);
      }
    } else {
      sendImpl(packet);
    }
  }

  private void sendImpl(Packet packet) {
    if (packet instanceof FullPacket) {
      buffer.put(((FullPacket) packet).getPacketNumber(), packet);
      log.debug("Buffered packet {}", ((FullPacket) packet).getPacketNumber());
    }
    sender.send(packet);
  }

  public void onPacket(Packet packet) {
    if (packet instanceof FullPacket && !(packet instanceof InitialPacket)) {
      ackQueue.add(((FullPacket) packet).getPacketNumber());
      log.debug("Acked packet {}", ((FullPacket) packet).getPacketNumber());

      handleAcks(packet);

      if (shouldFlush(packet)) {
        log.debug("Directly acking packet");
        flushAcks();
      }
    }
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
        ackListener.onAck(pn);
        largestAcked.getAndAccumulate(pn, PacketNumber::max);
      }
    }
  }

  private void flushAcks() {
    List<AckBlock> blocks = drainAcks(ackQueue);
    if (!blocks.isEmpty()) {
      AckFrame ackFrame = new AckFrame(123, blocks);
      Packet packet =
          new ShortPacket(
              false,
              connection.getRemoteConnectionId(),
              connection.nextSendPacketNumber(),
              new Payload(ackFrame));

      log.debug("Flushed acks {}", blocks);

      sendImpl(packet);
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

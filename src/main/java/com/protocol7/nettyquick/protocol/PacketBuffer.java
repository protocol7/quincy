package com.protocol7.nettyquick.protocol;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.protocol7.nettyquick.Connection;
import com.protocol7.nettyquick.protocol.frames.AckBlock;
import com.protocol7.nettyquick.protocol.frames.AckFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO resends
public class PacketBuffer {

  private final Logger log = LoggerFactory.getLogger(PacketBuffer.class);

  private final Map<PacketNumber, Packet> buffer = Maps.newConcurrentMap();
  private final BlockingQueue<PacketNumber> ackQueue = Queues.newArrayBlockingQueue(1000);
  private final Connection connection;
  private final Sender sender;

  public interface Sender {
    void send(Packet packet);
  }

  public PacketBuffer(final Connection connection, final Sender sender) {
    this.connection = connection;
    this.sender = sender;
  }

  @VisibleForTesting
  protected Map<PacketNumber, Packet> getBuffer() {
    return buffer;
  }

  public void send(Packet packet) {
    List<AckBlock> ackBlocks = drainAcks(ackQueue);
    if (!ackBlocks.isEmpty()) {
      // add to packet
      AckFrame ackFrame = new AckFrame(123, ackBlocks);
      sendImpl(Packet.addFrame(packet, ackFrame));
    } else {
      sendImpl(packet);
    }
  }

  private void sendImpl(Packet packet) {
    buffer.put(packet.getPacketNumber(), packet);
    log.debug("Buffered packet {}", packet.getPacketNumber());
    sender.send(packet);
  }

  public void onPacket(Packet packet) {
    ackQueue.add(packet.getPacketNumber());
    log.debug("Acked packet {}", packet.getPacketNumber());

    handleAcks(packet);

    if (shouldFlush(packet)) {
      log.debug("Directly acking packet");
      flushAcks();
    }
  }

  private boolean shouldFlush(Packet packet) {
    if (packet.getPacketType() == PacketType.Initial) {
      return false;
    } else if (acksOnly(packet)) {
      return false;
    } else {
      return true;
    }
  }

  private void handleAcks(Packet packet) {
    packet.getPayload().getFrames().stream().filter(frame -> frame instanceof AckFrame).forEach(frame -> handleAcks((AckFrame) frame));
  }

  private void handleAcks(AckFrame frame) {
    frame.getBlocks().forEach(this::handleAcks);
  }

  private void handleAcks(AckBlock block) {
    // TODO optimize
    long smallest = block.getSmallest().asLong();
    long largest = block.getLargest().asLong();
    for (long i = smallest; i<=largest; i++) {
      PacketNumber pn = new PacketNumber(i);
      if (buffer.remove(pn) != null) {
        log.debug("Acked packet {}", pn);
      }
    }
  }

  private void flushAcks() {
    List<AckBlock> blocks = drainAcks(ackQueue);
    if (!blocks.isEmpty()) {
      AckFrame ackFrame = new AckFrame(123, blocks);
      Packet packet = new ShortPacket(false,
                                      false,
                                      PacketType.Four_octets,
                                      connection.getConnectionId(),
                                      connection.nextPacketNumber(),
                                      new Payload(ackFrame));

      log.debug("Flushed acks {}", blocks);

      sendImpl(packet);
    }
  }

  // TODO break out and test directly
  private List<AckBlock> drainAcks(BlockingQueue<PacketNumber> queue) {
    List<PacketNumber> pns = Lists.newArrayList();
    ackQueue.drainTo(pns);
    if (pns.isEmpty()) {
      return Collections.emptyList();
    }

    List<Long> pnsLong = pns.stream().map(packetNumber -> packetNumber.asLong()).collect(Collectors.toList());
    Collections.sort(pnsLong);

    List<AckBlock> blocks = Lists.newArrayList();
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

  private boolean acksOnly(Packet packet) {
    return packet.getPayload().getFrames().stream().allMatch(frame -> frame instanceof AckFrame);
  }

}

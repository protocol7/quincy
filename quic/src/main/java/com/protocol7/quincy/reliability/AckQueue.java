package com.protocol7.quincy.reliability;

import static com.protocol7.quincy.protocol.packets.Packet.getEncryptionLevel;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.Queues;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.tls.EncryptionLevel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

public class AckQueue {

  public static class Entry {
    private final long packetNumber;
    private final long timestamp;

    public Entry(final long packetNumber, final long timestamp) {
      this.packetNumber = packetNumber;
      this.timestamp = timestamp;
    }

    public long getPacketNumber() {
      return packetNumber;
    }

    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Entry entry = (Entry) o;
      return packetNumber == entry.packetNumber && timestamp == entry.timestamp;
    }

    @Override
    public int hashCode() {
      return Objects.hash(packetNumber, timestamp);
    }
  }

  private final BlockingQueue<Entry> initialAckQueue = Queues.newArrayBlockingQueue(1000);
  private final BlockingQueue<Entry> handshakeAckQueue = Queues.newArrayBlockingQueue(1000);
  private final BlockingQueue<Entry> ackQueue = Queues.newArrayBlockingQueue(1000);

  public void add(final FullPacket packet, final long time) {
    requireNonNull(packet);
    final EncryptionLevel level = getEncryptionLevel(packet);
    getQueue(level).add(new Entry(packet.getPacketNumber().asLong(), time));
  }

  public Collection<Entry> drain(final EncryptionLevel level) {
    final BlockingQueue<Entry> queue = getQueue(level);

    final List<Entry> pns = new ArrayList<>(queue.size());
    queue.drainTo(pns);
    return pns;
  }

  private BlockingQueue<Entry> getQueue(final EncryptionLevel level) {
    if (level == EncryptionLevel.Initial) {
      return initialAckQueue;
    } else if (level == EncryptionLevel.Handshake) {
      return handshakeAckQueue;
    } else {
      return ackQueue;
    }
  }
}

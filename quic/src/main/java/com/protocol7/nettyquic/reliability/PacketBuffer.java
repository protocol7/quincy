package com.protocol7.nettyquic.reliability;

import static com.protocol7.nettyquic.utils.Pair.of;
import static java.util.Objects.requireNonNull;

import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.utils.Pair;
import com.protocol7.nettyquic.utils.Ticker;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PacketBuffer {

  private final ConcurrentMap<PacketNumber, Pair<List<Frame>, Long>> buffer =
      new ConcurrentHashMap<>();
  private final Ticker ticker;

  public PacketBuffer(final Ticker ticker) {
    this.ticker = requireNonNull(ticker);
  }

  public void put(final FullPacket packet) {
    requireNonNull(packet);
    buffer.put(packet.getPacketNumber(), of(packet.getPayload().getFrames(), ticker.nanoTime()));
  }

  public boolean remove(final PacketNumber packetNumber) {
    requireNonNull(packetNumber);
    return buffer.remove(packetNumber) != null;
  }

  public boolean contains(final PacketNumber packetNumber) {
    requireNonNull(packetNumber);
    return buffer.containsKey(packetNumber);
  }

  public boolean isEmpty() {
    return buffer.isEmpty();
  }

  public Collection<Frame> drainSince(final long ttl, final TimeUnit unit) {
    final long since = ticker.nanoTime() - unit.toNanos(ttl);

    final List<Entry<PacketNumber, Pair<List<Frame>, Long>>> toDrain =
        buffer
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getSecond() < since)
            .collect(Collectors.toUnmodifiableList());

    toDrain.forEach(entry -> remove(entry.getKey()));

    return toDrain
        .stream()
        .flatMap(entry -> entry.getValue().getFirst().stream())
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public String toString() {
    return "PacketBuffer{" + buffer + '}';
  }
}

package com.protocol7.nettyquic.reliability;

import com.google.common.collect.Queues;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.utils.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class AckQueue {

  private final BlockingQueue<Pair<Long, Long>> ackQueue = Queues.newArrayBlockingQueue(1000);

  public void add(final FullPacket packet, long time) {
    ackQueue.add(new Pair<>(packet.getPacketNumber().asLong(), time));
  }

  public Collection<Pair<Long, Long>> drain() {
    final List<Pair<Long, Long>> pns = new ArrayList<>();
    ackQueue.drainTo(pns);
    return pns;
  }
}

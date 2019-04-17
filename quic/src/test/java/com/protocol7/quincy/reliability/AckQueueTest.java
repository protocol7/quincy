package com.protocol7.quincy.reliability;

import static com.protocol7.quincy.tls.EncryptionLevel.OneRtt;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.reliability.AckQueue.Entry;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AckQueueTest {

  @Mock private ShortPacket packet1;
  @Mock private ShortPacket packet2;
  private PacketNumber pn1 = new PacketNumber(1);
  private PacketNumber pn2 = new PacketNumber(2);

  private AckQueue queue = new AckQueue();

  @Before
  public void setUp() {
    when(packet1.getPacketNumber()).thenReturn(pn1);
    when(packet2.getPacketNumber()).thenReturn(pn2);
  }

  @Test
  public void test() {
    assertTrue(queue.drain(OneRtt).isEmpty());

    queue.add(packet1, 123);
    queue.add(packet2, 456);

    assertEquals(List.of(new Entry(1L, 123L), new Entry(2L, 456L)), queue.drain(OneRtt));

    assertTrue(queue.drain(OneRtt).isEmpty());
  }
}

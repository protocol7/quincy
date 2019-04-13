package com.protocol7.nettyquic.reliability;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PacketBufferTest {

  @Mock private FullPacket packet;
  private PacketNumber pn = PacketNumber.MIN;

  private PacketBuffer buffer = new PacketBuffer();

  @Before
  public void setUp() {
    when(packet.getPacketNumber()).thenReturn(pn);
  }

  @Test
  public void test() {
    assertTrue(buffer.isEmpty());
    assertFalse(buffer.contains(pn));

    buffer.put(packet);

    assertFalse(buffer.isEmpty());
    assertTrue(buffer.contains(pn));

    assertTrue(buffer.remove(pn));

    assertTrue(buffer.isEmpty());
    assertFalse(buffer.contains(pn));
  }

  @Test
  public void remove() {
    assertFalse(buffer.remove(pn));
    buffer.put(packet);
    assertTrue(buffer.remove(pn));
  }
}

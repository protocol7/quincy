package com.protocol7.quincy.tls.extensions;

import static com.protocol7.quincy.utils.Hex.dehex;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class TransportParametersTest {

  @Test
  public void roundtrip() {
    final TransportParameters tps =
        TransportParameters.newBuilder()
            .withAckDelayExponent(130)
            .withDisableMigration(true)
            .withIdleTimeout(234)
            .withInitialMaxBidiStreams(345)
            .withInitialMaxData(456)
            .withInitialMaxStreamDataBidiLocal(567)
            .withInitialMaxStreamDataBidiRemote(678)
            .withInitialMaxStreamDataUni(789)
            .withInitialMaxUniStreams(890)
            .withMaxAckDelay(129)
            .withMaxPacketSize(432)
            .withStatelessResetToken("srt".getBytes())
            .withOriginalConnectionId("oci".getBytes())
            .build();

    final ByteBuf bb = Unpooled.buffer();

    tps.write(bb, true);

    final TransportParameters parsed = TransportParameters.parse(bb);

    assertEquals(tps.getAckDelayExponent(), parsed.getAckDelayExponent());
    assertEquals(tps.isDisableMigration(), parsed.isDisableMigration());
    assertEquals(tps.getIdleTimeout(), parsed.getIdleTimeout());
    assertEquals(tps.getInitialMaxBidiStreams(), parsed.getInitialMaxBidiStreams());
    assertEquals(tps.getInitialMaxData(), parsed.getInitialMaxData());
    assertEquals(tps.getInitialMaxStreamDataBidiLocal(), parsed.getInitialMaxStreamDataBidiLocal());
    assertEquals(
        tps.getInitialMaxStreamDataBidiRemote(), parsed.getInitialMaxStreamDataBidiRemote());
    assertEquals(tps.getInitialMaxStreamDataUni(), parsed.getInitialMaxStreamDataUni());
    assertEquals(tps.getInitialMaxUniStreams(), parsed.getInitialMaxUniStreams());
    assertEquals(tps.getMaxAckDelay(), parsed.getMaxAckDelay());
    assertEquals(tps.getMaxPacketSize(), parsed.getMaxPacketSize());
    assertArrayEquals(tps.getStatelessResetToken(), parsed.getStatelessResetToken());
    assertArrayEquals(tps.getOriginalConnectionId(), parsed.getOriginalConnectionId());
  }

  @Test
  public void parseKnown() {
    final byte[] data =
        dehex(
            "006400050004800800000006000480080000000700048008000000040004800c0000000800024064000900024064000100011e0003000245ac000c0000000200102a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a00000011ad9380222fa9c6e4b90085703882f5f4bd");
    final ByteBuf bb = Unpooled.wrappedBuffer(data);

    final TransportParameters parsed = TransportParameters.parse(bb);

    assertEquals(-1, parsed.getAckDelayExponent());
    assertEquals(true, parsed.isDisableMigration());
    assertEquals(30, parsed.getIdleTimeout());
    assertEquals(100, parsed.getInitialMaxBidiStreams());
    assertEquals(786432, parsed.getInitialMaxData());
    assertEquals(524288, parsed.getInitialMaxStreamDataBidiLocal());
    assertEquals(524288, parsed.getInitialMaxStreamDataBidiRemote());
    assertEquals(524288, parsed.getInitialMaxStreamDataUni());
    assertEquals(100, parsed.getInitialMaxUniStreams());
    assertEquals(-1, parsed.getMaxAckDelay());
    assertEquals(1452, parsed.getMaxPacketSize());
    assertEquals(16, parsed.getStatelessResetToken().length);
    assertEquals(17, parsed.getOriginalConnectionId().length);
  }
}

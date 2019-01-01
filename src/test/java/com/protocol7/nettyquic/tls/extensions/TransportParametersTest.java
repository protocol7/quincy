package com.protocol7.nettyquic.tls.extensions;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class TransportParametersTest {

  @Test
  public void roundtrip() {
    TransportParameters tps =
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

    ByteBuf bb = Unpooled.buffer();

    tps.write(bb, true);

    TransportParameters parsed = TransportParameters.parse(bb);

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
    byte[] data =
        Hex.dehex(
            "0000006508caaa4afa00000065004f000500048000800000060004800080000007000480008000000400048000c000000800024064000900024064000100011e0003000245ac000c0000000200102a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a");
    ByteBuf bb = Unpooled.wrappedBuffer(data);

    TransportParameters parsed = TransportParameters.parse(bb);

    assertEquals(-1, parsed.getAckDelayExponent());
    assertEquals(true, parsed.isDisableMigration());
    assertEquals(30, parsed.getIdleTimeout());
    assertEquals(100, parsed.getInitialMaxBidiStreams());
    assertEquals(49152, parsed.getInitialMaxData());
    assertEquals(32768, parsed.getInitialMaxStreamDataBidiLocal());
    assertEquals(32768, parsed.getInitialMaxStreamDataBidiRemote());
    assertEquals(32768, parsed.getInitialMaxStreamDataUni());
    assertEquals(100, parsed.getInitialMaxUniStreams());
    assertEquals(-1, parsed.getMaxAckDelay());
    assertEquals(1452, parsed.getMaxPacketSize());
    assertEquals(16, parsed.getStatelessResetToken().length);
    assertEquals(0, parsed.getOriginalConnectionId().length);
  }
}

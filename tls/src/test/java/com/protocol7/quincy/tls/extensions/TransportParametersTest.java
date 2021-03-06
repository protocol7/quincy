package com.protocol7.quincy.tls.extensions;

import static com.protocol7.quincy.utils.Hex.dehex;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class TransportParametersTest {

  @Test
  public void roundtrip() {
    final TransportParameters tps =
        TransportParameters.newBuilder()
            .withAckDelayExponent(130)
            .withDisableActiveMigration(true)
            .withMaxIdleTimeout(234)
            .withInitialMaxStreamsBidi(345)
            .withInitialMaxData(456)
            .withInitialMaxStreamDataBidiLocal(567)
            .withInitialMaxStreamDataBidiRemote(678)
            .withInitialMaxStreamDataUni(789)
            .withInitialMaxStreamsUni(890)
            .withMaxAckDelay(129)
            .withMaxUDPPacketSize(432)
            .withStatelessResetToken("srt".getBytes())
            .withOriginalDestinationConnectionId("oci".getBytes())
            .build();

    final ByteBuf bb = Unpooled.buffer();

    tps.write(bb, true);

    final TransportParameters parsed = TransportParameters.parse(bb);

    assertEquals(tps.getAckDelayExponent(), parsed.getAckDelayExponent());
    assertEquals(tps.isDisableActiveMigration(), parsed.isDisableActiveMigration());
    assertEquals(tps.getIdleTimeout(), parsed.getIdleTimeout());
    assertEquals(tps.getInitialMaxStreamsBidi(), parsed.getInitialMaxStreamsBidi());
    assertEquals(tps.getInitialMaxData(), parsed.getInitialMaxData());
    assertEquals(tps.getInitialMaxStreamDataBidiLocal(), parsed.getInitialMaxStreamDataBidiLocal());
    assertEquals(
        tps.getInitialMaxStreamDataBidiRemote(), parsed.getInitialMaxStreamDataBidiRemote());
    assertEquals(tps.getInitialMaxStreamDataUni(), parsed.getInitialMaxStreamDataUni());
    assertEquals(tps.getInitialMaxStreamsUni(), parsed.getInitialMaxStreamsUni());
    assertEquals(tps.getMaxAckDelay(), parsed.getMaxAckDelay());
    assertEquals(tps.getMaxUDPPacketSize(), parsed.getMaxUDPPacketSize());
    assertArrayEquals(tps.getStatelessResetToken(), parsed.getStatelessResetToken());
    assertArrayEquals(
        tps.getOriginalDestinationConnectionId(), parsed.getOriginalDestinationConnectionId());
  }

  @Test
  public void parseKnown() {
    final byte[] data =
        dehex(
            "010480007530030245460404809896800504800f42400604800f42400704800f424008024064090240640a01030b01190c000f147fc05a41792c01e47504c96118383516818f8f8e");
    final ByteBuf bb = Unpooled.wrappedBuffer(data);

    final TransportParameters parsed = TransportParameters.parse(bb);

    assertEquals(3, parsed.getAckDelayExponent());
    assertEquals(true, parsed.isDisableActiveMigration());
    assertEquals(30000, parsed.getIdleTimeout());
    assertEquals(100, parsed.getInitialMaxStreamsBidi());
    assertEquals(100, parsed.getInitialMaxStreamsUni());
    assertEquals(10000000, parsed.getInitialMaxData());
    assertEquals(1000000, parsed.getInitialMaxStreamDataBidiLocal());
    assertEquals(1000000, parsed.getInitialMaxStreamDataBidiRemote());
    assertEquals(1000000, parsed.getInitialMaxStreamDataUni());
    assertEquals(100, parsed.getInitialMaxStreamsUni());
    assertEquals(25, parsed.getMaxAckDelay());
    assertEquals(1350, parsed.getMaxUDPPacketSize());
    assertEquals(-1, parsed.getActiveConnectionIdLimit());
    assertEquals(0, parsed.getStatelessResetToken().length);
    assertEquals(0, parsed.getOriginalDestinationConnectionId().length);
    assertEquals(20, parsed.getInitialSourceConnectionId().length);
    assertEquals(0, parsed.getRetrySourceConnectionId().length);
  }

  @Test
  public void writeDisableActiveMigration() {
    final ByteBuf bb = Unpooled.buffer();
    TransportParameters.newBuilder().withDisableActiveMigration(true).build().write(bb, true);

    // disable active migration must be written as an empty value
    assertEquals("0c00", Hex.hex(Bytes.drainToArray(bb)));
  }
}

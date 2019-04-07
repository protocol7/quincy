package com.protocol7.nettyquic.tls;

import static com.protocol7.nettyquic.utils.Hex.hex;
import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.tls.extensions.TransportParameters;
import com.protocol7.nettyquic.utils.Bytes;
import io.netty.buffer.ByteBuf;

public class TestUtil {

  public static void assertHex(String expectedHex, byte[] actual) {
    assertEquals(expectedHex, hex(actual));
  }

  public static void assertHex(String expectedHex, ByteBuf actual) {
    byte[] actualBytes = Bytes.peekToArray(actual);
    assertHex(expectedHex, actualBytes);
  }

  public static void assertHex(byte[] expected, byte[] actual) {
    assertEquals(hex(expected), hex(actual));
  }

  public static TransportParameters tps(byte[] version) {
    return TransportParameters.newBuilder(version)
        .withInitialMaxStreamDataBidiLocal(32768)
        .withInitialMaxData(49152)
        .withInitialMaxBidiStreams(100)
        .withIdleTimeout(30)
        .withMaxPacketSize(1452)
        .withInitialMaxUniStreams(100)
        .withDisableMigration(true)
        .withInitialMaxStreamDataBidiRemote(32768)
        .withInitialMaxStreamDataUni(32768)
        .build();
  }
}

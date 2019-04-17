package com.protocol7.testcontainers.quicly;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.utils.Hex;
import java.util.List;
import org.junit.Test;

public class PacketParserTest {

  @Test
  public void parse() {
    List<QuiclyPacket> packets =
        PacketParser.parse(
            List.of(
                "recvmsg (1252 bytes):\n",
                "     c7 ff 00 00 12 98 c1 e5 2e b8 22 96 d7 b4 d4 cc\n",
                "     ec 03 b4 ef 52 48 3d ad 6d a0 63 15 bd\n",
                "sendmsg (1280 bytes):\n",
                "     c0 ff 00 00 12 85 b4 ef 52 48 3d ad 6d a0 63 15\n",
                "     bd ce 40 68 dd b5 75 4f b0 00 40 75 7b 69 05 4c\n"));

    assertEquals(2, packets.size());

    assertPacket(
        true,
        Hex.dehex(
            "c7 ff 00 00 12 98 c1 e5 2e b8 22 96 d7 b4 d4 cc ec 03 b4 ef 52 48 3d ad 6d a0 63 15 bd"),
        packets.get(0));
    assertPacket(
        false,
        Hex.dehex(
            "c0 ff 00 00 12 85 b4 ef 52 48 3d ad 6d a0 63 15 bd ce 40 68 dd b5 75 4f b0 00 40 75 7b 69 05 4c"),
        packets.get(1));
  }

  private void assertPacket(boolean inbound, byte[] bytes, QuiclyPacket actual) {
    assertEquals(inbound, actual.isInbound());
    assertArrayEquals(bytes, actual.getBytes());
  }
}

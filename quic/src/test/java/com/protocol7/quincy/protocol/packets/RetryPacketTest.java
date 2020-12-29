package com.protocol7.quincy.protocol.packets;

import static java.util.Optional.of;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.utils.Hex;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class RetryPacketTest {

  private ConnectionId dest = ConnectionId.random();
  private ConnectionId src = ConnectionId.random();
  private ConnectionId org = ConnectionId.random();
  private byte[] token = Rnd.rndBytes(18);
  private RetryPacket packet =
      new RetryPacket(Version.DRAFT_29, of(dest), of(src), of(org), token, null);

  private final AEAD aead = InitialAEAD.create(ConnectionId.random().asBytes(), true);

  @Test
  public void roundtrip() {
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    final RetryPacket parsed = RetryPacket.parse(bb).complete(l -> aead);

    assertEquals(Version.DRAFT_29, parsed.getVersion());
    assertEquals(dest, parsed.getDestinationConnectionId().get());
    assertEquals(src, parsed.getSourceConnectionId().get());
    assertArrayEquals(token, parsed.getRetryToken());

    // must be verifiable
    parsed.verify(org);
  }

  @Test
  public void parseKnown() {
    final byte[] data =
        Hex.dehex(
            "f0ff00001d123198cdb7fc8158ad26651d1333ab78e359dc143ef889880f335aa023d909c376d75aed9804916d7175696368657f000001365ca9c62bac3f083aac2f88a71b8ad8847afd8e41d3dace90ad5494ba86d05ec2f6");

    final RetryPacket parsed = RetryPacket.parse(Unpooled.wrappedBuffer(data)).complete(l -> aead);

    assertEquals(Version.DRAFT_29, parsed.getVersion());
    assertEquals(
        new ConnectionId(Hex.dehex("3198cdb7fc8158ad26651d1333ab78e359dc")),
        parsed.getDestinationConnectionId().get());
    assertEquals(
        new ConnectionId(Hex.dehex("3ef889880f335aa023d909c376d75aed9804916d")),
        parsed.getSourceConnectionId().get());
    assertEquals(28, parsed.getRetryToken().length);

    // must be verifiable
    parsed.verify(new ConnectionId(Hex.dehex("365ca9c62bac3f083aac2f88a71b8ad8847a")));
  }
}

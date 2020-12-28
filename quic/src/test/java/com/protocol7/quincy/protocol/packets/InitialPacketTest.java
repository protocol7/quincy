package com.protocol7.quincy.protocol.packets;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.AckBlock;
import com.protocol7.quincy.protocol.frames.AckFrame;
import com.protocol7.quincy.protocol.frames.ApplicationCloseFrame;
import com.protocol7.quincy.protocol.frames.ConnectionCloseFrame;
import com.protocol7.quincy.protocol.frames.CryptoFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.frames.MaxDataFrame;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.frames.PingFrame;
import com.protocol7.quincy.protocol.frames.StreamFrame;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.TestAEAD;
import com.protocol7.quincy.utils.Hex;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class InitialPacketTest {

  private final ConnectionId destConnId = ConnectionId.random();
  private final ConnectionId srcConnId = ConnectionId.random();
  private final byte[] token = Rnd.rndBytes(16);

  private final AEAD aead = TestAEAD.create();

  @Test
  public void testParseKnown() {
    final byte[] known = Hex.dehex("cababababa10f2edc36aae4b55fee1c6f18725c3567414fc0c1daf1d574fb616ad44c94a85f398428e2a160044824779c62922819a2410c5fb7cf8b678fdb6861c5eb4e14401a48ee4feeb2e05d8a6fb34bd12fb023d7217efd5bd2de2df23fe628b37e715843ccb7d0af23fe479f0b69a98a026cdad8f7221a49eff43a9f4d05431bc9c95d6733a9188a533aecf65b70ca9643b1715589fef4e98bf2e5c4410dbefc4a1ae838134d4c0ffe2308115987ce943ff1766f1118d96cdc22c01d4a5814dbcc950f866cde075557dfd42d7a70c435b67d352aa3b88ec82b2fdd5d77962f1a35b414a944ab344db14fd6accd936420d3e337c46f7384d81c7203ce60d737b261df68e9ad6f58425cbcc188f847a6e5d45b45d83831364efbb19b1db7b8be8de41634d4505c6f012d59d2e3823bef86f97a12700ad21ada62719499ce4921a81342cc433bfeea129fa2f44283509d3aba953568c2e17bd6891a55c0f0ad10959a877c2aa662df4dbf8b6d74237d86045aff625baf50ce40cb681bd6e5654314cf5a58fadbccec780642adec30afbc846e688964e769929423aa9d924ce290da08c2b7e448cebe717664ec43d997c4d2e1d7b6c0f5478e43bae68317384842269278c48bb32ee0013ffaa64267b41fa389c37195cccf2349f74c2e681a91feefb023b66591731970112186958d1e441c5b3de387a10428418af1947a9533c85f7e8a6e36223d5c81c19d9112f0959f12a2c3b98d9ec77f493d8a39733f27bc97e1c27273ee0ea3d425f539c97dcdd12b3d0a09070b6c0b7dfb15dbd619d505542edf09a81bb9b266a17a26feeb05208fa623541f87967299e8717a1697721dd35210e5859253e3992ed64f9866204fe2595c095ac5f000d768273c8f4d23e17d8cc063fcc4545e897c2ef4d3fa199ce5caa01983358290b7923d0cd466726e6cc24bec18ceaa82fff0bb5d415ad47b3810bba843037ec656a7c1974a73f0832fb47e4f9c56ead2919bd1f048781f34a0140b65d8ba56886b4e45444c0e5cfe61cc339851cb8bfd0f8eff1d15a65c0a43eccd784a35d5f9960e7ce29a4faf4ad6b308d1b999fce9542c869b8a2e8f77467e58241a2700c00be362c5c9fcdcee44df942c2fc5b00f9fecde6141b72ceda235a09dc3fe01e30713bad287200c3a8975b4c2c3a117532c026d435a076fdae611bd24c7eb889d0541fe74944395dc3265c7888a3817b4b49ddba1891c0e004ac67b96354cf35c562788ba7414e1d123b1d4dbe8f7bcdd69c8f07fc276862ec2f814c792bbb0d9c5cb1896886f0427b5b8cef6a7a910634c0eea691ca6665c1d071a9818cacad940f530de965c019fefa75173fc2c5ad273c0cc7c6e115e1b1572de88c5127fd94b61a841286f802d379a9161700f8359f7f6e3d34963e808df9e6a4e24f133d5f1314e232405c7fe080f540535cb63bb2c871f34856cb5a4c0616158024495ae66bbeafbd34ac78ab754ffb13166e33c0cf742af124144cd5fafda4062b8eb16d80138a01bde4468c814db8cf030e7390b15ea5554a5676ae82190c2ab51a15f87924ae37d66e17e64d3c49af21e8448413fb0b231209a218528666dfb62b190ab3a542c690f467f1546e9fbfe4aecfa4764fe25770137a9f2ac5ad05f97b539cac4fb4248a0002cce911849585b2c4c3e3206549e96a");

    final ByteBuf bb = Unpooled.wrappedBuffer(known);

    final HalfParsedPacket<InitialPacket> packet = InitialPacket.parse(bb);

  }

  @Test
  public void roundtrip() {
    final InitialPacket packet = p(123, Optional.of(token));

    final ByteBuf bb = Unpooled.buffer();

    packet.write(bb, aead);

    final InitialPacket parsed = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(destConnId, parsed.getDestinationConnectionId().get());
    assertEquals(srcConnId, parsed.getSourceConnectionId().get());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getVersion(), parsed.getVersion());
    assertArrayEquals(token, parsed.getToken().get());
    assertEquals(1 + AEAD.OVERHEAD, parsed.getPayload().calculateLength());
    assertTrue(parsed.getPayload().getFrames().get(0) instanceof PaddingFrame);
  }

  @Test
  public void roundtripMultiple() {
    final ByteBuf bb = Unpooled.buffer();

    p(1, Optional.of(token)).write(bb, aead);
    p(2, Optional.of(token)).write(bb, aead);

    final InitialPacket parsed1 = InitialPacket.parse(bb).complete(l -> aead);
    final InitialPacket parsed2 = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(parsed1.getDestinationConnectionId(), parsed2.getDestinationConnectionId());
    assertEquals(parsed1.getSourceConnectionId(), parsed2.getSourceConnectionId());
    assertEquals(1, parsed1.getPacketNumber());
    assertEquals(2, parsed2.getPacketNumber());
    assertEquals(parsed1.getVersion(), parsed2.getVersion());
    assertEquals(parsed1.getPayload(), parsed2.getPayload());
  }

  @Test
  public void roundtripNoToken() {
    final InitialPacket packet = p(123, empty());

    final ByteBuf bb = Unpooled.buffer();

    packet.write(bb, aead);

    final InitialPacket parsed = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(destConnId, parsed.getDestinationConnectionId().get());
    assertEquals(srcConnId, parsed.getSourceConnectionId().get());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getVersion(), parsed.getVersion());
    assertFalse(parsed.getToken().isPresent());
    assertEquals(1 + AEAD.OVERHEAD, parsed.getPayload().calculateLength());
    assertTrue(parsed.getPayload().getFrames().get(0) instanceof PaddingFrame);
  }

  @Test
  public void allowedFrames() {
    assertFrameAllowed(new CryptoFrame(0, new byte[0]));
    assertFrameAllowed(new AckFrame(0, new AckBlock(0, 0)));
    assertFrameAllowed(new PaddingFrame(1));
    assertFrameAllowed(new ConnectionCloseFrame(1, FrameType.CRYPTO, ""));
    assertFrameAllowed(new ApplicationCloseFrame(1, ""));
    assertFrameAllowed(PingFrame.INSTANCE);

    assertFrameNotAllowed(new MaxDataFrame(123));
    assertFrameNotAllowed(new StreamFrame(123, 0, false, new byte[0]));
  }

  private void assertFrameAllowed(final Frame frame) {
    InitialPacket.create(
        Optional.of(destConnId),
        Optional.of(srcConnId),
        PacketNumber.MIN,
        Version.DRAFT_29,
        empty(),
        frame);
  }

  private void assertFrameNotAllowed(final Frame frame) {
    try {
      InitialPacket.create(
          Optional.of(destConnId),
          Optional.of(srcConnId),
          PacketNumber.MIN,
          Version.DRAFT_29,
          empty(),
          frame);
      fail();
    } catch (final IllegalArgumentException e) {
    }
  }

  private InitialPacket p(final long pn, final Optional<byte[]> token) {
    return InitialPacket.create(
        Optional.of(destConnId),
        Optional.of(srcConnId),
        pn,
        Version.DRAFT_29,
        token,
        List.of(new PaddingFrame(1)));
  }
}

package com.protocol7.quincy.protocol.packets;

import static java.util.Optional.empty;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.AckFrame;
import com.protocol7.quincy.protocol.frames.AckRange;
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
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.aead.TestAEAD;
import com.protocol7.quincy.utils.Hex;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import org.junit.Test;

public class InitialPacketTest {

  private final ConnectionId destConnId = ConnectionId.random();
  private final ConnectionId srcConnId = ConnectionId.random();
  private final byte[] token = Rnd.rndBytes(16);
  final int paddingLength = 6;

  private final AEAD aead = TestAEAD.create();

  @Test
  public void testParseKnown() {
    final byte[] known =
        Hex.dehex(
            "cababababa100ab8c6777e6b7cb24441f8f1d030cf6314b4860ea2b36e0d4b8064611cfaf37f15c58c0e7b0044823f61637945dacb3ac7dbbcabfde18cc319c70a1aaf1289082eb63df42d7ec442af5ac39f5de9cb934e92119ea4608daf6c851e146c87cd43a318e3d1ca99851a9a68c6d1ec09f5d034eea6fd66f3162451d5ef7777e4ae8ac39905dc45da3dd06eb558c412a8ff7a3df608b5adab05c19a8dc6df42984eb7feed6cca40b1f2f3283d656f2c8ce998c8615341365456f1523737737f80201898321edd2a9b7d8c8af46d2b379db6855b5bfcc1608da0da0093ba4aa038e01aed2a892efb09e1e5261e538d196e4a2f58c0fc8cef6aed13b7d5c0703f4d765404acc5bd53893feea75a1dd743001857d5767315cbec4e9e5250d61497ca9712c7ef7cf6a7c91cea7a173348b83ed6c7f0b436881e3572f0345e46684750cd576c4c33a3e6242915607647c3f6100cc25caefa4d101640464c9b4f3bb2f0a85b6eb0879ababc099e275981997f6a4a81a475e7e5b8d0b4e404e517587bc6e0e9e6f0e352720cb0e2ddb4b74c1ed360022731cc3a411dc6c63f5b04f763368b9518b07dee091371107c9c20c71e0e49f41a08b5ef00168c0da7253c8da0dbdae9a267c2a4af6393cc4e6fed8d0716770300b0f91f092ea60ed94861bded3b14b4e467dfd62debed9371de66c83dfd33eca9b5ab287b4b4e763b05b33bdbe387c16ea827c58c4895aa1cce53ede90151b141bab7ca0aff778d8035b725517ae6f3a858bfcbb64389b21933a20532d54a248bb1454b95a519e3b0cbc7d206d891877143489a32e1b83a9051a4bf3e18c1275706484bac2f434c6b22e1423c5260a5f730e5b630130d4552039fc7b0b05ede17f6ae2c25b065536bc13bfb8db218bd75ef070eff0264d47a5de1d7c71969d6626570ab862e429264d350b71a69c31093ce87507ae42cd7e813bf37fa1f2b97d81f2f8e6fe201646ffb49bde14ab07af0f7db81cb3eae6e3804fae8a27509ee5c0403c976c932593f44955f90b696ebed4856609a48ebd4775fdec003f072ae65fb2f394f0094c9610d04d657b45298a83ef26cb652ec3d4ca984801e4fc609eb3f80ee0bb954d04cfb6f60513a4e5e30f46ab81c1f45f19b4ca831f25fd1945d9d9843781c3e495dc5a25c68de21d7ec3e33b05b973eea30e2c1bc682d604389d2b19fb2ba4074758b24e168ad7895144ecb9f227236092f56f33d639b7a6e3c504906b726da60733cd9cdcdc83dab6a74f5dc056328dd0834de16dce3f5d1d508b28541473c95d0b7bb8a6fb38d5695561be2579b7fb8fb0605add41543cc63838a435146d8eb4858fbca558175db2b3e5a635365ad1cc42069927ab596c2f50cae329a8b481d42f18050e33fa1e187f1aa0d2655dd932c530f63f1b86446c4c173b2c502a611694858ac006a0551c63439d2d87e8c4d6dd24fc829358275993c416be684833d9689e4e25908d46f6892383ec937747c2e5990035c1f0c64b05cca07cef33dc81dca2f0234107aaaf9edfff7fcb5fe655e163cb3cf215bf2a7a845f1e3aa02ad627e9e3fd351e3f66431ee6a6c443791de208beebe99f02259bcf89686faba074ba51f5b05dff75e906224750e89b735983801ca7e3a7d4c2948adad658e74a5fdaefe1f06139e681823f01faa2876439493");

    final ByteBuf bb = Unpooled.wrappedBuffer(known);

    final AEAD aead = InitialAEAD.create(Hex.dehex("0ab8c6777e6b7cb24441f8f1d030cf63"), false);

    final InitialPacket packet = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(0, packet.getPacketNumber());
    assertEquals(
        new ConnectionId(Hex.dehex("0ab8c6777e6b7cb24441f8f1d030cf63")),
        packet.getDestinationConnectionId());
    assertEquals(
        new ConnectionId(Hex.dehex("b4860ea2b36e0d4b8064611cfaf37f15c58c0e7b")),
        packet.getSourceConnectionId());
    assertEquals(PacketType.Initial, packet.getType());
    assertEquals(new Version(Hex.dehex("babababa")), packet.getVersion());
    assertEquals(Optional.empty(), packet.getToken());
    assertEquals(2, packet.getPayload().getFrames().size());
  }

  @Test
  public void roundtrip() {
    final InitialPacket packet = p(123, Optional.of(token));

    final ByteBuf bb = Unpooled.buffer();

    packet.write(bb, aead);

    final InitialPacket parsed = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(destConnId, parsed.getDestinationConnectionId());
    assertEquals(srcConnId, parsed.getSourceConnectionId());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getVersion(), parsed.getVersion());
    assertArrayEquals(token, parsed.getToken().get());
    assertEquals(paddingLength + AEAD.OVERHEAD, parsed.getPayload().calculateLength());
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

    assertEquals(destConnId, parsed.getDestinationConnectionId());
    assertEquals(srcConnId, parsed.getSourceConnectionId());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getVersion(), parsed.getVersion());
    assertFalse(parsed.getToken().isPresent());
    assertEquals(paddingLength + AEAD.OVERHEAD, parsed.getPayload().calculateLength());
    assertTrue(parsed.getPayload().getFrames().get(0) instanceof PaddingFrame);
  }

  @Test
  public void allowedFrames() {
    assertFrameAllowed(new CryptoFrame(0, new byte[0]));
    assertFrameAllowed(new AckFrame(0, new AckRange(0, 0)));
    assertFrameAllowed(new PaddingFrame(1));
    assertFrameAllowed(new ConnectionCloseFrame(1, FrameType.CRYPTO, ""));
    assertFrameAllowed(new ApplicationCloseFrame(1, ""));
    assertFrameAllowed(PingFrame.INSTANCE);

    assertFrameNotAllowed(new MaxDataFrame(123));
    assertFrameNotAllowed(new StreamFrame(123, 0, false, new byte[0]));
  }

  private void assertFrameAllowed(final Frame frame) {
    InitialPacket.create(destConnId, srcConnId, PacketNumber.MIN, Version.DRAFT_29, empty(), frame);
  }

  private void assertFrameNotAllowed(final Frame frame) {
    try {
      InitialPacket.create(
          destConnId, srcConnId, PacketNumber.MIN, Version.DRAFT_29, empty(), frame);
      fail();
    } catch (final IllegalArgumentException e) {
    }
  }

  private InitialPacket p(final long pn, final Optional<byte[]> token) {
    return InitialPacket.create(
        destConnId, srcConnId, pn, Version.DRAFT_29, token, new PaddingFrame(paddingLength));
  }
}

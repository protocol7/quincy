package com.protocol7.quincy.protocol.packets;

import static java.util.Optional.empty;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.StreamId;
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
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class InitialPacketTest {

  private final ConnectionId destConnId = ConnectionId.random();
  private final ConnectionId srcConnId = ConnectionId.random();
  private final byte[] token = Rnd.rndBytes(16);

  private final AEAD aead = TestAEAD.create();

  @Test
  public void roundtrip() {
    final InitialPacket packet = p(new PacketNumber(123), Optional.of(token));

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

    p(new PacketNumber(1), Optional.of(token)).write(bb, aead);
    p(new PacketNumber(2), Optional.of(token)).write(bb, aead);

    final InitialPacket parsed1 = InitialPacket.parse(bb).complete(l -> aead);
    final InitialPacket parsed2 = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(parsed1.getDestinationConnectionId(), parsed2.getDestinationConnectionId());
    assertEquals(parsed1.getSourceConnectionId(), parsed2.getSourceConnectionId());
    assertEquals(new PacketNumber(1), parsed1.getPacketNumber());
    assertEquals(new PacketNumber(2), parsed2.getPacketNumber());
    assertEquals(parsed1.getVersion(), parsed2.getVersion());
    assertEquals(parsed1.getPayload(), parsed2.getPayload());
  }

  @Test
  public void roundtripNoToken() {
    final InitialPacket packet = p(new PacketNumber(123), empty());

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

    assertFrameNotAllowed(PingFrame.INSTANCE);
    assertFrameNotAllowed(new MaxDataFrame(123));
    assertFrameNotAllowed(new StreamFrame(new StreamId(123), 0, false, new byte[0]));
  }

  private void assertFrameAllowed(final Frame frame) {
    InitialPacket.create(
        Optional.of(destConnId),
        Optional.of(srcConnId),
        PacketNumber.MIN,
        Version.DRAFT_18,
        empty(),
        frame);
  }

  private void assertFrameNotAllowed(final Frame frame) {
    try {
      InitialPacket.create(
          Optional.of(destConnId),
          Optional.of(srcConnId),
          PacketNumber.MIN,
          Version.DRAFT_18,
          empty(),
          frame);
      fail();
    } catch (final IllegalArgumentException e) {
    }
  }

  private InitialPacket p(final PacketNumber pn, final Optional<byte[]> token) {
    return InitialPacket.create(
        Optional.of(destConnId),
        Optional.of(srcConnId),
        pn,
        Version.DRAFT_18,
        token,
        List.of(new PaddingFrame(1)));
  }
}

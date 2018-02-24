package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import com.google.common.primitives.UnsignedLong;
import com.protocol7.nettyquick.TestUtil;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ConnectionIdTest {

  @Test
  public void roundtrip() {
    assertRoundtrip(ConnectionId.random());
    assertRoundtrip(new ConnectionId(UnsignedLong.ZERO));
    assertRoundtrip(new ConnectionId(UnsignedLong.MAX_VALUE));
  }

  private void assertRoundtrip(ConnectionId connectionId) {
    ByteBuf bb = Unpooled.buffer();
    connectionId.write(bb);

    ConnectionId parsed = ConnectionId.read(bb);

    assertEquals(parsed, connectionId);
  }

  @Test
  public void write() {
    assertWrite("0000000000000000", UnsignedLong.ZERO);
    assertWrite("ffffffffffffffff", UnsignedLong.MAX_VALUE);
  }

  private void assertWrite(String expected, UnsignedLong id) {
    ByteBuf bb = Unpooled.buffer();
    new ConnectionId(id).write(bb);
    TestUtil.assertBuffer(expected, bb);
  }
}
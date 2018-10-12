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
  }

  private void assertRoundtrip(ConnectionId connectionId) {
    ByteBuf bb = Unpooled.buffer();
    connectionId.write(bb);

    ConnectionId parsed = ConnectionId.read(connectionId.getLength(), bb);

    assertEquals(parsed, connectionId);
  }
}
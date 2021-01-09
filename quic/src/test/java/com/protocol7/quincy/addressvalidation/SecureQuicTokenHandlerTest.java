package com.protocol7.quincy.addressvalidation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.tls.KeyUtil;
import com.protocol7.quincy.utils.Ticker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class SecureQuicTokenHandlerTest {

  private final ConnectionId destConnId = ConnectionId.random();
  private SecureQuicTokenHandler rt;
  private InetSocketAddress addressV4;
  private InetSocketAddress addressV6;
  private final Ticker ticker = mock(Ticker.class);

  @Before
  public void setUp() throws UnknownHostException {
    final PrivateKey key = KeyUtil.getPrivateKey("src/test/resources/server.der");

    when(ticker.milliTime()).thenReturn(123L);

    rt = new SecureQuicTokenHandler(key, 100, TimeUnit.MILLISECONDS, ticker);
    addressV4 = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 1234);
    addressV6 =
        new InetSocketAddress(
            InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334"), 4567);
  }

  @Test
  public void roundtripV4() {
    final ByteBuf token = Unpooled.buffer();
    assertTrue(rt.writeToken(token, destConnId, addressV4));

    assertTrue(rt.validateToken(token, addressV4).isPresent());
  }

  @Test
  public void roundtripV6() {
    final ByteBuf token = Unpooled.buffer();
    assertTrue(rt.writeToken(token, destConnId, addressV6));

    assertTrue(rt.validateToken(token, addressV6).isPresent());
  }

  @Test
  public void roundtripFailTtl() {
    final ByteBuf token = Unpooled.buffer();
    assertTrue(rt.writeToken(token, destConnId, addressV4));

    when(ticker.milliTime()).thenReturn(999L);

    assertFalse(rt.validateToken(token, addressV4).isPresent());
  }

  @Test
  public void roundtripFailAddress() throws UnknownHostException {
    final ByteBuf token = Unpooled.buffer();
    assertTrue(rt.writeToken(token, destConnId, addressV4));

    assertFalse(
        rt.validateToken(token, new InetSocketAddress(InetAddress.getByName("127.0.0.2"), 9999))
            .isPresent());
  }

  @Test
  public void roundtripFailHmac() {
    final ByteBuf token = Unpooled.buffer();
    assertTrue(rt.writeToken(token, destConnId, addressV4));

    final int pos = token.writerIndex() - 1;
    token.setByte(pos, token.getByte(pos) + 1); // invalidate HMAC
    assertFalse(rt.validateToken(token, addressV4).isPresent());
  }
}

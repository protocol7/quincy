package com.protocol7.quincy.addressvalidation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.protocol7.quincy.tls.KeyUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import org.junit.Before;
import org.junit.Test;

public class RetryTokenTest {

  private RetryToken rt;
  private InetAddress address;
  private long ttl = System.currentTimeMillis();

  @Before
  public void setUp() throws UnknownHostException {
    final PrivateKey key = KeyUtil.getPrivateKey("src/test/resources/server.der");
    rt = new RetryToken(key);
    address = InetAddress.getByName("127.0.0.1");
  }

  @Test
  public void roundtrip() {
    final byte[] token = rt.create(address, ttl + 100);
    assertTrue(rt.validate(token, address, ttl));
  }

  @Test
  public void roundtripFailTtl() {
    final byte[] token = rt.create(address, ttl - 100);
    assertFalse(rt.validate(token, address, ttl));
  }

  @Test
  public void roundtripFailAddress() throws UnknownHostException {
    final byte[] token = rt.create(address, ttl + 100);
    assertFalse(rt.validate(token, InetAddress.getByName("127.0.0.2"), ttl));
  }

  @Test
  public void roundtripFailHmac() {
    final byte[] token = rt.create(address, ttl + 100);
    token[token.length - 1]++; // invalidate HMAC
    assertFalse(rt.validate(token, address, ttl));
  }
}

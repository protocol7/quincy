package com.protocol7.nettyquick.tls;

import static com.protocol7.nettyquick.TestUtil.assertHex;
import static com.protocol7.nettyquick.tls.CipherSuite.TLS_AES_128_GCM_SHA256;
import static com.protocol7.nettyquick.tls.CipherSuite.TLS_AES_256_GCM_SHA384;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;

public class CipherSuiteTest {

  @Test
  public void parseKnown() {
    ByteBuf bb = Unpooled.wrappedBuffer(Hex.dehex("00 04 13 01 99 99"));
    assertEquals(ImmutableList.of(TLS_AES_128_GCM_SHA256), CipherSuite.parseKnown(bb));
  }

  @Test
  public void parseOne() {
    ByteBuf bb = Unpooled.wrappedBuffer(Hex.dehex("13 01"));
    assertEquals(TLS_AES_128_GCM_SHA256, CipherSuite.parseOne(bb).get());
  }

  @Test
  public void parseOneUnknown() {
    ByteBuf bb = Unpooled.wrappedBuffer(Hex.dehex("99 99"));
    assertFalse(CipherSuite.parseOne(bb).isPresent());
  }

  @Test
  public void writeAll() {
    List<CipherSuite> cipherSuites =
        ImmutableList.of(TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384);

    ByteBuf bb = Unpooled.buffer();
    CipherSuite.writeAll(bb, cipherSuites);

    assertHex("000413011302", bb);
  }

  @Test
  public void fromValue() {
    assertEquals(TLS_AES_128_GCM_SHA256, CipherSuite.fromValue(0x1301).get());
  }

  @Test
  public void fromValueUnknown() {
    assertFalse(CipherSuite.fromValue(0x9999).isPresent());
  }
}

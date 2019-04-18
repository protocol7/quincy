package com.protocol7.quincy.tls;

import static com.protocol7.quincy.tls.CipherSuite.TLS_AES_128_GCM_SHA256;
import static com.protocol7.quincy.tls.CipherSuite.TLS_AES_256_GCM_SHA384;
import static com.protocol7.quincy.tls.TestUtil.assertHex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;

public class CipherSuiteTest {

  @Test
  public void parseKnown() {
    final ByteBuf bb = Unpooled.wrappedBuffer(Hex.dehex("00 04 13 01 99 99"));
    assertEquals(List.of(TLS_AES_128_GCM_SHA256), CipherSuite.parseKnown(bb));
  }

  @Test
  public void parseOne() {
    final ByteBuf bb = Unpooled.wrappedBuffer(Hex.dehex("13 01"));
    assertEquals(TLS_AES_128_GCM_SHA256, CipherSuite.parseOne(bb).get());
  }

  @Test
  public void parseOneUnknown() {
    final ByteBuf bb = Unpooled.wrappedBuffer(Hex.dehex("99 99"));
    assertFalse(CipherSuite.parseOne(bb).isPresent());
  }

  @Test
  public void writeAll() {
    final List<CipherSuite> cipherSuites = List.of(TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384);

    final ByteBuf bb = Unpooled.buffer();
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

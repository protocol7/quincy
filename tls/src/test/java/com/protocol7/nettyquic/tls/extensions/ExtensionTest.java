package com.protocol7.nettyquic.tls.extensions;

import static com.protocol7.nettyquic.tls.Group.X25519;
import static com.protocol7.nettyquic.tls.TestUtil.assertHex;
import static com.protocol7.nettyquic.utils.Hex.hex;
import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.Unpooled;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;

public class ExtensionTest {

  @Test
  public void parseKnown() {
    byte[] b =
        Hex.dehex(
            "002b0002030400330024001d0020ae3492c510ba781d6e30ff69b66d47c710f7ef060f846e28bda2f995b4fb4645");

    List<Extension> ext = Extension.parseAll(Unpooled.wrappedBuffer(b), true);

    Iterator<Extension> iter = ext.iterator();

    SupportedVersions supportedVersions = (SupportedVersions) iter.next();
    assertEquals("0304", hex(supportedVersions.getVersion()));

    KeyShare keyShare = (KeyShare) iter.next();
    assertEquals(1, keyShare.getKeys().size());
    assertHex(
        "ae3492c510ba781d6e30ff69b66d47c710f7ef060f846e28bda2f995b4fb4645",
        keyShare.getKey(X25519).get());
  }
}

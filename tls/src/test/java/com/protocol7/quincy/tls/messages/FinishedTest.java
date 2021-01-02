package com.protocol7.quincy.tls.messages;

import static com.protocol7.quincy.tls.TestUtil.assertHex;
import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class FinishedTest {

  @Test
  public void parseKnownServerHandshakeFinished() {
    final ByteBuf bb =
        Unpooled.wrappedBuffer(
            Hex.dehex("140000200c3dcdc53b56e8d4a127d104737ffc3093d005c7134837958cf32c33ee57ea96"));

    final Finished fin = Finished.parse(bb);

    assertHex(
        "0c3dcdc53b56e8d4a127d104737ffc3093d005c7134837958cf32c33ee57ea96",
        fin.getVerificationData());
  }

  @Test
  public void createClientFinished() {
    final Finished fin =
        Finished.createClientFinished(
            Hex.dehex("ff0e5b965291c608c1e8cd267eefc0afcc5e98a2786373f0db47b04786d72aea"),
            Hex.dehex("22844b930e5e0a59a09d5ac35fc032fc91163b193874a265236e568077378d8b"));

    assertEquals(
        "976017a77ae47f1658e28f7085fe37d149d1e9c91f56e1aebbe0c6bb054bd92b",
        Hex.hex(fin.getVerificationData()));
  }

  @Test
  public void roundtrip() {
    final byte[] certVerificationData =
        Hex.dehex("4c6e3380e3b4034484753f79b0946ffc8a201fb4d3c1e8031a815ede45d9dbed");

    final Finished fin = new Finished(certVerificationData);

    final ByteBuf bb = Unpooled.buffer();
    fin.write(bb);

    final Finished parsedSHE = Finished.parse(bb);

    assertHex(fin.getVerificationData(), parsedSHE.getVerificationData());
  }
}

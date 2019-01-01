package com.protocol7.nettyquic.tls.messages;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ClientFinishedTest {

  @Test
  public void roundtrip() {
    byte[] vd =
        Hex.dehex(
            "97 60 17 a7 7a e4 7f 16 58 e2 8f 70 85 fe 37 d1 49 d1 e9 c9 1f 56 e1 ae bb e0 c6 bb 05 4b d9 2b");

    ClientFinished fin = new ClientFinished(vd);

    ByteBuf bb = Unpooled.buffer();
    fin.write(bb);

    ClientFinished parsed = ClientFinished.parse(bb);
    assertEquals(Hex.hex(vd), Hex.hex(parsed.getVerificationData()));
  }

  @Test
  public void createKnownTls() {
    ClientFinished fin =
        ClientFinished.create(
            Hex.dehex("ff0e5b965291c608c1e8cd267eefc0afcc5e98a2786373f0db47b04786d72aea"),
            Hex.dehex("22844b930e5e0a59a09d5ac35fc032fc91163b193874a265236e568077378d8b"),
            false);

    assertEquals(
        "976017a77ae47f1658e28f7085fe37d149d1e9c91f56e1aebbe0c6bb054bd92b",
        Hex.hex(fin.getVerificationData()));
  }
}

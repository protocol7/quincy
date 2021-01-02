package com.protocol7.quincy.tls.messages;

import static com.protocol7.quincy.tls.TestUtil.assertHex;

import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ServerHandshakeFinishedTest {

  @Test
  public void parseKnownServerHandshakeFinished() {
    final ByteBuf bb =
        Unpooled.wrappedBuffer(
            Hex.dehex("140000200c3dcdc53b56e8d4a127d104737ffc3093d005c7134837958cf32c33ee57ea96"));

    final ServerHandshakeFinished fin = ServerHandshakeFinished.parse(bb);

    assertHex(
        "0c3dcdc53b56e8d4a127d104737ffc3093d005c7134837958cf32c33ee57ea96",
        fin.getVerificationData());
  }

  @Test
  public void roundtrip() {
    final byte[] certVerificationData =
        Hex.dehex("4c6e3380e3b4034484753f79b0946ffc8a201fb4d3c1e8031a815ede45d9dbed");

    final ServerHandshakeFinished fin = new ServerHandshakeFinished(certVerificationData);

    final ByteBuf bb = Unpooled.buffer();
    fin.write(bb);

    final ServerHandshakeFinished parsedSHE = ServerHandshakeFinished.parse(bb);

    assertHex(fin.getVerificationData(), parsedSHE.getVerificationData());
  }
}

package com.protocol7.quincy.tls;

import com.protocol7.quincy.utils.Hex;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.Assert;
import org.junit.Test;

public class CertificateVerifyTest {

  @Test
  public void roundtrip() {
    byte[] hash = Hex.dehex("3e66361ada42c7cb97f9a62b00cae1d8b584174c745f9a338cf9f7cdd51d15f8");

    PrivateKey privateKey = KeyUtil.getPrivateKey("src/test/resources/server.der");
    byte[] actual = CertificateVerify.sign(hash, privateKey, false);

    PublicKey publicKey = KeyUtil.getCertFromCrt("src/test/resources/server.crt").getPublicKey();

    Assert.assertTrue(CertificateVerify.verify(actual, hash, publicKey, false));
  }

  @Test
  public void verifyKnown() {
    // from quic-go, including a copy of the certificate for this test
    byte[] sign =
        Hex.dehex(
            "868b5cae8578cbb3a5e4f2290e164623565bf8671eda69c2f225c138429f32b29e99dcb284bd0b1926579682a5130c79e5657d375e3f92bcf4ac7f8f47f999e1e5034c91f569a6fe908b1b9b8bc6064201359640f9e5dfd290dc8a3450f4dd474bac948c3bb09a016bbd01f25b19e8d305ccfb690e1030331d5769fcd89eba8522deda6f070ca476f416eecb44f3996c1a7be07243f8fbf04ea5baef55ebbf8fa2d037d1b032796d99e4dc44194a99e3c2e93d0dc6bb365c3685070ba4aea94693aa73811cfcb5d56ea3295779562d23bba5838eee1121b9851bb795862f1b060b7a6611577ada8f76da34ff11a65c1380985e47996363726a2d739ac84754a1");
    byte[] hash = Hex.dehex("173322914473dfc8651a3f549eb7118a706be48f305aa738429d9af647f7722b");

    PublicKey publicKey = KeyUtil.getPublicKey("src/test/resources/quic-go.der");

    Assert.assertTrue(CertificateVerify.verify(sign, hash, publicKey, false));
  }
}

package com.protocol7.quincy.tls.messages;

import static com.protocol7.quincy.tls.TestUtil.assertHex;
import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ServerCertificateVerifyTest {

  @Test
  public void parseKnownServerCertificateVerify() {
    final ByteBuf bb =
        Unpooled.wrappedBuffer(
            Hex.dehex(
                "0f0001040804010062c0a38f1b6f04544157ec95e84024783e50f6ce0eff68d23529208d71e2cb615d77214973d60068de606b243ad43cac6a708d0efd7de55829e20b95ea8d65bc9e3a70e69ba6ccd6693cd4df68a1e3ced6fac9615a461f2a2c34c45d4234b88262b8fb6b3b37428b1f939dadc73e9783b30037480c38cae0db041cc637b000eb8e162ddb6ff115c192735ba487a45159dee18215a62d45e95a33f3ef62eb93835a2dae84944549eacd4405aa66b9245f8944b4de7c2fbf48070a9b770a2271555f00bb10032182bf3912f57fa22ef8b11f680b78c6c18484db750f623125dea2a97976c4b0e86d0532def08e61434be4006afbddd1de91238a6b2eb44c32e5c2140000206438e3dfae06a5f19b7da35905e073f2bf4f81d4f1a8a411bbdedda2f7f76f76"));

    final ServerCertificateVerify scv = ServerCertificateVerify.parse(bb);
    assertEquals(2052, scv.getVerifyType());
    assertHex(
        "62c0a38f1b6f04544157ec95e84024783e50f6ce0eff68d23529208d71e2cb615d77214973d60068de606b243ad43cac6a708d0efd7de55829e20b95ea8d65bc9e3a70e69ba6ccd6693cd4df68a1e3ced6fac9615a461f2a2c34c45d4234b88262b8fb6b3b37428b1f939dadc73e9783b30037480c38cae0db041cc637b000eb8e162ddb6ff115c192735ba487a45159dee18215a62d45e95a33f3ef62eb93835a2dae84944549eacd4405aa66b9245f8944b4de7c2fbf48070a9b770a2271555f00bb10032182bf3912f57fa22ef8b11f680b78c6c18484db750f623125dea2a97976c4b0e86d0532def08e61434be4006afbddd1de91238a6b2eb44c32e5c2",
        scv.getSignature());
  }

  @Test
  public void roundtrip() {
    final byte[] certVerificationSig =
        Hex.dehex(
            "3ba292498f188921651a6dcfe03416ff136070fbd84cb4d442fa9c389fb2cbfaa4e5ecff4bf644b93334faebc3b5732568ca965609ac32a37587ec4540f6617e9a5ee651b7e67bd4e46be7275813a1b1efdb5d5b072903ba0358e41e9fcdab52ae1b569cc5d82b00b70d30410d4ed382b978a41f01e5d6ee484edd34dc2d771c0bd35db8244ed6d024909cbf07d9864e38cadfe77a7172b8add527fa86b1f98d067b0803b8fabd8ae6ef957e52762d59dfb95c9c53cef5e4af4ba7b97974912edd37a84024f23ddac11e82cedd29f1d128137d1ae947f832cc4108fbc035f405ab93e48107db69795d26c6a1ec4e576b374b554b1a7877b5b059f5b6dd38293a");
    final ServerCertificateVerify scv = new ServerCertificateVerify(2052, certVerificationSig);

    final ByteBuf bb = Unpooled.buffer();
    scv.write(bb);

    final ServerCertificateVerify parsedSCV = ServerCertificateVerify.parse(bb);

    assertEquals(scv.getVerifyType(), parsedSCV.getVerifyType());
    assertHex(scv.getSignature(), parsedSCV.getSignature());
  }
}

package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquick.TestUtil;
import com.protocol7.nettyquick.protocol.frames.PaddingFrame;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.tls.AEAD;
import com.protocol7.nettyquick.tls.NullAEAD;
import com.protocol7.nettyquick.tls.TestAEAD;
import com.protocol7.nettyquick.utils.Hex;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class UnprotectedPayloadTest {

  private final AEAD aead = TestAEAD.create();
  private final PacketNumber pn = new PacketNumber(1);
  private final byte[] aad = new byte[12];

  @Test
  public void roundtrip() {
    UnprotectedPayload payload = new UnprotectedPayload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    payload.write(bb, aead, pn, aad);

    UnprotectedPayload parsed = UnprotectedPayload.parse(bb, payload.getLength(), aead, pn, aad);

    assertEquals(payload, parsed);
  }

  @Test
  public void write() {
    UnprotectedPayload payload = new UnprotectedPayload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    payload.write(bb, aead, pn, aad);

    TestUtil.assertBuffer("e7ece5d6d0adf7f6bf189050d8fcace66501", bb);
  }

  @Test
  public void parse() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("e7ece5d6d0adf7f6bf189050d8fcace66501"));
    UnprotectedPayload parsed = UnprotectedPayload.parse(bb, bb.writerIndex(), aead, pn, aad);

    UnprotectedPayload expected = new UnprotectedPayload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);
    assertEquals(expected, parsed);
  }

  @Test
  public void addFrame() {
    UnprotectedPayload payload = new UnprotectedPayload(PingFrame.INSTANCE);

    UnprotectedPayload withAdded = payload.addFrame(PaddingFrame.INSTANCE);

    UnprotectedPayload expected = new UnprotectedPayload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);

    assertEquals(expected, withAdded);
    assertEquals(new UnprotectedPayload(PingFrame.INSTANCE), payload); // must not been mutated
  }

  @Test
  public void parseKnown() {
    byte[] b = Hex.dehex("820607daeee3d48e6f5208b97d8380ea38c52177d3cb60cd9e965b3e062ef97cd7c7429f31a8e36dac45a9070df4260c1e097de320bcbfe4baea1cc7ce2c3b9ca01fafd79a66e304ca437876d3cea3065ddc2e40813f46a210e8df5ec56351037c55337b07da77558371329cb914e1674f42bc7d8ecbe9716c454872f5ed559b70be20845c18dd1b0bef002e13ed84d6ea6394bb71f740792c5c3a6d5e109668f4a8549e16af734100c64679a2d5e6ff1fa8e6a9fd81af8a96d3d4dc17a06ae6ebe15fe69038425d93fa2be107b8453d602560d548bb43c0ba60f2736d57449b22110f7014a87a294193f73fdd8666737c049d214978c9d6e3342ced6d079703c9ea3554201827e1281213054983d48be0492fe95960c5e2a9b028607627768b414ade77136b84f27f9dee1af7989ea1b07330753c9434c583d0ab8bf134b946605d7aae3f7c7a4eb9eed33b91e76c509f0c4b5a4a5ffc5a340a7f204596bbb0ac587f543a0912da555b254320f8b28fb2237fa88e00df5faa80751710d6620fe4b756ace3e311cc1069e4b09580247e6a5b10d0031f7f7f7b81f453ebff36eee9ed264bf3f2b74a446e70447566af5366958354567fa191339e6d890d3bf6521d5b1ecc8f40b6b795920c482c5802f2b6e7d2b471b003f27cfb1911bef44af4b9c2a4ffa867caa61f8c3b33b34489c22833673bc1eb558534a7f796f413adc97772855c1a73709752567f677d680e99e8c8b8b803fe2040012c3a7d8d71d6be76ef136a1142d36435c72c8472b4094fbdbcb2fdb0ea5df2e6ef29c4e91269d3cd69845b928de02b20332481dd64a4b602b415af30d50744b7a93f34b4a76c1c61593080cc45d5b37030cc642a6e31e1b00cd7df411571913e781f8b4b131cda6eeea9dec6de5e91b3616761cf2ac0af8e1cf85a29a81a0f47dce4d06e856128204c623007a119248c759a1816e71830e73a7cb4f6fae33f4ecf207b5bc32eff66c9f4055aa114e387e321034096a3cb3ce52ee37a8407f37d29cd45e0e2a64cee77594bbd8250ac4ef719fac3d5c2a7d527f03eacaa6faabb8a1f4745292a8eea359e41aaeb9dda1b77eabfb2a5191740e0bcedf6c3593ea5e982276c1b5041b97bab6213ab8eb8122e1a691de63bc24dda069903125c0d56414a20efa82fbd81b28f65bf151c2f42be3e7c0b6bf62063c2e31c5f6b102fbf27e18cc2a796b2e1e6d4425d362fee8cdc803356bcdc72e342458b7dbdcf982075ac7657b112a653b336ea0c6b4e956c6136b08d5ed06cc163072e531965a0dc8a81044c5fae28e4156c4b336869f7dd7a122c32caa225ca8c572c3a20ee0ce109090bacd57209b3bd59128ff3c360bc0ad5cdd9874e22528cec1c866024cdf5455fc7f84e8c8fc9f070eff3c800de91f95dad91987e9b9eff99050f2ec235eb98dde9b148bdb28c80a17cf91afa3aaf53a6b9dc66a751302359cd05fa30fd575d8ef2a107367296c481d5d8a5d73b4cc1eee5ba77ebb4a064a00aa2e54d8a4f26d854988123248fe90ea4b306942d02da55bbb0376ca0de455663bb8a0ca1ff7f24eee150611c44d85d250e5f6f67b33c193b49ae858170ef8f4270c993c4f2942182ac4cacaaabefc3170c9907debb79befae9897ca8f7b6642ec630421218312a4edd40f8b425a52bf6bb1f9c8b822e67237d40222782cd5f3581e531ecd47998239a179dfc");

    ByteBuf bb = Unpooled.wrappedBuffer(b);

    AEAD aead = new AEAD(
            Hex.dehex("9dc2872d9183102a4a75adbfad81c930"),
            Hex.dehex("71982e1f323b8d41377f79c6913f76d9"),

            Hex.dehex("c9ee450f01e4718214341a45"),
            Hex.dehex("af0c01b4fcd8c7e228f2dfe5"));

    PacketNumber pn = new PacketNumber(1);

    byte[] aad = Hex.dehex("ff000000651094f217c33fe1b40a695562cbf368fd43019bfd5ca6f32f8dc9e1ee7afcd038f553e696e92e06d6feb3e4c7197b91b0e047b7c3528f0cc8c8a23e631928bef2ddca99b78644c001");

    UnprotectedPayload up = UnprotectedPayload.parse(bb, bb.readableBytes(), aead, pn, aad);

    System.out.println(up);
  }

  @Test
  public void parseKnown2() {
    /*
    ###### Using secrets 36b0e5f536d75764fa21799d9964699f, b278918d0cd75fb81ecf747216d84dd3, ddd3bacd933c5d8fa9d7b2cb, 40338d77efea4e35c5f31acd
###### open 1 3268ec613883dd587bca2e28e637329a8534d3e7c52b592e891b3bfdd10504887597c6a063ed5f5d27e5eca016062029f4955e6d67c22617916c0dbe458e2db890c2f56bfa1ae00e926bb0f15f9b705dac750c1f2616e0aa13c1290a10c466ca262c43c718ae7686cddce744089fa67a682fb46b048a67a50b58bb9925762c7c
5b7932dc293e30276d88d4be8c58287458a0682abb4ae256c5c79baa1a43e6a254a57c5dd09333a38c14dd1f34a8f5840675dd7e48f7d1e22679699cd60217e13b42c4a1ff0cff3d2d755b488d3e3dce6ed3342221830c4fc2aaa37022983dc68801c0e4ba03ca2d4e6db6ffba5aee2d9279860e4abc64b71910aca64b336ae7afc89051a64fd3
8fb96cd2875109f555a8357ad44f5b62c2c7712966b8d75d43e5ecf82fe4f4d425e1f9875701df578874e834b717355aaea81b9b8e2a356041c861d2946ec4b02269108039e8230268acd8e69c5c9d327435567d927302ed110ba023d2135aafe6921f62f8f32eee848d3f34f75c588f37bc80184acd15d7ae67314162d90012e74c85e627c433
6c06f67fcc9b079782cd6d9dd1cf8f3e64a20e0dcaa82f8a66babcdc4110f26b05ad8c3ed909d5c97fe3f438662acc2fc0ae6c4bc8c12ec0618b7241166ffd70b45251efaa977830b95ce8b2127b16a94f2245f29ff768c9fea7c7149d89ff9afc48bbbc10aa1df7f0cc4621f06ac54fe2a91a6a5c9d3ede17ba7b46baf95d6983e265c252656f
0c731e33351fcca05a39f706753f19fc7006e1063261e565e44f5a03e2f083c3122837024ef58a6362647672df47135a83a705c5207e3787254d41ec5c15f8abc2233656a3ffb87a949467ae536182427cff9147e41a3dfc5f40aa4a14a76ee52b875bb8f81de488a69597af6ce45b60b26fe1751980ebcd1734ca7d19297ebc678ab641012c46
a6c769168f82212db0e6dc1811fe202661c3df7170303a48e6423a0d967501eb2719dcff057b0fa9d5ea24c636cb384c3b4642e596bc1e6c31fdd68222512810aef2e0a845b5fee48a3ac32c8ab0be1dd78703d9846548876eb0c64161b40b35e5a28aa8c852182c6147c5788867018d54be25a61c6c7e1e99e964b17dd1a0ad457e4cb8b733aa
9eec137defad81cea82c3e75c7ddd77355acbcb2042d4b6c4c5d9cb15ee0587ba8f294f795f42c52ff91b70eee341671b0210ca4e44896a57555c9f4800800cbba76464ee131a023913e7cfc70b2852f3ea48f152ebe7c430b202775b71e48a1ee65ce8f894643c4d70442d88d72493e7f4237959ff132b743a1dae4d9d4ff790164641025e36a
be5594a3d09b10ca4dbae509ea60730ba45f717e42c2ab9e72c716c3fc5e580373442e4e645c44aa2a4c757da54bb3880733e2df88c55991023d923c436e75d29156411da2d81d2284b098a278673095f321873c782fc0194aabc016e7c7089ae3aa69d3a583702efd8d201abb5114a4049a976e2a4840583a1c3499cdcda215fe8b87816e8da2
d861adf4e05c9fca159be878f647d084e238fd137b4ae59bf8a0ce4627d6fe64c3f3d70abb377344ab123078f7bfb856e1
###### open 2 1
###### open 2 ff0000006510bd0b6bf83fd330d99d30dc01853853b1196b38fde3b18db5ba619bbb9ec0b6f53c97022b3ce80cdab1521cffa18ab997de4c8efc9d579c8a884a90df4edcc37c26f0793544628001
     */


    byte[] b = Hex.dehex("3268ec613883dd587bca2e28e637329a8534d3e7c52b592e891b3bfdd10504887597c6a063ed5f5d27e5eca016062029f4955e6d67c22617916c0dbe458e2db890c2f56bfa1ae00e926bb0f15f9b705dac750c1f2616e0aa13c1290a10c466ca262c43c718ae7686cddce744089fa67a682fb46b048a67a50b58bb9925762c7c" +
            "5b7932dc293e30276d88d4be8c58287458a0682abb4ae256c5c79baa1a43e6a254a57c5dd09333a38c14dd1f34a8f5840675dd7e48f7d1e22679699cd60217e13b42c4a1ff0cff3d2d755b488d3e3dce6ed3342221830c4fc2aaa37022983dc68801c0e4ba03ca2d4e6db6ffba5aee2d9279860e4abc64b71910aca64b336ae7afc89051a64fd3" +
            "8fb96cd2875109f555a8357ad44f5b62c2c7712966b8d75d43e5ecf82fe4f4d425e1f9875701df578874e834b717355aaea81b9b8e2a356041c861d2946ec4b02269108039e8230268acd8e69c5c9d327435567d927302ed110ba023d2135aafe6921f62f8f32eee848d3f34f75c588f37bc80184acd15d7ae67314162d90012e74c85e627c433" +
            "6c06f67fcc9b079782cd6d9dd1cf8f3e64a20e0dcaa82f8a66babcdc4110f26b05ad8c3ed909d5c97fe3f438662acc2fc0ae6c4bc8c12ec0618b7241166ffd70b45251efaa977830b95ce8b2127b16a94f2245f29ff768c9fea7c7149d89ff9afc48bbbc10aa1df7f0cc4621f06ac54fe2a91a6a5c9d3ede17ba7b46baf95d6983e265c252656f" +
            "0c731e33351fcca05a39f706753f19fc7006e1063261e565e44f5a03e2f083c3122837024ef58a6362647672df47135a83a705c5207e3787254d41ec5c15f8abc2233656a3ffb87a949467ae536182427cff9147e41a3dfc5f40aa4a14a76ee52b875bb8f81de488a69597af6ce45b60b26fe1751980ebcd1734ca7d19297ebc678ab641012c46" +
            "a6c769168f82212db0e6dc1811fe202661c3df7170303a48e6423a0d967501eb2719dcff057b0fa9d5ea24c636cb384c3b4642e596bc1e6c31fdd68222512810aef2e0a845b5fee48a3ac32c8ab0be1dd78703d9846548876eb0c64161b40b35e5a28aa8c852182c6147c5788867018d54be25a61c6c7e1e99e964b17dd1a0ad457e4cb8b733aa" +
            "9eec137defad81cea82c3e75c7ddd77355acbcb2042d4b6c4c5d9cb15ee0587ba8f294f795f42c52ff91b70eee341671b0210ca4e44896a57555c9f4800800cbba76464ee131a023913e7cfc70b2852f3ea48f152ebe7c430b202775b71e48a1ee65ce8f894643c4d70442d88d72493e7f4237959ff132b743a1dae4d9d4ff790164641025e36a" +
            "be5594a3d09b10ca4dbae509ea60730ba45f717e42c2ab9e72c716c3fc5e580373442e4e645c44aa2a4c757da54bb3880733e2df88c55991023d923c436e75d29156411da2d81d2284b098a278673095f321873c782fc0194aabc016e7c7089ae3aa69d3a583702efd8d201abb5114a4049a976e2a4840583a1c3499cdcda215fe8b87816e8da2" +
            "d861adf4e05c9fca159be878f647d084e238fd137b4ae59bf8a0ce4627d6fe64c3f3d70abb377344ab123078f7bfb856e1");

    ByteBuf bb = Unpooled.wrappedBuffer(b);

    AEAD aead = new AEAD(
            Hex.dehex("36b0e5f536d75764fa21799d9964699f"),
            Hex.dehex("b278918d0cd75fb81ecf747216d84dd3"),

            Hex.dehex("ddd3bacd933c5d8fa9d7b2cb"),
            Hex.dehex("40338d77efea4e35c5f31acd"));

    PacketNumber pn = new PacketNumber(1);

    byte[] aad = Hex.dehex("ff0000006510bd0b6bf83fd330d99d30dc01853853b1196b38fde3b18db5ba619bbb9ec0b6f53c97022b3ce80cdab1521cffa18ab997de4c8efc9d579c8a884a90df4edcc37c26f0793544628001");

    UnprotectedPayload up = UnprotectedPayload.parse(bb, bb.readableBytes(), aead, pn, aad);

    System.out.println(up);
  }
}
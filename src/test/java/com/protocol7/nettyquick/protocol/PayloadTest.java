package com.protocol7.nettyquick.protocol;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.TestUtil;
import com.protocol7.nettyquick.protocol.frames.*;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.TestAEAD;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PayloadTest {

  private final AEAD aead = TestAEAD.create();
  private final PacketNumber pn = new PacketNumber(1);
  private final byte[] aad = new byte[12];

  @Test
  public void roundtrip() {
    Payload payload = new Payload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    payload.write(bb, aead, pn, aad);

    Payload parsed = Payload.parse(bb, payload.getLength(), aead, pn, aad);

    assertEquals(payload, parsed);
  }

  @Test
  public void write() {
    Payload payload = new Payload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    payload.write(bb, aead, pn, aad);

    TestUtil.assertBuffer("e7ece5d6d0adf7f6bf189050d8fcace66501", bb);
  }

  @Test
  public void parse() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("e7ece5d6d0adf7f6bf189050d8fcace66501"));
    Payload parsed = Payload.parse(bb, bb.writerIndex(), aead, pn, aad);

    Payload expected = new Payload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);
    assertEquals(expected, parsed);
  }

  @Test
  public void addFrame() {
    Payload payload = new Payload(PingFrame.INSTANCE);

    Payload withAdded = payload.addFrame(PaddingFrame.INSTANCE);

    Payload expected = new Payload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);

    assertEquals(expected, withAdded);
    assertEquals(new Payload(PingFrame.INSTANCE), payload); // must not been mutated
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

    Payload up = Payload.parse(bb, bb.readableBytes(), aead, pn, aad);

    CryptoFrame cf = (CryptoFrame) up.getFrames().get(0);

    assertEquals(0, cf.getOffset());
    TestUtil.assertHex("010001060303dec0ab54ecd6ac7a528ad533aeb1a9a4e7b543b70de3de5b37c8c16663a87a892057c92cc059e24266c7fb3b8ca7e33283e48fe4cc4306ff9c56cde1f8e7dab58400021301010000bb000500050100000000000a0012001000170018001901000101010201030104000d001e001c0403050306030804080508060809080a080b040105010601020302010032001e001c0403050306030804080508060809080a080b04010501060102030201002b0003020304002d000201010033004700450017004104409d6a24b309478239cb3c3c440bfab4dca0166f4ebe54f28bac2a63cc48b9a169c9e24623f79d7e7371c3a94542fdb5fa511774ccf8751340c8365be5fbac92", cf.getCryptoData());
  }

  @Test
  public void parseKnown2() {
    byte[] b = Hex.dehex("229264c1340f2101060c82c2daacb8b54b817f4c62b555177bca5b8eba8a4fa3c1702c61713609abecfc48444b611fa6f0eb76313218126bdf3c670c636aa384ce068a3af07868ba9271384e7e99153501a34fd679a5942dea92d5b782c75b4c9ded3c57428fdac8f4059c59ef0f618c374c07aeab35234038c46bb7a01607c5f55c23c9598babb56917997740393ce000da5790c83cccc382a423f863164237cfad46b6b1cabcac6ca151be01204040a09051285d");

    ByteBuf bb = Unpooled.wrappedBuffer(b);

    AEAD aead = new AEAD(
            Hex.dehex("f60338316756154f0610de3ed442dd51"),
            Hex.dehex("7021baaf03e02999a2d47a43e0cbe06a"),

            Hex.dehex("3960fbe721e18ac688b692e7"),
            Hex.dehex("4eec7558cb603004304b1952")
    );

    PacketNumber pn = new PacketNumber(1);

    byte[] aad = Hex.dehex("ff0000006501f67973660040b58001");

    Payload up = Payload.parse(bb, bb.readableBytes(), aead, pn, aad);

    AckFrame af = (AckFrame) up.getFrames().get(0);
    assertEquals(1519, af.getAckDelay());
    assertEquals(ImmutableList.of(new AckBlock(new PacketNumber(1), new PacketNumber(1))), af.getBlocks());

    CryptoFrame cf = (CryptoFrame) up.getFrames().get(1);

    assertEquals(0, cf.getOffset());
    TestUtil.assertHex("020000970303c5b1846bacefca18971b4285ef0bfcbab790059b5ab337df2d2dfcf0162b1d60203eda7adaf3b48724a58841bb97bc0ebbbe08de28bf5c11bc5a6774c2129fca80130100004f002b000203040033004500170041047e0f705a51a2ea2b91dab6dbcfd18ac30c6ac9966eb5f52bef0060d93d3d53a495173ee03a3c008e4cb9bd021f55aeae48f100bfc974e33df82178cb52928cae", cf.getCryptoData());

  }
}
package com.protocol7.nettyquic.tls;

import org.junit.Test;

public class HashTest {

  @Test
  public void multiple256() {
    TestUtil.assertHex(
        "936a185caaa266bb9cbe981e9e05cb78cd732b0b3280eb944412bb6f8f8f07af",
        Hash.sha256("hello".getBytes(), "world".getBytes()));
  }

  @Test
  public void single256() {
    TestUtil.assertHex(
        "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
        Hash.sha256("hello".getBytes()));
  }

  @Test
  public void empty256() {
    TestUtil.assertHex(
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Hash.sha256());
  }

  @Test(expected = NullPointerException.class)
  public void null256() {
    Hash.sha256(null);
  }
}

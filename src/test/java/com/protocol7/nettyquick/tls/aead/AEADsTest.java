package com.protocol7.nettyquick.tls.aead;

import static org.junit.Assert.*;

import com.protocol7.nettyquick.EncryptionLevel;
import org.junit.Test;

public class AEADsTest {

  private final AEAD initial = TestAEAD.create();
  private final AEAD handshake = TestAEAD.create();
  private final AEAD oneRtt = TestAEAD.create();

  private final AEADs aeads = new AEADs(initial);

  @Test
  public void getInitial() {
    assertAEAD(initial, aeads.get(EncryptionLevel.Initial));
  }

  @Test
  public void getHandshake() {
    aeads.setHandshakeAead(handshake);
    assertAEAD(handshake, aeads.get(EncryptionLevel.Handshake));
  }

  @Test
  public void getOneRtt() {
    aeads.setOneRttAead(oneRtt);
    assertAEAD(oneRtt, aeads.get(EncryptionLevel.OneRtt));
  }

  @Test(expected = IllegalStateException.class)
  public void getHandshakeNotSet() {
    aeads.get(EncryptionLevel.Handshake);
  }

  @Test(expected = IllegalStateException.class)
  public void getOneRttNotSet() {
    aeads.get(EncryptionLevel.OneRtt);
  }

  @Test(expected = NullPointerException.class)
  public void getNullEncryptionLevel() {
    aeads.get(null);
  }

  @Test(expected = NullPointerException.class)
  public void setNullInitial() {
    new AEADs(null);
  }

  @Test(expected = NullPointerException.class)
  public void setNullHandshake() {
    aeads.setHandshakeAead(null);
  }

  @Test(expected = NullPointerException.class)
  public void setNullOneRtt() {
    aeads.setOneRttAead(null);
  }

  private void assertAEAD(AEAD expected, AEAD actual) {
    assertArrayEquals(expected.getMyKey(), actual.getMyKey());
  }
}

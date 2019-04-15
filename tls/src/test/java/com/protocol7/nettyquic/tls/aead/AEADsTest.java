package com.protocol7.nettyquic.tls.aead;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.protocol7.nettyquic.tls.EncryptionLevel;
import org.junit.Test;

public class AEADsTest {

  private final AEAD initial = TestAEAD.create();
  private final AEAD handshake = TestAEAD.create();
  private final AEAD oneRtt = TestAEAD.create();

  private final AEADs aeads = new AEADs(initial);

  @Test
  public void getInitial() {
    assertTrue(aeads.available(EncryptionLevel.Initial));
    assertAEAD(initial, aeads.get(EncryptionLevel.Initial));
  }

  @Test
  public void getHandshake() {
    assertFalse(aeads.available(EncryptionLevel.Handshake));
    aeads.setHandshakeAead(handshake);
    assertTrue(aeads.available(EncryptionLevel.Handshake));
    assertAEAD(handshake, aeads.get(EncryptionLevel.Handshake));
  }

  @Test
  public void getOneRtt() {
    assertFalse(aeads.available(EncryptionLevel.OneRtt));
    aeads.setOneRttAead(oneRtt);
    assertTrue(aeads.available(EncryptionLevel.OneRtt));
    assertAEAD(oneRtt, aeads.get(EncryptionLevel.OneRtt));
  }

  @Test
  public void unsetInitial() {
    aeads.unsetInitialAead();

    assertFalse(aeads.available(EncryptionLevel.Initial));

    try {
      aeads.get(EncryptionLevel.Initial);
      fail();
    } catch (IllegalStateException e) {
      // ignore
    }
  }

  @Test
  public void unsetHandshake() {
    aeads.setHandshakeAead(handshake);
    assertTrue(aeads.available(EncryptionLevel.Handshake));

    aeads.unsetHandshakeAead();

    assertFalse(aeads.available(EncryptionLevel.Handshake));

    try {
      aeads.get(EncryptionLevel.Handshake);
      fail();
    } catch (IllegalStateException e) {
      // ignore
    }
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
    assertSame(expected, actual);
  }
}

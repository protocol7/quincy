package com.protocol7.nettyquic.tls.extensions;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.tls.Group;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class SupportedGroupsTest {

  @Test
  public void roundtrip() {
    SupportedGroups supportedGroups = new SupportedGroups(Group.X25519, Group.X448);

    ByteBuf bb = Unpooled.buffer();

    supportedGroups.write(bb, true);

    SupportedGroups parsed = SupportedGroups.parse(bb);

    assertEquals(supportedGroups.getGroups(), parsed.getGroups());
  }
}

package com.protocol7.nettyquick.tls.extensions;

import com.protocol7.nettyquick.tls.Group;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
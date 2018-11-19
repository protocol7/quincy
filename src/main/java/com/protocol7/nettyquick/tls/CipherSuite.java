package com.protocol7.nettyquick.tls;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public enum CipherSuite {

    TLS_AES_128_GCM_SHA256(0x1301),
    TLS_AES_256_GCM_SHA384(0x1302),
    TLS_CHACHA20_POLY1305_SHA256(0x1303);

    private static final EnumSet<CipherSuite> ALL = EnumSet.allOf(CipherSuite.class);

    public static final List<CipherSuite> SUPPORTED = ImmutableList.of(TLS_AES_128_GCM_SHA256);

    public static List<CipherSuite> parseKnown(ByteBuf bb) {
        int len = bb.readShort() / 2;

        List<CipherSuite> css = Lists.newArrayListWithCapacity(len);
        for (int i = 0; i<len; i++) {
            fromValue(bb.readShort()).ifPresent(css::add);
        }
        return css;
    }

    public static Optional<CipherSuite> parseOne(ByteBuf bb) {
        int value = bb.readShort();
        return fromValue(value);
    }

    public static Optional<CipherSuite> fromValue(int value) {
        for (CipherSuite cs : ALL) {
            if (cs.value == value) {
                return Optional.ofNullable(cs);
            }
        }
        return Optional.empty();
    }

    public static void writeAll(ByteBuf bb, Collection<CipherSuite> css) {
        bb.writeShort(css.size() * 2);

        for (CipherSuite cs : css) {
            bb.writeShort(cs.value);
        }
    }

    private final int value;

    CipherSuite(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

package com.protocol7.nettyquick.tls.extensions;

import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;

public class SupportedVersions implements Extension {

    public static final SupportedVersions TLS13 = new SupportedVersions(new byte[]{3, 4});

    public static SupportedVersions parse(ByteBuf bb, boolean isClient) {
        if (isClient) {
            bb.readByte();
        }

        byte[] version = Bytes.asArray(bb);

        return new SupportedVersions(version);
    }

    private final byte[] version;

    private SupportedVersions(byte[] version) {
        this.version = version;
    }

    @Override
    public ExtensionType getType() {
        return ExtensionType.supported_versions;
    }

    public byte[] getVersion() {
        return version;
    }

    @Override
    public void write(ByteBuf bb, boolean isClient) {
        if (isClient) {
            bb.writeByte(version.length);
        }
        bb.writeBytes(version);
    }

    @Override
    public String toString() {
        return "SupportedVersions{" + Hex.hex(version) + '}';
    }
}

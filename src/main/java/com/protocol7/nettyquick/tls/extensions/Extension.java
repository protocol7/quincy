package com.protocol7.nettyquick.tls.extensions;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Collection;
import java.util.List;

public interface Extension {

    static List<Extension> parseAll(ByteBuf bb, boolean isClient) {
        List<Extension> extensions = Lists.newArrayList();

        while (bb.isReadable()) {
            Extension ext = Extension.parse(bb, isClient);

            extensions.add(ext);
        }
        return extensions;
    }

    static Extension parse(ByteBuf bb, boolean isClient) {

        ExtensionType type = ExtensionType.fromValue(bb.readShort());

        int len = bb.readShort();
        if (type == ExtensionType.QUIC) {
            return TransportParameters.parse(bb.readBytes(len));
        } else if (type == ExtensionType.key_share) {
            return KeyShare.parse(bb.readBytes(len), isClient);
        } else if (type == ExtensionType.supported_versions) {
            return SupportedVersions.parse(bb.readBytes(len), isClient);
        } else if (type == ExtensionType.supported_groups) {
            return SupportedGroups.parse(bb.readBytes(len));
        } else {

            byte[] data = new byte[len];
            bb.readBytes(data);

            return RawExtension.parse(type, data);
        }
    }

    static void writeAll(Collection<Extension> extensions, ByteBuf bb, boolean isClient) {
        for (Extension extension : extensions) {
            bb.writeShort(extension.getType().getValue());

            int lenPos = bb.writerIndex();
            bb.writeShort(0);

            extension.write(bb, isClient);

            bb.setShort(lenPos, bb.writerIndex() -lenPos - 2);
        }
    }

    ExtensionType getType();

    void write(ByteBuf bb, boolean isClient);

}

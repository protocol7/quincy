package com.protocol7.nettyquick.tls;

import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Collection;
import java.util.SortedMap;

public interface Extension {

    static SortedMap parseAll(ByteBuf bb) {
        SortedMap<ExtensionType, Extension> extensions = Maps.newTreeMap((o1, o2) -> o2.getValue() - o1.getValue());

        while (bb.isReadable()) {
            Extension ext = Extension.parse(bb);

            extensions.put(ext.getType(), ext);
        }
        return extensions;
    }

    static Extension parse(ByteBuf bb) {

        ExtensionType type = ExtensionType.fromValue(bb.readShort());

        int len = bb.readShort();

        if (type == ExtensionType.QUIC) {
            return TransportParameters.parse(bb.readBytes(len));
        } else {

            byte[] data = new byte[len];
            bb.readBytes(data);

            return RawExtension.parse(type, data);
        }
    }

    static void writeAll(Collection<Extension> extensions, ByteBuf bb) {
        for (Extension extension : extensions) {
            bb.writeShort(extension.getType().getValue());

            ByteBuf b = Unpooled.buffer();
            extension.write(b);
            byte[] x = new byte[b.readableBytes()];
            b.readBytes(x);
            bb.writeShort(x.length);
            bb.writeBytes(x);
        }
    }

    ExtensionType getType();

    void write(ByteBuf bb);

}

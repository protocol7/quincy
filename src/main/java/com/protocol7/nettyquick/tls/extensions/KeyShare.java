package com.protocol7.nettyquick.tls.extensions;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMap.Builder;
import com.protocol7.nettyquick.tls.Group;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

public class KeyShare implements Extension {

    public static KeyShare parse(ByteBuf bb, boolean isClient) {
        if (isClient) {
            bb.readShort();
        }

        Builder<Group, byte[]> builder = ImmutableSortedMap.naturalOrder();

        while (bb.isReadable()) {
            Optional<Group> group = Group.fromValue(bb.readShort());
            int keyLen = bb.readShort();
            byte[] key = new byte[keyLen];
            bb.readBytes(key);

            if (group.isPresent()) {
                builder.put(group.get(), key);
            }
        }

        return new KeyShare(builder.build());
    }

    public static KeyShare of(Group group, byte[] publicKey) {
        return new KeyShare(ImmutableSortedMap.of(group, publicKey));
    }

    public static KeyShare of(Group group1, byte[] key1, Group group2, byte[] key2) {
        return new KeyShare(ImmutableSortedMap.of(group1, key1, group2, key2));
    }

    private final SortedMap<Group, byte[]> keys;

    private KeyShare(SortedMap<Group, byte[]> keys) {
        this.keys = ImmutableSortedMap.copyOf(keys);
    }

    @Override
    public ExtensionType getType() {
        return ExtensionType.key_share;
    }

    public SortedMap<Group, byte[]> getKeys() {
        return keys;
    }

    public Optional<byte[]> getKey(Group group) {
        return Optional.ofNullable(keys.get(group));
    }

    @Override
    public void write(ByteBuf bb, boolean isClient) {
        int lenPos = bb.writerIndex();
        if (isClient) {
            bb.writeShort(0);
        }

        for (Map.Entry<Group, byte[]> entry : keys.entrySet()) {
            bb.writeShort(entry.getKey().getValue());
            bb.writeShort(entry.getValue().length);
            bb.writeBytes(entry.getValue());
        }

        if (isClient) {
            bb.setShort(lenPos, bb.writerIndex() - lenPos - 2);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Group, byte[]> key : keys.entrySet()) {
            sb.append('{').append(key.getKey().name()).append('=').append(Hex.hex(key.getValue())).append('}');
        }

        return "KeyShare{" + keys + '}';
    }
}

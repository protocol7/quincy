package com.protocol7.nettyquick.tls.extensions;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMap.Builder;
import com.protocol7.nettyquick.tls.Group;
import com.protocol7.nettyquick.utils.Bytes;
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
            Group group = Group.fromValue(bb.readShort());
            int keyLen = bb.readShort();
            byte[] key = new byte[keyLen];
            bb.readBytes(key);

            builder.put(group, key);
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
        ByteBuf b = Unpooled.buffer();
        for (Map.Entry<Group, byte[]> entry : keys.entrySet()) {
            b.writeShort(entry.getKey().getValue());
            b.writeShort(entry.getValue().length);
            b.writeBytes(entry.getValue());
        }

        byte[] x = Bytes.asArray(b);
        if (isClient) {
            bb.writeShort(x.length);
        }
        bb.writeBytes(x);
    }

    @Override
    public String toString() {
        return "KeyShare{" + keys + '}';
    }
}

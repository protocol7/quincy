package com.protocol7.nettyquick.tls;

public enum Group {
    X25519(0x001d),
    SECP256R1(0x0017),
    SECP384R1(0x0018);

    public static Group fromValue(int value) {
        if (value == X25519.value) {
            return X25519;
        } else if (value == SECP256R1.value) {
            return SECP256R1;
        } else if (value == SECP384R1.value) {
            return SECP384R1;
        } else {
            throw new IllegalArgumentException("Unknown group value: " + value);
        }
    }

    private int value;

    Group(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

package com.protocol7.nettyquick.tls;

import com.google.common.base.Preconditions;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class KeyExchangeKeys {

    private static final byte[] PKCS_PUBLIC_PREFIX =  Hex.dehex("302c300706032b656e0500032100");
    private static final byte[] PKCS_PRIVATE_PREFIX = Hex.dehex("302e020100300706032b656e05000420");

    public static KeyExchangeKeys generate(Group group) {
        Preconditions.checkArgument(group == Group.X25519); // TOOD implement others

        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("X25519");
            return new KeyExchangeKeys(group, keyPairGen.generateKeyPair());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final Group group;
    private final KeyPair keyPair;

    private KeyExchangeKeys(Group group, KeyPair keyPair) {
        this.group = group;
        this.keyPair = keyPair;
    }

    public byte[] getPrivateKey() {
        byte[] privateKey = keyPair.getPrivate().getEncoded();
        return Arrays.copyOfRange(privateKey, PKCS_PRIVATE_PREFIX.length, privateKey.length);
    }

    public byte[] getPublicKey() {
        byte[] publicKey = keyPair.getPublic().getEncoded();
        return Arrays.copyOfRange(publicKey, PKCS_PUBLIC_PREFIX.length, publicKey.length);
    }

    public Group getGroup() {
        return group;
    }

    public byte[] generateSharedSecret(byte[] otherPublicKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("X25519");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Bytes.concat(PKCS_PUBLIC_PREFIX, otherPublicKey));
            PublicKey pubKey = keyFactory.generatePublic(x509KeySpec);

            KeyAgreement keyAgree = KeyAgreement.getInstance("X25519");
            keyAgree.init(keyPair.getPrivate());
            keyAgree.doPhase(pubKey, true);
            return keyAgree.generateSecret();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}

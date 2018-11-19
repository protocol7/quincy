package com.protocol7.nettyquick.tls;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.*;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class KeyUtil {

    public static PublicKey getPublicKeyFromPem(String path) throws IOException, CertificateException {
        PemReader reader = new PemReader(new FileReader(path));
        PemObject pemObject = reader.readPemObject();
        byte[] content = pemObject.getContent();
        CertificateFactory fact = CertificateFactory.getInstance("X.509");

        Certificate cert = fact.generateCertificate(new ByteArrayInputStream(content));
        return cert.getPublicKey();
    }

    public static X509Certificate getCertFromCrt(String path) throws FileNotFoundException, CertificateException {
        FileInputStream fin = new FileInputStream(path);
        CertificateFactory f = CertificateFactory.getInstance("X.509");
        return (X509Certificate)f.generateCertificate(fin);
    }

    public static RSAPrivateKey getPrivateKeyFromPem(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PemReader reader = new PemReader(new FileReader(path));
        PemObject pemObject = reader.readPemObject();
        byte[] content = pemObject.getContent();
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(privKeySpec);
    }
}

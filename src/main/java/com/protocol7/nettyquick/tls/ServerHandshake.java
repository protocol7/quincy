package com.protocol7.nettyquick.tls;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.tls.extensions.Extension;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;

import java.util.List;

public class ServerHandshake {

    public static ServerHandshake parse(ByteBuf bb) {
        // EE
        int eeType = bb.readByte();
        if (eeType != 0x08) {
            throw new IllegalArgumentException("Invalid EE type: " + eeType);
        }

        int eeMsgLen = Bytes.read24(bb);
        int extLen = bb.readShort();

        ByteBuf ext = bb.readBytes(extLen);
        List<Extension> extensions = Extension.parseAll(ext, false);

        // server cert
        int serverCertType = bb.readByte();
        if (serverCertType != 0x0b) {
            throw new IllegalArgumentException("Invalid server cert type: " + serverCertType);
        }

        int scMsgLen = Bytes.read24(bb);
        int requestContextLen = bb.readByte();

        int certsLen = Bytes.read24(bb);

        ByteBuf certBB = bb.readBytes(certsLen);

        List<byte[]> serverCertificates = Lists.newArrayList();

        while (certBB.isReadable()) {
            int certLen = Bytes.read24(certBB);

            byte[] cert = new byte[certLen];
            certBB.readBytes(cert);

            serverCertificates.add(cert);

            int certExtLen = certBB.readShort();
            byte[] certExt = new byte[certExtLen];
            certBB.readBytes(certExt);
        }

        // server cert verify
        int serverCertVerifyType = bb.readByte();
        if (serverCertVerifyType != 0x0f) {
            throw new IllegalArgumentException("Invalid server cert verify type: " + serverCertVerifyType);
        }

        int scvMsgLen = Bytes.read24(bb);

        int signType = bb.readShort();
        int signLen = bb.readShort();

        byte[] sign = new byte[signLen];
        bb.readBytes(sign);

        // server handshake finished
        int finType = bb.readByte();
        if (finType != 0x14) {
            throw new IllegalArgumentException("Invalid fin type: " + finType);
        }

        int finLen = Bytes.read24(bb);

        byte[] verifyData = new byte[finLen];
        bb.readBytes(verifyData);

        return new ServerHandshake(extensions,
                serverCertificates,
                signType,
                sign,
                verifyData);
    }

    private final List<Extension> extensions;
    private final List<byte[]> serverCertificates;
    private final int verificationSignatureType;
    private final byte[] verificationSignature;
    private final byte[] verificationData;

    public ServerHandshake(List<Extension> extensions,
                           List<byte[]> serverCertificates,
                           int verificationSignatureType,
                           byte[] verificationSignature,
                           byte[] verificationData) {
        this.extensions = extensions;
        this.serverCertificates = serverCertificates;
        this.verificationSignatureType = verificationSignatureType;
        this.verificationSignature = verificationSignature;
        this.verificationData = verificationData;
    }

    public List<Extension> getExtensions() {
        return extensions;
    }

    public List<byte[]> getServerCertificates() {
        return serverCertificates;
    }

    public int getVerificationSignatureType() {
        return verificationSignatureType;
    }

    public byte[] getVerificationSignature() {
        return verificationSignature;
    }

    public byte[] getVerificationData() {
        return verificationData;
    }

    @Override
    public String toString() {
        return "ServerHandshake{" +
                "extensions=" + extensions +
                ", serverCertificates=" + serverCertificates +
                ", verificationSignatureType=" + verificationSignatureType +
                ", verificationSignature=" + Hex.hex(verificationSignature) +
                ", verificationData=" + Hex.hex(verificationData) +
                '}';
    }
}

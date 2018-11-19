package com.protocol7.nettyquick.tls.messages;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.protocol7.nettyquick.tls.extensions.Extension;
import com.protocol7.nettyquick.tls.extensions.TransportParameters;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;

import static com.protocol7.nettyquick.utils.Bytes.write24;

public class ServerHandshake {

    public static ServerHandshake parse(ByteBuf bb) {

        return new ServerHandshake(
                EncryptedExtensions.parse(bb),
                ServerCertificate.parse(bb),
                ServerCertificateVerify.parse(bb),
                ServerHandshakeFinished.parse(bb));
    }

    private final EncryptedExtensions encryptedExtensions;
    private final ServerCertificate serverCertificate;
    private final ServerCertificateVerify serverCertificateVerify;
    private final ServerHandshakeFinished serverHandshakeFinished;

    public ServerHandshake(EncryptedExtensions encryptedExtensions,
                           ServerCertificate serverCertificate,
                           ServerCertificateVerify serverCertificateVerify,
                           ServerHandshakeFinished serverHandshakeFinished) {
        this.encryptedExtensions = encryptedExtensions;
        this.serverCertificate = serverCertificate;
        this.serverCertificateVerify = serverCertificateVerify;
        this.serverHandshakeFinished = serverHandshakeFinished;
    }

    public EncryptedExtensions getEncryptedExtensions() {
        return encryptedExtensions;
    }

    public ServerCertificate getServerCertificate() {
        return serverCertificate;
    }

    public ServerCertificateVerify getServerCertificateVerify() {
        return serverCertificateVerify;
    }

    public ServerHandshakeFinished getServerHandshakeFinished() {
        return serverHandshakeFinished;
    }

    public static class EncryptedExtensions {

        public static EncryptedExtensions defaults() {
            return new EncryptedExtensions(ImmutableList.of(TransportParameters.defaults()));
        }

        public static EncryptedExtensions parse(ByteBuf bb) {
            // EE
            int eeType = bb.readByte();
            if (eeType != 0x08) {
                throw new IllegalArgumentException("Invalid EE type: " + eeType);
            }

            int eeMsgLen = Bytes.read24(bb);
            int extLen = bb.readShort();

            ByteBuf ext = bb.readBytes(extLen);
            List<Extension> extensions = Extension.parseAll(ext, false);

            return new EncryptedExtensions(extensions);
        }

        private final List<Extension> extensions;

        public EncryptedExtensions(List<Extension> extensions) {
            this.extensions = extensions;
        }

        public EncryptedExtensions(Extension... extensions) {
            this.extensions = Arrays.asList(extensions);
        }

        public List<Extension> getExtensions() {
            return extensions;
        }

        public void write(ByteBuf bb) {
            // EE
            bb.writeByte(0x08);
            int eeMsgLenPos = bb.writerIndex();
            write24(bb, 0);

            int extLenPos = bb.writerIndex();
            bb.writeShort(0);

            Extension.writeAll(extensions, bb, false);

            write24(bb, bb.writerIndex() - eeMsgLenPos - 3, eeMsgLenPos);
            bb.setShort(extLenPos, bb.writerIndex() - extLenPos - 2);
        }
    }

    public static class ServerCertificate {

        public static ServerCertificate parse(ByteBuf bb) {
            // server cert
            int serverCertType = bb.readByte();
            if (serverCertType != 0x0b) {
                throw new IllegalArgumentException("Invalid server cert type: " + serverCertType);
            }

            int scMsgLen = Bytes.read24(bb);
            int requestContextLen = bb.readByte();

            byte[] requestContext = new byte[requestContextLen];
            bb.readBytes(requestContext);

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

            return new ServerCertificate(requestContext, serverCertificates);
        }

        private final byte[] requestContext;
        private final List<byte[]> serverCertificates;

        public ServerCertificate(byte[] requestContext, List<byte[]> serverCertificates) {
            this.requestContext = requestContext;
            this.serverCertificates = serverCertificates;
        }

        public ServerCertificate(byte[]... serverCertificates) {
            this(new byte[0], Arrays.asList(serverCertificates));
        }

        public byte[] getRequestContext() {
            return requestContext;
        }

        public List<byte[]> getServerCertificates() {
            return serverCertificates;
        }

        public void write(ByteBuf bb) {
            // server cert
            bb.writeByte(0x0b);
            int scMsgLenPos = bb.writerIndex();
            write24(bb, 0);

            bb.writeByte(requestContext.length);
            bb.writeBytes(requestContext);

            int certsLenPos = bb.writerIndex();
            write24(bb, 0);

            for (byte[] cert : serverCertificates) {
                write24(bb, cert.length);
                bb.writeBytes(cert);

                bb.writeShort(0); // TODO add support for cert extensions
            }

            write24(bb, bb.writerIndex() - scMsgLenPos - 3, scMsgLenPos);
            write24(bb, bb.writerIndex() - certsLenPos - 3, certsLenPos);
        }
    }

    public static class ServerCertificateVerify {

        public static ServerCertificateVerify parse(ByteBuf bb) {
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

            return new ServerCertificateVerify(signType, sign);
        }

        private final int verificationSignatureType;
        private final byte[] verificationSignature;

        public ServerCertificateVerify(int verificationSignatureType, byte[] verificationSignature) {
            this.verificationSignatureType = verificationSignatureType;
            this.verificationSignature = verificationSignature;
        }

        public int getVerificationSignatureType() {
            return verificationSignatureType;
        }

        public byte[] getVerificationSignature() {
            return verificationSignature;
        }

        public void write(ByteBuf bb) {
            // server cert verify
            bb.writeByte(0x0f);

            int scvMsgLenPos = bb.writerIndex();
            write24(bb, 0);

            bb.writeShort(verificationSignatureType);
            bb.writeShort(verificationSignature.length);
            bb.writeBytes(verificationSignature);

            write24(bb, bb.writerIndex() - scvMsgLenPos - 3, scvMsgLenPos);
        }
    }

    public static class ServerHandshakeFinished {

        public static ServerHandshakeFinished parse(ByteBuf bb) {
            // server handshake finished
            int finType = bb.readByte();
            if (finType != 0x14) {
                throw new IllegalArgumentException("Invalid fin type: " + finType);
            }

            int finLen = Bytes.read24(bb);

            byte[] verifyData = new byte[finLen];
            bb.readBytes(verifyData);

            return new ServerHandshakeFinished(verifyData);
        }

        private final byte[] verificationData;

        public ServerHandshakeFinished(byte[] verificationData) {
            this.verificationData = verificationData;
        }

        public byte[] getVerificationData() {
            return verificationData;
        }

        public void write(ByteBuf bb) {
            // server handshake finished
            bb.writeByte(0x14);
            write24(bb, verificationData.length);
            bb.writeBytes(verificationData);
        }
    }
}

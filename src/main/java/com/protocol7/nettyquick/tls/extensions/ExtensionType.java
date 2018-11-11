package com.protocol7.nettyquick.tls.extensions;

import com.google.common.collect.Sets;

import java.util.Objects;
import java.util.Set;

public class ExtensionType {

    public static final ExtensionType server_name = new ExtensionType("server_name", 0);
    public static final ExtensionType max_fragment_length = new ExtensionType("max_fragment_length", 1);
    public static final ExtensionType client_certificate_url = new ExtensionType("client_certificate_url", 2);
    public static final ExtensionType trusted_ca_keys = new ExtensionType("trusted_ca_keys", 3);
    public static final ExtensionType truncated_hmac = new ExtensionType("truncated_hmac", 4);
    public static final ExtensionType status_request = new ExtensionType("status_request", 5);
    public static final ExtensionType user_mapping = new ExtensionType("user_mapping", 6);
    public static final ExtensionType client_authz = new ExtensionType("client_authz", 7);
    public static final ExtensionType server_authz = new ExtensionType("server_authz", 8);
    public static final ExtensionType cert_type = new ExtensionType("cert_type", 9);
    public static final ExtensionType supported_groups = new ExtensionType("supported_groups", 10);
    public static final ExtensionType ec_point_formats = new ExtensionType("ec_point_formats", 11);
    public static final ExtensionType srp = new ExtensionType("srp", 12);
    public static final ExtensionType signature_algorithms = new ExtensionType("signature_algorithms", 13);
    public static final ExtensionType use_srtp = new ExtensionType("use_srtp", 14);
    public static final ExtensionType heartbeat = new ExtensionType("heartbeat", 15);
    public static final ExtensionType application_layer_protocol_negotiation = new ExtensionType("application_layer_protocol_negotiation", 16);
    public static final ExtensionType status_request_v2 = new ExtensionType("status_request_v2", 17);
    public static final ExtensionType signed_certificate_timestamp = new ExtensionType("signed_certificate_timestamp", 18);
    public static final ExtensionType client_certificate_type = new ExtensionType("client_certificate_type", 19);
    public static final ExtensionType server_certificate_type = new ExtensionType("server_certificate_type", 20);
    public static final ExtensionType padding = new ExtensionType("padding", 21);
    public static final ExtensionType encrypt_then_mac = new ExtensionType("encrypt_then_mac", 22);
    public static final ExtensionType extended_master_secret = new ExtensionType("extended_master_secret", 23);
    public static final ExtensionType token_binding = new ExtensionType("token_binding", 24);
    public static final ExtensionType cached_info = new ExtensionType("cached_info", 25);
    public static final ExtensionType tls_lts = new ExtensionType("tls_lts", 26);
    public static final ExtensionType compress_certificate = new ExtensionType("compress_certificate", 27);
    public static final ExtensionType record_size_limit = new ExtensionType("record_size_limit", 28);
    public static final ExtensionType pwd_protect = new ExtensionType("pwd_protect", 29);
    public static final ExtensionType pwd_clear = new ExtensionType("pwd_clear", 30);
    public static final ExtensionType password_salt = new ExtensionType("password_salt", 31);
    public static final ExtensionType session_ticket = new ExtensionType("session_ticket", 35);
    public static final ExtensionType pre_shared_key = new ExtensionType("pre_shared_key", 41);
    public static final ExtensionType early_data = new ExtensionType("early_data", 42);
    public static final ExtensionType supported_versions = new ExtensionType("supported_versions", 43);
    public static final ExtensionType cookie = new ExtensionType("cookie", 44);
    public static final ExtensionType psk_key_exchange_modes = new ExtensionType("psk_key_exchange_modes", 45);
    public static final ExtensionType certificate_authorities = new ExtensionType("certificate_authorities", 47);
    public static final ExtensionType oid_filters = new ExtensionType("oid_filters", 48);
    public static final ExtensionType post_handshake_auth = new ExtensionType("post_handshake_auth", 49);
    public static final ExtensionType signature_algorithms_cert = new ExtensionType("signature_algorithms_cert", 50);
    public static final ExtensionType key_share = new ExtensionType("key_share", 51);
    public static final ExtensionType renegotiation_info = new ExtensionType("renegotiation_info", 65281);

    public static final ExtensionType QUIC = new ExtensionType("QUIC", 4085);

    private static final Set<ExtensionType> all = Sets.newHashSet(
            server_name, max_fragment_length, client_certificate_url, trusted_ca_keys, truncated_hmac, status_request, user_mapping,
            client_authz, server_authz, cert_type, supported_groups, ec_point_formats, srp, signature_algorithms, use_srtp,
            heartbeat, application_layer_protocol_negotiation, status_request_v2, signed_certificate_timestamp, client_certificate_type,
            server_certificate_type, padding, encrypt_then_mac, extended_master_secret, token_binding, cached_info, tls_lts,
            compress_certificate, record_size_limit, pwd_protect, pwd_clear, password_salt, session_ticket, pre_shared_key,
            early_data, supported_versions, cookie, psk_key_exchange_modes, certificate_authorities, oid_filters,
            post_handshake_auth, signature_algorithms_cert, key_share, renegotiation_info, QUIC);


    // TODO optimize
    public static ExtensionType fromValue(int value) {
        for (ExtensionType type : all) {
            if (type.value == value) {
                return type;
            }
        }
        return new ExtensionType("Unknown", value);
    }

    private final String name;
    private final int value;

    ExtensionType(final String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtensionType that = (ExtensionType) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return name + '(' + value + ')';
    }
}

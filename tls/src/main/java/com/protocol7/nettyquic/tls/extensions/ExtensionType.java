package com.protocol7.nettyquic.tls.extensions;

import com.google.common.collect.Sets;
import java.util.Objects;
import java.util.Set;

public class ExtensionType {

  public static final ExtensionType SERVER_NAME = of("server_name", 0);
  public static final ExtensionType MAX_FRAGMENT_LENGTH = of("max_fragment_length", 1);
  public static final ExtensionType CLIENT_CERTIFICATE_URL = of("client_certificate_url", 2);
  public static final ExtensionType TRUSTED_CA_KEYS = of("trusted_ca_keys", 3);
  public static final ExtensionType TRUNCATED_HMAC = of("truncated_hmac", 4);
  public static final ExtensionType STATUS_REQUEST = of("status_request", 5);
  public static final ExtensionType USER_MAPPING = of("user_mapping", 6);
  public static final ExtensionType CLIENT_AUTHZ = of("client_authz", 7);
  public static final ExtensionType SERVER_AUTHZ = of("server_authz", 8);
  public static final ExtensionType CERT_TYPE = of("cert_type", 9);
  public static final ExtensionType SUPPORTED_GROUPS = of("supported_groups", 10);
  public static final ExtensionType EC_POINT_FORMATS = of("ec_point_formats", 11);
  public static final ExtensionType SRP = of("srp", 12);
  public static final ExtensionType SIGNATURE_ALGORITHMS = of("signature_algorithms", 13);
  public static final ExtensionType USE_SRTP = of("use_srtp", 14);
  public static final ExtensionType HEARTBEAT = of("heartbeat", 15);
  public static final ExtensionType APPLICATION_LAYER_PROTOCOL_NEGOTIATION =
      of("application_layer_protocol_negotiation", 16);
  public static final ExtensionType STATUS_REQUEST_V_2 = of("status_request_v2", 17);
  public static final ExtensionType SIGNED_CERTIFICATE_TIMESTAMP =
      of("signed_certificate_timestamp", 18);
  public static final ExtensionType CLIENT_CERTIFICATE_TYPE = of("client_certificate_type", 19);
  public static final ExtensionType SERVER_CERTIFICATE_TYPE = of("server_certificate_type", 20);
  public static final ExtensionType PADDING = of("padding", 21);
  public static final ExtensionType ENCRYPT_THEN_MAC = of("encrypt_then_mac", 22);
  public static final ExtensionType EXTENDED_MASTER_SECRET = of("extended_master_secret", 23);
  public static final ExtensionType TOKEN_BINDING = of("token_binding", 24);
  public static final ExtensionType CACHED_INFO = of("cached_info", 25);
  public static final ExtensionType TLS_LTS = of("tls_lts", 26);
  public static final ExtensionType COMPRESS_CERTIFICATE = of("compress_certificate", 27);
  public static final ExtensionType RECORD_SIZE_LIMIT = of("record_size_limit", 28);
  public static final ExtensionType PWD_PROTECT = of("pwd_protect", 29);
  public static final ExtensionType PWD_CLEAR = of("pwd_clear", 30);
  public static final ExtensionType PASSWORD_SALT = of("password_salt", 31);
  public static final ExtensionType SESSION_TICKET = of("session_ticket", 35);
  public static final ExtensionType PRE_SHARED_KEY = of("pre_shared_key", 41);
  public static final ExtensionType EARLY_DATA = of("early_data", 42);
  public static final ExtensionType SUPPORTED_VERSIONS = of("supported_versions", 43);
  public static final ExtensionType COOKIE = of("cookie", 44);
  public static final ExtensionType PSK_KEY_EXCHANGE_MODES = of("psk_key_exchange_modes", 45);
  public static final ExtensionType CERTIFICATE_AUTHORITIES = of("certificate_authorities", 47);
  public static final ExtensionType OID_FILTERS = of("oid_filters", 48);
  public static final ExtensionType POST_HANDSHAKE_AUTH = of("post_handshake_auth", 49);
  public static final ExtensionType SIGNATURE_ALGORITHMS_CERT = of("signature_algorithms_cert", 50);
  public static final ExtensionType KEY_SHARE = of("key_share", 51);
  public static final ExtensionType RENEGOTIATION_INFO = of("renegotiation_info", 65281);

  public static final ExtensionType QUIC = of("QUIC", 0xffa5);

  private static ExtensionType of(final String name, int value) {
    return new ExtensionType(name, value);
  }

  private static final Set<ExtensionType> ALL =
      Sets.newHashSet(
          SERVER_NAME,
          MAX_FRAGMENT_LENGTH,
          CLIENT_CERTIFICATE_URL,
          TRUSTED_CA_KEYS,
          TRUNCATED_HMAC,
          STATUS_REQUEST,
          USER_MAPPING,
          CLIENT_AUTHZ,
          SERVER_AUTHZ,
          CERT_TYPE,
          SUPPORTED_GROUPS,
          EC_POINT_FORMATS,
          SRP,
          SIGNATURE_ALGORITHMS,
          USE_SRTP,
          HEARTBEAT,
          APPLICATION_LAYER_PROTOCOL_NEGOTIATION,
          STATUS_REQUEST_V_2,
          SIGNED_CERTIFICATE_TIMESTAMP,
          CLIENT_CERTIFICATE_TYPE,
          SERVER_CERTIFICATE_TYPE,
          PADDING,
          ENCRYPT_THEN_MAC,
          EXTENDED_MASTER_SECRET,
          TOKEN_BINDING,
          CACHED_INFO,
          TLS_LTS,
          COMPRESS_CERTIFICATE,
          RECORD_SIZE_LIMIT,
          PWD_PROTECT,
          PWD_CLEAR,
          PASSWORD_SALT,
          SESSION_TICKET,
          PRE_SHARED_KEY,
          EARLY_DATA,
          SUPPORTED_VERSIONS,
          COOKIE,
          PSK_KEY_EXCHANGE_MODES,
          CERTIFICATE_AUTHORITIES,
          OID_FILTERS,
          POST_HANDSHAKE_AUTH,
          SIGNATURE_ALGORITHMS_CERT,
          KEY_SHARE,
          RENEGOTIATION_INFO,
          QUIC);

  // TODO optimize
  public static ExtensionType fromValue(int value) {
    for (ExtensionType type : ALL) {
      if (type.value == value) {
        return type;
      }
    }
    return new ExtensionType("Unknown", value);
  }

  private final String name;
  private final int value;

  private ExtensionType(final String name, int value) {
    this.name = name;
    this.value = value;
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

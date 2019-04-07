package com.protocol7.nettyquic.tls.extensions;

import com.google.common.collect.Sets;
import java.util.Objects;
import java.util.Set;

public class ExtensionType {

  public static final ExtensionType SERVER_NAME = new ExtensionType("server_name", 0);
  public static final ExtensionType MAX_FRAGMENT_LENGTH =
      new ExtensionType("max_fragment_length", 1);
  public static final ExtensionType CLIENT_CERTIFICATE_URL =
      new ExtensionType("client_certificate_url", 2);
  public static final ExtensionType TRUSTED_CA_KEYS = new ExtensionType("trusted_ca_keys", 3);
  public static final ExtensionType TRUNCATED_HMAC = new ExtensionType("truncated_hmac", 4);
  public static final ExtensionType STATUS_REQUEST = new ExtensionType("status_request", 5);
  public static final ExtensionType USER_MAPPING = new ExtensionType("user_mapping", 6);
  public static final ExtensionType CLIENT_AUTHZ = new ExtensionType("client_authz", 7);
  public static final ExtensionType SERVER_AUTHZ = new ExtensionType("server_authz", 8);
  public static final ExtensionType CERT_TYPE = new ExtensionType("cert_type", 9);
  public static final ExtensionType SUPPORTED_GROUPS = new ExtensionType("supported_groups", 10);
  public static final ExtensionType EC_POINT_FORMATS = new ExtensionType("ec_point_formats", 11);
  public static final ExtensionType SRP = new ExtensionType("srp", 12);
  public static final ExtensionType SIGNATURE_ALGORITHMS =
      new ExtensionType("signature_algorithms", 13);
  public static final ExtensionType USE_SRTP = new ExtensionType("use_srtp", 14);
  public static final ExtensionType HEARTBEAT = new ExtensionType("heartbeat", 15);
  public static final ExtensionType APPLICATION_LAYER_PROTOCOL_NEGOTIATION =
      new ExtensionType("application_layer_protocol_negotiation", 16);
  public static final ExtensionType STATUS_REQUEST_V_2 = new ExtensionType("status_request_v2", 17);
  public static final ExtensionType SIGNED_CERTIFICATE_TIMESTAMP =
      new ExtensionType("signed_certificate_timestamp", 18);
  public static final ExtensionType CLIENT_CERTIFICATE_TYPE =
      new ExtensionType("client_certificate_type", 19);
  public static final ExtensionType SERVER_CERTIFICATE_TYPE =
      new ExtensionType("server_certificate_type", 20);
  public static final ExtensionType PADDING = new ExtensionType("padding", 21);
  public static final ExtensionType ENCRYPT_THEN_MAC = new ExtensionType("encrypt_then_mac", 22);
  public static final ExtensionType EXTENDED_MASTER_SECRET =
      new ExtensionType("extended_master_secret", 23);
  public static final ExtensionType TOKEN_BINDING = new ExtensionType("token_binding", 24);
  public static final ExtensionType CACHED_INFO = new ExtensionType("cached_info", 25);
  public static final ExtensionType TLS_LTS = new ExtensionType("tls_lts", 26);
  public static final ExtensionType COMPRESS_CERTIFICATE =
      new ExtensionType("compress_certificate", 27);
  public static final ExtensionType RECORD_SIZE_LIMIT = new ExtensionType("record_size_limit", 28);
  public static final ExtensionType PWD_PROTECT = new ExtensionType("pwd_protect", 29);
  public static final ExtensionType PWD_CLEAR = new ExtensionType("pwd_clear", 30);
  public static final ExtensionType PASSWORD_SALT = new ExtensionType("password_salt", 31);
  public static final ExtensionType SESSION_TICKET = new ExtensionType("session_ticket", 35);
  public static final ExtensionType PRE_SHARED_KEY = new ExtensionType("pre_shared_key", 41);
  public static final ExtensionType EARLY_DATA = new ExtensionType("early_data", 42);
  public static final ExtensionType SUPPORTED_VERSIONS =
      new ExtensionType("supported_versions", 43);
  public static final ExtensionType COOKIE = new ExtensionType("cookie", 44);
  public static final ExtensionType PSK_KEY_EXCHANGE_MODES =
      new ExtensionType("psk_key_exchange_modes", 45);
  public static final ExtensionType CERTIFICATE_AUTHORITIES =
      new ExtensionType("certificate_authorities", 47);
  public static final ExtensionType OID_FILTERS = new ExtensionType("oid_filters", 48);
  public static final ExtensionType POST_HANDSHAKE_AUTH =
      new ExtensionType("post_handshake_auth", 49);
  public static final ExtensionType SIGNATURE_ALGORITHMS_CERT =
      new ExtensionType("signature_algorithms_cert", 50);
  public static final ExtensionType KEY_SHARE = new ExtensionType("key_share", 51);
  public static final ExtensionType RENEGOTIATION_INFO =
      new ExtensionType("renegotiation_info", 65281);

  public static final ExtensionType QUIC = new ExtensionType("QUIC", 0xffa5);

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

  ExtensionType(final String name, int value) {
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

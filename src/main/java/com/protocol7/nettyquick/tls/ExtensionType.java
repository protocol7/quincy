package com.protocol7.nettyquick.tls;

import java.util.EnumSet;

public enum ExtensionType {

    server_name(0),
    max_fragment_length(1),
	client_certificate_url(2),
	trusted_ca_keys(3),
	truncated_hmac(4),
	status_request(5),
	user_mapping(6),
	client_authz(7),
	server_authz(8),
	cert_type(9),
	supported_groups(10),
	ec_point_formats(11),
	srp(12),
	signature_algorithms(13),
	use_srtp(14),
	heartbeat(15),
	application_layer_protocol_negotiation(16),
	status_request_v2(17),
	signed_certificate_timestamp(18),
	client_certificate_type(19),
	server_certificate_type(20),
	padding(21),
	encrypt_then_mac(22),
	extended_master_secret(23),
	token_binding(24),
	cached_info(25),
	tls_lts(26),
	compress_certificate(27),
	record_size_limit(28),
	pwd_protect(29),
	pwd_clear(30),
	password_salt(31),
	session_ticket(35),
	pre_shared_key(41),
	early_data(42),
	supported_versions(43),
	cookie(44),
	psk_key_exchange_modes(45),
	Unassigned(46),
	certificate_authorities(47),
	oid_filters(48),
	post_handshake_auth(49),
	signature_algorithms_cert(50),
	key_share(51),
	renegotiation_info(65281);


    public static ExtensionType fromValue(int value) {
        for (ExtensionType type : EnumSet.allOf(ExtensionType.class)) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    private final int value;

    ExtensionType(int value) {
        this.value = value;
    }
}

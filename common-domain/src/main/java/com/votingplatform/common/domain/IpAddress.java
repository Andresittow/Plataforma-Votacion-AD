package com.votingplatform.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public record IpAddress(String value) {
    private static final Pattern IPV4 = Pattern.compile(
        "(?:0|[1-9][0-9]?|1[0-9]{2}|2[0-4][0-9]|25[0-5])"
        + "(?:\\.(?:0|[1-9][0-9]?|1[0-9]{2}|2[0-4][0-9]|25[0-5])){3}");
    private static final Pattern IPV6_CHARACTERS = Pattern.compile("[0-9a-fA-F:.]+");

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public IpAddress {
        Objects.requireNonNull(value, "IP address must not be null");
        if (!IPV4.matcher(value).matches() && !isValidIpv6(value)) {
            throw new IllegalArgumentException("Invalid IP address: " + value);
        }
    }

    @JsonValue
    public String value() {
        return value;
    }

    private static boolean isValidIpv6(String value) {
        // A colon and a strict character allowlist guarantee a literal, never a DNS lookup.
        if (!value.contains(":") || !IPV6_CHARACTERS.matcher(value).matches()) {
            return false;
        }
        try {
            InetAddress.getByName(value);
            return true;
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}

package net.nethersmp.storm.voucher.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record VoucherId(String value) {

    public static VoucherId of(String id) {
        return new VoucherId(id);
    }

    @JsonIgnore
    public boolean matches(String id) {
        return id.equalsIgnoreCase(value);
    }

}

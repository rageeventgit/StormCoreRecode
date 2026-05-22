package net.nethersmp.storm.voucher.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.kyori.adventure.text.format.TextColor;

public record VoucherColor(String value) {

    @JsonIgnore
    public TextColor adventure() {
        return TextColor.fromHexString(value);
    }
}

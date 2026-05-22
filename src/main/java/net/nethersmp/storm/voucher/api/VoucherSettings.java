package net.nethersmp.storm.voucher.api;

import org.bukkit.Material;

import java.util.List;

public record VoucherSettings(String name, String color, Material material, List<String> lore) {

    public static final VoucherSettings EMPTY = new VoucherSettings("", "", Material.AIR, List.of());

}

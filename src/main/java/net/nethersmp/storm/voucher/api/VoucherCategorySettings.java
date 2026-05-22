package net.nethersmp.storm.voucher.api;

import org.bukkit.Material;

import java.util.List;

public record VoucherCategorySettings(Material material, String command, String color, List<String> lore) {
}

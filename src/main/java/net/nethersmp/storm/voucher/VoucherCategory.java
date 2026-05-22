package net.nethersmp.storm.voucher;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import net.nethersmp.storm.utilities.Components;
import net.nethersmp.storm.utilities.Numbers;
import net.nethersmp.storm.utilities.Strings;
import net.nethersmp.storm.voucher.api.VoucherCategorySettings;
import net.nethersmp.storm.voucher.api.VoucherId;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

public record VoucherCategory(VoucherId id, VoucherCategorySettings settings, ConcurrentMap<String, VoucherChild<?>> children) {

    @JsonIgnore
    public Optional<VoucherChild<?>> findChild(String id) {
        return Optional.ofNullable(children.get(id));
    }

    @JsonIgnore
    public <T> VoucherChild<T> child(String id) {
        //noinspection unchecked
        return (VoucherChild<T>) children.get(id);
    }

    @JsonIgnore
    public ItemStack item(VoucherChild<?> child) {
        String color = child.settings().color().isEmpty() ? settings.color() : child.settings().color();
        Material material = child.settings().material() == Material.AIR ? settings.material() : child.settings().material();
        List<String> lore = child.settings().lore().isEmpty() ? settings.lore() : child.settings().lore();

        Object worth = child.worth();

        String name = child.settings().name().isEmpty()
                ? (Numbers.format(worth.toString()) + " " + Strings.fixCase(id.value()) + " Voucher") : child.settings().name();

        lore = lore.stream().map(line -> line.replace("%color%", color)
                        .replace("%cat_color%", settings.color())
                        .replace("%worth_simple%", Numbers.format(worth.toString()))
                        .replace("%worth_complex%", worth instanceof Number ? NumberFormat.getInstance().format(worth) : worth.toString())
                )
                .toList();

        return ItemBuilder.from(material)
                .name(Components.color("<%s>%s</%s>".formatted(color, name, color)))
                .lore(lore.stream().map(Components::color).toList())
                .flags(ItemFlag.values())
                .setNbt("voucher", child.id().value())
                .setNbt("category", id.value())
                .asGuiItem()
                .getItemStack();
    }
}

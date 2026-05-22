package net.nethersmp.storm.voucher.modules;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.nethersmp.storm.module.api.Module;
import net.nethersmp.storm.module.api.Result;
import net.nethersmp.storm.utilities.modules.CommandsModule;
import net.nethersmp.storm.utilities.modules.ListenerModule;
import net.nethersmp.storm.voucher.VoucherCategory;
import net.nethersmp.storm.voucher.VoucherChild;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static io.papermc.paper.command.brigadier.argument.ArgumentTypes.players;

@RequiredArgsConstructor
public class VoucherManagerModule implements Module<Void> {

    public static final String ID = "voucher_manager";
    public static final Set<String> DEPENDENCIES = Set.of(VoucherLoaderModule.ID, CommandsModule.ID, ListenerModule.ID);
    public static final int PRIORITY = 940;

    private final VoucherLoaderModule loader;
    private final CommandsModule commands;
    private final ListenerModule events;

    @Override
    public Result<Void> load() {
        commands.register(literal("voucher").requires(source -> source.getSender().hasPermission("stormcore.voucher"))
                .then(literal("give").requires(source -> source.getSender().hasPermission("stormcore.voucher.give")).then(argument("id", word()).then(argument("target", players()).executes(context -> {
                    CommandSourceStack source = context.getSource();
                    CommandSender sender = source.getSender();

                    String input = context.getArgument("id", String.class).toUpperCase();
                    final PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
                    final List<Player> targets = targetResolver.resolve(source);

                    findCategory(input.toLowerCase()).ifPresentOrElse(category -> {
                        String replace = input.replace(("_" + category.id().value() + "_VOUCHER").toUpperCase(), "");
                        sender.sendRichMessage(replace);
                        VoucherChild<Object> voucher = category.child(replace);
                        if (voucher == null) {
                            sender.sendRichMessage("<red>Failed</red> <gray> to find <green>specified</green> <yellow>voucher</yellow>.");
                            category.children().forEach((id, child) -> {
                                sender.sendRichMessage("<blue>" + id + "_" + category.id().value().toUpperCase() + "_VOUCHER");
                            });
                            return;
                        }

                        ItemStack voucherItem = category.item(voucher);
                        String displayName = PlainTextComponentSerializer.plainText().serialize(voucherItem.getItemMeta().customName());

                        for (Player target : targets) {
                            if (target.getInventory().firstEmpty() == -1) continue;
                            target.getInventory().addItem();
                            target.sendRichMessage("<gray>You've been <green>given</green> a <light_purple>" + displayName + "</light_purple> <yellow>voucher</yellow>!");
                            target.playSound(Sound.sound(Key.key("entity.experience_orb.pickup"), Sound.Source.MASTER, 1, 2));
                        }
                    }, () -> sender.sendRichMessage("<gray><red>Failed</red> to find <yellow>voucher category</yellow>."));
                    return 1;
                })))).build());

        return Result.success();
    }

    @Override
    public void unload() {
    }


    private Optional<VoucherCategory> findCategory(String input) {
        String[] words = input.split("_");

        for (String word : words) {
            Optional<VoucherCategory> category = loader.getVoucherCategory(word);

            if (category.isPresent()) {
                return category;
            }
        }
        return Optional.empty();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Set<String> dependencies() {
        return DEPENDENCIES;
    }
}

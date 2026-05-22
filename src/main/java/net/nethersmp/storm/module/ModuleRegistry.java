package net.nethersmp.storm.module;

import io.papermc.paper.command.brigadier.PaperCommands;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.nethersmp.storm.StormPlugin;
import net.nethersmp.storm.cooldown.CooldownModule;
import net.nethersmp.storm.crates.modules.CratesModule;
import net.nethersmp.storm.module.api.ModuleDefinition;
import net.nethersmp.storm.permission.modules.RankHandlerModule;
import net.nethersmp.storm.permission.modules.RankLoaderModule;
import net.nethersmp.storm.permission.modules.UserPermissionsModule;
import net.nethersmp.storm.punishment.modules.UserPunishmentModule;
import net.nethersmp.storm.tpa.modules.TeleportRequestModule;
import net.nethersmp.storm.utilities.modules.CommandsModule;
import net.nethersmp.storm.utilities.modules.ListenerModule;
import net.nethersmp.storm.voucher.modules.VoucherLoaderModule;
import net.nethersmp.storm.voucher.modules.VoucherManagerModule;

import java.lang.reflect.Field;

@RequiredArgsConstructor
public class ModuleRegistry {

    private final StormPlugin plugin;

    @Getter
    private ModuleLoader moduleLoader;

    private CommandsModule commandsModule;

    public void register() {
        moduleLoader = new ModuleLoader();

        commandsModule = new CommandsModule();

        moduleLoader.register(new ModuleDefinition<>(CommandsModule.ID, CommandsModule.DEPENDENCIES, CommandsModule.PRIORITY, access -> commandsModule));
        moduleLoader.register(new ModuleDefinition<>(ListenerModule.ID, ListenerModule.DEPENDENCIES, ListenerModule.PRIORITY, access -> new ListenerModule(plugin)));
        moduleLoader.register(new ModuleDefinition<>(CooldownModule.ID, CooldownModule.DEPENDENCIES, CooldownModule.PRIORITY, moduleAccess -> new CooldownModule()));

        moduleLoader.register(new ModuleDefinition<>(UserPermissionsModule.ID,
                UserPermissionsModule.DEPENDENCIES,
                UserPermissionsModule.PRIORITY,
                access -> new UserPermissionsModule(access.require(ListenerModule.ID, ListenerModule.class))));

        moduleLoader.register(new ModuleDefinition<>(RankLoaderModule.ID,
                RankLoaderModule.DEPENDENCIES,
                RankLoaderModule.PRIORITY,
                access -> new RankLoaderModule(plugin.getDataPath().resolve("ranks.json"))));

        moduleLoader.register(new ModuleDefinition<>(RankHandlerModule.ID,
                RankHandlerModule.DEPENDENCIES,
                RankHandlerModule.PRIORITY,
                access -> new RankHandlerModule(plugin,
                        access.require(RankLoaderModule.ID, RankLoaderModule.class),
                        access.require(ListenerModule.ID, ListenerModule.class),
                        access.require(CommandsModule.ID, CommandsModule.class))));

        moduleLoader.register(new ModuleDefinition<>(UserPunishmentModule.ID,
                UserPunishmentModule.DEPENDENCIES,
                UserPunishmentModule.PRIORITY,
                access -> new UserPunishmentModule(plugin,
                        access.require(CommandsModule.ID, CommandsModule.class),
                        access.require(ListenerModule.ID, ListenerModule.class))));

        moduleLoader.register(new ModuleDefinition<>(
                CratesModule.ID,
                CratesModule.DEPENDENCIES,
                CratesModule.PRIORITY,
                access -> new CratesModule(plugin,
                        access.require(CommandsModule.ID, CommandsModule.class),
                        access.require(CooldownModule.ID, CooldownModule.class),
                        access.require(ListenerModule.ID, ListenerModule.class),
                        plugin.getDataPath().resolve("crates.json"))
        ));

        moduleLoader.register(new ModuleDefinition<>(
                VoucherLoaderModule.ID,
                VoucherLoaderModule.DEPENDENCIES,
                VoucherLoaderModule.PRIORITY,
                access -> new VoucherLoaderModule(
                        plugin.getDataPath().resolve("vouchers.json"))
        ));
        moduleLoader.register(new ModuleDefinition<>(
                VoucherManagerModule.ID,
                VoucherManagerModule.DEPENDENCIES,
                VoucherManagerModule.PRIORITY,
                access -> new VoucherManagerModule(
                        access.require(VoucherLoaderModule.ID, VoucherLoaderModule.class),
                        access.require(CommandsModule.ID, CommandsModule.class),
                        access.require(ListenerModule.ID, ListenerModule.class))
        ));
        moduleLoader.register(
                new ModuleDefinition<>(
                        TeleportRequestModule.ID,
                        TeleportRequestModule.DEPENDENCIES,
                        TeleportRequestModule.PRIORITY,
                        access -> new TeleportRequestModule(
                                plugin,
                                access.require(CooldownModule.ID, CooldownModule.class),
                                access.require(CommandsModule.ID, CommandsModule.class),
                                access.require(ListenerModule.ID, ListenerModule.class)
                        )
                )
        );
    }


    @SneakyThrows
    public void registerCommands() {
        Field f = PaperCommands.INSTANCE.getClass().getDeclaredField("invalid");
        f.setAccessible(true);
        f.set(PaperCommands.INSTANCE, false);

        PaperCommands.INSTANCE.setCurrentContext(plugin);
        commandsModule.getCommandNodes().forEach(PaperCommands.INSTANCE::register);

        f.set(PaperCommands.INSTANCE, true);
        f.setAccessible(false);
    }

    public void cancel() {

    }
}

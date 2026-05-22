package net.nethersmp.storm;

import lombok.Getter;
import lombok.SneakyThrows;
import net.nethersmp.storm.listeners.PlayerEvents;
import net.nethersmp.storm.module.ModuleLoader;
import net.nethersmp.storm.module.ModuleRegistry;
import net.nethersmp.storm.punishment.UserPunishmentAccessor;
import net.nethersmp.storm.punishment.api.storage.PunishmentDataStore;
import net.nethersmp.storm.punishment.storage.JsonFilePunishmentDataStore;
import net.nethersmp.storm.user.UserDataAccessor;
import net.nethersmp.storm.user.UserDataModifier;
import net.nethersmp.storm.user.storage.UserDataStore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class StormPlugin extends JavaPlugin {

    @Getter
    private UserDataAccessor userDataAccessor;

    @Getter
    private ModuleRegistry moduleRegistry;

    private int moduleLoaderTaskId = -1;

    @SneakyThrows
    @Override
    public void onEnable() {
        UserDataStore userDataStore = new UserDataStore(getDataPath().resolve("players.json"));
        PunishmentDataStore punishmentDataStore = new JsonFilePunishmentDataStore(getDataPath().resolve("punishments.json"));
        punishmentDataStore.initialize();

        userDataAccessor = new UserDataAccessor(userDataStore);
        UserDataModifier.init(userDataAccessor);

        UserPunishmentAccessor.setPunishmentStorage(punishmentDataStore);
        moduleRegistry = new ModuleRegistry(this);
        moduleRegistry.register();

        ModuleLoader moduleLoader = moduleRegistry.getModuleLoader();

        moduleLoader.init();

        moduleLoaderTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            ModuleLoader.StepResult step = moduleLoader.step(2);

            if (!step.hasMoreWork()) {
                getServer().getScheduler().cancelTask(moduleLoaderTaskId);
                moduleLoaderTaskId = -1;

                getLogger().info("Modules loaded. States: " + moduleLoader.states());
                if (!moduleLoader.warnings().isEmpty()) getLogger().warning("Warnings: " + moduleLoader.warnings());
                if (!moduleLoader.reasons().isEmpty()) getLogger().warning("Reasons: " + moduleLoader.reasons());

                moduleRegistry.registerCommands();
            }
        }, 1L, 1L);


        Bukkit.getPluginManager().registerEvents(new PlayerEvents(this), this);
    }

    @Override
    public void onDisable() {
        if (moduleLoaderTaskId != -1) Bukkit.getScheduler().cancelTask(moduleLoaderTaskId);

        moduleRegistry.getModuleLoader().unloadAll();

        userDataAccessor.flushAll();

        moduleRegistry = null;
        userDataAccessor = null;
    }


    private void registerModules() {

    }
}

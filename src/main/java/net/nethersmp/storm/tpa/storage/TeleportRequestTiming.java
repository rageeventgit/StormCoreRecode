package net.nethersmp.storm.tpa.storage;

import lombok.RequiredArgsConstructor;
import net.nethersmp.storm.StormPlugin;
import net.nethersmp.storm.tpa.TeleportRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
public class TeleportRequestTiming extends BukkitRunnable {

    private final StormPlugin plugin;
    private final TeleportRequestStorage storage;

    @Override
    public void run() {
        for (TeleportRequest teleportRequest : storage.getTeleportRequests()) {
            if (teleportRequest.expired()) {
                Player sendingPlayer = Bukkit.getPlayer(teleportRequest.sender());
                Player receivingPlayer = Bukkit.getPlayer(teleportRequest.receiver());
                sendingPlayer.sendRichMessage("<gray>Your <green>teleport</green> <yellow>request</yellow> to <aqua>%s</aqua> has <red>expired</red>!</gray>".formatted(receivingPlayer.getName()));
                receivingPlayer.sendRichMessage("<gray>The <green>teleport</green> <yellow>request</yellow> from <aqua>%s</aqua> has <red>expired</red>!</gray>".formatted(sendingPlayer.getName()));
                storage.remove(teleportRequest.id());
            }
        }
    }

    public void start() {
        runTaskTimerAsynchronously(plugin, 0, 20);
    }
}

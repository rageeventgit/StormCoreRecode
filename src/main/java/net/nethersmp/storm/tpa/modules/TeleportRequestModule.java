package net.nethersmp.storm.tpa.modules;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import lombok.RequiredArgsConstructor;
import net.nethersmp.storm.StormPlugin;
import net.nethersmp.storm.brigadier.EnumValueArgument;
import net.nethersmp.storm.cooldown.CooldownModule;
import net.nethersmp.storm.module.api.Module;
import net.nethersmp.storm.module.api.Result;
import net.nethersmp.storm.tpa.TeleportRequest;
import net.nethersmp.storm.tpa.TeleportType;
import net.nethersmp.storm.tpa.events.TeleportRequestEvent;
import net.nethersmp.storm.tpa.storage.TeleportRequestStorage;
import net.nethersmp.storm.tpa.storage.TeleportRequestTiming;
import net.nethersmp.storm.utilities.modules.CommandsModule;
import net.nethersmp.storm.utilities.modules.ListenerModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;
import java.util.Set;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static io.papermc.paper.command.brigadier.argument.ArgumentTypes.player;

@RequiredArgsConstructor
public class TeleportRequestModule implements Module<Void> {

    public static final String ID = "teleport_request";
    public static final Set<String> DEPENDENCIES = Set.of(CooldownModule.ID, CommandsModule.ID, ListenerModule.ID);
    public static final int PRIORITY = 950;

    private final StormPlugin plugin;
    private final CooldownModule cooldowns;
    private final CommandsModule commands;
    private final ListenerModule events;

    private final TeleportRequestStorage requestStorage = new TeleportRequestStorage();
    private TeleportRequestTiming requestTiming;


    @Override
    public Result<Void> load() {
        requestTiming = new TeleportRequestTiming(plugin, requestStorage);
        requestTiming.start();

        events.listen(ID, PlayerQuitEvent.class, event -> {
            Player player = event.getPlayer();
            //Definitely gonna have to stress test this.
            requestStorage.getTeleportRequests().stream()
                    .filter(request -> request.sender() == player.getUniqueId() || request.receiver() == player.getUniqueId())
                    .forEach(request -> {
                        Player sendingPlayer = Bukkit.getPlayer(request.sender());
                        Player receivingPlayer = Bukkit.getPlayer(request.receiver());

                        if (sendingPlayer != null)
                            sendingPlayer.sendRichMessage("<gray>Your <green>teleport</green> <yellow>request</yellow> to <aqua>%s</aqua> has been <red>cancelled</red>!</gray> <dark_gray>(RECIPIENT LEAVE)</dark_gray>".formatted(receivingPlayer.getName()));

                        if (receivingPlayer != null)
                            receivingPlayer.sendRichMessage("<gray>The <green>teleport</green> <yellow>request</yellow> from <aqua>%s</aqua> has been <red>cancelled</red>!</gray> <dark_gray>(RECIPIENT LEAVE)</dark_gray>".formatted(sendingPlayer.getName()));

                        requestStorage.remove(request.id());
                    });
        });
        events.listen(ID, TeleportRequestEvent.class, event -> {
            TeleportRequest request = event.getRequest();

            if (requestStorage.getTeleportRequest(request.id()).isEmpty())
                return;

            Player sendingPlayer = Bukkit.getPlayer(request.sender());
            Player receivingPlayer = Bukkit.getPlayer(request.receiver());

            //There's a better way to handle this, but I don't feel like doing messaging.
            if (sendingPlayer == null || receivingPlayer == null) {
                requestStorage.remove(request.id());
                return;
            }

            if (event.getStatus() == TeleportRequestEvent.Status.DENIED) {
                sendingPlayer.sendRichMessage("<gray><aqua>%s</aqua> has <red>denied</red> your <yellow>TPA</yellow> request.".formatted(receivingPlayer.getName()));
                requestStorage.remove(request.id());
                return;
            }

            Location targetLocation = switch (request.type()) {
                case AWAY -> receivingPlayer.getLocation();
                case HERE -> sendingPlayer.getLocation();
            };

            String acceptedRequestOne = "<gray><aqua>%s</aqua> has <green>accepted</green> your <yellow>TPA</yellow> request.";
            String acceptedRequestTwo = "<gray><green>Teleporting</green> you to <yellow>%s</yellow>.";

            switch (request.type()) {
                case AWAY -> {
                    sendingPlayer.teleport(targetLocation);

                    receivingPlayer.sendRichMessage(acceptedRequestOne.formatted(sendingPlayer.getName()));
                    sendingPlayer.sendRichMessage(acceptedRequestTwo.formatted(receivingPlayer.getName()));
                }
                case HERE -> {
                    receivingPlayer.teleport(targetLocation);
                    receivingPlayer.sendRichMessage(acceptedRequestTwo.formatted(sendingPlayer.getName()));
                    sendingPlayer.sendRichMessage(acceptedRequestOne.formatted(receivingPlayer.getName()));
                }
            }
            requestStorage.remove(request.id());
        });


        commands.register(literal("tpa").requires(source -> source.getSender().hasPermission("stormcore.tpa.request"))
                .then(argument("type", new EnumValueArgument<>(TeleportType.class, true))
                        .then(argument("player", player()).executes(context -> {
                            CommandSourceStack source = context.getSource();
                            CommandSender sender = source.getSender();
                            if (!(sender instanceof Player player)) {
                                sender.sendRichMessage("<red>You must be a player to use this command.");
                                return 0;
                            }

                            final PlayerSelectorArgumentResolver argumentResolver = context.getArgument("player", PlayerSelectorArgumentResolver.class);
                            final Player target = argumentResolver.resolve(source).getFirst();

                            final TeleportType teleportType = context.getArgument("type", TeleportType.class);

                            if (target == null || !target.isOnline()) {
                                player.sendRichMessage("<red>Failed</red> to send <yellow>tpa</yellow> request. <gray>(PLAYER OFFLINE)");
                                return 0;
                            }

                            long expiryTime = System.currentTimeMillis() + (15 * 1000);

                            TeleportRequest teleportRequest = TeleportRequest.create(teleportType, expiryTime, player.getUniqueId(), target.getUniqueId());
                            requestStorage.add(teleportRequest);

                            player.sendRichMessage("<green>Successfully</green> sent a <yellow>TPA</yellow> request to <aqua>%s</aqua>!".formatted(target.getName()));
                            target.sendRichMessage("<aqua>%s</aqua> has sent you a <yellow>TPA</yellow> request.".formatted(target.getName()));
                            return 1;
                        })))
                .then(literal("accept").then(argument("player", player()).executes(context -> {
                    CommandSourceStack source = context.getSource();
                    CommandSender sender = source.getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendRichMessage("<red>You must be a player to use this command.");
                        return 0;
                    }
                    final PlayerSelectorArgumentResolver argumentResolver = context.getArgument("player", PlayerSelectorArgumentResolver.class);
                    final Player target = argumentResolver.resolve(source).getFirst();

                    Optional<TeleportRequest> requestOptional = requestStorage.getTeleportRequest(player.getUniqueId(), target.getUniqueId());

                    if (requestOptional.isEmpty()) {
                        player.sendRichMessage("<gray><red>Failed</red> to find a <yellow>TPA</yellow> request from <aqua>%s</aqua>."
                                .formatted(target.getName()));
                        return 0;
                    }
                    TeleportRequest teleportRequest = requestOptional.get();
                    new TeleportRequestEvent(teleportRequest, TeleportRequestEvent.Status.ACCEPTED).callEvent();
                    return 1;
                })))
                .then(literal("deny").then(argument("player", player()).executes(context -> {
                    CommandSourceStack source = context.getSource();
                    CommandSender sender = source.getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendRichMessage("<red>You must be a player to use this command.");
                        return 0;
                    }
                    final PlayerSelectorArgumentResolver argumentResolver = context.getArgument("player", PlayerSelectorArgumentResolver.class);
                    final Player target = argumentResolver.resolve(source).getFirst();

                    Optional<TeleportRequest> requestOptional = requestStorage.getTeleportRequest(player.getUniqueId(), target.getUniqueId());

                    if (requestOptional.isEmpty()) {
                        player.sendRichMessage("<gray><red>Failed</red> to find a <yellow>TPA</yellow> request from <aqua>%s</aqua>."
                                .formatted(target.getName()));
                        return 0;
                    }
                    TeleportRequest teleportRequest = requestOptional.get();
                    new TeleportRequestEvent(teleportRequest, TeleportRequestEvent.Status.DENIED).callEvent();
                    return 1;
                })))
                .build());

        return Result.success();
    }

    @Override
    public void unload() {
        requestTiming.cancel();
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

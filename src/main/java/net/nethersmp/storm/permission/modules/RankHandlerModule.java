package net.nethersmp.storm.permission.modules;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import net.nethersmp.storm.StormPlugin;
import net.nethersmp.storm.module.api.Module;
import net.nethersmp.storm.module.api.Result;
import net.nethersmp.storm.permission.UserRank;
import net.nethersmp.storm.permission.chat.UserRankChatRenderer;
import net.nethersmp.storm.punishment.UserPunishment;
import net.nethersmp.storm.punishment.UserPunishmentAccessor;
import net.nethersmp.storm.user.data.UserDataType;
import net.nethersmp.storm.utilities.Strings;
import net.nethersmp.storm.utilities.modules.CommandsModule;
import net.nethersmp.storm.utilities.modules.ListenerModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

@RequiredArgsConstructor
public class RankHandlerModule implements Module<Void> {

    public static final String ID = "user_ranks_handler";
    public static final Set<String> DEPENDENCIES = Set.of("user_ranks_loader",
            "commands",
            "listeners");
    public static final int PRIORITY = 980;


    private final StormPlugin plugin;
    private final RankLoaderModule rankLoader;
    private final ListenerModule events;
    private final CommandsModule commands;

    private final ConcurrentHashMap<UUID, PermissionAttachment> rankAttachments = new ConcurrentHashMap<>();


    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<String> dependencies() {
        return DEPENDENCIES;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Result<Void> load() {
        UserRankChatRenderer rankChatRenderer = new UserRankChatRenderer(rankLoader);

        events.listen(id(), PlayerJoinEvent.class, event -> {
            Player player = event.getPlayer();

            Result<String> appliedRank = applyRank(player);

            if (appliedRank.failed())
                player.sendMessage(appliedRank.toComponent());
        });
        events.listen(id(), PlayerQuitEvent.class, event -> {
            Player player = event.getPlayer();
            removeRank(player);
        });
        events.listen(id(), AsyncChatEvent.class, event -> {
            if (event.isCancelled()) return;
            event.setCancelled(true);
            Player player = event.getPlayer();

            Optional<UserPunishment> userPunishment = UserPunishmentAccessor.get(player.getUniqueId());
            if (userPunishment.isPresent() && !userPunishment.get().duration().isOver())
                return;

            event.renderer(rankChatRenderer);
        });

        commands.register(literal("rank").requires(source -> source.getSender().hasPermission("stormcore.rank"))
                .then(argument("target", ArgumentTypes.player()).then(argument("rank", word()).executes(context -> {
                    CommandSourceStack source = context.getSource();
                    CommandSender sender = source.getSender();

                    PlayerSelectorArgumentResolver playerResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
                    final Player target = playerResolver.resolve(context.getSource()).getFirst();

                    String rankId = getString(context, "rank").toUpperCase();

                    rankLoader.getUserRank(rankId).ifPresentOrElse(userRank -> {
                        UserDataType.RANK.set(target.getUniqueId(), rankId);
                        sender.sendRichMessage("<gray><green>Updated</green> <yellow>%s</yellow> rank to %s!</gray>".formatted(target.getName(), userRank.getFormattedPrefix()));
                        removeRank(target);

                        Result<String> appliedRank = applyRank(target);
                        //Probably the most cursed thing I think I've seen in a while.
                        (appliedRank.failed() ? sender : target).sendMessage(appliedRank.toComponent());

                    }, () -> sender.sendRichMessage("<red>Failed</red> <gray>to <green>update</green> <yellow>%s's</yellow> rank!</gray>".formatted(target.getName())));

                    return Command.SINGLE_SUCCESS;
                })))
                .build()
        );

        return Result.success();
    }

    @Override
    public void unload() {

    }

    private Result<String> applyRank(Player player) {
        AtomicReference<Result<String>> appliedRank = new AtomicReference<>(Result.success());

        getUserRank(player).ifPresentOrElse(rank -> {
            var rankPermissionAttachment = player.addAttachment(plugin);
            rankAttachments.put(player.getUniqueId(), rankPermissionAttachment);

            for (String permission : rank.permissions()) {
                rankPermissionAttachment.setPermission(permission, true);
            }
            player.recalculatePermissions();
            player.updateCommands();

            String prefix = rank.getFormattedPrefix();

            if (rank.prefix().isEmpty())
                prefix = rank.getColor() + Strings.fixCase(UserDataType.RANK.get(player.getUniqueId())) + rank.getEndColor();

            appliedRank.set(Result.success("<gray><green>Successfully</green> applied</gray> " + prefix));
        }, () -> appliedRank.set(Result.fail("RANKS", "Couldn't find '" + player.getUniqueId() + "' rank!")));
        return appliedRank.get();
    }

    private void removeRank(Player player) {
        PermissionAttachment attachment = rankAttachments.remove(player.getUniqueId());
        if (attachment == null) return;

        player.removeAttachment(attachment);
        player.recalculatePermissions();
        player.updateCommands();
    }

    private Optional<UserRank> getUserRank(Player player) {
        return rankLoader.getUserRank(UserDataType.RANK.get(player.getUniqueId()));
    }

}

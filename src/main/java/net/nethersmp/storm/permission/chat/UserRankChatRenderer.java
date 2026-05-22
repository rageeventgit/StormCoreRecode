package net.nethersmp.storm.permission.chat;

import io.papermc.paper.chat.ChatRenderer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.nethersmp.storm.permission.UserRank;
import net.nethersmp.storm.permission.modules.RankLoaderModule;
import net.nethersmp.storm.user.data.UserDataType;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

@RequiredArgsConstructor
public class UserRankChatRenderer implements ChatRenderer {

    private final RankLoaderModule rankLoader;

    @Override
    public @NonNull Component render(@NonNull Player player, @NonNull Component playerName, @NonNull Component message, @NonNull Audience audience) {
        UserRank userRank = getUserRank(player).get();
        String prefix = userRank.prefix();

        if (!prefix.isEmpty())
            prefix = prefix + " ";

        prefix = prefix + player.getName();

        String rankId = UserDataType.RANK.get(player.getUniqueId());
        String rankText = "<" + userRank.getColor() + ">" + prefix + "<" + userRank.getEndColor() + ">";
        String oldMessageString = PlainTextComponentSerializer.plainText().serialize(message);
        String colonColor = "<" + ("MEMBER".equals(rankId) ? "dark_gray" : "white") + ">";

        return MiniMessage.miniMessage().deserialize(rankText + colonColor + ": " + oldMessageString);
    }

    private Optional<UserRank> getUserRank(Player player) {
        return rankLoader.getUserRank(UserDataType.RANK.get(player.getUniqueId()));
    }
}

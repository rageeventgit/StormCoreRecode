package net.nethersmp.storm.crates.ui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiContainer;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.components.util.Legacy;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.nethersmp.storm.StormPlugin;
import net.nethersmp.storm.crates.CrateItem;
import net.nethersmp.storm.crates.api.CrateData;
import net.nethersmp.storm.utilities.Strings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SpinningCrateUserInterface extends Gui {

    private final StormPlugin plugin;
    private final List<ItemStack> row;
    private final ItemStack prize;

    private final int lowSlot;
    private final int highSlot;

    private BukkitTask task = null;
    private int counter;
    private boolean prizeClaimed = false;

    public SpinningCrateUserInterface(StormPlugin plugin, CrateData crateData) {
        super(new GuiContainer.Chest(Component.text(Strings.fixCase(crateData.name()) + " Crate Roll"),
                (title, owner, rows) ->
                        Bukkit.createInventory(owner, rows, Legacy.SERIALIZER.serialize(title)),
                3
        ), Set.of(InteractionModifier.values()));
        this.plugin = plugin;

        List<ItemStack> possibleItems = crateData.items().values().stream().toList();

        this.prize = possibleItems.get(ThreadLocalRandom.current().nextInt(possibleItems.size()));
        this.row = new ArrayList<>(possibleItems);

        System.out.println(prize);

        int itemsCount = possibleItems.size();

        int half = (int) (double) (itemsCount / 2);
        int middle = 13;
        this.lowSlot = (itemsCount > 7) ? 9 : (middle - half);
        this.highSlot = (itemsCount > 7) ? 17 : (middle + half);

        getFiller().fill(ItemBuilder.from(CrateItem.getCrateClassColor(crateData)).asGuiItem());
        setOpenGuiAction(event -> {
            Player player = (Player) event.getPlayer();
            spin(player);
        });

        setCloseGuiAction(event -> {
            if (task == null) return;
            task.cancel();
            givePrize((Player) event.getPlayer());
        });

    }


    public void spin(Player player) {
        this.task = new BukkitRunnable() {
            double delay = 0.0;
            int tick = 0;

            @Override
            public void run() {
                tick++;
                delay += 1.0 / (20.0 * 6.0);

                if (tick >= delay * 10.0) {
                    tick = 0;
                    progressItems();
                    counter++;
                }
                endCheck(player);
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);

    }

    private void progressItems() {

        for (int rowSlot = lowSlot; rowSlot <= highSlot; rowSlot++) {
            ItemStack next = row.get((rowSlot + counter) % row.size());
            getInventory().setItem(rowSlot, next);
        }
    }

    private void endCheck(Player player) {
        BukkitTask runningTask = task;
        if (runningTask == null) return;
        ItemStack maybeCenter = getInventory().getItem(13);
        if (counter >= 40 && maybeCenter.isSimilar(prize)) {
            runningTask.cancel();
            givePrize(player);
        }
    }

    private void givePrize(Player player) {
        if (prizeClaimed) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            close(player);
            player.getInventory().addItem(prize);
            player.playSound(Sound.sound(Key.key("entity.experience_orb.pickup"), Sound.Source.MASTER, 1f, 2f));
        }, 20L);

        prizeClaimed = true;
    }
}

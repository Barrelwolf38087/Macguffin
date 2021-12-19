package gay.realmromp;

import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.util.UUID;

public class EggTimeCheckTask extends BukkitRunnable {

    private final Macguffin plugin;

    public EggTimeCheckTask(Macguffin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {

        long eggTouchTimestamp = plugin.getConfig().getLong("eggTouchTimestamp");
        long eggInventoryTimestamp = plugin.getConfig().getLong("eggInventoryTimestamp");
        long time = Instant.now().getEpochSecond();
        //plugin.getLogger().info("running task");

        if (eggTouchTimestamp != 0 && time - eggTouchTimestamp >= Macguffin.TOUCH_INTERVAL) {
            plugin.getLogger().info("resetting egg (touch timeout)");

            plugin.resetEgg(true);
        } else if (eggInventoryTimestamp != 0 && time - eggInventoryTimestamp >= Macguffin.INVENTORY_INTERVAL) {
            plugin.getLogger().info("resetting egg (inventory timeout)");

            plugin.resetEgg(true);
        }
    }
}

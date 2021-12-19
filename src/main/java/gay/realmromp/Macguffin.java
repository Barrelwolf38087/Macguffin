package gay.realmromp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;

public class Macguffin extends JavaPlugin {

    // 48 hours in seconds
    public static final long TOUCH_INTERVAL = 60 * 60 * 26;
    //public static final long TOUCH_INTERVAL = 15;

    // 1 hour in seconds
    public static final long INVENTORY_INTERVAL = 60 * 60;
    //public static final long INVENTORY_INTERVAL = 30;

    public static final TextColor COLOR = TextColor.color(227, 60 , 34);

    private boolean validateConfig() {
        String worldName = getConfig().getString("world");
        if (worldName == null) {
            getLogger().severe("FATAL: world key not found in config.yml");
            return false;
        } else if (getServer().getWorld(worldName) == null) {
            getLogger().severe("FATAL: Unable to load world \"" + worldName + "\"");
            return false;
        }

        return true;

    }

    public void resetEgg() {
        resetEgg(false);
    }

    public void resetEgg(boolean remove) {
        World world = getServer().getWorld(Objects.requireNonNull(getConfig().getString("world")));

        if (remove) {
            // Set to null when the holder logs off, so if it isn't then we can assume the holder is online here
            String uuid = getConfig().getString("eggHolderUUID");
            String locString = getConfig().getString("eggLocation");
            if (uuid != null) {
                Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                Inventory inventory = Objects.requireNonNull(player).getInventory();
                inventory.remove(Material.DRAGON_EGG);
                getLogger().info("removing from inv");
                getConfig().set("eggHolderUUID", null);
                getConfig().set("eggInventoryTimestamp", null);
                saveConfig();
            } else if (locString == null) {
                // Could potentially happen if the holder drops the egg right before the time check
                getLogger().warning("resetEgg(true) called, but no egg location found in config.yml!");
            } else {
                Scanner scanner = new Scanner(locString);
                String worldname = scanner.next();
                int x = scanner.nextInt();
                int y = scanner.nextInt();
                int z = scanner.nextInt();
                scanner.close();

                World eggWorld = getServer().getWorld(worldname);
                if (eggWorld == null) {
                    getLogger().severe("FATAL: Unable to load world \"" + worldname + "\"");
                    getServer().shutdown();
                    return;
                }

                Block block = eggWorld.getBlockAt(x, y, z);
                if (block.getType() != Material.DRAGON_EGG) {
                    getLogger().severe("Attempted to remove egg at " + locString + ", but none found!");
                    getLogger().severe("Bad things will likely happen (potentially a dupe)!");
                } else {
                    block.setType(Material.AIR);
                }
            }

        }

        Block spawnAt = Objects.requireNonNull(world).getHighestBlockAt(0, 0).getRelative(BlockFace.UP);

        spawnAt.setType(Material.DRAGON_EGG);
        getConfig().set("eggTouchTimestamp", null);
        updateEggLocation(spawnAt.getLocation());

        getLogger().info("Egg respawned");
        getServer().sendActionBar(Component.text("The egg has been respawned!").color(COLOR));
    }

    public void updateEggLocation(Location location) {
        String out =  location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
        getConfig().set("eggLocation", out);
        getConfig().set("eggHolderUUID", null);
        getConfig().set("eggInventoryTimestamp", null);
        saveConfig();
        getLogger().info("Updated egg location");
    }

    @Override
    public void onEnable() {
        if (!new File("plugins/Macguffin/config.yml").exists()) {
            saveDefaultConfig();
        }

        if (!validateConfig()) {
            getServer().shutdown();
            return;
        }

        getServer().getPluginManager().registerEvents(new EggListener(this), this);
        //getServer().getScheduler().scheduleSyncRepeatingTask(this, new EggTimeCheckTask(this), 0L, 20L * 15L);
        EggTimeCheckTask timeCheckTask = new EggTimeCheckTask(this);
        timeCheckTask.runTaskTimer(this, 0L, 20L * 30L);
        getLogger().info("Macguffin started");
    }

    @Override
    public void onDisable() {
        saveConfig();
    }
}

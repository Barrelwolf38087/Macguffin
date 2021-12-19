package gay.realmromp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.UUID;

import static gay.realmromp.Macguffin.COLOR;

public class EggListener implements Listener {

    private final Macguffin plugin;

    public EggListener(Macguffin plugin) {
        this.plugin = plugin;
    }



    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
            event.getItem().remove();
        }

        UUID thrower = event.getItem().getThrower();
        if (thrower != null) {
            Player player = Bukkit.getPlayer(thrower);

            if (player != null) {
                plugin.getServer().broadcast(Component.text(player.getName() + " tried to throw the egg in a hopper!").color(COLOR));
            }
        }

        plugin.getLogger().info("Intercepted hopper pickup");

        plugin.resetEgg();
    }

    // This is actually for hoppers/dispensers, and does not get called for players putting items in containers.
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getItem().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
            plugin.getServer().broadcast(Component.text("Hopper shenanigans detected!").color(COLOR));

            plugin.resetEgg();

            plugin.getLogger().info("Intercepted inventory move");
        }

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clicked = event.getClickedInventory();

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack stack = event.getCurrentItem();
            if (stack != null && stack.getType() == Material.DRAGON_EGG) {
                event.setCancelled(true);
                plugin.getLogger().info("Intercepted shift click");
            }
        }

        if (player.getInventory() != clicked && event.getCursor() != null
            && event.getCursor().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
            plugin.getLogger().info("Intercepted drag");
        }

        if (event.isCancelled()) {
            plugin.getServer().broadcast(Component.text(player.getName() + " tried to put the egg in a container!").color(COLOR));
        }
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.DRAGON_EGG) {
            UUID thrower = event.getEntity().getThrower();
            if (thrower != null) {
                Player player = Bukkit.getPlayer(thrower);
                if (player != null) {
                    plugin.getServer().broadcast(Component.text(player.getName() + " let the egg despawn! What a dumbass!").color(COLOR));
                    plugin.getLogger().info("Intercepted despawn");
                    plugin.resetEgg();
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item) {
            Item item = (Item) event.getEntity();
            if (item.getItemStack().getType() == Material.DRAGON_EGG) {
                UUID thrower = item.getThrower();
                if (thrower != null) {
                    Player player = Bukkit.getPlayer(thrower);
                    if (player != null) {
                        StringBuilder messageBuilder = new StringBuilder(player.getName() + " ");

                        switch (event.getCause()) {
                            case ENTITY_EXPLOSION:
                            case BLOCK_EXPLOSION:
                                messageBuilder.append("blew up the egg!");
                                break;
                            case CONTACT:
                                messageBuilder.append("dropped the egg on a cactus or berry bush!");
                                break;
                            case FIRE:
                            case FIRE_TICK:
                                messageBuilder.append("burned the egg!");
                                break;
                            case LAVA:
                                messageBuilder.append("threw the egg in lava!");
                                break;
                            case LIGHTNING:
                                messageBuilder.append("let the egg be struck by lightning (and possibly caused the lightning)!");
                                break;
                            case VOID:
                                messageBuilder.append("threw the egg into the void!");
                                break;
                            default:
                                messageBuilder.append("allowed the egg to be destroyed!");
                        }

                        messageBuilder.append(" Point and laugh at this user!");

                        plugin.getServer().broadcast(Component.text(messageBuilder.toString()).color(COLOR));
                    }
                } else {
                    plugin.getServer().broadcast(Component.text("The egg was destroyed!").color(COLOR));
                }

                plugin.getLogger().info("Intercepted entity damage");
                item.remove();
                plugin.resetEgg();
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.DRAGON_EGG && event.getAction().isRightClick()) {
            Location loc = block.getLocation();
            plugin.updateEggLocation(loc);
            long timestamp = Instant.now().getEpochSecond();
            plugin.getConfig().set("eggTouchTimestamp", timestamp);
            plugin.saveConfig();
            event.getPlayer().sendActionBar(Component.text("Egg touched!").color(TextColor.color(18, 138, 14)));
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            plugin.getLogger().info("Intercepted teleport");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
            if (!(event.getEntity() instanceof Player)) {
                // Ensure mobs can't pick it up
                event.setCancelled(true);
            } else {
                Player player = (Player) event.getEntity();
                plugin.getLogger().info("Intercepted pickup");
                plugin.getConfig().set("eggHolderUUID", player.getUniqueId().toString());
                plugin.getConfig().set("eggInventoryTimestamp", Instant.now().getEpochSecond());
                plugin.saveConfig();
            }
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.DRAGON_EGG) {
            plugin.getLogger().info("Intercepted drop");
            plugin.getConfig().set("eggHolderUUID", null);
            plugin.getConfig().set("eggInventoryTimestamp", null);
            plugin.saveConfig();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String uuid = plugin.getConfig().getString("eggHolderUUID");
        if (uuid != null && uuid.equals(event.getPlayer().getUniqueId().toString())) {
            event.getPlayer().getInventory().remove(Material.DRAGON_EGG);
            plugin.getServer().broadcast(Component.text(event.getPlayer().getName() + " logged out with the egg in their inventory!").color(COLOR));
            plugin.getLogger().info("Intercepted logout with egg in inventory");
            plugin.resetEgg();
            plugin.getConfig().set("eggHolderUUID", null);
            plugin.getConfig().set("eggInventoryTimestamp", null);
            plugin.saveConfig();
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getDrops().stream().anyMatch((ItemStack itemStack) -> itemStack.getType() == Material.DRAGON_EGG)) {
                Player player = (Player) (event.getEntity());
                plugin.getServer().broadcast(Component.text(player.getName() + " died with the egg in their inventory!").color(COLOR));
                plugin.getLogger().info("Intercepted death with egg in inventory");
                plugin.getConfig().set("eggHolderUUID", null);
                plugin.getConfig().set("eggInventoryTimestamp", null);
                plugin.saveConfig();
            }
        }
    }

    // Gets called for the torch thing
    @EventHandler
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.DRAGON_EGG) {
            plugin.getLogger().info("Intercepted entity drop item");
            plugin.getConfig().set("eggLocation", null);
            plugin.getConfig().set("eggTouchTimestamp", null);
            plugin.saveConfig();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            plugin.getLogger().info("Intercepted block place");
            plugin.updateEggLocation(event.getBlock().getLocation());
            plugin.getConfig().set("eggHolderUUID", null);
            plugin.getConfig().set("eggInventoryTimestamp", null);
            plugin.getConfig().set("eggTouchTimestamp", Instant.now().getEpochSecond());
            plugin.saveConfig();
        }
    }

    // It's technically possible to mine the egg like a normal block in certain situations. TIL.
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            plugin.getLogger().info("Intercepted block break");
            plugin.getConfig().set("eggLocation", null);
            plugin.getConfig().set("eggTouchTimestamp", null);
            plugin.saveConfig();
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getTo() == Material.DRAGON_EGG) {
            plugin.getLogger().info("Intercepted EntityChangeBlockEvent");
            plugin.updateEggLocation(event.getBlock().getLocation());
            // Should not be counted as touching the block since it might be able to be automated (I'm not really sure, so I'm playing it safe)
        }
    }

    //// Warning: Hot event!
    //@EventHandler
    //public void onBlockPhysicsCheck(BlockPhysicsEvent event) {
    //    if (event.getChangedType() == Material.DRAGON_EGG && event.getSourceBlock().getType() == Material.PISTON_HEAD) {
    //
    //        plugin.getLogger().info("Intercepted BlockPhysicsEvent");
    //        plugin.getConfig().set("eggLocation", null);
    //        plugin.getConfig().set("eggTouchTimestamp", null);
    //    }
    //}

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlock().getRelative(event.getDirection(), 1).getType() == Material.DRAGON_EGG) {
            plugin.getLogger().info("Intercepted piston break");
            plugin.getConfig().set("eggLocation", null);
            plugin.getConfig().set("eggTouchTimestamp", null);
            plugin.saveConfig();
        }
    }
}

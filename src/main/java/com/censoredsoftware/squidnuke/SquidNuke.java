package com.censoredsoftware.squidnuke;

import com.google.common.collect.Sets;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SquidNuke extends JavaPlugin implements Listener {
    protected static boolean blockDamage, playerDamage, nukeCreeper;
    protected static Set<UUID> squids = Sets.newHashSet();

    private static final Random random = new Random();

    /**
     * Generates an integer with a value between <code>min</code> and <code>max</code>.
     *
     * @param min the minimum value of the integer.
     * @param max the maximum value of the integer.
     * @return Integer
     */
    public static int generateIntRange(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * The Bukkit enable method.
     */
    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        blockDamage = getConfig().getBoolean("damage.block");
        playerDamage = getConfig().getBoolean("damage.player");
        nukeCreeper = getConfig().getBoolean("natural.nuke_creeper");

        loadListeners();
        loadCommands();

        getLogger().info("Successfully enabled.");
    }

    /**
     * The Bukkit disable method.
     */
    @Override
    public void onDisable() {
        getLogger().info("Successfully disabled.");
    }

    public void loadListeners() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    public void loadCommands() {
        getCommand("squidnuke").setExecutor(new SquidNukeCommand(this));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSquidDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (squids.contains(event.getEntity().getUniqueId())) {
            squids.remove(event.getEntity().getUniqueId());
            NukeControl.nuke(this, event.getEntity().getLocation(), blockDamage, playerDamage);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreeperSpawn(EntitySpawnEvent event) {
        if (nukeCreeper && event.getEntity().getType().equals(EntityType.CREEPER) && generateIntRange(1, 100) > 75) {
            Creeper creeper = (Creeper) event.getEntity();
            creeper.setPowered(true);
            creeper.setCustomName("Nuke");
            creeper.setCustomNameVisible(true);
            squids.add(creeper.getUniqueId());

        }
    }
}

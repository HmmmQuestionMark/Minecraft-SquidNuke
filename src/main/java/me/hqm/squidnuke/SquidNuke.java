package me.hqm.squidnuke;

import com.google.common.collect.Sets;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SquidNuke extends JavaPlugin implements Listener {
    static Set<UUID> SQUIDS = Sets.newHashSet();
    static boolean BLOCK_DAMAGE, PLAYER_DAMAGE, NUKE_CREEPER;
    private Random random = new Random();

    /**
     * The Bukkit enable method.
     */
    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        BLOCK_DAMAGE = getConfig().getBoolean("damage.block");
        PLAYER_DAMAGE = getConfig().getBoolean("damage.player");
        NUKE_CREEPER = getConfig().getBoolean("natural.nuke_creeper");

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

    void loadListeners() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    void loadCommands() {
        getCommand("squidnuke").setExecutor(new SquidNukeCommand(this));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSquidDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (SQUIDS.contains(event.getEntity().getUniqueId())) {
            SQUIDS.remove(event.getEntity().getUniqueId());
            NukeControl.nuke(this, event.getEntity().getLocation(), BLOCK_DAMAGE, PLAYER_DAMAGE);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreeperSpawn(EntitySpawnEvent event) {
        if (NUKE_CREEPER && event.getEntity().getType().equals(EntityType.CREEPER) && random.nextInt(100) + 1 > 75) {
            Creeper creeper = (Creeper) event.getEntity();
            creeper.setPowered(true);
            creeper.setCustomName("Nuke");
            creeper.setCustomNameVisible(true);
            SQUIDS.add(creeper.getUniqueId());
        }
    }
}

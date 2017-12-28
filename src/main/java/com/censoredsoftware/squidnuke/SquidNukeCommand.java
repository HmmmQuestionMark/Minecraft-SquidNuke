package com.censoredsoftware.squidnuke;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;

class SquidNukeCommand implements CommandExecutor {
    private final Plugin plugin;

    SquidNukeCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("squidnuke") && (args.length == 1 || args.length == 2) &&
                sender instanceof Player && sender.hasPermission("squidnuke.nuke")) {
            String target = args[0];
            EntityType nukeType;
            if (args.length == 1) nukeType = EntityType.SQUID;
            else {
                try {
                    nukeType = EntityType.valueOf(args[1].toUpperCase());
                } catch (Throwable thrown) {
                    nukeType = EntityType.SQUID;
                }
            }
            Player player = (Player) sender;
            if (target.equalsIgnoreCase("me")) {
                player.sendMessage(ChatColor.DARK_RED + " ☣ LAUNCH ☣");
                return nukePlayer(player, player, nukeType);
            } else if (target.equals("*")) {
                for (Player online : Bukkit.getOnlinePlayers())
                    nukePlayer(player, online, nukeType);
                player.sendMessage(ChatColor.DARK_RED + " ☣ LAUNCH ☣");
                return true;
            } else if (Bukkit.getPlayer(target) != null) {
                player.sendMessage(ChatColor.DARK_RED + " ☣ LAUNCH ☣");
                return nukePlayer(player, Bukkit.getPlayer(args[0]), nukeType);
            }

        }
        return false;
    }

    private boolean nukePlayer(final Player owner, final Player player, final EntityType type) {
        int count = 0;
        final Location target = player.getLocation();
        for (final Entity exists : player.getWorld().getEntities()) {
            if (exists instanceof LivingEntity) {
                if (exists.equals(player) || exists.getLocation().distance(target) < 30 || exists.getLocation().
                        distance(target) > 100)
                    continue;
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> launchNuke(false, owner, type,
                        new Location(exists.getWorld(), exists.getLocation().getX(), 0.0 +
                                exists.getLocation().getWorld().getHighestBlockYAt(exists.getLocation()),
                                exists.getLocation().getZ()), player), count * 2);
                count++;
            }
        }
        if (count > 0) {
            for (Entity nearby : player.getNearbyEntities(10, 10, 10)) {
                if (nearby instanceof Player && !NukeControl.isATarget((Player) nearby)) {
                    Player ohshit = (Player) nearby;
                    ohshit.sendMessage(ChatColor.RED + " ⚠ MISSILES DETECTED ⚠");
                }
            }
            player.sendMessage(ChatColor.RED + " ⚠ MISSILES DETECTED ⚠");
        }
        return true;
    }

    private void launchNuke(final boolean alert, final Player owner, final EntityType type, final Location launch,
                            final OfflinePlayer target) {
        warningSiren(false, launch, target.getPlayer().getLocation());
        for (int i = 6; i > 0; i--) {
            final int count = i - 1;
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (count == 0) {
                    LivingEntity squid = (LivingEntity) launch.getWorld().spawnEntity(launch, type);
                    squid.setNoDamageTicks(3);
                    squid.setCustomName("Nuke");
                    squid.setCustomNameVisible(true);
                    NukeControl control = new NukeControl(plugin, squid, launch, target, NukeControl.
                            getTarget(target.getPlayer()));
                    control.startTravel();
                    SquidNuke.squids.add(squid.getUniqueId());
                } else if (alert) owner.sendMessage(ChatColor.GREEN + "" + count + "...");
            }, (6 - i) * 20);
        }
    }

    private void warningSiren(final boolean alertLaunch, final Location launch, final Location target) {
        for (int i = 0; i < 4; i++) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (alertLaunch) launch.getWorld().playSound(launch, Sound.AMBIENT_CAVE, 2F, 2F);
                target.getWorld().playSound(target, Sound.AMBIENT_CAVE, 2F, 2F);
            }, i * 30);
        }
    }
}

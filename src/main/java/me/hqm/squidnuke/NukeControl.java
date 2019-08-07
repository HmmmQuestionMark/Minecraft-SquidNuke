package me.hqm.squidnuke;

import com.google.common.collect.Sets;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Set;

class NukeControl {
    private static Set<String> targets = Sets.newHashSet();

    private Plugin plugin;
    private LivingEntity squid;
    private OfflinePlayer target;
    private Stage stage;
    private Location startPoint, checkpoint, targetLocation;

    NukeControl(Plugin plugin, LivingEntity squid, Location launchPoint, OfflinePlayer target,
                       Location targetLocation) {
        this.plugin = plugin;
        this.squid = squid;
        this.target = target;
        stage = Stage.LAUNCH;
        startPoint = launchPoint;
        this.targetLocation = targetLocation;
        targets.add(target.getName());
        calculateNextCheckpoint();
    }

    LivingEntity getNuke() {
        return this.squid;
    }

    Location getStartPoint() {
        return startPoint;
    }

    Location getCheckPoint() {
        return checkpoint;
    }

    OfflinePlayer getTarget() {
        return target;
    }

    Stage getStage() {
        return this.stage;
    }

    void startTravel() {
        Bukkit.getScheduler().runTask(plugin, new TravelStage(this));
    }

    private void calculateNextCheckpoint() {
        this.stage = stage.getNext();
        if (target.isOnline()) targetLocation = getTarget(target.getPlayer());
        switch (stage) {
            case ASCENT: {
                this.checkpoint = new Location(startPoint.getWorld(), startPoint.getX() > targetLocation.getX() ?
                        startPoint.getX() - 10 : startPoint.getX() + 10, startPoint.getY() + 50 > 248 ? 248 :
                        startPoint.getY() + 50, startPoint.getZ() > targetLocation.getZ() ? startPoint.getZ() - 10 :
                        startPoint.getZ() + 10);
                break;
            }
            case TRAVEL: {
                this.checkpoint = new Location(targetLocation.getWorld(), checkpoint.getX() > targetLocation.getX() ?
                        targetLocation.getX() + 10 : targetLocation.getX() - 10, checkpoint.getY() + 50 > 248 ? 248 :
                        checkpoint.getY() + 50, checkpoint.getZ() > targetLocation.getZ() ? targetLocation.getZ() + 10 :
                        targetLocation.getZ() - 10);
                break;
            }
            case DECENT: {
                this.checkpoint = new Location(targetLocation.getWorld(), targetLocation.getX(), targetLocation.getY(),
                        targetLocation.getZ());
                break;
            }
        }
    }

    static Location getTarget(Player target) {
        return new Location(target.getWorld(), target.getLocation().getX(), 0.0 + target.getWorld().
                getHighestBlockYAt(target.getLocation()), target.getLocation().getZ());
    }

    static boolean isATarget(OfflinePlayer player) {
        return targets.contains(player.getName());
    }

    static void nuke(Plugin plugin, final Location target, final boolean block, final boolean player) {
        for (int i = 1; i < 25; i++) {
            final int k = i;
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> nukeEffects(target, 30 * k, (double) k / 2, block, player), i);
        }
    }

    private static void nukeEffects(Location target, int particles, double offSetY, boolean block, boolean player) {

        if (player)
            target.getWorld().createExplosion(target.getX(), target.getY() + 3.0 + offSetY, target.getZ(),
                    6F, block, block);
        else {
            target.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, target, 1);
            target.getWorld().playSound(target, Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F);
        }
        target.getWorld().playSound(target, Sound.AMBIENT_CAVE, 1F, 1F);
        target.getWorld().spawnParticle(Particle.CLOUD, target, particles, 0, 3D + offSetY, 3D, 5D);
        target.getWorld().spawnParticle(Particle.LAVA, target, particles, 0, 3D + offSetY, 0, 5D);
        target.getWorld().spawnParticle(Particle.SMOKE_LARGE, target, particles, 0, 3D + offSetY, 0, 5D);
        target.getWorld().spawnParticle(Particle.FLAME, target, particles, 0, 3D + offSetY, offSetY, 5D);
    }

    public enum Stage {
        LAUNCH, ASCENT, TRAVEL, DECENT;

        public Stage getNext() {
            return get(ordinal() + 1);
        }

        private static Stage get(int order) {
            try {
                return values()[order];
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    static class TravelStage extends BukkitRunnable {
        private NukeControl control;

        TravelStage(NukeControl control) {
            this.control = control;
        }

        @Override
        public void run() {
            if (control.getNuke() == null || control.getNuke().isDead()) return;
            if (control.getNuke().getLocation().distance(control.getCheckPoint()) < 4) {
                if (!control.getStage().equals(Stage.DECENT)) startNextTravelStage();
                else {
                    SquidNuke.SQUIDS.remove(control.getNuke().getUniqueId());
                    NukeControl.nuke(control.plugin, control.getNuke().getLocation(), SquidNuke.BLOCK_DAMAGE,
                            SquidNuke.PLAYER_DAMAGE);
                    control.getNuke().remove();
                    targets.remove(control.getTarget().getName());
                }
            } else {
                go();
                if (!control.getStage().equals(Stage.DECENT)) control.getNuke().setNoDamageTicks(2);
                Bukkit.getScheduler().scheduleSyncDelayedTask(control.plugin, new TravelStage(control), 1);
            }
        }

        void go() {
            if (control.getNuke().getLocation().getBlockY() > 256) control.getNuke().teleport(control.getCheckPoint());
            Vector direction = control.getCheckPoint().toVector().subtract(control.getNuke().getLocation().toVector());
            control.getNuke().setVelocity(direction);
        }

        void startNextTravelStage() {
            control.calculateNextCheckpoint();
            Bukkit.getScheduler().scheduleSyncDelayedTask(control.plugin, new TravelStage(control), 1);
        }
    }
}

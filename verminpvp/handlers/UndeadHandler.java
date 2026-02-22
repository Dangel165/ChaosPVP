package com.verminpvp.handlers;

import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.models.ClassData;
import com.verminpvp.models.ClassType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Undead class
 * 
 * Passive: On death, revive with full HP (once per game)
 * On revive: Take 1 instant damage (0.5 hearts) every 0.5 seconds until death
 * On revive: Gain Speed I, Strength II, Resistance V until death
 */
public class UndeadHandler implements Listener {
    
    private final Plugin plugin;
    private final ClassManager classManager;
    private final GameManager gameManager;
    private final DamageHandler damageHandler;
    
    // Track if player has used revive
    private final Map<UUID, Boolean> hasRevived = new HashMap<>();
    
    // Track damage tasks
    private final Map<UUID, BukkitTask> damageTasks = new HashMap<>();
    
    // Track revival time (to give grace period before damage starts)
    private final Map<UUID, Long> revivalTime = new HashMap<>();
    
    public UndeadHandler(Plugin plugin, ClassManager classManager, GameManager gameManager,
                        DamageHandler damageHandler) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.gameManager = gameManager;
        this.damageHandler = damageHandler;
    }
    
    /**
     * Initialize player - set revive status to false
     */
    public void initializePlayer(Player player) {
        hasRevived.put(player.getUniqueId(), false);
    }
    
    /**
     * Check if player has already revived
     */
    public boolean hasPlayerRevived(Player player) {
        return hasRevived.getOrDefault(player.getUniqueId(), false);
    }
    
    /**
     * Handle player death - revive if first death
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ClassData data = classManager.getClassData(player);
        
        if (data == null || data.getClassType() != ClassType.UNDEAD) {
            return;
        }
        
        // Only work in game or practice mode
        boolean inPracticeMode = gameManager.isInPracticeMode(player);
        if (!gameManager.isGameActive() && !inPracticeMode) {
            return;
        }
        
        // Check if player is in spectator mode - if so, don't process
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }
        
        // Check if already revived
        boolean alreadyRevived = hasRevived.getOrDefault(player.getUniqueId(), false);
        if (alreadyRevived) {
            // Second death - stop damage task to prevent death loop
            BukkitTask task = damageTasks.remove(player.getUniqueId());
            if (task != null) {
                task.cancel();
            }
            
            // Remove buffs
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            
            // In practice mode, just let them die normally (don't end practice)
            // They can use /연습종료 to end practice manually
            player.sendMessage("§c사망했습니다!");
            return;
        }
        
        // First death - CANCEL EVENT and handle revival manually
        event.setCancelled(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        
        // Save death location
        final Location deathLocation = player.getLocation().clone();
        
        // Mark as revived
        hasRevived.put(player.getUniqueId(), true);
        
        // Set revival time (for grace period)
        revivalTime.put(player.getUniqueId(), System.currentTimeMillis());
        
        player.sendMessage("§c사망! §a부활 중...");
        
        Bukkit.getLogger().info("[VerminPVP] Undead first death for " + player.getName() + ", reviving at death location");
        
        // Revive player immediately (next tick)
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            
            // Restore full health
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(maxHealth);
            
            // Ensure game mode is adventure (not spectator)
            if (player.getGameMode() != org.bukkit.GameMode.ADVENTURE) {
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            }
            
            // Teleport to death location
            player.teleport(deathLocation);
            
            // Apply buffs
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 
                Integer.MAX_VALUE, 0, false, false, false)); // Speed I
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 
                Integer.MAX_VALUE, 1, false, false, false)); // Strength II
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 
                Integer.MAX_VALUE, 4, false, false, false)); // Resistance V
            
            // Visual effect
            player.getWorld().spawnParticle(org.bukkit.Particle.SOUL, 
                deathLocation, 50, 0.5, 1.0, 0.5, 0.1);
            player.playSound(deathLocation, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.5f);
            
            player.sendTitle("§c§l부활!", "§7언데드의 힘", 10, 40, 10);
            player.sendMessage("§a부활했습니다! §c지속 피해가 시작됩니다...");
            
            // Start damage task
            startDamageTask(player);
            
            Bukkit.getLogger().info("[VerminPVP] Undead revival complete for " + player.getName());
        });
    }
    
    // PlayerRespawnEvent handler removed - we handle revival manually in PlayerDeathEvent
    
    /**
     * Start continuous damage task
     */
    private void startDamageTask(Player player) {
        // Cancel existing task if any
        BukkitTask existingTask = damageTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Start new damage task (1 damage every 0.25 seconds)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                BukkitTask t = damageTasks.remove(player.getUniqueId());
                if (t != null) {
                    t.cancel();
                }
                return;
            }
            
            // Check if player is dead or in spectator mode - stop task
            if (player.isDead() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                BukkitTask t = damageTasks.remove(player.getUniqueId());
                if (t != null) {
                    t.cancel();
                }
                return;
            }
            
            // Check if still in game/practice mode
            if (!gameManager.isGameActive() && !gameManager.isInPracticeMode(player)) {
                BukkitTask t = damageTasks.remove(player.getUniqueId());
                if (t != null) {
                    t.cancel();
                }
                return;
            }
            
            // Check if player is still Undead class
            ClassData data = classManager.getClassData(player);
            if (data == null || data.getClassType() != ClassType.UNDEAD) {
                BukkitTask t = damageTasks.remove(player.getUniqueId());
                if (t != null) {
                    t.cancel();
                }
                return;
            }
            
            // Check if player has revived
            boolean revived = hasRevived.getOrDefault(player.getUniqueId(), false);
            if (!revived) {
                BukkitTask t = damageTasks.remove(player.getUniqueId());
                if (t != null) {
                    t.cancel();
                }
                return;
            }
            
            // Check grace period (3 seconds after revival)
            Long reviveTime = revivalTime.get(player.getUniqueId());
            if (reviveTime != null) {
                long timeSinceRevival = System.currentTimeMillis() - reviveTime;
                if (timeSinceRevival < 3000) { // 3 seconds grace period
                    return; // Skip damage during grace period
                }
            }
            
            // Apply 1 damage (0.5 hearts)
            damageHandler.applyInstantDamage(player, 1.0);
            
            // Visual effect
            player.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, 
                player.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.1);
            
        }, 5L, 5L); // Every 0.25 seconds (5 ticks)
        
        damageTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Cleanup player data
     */
    public void cleanupPlayer(Player player) {
        hasRevived.remove(player.getUniqueId());
        revivalTime.remove(player.getUniqueId());
        
        BukkitTask task = damageTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        
        // Remove buffs
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
    }
    
    /**
     * Cleanup all data
     */
    public void cleanupAll() {
        hasRevived.clear();
        revivalTime.clear();
        
        for (BukkitTask task : damageTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        damageTasks.clear();
    }
}

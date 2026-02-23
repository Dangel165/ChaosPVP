package com.verminpvp.handlers;

import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.AbilityIds;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Stamper class
 * 
 * Passive:
 * - No fall damage
 * - Landing from fall height: 4 damage to enemies within 4 blocks (8 if hitbox overlaps)
 * 
 * Stamp (도장 찍기):
 * - Right-click: Launch 20 blocks upward
 * - Cooldown: 12 seconds
 * 
 * Dive (다이브):
 * - Gained every 120 seconds (max 1)
 * - Right-click: Launch 80 blocks upward
 * - Landing: 12 damage to enemies within 20 blocks (24 if hitbox overlaps)
 * - Consumes item after use
 */
public class StamperHandler implements Listener {
    
    private final Plugin plugin;
    private final ClassManager classManager;
    private final CooldownManager cooldownManager;
    private final ItemProvider itemProvider;
    private final DamageHandler damageHandler;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    
    // Track fall heights
    private final Map<UUID, Double> fallStartHeights = new HashMap<>();
    private final Map<UUID, Boolean> isDiving = new HashMap<>();
    
    // Track dive gain tasks
    private final Map<UUID, BukkitTask> diveGainTasks = new HashMap<>();
    
    // Constants
    private static final int STAMP_COOLDOWN = 20; // seconds (nerfed from 12)
    private static final int DIVE_GAIN_INTERVAL = 200; // seconds (nerfed from 120)
    private static final double STAMP_LAUNCH_HEIGHT = 10.0; // blocks (nerfed from 20.0)
    private static final double DIVE_LAUNCH_HEIGHT = 40.0; // blocks (nerfed from 80.0)
    private static final double PASSIVE_DAMAGE_RANGE = 4.0; // blocks
    private static final double DIVE_DAMAGE_RANGE = 20.0; // blocks
    private static final double PASSIVE_DAMAGE = 6.0; // buffed from 4.0
    private static final double DIVE_DAMAGE = 16.0; // buffed from 12.0
    private static final double HITBOX_OVERLAP_DISTANCE = 1.5; // blocks
    
    public StamperHandler(Plugin plugin, ClassManager classManager, CooldownManager cooldownManager,
                         ItemProvider itemProvider, DamageHandler damageHandler, TeamManager teamManager,
                         GameManager gameManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.cooldownManager = cooldownManager;
        this.itemProvider = itemProvider;
        this.damageHandler = damageHandler;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }
    
    /**
     * Start dive gain system for a Stamper player
     */
    public void startDiveGainSystem(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        if (classManager.getPlayerClass(player) != ClassType.STAMPER) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Cancel existing task
        stopDiveGainSystem(player);
        
        // Schedule dive gain every 120 seconds
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Verify player is still online and game is active
            if (!player.isOnline()) {
                stopDiveGainSystem(player);
                return;
            }
            
            if (!gameManager.isGameActive() && !gameManager.isInPracticeMode(player)) {
                stopDiveGainSystem(player);
                return;
            }
            
            // Verify player is still a Stamper
            if (classManager.getPlayerClass(player) != ClassType.STAMPER) {
                stopDiveGainSystem(player);
                return;
            }
            
            // Check if player already has dive (max 1)
            if (hasDive(player)) {
                return; // Already has one, don't give another
            }
            
            // Give dive item
            ItemStack dive = itemProvider.createSpecialItem(ClassType.STAMPER, "dive");
            if (dive != null) {
                player.getInventory().addItem(dive);
                player.sendMessage("§6다이브를 획득했습니다!");
            }
        }, DIVE_GAIN_INTERVAL * 20L, DIVE_GAIN_INTERVAL * 20L);
        
        diveGainTasks.put(playerId, task);
    }
    
    /**
     * Stop dive gain system for a player
     */
    public void stopDiveGainSystem(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        BukkitTask task = diveGainTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Check if player has dive item
     */
    private boolean hasDive(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String itemId = itemProvider.getItemId(item);
                if ("dive".equals(itemId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Handle player interactions (right-click for Stamp and Dive)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (classManager.getPlayerClass(player) != ClassType.STAMPER) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        
        String itemId = itemProvider.getItemId(item);
        if (itemId == null) {
            return;
        }
        
        if (itemId.equals("stamp")) {
            event.setCancelled(true);
            handleStamp(player);
        } else if (itemId.equals("dive")) {
            event.setCancelled(true);
            handleDive(player, item);
        }
    }
    
    /**
     * Handle Stamp ability (도장 찍기)
     */
    private void handleStamp(Player player) {
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.STAMP)) {
            double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.STAMP);
            player.sendMessage("§c도장 찍기 쿨다운: " + String.format("%.1f", remaining) + "초");
            return;
        }
        
        // Launch player upward
        Vector velocity = new Vector(0, STAMP_LAUNCH_HEIGHT / 5.0, 0); // Divide by 5 for reasonable velocity
        player.setVelocity(velocity);
        
        // Set cooldown
        cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.STAMP, STAMP_COOLDOWN);
        
        // Mark as not diving (regular stamp)
        isDiving.put(player.getUniqueId(), false);
        
        player.sendMessage("§a도장 찍기 사용!");
    }
    
    /**
     * Handle Dive ability (다이브)
     */
    private void handleDive(Player player, ItemStack item) {
        // Launch player upward (much higher than stamp)
        Vector velocity = new Vector(0, DIVE_LAUNCH_HEIGHT / 5.0, 0);
        player.setVelocity(velocity);
        
        // Mark as diving
        isDiving.put(player.getUniqueId(), true);
        
        // Remove dive item
        item.setAmount(item.getAmount() - 1);
        
        player.sendMessage("§6다이브 사용!");
    }
    
    /**
     * Track fall heights
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (classManager.getPlayerClass(player) != ClassType.STAMPER) {
            return;
        }
        
        // Update fall start height if player is in air
        if (!player.isOnGround()) {
            UUID playerId = player.getUniqueId();
            double currentHeight = player.getLocation().getY();
            
            // Update to highest point
            if (!fallStartHeights.containsKey(playerId) || currentHeight > fallStartHeights.get(playerId)) {
                fallStartHeights.put(playerId, currentHeight);
            }
        }
    }
    
    /**
     * Handle fall damage immunity and landing damage
     */
    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        if (classManager.getPlayerClass(player) != ClassType.STAMPER) {
            return;
        }
        
        // Cancel fall damage
        event.setCancelled(true);
        
        UUID playerId = player.getUniqueId();
        
        // Check if this was a significant fall
        Double startHeight = fallStartHeights.get(playerId);
        if (startHeight == null) {
            return;
        }
        
        double fallDistance = startHeight - player.getLocation().getY();
        
        // Clear fall height tracking
        fallStartHeights.remove(playerId);
        
        // Minimum fall distance to trigger landing damage (3 blocks)
        if (fallDistance < 3.0) {
            isDiving.remove(playerId);
            return;
        }
        
        // Check if this was a dive
        Boolean diving = isDiving.remove(playerId);
        boolean wasDiving = diving != null && diving;
        
        // Apply landing damage
        applyLandingDamage(player, wasDiving);
    }
    
    /**
     * Apply landing damage to nearby enemies
     */
    private void applyLandingDamage(Player player, boolean wasDiving) {
        Location landingLoc = player.getLocation();
        double range = wasDiving ? DIVE_DAMAGE_RANGE : PASSIVE_DAMAGE_RANGE;
        double baseDamage = wasDiving ? DIVE_DAMAGE : PASSIVE_DAMAGE;
        
        int hitCount = 0;
        
        // Find all entities within range
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity == player) {
                continue;
            }
            
            LivingEntity target = (LivingEntity) entity;
            
            // Check team restrictions (unless in practice mode)
            if (target instanceof Player) {
                Player targetPlayer = (Player) target;
                
                // Skip if same team in team mode (not practice mode)
                if (gameManager.getGameMode() == com.verminpvp.models.GameMode.TEAM && !gameManager.isInPracticeMode(player)) {
                    Team playerTeam = teamManager.getPlayerTeam(player);
                    Team targetTeam = teamManager.getPlayerTeam(targetPlayer);
                    if (playerTeam != null && playerTeam == targetTeam) {
                        continue;
                    }
                }
            }
            
            // Check distance for hitbox overlap (2x damage)
            double distance = landingLoc.distance(target.getLocation());
            double damage = baseDamage;
            
            if (distance <= HITBOX_OVERLAP_DISTANCE) {
                damage *= 2.0; // Double damage if hitbox overlaps
            }
            
            // Apply damage
            damageHandler.applyInstantDamage(target, damage);
            hitCount++;
        }
        
        if (hitCount > 0) {
            if (wasDiving) {
                player.sendMessage("§6다이브 착지! §e" + hitCount + "명 타격");
            } else {
                player.sendMessage("§a착지 피해! §e" + hitCount + "명 타격");
            }
        }
    }
    
    /**
     * Cleanup all Stamper tasks
     */
    public void cleanupAll() {
        // Cancel all dive gain tasks
        for (BukkitTask task : diveGainTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        diveGainTasks.clear();
        
        // Clear tracking maps
        fallStartHeights.clear();
        isDiving.clear();
    }
    
    /**
     * Cleanup Stamper tasks for a specific player
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel dive gain task for this player
        BukkitTask task = diveGainTasks.get(playerId);
        if (task != null) {
            task.cancel();
            diveGainTasks.remove(playerId);
        }
        
        // Clear tracking for this player
        fallStartHeights.remove(playerId);
        isDiving.remove(playerId);
    }
}

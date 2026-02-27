package com.verminpvp.handlers;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.models.AbilityIds;
import com.verminpvp.models.ClassType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Marathoner class abilities
 * 
 * Passive:
 * - Sprint for 5 seconds: gain Speed I
 * - Continue sprinting for 3.5 more seconds: Speed II
 * - Continue sprinting for 3.5 more seconds: Speed III
 * - Continue sprinting for 3.5 more seconds: Speed IV + Strength II
 * - Stop sprinting, take damage, or jump: lose all speed and strength buffs
 * 
 * Active Ability:
 * - Crouching Start: Right-click to gain Speed III for 0.5 seconds, 5 second cooldown
 */
public class MarathonerHandler implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    private final CooldownManager cooldownManager;
    private final ItemProvider itemProvider;
    
    // Track sprint start time for each player
    private final Map<UUID, Long> sprintStartTime = new HashMap<>();
    
    // Track current speed level for each player
    private final Map<UUID, Integer> speedLevel = new HashMap<>();
    
    // Track monitoring tasks
    private final Map<UUID, BukkitTask> monitoringTasks = new HashMap<>();
    
    // Time thresholds in milliseconds
    private static final long SPEED_1_TIME = 5000;      // 5 seconds
    private static final long SPEED_2_TIME = 8500;      // 5 + 3.5 seconds
    private static final long SPEED_3_TIME = 12000;     // 5 + 3.5 + 3.5 seconds
    private static final long SPEED_4_TIME = 15500;     // 5 + 3.5 + 3.5 + 3.5 seconds
    
    public MarathonerHandler(VerminPVP plugin, ClassManager classManager,
                             CooldownManager cooldownManager, ItemProvider itemProvider) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.cooldownManager = cooldownManager;
        this.itemProvider = itemProvider;
    }
    
    /**
     * Check if game is active (including practice mode)
     */
    private boolean isGameActive() {
        return VerminPVP.getInstance().getGameManager().isGameActive() ||
               VerminPVP.getInstance().getGameManager().isInPracticeMode(null);
    }
    
    /**
     * Handle Crouching Start ability
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (classManager.getPlayerClass(player) != ClassType.MARATHONER) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        String itemId = itemProvider.getItemId(item);
        if (itemId == null) return;
        
        if (itemId.equals("crouching_start")) {
            event.setCancelled(true);
            handleCrouchingStart(player);
        }
    }
    
    /**
     * Handle Crouching Start ability
     */
    private void handleCrouchingStart(Player player) {
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.CROUCHING_START)) {
            long remaining = (long) cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.CROUCHING_START);
            player.sendMessage("§c크라우칭 스타트 쿨다운: " + remaining + "초");
            return;
        }
        
        // Apply Speed III for 0.5 seconds (10 ticks)
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED,
            10, // 0.5 seconds
            2, // Speed III = amplifier 2
            false,
            false,
            true
        ));
        
        // Set cooldown
        cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.CROUCHING_START, 5);
        
        // Show cooldown display
        VerminPVP.getInstance().getCooldownDisplay().showCooldown(player, AbilityIds.CROUCHING_START, "크라우칭 스타트", 5.0);
        
        player.sendMessage("§a크라우칭 스타트 사용!");
    }
    
    /**
     * Handle sprint toggle
     */
    @EventHandler
    public void onSprintToggle(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (classManager.getPlayerClass(player) != ClassType.MARATHONER) return;
        
        if (event.isSprinting()) {
            // Player started sprinting
            startSprinting(player);
        } else {
            // Player stopped sprinting
            stopSprinting(player);
        }
    }
    
    /**
     * Handle player movement to detect jumps
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (classManager.getPlayerClass(player) != ClassType.MARATHONER) return;
        
        // Only check if player has active speed buffs
        if (!speedLevel.containsKey(player.getUniqueId())) {
            // If player is sprinting but has no active tracking, start it
            if (player.isSprinting()) {
                startSprinting(player);
            }
            return; // No active buffs, no need to check
        }
        
        // Check if player jumped (Y velocity > 0.4 indicates jump)
        if (event.getTo() != null && event.getFrom() != null) {
            double yDiff = event.getTo().getY() - event.getFrom().getY();
            
            // If player is sprinting and jumped, reset buffs
            if (player.isSprinting() && yDiff > 0.4) {
                stopSprinting(player);
                player.sendMessage("§c점프로 인해 신속 효과가 사라졌습니다!");
                
                // Restart sprinting if player is still sprinting after a short delay
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && player.isSprinting() && 
                        classManager.getPlayerClass(player) == ClassType.MARATHONER) {
                        startSprinting(player);
                    }
                }, 1L);
            }
        }
    }
    
    /**
     * Handle damage to reset buffs
     */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        if (classManager.getPlayerClass(player) != ClassType.MARATHONER) return;
        
        // Only check if player has active speed buffs
        if (!speedLevel.containsKey(player.getUniqueId())) {
            return; // No active buffs, no need to check
        }
        
        // Remove buffs on damage
        stopSprinting(player);
        player.sendMessage("§c피격으로 인해 신속 효과가 사라졌습니다!");
        
        // Restart sprinting if player is still sprinting after a short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.isSprinting() && 
                classManager.getPlayerClass(player) == ClassType.MARATHONER) {
                startSprinting(player);
            }
        }, 1L);
    }
    
    /**
     * Start sprinting and begin monitoring
     */
    private void startSprinting(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Record sprint start time
        sprintStartTime.put(playerId, System.currentTimeMillis());
        speedLevel.put(playerId, 0);
        
        // Cancel existing monitoring task if any
        if (monitoringTasks.containsKey(playerId)) {
            monitoringTasks.get(playerId).cancel();
        }
        
        // Start monitoring task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || classManager.getPlayerClass(player) != ClassType.MARATHONER) {
                    cancel();
                    cleanup(playerId);
                    return;
                }
                
                // If player stopped sprinting, cancel
                if (!player.isSprinting()) {
                    cancel();
                    return;
                }
                
                updateSpeedLevel(player);
            }
        }.runTaskTimer(plugin, 0L, 2L); // Check every 0.1 seconds
        
        monitoringTasks.put(playerId, task);
    }
    
    /**
     * Stop sprinting and remove all buffs
     */
    private void stopSprinting(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel monitoring task
        if (monitoringTasks.containsKey(playerId)) {
            monitoringTasks.get(playerId).cancel();
            monitoringTasks.remove(playerId);
        }
        
        // Remove all buffs
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        
        // Clear tracking data
        sprintStartTime.remove(playerId);
        speedLevel.remove(playerId);
    }
    
    /**
     * Update speed level based on sprint duration
     */
    private void updateSpeedLevel(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!sprintStartTime.containsKey(playerId)) return;
        
        long sprintDuration = System.currentTimeMillis() - sprintStartTime.get(playerId);
        int currentLevel = speedLevel.get(playerId);
        int newLevel = currentLevel;
        
        // Determine new speed level based on duration
        if (sprintDuration >= SPEED_4_TIME && currentLevel < 4) {
            newLevel = 4;
            applySpeedEffect(player, 4);
            applyStrengthEffect(player);
            player.sendMessage("§a§l신속 IV + 힘 II 획득!");
        } else if (sprintDuration >= SPEED_3_TIME && currentLevel < 3) {
            newLevel = 3;
            applySpeedEffect(player, 3);
            player.sendMessage("§a신속 III 획득!");
        } else if (sprintDuration >= SPEED_2_TIME && currentLevel < 2) {
            newLevel = 2;
            applySpeedEffect(player, 2);
            player.sendMessage("§a신속 II 획득!");
        } else if (sprintDuration >= SPEED_1_TIME && currentLevel < 1) {
            newLevel = 1;
            applySpeedEffect(player, 1);
            player.sendMessage("§a신속 I 획득!");
        }
        
        speedLevel.put(playerId, newLevel);
    }
    
    /**
     * Apply speed effect
     */
    private void applySpeedEffect(Player player, int level) {
        // Remove existing speed effect
        player.removePotionEffect(PotionEffectType.SPEED);
        
        // Apply new speed effect (infinite duration while sprinting)
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED,
            Integer.MAX_VALUE,
            level - 1, // Level 1 = amplifier 0
            false,
            false,
            true
        ));
    }
    
    /**
     * Apply strength effect (only at Speed IV)
     */
    private void applyStrengthEffect(Player player) {
        // Remove existing strength effect
        player.removePotionEffect(PotionEffectType.STRENGTH);
        
        // Apply Strength II (infinite duration while sprinting)
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.STRENGTH,
            Integer.MAX_VALUE,
            1, // Strength II = amplifier 1
            false,
            false,
            true
        ));
    }
    
    /**
     * Cleanup player data
     */
    private void cleanup(UUID playerId) {
        if (monitoringTasks.containsKey(playerId)) {
            monitoringTasks.get(playerId).cancel();
            monitoringTasks.remove(playerId);
        }
        sprintStartTime.remove(playerId);
        speedLevel.remove(playerId);
    }
    
    /**
     * Cleanup all data (called on plugin disable)
     */
    public void cleanupAll() {
        for (BukkitTask task : monitoringTasks.values()) {
            task.cancel();
        }
        monitoringTasks.clear();
        sprintStartTime.clear();
        speedLevel.clear();
    }
}

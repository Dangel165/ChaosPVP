package com.verminpvp.handlers;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.models.AbilityIds;
import com.verminpvp.models.ClassType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Shield Soldier class abilities
 * 
 * NEW DESIGN:
 * Passive: 12s no damage → 4 absorption hearts (non-stacking, 60s duration)
 * No starting stone sword
 * Health ≤ 6 → Golden Shield
 * 
 * 3 Starting Shields:
 * - Red Shield: Block → 3 damage to attacker (5s cooldown)
 * - Blue Shield: Block → 4 absorption hearts non-stacking (10s duration, 11s cooldown)
 * - Basic Shield: Block → Weakness I 2s to attacker (3.5s cooldown)
 * 
 * Golden Shield: Block → Strength II 8s + Speed II 8s + 20 absorption hearts 8s, then disappears
 */
public class ShieldSoldierHandler implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    private final CooldownManager cooldownManager;
    private final ItemProvider itemProvider;
    private final EffectApplicator effectApplicator;
    private final DamageHandler damageHandler;
    
    // Track last damage time for absorption passive
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    
    // Track shield cooldowns per shield type
    private final Map<UUID, Map<String, Long>> shieldCooldowns = new HashMap<>();
    
    // Track shield deploy (blocking) cooldowns per shield type
    private final Map<UUID, Map<String, Long>> shieldDeployCooldowns = new HashMap<>();
    
    // Track if Golden Shield has been triggered
    private final Map<UUID, Boolean> goldenShieldTriggered = new HashMap<>();
    
    // Track passive scheduler tasks
    private final Map<UUID, BukkitTask> passiveTasks = new HashMap<>();
    
    public ShieldSoldierHandler(VerminPVP plugin, ClassManager classManager,
                                 CooldownManager cooldownManager, ItemProvider itemProvider,
                                 EffectApplicator effectApplicator, DamageHandler damageHandler) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.cooldownManager = cooldownManager;
        this.itemProvider = itemProvider;
        this.effectApplicator = effectApplicator;
        this.damageHandler = damageHandler;
    }
    
    /**
     * Start absorption passive scheduler for a Shield Soldier
     */
    public void startAbsorptionPassive(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing task if any
        stopAbsorptionPassive(player);
        
        // Initialize last damage time to now
        lastDamageTime.put(playerId, System.currentTimeMillis());
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || classManager.getPlayerClass(player) != ClassType.SHIELD_SOLDIER) {
                    cancel();
                    passiveTasks.remove(playerId);
                    return;
                }
                
                // Check if 12 seconds have passed since last damage
                long currentTime = System.currentTimeMillis();
                long lastDamage = lastDamageTime.getOrDefault(playerId, 0L);
                
                if (currentTime - lastDamage >= 12000) { // 12 seconds
                    // Grant 2 absorption hearts if not already present (non-stacking)
                    if (!effectApplicator.hasAbsorptionFrom(player, "shield_soldier_passive")) {
                        effectApplicator.applyAbsorption(player, 2, 1200, "shield_soldier_passive"); // 60s = 1200 ticks
                        player.sendMessage("§e패시브: 흡수 하트 2개 획득!");
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
        
        passiveTasks.put(playerId, task);
    }
    
    /**
     * Stop absorption passive scheduler for a player
     */
    public void stopAbsorptionPassive(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = passiveTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Track damage time for absorption passive and Golden Shield trigger
     */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.isCancelled()) return;
        
        Player player = (Player) event.getEntity();
        if (classManager.getPlayerClass(player) != ClassType.SHIELD_SOLDIER) return;
        
        // Update last damage time (resets 12s passive timer)
        lastDamageTime.put(player.getUniqueId(), System.currentTimeMillis());
        
        // Check for Golden Shield trigger (health ≤ 12 after damage)
        double healthAfter = player.getHealth() - event.getFinalDamage();
        if (healthAfter <= 12.0 && !goldenShieldTriggered.getOrDefault(player.getUniqueId(), false)) {
            // Check if player has golden shield in inventory
            if (hasGoldenShield(player)) {
                // Schedule Golden Shield activation after damage is applied
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline() && player.getHealth() <= 12.0) {
                        activateGoldenShield(player);
                        goldenShieldTriggered.put(player.getUniqueId(), true);
                    }
                });
            } else {
                // Give golden shield if not already triggered
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline() && player.getHealth() <= 12.0) {
                        giveGoldenShield(player);
                        goldenShieldTriggered.put(player.getUniqueId(), true);
                    }
                });
            }
        }
    }
    
    /**
     * Handle shield blocking
     */
    @EventHandler
    public void onShieldBlock(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player victim = (Player) event.getEntity();
        
        if (classManager.getPlayerClass(victim) != ClassType.SHIELD_SOLDIER) return;
        
        // Check if victim is blocking
        if (!victim.isBlocking()) return;
        
        // Get shield type
        ItemStack shield = getActiveShield(victim);
        if (shield == null) return;
        
        String shieldId = itemProvider.getItemId(shield);
        if (shieldId == null) return;
        
        // Check shield deploy (blocking) cooldown - 6 seconds for all shields EXCEPT golden shield
        if (!shieldId.equals("golden_shield") && isShieldDeployOnCooldown(victim, shieldId)) {
            // Cancel the block - shield cannot be deployed during cooldown
            event.setCancelled(false); // Let damage through
            long remainingMs = getRemainingDeployCooldown(victim, shieldId);
            double remainingSec = remainingMs / 1000.0;
            
            // Get shield name for display
            String shieldName = "";
            switch (shieldId) {
                case "red_shield":
                    shieldName = "빨간 방패";
                    break;
                case "blue_shield":
                    shieldName = "파란 방패";
                    break;
                case "basic_shield":
                    shieldName = "기본 방패";
                    break;
            }
            
            victim.sendMessage(String.format("§c%s 전개 쿨타임: %.1f초", shieldName, remainingSec));
            return;
        }
        
        // Set deploy cooldown (6 seconds for all shields EXCEPT golden shield)
        if (!shieldId.equals("golden_shield")) {
            setShieldDeployCooldown(victim, shieldId, 6000);
        }
        
        // Check shield-specific cooldown
        if (isShieldOnCooldown(victim, shieldId)) {
            // Cancel the block - shield cannot be used during cooldown
            event.setCancelled(false); // Let damage through
            long remainingMs = getRemainingCooldown(victim, shieldId);
            double remainingSec = remainingMs / 1000.0;
            
            // Get shield name for display
            String shieldName = "";
            switch (shieldId) {
                case "red_shield":
                    shieldName = "빨간 방패";
                    break;
                case "blue_shield":
                    shieldName = "파란 방패";
                    break;
                case "basic_shield":
                    shieldName = "기본 방패";
                    break;
            }
            
            victim.sendMessage(String.format("§c%s 스킬 쿨타임: %.1f초", shieldName, remainingSec));
            return;
        }
        
        // Apply shield effect based on type
        switch (shieldId) {
            case "red_shield":
                // Deal 6 damage to attacker (buffed from 3), 6s cooldown (nerfed from 5s)
                if (event.getDamager() instanceof Player) {
                    Player attacker = (Player) event.getDamager();
                    damageHandler.applyInstantDamage(attacker, 6.0);
                }
                setShieldCooldown(victim, shieldId, 6000);
                cooldownManager.setCooldown(victim.getUniqueId(), AbilityIds.RED_SHIELD, 6);
                VerminPVP.getInstance().getCooldownDisplay().showCooldown(victim, AbilityIds.RED_SHIELD, "빨간 방패", 6.0);
                victim.sendMessage("§c빨간 방패 막기! 공격자에게 6 데미지");
                break;
                
            case "blue_shield":
                // Grant 2 absorption hearts (non-stacking, 10s duration), 11s cooldown
                boolean applied = effectApplicator.applyAbsorption(victim, 2, 200, "blue_shield"); // 10s = 200 ticks
                if (applied) {
                    victim.sendMessage("§9파란 방패 막기! 흡수 하트 2개 획득");
                } else {
                    victim.sendMessage("§9파란 방패 막기! (이미 흡수 효과 보유)");
                }
                setShieldCooldown(victim, shieldId, 11000);
                cooldownManager.setCooldown(victim.getUniqueId(), AbilityIds.BLUE_SHIELD, 11);
                VerminPVP.getInstance().getCooldownDisplay().showCooldown(victim, AbilityIds.BLUE_SHIELD, "파란 방패", 11.0);
                break;
                
            case "basic_shield":
                // Apply Weakness I to attacker (1.5s duration, nerfed from 2s), 4s cooldown (nerfed from 3.5s)
                if (event.getDamager() instanceof Player) {
                    Player attacker = (Player) event.getDamager();
                    effectApplicator.applyEffect(attacker, PotionEffectType.WEAKNESS, 30, 0); // 1.5s = 30 ticks
                }
                setShieldCooldown(victim, shieldId, 4000);
                cooldownManager.setCooldown(victim.getUniqueId(), AbilityIds.BASIC_SHIELD, 4);
                VerminPVP.getInstance().getCooldownDisplay().showCooldown(victim, AbilityIds.BASIC_SHIELD, "기본 방패", 4.0);
                victim.sendMessage("§7기본 방패 막기! 공격자에게 약화 부여");
                break;
                
            case "golden_shield":
                // Apply all buffs: Strength II 8s + Speed I 8s + 12 absorption hearts (6칸) 8s
                effectApplicator.applyEffect(victim, PotionEffectType.STRENGTH, 160, 1); // Strength II 8s = 160 ticks
                effectApplicator.applyEffect(victim, PotionEffectType.SPEED, 160, 0); // Speed I 8s = 160 ticks (FIXED from II)
                effectApplicator.applyAbsorption(victim, 6, 160, "golden_shield"); // 12 absorption hearts (6칸) 8s = 160 ticks
                
                // Remove golden shield (one-time use)
                shield.setAmount(0);
                victim.sendMessage("§6§l황금 방패 발동! 힘 II + 신속 II + 흡수 6칸!");
                break;
        }
        
        // Check for Golden Shield trigger after blocking (health ≤ 12)
        // This allows golden shield to trigger even when blocking projectiles
        if (victim.getHealth() <= 12.0 && !goldenShieldTriggered.getOrDefault(victim.getUniqueId(), false)) {
            if (hasGoldenShield(victim)) {
                // Schedule Golden Shield activation
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (victim.isOnline() && victim.getHealth() <= 12.0) {
                        activateGoldenShield(victim);
                        goldenShieldTriggered.put(victim.getUniqueId(), true);
                    }
                });
            } else {
                // Give golden shield if not already triggered
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (victim.isOnline() && victim.getHealth() <= 12.0) {
                        giveGoldenShield(victim);
                        goldenShieldTriggered.put(victim.getUniqueId(), true);
                    }
                });
            }
        }
    }
    
    /**
     * Give Golden Shield to player when health ≤ 6
     */
    private void giveGoldenShield(Player player) {
        ItemStack goldenShield = itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "golden_shield");
        player.getInventory().addItem(goldenShield);
        player.sendMessage("§6§l황금 방패를 받았습니다! (체력 위기 시 발동)");
    }
    
    /**
     * Check if player has golden shield in inventory
     */
    private boolean hasGoldenShield(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.SHIELD) {
                String itemId = itemProvider.getItemId(item);
                if ("golden_shield".equals(itemId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Activate golden shield effect immediately (for projectile damage)
     */
    private void activateGoldenShield(Player player) {
        // Find and remove golden shield from inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.SHIELD) {
                String itemId = itemProvider.getItemId(item);
                if ("golden_shield".equals(itemId)) {
                    // Remove the shield
                    player.getInventory().setItem(i, null);
                    
                    // Apply all buffs: Strength II 8s + Speed I 8s + 12 absorption hearts (6칸) 8s
                    effectApplicator.applyEffect(player, PotionEffectType.STRENGTH, 160, 1); // Strength II 8s = 160 ticks
                    effectApplicator.applyEffect(player, PotionEffectType.SPEED, 160, 0); // Speed I 8s = 160 ticks (FIXED from II)
                    effectApplicator.applyAbsorption(player, 6, 160, "golden_shield"); // 12 absorption hearts (6칸) 8s = 160 ticks
                    
                    player.sendMessage("§6§l황금 방패 자동 발동! 힘 II + 신속 I + 흡수 6칸!");
                    return;
                }
            }
        }
    }
    
    /**
     * Get the shield the player is currently holding
     */
    private ItemStack getActiveShield(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() == Material.SHIELD) {
            return offhand;
        }
        
        ItemStack mainhand = player.getInventory().getItemInMainHand();
        if (mainhand != null && mainhand.getType() == Material.SHIELD) {
            return mainhand;
        }
        
        return null;
    }
    
    /**
     * Set shield-specific cooldown
     */
    private void setShieldCooldown(Player player, String shieldId, long durationMillis) {
        UUID playerId = player.getUniqueId();
        shieldCooldowns.putIfAbsent(playerId, new HashMap<>());
        
        long endTime = System.currentTimeMillis() + durationMillis;
        shieldCooldowns.get(playerId).put(shieldId, endTime);
    }
    
    /**
     * Set shield deploy (blocking) cooldown
     */
    private void setShieldDeployCooldown(Player player, String shieldId, long durationMillis) {
        UUID playerId = player.getUniqueId();
        shieldDeployCooldowns.putIfAbsent(playerId, new HashMap<>());
        
        long endTime = System.currentTimeMillis() + durationMillis;
        shieldDeployCooldowns.get(playerId).put(shieldId, endTime);
    }
    
    /**
     * Check if a specific shield is on cooldown
     */
    private boolean isShieldOnCooldown(Player player, String shieldId) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = shieldCooldowns.get(playerId);
        
        if (playerCooldowns == null || !playerCooldowns.containsKey(shieldId)) {
            return false;
        }
        
        long cooldownEnd = playerCooldowns.get(shieldId);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime >= cooldownEnd) {
            playerCooldowns.remove(shieldId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a specific shield deploy is on cooldown
     */
    private boolean isShieldDeployOnCooldown(Player player, String shieldId) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = shieldDeployCooldowns.get(playerId);
        
        if (playerCooldowns == null || !playerCooldowns.containsKey(shieldId)) {
            return false;
        }
        
        long cooldownEnd = playerCooldowns.get(shieldId);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime >= cooldownEnd) {
            playerCooldowns.remove(shieldId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get remaining cooldown time in milliseconds for a specific shield
     */
    private long getRemainingCooldown(Player player, String shieldId) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = shieldCooldowns.get(playerId);
        
        if (playerCooldowns == null || !playerCooldowns.containsKey(shieldId)) {
            return 0;
        }
        
        long cooldownEnd = playerCooldowns.get(shieldId);
        long currentTime = System.currentTimeMillis();
        
        return Math.max(0, cooldownEnd - currentTime);
    }
    
    /**
     * Get remaining deploy cooldown time in milliseconds for a specific shield
     */
    private long getRemainingDeployCooldown(Player player, String shieldId) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = shieldDeployCooldowns.get(playerId);
        
        if (playerCooldowns == null || !playerCooldowns.containsKey(shieldId)) {
            return 0;
        }
        
        long cooldownEnd = playerCooldowns.get(shieldId);
        long currentTime = System.currentTimeMillis();
        
        return Math.max(0, cooldownEnd - currentTime);
    }
    
    /**
     * Give starting shields to Shield Soldier (3 shields: Red, Blue, Basic)
     */
    public void giveStartingShields(Player player) {
        ItemStack redShield = itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "red_shield");
        ItemStack blueShield = itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "blue_shield");
        ItemStack basicShield = itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "basic_shield");
        
        player.getInventory().addItem(redShield);
        player.getInventory().addItem(blueShield);
        player.getInventory().addItem(basicShield);
    }
    
    /**
     * Reset Golden Shield trigger flag (called on game start/respawn)
     */
    public void resetGoldenShieldTrigger(Player player) {
        goldenShieldTriggered.remove(player.getUniqueId());
    }
    
    /**
     * Clean up player data
     */
    public void cleanup(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Stop passive task
        stopAbsorptionPassive(player);
        
        // Clear tracking data
        lastDamageTime.remove(playerId);
        shieldCooldowns.remove(playerId);
        goldenShieldTriggered.remove(playerId);
        
        // Clear absorption tracking
        effectApplicator.removeAbsorptionSource(player, "shield_soldier_passive");
        effectApplicator.removeAbsorptionSource(player, "blue_shield");
        effectApplicator.removeAbsorptionSource(player, "golden_shield");
    }
    
    /**
     * Clean up all Shield Soldier data (called on game end)
     */
    public void cleanupAll() {
        // Stop all passive tasks
        for (BukkitTask task : passiveTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        passiveTasks.clear();
        
        // Clear all tracking data
        lastDamageTime.clear();
        shieldCooldowns.clear();
        goldenShieldTriggered.clear();
    }
    
    /**
     * Clean up Shield Soldier data for a specific player
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Stop passive task for this player
        BukkitTask task = passiveTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Clear tracking data for this player
        lastDamageTime.remove(playerId);
        shieldCooldowns.remove(playerId);
        goldenShieldTriggered.remove(playerId);
    }
}


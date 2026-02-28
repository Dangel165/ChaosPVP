package com.verminpvp.handlers;

import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.ClassData;
import com.verminpvp.models.ClassType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Dragon Fury class
 * 
 * Passive: Gain 1 Reverse Scale on hit (max 30)
 * Starts with iron armor set (no diamond chestplate, no stone sword)
 * Has "Reverse Scale Counter" item
 * 
 * Reverse Scale Counter: Consume all scales to deal damage equal to scales consumed
 * If kill with skill, heal HP = scales consumed / 2
 * Cooldown: 60 seconds
 */
public class DragonFuryHandler implements Listener {
    
    private final Plugin plugin;
    private final ClassManager classManager;
    private final GameManager gameManager;
    private final CooldownManager cooldownManager;
    private final DamageHandler damageHandler;
    private final TeamManager teamManager;
    
    // Track reverse scales per player
    private final Map<UUID, Integer> reverseScales = new HashMap<>();
    
    // Track passive generation tasks
    private final Map<UUID, BukkitTask> passiveGenerationTasks = new HashMap<>();
    
    // Track counter ability usage for kill detection
    private final Map<UUID, CounterData> activeCounters = new HashMap<>();
    
    // Data class to track counter ability usage
    private static class CounterData {
        final UUID attackerId;
        final int scalesUsed;
        final long timestamp;
        
        CounterData(UUID attackerId, int scalesUsed) {
            this.attackerId = attackerId;
            this.scalesUsed = scalesUsed;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private static final int MAX_SCALES = 30;
    private static final String COUNTER_ABILITY_ID = "dragon_fury_counter";
    private static final double COUNTER_COOLDOWN = 30.0; // 30 seconds (changed from 60s)
    private static final int PASSIVE_GENERATION_INTERVAL = 160; // 8 seconds in ticks (changed from 15s)
    
    public DragonFuryHandler(Plugin plugin, ClassManager classManager, GameManager gameManager,
                            CooldownManager cooldownManager, DamageHandler damageHandler, TeamManager teamManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.gameManager = gameManager;
        this.cooldownManager = cooldownManager;
        this.damageHandler = damageHandler;
        this.teamManager = teamManager;
    }
    
    /**
     * Give starting items to Dragon Fury player
     */
    public void giveStartingItems(Player player) {
        // Iron armor set with Unbreakable
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        ItemMeta helmetMeta = helmet.getItemMeta();
        if (helmetMeta != null) {
            helmetMeta.setUnbreakable(true);
            helmet.setItemMeta(helmetMeta);
        }
        
        ItemStack chestplate = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta chestplateMeta = chestplate.getItemMeta();
        if (chestplateMeta != null) {
            chestplateMeta.setUnbreakable(true);
            chestplate.setItemMeta(chestplateMeta);
        }
        
        ItemStack leggings = new ItemStack(Material.IRON_LEGGINGS);
        ItemMeta leggingsMeta = leggings.getItemMeta();
        if (leggingsMeta != null) {
            leggingsMeta.setUnbreakable(true);
            leggings.setItemMeta(leggingsMeta);
        }
        
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        ItemMeta bootsMeta = boots.getItemMeta();
        if (bootsMeta != null) {
            bootsMeta.setUnbreakable(true);
            boots.setItemMeta(bootsMeta);
        }
        
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        
        // Reverse Scale Counter item (using BLAZE_POWDER - not consumable)
        ItemStack counterItem = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta counterMeta = counterItem.getItemMeta();
        if (counterMeta != null) {
            counterMeta.setDisplayName("§c§l역린의 반격");
            counterMeta.setLore(java.util.Arrays.asList(
                "§7우클릭으로 모든 역린 소모",
                "§7소모한 역린만큼 대상에게 피해",
                "§7처치 시: 역린 / 2 만큼 체력 회복",
                "§e쿨타임: 30초"
            ));
            counterMeta.setUnbreakable(true);
            counterItem.setItemMeta(counterMeta);
        }
        player.getInventory().addItem(counterItem);
        
        // Apply permanent Strength I and Resistance I effects
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.STRENGTH, 
            Integer.MAX_VALUE, 0, false, false, false));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.RESISTANCE, 
            Integer.MAX_VALUE, 0, false, false, false));
        
        // Initialize scales to 0
        reverseScales.put(player.getUniqueId(), 0);
        updateScaleDisplay(player);
        
        // Start passive scale generation task
        startPassiveScaleGeneration(player);
    }
    
    /**
     * Handle player taking damage - gain reverse scale (체력이 소모될 시)
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        ClassData data = classManager.getClassData(player);
        
        if (data == null || data.getClassType() != ClassType.DRAGON_FURY) {
            return;
        }
        
        // Only work in game or practice mode
        if (!gameManager.isGameActive() && !gameManager.isInPracticeMode(player)) {
            return;
        }
        
        // Check if damage will actually reduce health (not cancelled, not absorbed)
        if (event.isCancelled() || event.getFinalDamage() <= 0) {
            return;
        }
        
        // Gain 1 reverse scale
        int currentScales = reverseScales.getOrDefault(player.getUniqueId(), 0);
        if (currentScales < MAX_SCALES) {
            reverseScales.put(player.getUniqueId(), currentScales + 1);
            updateScaleDisplay(player);
            updateResistanceLevel(player, currentScales + 1);
            player.sendMessage("§e역린 +1 (§6" + (currentScales + 1) + "§e/§630§e)");
        }
    }
    
    /**
     * Update resistance level based on scale count
     */
    private void updateResistanceLevel(Player player, int scales) {
        // Remove old resistance
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE);
        
        // Apply new resistance based on scale count
        int resistanceLevel = 0; // Resistance I (base)
        if (scales > 20) {
            resistanceLevel = 2; // Resistance III
        } else if (scales > 10) {
            resistanceLevel = 1; // Resistance II
        }
        
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.RESISTANCE, 
            Integer.MAX_VALUE, resistanceLevel, false, false, false));
    }
    
    /**
     * Handle Reverse Scale Counter ability
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ClassData data = classManager.getClassData(player);
        
        if (data == null || data.getClassType() != ClassType.DRAGON_FURY) {
            return;
        }
        
        // Only work in game or practice mode
        if (!gameManager.isGameActive() && !gameManager.isInPracticeMode(player)) {
            return;
        }
        
        // Check if holding counter item (BLAZE_POWDER)
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BLAZE_POWDER) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.getDisplayName().equals("§c§l역린의 반격")) {
            return;
        }
        
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), COUNTER_ABILITY_ID)) {
            double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), COUNTER_ABILITY_ID);
            player.sendMessage("§c역린의 반격 쿨타임: " + String.format("%.1f", remaining) + "초");
            return;
        }
        
        // Check if has scales
        int scales = reverseScales.getOrDefault(player.getUniqueId(), 0);
        if (scales <= 0) {
            player.sendMessage("§c역린이 없습니다!");
            return;
        }
        
        event.setCancelled(true);
        handleCounter(player, scales);
        
        // Set cooldown
        cooldownManager.setCooldown(player.getUniqueId(), COUNTER_ABILITY_ID, COUNTER_COOLDOWN);
    }
    
    /**
     * Handle counter ability execution
     */
    private void handleCounter(Player player, int scales) {
        // Find nearest enemy
        LivingEntity target = null;
        double minDistance = 5.0; // 5 block range
        
        // Check if in team mode
        boolean isTeamMode = gameManager.getGameMode() == com.verminpvp.models.GameMode.TEAM;
        com.verminpvp.models.Team playerTeam = null;
        if (isTeamMode) {
            playerTeam = teamManager.getPlayerTeam(player);
        }
        
        for (Entity entity : player.getNearbyEntities(minDistance, minDistance, minDistance)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity livingEntity = (LivingEntity) entity;
                
                // Skip teammates in team mode
                if (livingEntity instanceof Player && isTeamMode) {
                    Player targetPlayer = (Player) livingEntity;
                    com.verminpvp.models.Team targetTeam = teamManager.getPlayerTeam(targetPlayer);
                    
                    // Skip if same team
                    if (playerTeam != null && targetTeam != null && playerTeam == targetTeam) {
                        continue;
                    }
                }
                
                double distance = player.getLocation().distance(livingEntity.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    target = livingEntity;
                }
            }
        }
        
        if (target == null) {
            player.sendMessage("§c범위 내에 적이 없습니다!");
            return;
        }
        
        // Consume all scales
        reverseScales.put(player.getUniqueId(), 0);
        updateScaleDisplay(player);
        
        // Deal damage equal to scales (1 scale = 1 damage)
        double damage = scales * 1.0;
        
        // Track this counter for kill detection
        activeCounters.put(target.getUniqueId(), new CounterData(player.getUniqueId(), scales));
        
        damageHandler.applyInstantDamage(target, damage);
        
        // Visual effect - use SOUL_FIRE_FLAME instead of DRAGON_BREATH
        player.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, 
            target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        
        player.sendMessage("§a역린의 반격! §e" + scales + "개 소모 → §c" + damage + " 피해");
        
        // Schedule cleanup of counter tracking after 1 second
        final LivingEntity finalTarget = target;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            activeCounters.remove(finalTarget.getUniqueId());
        }, 20L);
    }
    
    /**
     * Handle entity death to detect counter kills
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        CounterData counterData = activeCounters.remove(entity.getUniqueId());
        
        if (counterData == null) {
            return;
        }
        
        // Check if counter was used recently (within 1 second)
        if (System.currentTimeMillis() - counterData.timestamp > 1000) {
            return;
        }
        
        Player attacker = Bukkit.getPlayer(counterData.attackerId);
        if (attacker == null || !attacker.isOnline()) {
            return;
        }
        
        // Heal player (scales / 2 HP)
        double healAmount = counterData.scalesUsed / 2.0;
        double currentHealth = attacker.getHealth();
        double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(maxHealth, currentHealth + healAmount);
        attacker.setHealth(newHealth);
        attacker.sendMessage("§a처치 성공! §e+" + healAmount + " 체력 회복");
    }
    
    /**
     * Update scale display in action bar
     */
    private void updateScaleDisplay(Player player) {
        int scales = reverseScales.getOrDefault(player.getUniqueId(), 0);
        player.sendActionBar("§6역린: §e" + scales + " §7/ §630");
    }
    
    /**
     * Start passive scale generation (1 scale every 10 seconds)
     */
    private void startPassiveScaleGeneration(Player player) {
        // Cancel existing task if any
        stopPassiveScaleGeneration(player);
        
        // Start new generation task (changed from 15s to 10s)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopPassiveScaleGeneration(player);
                return;
            }
            
            ClassData data = classManager.getClassData(player);
            if (data == null || data.getClassType() != ClassType.DRAGON_FURY) {
                stopPassiveScaleGeneration(player);
                return;
            }
            
            // Only work in game or practice mode
            if (!gameManager.isGameActive() && !gameManager.isInPracticeMode(player)) {
                return;
            }
            
            // Gain 1 reverse scale
            int currentScales = reverseScales.getOrDefault(player.getUniqueId(), 0);
            if (currentScales < MAX_SCALES) {
                reverseScales.put(player.getUniqueId(), currentScales + 1);
                updateScaleDisplay(player);
                updateResistanceLevel(player, currentScales + 1);
                player.sendMessage("§e역린 자동 생성 +1 (§6" + (currentScales + 1) + "§e/§630§e)");
            }
        }, PASSIVE_GENERATION_INTERVAL, PASSIVE_GENERATION_INTERVAL); // 8 seconds
        
        passiveGenerationTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Stop passive scale generation
     */
    private void stopPassiveScaleGeneration(Player player) {
        BukkitTask task = passiveGenerationTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Cleanup player data
     */
    public void cleanupPlayer(Player player) {
        reverseScales.remove(player.getUniqueId());
        stopPassiveScaleGeneration(player);
        
        // Remove Strength I and Resistance effects
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE);
    }
    
    /**
     * Cleanup all data
     */
    public void cleanupAll() {
        reverseScales.clear();
        activeCounters.clear();
        
        for (BukkitTask task : passiveGenerationTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        passiveGenerationTasks.clear();
    }
}

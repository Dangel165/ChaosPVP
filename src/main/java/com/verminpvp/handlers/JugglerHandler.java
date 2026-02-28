package com.verminpvp.handlers;

import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.ClassData;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.Team;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Juggler class
 * 
 * Passive: Starts without stone sword, has "Light Thing" and "Heavy Thing" items
 * Gains 1 "Throw Time" every 60 seconds (max 1)
 * 
 * Light Thing: Right-click to throw snowball projectile
 * - Damage: 1 (0.5 hearts)
 * - No knockback
 * - Cooldown: 10s (resets on hit)
 * 
 * Heavy Thing: Right-click to throw wind charge projectile
 * - Damage: 8 (4 hearts)
 * - On hit: Slowness V (0.5s), Blindness (1s)
 * - Cooldown: 20s
 * 
 * Throw Time: Right-click to apply Slowness V (2.5s) to all entities within 10 blocks
 * - Resets "Light Thing" cooldown
 * - Consumes the item
 */
public class JugglerHandler implements Listener {
    
    private final Plugin plugin;
    private final ClassManager classManager;
    private final CooldownManager cooldownManager;
    private final ItemProvider itemProvider;
    private final DamageHandler damageHandler;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    
    // Ability IDs
    private static final String LIGHT_THING_ID = "juggler_light";
    private static final String HEAVY_THING_ID = "juggler_heavy";
    private static final String THROW_TIME_ID = "juggler_time";
    
    // Cooldowns
    private static final int LIGHT_THING_COOLDOWN = 4; // seconds - BUFFED from 10s
    private static final int HEAVY_THING_COOLDOWN = 13; // seconds (changed from 20s)
    private static final int THROW_TIME_GAIN_INTERVAL = 60; // seconds
    
    // Track projectiles
    private final Map<UUID, Player> snowballOwners = new HashMap<>();
    private final Map<UUID, Player> windChargeOwners = new HashMap<>();
    
    // Track throw time gain tasks
    private final Map<UUID, BukkitTask> throwTimeGainTasks = new HashMap<>();
    
    // Track entities frozen by Throw Time (position lock)
    private final Map<UUID, Location> frozenEntities = new HashMap<>();
    
    public JugglerHandler(Plugin plugin, ClassManager classManager, CooldownManager cooldownManager,
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
     * Enforce position lock for frozen entities (players)
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Only enforce freeze if game is active
        if (!gameManager.isGameActive() && !gameManager.isInPracticeMode(player)) {
            return;
        }
        
        Location frozenLoc = frozenEntities.get(player.getUniqueId());
        if (frozenLoc != null) {
            // Check if player moved from frozen position
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                // Teleport player back to frozen position
                event.setTo(frozenLoc);
            }
        }
    }
    
    /**
     * Start throw time gain system for a Juggler player
     */
    public void startThrowTimeGainSystem(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        ClassData data = classManager.getClassData(player);
        if (data == null || data.getClassType() != ClassType.JUGGLER) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Cancel existing task
        stopThrowTimeGainSystem(player);
        
        // Schedule throw time gain every 60 seconds
        // Use delay of 60 seconds (first gain at 1 minute), then repeat every 60 seconds
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Verify player is still online and game is active
            if (!player.isOnline()) {
                stopThrowTimeGainSystem(player);
                return;
            }
            
            if (!gameManager.isGameActive()) {
                stopThrowTimeGainSystem(player);
                return;
            }
            
            // Verify player is still a Juggler
            ClassData currentData = classManager.getClassData(player);
            if (currentData == null || currentData.getClassType() != ClassType.JUGGLER) {
                stopThrowTimeGainSystem(player);
                return;
            }
            
            // Check if player already has throw time (max 1)
            if (hasThrowTime(player)) {
                return; // Already has one, don't give another
            }
            
            // Give throw time item
            ItemStack throwTime = itemProvider.getJugglerThrowTime();
            if (throwTime != null) {
                player.getInventory().addItem(throwTime);
                player.sendMessage("§e투척 시간을 획득했습니다!");
            }
        }, THROW_TIME_GAIN_INTERVAL * 20L, THROW_TIME_GAIN_INTERVAL * 20L);
        
        throwTimeGainTasks.put(playerId, task);
    }
    
    /**
     * Stop throw time gain system for a player
     */
    public void stopThrowTimeGainSystem(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        BukkitTask task = throwTimeGainTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Check if player has throw time item
     */
    private boolean hasThrowTime(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (item.getItemMeta().getDisplayName().contains("투척 시간")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Handle player interactions (right-click)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ClassData data = classManager.getClassData(player);
        
        if (data == null || data.getClassType() != ClassType.JUGGLER) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        
        // Light Thing
        if (displayName.contains("가벼운 것")) {
            event.setCancelled(true);
            handleLightThing(player);
        }
        // Heavy Thing
        else if (displayName.contains("무거운 것")) {
            event.setCancelled(true);
            handleHeavyThing(player);
        }
        // Throw Time
        else if (displayName.contains("투척 시간")) {
            event.setCancelled(true);
            handleThrowTime(player, item);
        }
    }
    
    /**
     * Handle Light Thing ability
     */
    private void handleLightThing(Player player) {
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), LIGHT_THING_ID)) {
            double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), LIGHT_THING_ID);
            player.sendMessage("§c가벼운 것 쿨타임: " + String.format("%.1f", remaining) + "초");
            return;
        }
        
        // Launch snowball
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setVelocity(player.getLocation().getDirection().multiply(1.5));
        snowballOwners.put(snowball.getUniqueId(), player);
        
        // Set cooldown
        cooldownManager.setCooldown(player.getUniqueId(), LIGHT_THING_ID, LIGHT_THING_COOLDOWN);
        player.sendMessage("§a가벼운 것 사용!");
    }
    
    /**
     * Handle Heavy Thing ability
     */
    private void handleHeavyThing(Player player) {
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), HEAVY_THING_ID)) {
            double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), HEAVY_THING_ID);
            player.sendMessage("§c무거운 것 쿨타임: " + String.format("%.1f", remaining) + "초");
            return;
        }
        
        // Launch wind charge
        WindCharge windCharge = player.launchProjectile(WindCharge.class);
        windCharge.setVelocity(player.getLocation().getDirection().multiply(1.2));
        windChargeOwners.put(windCharge.getUniqueId(), player);
        
        // Set cooldown
        cooldownManager.setCooldown(player.getUniqueId(), HEAVY_THING_ID, HEAVY_THING_COOLDOWN);
        player.sendMessage("§a무거운 것 사용!");
    }
    
    /**
     * Handle Throw Time ability
     */
    private void handleThrowTime(Player player, ItemStack item) {
        // Apply Slowness V to all entities within 10 blocks and freeze their position
        int affectedCount = 0;
        java.util.List<UUID> frozenEntities = new java.util.ArrayList<>();
        java.util.Map<UUID, Location> frozenLocations = new java.util.HashMap<>();
        
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                
                // If target is a player, check team restrictions (unless in practice mode)
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
                
                // Apply Slowness V for 2.5 seconds (50 ticks)
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 4, false, true, true));
                
                // Freeze position (like Time Engraver)
                Location frozenLoc = target.getLocation().clone();
                frozenEntities.add(target.getUniqueId());
                frozenLocations.put(target.getUniqueId(), frozenLoc);
                
                affectedCount++;
            }
        }
        
        // Schedule position enforcement task for 2.5 seconds (50 ticks)
        if (!frozenEntities.isEmpty()) {
            org.bukkit.scheduler.BukkitTask freezeTask = new org.bukkit.scheduler.BukkitRunnable() {
                int ticksRemaining = 50; // 2.5 seconds
                
                @Override
                public void run() {
                    if (ticksRemaining <= 0) {
                        cancel();
                        return;
                    }
                    
                    // Enforce frozen positions for all affected entities
                    for (UUID entityId : frozenEntities) {
                        Entity entity = org.bukkit.Bukkit.getEntity(entityId);
                        if (entity instanceof LivingEntity && entity.isValid()) {
                            Location frozenLoc = frozenLocations.get(entityId);
                            if (frozenLoc != null) {
                                // Teleport entity back to frozen position if they moved
                                Location currentLoc = entity.getLocation();
                                if (currentLoc.getX() != frozenLoc.getX() || 
                                    currentLoc.getY() != frozenLoc.getY() || 
                                    currentLoc.getZ() != frozenLoc.getZ()) {
                                    entity.teleport(frozenLoc);
                                }
                            }
                        }
                    }
                    
                    ticksRemaining--;
                }
            }.runTaskTimer(plugin, 0L, 1L); // Run every tick
        }
        
        // Reset Light Thing cooldown
        cooldownManager.clearCooldown(player.getUniqueId(), LIGHT_THING_ID);
        
        // Remove throw time item
        item.setAmount(item.getAmount() - 1);
        
        player.sendMessage("§a투척 시간 사용! §e" + affectedCount + "개 영향");
        player.sendMessage("§a가벼운 것 쿨타임 초기화!");
    }
    
    /**
     * Handle projectile hits
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitEntity() == null) {
            return;
        }
        
        Entity hitEntity = event.getHitEntity();
        if (!(hitEntity instanceof LivingEntity)) {
            return;
        }
        
        LivingEntity target = (LivingEntity) hitEntity;
        
        // Handle snowball (Light Thing)
        if (event.getEntity() instanceof Snowball) {
            Snowball snowball = (Snowball) event.getEntity();
            Player shooter = snowballOwners.remove(snowball.getUniqueId());
            
            if (shooter != null) {
                // If target is a player, check team restrictions (unless in practice mode)
                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
                    
                    // Check team (not in practice mode)
                    if (gameManager.getGameMode() == com.verminpvp.models.GameMode.TEAM && !gameManager.isInPracticeMode(shooter)) {
                        Team shooterTeam = teamManager.getPlayerTeam(shooter);
                        Team targetTeam = teamManager.getPlayerTeam(targetPlayer);
                        if (shooterTeam != null && shooterTeam == targetTeam) {
                            return; // Same team, no damage
                        }
                    }
                }
                
                // Deal 1 damage (0.5 hearts) with no knockback
                damageHandler.applyDamageWithoutKnockback(target, 1.0);
                
                // Reset cooldown on hit
                cooldownManager.clearCooldown(shooter.getUniqueId(), LIGHT_THING_ID);
                shooter.sendMessage("§a가벼운 것 적중! 쿨타임 초기화!");
            }
        }
        // Handle wind charge (Heavy Thing)
        else if (event.getEntity() instanceof WindCharge) {
            WindCharge windCharge = (WindCharge) event.getEntity();
            Player shooter = windChargeOwners.remove(windCharge.getUniqueId());
            
            if (shooter != null) {
                // If target is a player, check team restrictions (unless in practice mode)
                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
                    
                    // Check team (not in practice mode)
                    if (gameManager.getGameMode() == com.verminpvp.models.GameMode.TEAM && !gameManager.isInPracticeMode(shooter)) {
                        Team shooterTeam = teamManager.getPlayerTeam(shooter);
                        Team targetTeam = teamManager.getPlayerTeam(targetPlayer);
                        if (shooterTeam != null && shooterTeam == targetTeam) {
                            return; // Same team, no damage
                        }
                    }
                }
                
                // Deal 8 damage (4 hearts)
                damageHandler.applyInstantDamage(target, 8.0);
                
                // Apply Slowness V for 1 second (20 ticks)
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 4, false, true, true));
                
                // Apply Blindness for 1 second (20 ticks)
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, true, true));
                
                shooter.sendMessage("§a무거운 것 적중!");
            }
        }
    }
    
    /**
     * Cleanup all Juggler tasks
     */
    public void cleanupAll() {
        // Cancel all throw time gain tasks
        for (BukkitTask task : throwTimeGainTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        throwTimeGainTasks.clear();
        
        // Clear projectile tracking
        snowballOwners.clear();
        windChargeOwners.clear();
        
        // Clear frozen entities
        frozenEntities.clear();
    }
    
    /**
     * Cleanup Juggler tasks for a specific player
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel throw time gain task for this player
        BukkitTask task = throwTimeGainTasks.get(playerId);
        if (task != null) {
            task.cancel();
            throwTimeGainTasks.remove(playerId);
        }
        
        // Clear projectile tracking for this player
        snowballOwners.values().removeIf(uuid -> uuid.equals(playerId));
        windChargeOwners.values().removeIf(uuid -> uuid.equals(playerId));
        
        // Clear frozen state for this player
        frozenEntities.remove(playerId);
    }
}


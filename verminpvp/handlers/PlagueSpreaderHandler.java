package com.verminpvp.handlers;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.managers.ResourceTracker;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.AbilityIds;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.HashSet;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Plague Spreader class abilities
 * 
 * Abilities:
 * - Poison immunity with Strength I buff on poison damage
 * - Virulent Engine generation (every 20s, max 2)
 * - Extreme Poison Engine generation (every 180s, max 1)
 * - Poison field system with area effects (toggle on/off like Singed)
 */
public class PlagueSpreaderHandler implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    private final CooldownManager cooldownManager;
    private final ItemProvider itemProvider;
    private final EffectApplicator effectApplicator;
    private final DamageHandler damageHandler;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    
    // Track active poison fields
    private final Map<UUID, AreaEffectCloud> activeFields = new HashMap<>();
    private final Map<UUID, BukkitTask> fieldTasks = new HashMap<>();
    private final Map<UUID, Boolean> fieldActive = new HashMap<>();
    
    public PlagueSpreaderHandler(VerminPVP plugin, ClassManager classManager, 
                                  CooldownManager cooldownManager, ItemProvider itemProvider,
                                  EffectApplicator effectApplicator, DamageHandler damageHandler,
                                  TeamManager teamManager, GameManager gameManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.cooldownManager = cooldownManager;
        this.itemProvider = itemProvider;
        this.effectApplicator = effectApplicator;
        this.damageHandler = damageHandler;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }
    
    /**
     * Start generation schedulers for a Plague Spreader
     */
    public void startGenerationSchedulers(Player player) {
        // Virulent Engine generation (every 20s, max 2)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || classManager.getPlayerClass(player) != ClassType.PLAGUE_SPREADER) {
                    cancel();
                    return;
                }
                
                // Count existing Virulent Engines
                int count = countItemsInInventory(player, "virulent_engine");
                if (count < 2) {
                    ItemStack engine = itemProvider.createSpecialItem(ClassType.PLAGUE_SPREADER, "virulent_engine");
                    player.getInventory().addItem(engine);
                    player.sendMessage("§a맹독 엔진을 받았습니다! (" + (count + 1) + "/2)");
                } else {
                    // Remove one old engine and add new one
                    removeOneEngine(player, "virulent_engine");
                    ItemStack engine = itemProvider.createSpecialItem(ClassType.PLAGUE_SPREADER, "virulent_engine");
                    player.getInventory().addItem(engine);
                    player.sendMessage("§a맹독 엔진 갱신! (2/2)");
                }
            }
        }.runTaskTimer(plugin, 20L * 20, 20L * 20); // 20s initial delay, 20s period
        
        // Extreme Poison Engine generation (every 100s, max 1)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || classManager.getPlayerClass(player) != ClassType.PLAGUE_SPREADER) {
                    cancel();
                    return;
                }
                
                // Count existing Extreme Poison Engines
                int count = countItemsInInventory(player, "extreme_poison_engine");
                if (count < 1) {
                    ItemStack engine = itemProvider.createSpecialItem(ClassType.PLAGUE_SPREADER, "extreme_poison_engine");
                    player.getInventory().addItem(engine);
                    player.sendMessage("§5극독 엔진을 받았습니다! (1/1)");
                } else {
                    // Remove old engine and add new one
                    removeOneEngine(player, "extreme_poison_engine");
                    ItemStack engine = itemProvider.createSpecialItem(ClassType.PLAGUE_SPREADER, "extreme_poison_engine");
                    player.getInventory().addItem(engine);
                    player.sendMessage("§5극독 엔진 갱신! (1/1)");
                }
            }
        }.runTaskTimer(plugin, 20L * 100, 20L * 100); // 100s initial delay, 100s period
    }
    
    /**
     * Poison immunity - cancel poison damage
     */
    @EventHandler
    public void onPoisonDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.POISON) return;
        
        Player player = (Player) event.getEntity();
        if (classManager.getPlayerClass(player) != ClassType.PLAGUE_SPREADER) return;
        
        // Cancel poison damage (no buff)
        event.setCancelled(true);
    }
    
    /**
     * Handle engine right-click to use (one-time use, not toggle)
     */
    @EventHandler
    public void onEngineUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (classManager.getPlayerClass(player) != ClassType.PLAGUE_SPREADER) return;
        
        String actionName = event.getAction().name();
        if (!actionName.contains("RIGHT_CLICK")) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        String itemId = itemProvider.getItemId(item);
        if (itemId == null) return;
        
        boolean isExtreme = false;
        boolean isEngine = false;
        
        if (itemId.equals("virulent_engine")) {
            isEngine = true;
            isExtreme = false;
        } else if (itemId.equals("extreme_poison_engine")) {
            isEngine = true;
            isExtreme = true;
        }
        
        if (!isEngine) return;
        
        event.setCancelled(true);
        
        // Check if field is already active
        boolean isActive = fieldActive.getOrDefault(player.getUniqueId(), false);
        
        if (isActive) {
            player.sendMessage("§c이미 장판이 활성화되어 있습니다!");
            return;
        }
        
        // Remove the engine item from inventory
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().remove(item);
        }
        
        // Make isExtreme final for lambda
        final boolean finalIsExtreme = isExtreme;
        
        // Start field for 10 seconds
        startPoisonField(player, finalIsExtreme);
        player.sendMessage(finalIsExtreme ? "§5극독 장판 활성화! (10초)" : "§a맹독 장판 활성화! (10초)");
        
        // Schedule field to stop after 10 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stopPoisonField(player);
            player.sendMessage(finalIsExtreme ? "§5극독 장판 종료" : "§a맹독 장판 종료");
        }, 200L); // 10 seconds = 200 ticks
    }
    
    /**
     * Start a poison field that follows the player
     */
    private void startPoisonField(Player player, boolean isExtreme) {
        // Stop existing field if any
        stopPoisonField(player);
        
        // Field parameters - increased radius
        double radius = isExtreme ? 4.0 : 3.0; // 8 block diameter / 6 block diameter
        
        // Create invisible area effect cloud at player's location
        Location loc = player.getLocation();
        AreaEffectCloud cloud = player.getWorld().spawn(loc, AreaEffectCloud.class);
        cloud.setRadius((float) radius);
        cloud.setDuration(Integer.MAX_VALUE); // Infinite duration
        cloud.setParticle(Particle.WITCH);
        cloud.setRadiusPerTick(0);
        
        activeFields.put(player.getUniqueId(), cloud);
        fieldActive.put(player.getUniqueId(), true);
        
        // Scheduler to move field with player and apply effects
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || !fieldActive.getOrDefault(player.getUniqueId(), false)) {
                    cancel();
                    return;
                }
                
                // Move cloud to player's location every tick
                cloud.teleport(player.getLocation());
                
                // Display field boundary particles every 5 ticks
                if (ticks % 5 == 0) {
                    displayFieldBoundary(player.getLocation(), radius, isExtreme);
                }
                
                // Apply effects based on field type
                // Both: every 20 ticks (1 second)
                if (ticks % 20 == 0) {
                    applyFieldEffects(player, cloud, isExtreme);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        fieldTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Stop the poison field
     */
    private void stopPoisonField(Player player) {
        // Cancel task
        BukkitTask task = fieldTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        
        // Remove cloud
        AreaEffectCloud cloud = activeFields.remove(player.getUniqueId());
        if (cloud != null && cloud.isValid()) {
            cloud.remove();
        }
        
        fieldActive.put(player.getUniqueId(), false);
    }
    
    /**
     * Display field boundary with particles
     */
    private void displayFieldBoundary(Location center, double radius, boolean isExtreme) {
        // Choose particle type based on field type
        Particle particle = isExtreme ? Particle.WITCH : Particle.HAPPY_VILLAGER;
        
        // Draw circle at ground level
        int points = 32; // Number of points in the circle
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            double y = center.getY() + 0.1; // Slightly above ground
            
            Location particleLoc = new Location(center.getWorld(), x, y, z);
            center.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }
        
        // Add some particles in the air for visibility
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            double x = center.getX() + radius * 0.7 * Math.cos(angle);
            double z = center.getZ() + radius * 0.7 * Math.sin(angle);
            double y = center.getY() + 1.0;
            
            Location particleLoc = new Location(center.getWorld(), x, y, z);
            center.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Apply poison field effects to entities in range
     */
    private void applyFieldEffects(Player owner, AreaEffectCloud cloud, boolean isExtreme) {
        Location center = cloud.getLocation();
        double radius = cloud.getRadius();
        
        // First, apply buff to the Plague Spreader owner ONLY (본인만)
        ClassType ownerClass = classManager.getPlayerClass(owner);
        if (ownerClass == ClassType.PLAGUE_SPREADER) {
            // Check if owner is still in the field
            if (owner.getLocation().distance(center) <= radius) {
                if (isExtreme) {
                    // Extreme field: Speed II + Poison I (1 second duration)
                    effectApplicator.applyEffect(owner, PotionEffectType.SPEED, 20, 1); // Speed II, 1s = 20 ticks
                    effectApplicator.applyEffect(owner, PotionEffectType.POISON, 20, 0); // Poison I, 1s = 20 ticks
                } else {
                    // Virulent field: Speed I (1 second duration)
                    effectApplicator.applyEffect(owner, PotionEffectType.SPEED, 20, 0); // Speed I, 1s = 20 ticks
                }
            }
        }
        
        // Affect all living entities (players and monsters)
        for (LivingEntity target : center.getWorld().getLivingEntities()) {
            if (target.getLocation().distance(center) > radius) continue;
            
            // Check if target is a player
            if (target instanceof Player) {
                Player player = (Player) target;
                ClassType targetClass = classManager.getPlayerClass(player);
                
                // Also check tag as backup
                if (targetClass == null) {
                    targetClass = classManager.getTagManager().getPlayerClassFromTag(player);
                }
                
                // Skip self (owner doesn't take damage from own field)
                if (player.equals(owner)) {
                    continue;
                }
                
                // In team mode (not practice mode), skip teammates only
                if (gameManager.getGameMode() == GameMode.TEAM && !gameManager.isInPracticeMode(owner)) {
                    if (teamManager.getPlayerTeam(owner) == teamManager.getPlayerTeam(player)) {
                        continue; // Skip all teammates
                    }
                }
                
                // Damage everyone including other Plague Spreaders (다른 역병전파자도 데미지 받음)
                if (targetClass != null) {
                    // Damage, debuff, and poison all players
                    if (isExtreme) {
                        // Extreme: 4 instant damage + Weakness II + Poison II (3 second duration)
                        damageHandler.applyInstantDamage(player, 4.0);
                        effectApplicator.applyEffect(player, PotionEffectType.WEAKNESS, 60, 1);
                        effectApplicator.applyEffect(player, PotionEffectType.POISON, 60, 1);
                    } else {
                        // Virulent: 3 instant damage + Weakness I + Poison I (3 second duration)
                        damageHandler.applyInstantDamage(player, 3.0);
                        effectApplicator.applyEffect(player, PotionEffectType.WEAKNESS, 60, 0);
                        effectApplicator.applyEffect(player, PotionEffectType.POISON, 60, 0);
                    }
                }
            } else {
                // In practice mode, apply poison to monsters
                if (gameManager.isInPracticeMode(owner)) {
                    if (isExtreme) {
                        // Extreme: Poison II (3 second duration)
                        effectApplicator.applyEffect(target, PotionEffectType.POISON, 60, 1);
                    } else {
                        // Virulent: Poison I (3 second duration)
                        effectApplicator.applyEffect(target, PotionEffectType.POISON, 60, 0);
                    }
                }
            }
        }
    }
    
    /**
     * Count specific items in player inventory
     */
    private int countItemsInInventory(Player player, String itemId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && itemId.equals(itemProvider.getItemId(item))) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    /**
     * Remove one engine from inventory
     */
    private void removeOneEngine(Player player, String itemId) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && itemId.equals(itemProvider.getItemId(item))) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                return;
            }
        }
    }
    
    /**
     * Handle player death - stop poison field
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        stopPoisonField(player);
    }
    
    /**
     * Handle game mode change - stop poison field
     */
    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        stopPoisonField(player);
    }
    
    /**
     * Handle player respawn - stop poison field
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        stopPoisonField(player);
    }
    
    /**
     * Clean up player data
     */
    public void cleanup(Player player) {
        stopPoisonField(player);
    }
    
    /**
     * Clean up all active poison fields (called on game end)
     */
    public void cleanupAll() {
        // Stop all active fields
        for (UUID playerId : new HashSet<>(fieldActive.keySet())) {
            // Cancel task
            BukkitTask task = fieldTasks.remove(playerId);
            if (task != null) {
                task.cancel();
            }
            
            // Remove cloud
            AreaEffectCloud cloud = activeFields.remove(playerId);
            if (cloud != null && cloud.isValid()) {
                cloud.remove();
            }
            
            fieldActive.remove(playerId);
        }
    }
    
    /**
     * Clean up poison fields for a specific player
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel task for this player
        BukkitTask task = fieldTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Remove cloud for this player
        AreaEffectCloud cloud = activeFields.remove(playerId);
        if (cloud != null && cloud.isValid()) {
            cloud.remove();
        }
        
        fieldActive.remove(playerId);
    }
}


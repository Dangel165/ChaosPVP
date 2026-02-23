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
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Time Engraver class (시간 각인자)
 * 
 * Abilities:
 * - Time Engrave (시간 각인): AOE slowness + weakness after, gain time preserves
 * - Clock Needle Stitch (시곗바늘 꿰메기): Projectile with bonus damage on slowed targets
 * - Eternal Clock (영원한 시계): Ultimate ability with world-wide CC
 */
public class TimeEngraverHandler implements Listener {
    
    private final Plugin plugin;
    private final ClassManager classManager;
    private final CooldownManager cooldownManager;
    private final ItemProvider itemProvider;
    private final DamageHandler damageHandler;
    private final EffectApplicator effectApplicator;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    
    // Track projectiles
    private final Map<UUID, UUID> needleProjectiles = new HashMap<>(); // Arrow UUID -> Player UUID
    
    // Track time preserves count
    private final Map<UUID, Integer> timePreserves = new HashMap<>();
    
    // Track entities affected by Time Engrave for delayed weakness
    private final Map<UUID, List<UUID>> engraveAffectedEntities = new HashMap<>();
    
    // Track entities frozen by Time Engrave (position lock)
    private final Map<UUID, Location> frozenEntities = new HashMap<>();
    
    // Track eternal clock active state
    private final Map<UUID, Long> eternalClockActive = new HashMap<>();
    
    // Track frozen projectiles
    private final List<UUID> frozenProjectiles = new ArrayList<>();
    
    // Constants
    private static final int TIME_ENGRAVE_COOLDOWN = 16; // seconds
    private static final int CLOCK_NEEDLE_COOLDOWN = 12; // seconds
    private static final int CLOCK_NEEDLE_COOLDOWN_DURING_ETERNAL = 1; // seconds during eternal clock
    private static final double TIME_ENGRAVE_RANGE = 8.0; // blocks (buffed from 5.0)
    private static final double NEEDLE_DAMAGE = 6.0; // nerfed from 10.0
    private static final double NEEDLE_BONUS_DAMAGE = 8.0; // buffed from 6.0
    private static final int TIME_PRESERVES_FOR_ETERNAL = 7;
    private static final int ETERNAL_CLOCK_DURATION = 8; // seconds
    
    public TimeEngraverHandler(Plugin plugin, ClassManager classManager, CooldownManager cooldownManager,
                              ItemProvider itemProvider, DamageHandler damageHandler, EffectApplicator effectApplicator,
                              TeamManager teamManager, GameManager gameManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.cooldownManager = cooldownManager;
        this.itemProvider = itemProvider;
        this.damageHandler = damageHandler;
        this.effectApplicator = effectApplicator;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }
    
    /**
     * Enforce position lock for frozen entities (players)
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
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
     * Handle player interactions (right-click for abilities)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (classManager.getPlayerClass(player) != ClassType.TIME_ENGRAVER) {
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
        
        if (itemId.equals("time_engrave")) {
            event.setCancelled(true);
            handleTimeEngrave(player);
        } else if (itemId.equals("clock_needle_stitch")) {
            event.setCancelled(true);
            handleClockNeedleStitch(player);
        } else if (itemId.equals("eternal_clock")) {
            event.setCancelled(true);
            handleEternalClock(player, item);
        }
    }
    
    /**
     * Handle Time Engrave ability (시간 각인)
     */
    private void handleTimeEngrave(Player player) {
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.TIME_ENGRAVE)) {
            double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.TIME_ENGRAVE);
            player.sendMessage("§c시간 각인 쿨다운: " + String.format("%.1f", remaining) + "초");
            return;
        }
        
        Location playerLoc = player.getLocation();
        List<UUID> affectedEntities = new ArrayList<>();
        int affectedCount = 0;
        
        // Find all entities within range
        for (Entity entity : player.getNearbyEntities(TIME_ENGRAVE_RANGE, TIME_ENGRAVE_RANGE, TIME_ENGRAVE_RANGE)) {
            if (!(entity instanceof LivingEntity) || entity == player) {
                continue;
            }
            
            LivingEntity target = (LivingEntity) entity;
            
            // Check team restrictions (only in team mode, not in practice mode)
            if (target instanceof Player) {
                Player targetPlayer = (Player) target;
                
                // In team mode (not practice), skip same team
                // But allow affecting teammates in practice mode or solo mode
                if (gameManager.getGameMode() == com.verminpvp.models.GameMode.TEAM && !gameManager.isInPracticeMode(player)) {
                    Team playerTeam = teamManager.getPlayerTeam(player);
                    Team targetTeam = teamManager.getPlayerTeam(targetPlayer);
                    if (playerTeam != null && playerTeam == targetTeam) {
                        continue; // Skip same team in team mode
                    }
                }
            }
            
            // Apply Slowness V for 2 seconds (buffed from 1 second)
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 4, false, true, true));
            
            // Freeze position (complete position lock like Navigator's wave riding)
            Location frozenLoc = target.getLocation().clone();
            frozenEntities.put(target.getUniqueId(), frozenLoc);
            
            affectedEntities.add(target.getUniqueId());
            affectedCount++;
        }
        
        // Store affected entities for delayed weakness
        if (!affectedEntities.isEmpty()) {
            engraveAffectedEntities.put(player.getUniqueId(), affectedEntities);
            
            // Schedule weakness application after 2 seconds (changed from 1 second)
            new BukkitRunnable() {
                @Override
                public void run() {
                    List<UUID> entities = engraveAffectedEntities.remove(player.getUniqueId());
                    if (entities != null) {
                        for (UUID entityId : entities) {
                            Entity entity = Bukkit.getEntity(entityId);
                            if (entity instanceof LivingEntity) {
                                LivingEntity target = (LivingEntity) entity;
                                // Apply Weakness I for 1.5 seconds
                                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, 0, false, true, true));
                            }
                            // Unfreeze entity
                            frozenEntities.remove(entityId);
                        }
                    }
                }
            }.runTaskLater(plugin, 40L); // 2 second delay (changed from 20L)
        }
        
        // Add time preserves
        int currentPreserves = timePreserves.getOrDefault(player.getUniqueId(), 0);
        int newPreserves = Math.min(currentPreserves + affectedCount, TIME_PRESERVES_FOR_ETERNAL);
        timePreserves.put(player.getUniqueId(), newPreserves);
        
        // Check if player can get eternal clock
        if (newPreserves >= TIME_PRESERVES_FOR_ETERNAL) {
            timePreserves.put(player.getUniqueId(), 0); // Reset count
            ItemStack eternalClock = itemProvider.createSpecialItem(ClassType.TIME_ENGRAVER, "eternal_clock");
            player.getInventory().addItem(eternalClock);
            player.sendMessage("§6§l영원한 시계를 획득했습니다!");
        }
        
        // Set cooldown
        cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.TIME_ENGRAVE, TIME_ENGRAVE_COOLDOWN);
        
        player.sendMessage("§e시간 각인 사용! §7" + affectedCount + "명 영향 §e(시간 박제: " + newPreserves + "/" + TIME_PRESERVES_FOR_ETERNAL + ")");
    }
    
    /**
     * Handle Clock Needle Stitch ability (시곗바늘 꿰메기)
     */
    private void handleClockNeedleStitch(Player player) {
        // Check if eternal clock is active for reduced cooldown
        Long eternalEnd = eternalClockActive.get(player.getUniqueId());
        boolean duringEternal = eternalEnd != null && System.currentTimeMillis() < eternalEnd;
        
        int cooldown = duringEternal ? CLOCK_NEEDLE_COOLDOWN_DURING_ETERNAL : CLOCK_NEEDLE_COOLDOWN;
        
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.CLOCK_NEEDLE_STITCH)) {
            double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.CLOCK_NEEDLE_STITCH);
            player.sendMessage("§c시곗바늘 꿰메기 쿨다운: " + String.format("%.1f", remaining) + "초");
            return;
        }
        
        // Shoot arrow projectile
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(player.getLocation().getDirection().multiply(3.0));
        arrow.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setCustomName("§e시곗바늘");
        arrow.setCustomNameVisible(false);
        arrow.setDamage(0.0); // Remove arrow damage
        
        // Track projectile
        needleProjectiles.put(arrow.getUniqueId(), player.getUniqueId());
        
        // Set cooldown
        cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.CLOCK_NEEDLE_STITCH, cooldown);
        
        player.sendMessage("§e시곗바늘 꿰메기 발사!");
    }
    
    /**
     * Handle Eternal Clock ability (영원한 시계)
     */
    private void handleEternalClock(Player player, ItemStack item) {
        // Apply effects to all entities in the world
        int affectedCount = 0;
        
        // Affect all players (including teammates)
        for (Player worldPlayer : Bukkit.getOnlinePlayers()) {
            if (worldPlayer == player) {
                continue;
            }
            
            // Apply effects: Slowness V, Darkness, Blindness for 8 seconds
            worldPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 4, false, true, true));
            worldPlayer.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 160, 0, false, true, true));
            worldPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 0, false, true, true));
            
            // Freeze position (complete position lock like Time Engrave)
            Location frozenLoc = worldPlayer.getLocation().clone();
            frozenEntities.put(worldPlayer.getUniqueId(), frozenLoc);
            
            affectedCount++;
        }
        
        // Affect all living entities (mobs, animals, etc.)
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    
                    // Apply effects: Slowness V, Darkness, Blindness for 8 seconds
                    livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 4, false, true, true));
                    livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 160, 0, false, true, true));
                    livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 0, false, true, true));
                    
                    // Freeze position for mobs too
                    frozenEntities.put(livingEntity.getUniqueId(), livingEntity.getLocation().clone());
                    
                    affectedCount++;
                }
            }
        }
        
        // Freeze all projectiles in the world
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Projectile && !(entity instanceof org.bukkit.entity.FishHook)) {
                    Projectile projectile = (Projectile) entity;
                    // Don't freeze player's own projectiles
                    if (projectile.getShooter() != player) {
                        projectile.setVelocity(new Vector(0, 0, 0));
                        projectile.setGravity(false);
                        frozenProjectiles.add(projectile.getUniqueId());
                    }
                }
            }
        }
        
        // Mark eternal clock as active
        eternalClockActive.put(player.getUniqueId(), System.currentTimeMillis() + (ETERNAL_CLOCK_DURATION * 1000L));
        
        // Store list of frozen entities for this eternal clock
        List<UUID> eternalClockFrozenEntities = new ArrayList<>();
        for (Player worldPlayer : Bukkit.getOnlinePlayers()) {
            if (worldPlayer != player) {
                eternalClockFrozenEntities.add(worldPlayer.getUniqueId());
            }
        }
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    eternalClockFrozenEntities.add(entity.getUniqueId());
                }
            }
        }
        
        // Schedule unfreezing after 8 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                // Unfreeze projectiles
                for (UUID projectileId : frozenProjectiles) {
                    Entity entity = Bukkit.getEntity(projectileId);
                    if (entity instanceof Projectile) {
                        Projectile projectile = (Projectile) entity;
                        projectile.setGravity(true);
                        // Remove the projectile as it's been frozen too long
                        projectile.remove();
                    }
                }
                frozenProjectiles.clear();
                
                // Unfreeze all entities affected by eternal clock
                for (UUID entityId : eternalClockFrozenEntities) {
                    frozenEntities.remove(entityId);
                }
                
                eternalClockActive.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 160L); // 8 seconds
        
        // Remove eternal clock item
        item.setAmount(item.getAmount() - 1);
        
        player.sendMessage("§6§l영원한 시계 발동! §e" + affectedCount + "명 영향");
    }
    
    /**
     * Handle projectile hits
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getHitEntity() instanceof LivingEntity)) return;
        
        Arrow arrow = (Arrow) event.getEntity();
        UUID shooterId = needleProjectiles.remove(arrow.getUniqueId());
        if (shooterId == null) return;
        
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null) return;
        
        LivingEntity target = (LivingEntity) event.getHitEntity();
        
        // Check team restrictions
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            
            if (gameManager.getGameMode() == com.verminpvp.models.GameMode.TEAM && !gameManager.isInPracticeMode(shooter)) {
                Team shooterTeam = teamManager.getPlayerTeam(shooter);
                Team targetTeam = teamManager.getPlayerTeam(targetPlayer);
                if (shooterTeam != null && shooterTeam == targetTeam) {
                    arrow.remove();
                    return;
                }
            }
        }
        
        // Check if target has slowness effect (구속 효과)
        if (target.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            // Apply instant damage of 8 (4 hearts) if target has slowness
            damageHandler.applyInstantDamage(target, 8.0);
            shooter.sendMessage("§e시곗바늘 적중! §6+구속 보너스 피해");
        } else {
            // Apply base damage of 6 (3 hearts)
            damageHandler.applyInstantDamage(target, 6.0);
            shooter.sendMessage("§e시곗바늘 적중!");
        }
        
        // Apply Darkness and Blindness for 3 seconds
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true, true));
        
        // Remove arrow
        arrow.remove();
    }
    
    /**
     * Get time preserves count for a player
     */
    public int getTimePreserves(Player player) {
        return timePreserves.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Cleanup all Time Engraver data
     */
    public void cleanupAll() {
        needleProjectiles.clear();
        timePreserves.clear();
        engraveAffectedEntities.clear();
        eternalClockActive.clear();
        frozenProjectiles.clear();
        frozenEntities.clear();
    }
    
    /**
     * Cleanup Time Engraver data for a specific player
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        timePreserves.remove(playerId);
        engraveAffectedEntities.remove(playerId);
        eternalClockActive.remove(playerId);
        frozenEntities.remove(playerId);
    }
}

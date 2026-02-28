package com.verminpvp.handlers;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.models.AbilityIds;
import com.verminpvp.models.ClassType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Cavalry class abilities
 * 
 * Passive:
 * - No stone sword at start
 * - Starts with stone spear (stone sword with different name)
 * - Permanent Slowness I effect
 * 
 * Abilities:
 * - Mount & Dismount: Summon horse with shared health, dismount heals 6 HP, 22s cooldown
 * - Sweep: 3 block forward AOE attack, 6 damage, 6s cooldown (reduced by 1s per hit)
 */
public class CavalryHandler implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    private final CooldownManager cooldownManager;
    private final ItemProvider itemProvider;
    private final DamageHandler damageHandler;
    
    // Track cavalry horses (Player UUID -> Horse UUID)
    private final Map<UUID, UUID> cavalryHorses = new HashMap<>();
    
    // Track horse health sync (Horse UUID -> Player UUID)
    private final Map<UUID, UUID> horseToPlayer = new HashMap<>();
    
    public CavalryHandler(VerminPVP plugin, ClassManager classManager,
                          CooldownManager cooldownManager, ItemProvider itemProvider,
                          DamageHandler damageHandler) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.cooldownManager = cooldownManager;
        this.itemProvider = itemProvider;
        this.damageHandler = damageHandler;
    }
    
    /**
     * Apply permanent Slowness I passive effect
     */
    public void applyPassiveEffect(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || classManager.getPlayerClass(player) != ClassType.CAVALRY) {
                    cancel();
                    return;
                }
                
                // Apply Slowness I if not already present
                if (!player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 
                        Integer.MAX_VALUE, 0, false, false, false));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Check every second
    }
    
    /**
     * Handle Mount & Dismount and Sweep abilities
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (classManager.getPlayerClass(player) != ClassType.CAVALRY) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        String itemId = itemProvider.getItemId(item);
        if (itemId == null) return;
        
        if (itemId.equals("mount_dismount")) {
            event.setCancelled(true);
            handleMountDismount(player);
        } else if (itemId.equals("sweep")) {
            event.setCancelled(true);
            handleSweep(player);
        }
    }
    
    /**
     * Handle Mount & Dismount ability
     */
    private void handleMountDismount(Player player) {
        // Check if game is active (prevent usage before game starts)
        if (!plugin.getGameManager().isGameActive() && !plugin.getGameManager().isInPracticeMode(player)) {
            player.sendMessage("§c게임이 시작되지 않았습니다!");
            return;
        }
        
        // Check if player is already mounted
        if (player.isInsideVehicle() && player.getVehicle() instanceof Horse) {
            // Check cooldown before dismounting
            if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.MOUNT_DISMOUNT)) {
                long remaining = (long) cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.MOUNT_DISMOUNT);
                player.sendMessage("§c승마&하마 쿨다운: " + remaining + "초");
                return;
            }
            
            // Dismount and heal
            Horse horse = (Horse) player.getVehicle();
            
            // Remove horse
            horse.remove();
            cavalryHorses.remove(player.getUniqueId());
            horseToPlayer.remove(horse.getUniqueId());
            
            // Heal player 6 HP (3 hearts)
            double newHealth = Math.min(player.getHealth() + 6.0, player.getMaxHealth());
            player.setHealth(newHealth);
            
            // Set cooldown for dismount FIRST before message (changed from 22 to 18 seconds)
            cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.MOUNT_DISMOUNT, 18);
            
            // Show cooldown display
            VerminPVP.getInstance().getCooldownDisplay().showCooldown(player, AbilityIds.MOUNT_DISMOUNT, "승마&하마", 18.0);
            
            player.sendMessage("§a말에서 내렸습니다! 체력 6 회복");
            return;
        }
        
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.MOUNT_DISMOUNT)) {
            long remaining = (long) cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.MOUNT_DISMOUNT);
            player.sendMessage("§c승마&하마 쿨다운: " + remaining + "초");
            return;
        }
        
        // Summon horse
        Location spawnLoc = player.getLocation();
        Horse horse = player.getWorld().spawn(spawnLoc, Horse.class);
        
        // Configure horse
        horse.setAdult();
        horse.setTamed(true);
        horse.setOwner(player);
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.setCustomName("§6" + player.getName() + "의 말");
        horse.setCustomNameVisible(true);
        horse.setRemoveWhenFarAway(false); // Prevent horse from despawning
        horse.setPersistent(true); // Make horse persistent
        
        // Set horse health to match player's current health
        double playerHealth = player.getHealth();
        horse.getAttribute(Attribute.MAX_HEALTH).setBaseValue(playerHealth);
        horse.setHealth(playerHealth);
        
        // Track horse
        cavalryHorses.put(player.getUniqueId(), horse.getUniqueId());
        horseToPlayer.put(horse.getUniqueId(), player.getUniqueId());
        
        // Mount player on horse after a short delay to ensure horse is fully spawned
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (horse.isValid() && player.isOnline()) {
                horse.addPassenger(player);
            }
        }, 2L); // 2 ticks delay
        
        // Set cooldown (changed from 22 to 18 seconds)
        cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.MOUNT_DISMOUNT, 18);
        
        // Show cooldown display
        VerminPVP.getInstance().getCooldownDisplay().showCooldown(player, AbilityIds.MOUNT_DISMOUNT, "승마&하마", 18.0);
        
        player.sendMessage("§a말을 소환했습니다!");
    }
    
    /**
     * Handle Sweep ability
     */
    private void handleSweep(Player player) {
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.SWEEP)) {
            long remaining = (long) cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.SWEEP);
            player.sendMessage("§c휩쓸기 쿨다운: " + remaining + "초");
            return;
        }
        
        // Get player's horse UUID if mounted
        UUID playerHorseUUID = cavalryHorses.get(player.getUniqueId());
        
        // Check if player is mounted on their horse
        boolean isMounted = player.isInsideVehicle() && player.getVehicle() instanceof Horse;
        
        // Determine damage based on mounted status
        double damage = isMounted ? 8.0 : 6.0;
        
        // Get player location
        Location playerLoc = player.getLocation();
        
        // Find entities within 3 block radius (diameter 3 = radius 1.5)
        int hitCount = 0;
        for (Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5)) {
            if (!(entity instanceof LivingEntity) || entity == player) continue;
            
            // Skip player's own horse
            if (playerHorseUUID != null && entity.getUniqueId().equals(playerHorseUUID)) continue;
            
            LivingEntity target = (LivingEntity) entity;
            
            // Check if entity is within 1.5 block radius (3 block diameter)
            double distance = target.getLocation().distance(playerLoc);
            
            if (distance <= 1.5) {
                // Deal damage (6 or 8 based on mounted status)
                damageHandler.applyInstantDamage(target, damage);
                hitCount++;
            }
        }
        
        // Calculate cooldown (6s - 1s per hit, minimum 1s)
        int cooldown = Math.max(1, 6 - hitCount);
        
        // Set cooldown
        cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.SWEEP, cooldown);
        
        // Show cooldown display
        VerminPVP.getInstance().getCooldownDisplay().showCooldown(player, AbilityIds.SWEEP, "휩쓸기", (double) cooldown);
        
        String mountedStatus = isMounted ? " (기마 상태)" : "";
        player.sendMessage("§a휩쓸기 사용" + mountedStatus + "! " + hitCount + "명 적중 (쿨다운: " + cooldown + "초)");
    }
    
    /**
     * Handle horse damage - transfer half damage to rider
     * Using HIGH priority to catch damage before it's fully processed
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onHorseDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Horse)) return;
        
        Horse horse = (Horse) event.getEntity();
        UUID playerUUID = horseToPlayer.get(horse.getUniqueId());
        
        if (playerUUID == null) return;
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            // Clean up if player is offline
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (horse.isValid()) {
                    horse.remove();
                }
                horseToPlayer.remove(horse.getUniqueId());
                cavalryHorses.remove(playerUUID);
            });
            return;
        }
        
        // Get the final damage that will be applied
        double damage = event.getFinalDamage();
        if (damage <= 0) return;
        
        // Transfer half damage to player
        double playerDamage = damage / 2.0;
        
        // Apply damage to player using scheduler to avoid conflicts
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.getHealth() > 0) {
                double newHealth = Math.max(0.0, player.getHealth() - playerDamage);
                player.setHealth(newHealth);
                
                // Show damage feedback to player
                player.sendMessage("§c말이 피해를 입었습니다! (당신도 " + String.format("%.1f", playerDamage) + " 피해)");
            }
        });
        
        // Check if horse will die from this damage
        double remainingHealth = horse.getHealth() - damage;
        if (remainingHealth <= 0) {
            // Schedule horse removal for next tick to avoid concurrent modification
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (horse.isValid()) {
                    horse.remove();
                }
                cavalryHorses.remove(playerUUID);
                horseToPlayer.remove(horse.getUniqueId());
                if (player.isOnline()) {
                    player.sendMessage("§c말이 사라졌습니다!");
                }
            });
        }
    }
    
    /**
     * Handle player dismounting - Remove horse when player dismounts
     */
    @EventHandler
    public void onDismount(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof Horse)) return;
        if (!(event.getExited() instanceof Player)) return;
        
        Horse horse = (Horse) event.getVehicle();
        Player player = (Player) event.getExited();
        
        // Check if this is a cavalry horse
        UUID playerUUID = horseToPlayer.get(horse.getUniqueId());
        if (playerUUID == null || !playerUUID.equals(player.getUniqueId())) return;
        
        // Check cooldown before allowing dismount
        if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.MOUNT_DISMOUNT)) {
            // If on cooldown, prevent dismount
            event.setCancelled(true);
            long remaining = (long) cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.MOUNT_DISMOUNT);
            player.sendMessage("§c승마&하마 쿨다운: " + remaining + "초");
            return;
        }
        
        // Remove horse when player dismounts (including shift-dismount)
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (horse.isValid()) {
                horse.remove();
            }
            cavalryHorses.remove(player.getUniqueId());
            horseToPlayer.remove(horse.getUniqueId());
            
            // Heal player 6 HP (3 hearts) when dismounting - only if not already healed by handleMountDismount
            // Check if cooldown was just set (within 1 second) to avoid double healing
            if (!cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.MOUNT_DISMOUNT)) {
                double newHealth = Math.min(player.getHealth() + 6.0, player.getMaxHealth());
                player.setHealth(newHealth);
                
                // Set cooldown for dismount (changed from 22 to 18 seconds)
                cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.MOUNT_DISMOUNT, 18);
                
                // Show cooldown display
                VerminPVP.getInstance().getCooldownDisplay().showCooldown(player, AbilityIds.MOUNT_DISMOUNT, "승마&하마", 18.0);
                
                player.sendMessage("§a말에서 내렸습니다! 체력 6 회복");
            }
        });
    }
    
    /**
     * Cleanup all cavalry horses
     */
    public void cleanupAll() {
        for (UUID horseUUID : new java.util.HashSet<>(horseToPlayer.keySet())) {
            Entity entity = Bukkit.getEntity(horseUUID);
            if (entity instanceof Horse) {
                entity.remove();
            }
        }
        cavalryHorses.clear();
        horseToPlayer.clear();
    }
    
    /**
     * Cleanup cavalry horse for a specific player
     */
    public void cleanupPlayer(Player player) {
        UUID horseUUID = cavalryHorses.get(player.getUniqueId());
        if (horseUUID != null) {
            Entity entity = Bukkit.getEntity(horseUUID);
            if (entity instanceof Horse) {
                entity.remove();
            }
            cavalryHorses.remove(player.getUniqueId());
            horseToPlayer.remove(horseUUID);
        }
    }
}

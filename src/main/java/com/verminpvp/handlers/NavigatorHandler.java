package com.verminpvp.handlers;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.models.AbilityIds;
import com.verminpvp.models.ClassType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Navigator class abilities
 * 
 * Abilities:
 * - Starting sword with Knockback I
 * - Naval Combat generation (every 20s, max 3, durability 1)
 * - Wave Riding (Speed IV 1.5s, 11s cooldown, collision knockback)
 * - Harpoon Throw (6 damage, Slowness V 1.5s, 15s cooldown, 50% reduction on miss)
 * - Naval Combat attack (1 base + 4 instant damage, Knockback II)
 */
public class NavigatorHandler implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    private final CooldownManager cooldownManager;
    private final ItemProvider itemProvider;
    private final EffectApplicator effectApplicator;
    private final DamageHandler damageHandler;
    
    // Track Wave Riding active players
    private final Map<UUID, Long> waveRidingActive = new HashMap<>();
    
    // Track harpoon projectiles
    private final Map<UUID, UUID> harpoonArrows = new HashMap<>(); // Arrow UUID -> Player UUID
    
    public NavigatorHandler(VerminPVP plugin, ClassManager classManager,
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
     * Start Naval Combat generation scheduler
     */
    public void startNavalCombatGeneration(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || classManager.getPlayerClass(player) != ClassType.NAVIGATOR) {
                    cancel();
                    return;
                }
                
                // Count existing Naval Combat items
                int count = countItemsInInventory(player, "naval_combat");
                if (count < 3) {
                    ItemStack navalCombat = itemProvider.createSpecialItem(ClassType.NAVIGATOR, "naval_combat");
                    player.getInventory().addItem(navalCombat);
                    player.sendMessage("§b해전 무기를 받았습니다!");
                }
            }
        }.runTaskTimer(plugin, 20L * 20, 20L * 20); // 20s initial delay, 20s period
    }
    
    /**
     * Handle Wave Riding and Harpoon Throw abilities
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (classManager.getPlayerClass(player) != ClassType.NAVIGATOR) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        String itemId = itemProvider.getItemId(item);
        if (itemId == null) return;
        
        if (itemId.equals("wave_riding_item")) {
            event.setCancelled(true);
            handleWaveRiding(player);
        } else if (itemId.equals("harpoon")) {
            event.setCancelled(true);
            handleHarpoonThrow(player);
        }
    }
    
    /**
     * Handle Wave Riding ability
     */
    private void handleWaveRiding(Player player) {
        if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.WAVE_RIDING)) {
            long remaining = (long) cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.WAVE_RIDING);
            player.sendMessage("§c파도 타기 쿨다운: " + remaining + "초");
            return;
        }
        
        // Apply Speed IV for 1 second (changed from 1.5s)
        effectApplicator.applyEffect(player, PotionEffectType.SPEED, 20, 3);
        
        // Track Wave Riding active state
        waveRidingActive.put(player.getUniqueId(), System.currentTimeMillis() + 1000);
        
        // Set cooldown (changed from 15s to 12s)
        cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.WAVE_RIDING, 12);
        
        // Show cooldown display
        VerminPVP.getInstance().getCooldownDisplay().showCooldown(player, AbilityIds.WAVE_RIDING, "파도 타기", 12.0);
        
        player.sendMessage("§b파도 타기 활성화!");
    }
    
    /**
     * Handle Wave Riding collision effects
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (classManager.getPlayerClass(player) != ClassType.NAVIGATOR) return;
        
        // Check if Wave Riding is active
        Long endTime = waveRidingActive.get(player.getUniqueId());
        if (endTime == null || System.currentTimeMillis() > endTime) {
            waveRidingActive.remove(player.getUniqueId());
            return;
        }
        
        // Check for nearby entities (all living entities in practice mode, only players otherwise)
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                
                // Apply knockback (reduced by 50%)
                Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                direction.setY(0.5); // Add upward component
                target.setVelocity(direction.multiply(1.0)); // Reduced from 2.0 to 1.0
                
                // Apply simple Slowness V for 1s (no position lock)
                effectApplicator.applyEffect(target, PotionEffectType.SLOWNESS, 20, 4);
            }
        }
    }
    
    /**
     * Handle Harpoon Throw ability - throws a trident
     */
    private void handleHarpoonThrow(Player player) {
        // Check cooldown first
        if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.HARPOON_THROW)) {
            long remaining = (long) cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.HARPOON_THROW);
            player.sendMessage("§c삼치창 던지기 쿨다운: " + remaining + "초");
            return;
        }
        
        // Set cooldown BEFORE launching projectile to prevent spam
        cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.HARPOON_THROW, 15);
        
        // Show cooldown display
        VerminPVP.getInstance().getCooldownDisplay().showCooldown(player, AbilityIds.HARPOON_THROW, "삼치창 던지기", 15.0);
        
        // Shoot trident projectile
        org.bukkit.entity.Trident trident = player.launchProjectile(org.bukkit.entity.Trident.class);
        trident.setVelocity(player.getLocation().getDirection().multiply(2.5));
        trident.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
        trident.setCustomName("§b삼치창");
        trident.setCustomNameVisible(false);
        
        // Track trident
        harpoonArrows.put(trident.getUniqueId(), player.getUniqueId());
        
        player.sendMessage("§b삼치창 발사!");
        
        // Schedule miss detection (after 3 seconds)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (harpoonArrows.containsKey(trident.getUniqueId())) {
                    // Trident missed - reduce cooldown by 50%
                    cooldownManager.reduceCooldown(player.getUniqueId(), AbilityIds.HARPOON_THROW, 0.5);
                    harpoonArrows.remove(trident.getUniqueId());
                    player.sendMessage("§e삼치창 빗나감! 쿨다운 50% 감소");
                    trident.remove();
                }
            }
        }.runTaskLater(plugin, 60L); // 3 seconds
    }
    
    /**
     * Handle Harpoon hit
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Trident)) return;
        if (!(event.getHitEntity() instanceof LivingEntity)) return;
        
        org.bukkit.entity.Trident trident = (org.bukkit.entity.Trident) event.getEntity();
        UUID shooterId = harpoonArrows.get(trident.getUniqueId());
        if (shooterId == null) return;
        
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null) return;
        
        LivingEntity target = (LivingEntity) event.getHitEntity();
        
        // Apply Slowness V for 1 second (changed from 1.5s)
        effectApplicator.applyEffect(target, PotionEffectType.SLOWNESS, 20, 4);
        
        // Remove from tracking (hit, not miss)
        harpoonArrows.remove(trident.getUniqueId());
        
        // Remove trident
        trident.remove();
        
        shooter.sendMessage("§b삼치창 적중!");
    }
    
    /**
     * Handle Harpoon damage - override trident damage
     * Using HIGHEST priority to ensure we cancel before other plugins process it
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onHarpoonDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof org.bukkit.entity.Trident)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        org.bukkit.entity.Trident trident = (org.bukkit.entity.Trident) event.getDamager();
        UUID shooterId = harpoonArrows.get(trident.getUniqueId());
        if (shooterId == null) return;
        
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null) return;
        
        // Cancel trident's default damage
        event.setCancelled(true);
        
        LivingEntity target = (LivingEntity) event.getEntity();
        
        // Apply 6 damage (buffed from 5) - skill damage only, no trident damage
        damageHandler.applyInstantDamage(target, 6.0);
    }
    
    /**
     * Handle Naval Combat attack and prevent trident melee attacks
     * Using HIGHEST priority to ensure we cancel trident attacks before damage is applied
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onNavalCombatAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return; // Don't process already cancelled events
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        Player attacker = (Player) event.getDamager();
        if (classManager.getPlayerClass(attacker) != ClassType.NAVIGATOR) return;
        
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null) return;
        
        // Check if holding trident - cancel all trident melee attacks (silently)
        if (weapon.getType() == Material.TRIDENT) {
            event.setCancelled(true);
            event.setDamage(0.0); // Set damage to 0
            return;
        }
        
        String itemId = itemProvider.getItemId(weapon);
        if (itemId == null || !itemId.equals("naval_combat")) return;
        
        LivingEntity target = (LivingEntity) event.getEntity();
        
        // Modify damage instead of cancelling and reapplying
        // Set base damage to 1, then add 4 instant damage separately
        event.setDamage(1.0);
        
        // Apply additional instant damage (bypasses armor)
        damageHandler.applyInstantDamage(target, 4.0);
        
        // Apply knockback (Knockback II effect - nerfed from III)
        Vector direction = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        direction.setY(0.4);
        target.setVelocity(direction.multiply(1.2)); // Reduced from 1.5
        
        attacker.sendMessage("§b해전 무기 공격!");
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
     * Clean up player data
     */
    public void cleanup(Player player) {
        waveRidingActive.remove(player.getUniqueId());
    }
}

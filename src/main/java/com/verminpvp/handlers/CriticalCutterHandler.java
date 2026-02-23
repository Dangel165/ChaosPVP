package com.verminpvp.handlers;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.AbilityIds;
import com.verminpvp.models.ClassData;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.GameMode;
import com.verminpvp.models.Team;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Handler for Critical Cutter class abilities
 * 
 * Passive:
 * - Starts with "크리티컬 커터" sword instead of stone sword
 * - Has "확정 크리티컬" item
 * 
 * 크리티컬 커터 Sword:
 * - Same damage as stone sword
 * - Attack speed 2.2 (fast)
 * - 5% base critical hit chance on attack
 * - Critical hit: deals additional instant damage equal to dealt damage, heals 2 HP (1 heart)
 * - Each non-crit attack increases crit chance by 1%
 * 
 * 확정 크리티컬 (Guaranteed Critical):
 * - Right-click to attack forward once
 * - Deals 6 damage (3 hearts)
 * - Deals additional instant damage equal to dealt damage, heals 6 HP (3 hearts)
 * - No cooldown mentioned, so no cooldown
 */
public class CriticalCutterHandler implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    private final CooldownManager cooldownManager;
    private final ItemProvider itemProvider;
    private final DamageHandler damageHandler;
    private final Random random;
    
    public CriticalCutterHandler(VerminPVP plugin, ClassManager classManager, 
                                  CooldownManager cooldownManager, ItemProvider itemProvider,
                                  DamageHandler damageHandler) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.cooldownManager = cooldownManager;
        this.itemProvider = itemProvider;
        this.damageHandler = damageHandler;
        this.random = new Random();
    }
    
    /**
     * Start action bar display for a Critical Cutter
     */
    public void startActionBarDisplay(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || classManager.getPlayerClass(player) != ClassType.CRITICAL_CUTTER) {
                    cancel();
                    return;
                }
                
                // Get current crit chance
                ClassData data = classManager.getClassData(player);
                double critChance = data.getCritChance();
                
                // Display in action bar
                String message = String.format("§e크리티컬 확률: §c%.1f%%", critChance);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            }
        }.runTaskTimer(plugin, 0L, 10L); // Update every 0.5 seconds
    }
    
    /**
     * Handle 확정 크리티컬 right-click
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (classManager.getPlayerClass(player) != ClassType.CRITICAL_CUTTER) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        String itemId = itemProvider.getItemId(item);
        if (itemId == null) return;
        
        if (itemId.equals("guaranteed_critical")) {
            event.setCancelled(true);
            
            // Check cooldown (25 seconds)
            if (cooldownManager.isOnCooldown(player.getUniqueId(), "guaranteed_critical")) {
                double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), "guaranteed_critical");
                player.sendMessage("§c확정 크리티컬 쿨타임: " + String.format("%.1f", remaining) + "초");
                return;
            }
            
            handleGuaranteedCritical(player);
            
            // Set cooldown (25 seconds)
            cooldownManager.setCooldown(player.getUniqueId(), "guaranteed_critical", 25.0);
        }
    }
    
    /**
     * Handle 확정 크리티컬 ability
     */
    private void handleGuaranteedCritical(Player player) {
        // Get target in front of player (within 3 blocks)
        org.bukkit.entity.LivingEntity target = getTargetInFront(player, 3.0);
        
        if (target == null) {
            player.sendMessage("§c대상을 찾을 수 없습니다!");
            return;
        }
        
        // Check if target is a teammate in team mode (not practice mode)
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            GameManager gameManager = plugin.getGameManager();
            TeamManager teamManager = plugin.getTeamManager();
            
            if (gameManager.getGameMode() == GameMode.TEAM && !gameManager.isInPracticeMode(player)) {
                Team attackerTeam = teamManager.getPlayerTeam(player);
                Team targetTeam = teamManager.getPlayerTeam(targetPlayer);
                
                if (attackerTeam != null && attackerTeam == targetTeam) {
                    player.sendMessage("§c아군을 공격할 수 없습니다!");
                    return;
                }
            }
        }
        
        // Deal 6 base damage
        double baseDamage = 6.0;
        target.damage(baseDamage, player);
        
        // Deal additional instant damage equal to base damage
        damageHandler.applyInstantDamage(target, baseDamage);
        
        // Heal player 6 HP (3 hearts)
        double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + 6.0);
        player.setHealth(newHealth);
        
        // Visual and audio feedback
        player.sendMessage("§c§l확정 크리티컬! §e12.0 데미지");
        target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.2);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
        
        // Knockback
        Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        direction.setY(0.3);
        target.setVelocity(direction.multiply(0.5));
    }
    
    /**
     * Get target entity in front of player
     */
    private org.bukkit.entity.LivingEntity getTargetInFront(Player player, double range) {
        Vector direction = player.getLocation().getDirection();
        org.bukkit.Location start = player.getEyeLocation();
        
        // Raycast to find target
        for (double i = 0; i <= range; i += 0.5) {
            org.bukkit.Location check = start.clone().add(direction.clone().multiply(i));
            
            // Check for entities at this location
            for (org.bukkit.entity.Entity entity : check.getWorld().getNearbyEntities(check, 0.5, 0.5, 0.5)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                    return (org.bukkit.entity.LivingEntity) entity;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Handle critical hit system for 크리티컬 커터 sword
     */
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof org.bukkit.entity.LivingEntity)) return;
        
        Player attacker = (Player) event.getDamager();
        if (classManager.getPlayerClass(attacker) != ClassType.CRITICAL_CUTTER) return;
        
        // Check if using 크리티컬 커터 sword
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null) return;
        
        String itemId = itemProvider.getItemId(weapon);
        if (itemId == null || !itemId.equals("critical_cutter_sword")) return;
        
        // Get class data for crit chance
        ClassData data = classManager.getClassData(attacker);
        double critChance = data.getCritChance();
        
        // Roll for critical hit
        double roll = random.nextDouble() * 100.0;
        
        // Adjust damage to 5.0 (stone sword level) since iron sword base is 6.0
        // Save the adjusted damage BEFORE modifying the event
        double adjustedDamage = 5.0;
        event.setDamage(adjustedDamage);
        
        if (roll < critChance) {
            // Critical hit! Pass the adjusted damage (5.0)
            handleCriticalHit(event, attacker, data, adjustedDamage);
        } else {
            // Non-crit: increase crit chance by 1%
            double newCritChance = Math.min(100.0, critChance + 1.0);
            data.setCritChance(newCritChance);
        }
    }
    
    /**
     * Handle critical hit effects for 크리티컬 커터 sword
     */
    private void handleCriticalHit(EntityDamageByEntityEvent event, Player attacker, ClassData data, double baseDamage) {
        org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) event.getEntity();
        
        // Deal additional instant damage equal to base damage (5.0)
        damageHandler.applyInstantDamage(target, baseDamage);
        
        // Heal attacker 2 HP (1 heart)
        double newHealth = Math.min(attacker.getMaxHealth(), attacker.getHealth() + 2.0);
        attacker.setHealth(newHealth);
        
        // DON'T reset crit chance - keep it as is
        // Crit chance only increases on non-crit attacks
        
        // Visual feedback
        double totalDamage = baseDamage * 2.0; // Base + instant
        attacker.sendMessage("§c§l크리티컬 적중! §e" + String.format("%.1f", totalDamage) + " 데미지");
        target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        attacker.playSound(attacker.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
    }
}

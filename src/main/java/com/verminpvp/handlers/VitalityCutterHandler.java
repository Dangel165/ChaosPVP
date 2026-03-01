package com.verminpvp.handlers;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.GameMode;
import com.verminpvp.models.Team;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handler for Vitality Cutter class abilities
 * 
 * Passive:
 * - Does not start with stone sword
 * - Left-click attack: deals additional instant damage equal to 1/6 of target's current health
 * - Left-click attack: heals 1 HP (0.5 hearts)
 * - Left-click attack: cannot kill (minimum 0.5 HP)
 * - Player name tag shows class name
 * - Glowing effect in FFA mode
 * 
 * Life Cut (생명 절단):
 * - Right-click: Deal 2 instant damage (1 heart) to target within 5 blocks
 * - Consumes item after use
 */
public class VitalityCutterHandler implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    private final DamageHandler damageHandler;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final ItemProvider itemProvider;
    
    public VitalityCutterHandler(VerminPVP plugin, ClassManager classManager, 
                                  DamageHandler damageHandler, GameManager gameManager,
                                  TeamManager teamManager, ItemProvider itemProvider) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.damageHandler = damageHandler;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.itemProvider = itemProvider;
    }
    
    /**
     * Handle Vitality Cutter passive on attack
     */
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof org.bukkit.entity.LivingEntity)) return;
        
        Player attacker = (Player) event.getDamager();
        if (classManager.getPlayerClass(attacker) != ClassType.VITALITY_CUTTER) return;
        
        org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) event.getEntity();
        
        // Check if target is a teammate in team mode (not practice mode)
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            
            if (gameManager.getGameMode() == GameMode.TEAM && !gameManager.isInPracticeMode(attacker)) {
                Team attackerTeam = teamManager.getPlayerTeam(attacker);
                Team targetTeam = teamManager.getPlayerTeam(targetPlayer);
                
                if (attackerTeam != null && attackerTeam == targetTeam) {
                    return; // Don't apply passive to teammates
                }
            }
        }
        
        // Modify base damage to 0 (prevent killing with left-click)
        event.setDamage(0.0);
        
        // Calculate 1/6 of target's current health
        double currentHealth = target.getHealth();
        double bonusDamage = currentHealth / 6.0;
        
        // Apply instant damage (1/6 of current health) but ensure target stays above 0.5 HP
        double newHealth = Math.max(0.5, currentHealth - bonusDamage);
        target.setHealth(newHealth);
        
        // Heal attacker 1 HP (0.5 hearts)
        double attackerNewHealth = Math.min(attacker.getMaxHealth(), attacker.getHealth() + 1.0);
        attacker.setHealth(attackerNewHealth);
        
        // Visual feedback
        attacker.sendMessage("§c활력 절단! §e" + String.format("%.1f", bonusDamage) + " 데미지");
        target.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, 
            target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
    }
    
    /**
     * Handle Life Cut ability (right-click)
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (classManager.getPlayerClass(player) != ClassType.VITALITY_CUTTER) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        String itemId = itemProvider.getItemId(item);
        if (itemId == null || !itemId.equals("life_cut")) return;
        
        event.setCancelled(true);
        
        // Find nearest entity within 5 blocks
        LivingEntity target = null;
        double minDistance = 5.0;
        
        for (Entity entity : player.getNearbyEntities(5.0, 5.0, 5.0)) {
            if (!(entity instanceof LivingEntity) || entity == player) continue;
            
            LivingEntity livingEntity = (LivingEntity) entity;
            
            // Check if target is a teammate in team mode (not practice mode)
            if (livingEntity instanceof Player) {
                Player targetPlayer = (Player) livingEntity;
                
                if (gameManager.getGameMode() == GameMode.TEAM && !gameManager.isInPracticeMode(player)) {
                    Team playerTeam = teamManager.getPlayerTeam(player);
                    Team targetTeam = teamManager.getPlayerTeam(targetPlayer);
                    
                    if (playerTeam != null && playerTeam == targetTeam) {
                        continue; // Skip teammates
                    }
                }
            }
            
            double distance = player.getLocation().distance(livingEntity.getLocation());
            if (distance < minDistance) {
                minDistance = distance;
                target = livingEntity;
            }
        }
        
        if (target == null) {
            player.sendMessage("§c범위 내에 적이 없습니다!");
            return;
        }
        
        // Deal 2 instant damage (1 heart)
        damageHandler.applyInstantDamage(target, 2.0);
        
        // Visual effect
        target.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, 
            target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
        
        player.sendMessage("§4생명 절단! §c2 즉시 피해");
        
        // Remove item after use
        item.setAmount(item.getAmount() - 1);
    }
}

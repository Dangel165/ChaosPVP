package com.verminpvp.handlers;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.GameMode;
import com.verminpvp.models.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Handler for Vitality Cutter class abilities
 * 
 * Passive:
 * - Does not start with stone sword
 * - Left-click attack: deals additional instant damage equal to 1/6 of target's current health
 * - Left-click attack: heals 1 HP (0.5 hearts)
 * - Player name tag shows class name
 * - Glowing effect in FFA mode
 */
public class VitalityCutterHandler implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    private final DamageHandler damageHandler;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    
    public VitalityCutterHandler(VerminPVP plugin, ClassManager classManager, 
                                  DamageHandler damageHandler, GameManager gameManager,
                                  TeamManager teamManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.damageHandler = damageHandler;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
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
        
        // Calculate 1/6 of target's current health
        double currentHealth = target.getHealth();
        double bonusDamage = currentHealth / 6.0;
        
        // Apply instant damage (1/6 of current health)
        damageHandler.applyInstantDamage(target, bonusDamage);
        
        // Heal attacker 1 HP (0.5 hearts)
        double newHealth = Math.min(attacker.getMaxHealth(), attacker.getHealth() + 1.0);
        attacker.setHealth(newHealth);
        
        // Visual feedback
        double totalDamage = event.getDamage() + bonusDamage;
        attacker.sendMessage("§c활력 절단! §e" + String.format("%.1f", totalDamage) + " 데미지");
        target.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, 
            target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
    }
}

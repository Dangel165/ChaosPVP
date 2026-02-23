package com.verminpvp.handlers;

import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Prevents team killing in team mode
 */
public class TeamKillPreventionHandler implements Listener {
    
    private final GameManager gameManager;
    private final TeamManager teamManager;
    
    public TeamKillPreventionHandler(GameManager gameManager, TeamManager teamManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Only prevent team kill in team mode
        if (gameManager.getGameMode() != GameMode.TEAM) {
            return;
        }
        
        // Check if victim is a player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = null;
        
        // Check if attacker is a player (direct or projectile)
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }
        
        // If attacker is a player, check if same team
        if (attacker != null) {
            if (teamManager.areSameTeam(attacker, victim)) {
                event.setCancelled(true);
                attacker.sendMessage("§c같은 팀은 공격할 수 없습니다!");
            }
        }
    }
}

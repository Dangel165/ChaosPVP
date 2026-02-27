package com.verminpvp.handlers;

import com.verminpvp.managers.GameManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Prevents PVP and movement during freeze period (first 10 seconds of game)
 */
public class FreezeProtectionHandler implements Listener {
    
    private final GameManager gameManager;
    
    public FreezeProtectionHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Only handle player vs player damage
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        
        // Check if game is active
        if (!gameManager.isGameActive()) {
            return;
        }
        
        // Check if in freeze period using GameManager
        if (gameManager.isInFreezePeriod()) {
            // Cancel damage during freeze period
            event.setCancelled(true);
            attacker.sendMessage("§c프리즈 기간 동안에는 공격할 수 없습니다!");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check if game is active
        if (!gameManager.isGameActive()) {
            return;
        }
        
        // Check if in freeze period using GameManager
        if (gameManager.isInFreezePeriod()) {
            // Get from and to locations
            Location from = event.getFrom();
            Location to = event.getTo();
            
            // If player moved (not just looking around)
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                // Cancel movement, keep rotation
                Location newLoc = from.clone();
                newLoc.setYaw(to.getYaw());
                newLoc.setPitch(to.getPitch());
                event.setTo(newLoc);
            }
        }
    }
}

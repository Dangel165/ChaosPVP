package com.verminpvp.handlers;

import com.verminpvp.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PotionSplashEvent;

/**
 * Handles world protection during active games
 * Prevents block breaking, placing, explosions, and pre-game PvP damage
 */
public class WorldProtectionHandler implements Listener {
    
    private final GameManager gameManager;
    
    public WorldProtectionHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    /**
     * Prevent all PvP damage before game starts (lobby protection)
     * This includes direct attacks, projectiles, and splash potions
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Only protect players
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        // Only block damage when game is NOT active (lobby protection)
        if (gameManager.isGameActive()) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        
        // Check if damage is from another player (direct or projectile)
        Player attacker = null;
        
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }
        
        // If attacker is a player, cancel the damage (lobby protection)
        if (attacker != null) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevent splash potion effects on other players before game starts
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPotionSplash(PotionSplashEvent event) {
        // Only block when game is NOT active (lobby protection)
        if (gameManager.isGameActive()) {
            return;
        }
        
        // Check if thrower is a player
        if (!(event.getPotion().getShooter() instanceof Player)) {
            return;
        }
        
        Player thrower = (Player) event.getPotion().getShooter();
        
        // Remove all affected players except the thrower (allow self-application)
        event.getAffectedEntities().removeIf(entity -> 
            entity instanceof Player && !entity.equals(thrower)
        );
    }
    
    /**
     * Prevent block breaking during active game
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (gameManager.isGameActive()) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevent block placing during active game
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (gameManager.isGameActive()) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevent explosions (TNT, Creeper, etc.) during active game
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (gameManager.isGameActive()) {
            event.blockList().clear(); // Clear the list of blocks to be destroyed
        }
    }
}

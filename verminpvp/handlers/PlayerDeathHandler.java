package com.verminpvp.handlers;

import com.verminpvp.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handles player death events during games
 * Sets dead players to spectator mode when game is active
 * Checks for win conditions after each death
 */
public class PlayerDeathHandler implements Listener {
    
    private final GameManager gameManager;
    
    public PlayerDeathHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // If event is cancelled (e.g., by UndeadHandler for revival), don't process death
        if (event.isCancelled()) {
            Bukkit.getLogger().info("[VerminPVP] Death event cancelled for " + player.getName() + ", skipping death handling");
            return;
        }
        
        // Check if player is Undead and this is their first death (they will revive)
        // Don't set to spectator or check win condition because they will revive
        com.verminpvp.managers.ClassManager classManager = ((com.verminpvp.VerminPVP) gameManager.getPlugin()).getClassManager();
        com.verminpvp.models.ClassData classData = classManager.getClassData(player);
        if (classData != null && classData.getClassType() == com.verminpvp.models.ClassType.UNDEAD) {
            // Check if this is their first death (they haven't revived yet)
            com.verminpvp.handlers.UndeadHandler undeadHandler = ((com.verminpvp.VerminPVP) gameManager.getPlugin()).getUndeadHandler();
            if (undeadHandler != null && !undeadHandler.hasPlayerRevived(player)) {
                // This is their first death, they will revive
                // Don't set to spectator, don't check win condition
                // The UndeadHandler will handle the revival
                Bukkit.getLogger().info("[VerminPVP] Undead first death detected for " + player.getName() + ", skipping spectator mode");
                return;
            }
        }
        
        // Check if player is in practice mode
        if (gameManager.isInPracticeMode(player)) {
            // In practice mode, teleport to practice map and end practice mode
            player.sendMessage("§c사망했습니다! 연습모드를 종료합니다.");
            
            // Respawn player and end practice mode after a short delay
            Bukkit.getScheduler().runTaskLater(gameManager.getPlugin(), () -> {
                player.spigot().respawn();
                
                // Teleport to practice map
                org.bukkit.Location practiceMap = ((com.verminpvp.VerminPVP) gameManager.getPlugin()).getGameManager().getMapManager().getPracticeMap();
                if (practiceMap != null) {
                    player.teleport(practiceMap);
                }
                
                // End practice mode (cleanup and restore to creative)
                endPracticeMode(player);
                
            }, 10L); // 0.5 second delay
            
            return;
        }
        
        // Check if game is active
        if (!gameManager.isGameActive()) {
            return;
        }
        
        // Set player to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        
        // Send message to player
        player.sendMessage("§c사망했습니다! 관전 모드로 전환됩니다.");
        
        // Broadcast death message
        String deathMessage = "§7" + player.getName() + "님이 탈락했습니다!";
        player.getServer().broadcastMessage(deathMessage);
        
        // Debug message
        Bukkit.getLogger().info("[VerminPVP] Player died: " + player.getName() + ", checking win condition...");
        
        // Check win condition after a short delay (to ensure player is fully set to spectator)
        Bukkit.getScheduler().runTaskLater(gameManager.getPlugin(), () -> {
            if (gameManager.isGameActive()) {
                Bukkit.getLogger().info("[VerminPVP] Game is active, calling checkWinCondition()");
                gameManager.checkWinCondition();
            } else {
                Bukkit.getLogger().info("[VerminPVP] Game is not active, skipping win condition check");
            }
        }, 5L); // 0.25 second delay
    }
    
    /**
     * End practice mode and restore player to normal state
     */
    private void endPracticeMode(Player player) {
        // Get managers
        com.verminpvp.managers.ClassManager classManager = ((com.verminpvp.VerminPVP) gameManager.getPlugin()).getClassManager();
        
        // Clean up all class-specific effects for this player
        cleanupPlayerClassEffects(player);
        
        // Clear player class (this will also stop schedulers)
        classManager.clearPlayerClass(player);
        
        // Clear inventory
        player.getInventory().clear();
        
        // Remove ALL potion effects
        for (org.bukkit.potion.PotionEffectType effectType : org.bukkit.potion.PotionEffectType.values()) {
            if (effectType != null && player.hasPotionEffect(effectType)) {
                player.removePotionEffect(effectType);
            }
        }
        
        // Remove game effects
        gameManager.removeGameEffects(player);
        
        // Remove practice mode flag
        gameManager.setPracticeMode(player, false);
        
        // Re-enable natural regeneration in player's world
        if (player.getWorld() != null) {
            player.getWorld().setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, true);
        }
        
        // Set to creative mode
        player.setGameMode(org.bukkit.GameMode.CREATIVE);
        
        // Restore max health to default (20 HP = 10 hearts)
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
        
        player.sendMessage("§a연습모드가 종료되었습니다.");
        player.sendMessage("§7크리에이티브 모드로 복귀했습니다.");
    }
    
    /**
     * Clean up class-specific effects for a single player
     */
    private void cleanupPlayerClassEffects(Player player) {
        com.verminpvp.VerminPVP plugin = (com.verminpvp.VerminPVP) gameManager.getPlugin();
        
        // Clean up Plague Spreader poison fields for this player
        if (plugin.getPlagueSpreaderHandler() != null) {
            plugin.getPlagueSpreaderHandler().cleanupPlayer(player);
        }
        
        // Clean up Shield Soldier passive tasks for this player
        if (plugin.getShieldSoldierHandler() != null) {
            plugin.getShieldSoldierHandler().cleanupPlayer(player);
        }
        
        // Clean up Shapeshifter evolution tasks and disguises for this player
        if (plugin.getShapeshifterHandler() != null) {
            plugin.getShapeshifterHandler().cleanupPlayer(player);
        }
        
        // Clean up Juggler throw time gain tasks for this player
        if (plugin.getJugglerHandler() != null) {
            plugin.getJugglerHandler().cleanupPlayer(player);
        }
    }
}

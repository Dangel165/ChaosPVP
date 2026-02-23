package com.verminpvp.handlers;

import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Handles player join events to restore state during active games
 */
public class PlayerJoinHandler implements Listener {
    
    private final GameManager gameManager;
    private final ClassManager classManager;
    
    public PlayerJoinHandler(GameManager gameManager, ClassManager classManager) {
        this.gameManager = gameManager;
        this.classManager = classManager;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Always give night vision on join (infinite, no particles)
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.NIGHT_VISION, 
            Integer.MAX_VALUE, 0, false, false, false));
        
        // If game is not active, give creative mode and OP
        if (!gameManager.isGameActive()) {
            // Give OP first
            if (!player.isOp()) {
                player.setOp(true);
            }
            
            // Set to creative mode after 5 ticks (to override other plugins)
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                gameManager.getPlugin(), 
                () -> {
                    if (player.isOnline() && !gameManager.isGameActive()) {
                        player.setGameMode(org.bukkit.GameMode.CREATIVE);
                        player.sendMessage("§a크리에이티브 모드로 설정되었습니다.");
                    }
                }, 
                5L
            );
            
            player.sendMessage("§a서버에 접속했습니다!");
            return;
        }
        
        // Check if player had a class before disconnecting
        if (classManager.getPlayerClass(player) != null) {
            // Player was in the game, restore their state
            
            // Set to adventure mode
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            
            // Remove OP during game
            if (player.isOp()) {
                player.setOp(false);
            }
            
            // Set max health to 20 hearts (40 HP)
            player.setMaxHealth(40.0);
            player.setHealth(40.0);
            
            // Apply game effects
            gameManager.applyGameEffects(player);
            
            player.sendMessage("§a게임이 진행 중입니다. 게임 상태가 복구되었습니다.");
        } else {
            // Player was not in the game, set to spectator
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            player.sendMessage("§7게임이 진행 중입니다. 관전 모드로 전환되었습니다.");
        }
    }
}

package com.verminpvp.handlers;

import com.verminpvp.managers.GameManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handles player respawn events during games
 * Ensures players respawn in spectator mode when game is active
 */
public class PlayerRespawnHandler implements Listener {
    
    private final GameManager gameManager;
    
    public PlayerRespawnHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Practice mode deaths are handled in PlayerDeathHandler
        // Practice mode ends on death, so no respawn handling needed
        
        // Check if game is active
        if (!gameManager.isGameActive()) {
            return;
        }
        
        // Get the world where player died
        org.bukkit.World deathWorld = player.getWorld();
        
        // Set respawn location to the world spawn of the death world
        // This ensures players respawn in the same world they died in
        if (deathWorld != null) {
            org.bukkit.Location respawnLocation = deathWorld.getSpawnLocation();
            event.setRespawnLocation(respawnLocation);
        }
        
        // Set player to spectator mode after respawn
        // Use scheduler to ensure it happens after respawn completes
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            gameManager.getPlugin(),
            () -> {
                if (gameManager.isGameActive()) {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage("§7관전 모드로 게임을 지켜보세요.");
                }
            },
            1L
        );
    }
}

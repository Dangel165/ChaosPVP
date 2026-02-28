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
        
        // Get the current map's spawn location from MapManager
        com.verminpvp.VerminPVP plugin = (com.verminpvp.VerminPVP) gameManager.getPlugin();
        com.verminpvp.managers.MapManager mapManager = plugin.getMapManager();
        
        // Get voted map location
        org.bukkit.Location mapSpawn = mapManager.getVotedMap();
        
        // Set respawn location to current map's spawn
        if (mapSpawn != null) {
            event.setRespawnLocation(mapSpawn);
        } else {
            // Fallback to world spawn if map spawn not set
            org.bukkit.World deathWorld = player.getWorld();
            if (deathWorld != null) {
                org.bukkit.Location respawnLocation = deathWorld.getSpawnLocation();
                event.setRespawnLocation(respawnLocation);
            }
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

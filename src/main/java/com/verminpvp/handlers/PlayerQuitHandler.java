package com.verminpvp.handlers;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player quit events to clean up player data
 */
public class PlayerQuitHandler implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    
    public PlayerQuitHandler(VerminPVP plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clear player's class to prevent duplicate class issues in future games
        classManager.clearPlayerClass(player);
        
        // Remove player from ClassManager's tracking
        classManager.removePlayer(player.getUniqueId());
        
        // Clear any resources tracked for this player
        if (plugin.getResourceTracker() != null) {
            plugin.getResourceTracker().clearResources(player.getUniqueId());
        }
        
        plugin.getLogger().info("Cleaned up data for disconnected player: " + player.getName());
    }
}

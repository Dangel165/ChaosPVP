package com.verminpvp.managers;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages excluded players who cannot participate in games
 */
public class ExcludeManager {
    
    private final Set<UUID> excludedPlayers;
    
    public ExcludeManager() {
        this.excludedPlayers = new HashSet<>();
    }
    
    /**
     * Add a player to the exclusion list
     */
    public void excludePlayer(Player player) {
        excludedPlayers.add(player.getUniqueId());
    }
    
    /**
     * Remove a player from the exclusion list
     */
    public void includePlayer(Player player) {
        excludedPlayers.remove(player.getUniqueId());
    }
    
    /**
     * Check if a player is excluded
     */
    public boolean isExcluded(Player player) {
        return excludedPlayers.contains(player.getUniqueId());
    }
    
    /**
     * Clear all exclusions
     */
    public void clearAll() {
        excludedPlayers.clear();
    }
    
    /**
     * Get the number of excluded players
     */
    public int getExcludedCount() {
        return excludedPlayers.size();
    }
}

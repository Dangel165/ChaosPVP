package com.verminpvp.managers;

import org.bukkit.Location;

/**
 * Manages the lobby spawn location
 */
public class LobbyManager {
    
    private Location lobbyLocation = null;
    
    /**
     * Set the lobby spawn location
     */
    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location;
    }
    
    /**
     * Get the lobby spawn location
     */
    public Location getLobbyLocation() {
        return lobbyLocation;
    }
    
    /**
     * Check if lobby location is set
     */
    public boolean hasLobbyLocation() {
        return lobbyLocation != null;
    }
    
    /**
     * Clear the lobby location
     */
    public void clearLobbyLocation() {
        this.lobbyLocation = null;
    }
    
    /**
     * Remove the lobby location (alias for clearLobbyLocation)
     */
    public void removeLobbyLocation() {
        clearLobbyLocation();
    }
}

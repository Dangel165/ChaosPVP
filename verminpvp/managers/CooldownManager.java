package com.verminpvp.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ability cooldowns for all players.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class CooldownManager {
    
    // Map: Player UUID -> (Ability ID -> Cooldown End Time in milliseconds)
    private final Map<UUID, Map<String, Long>> cooldowns;
    
    public CooldownManager() {
        this.cooldowns = new ConcurrentHashMap<>();
    }
    
    /**
     * Checks if an ability is currently on cooldown for a player.
     * 
     * @param playerUuid The player's UUID
     * @param abilityId The ability identifier
     * @return true if the ability is on cooldown, false otherwise
     */
    public boolean isOnCooldown(UUID playerUuid, String abilityId) {
        if (playerUuid == null || abilityId == null) {
            return false;
        }
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerUuid);
        if (playerCooldowns == null) {
            return false;
        }
        
        Long endTime = playerCooldowns.get(abilityId);
        if (endTime == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime >= endTime) {
            // Cooldown expired, remove it
            playerCooldowns.remove(abilityId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Sets a cooldown for an ability.
     * 
     * @param playerUuid The player's UUID
     * @param abilityId The ability identifier
     * @param durationSeconds The cooldown duration in seconds
     */
    public void setCooldown(UUID playerUuid, String abilityId, double durationSeconds) {
        if (playerUuid == null || abilityId == null || durationSeconds <= 0) {
            return;
        }
        
        long endTime = System.currentTimeMillis() + (long)(durationSeconds * 1000);
        
        cooldowns.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                 .put(abilityId, endTime);
    }
    
    /**
     * Reduces the cooldown of an ability by a percentage.
     * 
     * @param playerUuid The player's UUID
     * @param abilityId The ability identifier
     * @param reductionPercent The percentage to reduce (0.0 to 1.0, e.g., 0.5 for 50%)
     */
    public void reduceCooldown(UUID playerUuid, String abilityId, double reductionPercent) {
        if (playerUuid == null || abilityId == null || reductionPercent <= 0) {
            return;
        }
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerUuid);
        if (playerCooldowns == null) {
            return;
        }
        
        Long endTime = playerCooldowns.get(abilityId);
        if (endTime == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long remainingTime = endTime - currentTime;
        
        if (remainingTime <= 0) {
            playerCooldowns.remove(abilityId);
            return;
        }
        
        long reduction = (long)(remainingTime * reductionPercent);
        long newEndTime = endTime - reduction;
        
        if (newEndTime <= currentTime) {
            playerCooldowns.remove(abilityId);
        } else {
            playerCooldowns.put(abilityId, newEndTime);
        }
    }
    
    /**
     * Gets the remaining cooldown time in seconds.
     * 
     * @param playerUuid The player's UUID
     * @param abilityId The ability identifier
     * @return The remaining cooldown in seconds, or 0 if not on cooldown
     */
    public double getRemainingCooldown(UUID playerUuid, String abilityId) {
        if (playerUuid == null || abilityId == null) {
            return 0.0;
        }
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerUuid);
        if (playerCooldowns == null) {
            return 0.0;
        }
        
        Long endTime = playerCooldowns.get(abilityId);
        if (endTime == null) {
            return 0.0;
        }
        
        long currentTime = System.currentTimeMillis();
        long remainingMs = endTime - currentTime;
        
        if (remainingMs <= 0) {
            playerCooldowns.remove(abilityId);
            return 0.0;
        }
        
        return remainingMs / 1000.0;
    }
    
    /**
     * Clears all cooldowns for a player.
     * 
     * @param playerUuid The player's UUID
     */
    public void clearCooldowns(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        
        cooldowns.remove(playerUuid);
    }
    
    /**
     * Clears a specific cooldown for a player.
     * 
     * @param playerUuid The player's UUID
     * @param abilityId The ability identifier
     */
    public void clearCooldown(UUID playerUuid, String abilityId) {
        if (playerUuid == null || abilityId == null) {
            return;
        }
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerUuid);
        if (playerCooldowns != null) {
            playerCooldowns.remove(abilityId);
        }
    }
    
    /**
     * Gets all active cooldowns for a player.
     * 
     * @param playerUuid The player's UUID
     * @return A map of ability IDs to remaining cooldown times in seconds
     */
    public Map<String, Double> getActiveCooldowns(UUID playerUuid) {
        if (playerUuid == null) {
            return new HashMap<>();
        }
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerUuid);
        if (playerCooldowns == null) {
            return new HashMap<>();
        }
        
        Map<String, Double> activeCooldowns = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        
        playerCooldowns.forEach((abilityId, endTime) -> {
            long remainingMs = endTime - currentTime;
            if (remainingMs > 0) {
                activeCooldowns.put(abilityId, remainingMs / 1000.0);
            }
        });
        
        return activeCooldowns;
    }
    
    /**
     * Removes a player from the cooldown system.
     * Should be called when a player leaves or changes class.
     * 
     * @param playerUuid The player's UUID
     */
    public void removePlayer(UUID playerUuid) {
        clearCooldowns(playerUuid);
    }
}

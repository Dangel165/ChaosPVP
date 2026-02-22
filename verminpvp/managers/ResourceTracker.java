package com.verminpvp.managers;

import com.verminpvp.models.ResourceType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ultimate resource collection for all players.
 * Tracks resources like Prismarine Shards (Swordsman) and Ultimate Ingredients (Scientist).
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class ResourceTracker {
    
    // Map: Player UUID -> (ResourceType -> Count)
    private final Map<UUID, Map<ResourceType, Integer>> resources;
    
    public ResourceTracker() {
        this.resources = new ConcurrentHashMap<>();
    }
    
    /**
     * Adds resources to a player's collection.
     * 
     * @param playerUuid The player's UUID
     * @param resourceType The type of resource to add
     * @param amount The amount to add (must be positive)
     */
    public void addResource(UUID playerUuid, ResourceType resourceType, int amount) {
        if (playerUuid == null || resourceType == null || amount <= 0) {
            return;
        }
        
        resources.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                 .merge(resourceType, amount, Integer::sum);
    }
    
    /**
     * Gets the count of a specific resource for a player.
     * 
     * @param playerUuid The player's UUID
     * @param resourceType The type of resource to check
     * @return The resource count, or 0 if none
     */
    public int getResourceCount(UUID playerUuid, ResourceType resourceType) {
        if (playerUuid == null || resourceType == null) {
            return 0;
        }
        
        Map<ResourceType, Integer> playerResources = resources.get(playerUuid);
        if (playerResources == null) {
            return 0;
        }
        
        return playerResources.getOrDefault(resourceType, 0);
    }
    
    /**
     * Checks if a player has enough of a specific resource.
     * 
     * @param playerUuid The player's UUID
     * @param resourceType The type of resource to check
     * @param requiredAmount The required amount
     * @return true if the player has enough resources, false otherwise
     */
    public boolean hasEnoughResources(UUID playerUuid, ResourceType resourceType, int requiredAmount) {
        if (requiredAmount <= 0) {
            return true;
        }
        
        return getResourceCount(playerUuid, resourceType) >= requiredAmount;
    }
    
    /**
     * Consumes (removes) resources from a player's collection.
     * Only consumes if the player has enough resources.
     * 
     * @param playerUuid The player's UUID
     * @param resourceType The type of resource to consume
     * @param amount The amount to consume
     * @return true if resources were consumed, false if not enough resources
     */
    public boolean consumeResources(UUID playerUuid, ResourceType resourceType, int amount) {
        if (playerUuid == null || resourceType == null || amount <= 0) {
            return false;
        }
        
        Map<ResourceType, Integer> playerResources = resources.get(playerUuid);
        if (playerResources == null) {
            return false;
        }
        
        int currentAmount = playerResources.getOrDefault(resourceType, 0);
        if (currentAmount < amount) {
            return false;
        }
        
        int newAmount = currentAmount - amount;
        if (newAmount == 0) {
            playerResources.remove(resourceType);
        } else {
            playerResources.put(resourceType, newAmount);
        }
        
        return true;
    }
    
    /**
     * Clears all resources for a specific resource type for a player.
     * 
     * @param playerUuid The player's UUID
     * @param resourceType The type of resource to clear
     */
    public void clearResource(UUID playerUuid, ResourceType resourceType) {
        if (playerUuid == null || resourceType == null) {
            return;
        }
        
        Map<ResourceType, Integer> playerResources = resources.get(playerUuid);
        if (playerResources != null) {
            playerResources.remove(resourceType);
        }
    }
    
    /**
     * Clears all resources for a player.
     * 
     * @param playerUuid The player's UUID
     */
    public void clearResources(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        
        resources.remove(playerUuid);
    }
    
    /**
     * Sets a specific resource count for a player.
     * Useful for loading saved data or testing.
     * 
     * @param playerUuid The player's UUID
     * @param resourceType The type of resource to set
     * @param amount The amount to set (must be non-negative)
     */
    public void setResourceCount(UUID playerUuid, ResourceType resourceType, int amount) {
        if (playerUuid == null || resourceType == null || amount < 0) {
            return;
        }
        
        if (amount == 0) {
            clearResource(playerUuid, resourceType);
            return;
        }
        
        resources.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                 .put(resourceType, amount);
    }
    
    /**
     * Gets all resources for a player.
     * 
     * @param playerUuid The player's UUID
     * @return A map of resource types to counts (defensive copy)
     */
    public Map<ResourceType, Integer> getAllResources(UUID playerUuid) {
        if (playerUuid == null) {
            return new HashMap<>();
        }
        
        Map<ResourceType, Integer> playerResources = resources.get(playerUuid);
        if (playerResources == null) {
            return new HashMap<>();
        }
        
        return new HashMap<>(playerResources);
    }
    
    /**
     * Removes a player from the resource tracking system.
     * Should be called when a player leaves or changes class.
     * 
     * @param playerUuid The player's UUID
     */
    public void removePlayer(UUID playerUuid) {
        clearResources(playerUuid);
    }
}

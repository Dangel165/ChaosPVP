package com.verminpvp.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores per-player class state including cooldowns, resources, and class-specific data
 */
public class ClassData {
    
    private final UUID playerId;
    private ClassType classType;
    private final Map<String, Long> cooldowns;
    private final Map<ResourceType, Integer> resources;
    private final Map<String, Object> classSpecificData;
    
    public ClassData(UUID playerId, ClassType classType) {
        this.playerId = playerId;
        this.classType = classType;
        this.cooldowns = new HashMap<>();
        this.resources = new HashMap<>();
        this.classSpecificData = new HashMap<>();
        
        // Initialize resources to 0
        for (ResourceType type : ResourceType.values()) {
            resources.put(type, 0);
        }
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public ClassType getClassType() {
        return classType;
    }
    
    public void setClassType(ClassType classType) {
        this.classType = classType;
    }
    
    // Cooldown methods
    public Map<String, Long> getCooldowns() {
        return cooldowns;
    }
    
    // Resource methods
    public int getResourceCount(ResourceType type) {
        return resources.getOrDefault(type, 0);
    }
    
    public void addResource(ResourceType type, int amount) {
        resources.put(type, getResourceCount(type) + amount);
    }
    
    public void setResourceCount(ResourceType type, int amount) {
        resources.put(type, amount);
    }
    
    // Class-specific data methods
    
    /**
     * Get critical hit chance for Critical Cutter class
     * @return The current crit chance percentage (default 5.0)
     */
    public double getCritChance() {
        return (double) classSpecificData.getOrDefault("crit_chance", 5.0);
    }
    
    /**
     * Set critical hit chance for Critical Cutter class
     * @param chance The crit chance percentage
     */
    public void setCritChance(double chance) {
        classSpecificData.put("crit_chance", chance);
    }
    
    /**
     * Get last damage time for Shield Soldier class
     * @return The timestamp of last damage (default 0)
     */
    public long getLastDamageTime() {
        return (long) classSpecificData.getOrDefault("last_damage_time", 0L);
    }
    
    /**
     * Set last damage time for Shield Soldier class
     * @param time The timestamp of damage
     */
    public void setLastDamageTime(long time) {
        classSpecificData.put("last_damage_time", time);
    }
    
    /**
     * Get evolution stage for Shapeshifter class
     * @return The current evolution stage (default 0)
     */
    public int getEvolutionStage() {
        return (int) classSpecificData.getOrDefault("evolution_stage", 0);
    }
    
    /**
     * Set evolution stage for Shapeshifter class
     * @param stage The evolution stage
     */
    public void setEvolutionStage(int stage) {
        classSpecificData.put("evolution_stage", stage);
    }
    
    /**
     * Get a custom class-specific data value
     * @param key The data key
     * @return The data value, or null if not found
     */
    public Object getCustomData(String key) {
        return classSpecificData.get(key);
    }
    
    /**
     * Set a custom class-specific data value
     * @param key The data key
     * @param value The data value
     */
    public void setCustomData(String key, Object value) {
        classSpecificData.put(key, value);
    }
    
    /**
     * Clear all data (used when switching classes)
     */
    public void clear() {
        cooldowns.clear();
        resources.replaceAll((k, v) -> 0);
        classSpecificData.clear();
    }
}

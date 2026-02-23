package com.verminpvp.managers;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Manages spawn locations for game maps with names
 */
public class MapManager {
    
    private final List<Location> spawnLocations;
    private final Map<Location, String> mapNames;
    private Location practiceMap; // Only one practice map allowed
    private String practiceMapName;
    private final Random random;
    private Location lastSelectedMap;
    
    // Map slot system for voting
    private Location slot1Map;
    private String slot1MapName;
    private Location slot2Map;
    private String slot2MapName;
    private Location votedMap; // The map selected by voting
    
    // Dedicated spawn lists for each map slot
    private final List<Location> slot1Spawns;
    private final List<Location> slot2Spawns;
    
    public MapManager() {
        this.spawnLocations = new ArrayList<>();
        this.mapNames = new HashMap<>();
        this.practiceMap = null;
        this.practiceMapName = null;
        this.random = new Random();
        this.lastSelectedMap = null;
        this.slot1Map = null;
        this.slot1MapName = null;
        this.slot2Map = null;
        this.slot2MapName = null;
        this.votedMap = null;
        this.slot1Spawns = new ArrayList<>();
        this.slot2Spawns = new ArrayList<>();
    }
    
    /**
     * Add a spawn location with a name
     */
    public void addSpawnLocation(Location location, String name) {
        spawnLocations.add(location);
        if (name != null && !name.isEmpty()) {
            mapNames.put(location, name);
        }
    }
    
    /**
     * Add a spawn location without a name
     */
    public void addSpawnLocation(Location location) {
        addSpawnLocation(location, null);
    }
    
    /**
     * Remove a spawn location at index
     */
    public boolean removeSpawnLocation(int index) {
        if (index >= 0 && index < spawnLocations.size()) {
            Location loc = spawnLocations.remove(index);
            mapNames.remove(loc);
            return true;
        }
        return false;
    }
    
    /**
     * Clear all spawn locations
     */
    public void clearAllSpawnLocations() {
        spawnLocations.clear();
        mapNames.clear();
    }
    
    /**
     * Get a random spawn location and store it as last selected
     */
    public Location getRandomSpawnLocation() {
        if (spawnLocations.isEmpty()) {
            return null;
        }
        lastSelectedMap = spawnLocations.get(random.nextInt(spawnLocations.size()));
        return lastSelectedMap.clone();
    }
    
    /**
     * Get the name of the last selected map
     */
    public String getLastSelectedMapName() {
        if (lastSelectedMap == null) {
            return null;
        }
        return mapNames.get(lastSelectedMap);
    }
    
    /**
     * Get the name of a specific location
     */
    public String getMapName(Location location) {
        return mapNames.get(location);
    }
    
    /**
     * Get all spawn locations
     */
    public List<Location> getAllSpawnLocations() {
        return new ArrayList<>(spawnLocations);
    }
    
    /**
     * Get the number of spawn locations
     */
    public int getSpawnLocationCount() {
        return spawnLocations.size();
    }
    
    /**
     * Check if there are any spawn locations
     */
    public boolean hasSpawnLocations() {
        return !spawnLocations.isEmpty();
    }
    
    // ========== Practice Mode Map Management ==========
    
    /**
     * Set the practice mode map (only one allowed)
     */
    public void setPracticeMap(Location location, String name) {
        this.practiceMap = location;
        this.practiceMapName = name;
    }
    
    /**
     * Set the practice mode map without a name
     */
    public void setPracticeMap(Location location) {
        setPracticeMap(location, null);
    }
    
    /**
     * Get the practice mode map location
     */
    public Location getPracticeMap() {
        return practiceMap != null ? practiceMap.clone() : null;
    }
    
    /**
     * Get the practice mode map name
     */
    public String getPracticeMapName() {
        return practiceMapName;
    }
    
    /**
     * Check if practice mode map is set
     */
    public boolean hasPracticeMap() {
        return practiceMap != null;
    }
    
    /**
     * Clear the practice mode map
     */
    public void clearPracticeMap() {
        this.practiceMap = null;
        this.practiceMapName = null;
    }
    
    // ========== Map Slot System for Voting ==========
    
    /**
     * Set the first map slot
     */
    public void setSlot1Map(Location location, String name) {
        this.slot1Map = location;
        this.slot1MapName = name;
    }
    
    /**
     * Set the second map slot
     */
    public void setSlot2Map(Location location, String name) {
        this.slot2Map = location;
        this.slot2MapName = name;
    }
    
    /**
     * Get the first map slot location
     */
    public Location getSlot1Map() {
        return slot1Map != null ? slot1Map.clone() : null;
    }
    
    /**
     * Get the second map slot location
     */
    public Location getSlot2Map() {
        return slot2Map != null ? slot2Map.clone() : null;
    }
    
    /**
     * Get the first map slot name
     */
    public String getSlot1MapName() {
        return slot1MapName;
    }
    
    /**
     * Get the second map slot name
     */
    public String getSlot2MapName() {
        return slot2MapName;
    }
    
    /**
     * Check if both map slots are set
     */
    public boolean hasBothMapSlots() {
        return slot1Map != null && slot2Map != null;
    }
    
    /**
     * Check if slot 1 is set
     */
    public boolean hasSlot1Map() {
        return slot1Map != null;
    }
    
    /**
     * Check if slot 2 is set
     */
    public boolean hasSlot2Map() {
        return slot2Map != null;
    }
    
    /**
     * Set the voted map (selected by players)
     */
    public void setVotedMap(Location location) {
        this.votedMap = location;
    }
    
    /**
     * Get the voted map location
     */
    public Location getVotedMap() {
        return votedMap != null ? votedMap.clone() : null;
    }
    
    /**
     * Clear the voted map
     */
    public void clearVotedMap() {
        this.votedMap = null;
    }
    
    /**
     * Check if a location is the "하늘섬" (Sky Island) map
     */
    public boolean isSkyIslandMap(Location location) {
        if (location == null) return false;
        
        // Check if slot1 is sky island
        if (slot1Map != null && slot1MapName != null && 
            slot1MapName.contains("하늘섬") && 
            location.getWorld().equals(slot1Map.getWorld()) &&
            location.distance(slot1Map) < 10) {
            return true;
        }
        
        // Check if slot2 is sky island
        if (slot2Map != null && slot2MapName != null && 
            slot2MapName.contains("하늘섬") && 
            location.getWorld().equals(slot2Map.getWorld()) &&
            location.distance(slot2Map) < 10) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get all spawn locations that belong to a specific map slot
     * Uses dedicated spawn lists for each slot (slot1Spawns, slot2Spawns)
     * Falls back to distance-based matching if dedicated lists are empty
     */
    public List<Location> getSpawnLocationsForSlot(Location slotLocation) {
        if (slotLocation == null || slotLocation.getWorld() == null) {
            return new ArrayList<>();
        }
        
        // Check if this is slot1 or slot2
        if (slot1Map != null && slotLocation.equals(slot1Map)) {
            // Return dedicated slot1 spawns if available
            if (!slot1Spawns.isEmpty()) {
                return new ArrayList<>(slot1Spawns);
            }
        } else if (slot2Map != null && slotLocation.equals(slot2Map)) {
            // Return dedicated slot2 spawns if available
            if (!slot2Spawns.isEmpty()) {
                return new ArrayList<>(slot2Spawns);
            }
        }
        
        // Fallback: use old distance-based matching for backward compatibility
        List<Location> matchingSpawns = new ArrayList<>();
        String slotWorldName = slotLocation.getWorld().getName();
        
        for (Location spawn : spawnLocations) {
            // Must be in the exact same world (by name comparison)
            if (spawn.getWorld() != null && 
                spawn.getWorld().getName().equals(slotWorldName)) {
                
                // Calculate horizontal distance only (ignore Y-axis)
                double dx = spawn.getX() - slotLocation.getX();
                double dz = spawn.getZ() - slotLocation.getZ();
                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
                
                if (horizontalDistance <= 200) {
                    matchingSpawns.add(spawn);
                }
            }
        }
        
        return matchingSpawns;
    }
    
    /**
     * Add a spawn location to slot 1
     */
    public void addSlot1Spawn(Location location) {
        if (location != null) {
            slot1Spawns.add(location);
        }
    }
    
    /**
     * Add a spawn location to slot 2
     */
    public void addSlot2Spawn(Location location) {
        if (location != null) {
            slot2Spawns.add(location);
        }
    }
    
    /**
     * Get all slot 1 spawns
     */
    public List<Location> getSlot1Spawns() {
        return new ArrayList<>(slot1Spawns);
    }
    
    /**
     * Get all slot 2 spawns
     */
    public List<Location> getSlot2Spawns() {
        return new ArrayList<>(slot2Spawns);
    }
    
    /**
     * Get slot 1 spawn count
     */
    public int getSlot1SpawnCount() {
        return slot1Spawns.size();
    }
    
    /**
     * Get slot 2 spawn count
     */
    public int getSlot2SpawnCount() {
        return slot2Spawns.size();
    }
    
    /**
     * Clear all slot 1 spawns
     */
    public void clearSlot1Spawns() {
        slot1Spawns.clear();
    }
    
    /**
     * Clear all slot 2 spawns
     */
    public void clearSlot2Spawns() {
        slot2Spawns.clear();
    }
}

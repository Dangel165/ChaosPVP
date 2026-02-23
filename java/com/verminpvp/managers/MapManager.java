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
    
    public MapManager() {
        this.spawnLocations = new ArrayList<>();
        this.mapNames = new HashMap<>();
        this.practiceMap = null;
        this.practiceMapName = null;
        this.random = new Random();
        this.lastSelectedMap = null;
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
}

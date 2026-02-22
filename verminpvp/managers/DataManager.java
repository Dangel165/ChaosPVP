package com.verminpvp.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Manages persistent data storage for maps and lobby locations
 */
public class DataManager {
    
    private final Plugin plugin;
    private final File dataFile;
    private FileConfiguration config;
    private final MapManager mapManager;
    private final LobbyManager lobbyManager;
    
    public DataManager(Plugin plugin, MapManager mapManager, LobbyManager lobbyManager) {
        this.plugin = plugin;
        this.mapManager = mapManager;
        this.lobbyManager = lobbyManager;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }
    
    /**
     * Load all data from file
     */
    public void loadData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("데이터 파일이 없습니다. 새로 생성됩니다.");
            return;
        }
        
        config = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load lobby location
        if (config.contains("lobby")) {
            Location lobby = deserializeLocation(config.getConfigurationSection("lobby"));
            if (lobby != null) {
                lobbyManager.setLobbyLocation(lobby);
                plugin.getLogger().info("로비 위치 로드 완료");
            }
        }
        
        // Load spawn maps
        if (config.contains("maps")) {
            ConfigurationSection mapsSection = config.getConfigurationSection("maps");
            if (mapsSection != null) {
                for (String key : mapsSection.getKeys(false)) {
                    ConfigurationSection mapSection = mapsSection.getConfigurationSection(key);
                    if (mapSection != null) {
                        Location loc = deserializeLocation(mapSection);
                        String name = mapSection.getString("name");
                        if (loc != null) {
                            mapManager.addSpawnLocation(loc, name);
                        }
                    }
                }
                plugin.getLogger().info("일반 맵 " + mapManager.getSpawnLocationCount() + "개 로드 완료");
            }
        }
        
        // Load practice map
        if (config.contains("practiceMap")) {
            ConfigurationSection practiceSection = config.getConfigurationSection("practiceMap");
            if (practiceSection != null) {
                Location loc = deserializeLocation(practiceSection);
                String name = practiceSection.getString("name");
                if (loc != null) {
                    mapManager.setPracticeMap(loc, name);
                    plugin.getLogger().info("연습 맵 로드 완료");
                }
            }
        }
    }
    
    /**
     * Save all data to file
     */
    public void saveData() {
        config = new YamlConfiguration();
        
        // Save lobby location
        if (lobbyManager.hasLobbyLocation()) {
            ConfigurationSection lobbySection = config.createSection("lobby");
            serializeLocation(lobbySection, lobbyManager.getLobbyLocation());
        }
        
        // Save spawn maps
        List<Location> spawnMaps = mapManager.getAllSpawnLocations();
        if (!spawnMaps.isEmpty()) {
            ConfigurationSection mapsSection = config.createSection("maps");
            for (int i = 0; i < spawnMaps.size(); i++) {
                Location loc = spawnMaps.get(i);
                ConfigurationSection mapSection = mapsSection.createSection("map" + i);
                serializeLocation(mapSection, loc);
                String name = mapManager.getMapName(loc);
                if (name != null) {
                    mapSection.set("name", name);
                }
            }
        }
        
        // Save practice map
        if (mapManager.hasPracticeMap()) {
            ConfigurationSection practiceSection = config.createSection("practiceMap");
            serializeLocation(practiceSection, mapManager.getPracticeMap());
            String name = mapManager.getPracticeMapName();
            if (name != null) {
                practiceSection.set("name", name);
            }
        }
        
        // Write to file
        try {
            // Create plugin data folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(dataFile);
            plugin.getLogger().info("데이터 저장 완료");
        } catch (IOException e) {
            plugin.getLogger().severe("데이터 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Serialize a location to a configuration section
     */
    private void serializeLocation(ConfigurationSection section, Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", loc.getYaw());
        section.set("pitch", loc.getPitch());
    }
    
    /**
     * Deserialize a location from a configuration section
     */
    private Location deserializeLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("월드를 찾을 수 없습니다: " + worldName);
            return null;
        }
        
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        
        return new Location(world, x, y, z, yaw, pitch);
    }
}

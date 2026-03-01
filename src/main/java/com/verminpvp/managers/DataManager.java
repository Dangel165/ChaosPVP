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
        
        // Load map slot 1
        if (config.contains("mapSlot1")) {
            ConfigurationSection slot1Section = config.getConfigurationSection("mapSlot1");
            if (slot1Section != null) {
                Location loc = deserializeLocation(slot1Section);
                String name = slot1Section.getString("name");
                if (loc != null) {
                    mapManager.setSlot1Map(loc, name);
                    plugin.getLogger().info("1번 맵 슬롯 로드 완료");
                }
            }
        }
        
        // Load map slot 2
        if (config.contains("mapSlot2")) {
            ConfigurationSection slot2Section = config.getConfigurationSection("mapSlot2");
            if (slot2Section != null) {
                Location loc = deserializeLocation(slot2Section);
                String name = slot2Section.getString("name");
                if (loc != null) {
                    mapManager.setSlot2Map(loc, name);
                    plugin.getLogger().info("2번 맵 슬롯 로드 완료");
                }
            }
        }
        
        // Load slot 1 spawns
        if (config.contains("slot1Spawns")) {
            ConfigurationSection slot1SpawnsSection = config.getConfigurationSection("slot1Spawns");
            if (slot1SpawnsSection != null) {
                for (String key : slot1SpawnsSection.getKeys(false)) {
                    ConfigurationSection spawnSection = slot1SpawnsSection.getConfigurationSection(key);
                    if (spawnSection != null) {
                        Location loc = deserializeLocation(spawnSection);
                        if (loc != null) {
                            mapManager.addSlot1Spawn(loc);
                        }
                    }
                }
                plugin.getLogger().info("1번 맵 스폰 " + mapManager.getSlot1SpawnCount() + "개 로드 완료");
            }
        }
        
        // Load slot 2 spawns
        if (config.contains("slot2Spawns")) {
            ConfigurationSection slot2SpawnsSection = config.getConfigurationSection("slot2Spawns");
            if (slot2SpawnsSection != null) {
                for (String key : slot2SpawnsSection.getKeys(false)) {
                    ConfigurationSection spawnSection = slot2SpawnsSection.getConfigurationSection(key);
                    if (spawnSection != null) {
                        Location loc = deserializeLocation(spawnSection);
                        if (loc != null) {
                            mapManager.addSlot2Spawn(loc);
                        }
                    }
                }
                plugin.getLogger().info("2번 맵 스폰 " + mapManager.getSlot2SpawnCount() + "개 로드 완료");
            }
        }
    }
    
    // ===== Music Settings =====
    
    /**
     * Get music URL
     */
    public String getMusicUrl() {
        if (config == null) return null;
        return config.getString("music.url");
    }
    
    /**
     * Set music URL
     */
    public void setMusicUrl(String url) {
        if (config == null) config = new YamlConfiguration();
        config.set("music.url", url);
        saveData();
    }
    
    /**
     * Get music sound name
     */
    public String getMusicSound() {
        if (config == null) return "MUSIC_DISC_PIGSTEP";
        return config.getString("music.sound", "MUSIC_DISC_PIGSTEP");
    }
    
    /**
     * Set music sound name
     */
    public void setMusicSound(String sound) {
        if (config == null) config = new YamlConfiguration();
        config.set("music.sound", sound);
        saveData();
    }
    
    /**
     * Check if using music URL
     */
    public boolean useMusicUrl() {
        if (config == null) return false;
        return config.getBoolean("music.useUrl", false);
    }
    
    /**
     * Set whether to use music URL
     */
    public void setUseMusicUrl(boolean use) {
        if (config == null) config = new YamlConfiguration();
        config.set("music.useUrl", use);
        saveData();
    }
    
    /**
     * Get music volume
     */
    public float getMusicVolume() {
        if (config == null) return 1.0f;
        return (float) config.getDouble("music.volume", 1.0);
    }
    
    /**
     * Set music volume
     */
    public void setMusicVolume(float volume) {
        if (config == null) config = new YamlConfiguration();
        config.set("music.volume", volume);
        saveData();
    }
    
    /**
     * Get music pitch
     */
    public float getMusicPitch() {
        if (config == null) return 1.0f;
        return (float) config.getDouble("music.pitch", 1.0);
    }
    
    /**
     * Set music pitch
     */
    public void setMusicPitch(float pitch) {
        if (config == null) config = new YamlConfiguration();
        config.set("music.pitch", pitch);
        saveData();
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
        
        // Save map slot 1
        if (mapManager.hasSlot1Map()) {
            ConfigurationSection slot1Section = config.createSection("mapSlot1");
            serializeLocation(slot1Section, mapManager.getSlot1Map());
            String name = mapManager.getSlot1MapName();
            if (name != null) {
                slot1Section.set("name", name);
            }
        }
        
        // Save map slot 2
        if (mapManager.hasSlot2Map()) {
            ConfigurationSection slot2Section = config.createSection("mapSlot2");
            serializeLocation(slot2Section, mapManager.getSlot2Map());
            String name = mapManager.getSlot2MapName();
            if (name != null) {
                slot2Section.set("name", name);
            }
        }
        
        // Save slot 1 spawns
        List<Location> slot1Spawns = mapManager.getSlot1Spawns();
        if (!slot1Spawns.isEmpty()) {
            ConfigurationSection slot1SpawnsSection = config.createSection("slot1Spawns");
            for (int i = 0; i < slot1Spawns.size(); i++) {
                Location loc = slot1Spawns.get(i);
                ConfigurationSection spawnSection = slot1SpawnsSection.createSection("spawn" + i);
                serializeLocation(spawnSection, loc);
            }
        }
        
        // Save slot 2 spawns
        List<Location> slot2Spawns = mapManager.getSlot2Spawns();
        if (!slot2Spawns.isEmpty()) {
            ConfigurationSection slot2SpawnsSection = config.createSection("slot2Spawns");
            for (int i = 0; i < slot2Spawns.size(); i++) {
                Location loc = slot2Spawns.get(i);
                ConfigurationSection spawnSection = slot2SpawnsSection.createSection("spawn" + i);
                serializeLocation(spawnSection, loc);
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

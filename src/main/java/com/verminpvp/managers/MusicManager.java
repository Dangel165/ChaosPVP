package com.verminpvp.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages game start music and sound effects
 * Supports custom music URLs (requires resource pack) or built-in sounds
 */
public class MusicManager {
    
    private final Plugin plugin;
    private final DataManager dataManager;
    
    // Music settings
    private String musicUrl = null; // YouTube or custom music URL
    private Sound builtInSound = null; // Built-in sound (null = disabled by default)
    private boolean useMusicUrl = false; // Use URL or built-in sound
    private boolean musicEnabled = false; // Music is disabled by default
    private float volume = 1.0f;
    private float pitch = 1.0f;
    
    // Track playing music for each player
    private final Map<Player, Boolean> playingMusic = new HashMap<>();
    
    public MusicManager(Plugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        loadSettings();
    }
    
    /**
     * Load music settings from config
     */
    private void loadSettings() {
        if (dataManager != null) {
            musicUrl = dataManager.getMusicUrl();
            String soundName = dataManager.getMusicSound();
            if (soundName != null && !soundName.isEmpty()) {
                try {
                    builtInSound = Sound.valueOf(soundName);
                    musicEnabled = true; // Enable music if sound is set
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound name: " + soundName);
                    builtInSound = null;
                    musicEnabled = false;
                }
            }
            useMusicUrl = dataManager.useMusicUrl();
            
            // Enable music if URL is set
            if (useMusicUrl && musicUrl != null && !musicUrl.isEmpty()) {
                musicEnabled = true;
            }
            
            volume = dataManager.getMusicVolume();
            pitch = dataManager.getMusicPitch();
        }
    }
    
    /**
     * Save music settings to config
     */
    private void saveSettings() {
        if (dataManager != null) {
            dataManager.setMusicUrl(musicUrl);
            dataManager.setMusicSound(builtInSound.name());
            dataManager.setUseMusicUrl(useMusicUrl);
            dataManager.setMusicVolume(volume);
            dataManager.setMusicPitch(pitch);
        }
    }
    
    /**
     * Set music URL (YouTube or custom)
     */
    public void setMusicUrl(String url) {
        this.musicUrl = url;
        this.musicEnabled = (url != null && !url.isEmpty());
        saveSettings();
    }
    
    /**
     * Get music URL
     */
    public String getMusicUrl() {
        return musicUrl;
    }
    
    /**
     * Set built-in sound
     */
    public void setBuiltInSound(Sound sound) {
        this.builtInSound = sound;
        this.musicEnabled = (sound != null);
        saveSettings();
    }
    
    /**
     * Get built-in sound
     */
    public Sound getBuiltInSound() {
        return builtInSound;
    }
    
    /**
     * Check if music is enabled
     */
    public boolean isMusicEnabled() {
        return musicEnabled && ((useMusicUrl && musicUrl != null && !musicUrl.isEmpty()) || 
                                (!useMusicUrl && builtInSound != null));
    }
    
    /**
     * Disable music
     */
    public void disableMusic() {
        this.musicEnabled = false;
        this.musicUrl = null;
        this.builtInSound = null;
        saveSettings();
    }
    
    /**
     * Set whether to use music URL or built-in sound
     */
    public void setUseMusicUrl(boolean use) {
        this.useMusicUrl = use;
        saveSettings();
    }
    
    /**
     * Check if using music URL
     */
    public boolean isUsingMusicUrl() {
        return useMusicUrl;
    }
    
    /**
     * Set volume (0.0 to 1.0)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        saveSettings();
    }
    
    /**
     * Get volume
     */
    public float getVolume() {
        return volume;
    }
    
    /**
     * Set pitch (0.5 to 2.0)
     */
    public void setPitch(float pitch) {
        this.pitch = Math.max(0.5f, Math.min(2.0f, pitch));
        saveSettings();
    }
    
    /**
     * Get pitch
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Play game start music for all players
     */
    public void playGameStartMusic() {
        // Only play if music is enabled
        if (!isMusicEnabled()) {
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                playMusicForPlayer(player);
            }
        }
    }
    
    /**
     * Play music for a specific player
     */
    public void playMusicForPlayer(Player player) {
        if (useMusicUrl && musicUrl != null && !musicUrl.isEmpty()) {
            // Send clickable YouTube link to player
            player.sendMessage("§e♪ §6게임 시작 음악 §e♪");
            
            // Create clickable link component
            net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent("§a[클릭하여 음악 듣기]");
            message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, 
                musicUrl
            ));
            message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("§e클릭하면 브라우저에서 음악이 재생됩니다!\n§7" + musicUrl).create()
            ));
            
            player.spigot().sendMessage(message);
            player.sendMessage("§7게임을 하면서 음악을 들으세요!");
            
            playingMusic.put(player, true);
        } else {
            // Play built-in sound
            player.playSound(player.getLocation(), builtInSound, SoundCategory.MUSIC, volume, pitch);
            playingMusic.put(player, true);
        }
    }
    
    /**
     * Stop music for all players
     */
    public void stopAllMusic() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopMusicForPlayer(player);
        }
        playingMusic.clear();
    }
    
    /**
     * Stop music for a specific player
     */
    public void stopMusicForPlayer(Player player) {
        if (playingMusic.containsKey(player)) {
            // Stop built-in sound
            player.stopSound(builtInSound, SoundCategory.MUSIC);
            playingMusic.remove(player);
        }
    }
    
    /**
     * Check if player is playing music
     */
    public boolean isPlayingMusic(Player player) {
        return playingMusic.getOrDefault(player, false);
    }
    
    /**
     * Get music info message
     */
    public String getMusicInfo() {
        if (!isMusicEnabled()) {
            return "§e음악 상태: §c비활성화\n§7음악을 활성화하려면 URL 또는 사운드를 설정하세요.";
        }
        
        if (useMusicUrl && musicUrl != null && !musicUrl.isEmpty()) {
            return "§e음악 상태: §a활성화\n§e음악 모드: §fURL (클릭 가능한 링크)\n§e음악 URL: §f" + musicUrl;
        } else if (builtInSound != null) {
            return "§e음악 상태: §a활성화\n§e음악 모드: §f내장 사운드\n§e사운드: §f" + builtInSound.name();
        } else {
            return "§e음악 상태: §c비활성화\n§7음악을 활성화하려면 URL 또는 사운드를 설정하세요.";
        }
    }
}

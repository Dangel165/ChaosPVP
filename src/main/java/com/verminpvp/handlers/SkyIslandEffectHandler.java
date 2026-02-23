package com.verminpvp.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles special effects for the Sky Island map
 * - Fixed sunset/dusk time (13000 ticks)
 * - Weather control (no rain/thunder)
 */
public class SkyIslandEffectHandler {
    
    private final Plugin plugin;
    private BukkitTask timeFixTask;
    private World skyIslandWorld;
    private Location skyIslandCenter;
    
    private static final long SUNSET_TIME = 13000L; // Sunset time in ticks (13000 = sunset/dusk)
    
    public SkyIslandEffectHandler(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start sky island effects
     */
    public void startEffects(Location center) {
        if (center == null || center.getWorld() == null) {
            plugin.getLogger().warning("[ChaosPVP] Cannot start sky island effects - center or world is null");
            return;
        }
        
        plugin.getLogger().info("[ChaosPVP] Starting sky island effects at " + center);
        
        // Stop existing tasks if any (but don't clear world/center yet)
        if (timeFixTask != null) {
            timeFixTask.cancel();
            timeFixTask = null;
        }
        
        // NOW set the world and center (after canceling old tasks)
        this.skyIslandCenter = center.clone();
        this.skyIslandWorld = center.getWorld();
        
        // Start time fix task (keep time at sunset)
        timeFixTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (skyIslandWorld == null) {
                plugin.getLogger().warning("[ChaosPVP] Sky island world is null in time fix task!");
                return;
            }
            
            // Set time to sunset
            long currentTime = skyIslandWorld.getTime();
            if (currentTime != SUNSET_TIME) {
                skyIslandWorld.setTime(SUNSET_TIME);
                plugin.getLogger().info("[ChaosPVP] Fixed time from " + currentTime + " to " + SUNSET_TIME);
            }
            
            // Disable weather
            if (skyIslandWorld.hasStorm()) {
                skyIslandWorld.setStorm(false);
                plugin.getLogger().info("[ChaosPVP] Disabled storm");
            }
            if (skyIslandWorld.isThundering()) {
                skyIslandWorld.setThundering(false);
                plugin.getLogger().info("[ChaosPVP] Disabled thunder");
            }
        }, 0L, 20L); // Check every second
        
        plugin.getLogger().info("[ChaosPVP] Sky Island effects started - time task: " + (timeFixTask != null));
    }
    
    /**
     * Stop all sky island effects
     */
    public void stopEffects() {
        if (timeFixTask != null) {
            timeFixTask.cancel();
            timeFixTask = null;
        }
        
        // Don't reset world time here - let the world naturally reset
        // Resetting time here causes issues when restarting effects
        
        skyIslandWorld = null;
        skyIslandCenter = null;
        
        plugin.getLogger().info("[ChaosPVP] Sky Island effects stopped");
    }
    
    /**
     * Check if effects are currently active
     */
    public boolean isActive() {
        return timeFixTask != null;
    }
}

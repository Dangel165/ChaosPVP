package com.verminpvp.ui;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Displays cooldown information to players using action bar
 */
public class CooldownDisplay {
    
    private final VerminPVP plugin;
    private final CooldownManager cooldownManager;
    private final Map<UUID, BukkitRunnable> activeDisplays;
    
    public CooldownDisplay(VerminPVP plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.activeDisplays = new HashMap<>();
    }
    
    /**
     * Show cooldown bar for a specific ability
     */
    public void showCooldown(Player player, String abilityId, String abilityName, double cooldownSeconds) {
        // Cancel existing display for this player
        cancelDisplay(player);
        
        // Create new display task
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), abilityId);
                
                if (remaining <= 0) {
                    // Cooldown finished
                    player.sendActionBar(Component.text("✓ " + abilityName + " 준비 완료!", NamedTextColor.GREEN));
                    cancel();
                    activeDisplays.remove(player.getUniqueId());
                    return;
                }
                
                // Calculate progress
                double progress = 1.0 - (remaining / cooldownSeconds);
                int barLength = 20;
                int filledBars = (int) (progress * barLength);
                
                // Create progress bar
                StringBuilder bar = new StringBuilder();
                bar.append("§e").append(abilityName).append(" §7[");
                
                for (int i = 0; i < barLength; i++) {
                    if (i < filledBars) {
                        bar.append("§a█");
                    } else {
                        bar.append("§7█");
                    }
                }
                
                bar.append("§7] §c").append(String.format("%.1f", remaining)).append("초");
                
                player.sendActionBar(Component.text(bar.toString()));
            }
        };
        
        task.runTaskTimer(plugin, 0L, 2L); // Update every 2 ticks (0.1 seconds)
        activeDisplays.put(player.getUniqueId(), task);
    }
    
    /**
     * Show multiple cooldowns at once
     */
    public void showMultipleCooldowns(Player player, Map<String, CooldownInfo> cooldowns) {
        // Cancel existing display
        cancelDisplay(player);
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                StringBuilder display = new StringBuilder();
                boolean allReady = true;
                
                for (Map.Entry<String, CooldownInfo> entry : cooldowns.entrySet()) {
                    String abilityId = entry.getKey();
                    CooldownInfo info = entry.getValue();
                    
                    double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), abilityId);
                    
                    if (remaining > 0) {
                        allReady = false;
                        display.append("§e").append(info.shortName).append(" §c")
                               .append(String.format("%.1f", remaining)).append("s §7| ");
                    } else {
                        display.append("§a").append(info.shortName).append(" ✓ §7| ");
                    }
                }
                
                if (allReady) {
                    player.sendActionBar(Component.text("§a모든 스킬 준비 완료!", NamedTextColor.GREEN));
                    cancel();
                    activeDisplays.remove(player.getUniqueId());
                    return;
                }
                
                // Remove trailing separator
                if (display.length() > 3) {
                    display.setLength(display.length() - 3);
                }
                
                player.sendActionBar(Component.text(display.toString()));
            }
        };
        
        task.runTaskTimer(plugin, 0L, 4L); // Update every 4 ticks (0.2 seconds)
        activeDisplays.put(player.getUniqueId(), task);
    }
    
    /**
     * Cancel cooldown display for a player
     */
    public void cancelDisplay(Player player) {
        BukkitRunnable task = activeDisplays.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Cancel all displays
     */
    public void cancelAllDisplays() {
        for (BukkitRunnable task : activeDisplays.values()) {
            task.cancel();
        }
        activeDisplays.clear();
    }
    
    /**
     * Information about a cooldown for display
     */
    public static class CooldownInfo {
        public final String shortName;
        public final double maxCooldown;
        
        public CooldownInfo(String shortName, double maxCooldown) {
            this.shortName = shortName;
            this.maxCooldown = maxCooldown;
        }
    }
}

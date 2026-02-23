package com.verminpvp.handlers;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies potion effects and manages absorption hearts with stacking prevention
 */
public class EffectApplicator {
    
    private final Plugin plugin;
    private final NamespacedKey absorptionSourceKey;
    
    // Track absorption sources per player to prevent stacking
    private final Map<UUID, Map<String, Long>> absorptionSources;
    
    public EffectApplicator(Plugin plugin) {
        this.plugin = plugin;
        this.absorptionSourceKey = new NamespacedKey(plugin, "absorption_source");
        this.absorptionSources = new HashMap<>();
    }
    
    /**
     * Applies a standard potion effect to an entity
     */
    public void applyEffect(LivingEntity entity, PotionEffectType type, int durationTicks, int amplifier) {
        if (entity == null || type == null || entity.isDead()) {
            return;
        }
        
        if (durationTicks <= 0) {
            return;
        }
        
        // Create and apply the potion effect
        PotionEffect effect = new PotionEffect(type, durationTicks, amplifier, false, true, true);
        entity.addPotionEffect(effect);
    }
    
    /**
     * Applies absorption hearts with source tracking to prevent stacking
     * Returns true if absorption was applied, false if prevented by stacking rules
     */
    public boolean applyAbsorption(Player player, int hearts, int durationTicks, String source) {
        if (player == null || player.isDead()) {
            return false;
        }
        
        if (hearts <= 0 || durationTicks <= 0 || source == null) {
            return false;
        }
        
        // Check if player already has absorption from this source
        if (hasAbsorptionFrom(player, source)) {
            return false; // Prevent stacking
        }
        
        // Calculate absorption amount (1 heart = 2 absorption points)
        double absorptionAmount = hearts * 2.0;
        
        // Cap at maximum absorption (20 hearts = 40 points)
        absorptionAmount = Math.min(absorptionAmount, 40.0);
        
        // Apply absorption effect
        PotionEffect absorption = new PotionEffect(
            PotionEffectType.ABSORPTION,
            durationTicks,
            (int) Math.ceil(absorptionAmount / 4.0) - 1, // Amplifier calculation
            false,
            true,
            true
        );
        player.addPotionEffect(absorption);
        
        // Track the absorption source
        UUID playerId = player.getUniqueId();
        absorptionSources.putIfAbsent(playerId, new HashMap<>());
        absorptionSources.get(playerId).put(source, System.currentTimeMillis() + (durationTicks * 50L));
        
        return true;
    }
    
    /**
     * Checks if a player has absorption from a specific source
     */
    public boolean hasAbsorptionFrom(Player player, String source) {
        if (player == null || source == null) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        Map<String, Long> sources = absorptionSources.get(playerId);
        
        if (sources == null || !sources.containsKey(source)) {
            return false;
        }
        
        // Check if the absorption has expired
        long expirationTime = sources.get(source);
        if (System.currentTimeMillis() >= expirationTime) {
            sources.remove(source);
            return false;
        }
        
        return true;
    }
    
    /**
     * Removes a specific potion effect from an entity
     */
    public void removeEffect(LivingEntity entity, PotionEffectType type) {
        if (entity == null || type == null || entity.isDead()) {
            return;
        }
        
        entity.removePotionEffect(type);
    }
    
    /**
     * Removes all potion effects from an entity
     */
    public void removeAllEffects(LivingEntity entity) {
        if (entity == null || entity.isDead()) {
            return;
        }
        
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            entity.removePotionEffect(effect.getType());
        }
    }
    
    /**
     * Clears absorption tracking for a player
     */
    public void clearAbsorptionTracking(Player player) {
        if (player == null) {
            return;
        }
        
        absorptionSources.remove(player.getUniqueId());
    }
    
    /**
     * Removes a specific absorption source from tracking
     */
    public void removeAbsorptionSource(Player player, String source) {
        if (player == null || source == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        Map<String, Long> sources = absorptionSources.get(playerId);
        
        if (sources != null) {
            sources.remove(source);
        }
    }
    
    /**
     * Checks if an entity has a specific potion effect
     */
    public boolean hasEffect(LivingEntity entity, PotionEffectType type) {
        if (entity == null || type == null || entity.isDead()) {
            return false;
        }
        
        return entity.hasPotionEffect(type);
    }
    
    /**
     * Gets the amplifier of a potion effect (returns -1 if not present)
     */
    public int getEffectAmplifier(LivingEntity entity, PotionEffectType type) {
        if (entity == null || type == null || entity.isDead()) {
            return -1;
        }
        
        PotionEffect effect = entity.getPotionEffect(type);
        return effect != null ? effect.getAmplifier() : -1;
    }
    
    /**
     * Gets the remaining duration of a potion effect in ticks (returns 0 if not present)
     */
    public int getEffectDuration(LivingEntity entity, PotionEffectType type) {
        if (entity == null || type == null || entity.isDead()) {
            return 0;
        }
        
        PotionEffect effect = entity.getPotionEffect(type);
        return effect != null ? effect.getDuration() : 0;
    }
    
    /**
     * Applies multiple effects in order
     * Ensures consistent application order for Property 23
     */
    public void applyEffects(LivingEntity entity, EffectData... effects) {
        if (entity == null || effects == null || entity.isDead()) {
            return;
        }
        
        for (EffectData effectData : effects) {
            if (effectData != null) {
                applyEffect(entity, effectData.type, effectData.durationTicks, effectData.amplifier);
            }
        }
    }
    
    /**
     * Helper class for batch effect application
     */
    public static class EffectData {
        public final PotionEffectType type;
        public final int durationTicks;
        public final int amplifier;
        
        public EffectData(PotionEffectType type, int durationTicks, int amplifier) {
            this.type = type;
            this.durationTicks = durationTicks;
            this.amplifier = amplifier;
        }
    }
    
    /**
     * Heals an entity by a specific amount
     */
    public void heal(LivingEntity entity, double amount) {
        if (entity == null || entity.isDead()) {
            return;
        }
        
        if (amount <= 0) {
            return;
        }
        
        double currentHealth = entity.getHealth();
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(maxHealth, currentHealth + amount);
        
        entity.setHealth(newHealth);
    }
    
    /**
     * Cleans up expired absorption sources
     */
    public void cleanupExpiredAbsorption() {
        long currentTime = System.currentTimeMillis();
        
        for (Map<String, Long> sources : absorptionSources.values()) {
            sources.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
        }
        
        // Remove empty player entries
        absorptionSources.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    /**
     * Removes all tracking data for a player
     */
    public void removePlayer(Player player) {
        if (player == null) {
            return;
        }
        
        clearAbsorptionTracking(player);
    }
}

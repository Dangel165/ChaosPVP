package com.verminpvp.handlers;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Handles damage calculations and application with class-specific modifiers
 */
public class DamageHandler {
    
    /**
     * Applies melee damage that respects armor and enchantments
     * This is the standard Minecraft damage calculation
     */
    public void applyMeleeDamage(Player attacker, LivingEntity target, double baseDamage) {
        if (attacker == null || target == null || target.isDead()) {
            return;
        }
        
        if (baseDamage <= 0) {
            return;
        }
        
        // Apply damage - Minecraft will handle armor reduction automatically
        target.damage(baseDamage, attacker);
    }
    
    /**
     * Applies instant damage that bypasses armor and enchantments
     * Used for abilities like Swordsman ultimate, Navigator Naval Combat
     */
    public void applyInstantDamage(LivingEntity target, double damage) {
        if (target == null || target.isDead()) {
            return;
        }
        
        if (damage <= 0) {
            return;
        }
        
        // Get current health and apply damage directly
        double currentHealth = target.getHealth();
        double newHealth = Math.max(0, currentHealth - damage);
        target.setHealth(newHealth);
    }
    
    /**
     * Applies damage with custom knockback
     * Used for abilities that need specific knockback values
     */
    public void applyDamageWithKnockback(Player attacker, LivingEntity target, double baseDamage, double knockbackStrength) {
        if (attacker == null || target == null || target.isDead()) {
            return;
        }
        
        if (baseDamage <= 0) {
            return;
        }
        
        // Apply damage first
        target.damage(baseDamage, attacker);
        
        // Apply custom knockback
        if (knockbackStrength > 0) {
            Vector direction = target.getLocation().toVector()
                .subtract(attacker.getLocation().toVector())
                .normalize()
                .multiply(knockbackStrength)
                .setY(0.3); // Add upward component
            
            target.setVelocity(direction);
        }
    }
    
    /**
     * Applies damage without any knockback
     * Used for Plague Spreader poison fields
     */
    public void applyDamageWithoutKnockback(LivingEntity target, double damage) {
        if (target == null || target.isDead()) {
            return;
        }
        
        if (damage <= 0) {
            return;
        }
        
        // Store current velocity
        Vector currentVelocity = target.getVelocity().clone();
        
        // Apply instant damage (bypasses armor, no knockback)
        applyInstantDamage(target, damage);
        
        // Restore velocity to prevent any knockback
        target.setVelocity(currentVelocity);
    }
    
    /**
     * Calculates the final damage after armor reduction
     * Used for testing and validation
     */
    public double calculateArmorReducedDamage(LivingEntity target, double baseDamage) {
        if (target == null) {
            return baseDamage;
        }
        
        // This is an approximation of Minecraft's armor calculation
        // Actual calculation is more complex and handled by the server
        double armor = target.getAttribute(Attribute.ARMOR).getValue();
        double toughness = target.getAttribute(Attribute.ARMOR_TOUGHNESS).getValue();
        
        // Simplified armor calculation
        double damageReduction = Math.min(20, Math.max(armor / 5, armor - baseDamage / (2 + toughness / 4)));
        double multiplier = 1 - (damageReduction / 25);
        
        return baseDamage * multiplier;
    }
    
    /**
     * Applies damage with a specific attacker for proper damage attribution
     */
    public void applyDamageFrom(Player attacker, LivingEntity target, double damage, boolean bypassArmor) {
        if (attacker == null || target == null || target.isDead()) {
            return;
        }
        
        if (damage <= 0) {
            return;
        }
        
        if (bypassArmor) {
            applyInstantDamage(target, damage);
        } else {
            target.damage(damage, attacker);
        }
    }
    
    /**
     * Checks if an entity can take damage
     */
    public boolean canTakeDamage(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        
        return !entity.isDead() && entity.isValid() && entity.getHealth() > 0;
    }
    
    /**
     * Gets the remaining health of an entity
     */
    public double getRemainingHealth(LivingEntity entity) {
        if (entity == null || entity.isDead()) {
            return 0;
        }
        
        return entity.getHealth();
    }
    
    /**
     * Gets the maximum health of an entity
     */
    public double getMaxHealth(LivingEntity entity) {
        if (entity == null) {
            return 0;
        }
        
        return entity.getAttribute(Attribute.MAX_HEALTH).getValue();
    }
}

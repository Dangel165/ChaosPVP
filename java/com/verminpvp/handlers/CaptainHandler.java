package com.verminpvp.handlers;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.AbilityIds;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Captain class abilities
 * 
 * Abilities:
 * - Passive team buff on hit (random teammate gets Strength I 2s in team mode, self in solo)
 * - Captain's Command (give Naval Combat + Strength I 5s, 15s cooldown)
 * - Captain's Harpoon Throw (2 damage, buffs/debuffs, 12s cooldown)
 */
public class CaptainHandler implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    private final CooldownManager cooldownManager;
    private final ItemProvider itemProvider;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    private final EffectApplicator effectApplicator;
    private final DamageHandler damageHandler;
    
    // Track Captain's Harpoon projectiles
    private final Map<UUID, UUID> captainHarpoons = new HashMap<>(); // Arrow UUID -> Player UUID
    
    public CaptainHandler(VerminPVP plugin, ClassManager classManager,
                           CooldownManager cooldownManager, ItemProvider itemProvider,
                           TeamManager teamManager, GameManager gameManager,
                           EffectApplicator effectApplicator, DamageHandler damageHandler) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.cooldownManager = cooldownManager;
        this.itemProvider = itemProvider;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.effectApplicator = effectApplicator;
        this.damageHandler = damageHandler;
    }
    
    /**
     * Passive team buff on hit - buffs teammate in team mode, self in solo mode
     */
    @EventHandler
    public void onCaptainHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player captain = (Player) event.getDamager();
        if (classManager.getPlayerClass(captain) != ClassType.CAPTAIN) return;
        
        Player target;
        
        // Check game mode
        if (gameManager.getGameMode() == GameMode.TEAM) {
            // Team mode: buff random teammate
            target = teamManager.getRandomTeammate(captain);
            if (target == null) {
                // No teammates, buff self
                target = captain;
            }
        } else {
            // Solo mode: buff self
            target = captain;
        }
        
        // Apply Strength I for 2 seconds
        effectApplicator.applyEffect(target, PotionEffectType.STRENGTH, 40, 0);
        
        if (!target.equals(captain)) {
            target.sendMessage("§6선장이 당신을 강화했습니다!");
        }
    }
    
    /**
     * Handle Captain's Command and Captain's Harpoon
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (classManager.getPlayerClass(player) != ClassType.CAPTAIN) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        String itemId = itemProvider.getItemId(item);
        if (itemId == null) return;
        
        if (itemId.equals("captains_command")) {
            event.setCancelled(true);
            handleCaptainsCommand(player);
        } else if (itemId.equals("captains_harpoon")) {
            event.setCancelled(true);
            handleCaptainsHarpoon(player);
        }
    }
    
    /**
     * Handle Captain's Command ability - gives Naval Combat to teammate in team mode, self in solo
     */
    private void handleCaptainsCommand(Player captain) {
        if (cooldownManager.isOnCooldown(captain.getUniqueId(), AbilityIds.CAPTAINS_COMMAND)) {
            long remaining = (long) cooldownManager.getRemainingCooldown(captain.getUniqueId(), AbilityIds.CAPTAINS_COMMAND);
            captain.sendMessage("§c선장의 명령 쿨다운: " + remaining + "초");
            return;
        }
        
        Player target;
        
        // Check game mode
        if (gameManager.getGameMode() == GameMode.TEAM) {
            // Team mode: give to teammate with less than 3 Naval Combat items
            List<Player> validTeammates = new ArrayList<>();
            for (Player teammate : teamManager.getTeammates(captain)) {
                int navalCombatCount = countItemsInInventory(teammate, "naval_combat");
                if (navalCombatCount < 3) {
                    validTeammates.add(teammate);
                }
            }
            
            if (validTeammates.isEmpty()) {
                // No valid teammates, give to self
                target = captain;
                captain.sendMessage("§6사용 가능한 팀원이 없어 자신을 강화합니다!");
            } else {
                // Select random valid teammate
                target = validTeammates.get((int) (Math.random() * validTeammates.size()));
            }
        } else {
            // Solo mode: give to self
            target = captain;
        }
        
        // Give Naval Combat item
        ItemStack navalCombat = itemProvider.createSpecialItem(ClassType.NAVIGATOR, "naval_combat");
        target.getInventory().addItem(navalCombat);
        
        // Apply Strength I for 5 seconds
        effectApplicator.applyEffect(target, PotionEffectType.STRENGTH, 100, 0);
        
        // Set cooldown
        cooldownManager.setCooldown(captain.getUniqueId(), AbilityIds.CAPTAINS_COMMAND, 15);
        
        // Show cooldown display
        VerminPVP.getInstance().getCooldownDisplay().showCooldown(captain, AbilityIds.CAPTAINS_COMMAND, "선장의 명령", 15.0);
        
        captain.sendMessage("§6선장의 명령 사용!");
        if (!target.equals(captain)) {
            target.sendMessage("§6선장이 해전 무기와 힘을 부여했습니다!");
        }
    }
    
    /**
     * Handle Captain's Harpoon Throw ability
     */
    private void handleCaptainsHarpoon(Player captain) {
        if (cooldownManager.isOnCooldown(captain.getUniqueId(), AbilityIds.CAPTAINS_HARPOON)) {
            long remaining = (long) cooldownManager.getRemainingCooldown(captain.getUniqueId(), AbilityIds.CAPTAINS_HARPOON);
            captain.sendMessage("§c선장의 작살 쿨다운: " + remaining + "초");
            return;
        }
        
        // Shoot arrow
        Arrow arrow = captain.launchProjectile(Arrow.class);
        arrow.setVelocity(captain.getLocation().getDirection().multiply(2.5));
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        
        // Track arrow
        captainHarpoons.put(arrow.getUniqueId(), captain.getUniqueId());
        
        // Set cooldown (changed from 12s to 9s)
        cooldownManager.setCooldown(captain.getUniqueId(), AbilityIds.CAPTAINS_HARPOON, 9);
        
        // Show cooldown display
        VerminPVP.getInstance().getCooldownDisplay().showCooldown(captain, AbilityIds.CAPTAINS_HARPOON, "선장의 작살", 9.0);
        
        captain.sendMessage("§6선장의 작살 발사!");
    }
    
    /**
     * Handle Captain's Harpoon hit - buffs teammate in team mode, self in solo mode
     */
    @EventHandler
    public void onCaptainHarpoonHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getHitEntity() instanceof LivingEntity)) return;
        
        Arrow arrow = (Arrow) event.getEntity();
        UUID captainId = captainHarpoons.get(arrow.getUniqueId());
        if (captainId == null) return;
        
        Player captain = Bukkit.getPlayer(captainId);
        if (captain == null) return;
        
        LivingEntity target = (LivingEntity) event.getHitEntity();
        
        // Cancel the arrow's default damage
        event.setCancelled(true);
        
        // Apply 4 damage (buffed from 2) - skill damage only, no arrow damage
        damageHandler.applyInstantDamage(target, 4.0);
        
        Player buffTarget;
        
        // Check game mode
        if (gameManager.getGameMode() == GameMode.TEAM) {
            // Team mode: buff random teammate
            buffTarget = teamManager.getRandomTeammate(captain);
            if (buffTarget == null) {
                // No teammates, buff self
                buffTarget = captain;
            }
        } else {
            // Solo mode: buff self
            buffTarget = captain;
        }
        
        // Buff with Strength I for 3 seconds
        effectApplicator.applyEffect(buffTarget, PotionEffectType.STRENGTH, 60, 0);
        
        if (!buffTarget.equals(captain)) {
            buffTarget.sendMessage("§6선장의 작살이 당신을 강화했습니다!");
        }
        
        // Debuff target with Weakness I for 3 seconds
        if (target instanceof Player) {
            effectApplicator.applyEffect((Player) target, PotionEffectType.WEAKNESS, 60, 0);
        }
        
        // Reduce cooldown by 3 seconds (changed from 4s), but ensure minimum 1 second remains
        double currentCooldown = cooldownManager.getRemainingCooldown(captain.getUniqueId(), AbilityIds.CAPTAINS_HARPOON);
        double newCooldown = Math.max(1.0, currentCooldown - 3.0);
        
        // Set the new cooldown
        cooldownManager.setCooldown(captain.getUniqueId(), AbilityIds.CAPTAINS_HARPOON, (int) Math.ceil(newCooldown));
        
        // Update cooldown display
        VerminPVP.getInstance().getCooldownDisplay().showCooldown(captain, AbilityIds.CAPTAINS_HARPOON, "선장의 작살", newCooldown);
        
        // Remove from tracking
        captainHarpoons.remove(arrow.getUniqueId());
        
        // Remove arrow
        arrow.remove();
        
        captain.sendMessage(String.format("§6선장의 작살 적중! 쿨다운: %.1f초", newCooldown));
    }
    
    /**
     * Count specific items in player inventory
     */
    private int countItemsInInventory(Player player, String itemId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && itemId.equals(itemProvider.getItemId(item))) {
                count += item.getAmount();
            }
        }
        return count;
    }
}

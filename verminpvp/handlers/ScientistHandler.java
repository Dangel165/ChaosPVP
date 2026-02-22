package com.verminpvp.handlers;

import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.managers.ResourceTracker;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.GameMode;
import com.verminpvp.models.ResourceType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Handles Scientist class abilities and mechanics
 */
public class ScientistHandler implements Listener {
    
    private final Plugin plugin;
    private final ClassManager classManager;
    private final ResourceTracker resourceTracker;
    private final ItemProvider itemProvider;
    private final EffectApplicator effectApplicator;
    private final DamageHandler damageHandler;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    
    private static final int MAX_POTIONS = 3;
    private static final int POTION_GENERATION_INTERVAL = 150; // 7.5 seconds in ticks
    private static final int ULTIMATE_INGREDIENT_THRESHOLD = 9;
    
    // Potion generation tasks per player
    private final Map<UUID, BukkitTask> potionGenerationTasks;
    
    // Potion types for random generation (6 types)
    private final String[] potionTypes = {"instant_damage", "instant_healing", "slowness", "blindness", "resistance", "poison"};
    private final Random random;
    
    public ScientistHandler(Plugin plugin, ClassManager classManager, ResourceTracker resourceTracker,
                           ItemProvider itemProvider, EffectApplicator effectApplicator, DamageHandler damageHandler,
                           TeamManager teamManager, GameManager gameManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.resourceTracker = resourceTracker;
        this.itemProvider = itemProvider;
        this.effectApplicator = effectApplicator;
        this.damageHandler = damageHandler;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.potionGenerationTasks = new HashMap<>();
        this.random = new Random();
    }
    
    /**
     * Starts potion generation for a Scientist player
     */
    public void startPotionGeneration(Player player) {
        if (classManager.getPlayerClass(player) != ClassType.SCIENTIST) {
            return;
        }
        
        // Cancel existing task if any
        stopPotionGeneration(player);
        
        // Start new generation task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                generatePotion(player);
            }
        }.runTaskTimer(plugin, POTION_GENERATION_INTERVAL, POTION_GENERATION_INTERVAL);
        
        potionGenerationTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Stops potion generation for a player
     */
    public void stopPotionGeneration(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = potionGenerationTasks.remove(playerId);
        
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Generates a random potion for the Scientist
     */
    private void generatePotion(Player player) {
        if (!player.isOnline() || classManager.getPlayerClass(player) != ClassType.SCIENTIST) {
            stopPotionGeneration(player);
            return;
        }
        
        // Count existing potions
        int potionCount = countScientistPotions(player);
        
        if (potionCount >= MAX_POTIONS) {
            return; // Max limit reached
        }
        
        // Generate random potion type
        String potionType = potionTypes[random.nextInt(potionTypes.length)];
        
        // Create potion item
        ItemStack potion;
        if (potionType.equals("instant_healing")) {
            // For healing, give Minecraft Regeneration II potion instead of custom potion
            potion = itemProvider.createMinecraftRegenerationPotion();
        } else {
            potion = itemProvider.createSpecialItem(ClassType.SCIENTIST, potionType);
        }
        
        // Add to inventory
        player.getInventory().addItem(potion);
        
        String koreanType = "";
        switch(potionType) {
            case "instant_damage": koreanType = "즉시피해"; break;
            case "instant_healing": koreanType = "즉시회복 II"; break;
            case "slowness": koreanType = "감속"; break;
            case "blindness": koreanType = "실명"; break;
            case "resistance": koreanType = "저항"; break;
            case "poison": koreanType = "독"; break;
            default: koreanType = potionType;
        }
        player.sendMessage("§a물약 생성: " + koreanType);
    }
    
    /**
     * Counts Scientist potions in player inventory
     */
    private int countScientistPotions(Player player) {
        int count = 0;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (item.getType() == Material.SPLASH_POTION || item.getType() == Material.POTION)) {
                if (itemProvider.isClassItem(item)) {
                    String itemId = itemProvider.getItemId(item);
                    if (itemId != null && (itemId.startsWith("scientist_potion_") || itemId.equals("minecraft_regeneration_potion"))) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    /**
     * Passive: Speed I when throwing items
     */
    @EventHandler
    public void onScientistThrow(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is Scientist
        if (classManager.getPlayerClass(player) != ClassType.SCIENTIST) {
            return;
        }
        
        // Check if right-click with throwable item
        if (!event.getAction().name().contains("RIGHT_CLICK")) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        
        // Check if item is throwable (splash potion, snowball, egg, etc.)
        Material type = item.getType();
        if (type == Material.SPLASH_POTION || type == Material.LINGERING_POTION ||
            type == Material.SNOWBALL || type == Material.EGG || type == Material.ENDER_PEARL) {
            
            // Apply Speed I for 0.5 seconds (10 ticks)
            effectApplicator.applyEffect(player, PotionEffectType.SPEED, 10, 0);
        }
    }
    
    /**
     * Handles potion splash effects and ingredient collection
     */
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        
        if (!(potion.getShooter() instanceof Player)) {
            return;
        }
        
        Player thrower = (Player) potion.getShooter();
        
        // Check if thrower is Scientist
        if (classManager.getPlayerClass(thrower) != ClassType.SCIENTIST) {
            return;
        }
        
        // Check if it's a Scientist potion
        ItemStack potionItem = potion.getItem();
        if (!itemProvider.isClassItem(potionItem)) {
            return;
        }
        
        String itemId = itemProvider.getItemId(potionItem);
        if (itemId == null || !itemId.startsWith("scientist_potion_")) {
            return;
        }
        
        // Extract potion type
        String potionType = itemId.replace("scientist_potion_", "");
        
        // Apply effects to hit entities
        for (LivingEntity entity : event.getAffectedEntities()) {
            applyPotionEffect(entity, potionType, thrower);
        }
        
        // Grant ultimate ingredient if hit any entity
        if (!event.getAffectedEntities().isEmpty()) {
            grantUltimateIngredient(thrower);
        }
    }
    
    /**
     * Handles drinkable potion consumption (beneficial potions)
     */
    @EventHandler
    public void onPotionDrink(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is Scientist
        if (classManager.getPlayerClass(player) != ClassType.SCIENTIST) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.POTION) {
            return;
        }
        
        // Check if it's a Scientist potion
        if (!itemProvider.isClassItem(item)) {
            return;
        }
        
        String itemId = itemProvider.getItemId(item);
        if (itemId == null) {
            return;
        }
        
        // Handle Minecraft Instant Health II potion
        if (itemId.equals("minecraft_regeneration_potion")) {
            // Grant ultimate ingredient (Minecraft potion applies its own effect)
            grantUltimateIngredient(player);
            player.sendMessage("§a물약 효과 적용: 즉시회복 II");
            
            // Remove glass bottle after drinking (schedule for next tick to avoid conflicts)
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Remove one glass bottle from inventory
                player.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE, 1));
            }, 1L);
            return;
        }
        
        // Handle custom scientist potions
        if (!itemId.startsWith("scientist_potion_")) {
            return;
        }
        
        // Extract potion type
        String potionType = itemId.replace("scientist_potion_", "");
        
        // Apply effect to self
        applyPotionEffect(player, potionType, player);
        
        // Grant ultimate ingredient
        grantUltimateIngredient(player);
        
        player.sendMessage("§a물약 효과 적용: " + potionType);
        
        // Remove glass bottle after drinking (schedule for next tick to avoid conflicts)
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Remove one glass bottle from inventory
            player.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE, 1));
        }, 1L);
    }
    
    /**
     * Applies potion effect based on type
     */
    private void applyPotionEffect(LivingEntity entity, String potionType, Player thrower) {
        // Check if entity is a player
        if (entity instanceof Player) {
            Player target = (Player) entity;
            
            // Skip self check - allow self-application
            if (target.equals(thrower)) {
                // Apply effect to self normally
                applyEffectByType(entity, potionType);
                return;
            }
            
            // In team mode (not practice mode), check if target is on same team as thrower
            if (gameManager.getGameMode() == GameMode.TEAM && !gameManager.isInPracticeMode(thrower)) {
                if (teamManager.getPlayerTeam(thrower) == teamManager.getPlayerTeam(target)) {
                    // Same team - only apply beneficial effects, skip harmful effects
                    switch (potionType) {
                        case "instant_healing":
                            effectApplicator.heal(target, 8.0); // Fixed: 8 HP = 4 hearts
                            return;
                        case "resistance":
                            effectApplicator.applyEffect(target, PotionEffectType.RESISTANCE, 60, 3); // Buffed: 3 seconds
                            return;
                        // Skip harmful effects for teammates
                        case "instant_damage":
                        case "slowness":
                        case "blindness":
                        case "poison":
                            return; // Don't apply harmful effects to teammates
                        default:
                            return;
                    }
                }
            }
        }
        
        // Apply effects normally (not same team or solo mode or practice mode with mobs)
        applyEffectByType(entity, potionType);
    }
    
    /**
     * Apply effect by potion type
     */
    private void applyEffectByType(LivingEntity entity, String potionType) {
        switch (potionType) {
            case "instant_damage":
                damageHandler.applyInstantDamage(entity, 8.0); // Buffed from 4 to 8
                break;
            case "instant_healing":
                effectApplicator.heal(entity, 8.0); // Fixed: 8 HP = 4 hearts
                break;
            case "slowness":
                effectApplicator.applyEffect(entity, PotionEffectType.SLOWNESS, 40, 4); // Buffed: 2 seconds (was 1 second)
                break;
            case "blindness":
                effectApplicator.applyEffect(entity, PotionEffectType.BLINDNESS, 60, 0); // Buffed: 3 seconds (was 2.5 seconds)
                break;
            case "resistance":
                effectApplicator.applyEffect(entity, PotionEffectType.RESISTANCE, 60, 3); // Buffed: 3 seconds (was 1.5 seconds)
                break;
            case "poison":
                effectApplicator.applyEffect(entity, PotionEffectType.POISON, 110, 1); // Buffed: 5.5 seconds (was 3 seconds)
                break;
        }
    }
    
    /**
     * Grants an ultimate ingredient and checks for threshold
     */
    private void grantUltimateIngredient(Player player) {
        resourceTracker.addResource(player.getUniqueId(), ResourceType.ULTIMATE_INGREDIENT, 1);
        
        int ingredientCount = resourceTracker.getResourceCount(player.getUniqueId(), ResourceType.ULTIMATE_INGREDIENT);
        
        player.sendMessage("§a궁극 재료: " + ingredientCount + "/9");
        
        // Check if player reached threshold for ultimate
        if (ingredientCount >= ULTIMATE_INGREDIENT_THRESHOLD) {
            grantUltimatePotion(player);
        }
    }
    
    /**
     * Grants the Ultimate Potion
     */
    private void grantUltimatePotion(Player player) {
        // Consume 9 ingredients
        resourceTracker.consumeResources(player.getUniqueId(), ResourceType.ULTIMATE_INGREDIENT, ULTIMATE_INGREDIENT_THRESHOLD);
        
        // Give Ultimate Potion
        ItemStack ultimatePotion = itemProvider.createUltimateItem(ClassType.SCIENTIST);
        player.getInventory().addItem(ultimatePotion);
        
        player.sendMessage("§5§l궁극기 해금! 궁극의 물약 획득!");
        player.playSound(player.getLocation(), "entity.player.levelup", 1.0f, 1.0f);
    }
    
    /**
     * Handles Ultimate Potion lingering effect
     */
    @EventHandler
    public void onUltimatePotionSplash(LingeringPotionSplashEvent event) {
        ThrownPotion potion = event.getEntity();
        
        if (!(potion.getShooter() instanceof Player)) {
            return;
        }
        
        Player thrower = (Player) potion.getShooter();
        
        // Check if thrower is Scientist
        if (classManager.getPlayerClass(thrower) != ClassType.SCIENTIST) {
            return;
        }
        
        // Check if it's the Ultimate Potion
        ItemStack potionItem = potion.getItem();
        if (!itemProvider.isClassItem(potionItem)) {
            return;
        }
        
        String itemId = itemProvider.getItemId(potionItem);
        if (!"ultimate_potion".equals(itemId)) {
            return;
        }
        
        // Create area effect cloud
        AreaEffectCloud cloud = event.getAreaEffectCloud();
        cloud.setRadius(3.5f); // 7 block diameter
        cloud.setDuration(160); // 8 seconds
        cloud.setRadiusPerTick(0);
        
        // Start area effect task
        startUltimatePotionEffect(cloud, thrower);
    }
    
    /**
     * Starts the Ultimate Potion area effect
     */
    private void startUltimatePotionEffect(AreaEffectCloud cloud, Player scientist) {
        new BukkitRunnable() {
            int ticksElapsed = 0;
            
            @Override
            public void run() {
                if (!cloud.isValid() || cloud.isDead() || ticksElapsed >= 160) {
                    this.cancel();
                    return;
                }
                
                // Apply heal every 1 second (20 ticks) for Scientists
                // Apply damage every 0.5 seconds (10 ticks) for others
                if (ticksElapsed % 10 == 0) {
                    boolean isHealTick = (ticksElapsed % 20 == 0);
                    applyUltimateAreaEffects(cloud.getLocation(), scientist, isHealTick);
                }
                
                ticksElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Applies Ultimate Potion area effects
     */
    private void applyUltimateAreaEffects(Location center, Player scientist, boolean isHealTick) {
        double radius = 3.5;
        
        // Heal scientist every 0.5 seconds (changed from 1 second)
        effectApplicator.heal(scientist, 0.5); // 1 HP every 0.5 seconds (was 1 HP every 1 second)
        
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            
            LivingEntity target = (LivingEntity) entity;
            
            // Skip self
            if (target.equals(scientist)) {
                continue;
            }
            
            // If target is a player, check team mode restrictions (unless in practice mode)
            if (target instanceof Player) {
                Player player = (Player) target;
                ClassType playerClass = classManager.getPlayerClass(player);
                
                // Also check tag as backup
                if (playerClass == null) {
                    playerClass = classManager.getTagManager().getPlayerClassFromTag(player);
                }
                
                // In team mode (not practice mode), skip teammates only
                if (gameManager.getGameMode() == GameMode.TEAM && !gameManager.isInPracticeMode(scientist)) {
                    if (teamManager.getPlayerTeam(scientist) == teamManager.getPlayerTeam(player)) {
                        continue; // Skip all teammates
                    }
                }
                
                // Damage all players 2 HP every 0.5 seconds
                damageHandler.applyInstantDamage(player, 2.0);
            } else {
                // In practice mode, damage mobs too
                if (gameManager.isInPracticeMode(scientist)) {
                    damageHandler.applyInstantDamage(target, 2.0);
                }
            }
        }
    }
}

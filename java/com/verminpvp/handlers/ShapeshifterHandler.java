package com.verminpvp.handlers;

import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.models.ClassData;
import com.verminpvp.models.ClassType;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Shapeshifter class
 * 
 * Passive: Transforms over time, heals 20 HP on each transformation
 * 0min: Ocelot (5 hearts, Speed III)
 * 1min: Cow (15 hearts, 1 HP/3s regen, gets stone sword)
 * 2min: Sniffer (25 hearts, Slowness I + Resistance I, gets iron sword)
 * 4min: Ravager (35 hearts, Resistance II + Strength I, Charge ability)
 */
public class ShapeshifterHandler implements Listener {
    
    private final Plugin plugin;
    private final ClassManager classManager;
    private final GameManager gameManager;
    private final CooldownManager cooldownManager;
    private final DamageHandler damageHandler;
    
    // Track evolution tasks per player
    private final Map<UUID, BukkitTask> evolutionTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> regenTasks = new HashMap<>();
    
    // Evolution stages
    private static final int STAGE_OCELOT = 0;
    private static final int STAGE_COW = 1;
    private static final int STAGE_SNIFFER = 2;
    private static final int STAGE_RAVAGER = 3;
    
    // Evolution times (in seconds from game start)
    private static final int TIME_OCELOT = 0;
    private static final int TIME_COW = 60;      // 1 minute
    private static final int TIME_SNIFFER = 120; // 2 minutes
    private static final int TIME_RAVAGER = 240; // 4 minutes
    
    // Charge ability cooldown
    private static final String CHARGE_ABILITY_ID = "shapeshifter_charge";
    private static final double CHARGE_COOLDOWN = 7.0; // 7 seconds (changed from 15s)
    
    public ShapeshifterHandler(Plugin plugin, ClassManager classManager, GameManager gameManager,
                              CooldownManager cooldownManager, DamageHandler damageHandler) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.gameManager = gameManager;
        this.cooldownManager = cooldownManager;
        this.damageHandler = damageHandler;
    }
    
    /**
     * Start evolution system for a Shapeshifter player
     * Called when game starts
     */
    public void startEvolutionSystem(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        ClassData data = classManager.getClassData(player);
        if (data == null || data.getClassType() != ClassType.SHAPESHIFTER) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Cancel existing tasks
        stopEvolutionSystem(player);
        
        // Set initial stage to Ocelot
        data.setEvolutionStage(STAGE_OCELOT);
        applyEvolutionStage(player, STAGE_OCELOT);
        
        // Schedule evolution stages
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Check if player is online and in game or practice mode
            if (!player.isOnline()) {
                stopEvolutionSystem(player);
                return;
            }
            
            boolean inGame = gameManager.isGameActive();
            boolean inPractice = gameManager.isInPracticeMode(player);
            
            if (!inGame && !inPractice) {
                stopEvolutionSystem(player);
                return;
            }
            
            // Get game time elapsed
            int gameTimeElapsed;
            if (inPractice) {
                // In practice mode, use practice mode time
                gameTimeElapsed = 300 - gameManager.getPracticeModeTimeRemaining(player);
            } else {
                // In real game, use game time
                gameTimeElapsed = 300 - gameManager.getGameTimeRemaining(); // 5 minutes = 300 seconds
            }
            
            // Check which stage player should be at
            int targetStage = STAGE_OCELOT;
            if (gameTimeElapsed >= TIME_RAVAGER) {
                targetStage = STAGE_RAVAGER;
            } else if (gameTimeElapsed >= TIME_SNIFFER) {
                targetStage = STAGE_SNIFFER;
            } else if (gameTimeElapsed >= TIME_COW) {
                targetStage = STAGE_COW;
            }
            
            // Evolve if needed
            int currentStage = data.getEvolutionStage();
            if (targetStage > currentStage) {
                evolveToStage(player, targetStage);
            }
        }, 20L, 20L); // Check every second
        
        evolutionTasks.put(playerId, task);
    }
    
    /**
     * Stop evolution system for a player
     */
    public void stopEvolutionSystem(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Cancel evolution task
        BukkitTask evolutionTask = evolutionTasks.remove(playerId);
        if (evolutionTask != null) {
            evolutionTask.cancel();
        }
        
        // Cancel regen task
        BukkitTask regenTask = regenTasks.remove(playerId);
        if (regenTask != null) {
            regenTask.cancel();
        }
        
        // Remove disguise
        if (DisguiseAPI.isDisguised(player)) {
            DisguiseAPI.getDisguise(player).removeDisguise();
        }
    }
    
    /**
     * Evolve player to a specific stage
     */
    private void evolveToStage(Player player, int stage) {
        ClassData data = classManager.getClassData(player);
        if (data == null) {
            return;
        }
        
        int currentStage = data.getEvolutionStage();
        if (stage <= currentStage) {
            return;
        }
        
        // Update stage
        data.setEvolutionStage(stage);
        
        // Heal 20 HP on evolution
        double currentHealth = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxHealth, currentHealth + 40.0)); // 20 hearts = 40 HP
        
        // Apply new stage
        applyEvolutionStage(player, stage);
        
        // Announce evolution
        String stageName = getStageName(stage);
        player.sendMessage("§a진화: §e" + stageName);
        player.sendTitle("§a진화!", "§e" + stageName, 10, 40, 10);
    }
    
    /**
     * Apply evolution stage effects
     */
    private void applyEvolutionStage(Player player, int stage) {
        // Remove old effects
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        
        // Stop old regen task
        BukkitTask oldRegenTask = regenTasks.remove(player.getUniqueId());
        if (oldRegenTask != null) {
            oldRegenTask.cancel();
        }
        
        // Give charge item to all stages
        giveChargeItem(player);
        
        switch (stage) {
            case STAGE_OCELOT:
                applyOcelotStage(player);
                break;
            case STAGE_COW:
                applyCowStage(player);
                break;
            case STAGE_SNIFFER:
                applySnifferStage(player);
                break;
            case STAGE_RAVAGER:
                applyRavagerStage(player);
                break;
        }
    }
    
    /**
     * Apply Ocelot stage (0 min)
     * 5 hearts (10 HP), Speed III, Weakness III
     */
    private void applyOcelotStage(Player player) {
        // Set max health to 5 hearts (10 HP)
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(10.0);
        player.setHealth(10.0);
        
        // Apply Speed III
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 
            Integer.MAX_VALUE, 2, false, false, false));
        
        // Apply Weakness III (nerf)
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 
            Integer.MAX_VALUE, 2, false, false, false));
        
        // Apply disguise
        MobDisguise disguise = new MobDisguise(DisguiseType.OCELOT);
        DisguiseAPI.disguiseToAll(player, disguise);
    }
    
    /**
     * Apply Cow stage (1 min)
     * 15 hearts (30 HP), 1 HP/3s regen, gets stone sword
     */
    private void applyCowStage(Player player) {
        // Set max health to 15 hearts (30 HP)
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(30.0);
        player.setHealth(30.0);
        
        // Start regen task (1 HP every 3 seconds)
        BukkitTask regenTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !gameManager.isGameActive()) {
                return;
            }
            
            ClassData data = classManager.getClassData(player);
            if (data == null || data.getEvolutionStage() != STAGE_COW) {
                return;
            }
            
            double currentHealth = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (currentHealth < maxHealth) {
                player.setHealth(Math.min(maxHealth, currentHealth + 2.0)); // 1 heart = 2 HP
            }
        }, 60L, 60L); // Every 3 seconds
        
        regenTasks.put(player.getUniqueId(), regenTask);
        
        // Give stone sword
        ItemStack stoneSword = new ItemStack(Material.STONE_SWORD);
        ItemMeta swordMeta = stoneSword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.setUnbreakable(true);
            swordMeta.setDisplayName("§7돌검");
            stoneSword.setItemMeta(swordMeta);
        }
        player.getInventory().addItem(stoneSword);
        
        // Apply disguise
        MobDisguise disguise = new MobDisguise(DisguiseType.COW);
        DisguiseAPI.disguiseToAll(player, disguise);
    }
    
    /**
     * Apply Sniffer stage (2 min)
     * 25 hearts (50 HP), Slowness I + Resistance I, gets iron sword
     */
    private void applySnifferStage(Player player) {
        // Set max health to 25 hearts (50 HP)
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(50.0);
        player.setHealth(50.0);
        
        // DON'T apply Slowness - it prevents movement completely
        // Sniffer is naturally slow, no need for slowness effect
        
        // Apply Resistance I
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 
            Integer.MAX_VALUE, 0, false, false, false));
        
        // Give iron sword
        ItemStack ironSword = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = ironSword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.setUnbreakable(true);
            swordMeta.setDisplayName("§f철검");
            ironSword.setItemMeta(swordMeta);
        }
        player.getInventory().addItem(ironSword);
        
        // Apply disguise
        MobDisguise disguise = new MobDisguise(DisguiseType.SNIFFER);
        DisguiseAPI.disguiseToAll(player, disguise);
    }
    
    /**
     * Apply Ravager stage (4 min)
     * 35 hearts (70 HP), Resistance II + Strength I, gets charge item
     */
    private void applyRavagerStage(Player player) {
        // Set max health to 35 hearts (70 HP)
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(70.0);
        player.setHealth(70.0);
        
        // Apply Resistance II
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 
            Integer.MAX_VALUE, 1, false, false, false));
        
        // Apply Strength I
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 
            Integer.MAX_VALUE, 0, false, false, false));
        
        // Apply disguise
        MobDisguise disguise = new MobDisguise(DisguiseType.RAVAGER);
        DisguiseAPI.disguiseToAll(player, disguise);
    }
    
    /**
     * Give charge item to player (only if they don't have it)
     */
    private void giveChargeItem(Player player) {
        // Check if player already has charge item
        boolean hasChargeItem = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BLAZE_ROD) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§c§l돌진")) {
                    hasChargeItem = true;
                    break;
                }
            }
        }
        
        // Only give charge item if player doesn't have it
        if (!hasChargeItem) {
            ItemStack chargeItem = new ItemStack(Material.BLAZE_ROD);
            ItemMeta chargeMeta = chargeItem.getItemMeta();
            if (chargeMeta != null) {
                chargeMeta.setDisplayName("§c§l돌진");
                chargeMeta.setLore(java.util.Arrays.asList(
                    "§7우클릭으로 신속 V 획득 (0.4초)",
                    "§7이 상태에서 충돌한 대상에게 구속 V (0.5초)",
                    "§e쿨타임: 7초"
                ));
                chargeMeta.setUnbreakable(true);
                chargeItem.setItemMeta(chargeMeta);
            }
            
            // Add to inventory
            player.getInventory().addItem(chargeItem);
        }
    }
    
    /**
     * Get stage name for display
     */
    private String getStageName(int stage) {
        switch (stage) {
            case STAGE_OCELOT:
                return "오셀롯";
            case STAGE_COW:
                return "소";
            case STAGE_SNIFFER:
                return "스니퍼";
            case STAGE_RAVAGER:
                return "파괴수";
            default:
                return "알 수 없음";
        }
    }
    
    /**
     * Handle player interactions for charge ability
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ClassData data = classManager.getClassData(player);
        
        if (data == null || data.getClassType() != ClassType.SHAPESHIFTER) {
            return;
        }
        
        // Only work in game or practice mode
        if (!gameManager.isGameActive() && !gameManager.isInPracticeMode(player)) {
            return;
        }
        
        // Check if player is holding charge item (Blaze Rod with specific name)
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BLAZE_ROD) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.getDisplayName().equals("§c§l돌진")) {
            return;
        }
        
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), CHARGE_ABILITY_ID)) {
            double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), CHARGE_ABILITY_ID);
            player.sendMessage("§c돌진 쿨타임: " + String.format("%.1f", remaining) + "초");
            return;
        }
        
        event.setCancelled(true);
        handleCharge(player);
        
        // Set cooldown
        cooldownManager.setCooldown(player.getUniqueId(), CHARGE_ABILITY_ID, CHARGE_COOLDOWN);
    }
    
    // Track players in charge state
    private final Map<UUID, BukkitTask> chargeStateTasks = new HashMap<>();
    
    /**
     * Handle charge ability
     * Give Speed V for 0.4 seconds, collision detection for Slowness V
     */
    private void handleCharge(Player player) {
        // Apply Speed V for 0.4 seconds (8 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 
            8, 4, false, false, true)); // 0.4 seconds, Speed V
        
        // Visual effect
        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, 
            player.getLocation(), 20, 0.3, 0.5, 0.3, 0.1);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.5f);
        
        player.sendMessage("§a돌진 활성화!");
        
        // Cancel existing charge state task if any
        BukkitTask existingTask = chargeStateTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Start collision detection for 0.4 seconds
        BukkitTask chargeTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int ticksElapsed = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksElapsed >= 8) { // 8 ticks = 0.4 seconds
                    BukkitTask task = chargeStateTasks.remove(player.getUniqueId());
                    if (task != null) {
                        task.cancel();
                    }
                    return;
                }
                
                // Check for nearby entities
                for (Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        
                        // Skip teammates in team mode (unless in practice mode)
                        if (target instanceof Player) {
                            Player targetPlayer = (Player) target;
                            if (gameManager.getGameMode() == com.verminpvp.models.GameMode.TEAM && !gameManager.isInPracticeMode(player)) {
                                // Get team manager from VerminPVP plugin
                                com.verminpvp.VerminPVP verminPlugin = (com.verminpvp.VerminPVP) plugin;
                                com.verminpvp.managers.TeamManager teamMgr = verminPlugin.getTeamManager();
                                
                                com.verminpvp.models.Team playerTeam = teamMgr.getPlayerTeam(player);
                                com.verminpvp.models.Team targetTeam = teamMgr.getPlayerTeam(targetPlayer);
                                if (playerTeam != null && playerTeam == targetTeam) {
                                    continue;
                                }
                            }
                        }
                        
                        // Apply Slowness V for 0.5 seconds (changed from 1s)
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 
                            10, 4, false, false, false)); // 0.5 seconds, Slowness V
                        
                        // Visual effect on hit
                        target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, 
                            target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
                        
                        player.sendMessage("§e돌진 적중!");
                        
                        // End charge state after hitting
                        BukkitTask task = chargeStateTasks.remove(player.getUniqueId());
                        if (task != null) {
                            task.cancel();
                        }
                        return;
                    }
                }
                
                ticksElapsed++;
            }
        }, 0L, 1L); // Check every tick
        
        chargeStateTasks.put(player.getUniqueId(), chargeTask);
    }
    
    /**
     * Cleanup all Shapeshifter tasks and disguises
     */
    public void cleanupAll() {
        // Cancel all tasks
        for (BukkitTask task : evolutionTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        evolutionTasks.clear();
        
        for (BukkitTask task : regenTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        regenTasks.clear();
        
        for (BukkitTask task : chargeStateTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        chargeStateTasks.clear();
        
        // Remove all disguises
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (DisguiseAPI.isDisguised(player)) {
                DisguiseAPI.getDisguise(player).removeDisguise();
            }
        }
    }
    
    /**
     * Cleanup Shapeshifter tasks and disguise for a specific player
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel evolution task for this player
        BukkitTask evolutionTask = evolutionTasks.get(playerId);
        if (evolutionTask != null) {
            evolutionTask.cancel();
            evolutionTasks.remove(playerId);
        }
        
        // Cancel regen task for this player
        BukkitTask regenTask = regenTasks.get(playerId);
        if (regenTask != null) {
            regenTask.cancel();
            regenTasks.remove(playerId);
        }
        
        // Cancel charge state task for this player
        BukkitTask chargeTask = chargeStateTasks.get(playerId);
        if (chargeTask != null) {
            chargeTask.cancel();
            chargeStateTasks.remove(playerId);
        }
        
        // Remove disguise for this player
        if (DisguiseAPI.isDisguised(player)) {
            DisguiseAPI.getDisguise(player).removeDisguise();
        }
        
        // Clear evolution stage from ClassData
        ClassData data = classManager.getClassData(player);
        if (data != null) {
            data.setEvolutionStage(0);
        }
    }
}


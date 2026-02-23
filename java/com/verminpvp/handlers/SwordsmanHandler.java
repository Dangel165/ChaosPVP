package com.verminpvp.handlers;

import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.CooldownManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.managers.ResourceTracker;
import com.verminpvp.models.AbilityIds;
import com.verminpvp.models.ClassData;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.ResourceType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Handles Swordsman class abilities and mechanics
 * 
 * Passive: +1 damage with stone sword
 * Diamond Sword: Right-click to attack forward (8 damage, 12.5s cooldown, grants Netherite Shard)
 * Netherite Sword: Left-click deals 4 base + 16 instant damage (durability 1, max 1)
 */
public class SwordsmanHandler implements Listener {
    
    private final Plugin plugin;
    private final ClassManager classManager;
    private final GameManager gameManager;
    private final CooldownManager cooldownManager;
    private final ResourceTracker resourceTracker;
    private final ItemProvider itemProvider;
    private final DamageHandler damageHandler;
    
    private static final double RAYCAST_RANGE = 3.5;
    private static final int NETHERITE_SHARD_THRESHOLD = 3;
    private static final double DIAMOND_SWORD_COOLDOWN = 12.5;
    
    public SwordsmanHandler(Plugin plugin, ClassManager classManager, GameManager gameManager,
                           CooldownManager cooldownManager, ResourceTracker resourceTracker, 
                           ItemProvider itemProvider, DamageHandler damageHandler) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.gameManager = gameManager;
        this.cooldownManager = cooldownManager;
        this.resourceTracker = resourceTracker;
        this.itemProvider = itemProvider;
        this.damageHandler = damageHandler;
    }
    
    /**
     * Give starting items to Swordsman player
     */
    public void giveStartingItems(Player player) {
        // Give Diamond Sword with stone sword damage
        ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = diamondSword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§l다이아 검");
            meta.setLore(java.util.Arrays.asList(
                "§7우클릭으로 전방 공격",
                "§7피해량: 8 (4칸)",
                "§7적중시 네더라이트 파편 획득",
                "§e쿨타임: 12.5초"
            ));
            meta.setUnbreakable(true);
            
            // Set damage to stone sword level (5 damage = 2.5 hearts)
            // Diamond sword base is 7, stone sword is 5, so reduce by 2
            meta.addAttributeModifier(
                org.bukkit.attribute.Attribute.ATTACK_DAMAGE,
                new org.bukkit.attribute.AttributeModifier(
                    java.util.UUID.randomUUID(),
                    "generic.attack_damage",
                    -2.0, // Reduce damage by 2 (7 - 2 = 5, same as stone sword)
                    org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                    org.bukkit.inventory.EquipmentSlot.HAND
                )
            );
            
            diamondSword.setItemMeta(meta);
        }
        player.getInventory().addItem(diamondSword);
    }
    
    /**
     * Passive: +1 damage bonus on stone sword attacks
     */
    @EventHandler
    public void onSwordsmanAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        ClassData data = classManager.getClassData(attacker);
        
        if (data == null || data.getClassType() != ClassType.SWORDSMAN) {
            return;
        }
        
        // Only work in game or practice mode
        if (!gameManager.isGameActive() && !gameManager.isInPracticeMode(attacker)) {
            return;
        }
        
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        
        // Check if using stone sword (not diamond or netherite)
        if (weapon != null && weapon.getType() == Material.STONE_SWORD) {
            // Add +1 damage bonus
            event.setDamage(event.getDamage() + 1.0);
        }
    }
    
    /**
     * Handle Diamond Sword right-click attack
     */
    @EventHandler
    public void onDiamondSwordUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ClassData data = classManager.getClassData(player);
        
        if (data == null || data.getClassType() != ClassType.SWORDSMAN) {
            return;
        }
        
        // Only work in game or practice mode
        if (!gameManager.isGameActive() && !gameManager.isInPracticeMode(player)) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.DIAMOND_SWORD) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.getDisplayName().equals("§b§l다이아 검")) {
            return;
        }
        
        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), AbilityIds.DIAMOND_SWORD)) {
            double remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), AbilityIds.DIAMOND_SWORD);
            player.sendMessage("§c다이아 검 쿨타임: " + String.format("%.1f", remaining) + "초");
            return;
        }
        
        event.setCancelled(true);
        
        // Raycast to find target
        LivingEntity target = raycastForTarget(player);
        
        if (target == null) {
            player.sendMessage("§c대상을 찾을 수 없습니다!");
            return;
        }
        
        // Check if target is a teammate in team mode (not practice mode)
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            
            // Get TeamManager from plugin
            com.verminpvp.VerminPVP pluginInstance = (com.verminpvp.VerminPVP) plugin;
            com.verminpvp.managers.TeamManager teamManager = pluginInstance.getTeamManager();
            
            // In team mode (not practice mode), check if target is on same team
            if (gameManager.getGameMode() == com.verminpvp.models.GameMode.TEAM && 
                !gameManager.isInPracticeMode(player)) {
                if (teamManager.areSameTeam(player, targetPlayer)) {
                    player.sendMessage("§c아군을 공격할 수 없습니다!");
                    return;
                }
            }
        }
        
        // Apply 8 damage (4 hearts)
        damageHandler.applyMeleeDamage(player, target, 8.0);
        
        // Grant Netherite Shard
        grantNetheriteShard(player);
        
        // Set cooldown
        cooldownManager.setCooldown(player.getUniqueId(), AbilityIds.DIAMOND_SWORD, DIAMOND_SWORD_COOLDOWN);
        
        player.sendMessage("§b다이아 검 적중! 네더라이트 파편: " + 
            resourceTracker.getResourceCount(player.getUniqueId(), ResourceType.PRISMARINE_SHARD) + "/3");
    }
    
    /**
     * Grants a Netherite Shard and checks for ultimate threshold
     */
    private void grantNetheriteShard(Player player) {
        resourceTracker.addResource(player.getUniqueId(), ResourceType.PRISMARINE_SHARD, 1);
        
        int shardCount = resourceTracker.getResourceCount(player.getUniqueId(), ResourceType.PRISMARINE_SHARD);
        
        // Check if player reached threshold for ultimate
        if (shardCount >= NETHERITE_SHARD_THRESHOLD) {
            grantNetheriteSword(player);
        }
    }
    
    /**
     * Grants the Netherite Sword ultimate item
     */
    private void grantNetheriteSword(Player player) {
        // Check if player already has a Netherite Sword
        int netheriteCount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.NETHERITE_SWORD) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§5§l네더라이트 검")) {
                    netheriteCount++;
                }
            }
        }
        
        // If player already has 1 or more, don't give another
        if (netheriteCount >= 1) {
            player.sendMessage("§c이미 네더라이트 검을 보유하고 있습니다! (최대 1개)");
            return;
        }
        
        // Consume 3 shards
        resourceTracker.consumeResources(player.getUniqueId(), ResourceType.PRISMARINE_SHARD, NETHERITE_SHARD_THRESHOLD);
        
        // Give Netherite Sword with durability 1
        ItemStack netheriteSword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = netheriteSword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5§l네더라이트 검");
            meta.setLore(java.util.Arrays.asList(
                "§7좌클릭 공격시 추가 즉시피해 16 (8칸)",
                "§7피해량: 4 (2칸)",
                "§c내구도: 1회 사용"
            ));
            // Set max durability to 1 by setting damage to max-1
            netheriteSword.setDurability((short) (netheriteSword.getType().getMaxDurability() - 1));
            netheriteSword.setItemMeta(meta);
        }
        player.getInventory().addItem(netheriteSword);
        
        player.sendMessage("§5§l궁극기 해금! 네더라이트 검 획득!");
        player.playSound(player.getLocation(), "entity.player.levelup", 1.0f, 1.0f);
    }
    
    /**
     * Handles Netherite Sword ultimate attack
     */
    @EventHandler
    public void onNetheriteSwordAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        ClassData data = classManager.getClassData(attacker);
        
        if (data == null || data.getClassType() != ClassType.SWORDSMAN) {
            return;
        }
        
        // Only work in game or practice mode
        if (!gameManager.isGameActive() && !gameManager.isInPracticeMode(attacker)) {
            return;
        }
        
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() != Material.NETHERITE_SWORD) {
            return;
        }
        
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.getDisplayName().equals("§5§l네더라이트 검")) {
            return;
        }
        
        // Apply ultimate damage: 4 base + 16 instant (8 hearts instant)
        event.setDamage(4.0); // Base damage (respects armor) - 2 hearts
        
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            damageHandler.applyInstantDamage(target, 16.0); // Instant damage (bypasses armor) - 8 hearts
        }
        
        // Remove the sword after use (durability 1)
        weapon.setAmount(0);
        
        attacker.sendMessage("§5§l궁극기 공격!");
    }
    
    /**
     * Raycasts forward to find a target entity
     */
    private LivingEntity raycastForTarget(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        
        // Raycast for entities
        RayTraceResult result = player.getWorld().rayTraceEntities(
            eyeLocation,
            direction,
            RAYCAST_RANGE,
            0.5,
            entity -> entity instanceof LivingEntity && !entity.equals(player)
        );
        
        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }
        
        return null;
    }
    
    /**
     * Cleanup player data
     */
    public void cleanupPlayer(Player player) {
        resourceTracker.clearResource(player.getUniqueId(), ResourceType.PRISMARINE_SHARD);
    }
}

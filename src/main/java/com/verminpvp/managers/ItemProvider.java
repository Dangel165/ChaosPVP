package com.verminpvp.managers;

import com.verminpvp.models.ClassType;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides class-specific items with NBT tagging for identification
 */
public class ItemProvider {
    
    private final Plugin plugin;
    private final NamespacedKey classItemKey;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey infiniteDurabilityKey;
    
    public ItemProvider(Plugin plugin) {
        this.plugin = plugin;
        this.classItemKey = new NamespacedKey(plugin, "class_item");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.infiniteDurabilityKey = new NamespacedKey(plugin, "infinite_durability");
    }
    
    /**
     * Creates the primary weapon for a class
     */
    public ItemStack createClassWeapon(ClassType classType) {
        if (classType == null) {
            throw new IllegalArgumentException("ClassType cannot be null");
        }
        
        switch (classType) {
            case SWORDSMAN:
                return createSwordsmanWeapon();
            case SCIENTIST:
                return null; // Scientist has no starting weapon
            case PLAGUE_SPREADER:
                return createPlagueWeaponSword();
            case SHIELD_SOLDIER:
                return null; // Shield Soldier has no starting sword
            case CRITICAL_CUTTER:
                return createCriticalCutterSword();
            case NAVIGATOR:
                return createNavigatorSword();
            case CAPTAIN:
                return createCaptainSword();
            case STAMPER:
                return null; // Stamper has no starting sword
            case TIME_ENGRAVER:
                return null; // Time Engraver has no starting sword
            case CAVALRY:
                return createCavalrySpear();
            case VITALITY_CUTTER:
                return null; // Vitality Cutter has no starting sword
            case MARATHONER:
                return null; // Marathoner has no starting sword
            default:
                return null;
        }
    }
    
    /**
     * Creates a special item for a class (e.g., shields, swords, engines)
     */
    public ItemStack createSpecialItem(ClassType classType, String itemId) {
        if (classType == null || itemId == null) {
            throw new IllegalArgumentException("ClassType and itemId cannot be null");
        }
        
        switch (classType) {
            case SWORDSMAN:
                return createSwordsmanSpecialItem(itemId);
            case SCIENTIST:
                return createScientistPotion(itemId);
            case PLAGUE_SPREADER:
                return createPlagueEngine(itemId);
            case SHIELD_SOLDIER:
                return createShield(itemId);
            case CRITICAL_CUTTER:
                return createCriticalCutterItem(itemId);
            case NAVIGATOR:
                return createNavigatorItem(itemId);
            case CAPTAIN:
                return createCaptainItem(itemId);
            case STAMPER:
                return createStamperItem(itemId);
            case TIME_ENGRAVER:
                return createTimeEngraverItem(itemId);
            case CAVALRY:
                return createCavalryItem(itemId);
            case VITALITY_CUTTER:
                return null; // Vitality Cutter has no special items
            case MARATHONER:
                return createMarathonerItem(itemId);
            default:
                return null;
        }
    }
    
    /**
     * Creates a Minecraft Instant Health II potion for Scientist
     */
    public ItemStack createMinecraftRegenerationPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) potion.getItemMeta();
        
        // Add Instant Health II effect
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1), true);
        
        meta.setDisplayName("§c즉시 회복의 물약 II");
        List<String> lore = new ArrayList<>();
        lore.add("§7마시기: 즉시 회복 II");
        lore.add("§7사용 시 궁극의 재료 1개 획득");
        meta.setLore(lore);
        
        // Mark as class item so it counts toward potion limit
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "SCIENTIST");
        pdc.set(itemIdKey, PersistentDataType.STRING, "minecraft_regeneration_potion");
        
        potion.setItemMeta(meta);
        return potion;
    }
    
    /**
     * Creates an ultimate item for a class
     */
    public ItemStack createUltimateItem(ClassType classType) {
        if (classType == null) {
            throw new IllegalArgumentException("ClassType cannot be null");
        }
        
        switch (classType) {
            case SWORDSMAN:
                return createNetheriteSword();
            case SCIENTIST:
                return createUltimatePotion();
            default:
                return null;
        }
    }
    
    /**
     * Checks if an item is a class-specific item
     */
    public boolean isClassItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(classItemKey, PersistentDataType.STRING);
    }
    
    /**
     * Gets the item ID from a class item
     */
    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(itemIdKey, PersistentDataType.STRING);
    }
    
    // Private helper methods for creating specific items
    
    private ItemStack createSwordsmanWeapon() {
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        
        meta.setDisplayName("§6검술가의 검");
        List<String> lore = new ArrayList<>();
        lore.add("§7검술가 시작 무기");
        lore.add("§7+1 데미지 보너스");
        meta.setLore(lore);
        
        // Mark as class item with infinite durability
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "SWORDSMAN");
        pdc.set(itemIdKey, PersistentDataType.STRING, "swordsman_sword");
        pdc.set(infiniteDurabilityKey, PersistentDataType.BYTE, (byte) 1);
        
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        sword.setItemMeta(meta);
        return sword;
    }

    
    private ItemStack createSwordsmanSpecialItem(String itemId) {
        switch (itemId) {
            case "diamond_sword":
                return createSpecialSword(Material.DIAMOND_SWORD, "§b다이아몬드 검", 
                    "§7우클릭: 1+1+1 데미지", "§7쿨다운: 20초", "diamond_sword");
            default:
                return null;
        }
    }
    
    private ItemStack createSpecialSword(Material material, String name, String... loreLines) {
        ItemStack sword = new ItemStack(material);
        ItemMeta meta = sword.getItemMeta();
        
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        for (int i = 0; i < loreLines.length - 1; i++) {
            lore.add(loreLines[i]);
        }
        meta.setLore(lore);
        
        String itemId = loreLines[loreLines.length - 1];
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "SWORDSMAN");
        pdc.set(itemIdKey, PersistentDataType.STRING, itemId);
        pdc.set(infiniteDurabilityKey, PersistentDataType.BYTE, (byte) 1);
        
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        // Set attack damage to match stone sword (5.0 damage = 2.5 hearts)
        meta.addAttributeModifier(org.bukkit.attribute.Attribute.ATTACK_DAMAGE,
            new org.bukkit.attribute.AttributeModifier(
                java.util.UUID.randomUUID(),
                "generic.attack_damage",
                5.0,
                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                org.bukkit.inventory.EquipmentSlot.HAND
            )
        );
        
        sword.setItemMeta(meta);
        return sword;
    }
    
    private ItemStack createNetheriteSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        
        meta.setDisplayName("§5궁극의 네더라이트 검");
        List<String> lore = new ArrayList<>();
        lore.add("§7공격: 6 기본 + 12 즉시 데미지");
        lore.add("§7일회용");
        meta.setLore(lore);
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "SWORDSMAN");
        pdc.set(itemIdKey, PersistentDataType.STRING, "netherite_sword_ultimate");
        
        sword.setItemMeta(meta);
        return sword;
    }
    
    private ItemStack createScientistPotion(String potionType) {
        Material potionMaterial;
        String koreanType;
        String effectDesc;
        
        // Determine material and descriptions based on potion type
        switch(potionType) {
            case "instant_damage":
                potionMaterial = Material.SPLASH_POTION;
                koreanType = "즉시피해";
                effectDesc = "§7투척: 8 데미지"; // Updated from 4 to 8
                break;
            case "instant_healing":
                potionMaterial = Material.POTION;
                koreanType = "즉시회복";
                effectDesc = "§7마시기: 8 회복"; // Updated from 4 to 8
                break;
            case "slowness":
                potionMaterial = Material.SPLASH_POTION;
                koreanType = "감속";
                effectDesc = "§7투척: 구속 V (1초)";
                break;
            case "blindness":
                potionMaterial = Material.SPLASH_POTION;
                koreanType = "실명";
                effectDesc = "§7투척: 실명 (2.5초)";
                break;
            case "resistance":
                potionMaterial = Material.POTION;
                koreanType = "저항";
                effectDesc = "§7마시기: 저항 IV (1.5초)";
                break;
            case "poison":
                potionMaterial = Material.SPLASH_POTION;
                koreanType = "독";
                effectDesc = "§7투척: 독 II (3초)";
                break;
            default:
                potionMaterial = Material.POTION;
                koreanType = potionType;
                effectDesc = "§7효과 적용";
        }
        
        ItemStack potion = new ItemStack(potionMaterial);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) potion.getItemMeta();
        
        // Add custom effects for specific potions
        if (potionType.equals("instant_damage")) {
            meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1), true);
        } else if (potionType.equals("instant_healing")) {
            meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1), true);
        } else if (potionType.equals("blindness")) {
            meta.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0), true);
        } else if (potionType.equals("resistance")) {
            meta.addCustomEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30, 3), true);
        } else if (potionType.equals("slowness")) {
            meta.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 4), true);
        } else if (potionType.equals("poison")) {
            meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 60, 1), true);
        }
        
        meta.setDisplayName("§a과학자 물약: " + koreanType);
        List<String> lore = new ArrayList<>();
        lore.add(effectDesc);
        lore.add("§7적중/사용 시 궁극의 재료 1개 획득");
        meta.setLore(lore);
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "SCIENTIST");
        pdc.set(itemIdKey, PersistentDataType.STRING, "scientist_potion_" + potionType.toLowerCase());
        
        potion.setItemMeta(meta);
        return potion;
    }
    
    private ItemStack createUltimatePotion() {
        ItemStack potion = new ItemStack(Material.LINGERING_POTION);
        ItemMeta meta = potion.getItemMeta();
        
        meta.setDisplayName("§5궁극의 물약");
        List<String> lore = new ArrayList<>();
        lore.add("§77블록 범위 8초간 생성");
        lore.add("§7과학자 1 HP/초 회복");
        lore.add("§7다른 플레이어 0.5초마다 2 HP 데미지");
        meta.setLore(lore);
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "SCIENTIST");
        pdc.set(itemIdKey, PersistentDataType.STRING, "ultimate_potion");
        
        potion.setItemMeta(meta);
        return potion;
    }
    
    private ItemStack createPlagueWeaponSword() {
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        
        meta.setDisplayName("§2역병전파자의 검");
        List<String> lore = new ArrayList<>();
        lore.add("§7역병전파자 시작 무기");
        meta.setLore(lore);
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "PLAGUE_SPREADER");
        pdc.set(itemIdKey, PersistentDataType.STRING, "plague_sword");
        pdc.set(infiniteDurabilityKey, PersistentDataType.BYTE, (byte) 1);
        
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        sword.setItemMeta(meta);
        return sword;
    }
    
    private ItemStack createPlagueEngine(String engineType) {
        // Use nether star as non-consumable item
        Material material = Material.NETHER_STAR;
        ItemStack engine = new ItemStack(material);
        ItemMeta meta = engine.getItemMeta();
        
        if (engineType.equals("virulent_engine")) {
            meta.setDisplayName("§2맹독 엔진");
            List<String> lore = new ArrayList<>();
            lore.add("§7우클릭: 맹독 장판 생성 (10초)");
            lore.add("§76블록 직경");
            lore.add("§a역병전파자: 신속 I (1초)");
            lore.add("§7다른 직업: 3 피해 + 약화 I + 독 I");
            lore.add("§c사용 후 소멸");
            meta.setLore(lore);
        } else {
            meta.setDisplayName("§5극독 엔진");
            List<String> lore = new ArrayList<>();
            lore.add("§7우클릭: 극독 장판 생성 (10초)");
            lore.add("§78블록 직경");
            lore.add("§a역병전파자: 신속 II + 독 I (1초)");
            lore.add("§7다른 직업: 4 피해 + 약화 II + 독 II");
            lore.add("§c사용 후 소멸");
            meta.setLore(lore);
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "PLAGUE_SPREADER");
        pdc.set(itemIdKey, PersistentDataType.STRING, engineType);
        
        engine.setItemMeta(meta);
        return engine;
    }
    
    private ItemStack createShield(String shieldType) {
        ItemStack shield = new ItemStack(Material.SHIELD);
        BlockStateMeta meta = (BlockStateMeta) shield.getItemMeta();
        
        // Apply banner pattern for colored shields
        org.bukkit.block.Banner banner = (org.bukkit.block.Banner) meta.getBlockState();
        
        switch (shieldType) {
            case "red_shield":
                meta.setDisplayName("§c빨간 방패");
                List<String> loreRed = new ArrayList<>();
                loreRed.add("§7막기: 공격자에게 6 데미지");
                loreRed.add("§7쿨다운: 6초");
                meta.setLore(loreRed);
                
                // Red shield pattern
                banner.setBaseColor(DyeColor.RED);
                banner.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                banner.addPattern(new Pattern(DyeColor.RED, PatternType.GRADIENT));
                break;
                
            case "blue_shield":
                meta.setDisplayName("§9파란 방패");
                List<String> loreBlue = new ArrayList<>();
                loreBlue.add("§7막기: 흡수 하트 2개 (10초)");
                loreBlue.add("§7중첩 불가");
                loreBlue.add("§7쿨다운: 11초");
                meta.setLore(loreBlue);
                
                // Blue shield pattern
                banner.setBaseColor(DyeColor.BLUE);
                banner.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.BORDER));
                banner.addPattern(new Pattern(DyeColor.BLUE, PatternType.GRADIENT));
                break;
                
            case "basic_shield":
                meta.setDisplayName("§7기본 방패");
                List<String> loreBasic = new ArrayList<>();
                loreBasic.add("§7막기: 공격자에게 약화 I (1.5초)");
                loreBasic.add("§7쿨다운: 4초");
                meta.setLore(loreBasic);
                
                // Gray shield pattern
                banner.setBaseColor(DyeColor.GRAY);
                banner.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                banner.addPattern(new Pattern(DyeColor.GRAY, PatternType.STRIPE_CENTER));
                break;
                
            case "golden_shield":
                meta.setDisplayName("§6§l황금 방패");
                List<String> loreGolden = new ArrayList<>();
                loreGolden.add("§7막기: 힘 II (8초)");
                loreGolden.add("§7+ 신속 II (8초)");
                loreGolden.add("§7+ 흡수 하트 6칸 (8초)");
                loreGolden.add("§c일회용");
                meta.setLore(loreGolden);
                
                // Golden shield pattern
                banner.setBaseColor(DyeColor.YELLOW);
                banner.addPattern(new Pattern(DyeColor.ORANGE, PatternType.BORDER));
                banner.addPattern(new Pattern(DyeColor.YELLOW, PatternType.GRADIENT));
                banner.addPattern(new Pattern(DyeColor.ORANGE, PatternType.RHOMBUS));
                break;
        }
        
        banner.update();
        meta.setBlockState(banner);
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "SHIELD_SOLDIER");
        pdc.set(itemIdKey, PersistentDataType.STRING, shieldType);
        pdc.set(infiniteDurabilityKey, PersistentDataType.BYTE, (byte) 1);
        
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        shield.setItemMeta(meta);
        return shield;
    }
    
    private ItemStack createCriticalCutterSword() {
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = sword.getItemMeta();
        
        meta.setDisplayName("§c크리티컬 커터");
        List<String> lore = new ArrayList<>();
        lore.add("§7피해량: 5 (2.5칸)");
        lore.add("§7공격 속도: 2.2");
        lore.add("§7공격 적중 시 5% 확률로 치명타");
        lore.add("§7치명타: 가한 피해량만큼 추가 즉시 피해");
        lore.add("§7치명타: 체력 2 (1칸) 회복");
        lore.add("§7비-치명타 공격마다 확률 +1%");
        meta.setLore(lore);
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "CRITICAL_CUTTER");
        pdc.set(itemIdKey, PersistentDataType.STRING, "critical_cutter_sword");
        pdc.set(infiniteDurabilityKey, PersistentDataType.BYTE, (byte) 1);
        
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        // Set attack speed to 2.2 (base is 4.0, so add -1.8)
        meta.addAttributeModifier(
            org.bukkit.attribute.Attribute.ATTACK_SPEED,
            new org.bukkit.attribute.AttributeModifier(
                new NamespacedKey(plugin, "critical_cutter_attack_speed"),
                -1.8,
                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                org.bukkit.inventory.EquipmentSlotGroup.HAND
            )
        );
        
        sword.setItemMeta(meta);
        return sword;
    }
    
    private ItemStack createCriticalCutterItem(String itemId) {
        if (itemId.equals("guaranteed_critical")) {
            ItemStack item = new ItemStack(Material.BLAZE_ROD);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName("§c§l확정 크리티컬");
            List<String> lore = new ArrayList<>();
            lore.add("§7우클릭: 전방 공격");
            lore.add("§7피해량: 6 (3칸)");
            lore.add("§7가한 피해량만큼 추가 즉시 피해");
            lore.add("§7체력 6 (3칸) 회복");
            meta.setLore(lore);
            
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(classItemKey, PersistentDataType.STRING, "CRITICAL_CUTTER");
            pdc.set(itemIdKey, PersistentDataType.STRING, "guaranteed_critical");
            pdc.set(infiniteDurabilityKey, PersistentDataType.BYTE, (byte) 1);
            
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            
            item.setItemMeta(meta);
            return item;
        }
        return null;
    }
    
    private ItemStack createNavigatorSword() {
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        
        meta.setDisplayName("§3항해사의 검");
        List<String> lore = new ArrayList<>();
        lore.add("§7항해사 시작 무기");
        meta.setLore(lore);
        
        meta.addEnchant(Enchantment.KNOCKBACK, 1, true);
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "NAVIGATOR");
        pdc.set(itemIdKey, PersistentDataType.STRING, "navigator_sword");
        pdc.set(infiniteDurabilityKey, PersistentDataType.BYTE, (byte) 1);
        
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        sword.setItemMeta(meta);
        return sword;
    }
    
    private ItemStack createNavigatorItem(String itemId) {
        switch (itemId) {
            case "naval_combat":
                ItemStack navalCombat = new ItemStack(Material.WOODEN_SWORD);
                ItemMeta navalMeta = navalCombat.getItemMeta();
                
                navalMeta.setDisplayName("§3해전 무기");
                List<String> navalLore = new ArrayList<>();
                navalLore.add("§7공격: 1 기본 + 4 즉시 데미지");
                navalLore.add("§7밀치기 II");
                navalLore.add("§c내구도: 1 (한 번 사용 후 파괴)");
                navalMeta.setLore(navalLore);
                
                navalMeta.addEnchant(Enchantment.KNOCKBACK, 2, true);
                
                // Set durability to 1 (59 damage on wooden sword with 60 max durability)
                PersistentDataContainer navalPdc = navalMeta.getPersistentDataContainer();
                navalPdc.set(classItemKey, PersistentDataType.STRING, "NAVIGATOR");
                navalPdc.set(itemIdKey, PersistentDataType.STRING, "naval_combat");
                
                navalCombat.setItemMeta(navalMeta);
                
                // Set durability to 1 (wooden sword has 60 max durability, so damage = 59)
                org.bukkit.inventory.meta.Damageable damageableMeta = (org.bukkit.inventory.meta.Damageable) navalCombat.getItemMeta();
                damageableMeta.setDamage(59); // 60 - 1 = 59 damage
                navalCombat.setItemMeta(damageableMeta);
                
                return navalCombat;
                
            case "wave_riding_item":
                ItemStack waveRiding = new ItemStack(Material.FEATHER);
                ItemMeta waveMeta = waveRiding.getItemMeta();
                
                waveMeta.setDisplayName("§b파도 타기");
                List<String> waveLore = new ArrayList<>();
                waveLore.add("§7우클릭: 1.5초간 신속 IV");
                waveLore.add("§7충돌: 밀치기 + 구속 V");
                waveLore.add("§7쿨다운: 11초");
                waveMeta.setLore(waveLore);
                
                PersistentDataContainer wavePdc = waveMeta.getPersistentDataContainer();
                wavePdc.set(classItemKey, PersistentDataType.STRING, "NAVIGATOR");
                wavePdc.set(itemIdKey, PersistentDataType.STRING, "wave_riding_item");
                
                waveRiding.setItemMeta(waveMeta);
                return waveRiding;
                
            case "harpoon":
                ItemStack harpoon = new ItemStack(Material.TRIDENT);
                ItemMeta harpoonMeta = harpoon.getItemMeta();
                
                harpoonMeta.setDisplayName("§3작살");
                List<String> harpoonLore = new ArrayList<>();
                harpoonLore.add("§7우클릭: 삼치창 던지기");
                harpoonLore.add("§7적중: 6 데미지 + 구속 V (1.5초)");
                harpoonLore.add("§7빗나감: 50% 쿨다운 감소");
                harpoonLore.add("§7쿨다운: 15초");
                harpoonMeta.setLore(harpoonLore);
                
                PersistentDataContainer harpoonPdc = harpoonMeta.getPersistentDataContainer();
                harpoonPdc.set(classItemKey, PersistentDataType.STRING, "NAVIGATOR");
                harpoonPdc.set(itemIdKey, PersistentDataType.STRING, "harpoon");
                harpoonPdc.set(infiniteDurabilityKey, PersistentDataType.BYTE, (byte) 1);
                
                harpoonMeta.setUnbreakable(true);
                harpoonMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                
                harpoon.setItemMeta(harpoonMeta);
                return harpoon;
        }
        return null;
    }
    
    private ItemStack createCaptainSword() {
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        
        meta.setDisplayName("§6선장의 검");
        List<String> lore = new ArrayList<>();
        lore.add("§7선장 시작 무기");
        lore.add("§7공격 시: 랜덤 팀원에게 힘 I");
        meta.setLore(lore);
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "CAPTAIN");
        pdc.set(itemIdKey, PersistentDataType.STRING, "captain_sword");
        pdc.set(infiniteDurabilityKey, PersistentDataType.BYTE, (byte) 1);
        
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        sword.setItemMeta(meta);
        return sword;
    }
    
    private ItemStack createCaptainItem(String itemId) {
        switch (itemId) {
            case "naval_combat":
                ItemStack navalCombat = new ItemStack(Material.WOODEN_SWORD);
                ItemMeta navalMeta = navalCombat.getItemMeta();
                
                navalMeta.setDisplayName("§3해전 무기");
                List<String> navalLore = new ArrayList<>();
                navalLore.add("§7공격: 1 기본 + 4 즉시 데미지");
                navalLore.add("§7밀치기 II");
                navalLore.add("§c내구도: 1 (한 번 사용 후 파괴)");
                navalMeta.setLore(navalLore);
                
                navalMeta.addEnchant(Enchantment.KNOCKBACK, 2, true);
                
                // Set durability to 1 (59 damage on wooden sword with 60 max durability)
                PersistentDataContainer navalPdc = navalMeta.getPersistentDataContainer();
                navalPdc.set(classItemKey, PersistentDataType.STRING, "CAPTAIN");
                navalPdc.set(itemIdKey, PersistentDataType.STRING, "naval_combat");
                
                navalCombat.setItemMeta(navalMeta);
                
                // Set durability to 1 (wooden sword has 60 max durability, so damage = 59)
                org.bukkit.inventory.meta.Damageable damageableMeta = (org.bukkit.inventory.meta.Damageable) navalCombat.getItemMeta();
                damageableMeta.setDamage(59); // 60 - 1 = 59 damage
                navalCombat.setItemMeta(damageableMeta);
                
                return navalCombat;
                
            case "captains_command":
                ItemStack command = new ItemStack(Material.GOLD_INGOT);
                ItemMeta commandMeta = command.getItemMeta();
                
                commandMeta.setDisplayName("§6선장의 명령");
                List<String> commandLore = new ArrayList<>();
                commandLore.add("§7우클릭: 팀원에게 해전 무기 지급");
                commandLore.add("§7+ 5초간 힘 I");
                commandLore.add("§7쿨다운: 15초");
                commandMeta.setLore(commandLore);
                
                PersistentDataContainer commandPdc = commandMeta.getPersistentDataContainer();
                commandPdc.set(classItemKey, PersistentDataType.STRING, "CAPTAIN");
                commandPdc.set(itemIdKey, PersistentDataType.STRING, "captains_command");
                
                command.setItemMeta(commandMeta);
                return command;
                
            case "captains_harpoon":
                ItemStack harpoon = new ItemStack(Material.SPECTRAL_ARROW);
                ItemMeta harpoonMeta = harpoon.getItemMeta();
                
                harpoonMeta.setDisplayName("§6선장의 삼치창");
                List<String> harpoonLore = new ArrayList<>();
                harpoonLore.add("§7우클릭: 삼치창 던지기");
                harpoonLore.add("§7적중: 4 데미지 + 팀원 강화");
                harpoonLore.add("§7+ 대상 약화 + 쿨다운 4초 감소");
                harpoonLore.add("§7쿨다운: 12초");
                harpoonMeta.setLore(harpoonLore);
                
                PersistentDataContainer harpoonPdc = harpoonMeta.getPersistentDataContainer();
                harpoonPdc.set(classItemKey, PersistentDataType.STRING, "CAPTAIN");
                harpoonPdc.set(itemIdKey, PersistentDataType.STRING, "captains_harpoon");
                
                harpoon.setItemMeta(harpoonMeta);
                return harpoon;
        }
        return null;
    }
    
    /**
     * Get Juggler Light Thing item (Snowball)
     */
    public ItemStack getJugglerLightThing() {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§f가벼운 것");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7우클릭: 눈덩이 투척");
        lore.add("§7피해량: 1 (0.5칸)");
        lore.add("§7넉백 없음");
        lore.add("§7쿨타임: 10초");
        lore.add("§a적중 시 쿨타임 초기화");
        meta.setLore(lore);
        
        // Add NBT tags
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(classItemKey, PersistentDataType.STRING, ClassType.JUGGLER.name());
        container.set(itemIdKey, PersistentDataType.STRING, "light_thing");
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get Juggler Heavy Thing item (Wind Charge)
     */
    public ItemStack getJugglerHeavyThing() {
        ItemStack item = new ItemStack(Material.WIND_CHARGE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§8무거운 것");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7우클릭: 돌풍 투척");
        lore.add("§7피해량: 8 (4칸)");
        lore.add("§7적중 시:");
        lore.add("§7  - 구속 V (0.5초)");
        lore.add("§7  - 실명 (1초)");
        lore.add("§7쿨타임: 20초");
        meta.setLore(lore);
        
        // Add NBT tags
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(classItemKey, PersistentDataType.STRING, ClassType.JUGGLER.name());
        container.set(itemIdKey, PersistentDataType.STRING, "heavy_thing");
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get Juggler Throw Time item
     */
    public ItemStack getJugglerThrowTime() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§e투척 시간");
        
        List<String> lore = new ArrayList<>();
        lore.add("§760초마다 1개 획득 (최대 1개)");
        lore.add("§7우클릭: 10칸 내 모든 대상에게");
        lore.add("§7구속 V (2.5초) 부여");
        lore.add("§a가벼운 것 쿨타임 초기화");
        lore.add("§c사용 후 소멸");
        meta.setLore(lore);
        
        // Add NBT tags
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(classItemKey, PersistentDataType.STRING, ClassType.JUGGLER.name());
        container.set(itemIdKey, PersistentDataType.STRING, "throw_time");
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create Stamper items
     */
    private ItemStack createStamperItem(String itemId) {
        switch (itemId) {
            case "stamp":
                ItemStack stamp = new ItemStack(Material.HEAVY_CORE);
                ItemMeta stampMeta = stamp.getItemMeta();
                
                stampMeta.setDisplayName("§a도장 찍기");
                List<String> stampLore = new ArrayList<>();
                stampLore.add("§7우클릭: 10칸 위로 솟아오름");
                stampLore.add("§7쿨타임: 20초");
                stampMeta.setLore(stampLore);
                
                // Set attack damage to 0
                stampMeta.addAttributeModifier(org.bukkit.attribute.Attribute.ATTACK_DAMAGE,
                    new org.bukkit.attribute.AttributeModifier(
                        java.util.UUID.randomUUID(),
                        "generic.attack_damage",
                        0.0,
                        org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                        org.bukkit.inventory.EquipmentSlot.HAND
                    )
                );
                
                PersistentDataContainer stampPdc = stampMeta.getPersistentDataContainer();
                stampPdc.set(classItemKey, PersistentDataType.STRING, "STAMPER");
                stampPdc.set(itemIdKey, PersistentDataType.STRING, "stamp");
                
                stamp.setItemMeta(stampMeta);
                return stamp;
                
            case "dive":
                ItemStack dive = new ItemStack(Material.NETHER_STAR);
                ItemMeta diveMeta = dive.getItemMeta();
                
                diveMeta.setDisplayName("§6§l다이브");
                List<String> diveLore = new ArrayList<>();
                diveLore.add("§7200초마다 1개 획득 (최대 1개)");
                diveLore.add("§7우클릭: 40칸 위로 솟아오름");
                diveLore.add("§7착지 시: 주변 20칸 범위");
                diveLore.add("§7  - 16 (8칸) 피해");
                diveLore.add("§7  - 히트박스 겹침 시 2배");
                diveLore.add("§c사용 후 소멸");
                diveMeta.setLore(diveLore);
                
                PersistentDataContainer divePdc = diveMeta.getPersistentDataContainer();
                divePdc.set(classItemKey, PersistentDataType.STRING, "STAMPER");
                divePdc.set(itemIdKey, PersistentDataType.STRING, "dive");
                
                dive.setItemMeta(diveMeta);
                return dive;
        }
        return null;
    }
    
    /**
     * Create Time Engraver items
     */
    private ItemStack createTimeEngraverItem(String itemId) {
        switch (itemId) {
            case "time_engrave":
                ItemStack timeEngrave = new ItemStack(Material.CLOCK);
                ItemMeta engraveMeta = timeEngrave.getItemMeta();
                
                engraveMeta.setDisplayName("§e시간 각인");
                List<String> engraveLore = new ArrayList<>();
                engraveLore.add("§7우클릭: 5칸 범위 AOE");
                engraveLore.add("§7구속 V (1초)");
                engraveLore.add("§7→ 나약함 I (1.5초)");
                engraveLore.add("§7영향받은 엔티티당 시간 박제 1개");
                engraveLore.add("§7쿨타임: 16초");
                engraveMeta.setLore(engraveLore);
                
                PersistentDataContainer engravePdc = engraveMeta.getPersistentDataContainer();
                engravePdc.set(classItemKey, PersistentDataType.STRING, "TIME_ENGRAVER");
                engravePdc.set(itemIdKey, PersistentDataType.STRING, "time_engrave");
                
                timeEngrave.setItemMeta(engraveMeta);
                return timeEngrave;
                
            case "clock_needle_stitch":
                ItemStack needle = new ItemStack(Material.ARROW);
                ItemMeta needleMeta = needle.getItemMeta();
                
                needleMeta.setDisplayName("§6시곗바늘 꿰메기");
                List<String> needleLore = new ArrayList<>();
                needleLore.add("§7우클릭: 투사체 발사");
                needleLore.add("§7피해량: 6 (3칸)");
                needleLore.add("§7구속 대상: 8 즉시 피해 (4칸)");
                needleLore.add("§7적중: 어둠 + 실명 (3초)");
                needleLore.add("§7쿨타임: 12초");
                needleMeta.setLore(needleLore);
                
                PersistentDataContainer needlePdc = needleMeta.getPersistentDataContainer();
                needlePdc.set(classItemKey, PersistentDataType.STRING, "TIME_ENGRAVER");
                needlePdc.set(itemIdKey, PersistentDataType.STRING, "clock_needle_stitch");
                
                needle.setItemMeta(needleMeta);
                return needle;
                
            case "eternal_clock":
                ItemStack eternal = new ItemStack(Material.RECOVERY_COMPASS);
                ItemMeta eternalMeta = eternal.getItemMeta();
                
                eternalMeta.setDisplayName("§6§l영원한 시계");
                List<String> eternalLore = new ArrayList<>();
                eternalLore.add("§7시간 박제 7개로 획득");
                eternalLore.add("§7우클릭: 월드 전체 CC");
                eternalLore.add("§7구속 V + 어둠 + 실명 (8초)");
                eternalLore.add("§7투사체 정지");
                eternalLore.add("§78초간 시곗바늘 쿨타임 1초");
                eternalLore.add("§c사용 후 소멸");
                eternalMeta.setLore(eternalLore);
                
                PersistentDataContainer eternalPdc = eternalMeta.getPersistentDataContainer();
                eternalPdc.set(classItemKey, PersistentDataType.STRING, "TIME_ENGRAVER");
                eternalPdc.set(itemIdKey, PersistentDataType.STRING, "eternal_clock");
                
                eternal.setItemMeta(eternalMeta);
                return eternal;
        }
        return null;
    }
    
    /**
     * Create Cavalry spear (stone spear from 1.21.1)
     */
    private ItemStack createCavalrySpear() {
        // Try to use STONE_SPEAR if available, fallback to STONE_SWORD
        Material spearMaterial;
        try {
            spearMaterial = Material.valueOf("STONE_SPEAR");
        } catch (IllegalArgumentException e) {
            // STONE_SPEAR doesn't exist, use STONE_SWORD as fallback
            spearMaterial = Material.STONE_SWORD;
        }
        
        ItemStack spear = new ItemStack(spearMaterial);
        ItemMeta meta = spear.getItemMeta();
        
        meta.setDisplayName("§6기마병의 돌창");
        List<String> lore = new ArrayList<>();
        lore.add("§7기마병 시작 무기");
        meta.setLore(lore);
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classItemKey, PersistentDataType.STRING, "CAVALRY");
        pdc.set(itemIdKey, PersistentDataType.STRING, "cavalry_spear");
        pdc.set(infiniteDurabilityKey, PersistentDataType.BYTE, (byte) 1);
        
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        spear.setItemMeta(meta);
        return spear;
    }
    
    /**
     * Create Cavalry items
     */
    private ItemStack createCavalryItem(String itemId) {
        switch (itemId) {
            case "mount_dismount":
                ItemStack mount = new ItemStack(Material.SADDLE);
                ItemMeta mountMeta = mount.getItemMeta();
                
                mountMeta.setDisplayName("§6승마&하마");
                List<String> mountLore = new ArrayList<>();
                mountLore.add("§7우클릭: 말 소환 및 탑승");
                mountLore.add("§7말: 체력 공유");
                mountLore.add("§7말: 피해의 절반을 탑승자에게 전달");
                mountLore.add("§7탑승 중 사용: 하마 + 체력 6 회복");
                mountLore.add("§7쿨타임: 22초");
                mountMeta.setLore(mountLore);
                
                PersistentDataContainer mountPdc = mountMeta.getPersistentDataContainer();
                mountPdc.set(classItemKey, PersistentDataType.STRING, "CAVALRY");
                mountPdc.set(itemIdKey, PersistentDataType.STRING, "mount_dismount");
                
                mount.setItemMeta(mountMeta);
                return mount;
                
            case "sweep":
                ItemStack sweep = new ItemStack(Material.IRON_SWORD);
                ItemMeta sweepMeta = sweep.getItemMeta();
                
                sweepMeta.setDisplayName("§c휩쓸기");
                List<String> sweepLore = new ArrayList<>();
                sweepLore.add("§7우클릭: 전방 3칸 범위 공격");
                sweepLore.add("§7피해량: 6 (3칸)");
                sweepLore.add("§7기마 시: 8 피해 (4칸)");
                sweepLore.add("§7쿨타임: 6초");
                sweepLore.add("§a적중 1명당 쿨타임 1초 감소");
                sweepMeta.setLore(sweepLore);
                
                // Set attack damage to 0 to prevent normal attacks
                sweepMeta.addAttributeModifier(org.bukkit.attribute.Attribute.ATTACK_DAMAGE,
                    new org.bukkit.attribute.AttributeModifier(
                        java.util.UUID.randomUUID(),
                        "generic.attack_damage",
                        0.0,
                        org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                        org.bukkit.inventory.EquipmentSlot.HAND
                    )
                );
                
                PersistentDataContainer sweepPdc = sweepMeta.getPersistentDataContainer();
                sweepPdc.set(classItemKey, PersistentDataType.STRING, "CAVALRY");
                sweepPdc.set(itemIdKey, PersistentDataType.STRING, "sweep");
                
                sweepMeta.setUnbreakable(true);
                sweepMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                sweepMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                
                sweep.setItemMeta(sweepMeta);
                return sweep;
        }
        return null;
    }
    
    /**
     * Create Marathoner items
     */
    private ItemStack createMarathonerItem(String itemId) {
        switch (itemId) {
            case "crouching_start":
                ItemStack crouch = new ItemStack(Material.FEATHER);
                ItemMeta crouchMeta = crouch.getItemMeta();
                
                crouchMeta.setDisplayName("§b크라우칭 스타트");
                List<String> crouchLore = new ArrayList<>();
                crouchLore.add("§7우클릭: 0.5초간 신속 III");
                crouchLore.add("§7쿨타임: 5초");
                crouchMeta.setLore(crouchLore);
                
                PersistentDataContainer crouchPdc = crouchMeta.getPersistentDataContainer();
                crouchPdc.set(classItemKey, PersistentDataType.STRING, "MARATHONER");
                crouchPdc.set(itemIdKey, PersistentDataType.STRING, "crouching_start");
                
                crouch.setItemMeta(crouchMeta);
                return crouch;
        }
        return null;
    }
}

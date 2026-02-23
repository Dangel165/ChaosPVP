package com.verminpvp.managers;

import com.verminpvp.models.ClassType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ItemProvider
 */
class ItemProviderTest {
    
    @Mock
    private Plugin mockPlugin;
    
    @Mock
    private ItemMeta mockMeta;
    
    @Mock
    private PersistentDataContainer mockPdc;
    
    private ItemProvider itemProvider;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock NamespacedKey creation
        when(mockPlugin.getName()).thenReturn("ChaosPVP");
        
        itemProvider = new ItemProvider(mockPlugin);
    }
    
    @Test
    void testCreateClassWeapon_Swordsman() {
        ItemStack weapon = itemProvider.createClassWeapon(ClassType.SWORDSMAN);
        
        assertNotNull(weapon);
        assertEquals(Material.STONE_SWORD, weapon.getType());
        assertTrue(weapon.hasItemMeta());
        
        ItemMeta meta = weapon.getItemMeta();
        assertNotNull(meta.getDisplayName());
        assertTrue(meta.getDisplayName().contains("Swordsman"));
        assertTrue(meta.isUnbreakable());
    }
    
    @Test
    void testCreateClassWeapon_Scientist() {
        ItemStack weapon = itemProvider.createClassWeapon(ClassType.SCIENTIST);
        
        // Scientist has no starting weapon
        assertNull(weapon);
    }
    
    @Test
    void testCreateClassWeapon_PlagueSpreader() {
        ItemStack weapon = itemProvider.createClassWeapon(ClassType.PLAGUE_SPREADER);
        
        assertNotNull(weapon);
        assertEquals(Material.STONE_SWORD, weapon.getType());
        assertTrue(weapon.hasItemMeta());
        
        ItemMeta meta = weapon.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Plague"));
        assertTrue(meta.isUnbreakable());
    }
    
    @Test
    void testCreateClassWeapon_ShieldSoldier() {
        ItemStack weapon = itemProvider.createClassWeapon(ClassType.SHIELD_SOLDIER);
        
        // Shield Soldier has no starting sword
        assertNull(weapon);
    }
    
    @Test
    void testCreateClassWeapon_CriticalCutter() {
        ItemStack weapon = itemProvider.createClassWeapon(ClassType.CRITICAL_CUTTER);
        
        assertNotNull(weapon);
        assertEquals(Material.STONE_SWORD, weapon.getType());
        assertTrue(weapon.hasItemMeta());
        
        ItemMeta meta = weapon.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Critical"));
        assertTrue(meta.isUnbreakable());
    }
    
    @Test
    void testCreateClassWeapon_Navigator() {
        ItemStack weapon = itemProvider.createClassWeapon(ClassType.NAVIGATOR);
        
        assertNotNull(weapon);
        assertEquals(Material.STONE_SWORD, weapon.getType());
        assertTrue(weapon.hasItemMeta());
        
        ItemMeta meta = weapon.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Navigator"));
        assertTrue(meta.hasEnchant(Enchantment.KNOCKBACK));
        assertEquals(1, meta.getEnchantLevel(Enchantment.KNOCKBACK));
        assertTrue(meta.isUnbreakable());
    }
    
    @Test
    void testCreateClassWeapon_Captain() {
        ItemStack weapon = itemProvider.createClassWeapon(ClassType.CAPTAIN);
        
        assertNotNull(weapon);
        assertEquals(Material.STONE_SWORD, weapon.getType());
        assertTrue(weapon.hasItemMeta());
        
        ItemMeta meta = weapon.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Captain"));
        assertTrue(meta.isUnbreakable());
    }
    
    @Test
    void testCreateClassWeapon_NullClassType() {
        assertThrows(IllegalArgumentException.class, () -> {
            itemProvider.createClassWeapon(null);
        });
    }
    
    @Test
    void testCreateSpecialItem_SwordsmanDiamondSword() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.SWORDSMAN, "diamond_sword");
        
        assertNotNull(item);
        assertEquals(Material.DIAMOND_SWORD, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Diamond"));
        assertTrue(meta.isUnbreakable());
    }
    
    @Test
    void testCreateSpecialItem_ScientistPotion() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.SCIENTIST, "healing");
        
        assertNotNull(item);
        assertEquals(Material.SPLASH_POTION, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Scientist Potion"));
    }
    
    @Test
    void testCreateSpecialItem_PlagueVirulentEngine() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.PLAGUE_SPREADER, "virulent");
        
        assertNotNull(item);
        assertEquals(Material.FERMENTED_SPIDER_EYE, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Virulent"));
    }
    
    @Test
    void testCreateSpecialItem_PlagueExtremeEngine() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.PLAGUE_SPREADER, "extreme");
        
        assertNotNull(item);
        assertEquals(Material.SPIDER_EYE, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Extreme"));
    }
    
    @Test
    void testCreateSpecialItem_ShieldRedShield() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "red_shield");
        
        assertNotNull(item);
        assertEquals(Material.SHIELD, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Red"));
        assertTrue(meta.isUnbreakable());
    }
    
    @Test
    void testCreateSpecialItem_ShieldBlueShield() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "blue_shield");
        
        assertNotNull(item);
        assertEquals(Material.SHIELD, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Blue"));
        assertTrue(meta.isUnbreakable());
    }
    
    @Test
    void testCreateSpecialItem_ShieldBasicShield() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "basic_shield");
        
        assertNotNull(item);
        assertEquals(Material.SHIELD, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Basic"));
        assertTrue(meta.isUnbreakable());
    }
    
    @Test
    void testCreateSpecialItem_ShieldGoldenShield() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "golden_shield");
        
        assertNotNull(item);
        assertEquals(Material.SHIELD, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Golden"));
        assertTrue(meta.isUnbreakable());
    }
    
    @Test
    void testCreateSpecialItem_NavigatorNavalCombat() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.NAVIGATOR, "naval_combat");
        
        assertNotNull(item);
        assertEquals(Material.WOODEN_SWORD, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Naval Combat"));
        assertTrue(meta.hasEnchant(Enchantment.KNOCKBACK));
        assertEquals(3, meta.getEnchantLevel(Enchantment.KNOCKBACK));
    }
    
    @Test
    void testCreateSpecialItem_CaptainNavalCombat() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.CAPTAIN, "naval_combat");
        
        assertNotNull(item);
        assertEquals(Material.WOODEN_SWORD, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Naval Combat"));
        assertTrue(meta.hasEnchant(Enchantment.KNOCKBACK));
        assertEquals(3, meta.getEnchantLevel(Enchantment.KNOCKBACK));
    }
    
    @Test
    void testCreateSpecialItem_NullArguments() {
        assertThrows(IllegalArgumentException.class, () -> {
            itemProvider.createSpecialItem(null, "test");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            itemProvider.createSpecialItem(ClassType.SWORDSMAN, null);
        });
    }
    
    @Test
    void testCreateUltimateItem_SwordsmanNetheriteSword() {
        ItemStack item = itemProvider.createUltimateItem(ClassType.SWORDSMAN);
        
        assertNotNull(item);
        assertEquals(Material.NETHERITE_SWORD, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Ultimate"));
        assertTrue(meta.getDisplayName().contains("Netherite"));
    }
    
    @Test
    void testCreateUltimateItem_ScientistUltimatePotion() {
        ItemStack item = itemProvider.createUltimateItem(ClassType.SCIENTIST);
        
        assertNotNull(item);
        assertEquals(Material.LINGERING_POTION, item.getType());
        assertTrue(item.hasItemMeta());
        
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.getDisplayName().contains("Ultimate"));
    }
    
    @Test
    void testCreateUltimateItem_OtherClasses() {
        // Other classes don't have ultimate items
        assertNull(itemProvider.createUltimateItem(ClassType.PLAGUE_SPREADER));
        assertNull(itemProvider.createUltimateItem(ClassType.SHIELD_SOLDIER));
        assertNull(itemProvider.createUltimateItem(ClassType.CRITICAL_CUTTER));
        assertNull(itemProvider.createUltimateItem(ClassType.NAVIGATOR));
        assertNull(itemProvider.createUltimateItem(ClassType.CAPTAIN));
    }
    
    @Test
    void testCreateUltimateItem_NullClassType() {
        assertThrows(IllegalArgumentException.class, () -> {
            itemProvider.createUltimateItem(null);
        });
    }
    
    @Test
    void testIsClassItem_WithClassItem() {
        ItemStack item = itemProvider.createClassWeapon(ClassType.SWORDSMAN);
        
        assertTrue(itemProvider.isClassItem(item));
    }
    
    @Test
    void testIsClassItem_WithNonClassItem() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        
        assertFalse(itemProvider.isClassItem(item));
    }
    
    @Test
    void testIsClassItem_WithNullItem() {
        assertFalse(itemProvider.isClassItem(null));
    }
    
    @Test
    void testIsClassItem_WithItemWithoutMeta() {
        ItemStack item = new ItemStack(Material.STONE);
        
        assertFalse(itemProvider.isClassItem(item));
    }
    
    @Test
    void testGetItemId_WithClassItem() {
        ItemStack item = itemProvider.createClassWeapon(ClassType.SWORDSMAN);
        
        String itemId = itemProvider.getItemId(item);
        assertNotNull(itemId);
        assertEquals("swordsman_sword", itemId);
    }
    
    @Test
    void testGetItemId_WithSpecialItem() {
        ItemStack item = itemProvider.createSpecialItem(ClassType.SWORDSMAN, "diamond_sword");
        
        String itemId = itemProvider.getItemId(item);
        assertNotNull(itemId);
        assertEquals("diamond_sword", itemId);
    }
    
    @Test
    void testGetItemId_WithNonClassItem() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        
        assertNull(itemProvider.getItemId(item));
    }
    
    @Test
    void testGetItemId_WithNullItem() {
        assertNull(itemProvider.getItemId(null));
    }
    
    @Test
    void testNavalCombatConsistency() {
        // Naval Combat items from Navigator and Captain should be identical
        ItemStack navigatorNaval = itemProvider.createSpecialItem(ClassType.NAVIGATOR, "naval_combat");
        ItemStack captainNaval = itemProvider.createSpecialItem(ClassType.CAPTAIN, "naval_combat");
        
        assertNotNull(navigatorNaval);
        assertNotNull(captainNaval);
        
        // Check material
        assertEquals(navigatorNaval.getType(), captainNaval.getType());
        
        // Check enchantments
        ItemMeta navMeta = navigatorNaval.getItemMeta();
        ItemMeta capMeta = captainNaval.getItemMeta();
        
        assertTrue(navMeta.hasEnchant(Enchantment.KNOCKBACK));
        assertTrue(capMeta.hasEnchant(Enchantment.KNOCKBACK));
        assertEquals(navMeta.getEnchantLevel(Enchantment.KNOCKBACK), 
                     capMeta.getEnchantLevel(Enchantment.KNOCKBACK));
    }
    
    @Test
    void testInfiniteDurabilityMarking() {
        // Test that infinite durability items are marked correctly
        ItemStack swordsmanSword = itemProvider.createClassWeapon(ClassType.SWORDSMAN);
        ItemStack diamondSword = itemProvider.createSpecialItem(ClassType.SWORDSMAN, "diamond_sword");
        ItemStack netheriteSword = itemProvider.createUltimateItem(ClassType.SWORDSMAN);
        
        // Swordsman sword and special swords should be unbreakable
        assertTrue(swordsmanSword.getItemMeta().isUnbreakable());
        assertTrue(diamondSword.getItemMeta().isUnbreakable());
        
        // Netherite ultimate sword should NOT be unbreakable (durability 1)
        assertFalse(netheriteSword.getItemMeta().isUnbreakable());
    }
}

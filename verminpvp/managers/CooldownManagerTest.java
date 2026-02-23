package com.verminpvp.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CooldownManager Tests")
class CooldownManagerTest {
    
    private CooldownManager cooldownManager;
    private UUID testPlayer;
    
    @BeforeEach
    void setUp() {
        cooldownManager = new CooldownManager();
        testPlayer = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("Should not be on cooldown initially")
    void testNoCooldownInitially() {
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "test_ability"));
    }
    
    @Test
    @DisplayName("Should set cooldown correctly")
    void testSetCooldown() {
        cooldownManager.setCooldown(testPlayer, "test_ability", 5.0);
        assertTrue(cooldownManager.isOnCooldown(testPlayer, "test_ability"));
    }
    
    @Test
    @DisplayName("Should get remaining cooldown time")
    void testGetRemainingCooldown() {
        cooldownManager.setCooldown(testPlayer, "test_ability", 5.0);
        double remaining = cooldownManager.getRemainingCooldown(testPlayer, "test_ability");
        
        assertTrue(remaining > 4.9 && remaining <= 5.0, 
                   "Remaining cooldown should be close to 5 seconds");
    }
    
    @Test
    @DisplayName("Should expire cooldown after duration")
    void testCooldownExpiration() throws InterruptedException {
        cooldownManager.setCooldown(testPlayer, "test_ability", 0.1);
        assertTrue(cooldownManager.isOnCooldown(testPlayer, "test_ability"));
        
        Thread.sleep(150);
        
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "test_ability"));
    }
    
    @Test
    @DisplayName("Should reduce cooldown by percentage")
    void testReduceCooldown() {
        cooldownManager.setCooldown(testPlayer, "test_ability", 10.0);
        cooldownManager.reduceCooldown(testPlayer, "test_ability", 0.5); // 50% reduction
        
        double remaining = cooldownManager.getRemainingCooldown(testPlayer, "test_ability");
        assertTrue(remaining > 4.9 && remaining <= 5.1, 
                   "Remaining cooldown should be approximately 5 seconds after 50% reduction");
    }
    
    @Test
    @DisplayName("Should clear specific cooldown")
    void testClearCooldown() {
        cooldownManager.setCooldown(testPlayer, "test_ability", 5.0);
        assertTrue(cooldownManager.isOnCooldown(testPlayer, "test_ability"));
        
        cooldownManager.clearCooldown(testPlayer, "test_ability");
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "test_ability"));
    }
    
    @Test
    @DisplayName("Should clear all cooldowns for player")
    void testClearAllCooldowns() {
        cooldownManager.setCooldown(testPlayer, "ability1", 5.0);
        cooldownManager.setCooldown(testPlayer, "ability2", 10.0);
        
        assertTrue(cooldownManager.isOnCooldown(testPlayer, "ability1"));
        assertTrue(cooldownManager.isOnCooldown(testPlayer, "ability2"));
        
        cooldownManager.clearCooldowns(testPlayer);
        
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "ability1"));
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "ability2"));
    }
    
    @Test
    @DisplayName("Should handle multiple abilities per player")
    void testMultipleAbilities() {
        cooldownManager.setCooldown(testPlayer, "ability1", 5.0);
        cooldownManager.setCooldown(testPlayer, "ability2", 10.0);
        cooldownManager.setCooldown(testPlayer, "ability3", 15.0);
        
        assertTrue(cooldownManager.isOnCooldown(testPlayer, "ability1"));
        assertTrue(cooldownManager.isOnCooldown(testPlayer, "ability2"));
        assertTrue(cooldownManager.isOnCooldown(testPlayer, "ability3"));
        
        Map<String, Double> activeCooldowns = cooldownManager.getActiveCooldowns(testPlayer);
        assertEquals(3, activeCooldowns.size());
    }
    
    @Test
    @DisplayName("Should handle multiple players independently")
    void testMultiplePlayers() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        cooldownManager.setCooldown(player1, "ability1", 5.0);
        cooldownManager.setCooldown(player2, "ability1", 10.0);
        
        assertTrue(cooldownManager.isOnCooldown(player1, "ability1"));
        assertTrue(cooldownManager.isOnCooldown(player2, "ability1"));
        
        double remaining1 = cooldownManager.getRemainingCooldown(player1, "ability1");
        double remaining2 = cooldownManager.getRemainingCooldown(player2, "ability1");
        
        assertTrue(remaining1 < remaining2, "Player 1 should have less cooldown than Player 2");
    }
    
    @Test
    @DisplayName("Should get active cooldowns map")
    void testGetActiveCooldowns() {
        cooldownManager.setCooldown(testPlayer, "ability1", 5.0);
        cooldownManager.setCooldown(testPlayer, "ability2", 10.0);
        
        Map<String, Double> activeCooldowns = cooldownManager.getActiveCooldowns(testPlayer);
        
        assertEquals(2, activeCooldowns.size());
        assertTrue(activeCooldowns.containsKey("ability1"));
        assertTrue(activeCooldowns.containsKey("ability2"));
        assertTrue(activeCooldowns.get("ability1") <= 5.0);
        assertTrue(activeCooldowns.get("ability2") <= 10.0);
    }
    
    @Test
    @DisplayName("Should remove player from cooldown system")
    void testRemovePlayer() {
        cooldownManager.setCooldown(testPlayer, "ability1", 5.0);
        cooldownManager.setCooldown(testPlayer, "ability2", 10.0);
        
        cooldownManager.removePlayer(testPlayer);
        
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "ability1"));
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "ability2"));
        assertTrue(cooldownManager.getActiveCooldowns(testPlayer).isEmpty());
    }
    
    @Test
    @DisplayName("Should handle null player UUID gracefully")
    void testNullPlayerUuid() {
        assertFalse(cooldownManager.isOnCooldown(null, "ability"));
        assertEquals(0.0, cooldownManager.getRemainingCooldown(null, "ability"));
        
        cooldownManager.setCooldown(null, "ability", 5.0);
        cooldownManager.clearCooldown(null, "ability");
        cooldownManager.clearCooldowns(null);
        cooldownManager.removePlayer(null);
        
        assertTrue(cooldownManager.getActiveCooldowns(null).isEmpty());
    }
    
    @Test
    @DisplayName("Should handle null ability ID gracefully")
    void testNullAbilityId() {
        assertFalse(cooldownManager.isOnCooldown(testPlayer, null));
        assertEquals(0.0, cooldownManager.getRemainingCooldown(testPlayer, null));
        
        cooldownManager.setCooldown(testPlayer, null, 5.0);
        cooldownManager.clearCooldown(testPlayer, null);
    }
    
    @Test
    @DisplayName("Should handle negative cooldown duration")
    void testNegativeCooldownDuration() {
        cooldownManager.setCooldown(testPlayer, "ability", -5.0);
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "ability"));
    }
    
    @Test
    @DisplayName("Should handle zero cooldown duration")
    void testZeroCooldownDuration() {
        cooldownManager.setCooldown(testPlayer, "ability", 0.0);
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "ability"));
    }
    
    @Test
    @DisplayName("Should handle negative reduction percentage")
    void testNegativeReductionPercentage() {
        cooldownManager.setCooldown(testPlayer, "ability", 10.0);
        double beforeReduction = cooldownManager.getRemainingCooldown(testPlayer, "ability");
        
        cooldownManager.reduceCooldown(testPlayer, "ability", -0.5);
        double afterReduction = cooldownManager.getRemainingCooldown(testPlayer, "ability");
        
        assertEquals(beforeReduction, afterReduction, 0.1, 
                     "Negative reduction should not change cooldown");
    }
    
    @Test
    @DisplayName("Should remove cooldown when reduction exceeds remaining time")
    void testExcessiveReduction() {
        cooldownManager.setCooldown(testPlayer, "ability", 10.0);
        cooldownManager.reduceCooldown(testPlayer, "ability", 1.0); // 100% reduction
        
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "ability"));
    }
    
    @Test
    @DisplayName("Should handle reduction on non-existent cooldown")
    void testReductionOnNonExistentCooldown() {
        cooldownManager.reduceCooldown(testPlayer, "ability", 0.5);
        assertFalse(cooldownManager.isOnCooldown(testPlayer, "ability"));
    }
    
    @Test
    @DisplayName("Should return zero for remaining cooldown on non-existent ability")
    void testRemainingCooldownNonExistent() {
        assertEquals(0.0, cooldownManager.getRemainingCooldown(testPlayer, "non_existent"));
    }
    
    @Test
    @DisplayName("Should return empty map for player with no cooldowns")
    void testActiveCooldownsEmpty() {
        Map<String, Double> activeCooldowns = cooldownManager.getActiveCooldowns(testPlayer);
        assertNotNull(activeCooldowns);
        assertTrue(activeCooldowns.isEmpty());
    }
}

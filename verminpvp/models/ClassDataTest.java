package com.verminpvp.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClassData
 */
class ClassDataTest {
    
    private ClassData classData;
    private UUID playerId;
    
    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        classData = new ClassData(playerId, ClassType.SWORDSMAN);
    }
    
    @Test
    void testInitialization() {
        assertEquals(playerId, classData.getPlayerId());
        assertEquals(ClassType.SWORDSMAN, classData.getClassType());
    }
    
    @Test
    void testResourceInitialization() {
        assertEquals(0, classData.getResourceCount(ResourceType.PRISMARINE_SHARD));
        assertEquals(0, classData.getResourceCount(ResourceType.ULTIMATE_INGREDIENT));
    }
    
    @Test
    void testAddResource() {
        classData.addResource(ResourceType.PRISMARINE_SHARD, 1);
        assertEquals(1, classData.getResourceCount(ResourceType.PRISMARINE_SHARD));
        
        classData.addResource(ResourceType.PRISMARINE_SHARD, 2);
        assertEquals(3, classData.getResourceCount(ResourceType.PRISMARINE_SHARD));
    }
    
    @Test
    void testSetResourceCount() {
        classData.setResourceCount(ResourceType.ULTIMATE_INGREDIENT, 5);
        assertEquals(5, classData.getResourceCount(ResourceType.ULTIMATE_INGREDIENT));
        
        classData.setResourceCount(ResourceType.ULTIMATE_INGREDIENT, 0);
        assertEquals(0, classData.getResourceCount(ResourceType.ULTIMATE_INGREDIENT));
    }
    
    @Test
    void testCritChanceDefault() {
        assertEquals(5.0, classData.getCritChance());
    }
    
    @Test
    void testSetCritChance() {
        classData.setCritChance(10.5);
        assertEquals(10.5, classData.getCritChance());
        
        classData.setCritChance(100.0);
        assertEquals(100.0, classData.getCritChance());
    }
    
    @Test
    void testLastDamageTimeDefault() {
        assertEquals(0L, classData.getLastDamageTime());
    }
    
    @Test
    void testSetLastDamageTime() {
        long currentTime = System.currentTimeMillis();
        classData.setLastDamageTime(currentTime);
        assertEquals(currentTime, classData.getLastDamageTime());
    }
    
    @Test
    void testCustomData() {
        assertNull(classData.getCustomData("test_key"));
        
        classData.setCustomData("test_key", "test_value");
        assertEquals("test_value", classData.getCustomData("test_key"));
        
        classData.setCustomData("number_key", 42);
        assertEquals(42, classData.getCustomData("number_key"));
    }
    
    @Test
    void testClear() {
        // Set up some data
        classData.addResource(ResourceType.PRISMARINE_SHARD, 3);
        classData.setCritChance(15.0);
        classData.setCustomData("test", "value");
        classData.getCooldowns().put("test_cooldown", System.currentTimeMillis());
        
        // Clear all data
        classData.clear();
        
        // Verify everything is cleared
        assertEquals(0, classData.getResourceCount(ResourceType.PRISMARINE_SHARD));
        assertEquals(5.0, classData.getCritChance()); // Back to default
        assertNull(classData.getCustomData("test"));
        assertTrue(classData.getCooldowns().isEmpty());
    }
    
    @Test
    void testSetClassType() {
        classData.setClassType(ClassType.SCIENTIST);
        assertEquals(ClassType.SCIENTIST, classData.getClassType());
    }
}

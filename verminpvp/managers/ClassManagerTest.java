package com.verminpvp.managers;

import com.verminpvp.models.ClassData;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.ResourceType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ClassManager
 */
class ClassManagerTest {
    
    private ClassManager classManager;
    private Player mockPlayer;
    private UUID playerId;
    
    @BeforeEach
    void setUp() {
        classManager = new ClassManager();
        mockPlayer = Mockito.mock(Player.class);
        playerId = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
    }
    
    @Test
    void testSetPlayerClass() {
        classManager.setPlayerClass(mockPlayer, ClassType.SWORDSMAN);
        
        assertEquals(ClassType.SWORDSMAN, classManager.getPlayerClass(mockPlayer));
        assertTrue(classManager.hasClass(mockPlayer));
    }
    
    @Test
    void testSetPlayerClassCreatesClassData() {
        classManager.setPlayerClass(mockPlayer, ClassType.SCIENTIST);
        
        ClassData data = classManager.getClassData(mockPlayer);
        assertNotNull(data);
        assertEquals(playerId, data.getPlayerId());
        assertEquals(ClassType.SCIENTIST, data.getClassType());
    }
    
    @Test
    void testSetPlayerClassWithNullPlayerThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            classManager.setPlayerClass(null, ClassType.SWORDSMAN);
        });
    }
    
    @Test
    void testSetPlayerClassWithNullClassTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            classManager.setPlayerClass(mockPlayer, null);
        });
    }
    
    @Test
    void testGetPlayerClassReturnsNullWhenNotSet() {
        assertNull(classManager.getPlayerClass(mockPlayer));
        assertFalse(classManager.hasClass(mockPlayer));
    }
    
    @Test
    void testGetPlayerClassWithNullPlayer() {
        assertNull(classManager.getPlayerClass(null));
    }
    
    @Test
    void testClearPlayerClass() {
        // Set up a player with class and data
        classManager.setPlayerClass(mockPlayer, ClassType.SWORDSMAN);
        ClassData data = classManager.getClassData(mockPlayer);
        data.addResource(ResourceType.PRISMARINE_SHARD, 2);
        data.setCritChance(10.0);
        
        // Clear the class
        classManager.clearPlayerClass(mockPlayer);
        
        // Verify class is removed
        assertNull(classManager.getPlayerClass(mockPlayer));
        assertFalse(classManager.hasClass(mockPlayer));
        
        // Verify data is cleared (but still exists)
        ClassData clearedData = classManager.getClassData(mockPlayer);
        assertNotNull(clearedData); // Data object still exists
        assertEquals(0, clearedData.getResourceCount(ResourceType.PRISMARINE_SHARD));
        assertEquals(5.0, clearedData.getCritChance()); // Back to default
    }
    
    @Test
    void testClearPlayerClassWithNullPlayer() {
        // Should not throw exception
        assertDoesNotThrow(() -> classManager.clearPlayerClass(null));
    }
    
    @Test
    void testGetClassDataReturnsNullWhenNotSet() {
        assertNull(classManager.getClassData(mockPlayer));
    }
    
    @Test
    void testGetClassDataWithNullPlayer() {
        assertNull(classManager.getClassData(null));
    }
    
    @Test
    void testSwitchClass() {
        // Set initial class
        classManager.setPlayerClass(mockPlayer, ClassType.SWORDSMAN);
        ClassData data = classManager.getClassData(mockPlayer);
        data.addResource(ResourceType.PRISMARINE_SHARD, 3);
        
        // Switch to different class
        classManager.setPlayerClass(mockPlayer, ClassType.SCIENTIST);
        
        // Verify class changed
        assertEquals(ClassType.SCIENTIST, classManager.getPlayerClass(mockPlayer));
        
        // Verify data object is reused but class type updated
        ClassData updatedData = classManager.getClassData(mockPlayer);
        assertSame(data, updatedData); // Same object
        assertEquals(ClassType.SCIENTIST, updatedData.getClassType());
        
        // Note: Resources are NOT automatically cleared when switching via setPlayerClass
        // That's the responsibility of the caller to call clearPlayerClass first
        assertEquals(3, updatedData.getResourceCount(ResourceType.PRISMARINE_SHARD));
    }
    
    @Test
    void testRemovePlayer() {
        // Set up player with class
        classManager.setPlayerClass(mockPlayer, ClassType.CAPTAIN);
        assertTrue(classManager.hasClass(mockPlayer));
        
        // Remove player
        classManager.removePlayer(playerId);
        
        // Verify all data is removed
        assertNull(classManager.getPlayerClass(mockPlayer));
        assertNull(classManager.getClassData(mockPlayer));
        assertFalse(classManager.hasClass(mockPlayer));
    }
    
    @Test
    void testHasClassWithNullPlayer() {
        assertFalse(classManager.hasClass(null));
    }
    
    @Test
    void testMultiplePlayers() {
        // Create multiple players
        Player player1 = Mockito.mock(Player.class);
        Player player2 = Mockito.mock(Player.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(player1.getUniqueId()).thenReturn(id1);
        when(player2.getUniqueId()).thenReturn(id2);
        
        // Set different classes
        classManager.setPlayerClass(player1, ClassType.SWORDSMAN);
        classManager.setPlayerClass(player2, ClassType.SCIENTIST);
        
        // Verify each player has correct class
        assertEquals(ClassType.SWORDSMAN, classManager.getPlayerClass(player1));
        assertEquals(ClassType.SCIENTIST, classManager.getPlayerClass(player2));
        
        // Verify class data is separate
        ClassData data1 = classManager.getClassData(player1);
        ClassData data2 = classManager.getClassData(player2);
        assertNotSame(data1, data2);
        assertEquals(id1, data1.getPlayerId());
        assertEquals(id2, data2.getPlayerId());
    }
}

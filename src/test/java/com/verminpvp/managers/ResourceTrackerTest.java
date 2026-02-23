package com.verminpvp.managers;

import com.verminpvp.models.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResourceTracker Tests")
class ResourceTrackerTest {
    
    private ResourceTracker resourceTracker;
    private UUID testPlayer;
    
    @BeforeEach
    void setUp() {
        resourceTracker = new ResourceTracker();
        testPlayer = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("Should start with zero resources")
    void testInitialResourceCount() {
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.ULTIMATE_INGREDIENT));
    }
    
    @Test
    @DisplayName("Should add resources correctly")
    void testAddResource() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 1);
        assertEquals(1, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
        
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 2);
        assertEquals(3, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
    }
    
    @Test
    @DisplayName("Should track multiple resource types independently")
    void testMultipleResourceTypes() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 3);
        resourceTracker.addResource(testPlayer, ResourceType.ULTIMATE_INGREDIENT, 5);
        
        assertEquals(3, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
        assertEquals(5, resourceTracker.getResourceCount(testPlayer, ResourceType.ULTIMATE_INGREDIENT));
    }
    
    @Test
    @DisplayName("Should check if player has enough resources")
    void testHasEnoughResources() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 3);
        
        assertTrue(resourceTracker.hasEnoughResources(testPlayer, ResourceType.PRISMARINE_SHARD, 3));
        assertTrue(resourceTracker.hasEnoughResources(testPlayer, ResourceType.PRISMARINE_SHARD, 2));
        assertFalse(resourceTracker.hasEnoughResources(testPlayer, ResourceType.PRISMARINE_SHARD, 4));
    }
    
    @Test
    @DisplayName("Should consume resources when enough available")
    void testConsumeResources() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 5);
        
        assertTrue(resourceTracker.consumeResources(testPlayer, ResourceType.PRISMARINE_SHARD, 3));
        assertEquals(2, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
    }
    
    @Test
    @DisplayName("Should not consume resources when not enough available")
    void testConsumeResourcesInsufficient() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 2);
        
        assertFalse(resourceTracker.consumeResources(testPlayer, ResourceType.PRISMARINE_SHARD, 3));
        assertEquals(2, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
    }
    
    @Test
    @DisplayName("Should remove resource entry when consumed to zero")
    void testConsumeToZero() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 3);
        
        assertTrue(resourceTracker.consumeResources(testPlayer, ResourceType.PRISMARINE_SHARD, 3));
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
    }
    
    @Test
    @DisplayName("Should set resource count directly")
    void testSetResourceCount() {
        resourceTracker.setResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD, 10);
        assertEquals(10, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
        
        resourceTracker.setResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD, 5);
        assertEquals(5, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
    }
    
    @Test
    @DisplayName("Should clear specific resource type")
    void testClearResource() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 5);
        resourceTracker.addResource(testPlayer, ResourceType.ULTIMATE_INGREDIENT, 3);
        
        resourceTracker.clearResource(testPlayer, ResourceType.PRISMARINE_SHARD);
        
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
        assertEquals(3, resourceTracker.getResourceCount(testPlayer, ResourceType.ULTIMATE_INGREDIENT));
    }
    
    @Test
    @DisplayName("Should clear all resources for player")
    void testClearAllResources() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 5);
        resourceTracker.addResource(testPlayer, ResourceType.ULTIMATE_INGREDIENT, 3);
        
        resourceTracker.clearResources(testPlayer);
        
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.ULTIMATE_INGREDIENT));
    }
    
    @Test
    @DisplayName("Should get all resources for player")
    void testGetAllResources() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 3);
        resourceTracker.addResource(testPlayer, ResourceType.ULTIMATE_INGREDIENT, 7);
        
        Map<ResourceType, Integer> allResources = resourceTracker.getAllResources(testPlayer);
        
        assertEquals(2, allResources.size());
        assertEquals(3, allResources.get(ResourceType.PRISMARINE_SHARD));
        assertEquals(7, allResources.get(ResourceType.ULTIMATE_INGREDIENT));
    }
    
    @Test
    @DisplayName("Should track resources for multiple players independently")
    void testMultiplePlayers() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        resourceTracker.addResource(player1, ResourceType.PRISMARINE_SHARD, 3);
        resourceTracker.addResource(player2, ResourceType.PRISMARINE_SHARD, 5);
        
        assertEquals(3, resourceTracker.getResourceCount(player1, ResourceType.PRISMARINE_SHARD));
        assertEquals(5, resourceTracker.getResourceCount(player2, ResourceType.PRISMARINE_SHARD));
    }
    
    @Test
    @DisplayName("Should remove player from tracking system")
    void testRemovePlayer() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 5);
        resourceTracker.addResource(testPlayer, ResourceType.ULTIMATE_INGREDIENT, 3);
        
        resourceTracker.removePlayer(testPlayer);
        
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.ULTIMATE_INGREDIENT));
    }
    
    @Test
    @DisplayName("Should handle null player UUID gracefully")
    void testNullPlayerUuid() {
        resourceTracker.addResource(null, ResourceType.PRISMARINE_SHARD, 5);
        assertEquals(0, resourceTracker.getResourceCount(null, ResourceType.PRISMARINE_SHARD));
        
        assertFalse(resourceTracker.hasEnoughResources(null, ResourceType.PRISMARINE_SHARD, 1));
        assertFalse(resourceTracker.consumeResources(null, ResourceType.PRISMARINE_SHARD, 1));
        
        resourceTracker.clearResource(null, ResourceType.PRISMARINE_SHARD);
        resourceTracker.clearResources(null);
        resourceTracker.removePlayer(null);
        
        assertTrue(resourceTracker.getAllResources(null).isEmpty());
    }
    
    @Test
    @DisplayName("Should handle null resource type gracefully")
    void testNullResourceType() {
        resourceTracker.addResource(testPlayer, null, 5);
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, null));
        
        assertFalse(resourceTracker.hasEnoughResources(testPlayer, null, 1));
        assertFalse(resourceTracker.consumeResources(testPlayer, null, 1));
        
        resourceTracker.clearResource(testPlayer, null);
    }
    
    @Test
    @DisplayName("Should handle negative amounts gracefully")
    void testNegativeAmounts() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, -5);
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
        
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 5);
        assertFalse(resourceTracker.consumeResources(testPlayer, ResourceType.PRISMARINE_SHARD, -1));
        assertEquals(5, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
    }
    
    @Test
    @DisplayName("Should handle zero amounts correctly")
    void testZeroAmounts() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 0);
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
        
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 5);
        assertTrue(resourceTracker.hasEnoughResources(testPlayer, ResourceType.PRISMARINE_SHARD, 0));
        assertFalse(resourceTracker.consumeResources(testPlayer, ResourceType.PRISMARINE_SHARD, 0));
    }
    
    @Test
    @DisplayName("Should set resource count to zero removes entry")
    void testSetResourceCountToZero() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 5);
        resourceTracker.setResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD, 0);
        
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.PRISMARINE_SHARD));
    }
    
    @Test
    @DisplayName("Should return empty map for player with no resources")
    void testGetAllResourcesEmpty() {
        Map<ResourceType, Integer> allResources = resourceTracker.getAllResources(testPlayer);
        
        assertNotNull(allResources);
        assertTrue(allResources.isEmpty());
    }
    
    @Test
    @DisplayName("Should return defensive copy of resources")
    void testGetAllResourcesDefensiveCopy() {
        resourceTracker.addResource(testPlayer, ResourceType.PRISMARINE_SHARD, 5);
        
        Map<ResourceType, Integer> allResources = resourceTracker.getAllResources(testPlayer);
        allResources.put(ResourceType.ULTIMATE_INGREDIENT, 100);
        
        assertEquals(0, resourceTracker.getResourceCount(testPlayer, ResourceType.ULTIMATE_INGREDIENT));
    }
}

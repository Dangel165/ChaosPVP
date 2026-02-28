package com.verminpvp.managers;

import com.verminpvp.models.ClassType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages class ban voting and stores the banned class for the current game
 */
public class ClassBanManager {
    
    private final Map<UUID, ClassType> playerVotes; // Player UUID -> Voted class
    private ClassType bannedClass; // Currently banned class
    
    public ClassBanManager() {
        this.playerVotes = new HashMap<>();
        this.bannedClass = null;
    }
    
    /**
     * Reset all voting data
     */
    public void resetVoting() {
        playerVotes.clear();
        bannedClass = null;
    }
    
    /**
     * Record a player's vote for a class to ban
     */
    public void voteForClass(Player player, ClassType classType) {
        playerVotes.put(player.getUniqueId(), classType);
    }
    
    /**
     * Check if a player has voted
     */
    public boolean hasVoted(Player player) {
        return playerVotes.containsKey(player.getUniqueId());
    }
    
    /**
     * Get the class that a player voted for
     */
    public ClassType getPlayerVote(Player player) {
        return playerVotes.get(player.getUniqueId());
    }
    
    /**
     * Calculate the most voted class and set it as banned
     * Returns the banned class
     */
    public ClassType calculateBannedClass() {
        if (playerVotes.isEmpty()) {
            return null;
        }
        
        // Count votes for each class
        Map<ClassType, Integer> voteCounts = new HashMap<>();
        for (ClassType classType : playerVotes.values()) {
            voteCounts.put(classType, voteCounts.getOrDefault(classType, 0) + 1);
        }
        
        // Find class with most votes
        ClassType mostVoted = null;
        int maxVotes = 0;
        
        for (Map.Entry<ClassType, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                mostVoted = entry.getKey();
            } else if (entry.getValue() == maxVotes && mostVoted != null) {
                // Tie - randomly select between tied classes
                if (Math.random() < 0.5) {
                    mostVoted = entry.getKey();
                }
            }
        }
        
        bannedClass = mostVoted;
        return bannedClass;
    }
    
    /**
     * Get the currently banned class
     */
    public ClassType getBannedClass() {
        return bannedClass;
    }
    
    /**
     * Set the banned class directly (for testing or admin override)
     */
    public void setBannedClass(ClassType classType) {
        this.bannedClass = classType;
    }
    
    /**
     * Check if a class is banned
     */
    public boolean isClassBanned(ClassType classType) {
        return bannedClass != null && bannedClass == classType;
    }
    
    /**
     * Clear the banned class (called after game ends)
     */
    public void clearBannedClass() {
        bannedClass = null;
        playerVotes.clear();
    }
    
    /**
     * Get the number of votes for a specific class
     */
    public int getVoteCount(ClassType classType) {
        int count = 0;
        for (ClassType voted : playerVotes.values()) {
            if (voted == classType) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Get total number of players who have voted
     */
    public int getTotalVotes() {
        return playerVotes.size();
    }
}

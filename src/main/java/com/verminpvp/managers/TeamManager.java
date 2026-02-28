package com.verminpvp.managers;

import com.verminpvp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages team detection for Captain abilities and team mode
 * 
 * Uses Bukkit scoreboard teams when available, falls back to all players
 */
public class TeamManager {
    
    private final Random random;
    private final Map<UUID, Team> playerTeams;
    private org.bukkit.scoreboard.Team blueTeam;
    private org.bukkit.scoreboard.Team redTeam;
    
    public TeamManager() {
        this.random = new Random();
        this.playerTeams = new HashMap<>();
        initializeScoreboardTeams();
    }
    
    /**
     * Initialize scoreboard teams for display
     */
    private void initializeScoreboardTeams() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        
        Scoreboard scoreboard = manager.getMainScoreboard();
        
        // Unregister existing teams to ensure clean state
        org.bukkit.scoreboard.Team existingBlue = scoreboard.getTeam("blue_team");
        if (existingBlue != null) {
            existingBlue.unregister();
        }
        org.bukkit.scoreboard.Team existingRed = scoreboard.getTeam("red_team");
        if (existingRed != null) {
            existingRed.unregister();
        }
        
        // Create Blue Team with BLUE color (distinct from AQUA)
        blueTeam = scoreboard.registerNewTeam("blue_team");
        blueTeam.setColor(org.bukkit.ChatColor.BLUE);
        blueTeam.setAllowFriendlyFire(false);
        
        // Create Red Team with RED color
        redTeam = scoreboard.registerNewTeam("red_team");
        redTeam.setColor(org.bukkit.ChatColor.RED);
        redTeam.setAllowFriendlyFire(false);
    }
    
    /**
     * Set a player's team
     */
    public void setPlayerTeam(Player player, Team team) {
        // Store team assignment FIRST before any scoreboard operations
        playerTeams.put(player.getUniqueId(), team);
        
        // Remove from both scoreboard teams first to prevent conflicts
        if (blueTeam != null) blueTeam.removeEntry(player.getName());
        if (redTeam != null) redTeam.removeEntry(player.getName());
        
        // Add player to correct scoreboard team for display
        if (team == Team.BLUE) {
            if (blueTeam != null) {
                blueTeam.addEntry(player.getName());
                player.sendMessage("§9블루 팀에 배정되었습니다!");
            }
        } else if (team == Team.RED) {
            if (redTeam != null) {
                redTeam.addEntry(player.getName());
                player.sendMessage("§c레드 팀에 배정되었습니다!");
            }
        }
    }
    
    /**
     * Get a player's team
     */
    public Team getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }
    
    /**
     * Check if two players are on the same team
     */
    public boolean areSameTeam(Player player1, Player player2) {
        Team team1 = getPlayerTeam(player1);
        Team team2 = getPlayerTeam(player2);
        
        if (team1 == null || team2 == null) {
            return false;
        }
        
        return team1 == team2;
    }
    
    /**
     * Clear a player's team
     */
    public void clearPlayerTeam(Player player) {
        playerTeams.remove(player.getUniqueId());
        
        // Remove from scoreboard teams
        if (blueTeam != null) blueTeam.removeEntry(player.getName());
        if (redTeam != null) redTeam.removeEntry(player.getName());
    }
    
    /**
     * Clear all team assignments
     */
    public void clearAllTeams() {
        playerTeams.clear();
        
        // Clear scoreboard teams
        if (blueTeam != null) {
            for (String entry : new ArrayList<>(blueTeam.getEntries())) {
                blueTeam.removeEntry(entry);
            }
        }
        if (redTeam != null) {
            for (String entry : new ArrayList<>(redTeam.getEntries())) {
                redTeam.removeEntry(entry);
            }
        }
    }
    
    /**
     * Get all teammates of a player (excluding the player themselves)
     * 
     * @param player The player to get teammates for
     * @return List of teammates
     */
    public List<Player> getTeammates(Player player) {
        Team playerTeam = getPlayerTeam(player);
        if (playerTeam == null) {
            return new ArrayList<>();
        }
        
        return Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.equals(player))
            .filter(p -> getPlayerTeam(p) == playerTeam)
            .collect(Collectors.toList());
    }
    
    /**
     * Get a random teammate (excluding the player themselves)
     * 
     * @param player The player to get a teammate for
     * @return A random teammate, or null if no teammates available
     */
    public Player getRandomTeammate(Player player) {
        List<Player> teammates = getTeammates(player);
        if (teammates.isEmpty()) {
            return null;
        }
        return teammates.get(random.nextInt(teammates.size()));
    }
    
    /**
     * Get a random teammate excluding specific players
     * 
     * @param player The player to get a teammate for
     * @param exclude Players to exclude from selection
     * @return A random teammate, or null if no valid teammates available
     */
    public Player getRandomTeammate(Player player, List<Player> exclude) {
        List<Player> teammates = getTeammates(player).stream()
            .filter(p -> !exclude.contains(p))
            .collect(Collectors.toList());
        
        if (teammates.isEmpty()) {
            return null;
        }
        return teammates.get(random.nextInt(teammates.size()));
    }
    
    /**
     * Check if two players are on the same team (for Captain abilities)
     * 
     * @param player1 First player
     * @param player2 Second player
     * @return True if on same team, false otherwise
     */
    public boolean areTeammates(Player player1, Player player2) {
        if (player1.equals(player2)) {
            return false;
        }
        
        Team team1 = getPlayerTeam(player1);
        Team team2 = getPlayerTeam(player2);
        
        if (team1 == null || team2 == null) {
            // No team assigned, consider all as teammates for Captain
            return true;
        }
        
        return team1 == team2;
    }
}

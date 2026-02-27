package com.verminpvp.managers;

import com.verminpvp.models.ClassType;
import com.verminpvp.models.GameMode;
import com.verminpvp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Manages player name tags to display their class above their head
 */
public class ClassTagManager {
    
    private final Scoreboard scoreboard;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    
    public ClassTagManager(TeamManager teamManager, GameManager gameManager) {
        // Get or create main scoreboard
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        
        // Initialize teams for each class
        initializeTeams();
    }
    
    /**
     * Initialize scoreboard teams for each class
     */
    private void initializeTeams() {
        for (ClassType classType : ClassType.values()) {
            String teamName = "class_" + classType.name().toLowerCase();
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
            
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            
            // Set team prefix (displayed above player's head)
            String prefix = getClassPrefix(classType);
            team.setPrefix(prefix);
            
            // Set team color (will be overridden in team mode)
            team.setColor(getClassColor(classType));
            
            // Allow friendly fire
            team.setAllowFriendlyFire(true);
            
            // Show name tag
            team.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        }
    }
    
    /**
     * Get the prefix for a class type
     */
    private String getClassPrefix(ClassType classType) {
        switch (classType) {
            case SWORDSMAN:
                return ChatColor.GOLD + "[검사] " + ChatColor.RESET;
            case SCIENTIST:
                return ChatColor.GREEN + "[과학자] " + ChatColor.RESET;
            case PLAGUE_SPREADER:
                return ChatColor.DARK_GREEN + "[역병] " + ChatColor.RESET;
            case SHIELD_SOLDIER:
                return ChatColor.BLUE + "[방패병] " + ChatColor.RESET;
            case CRITICAL_CUTTER:
                return ChatColor.RED + "[크리티컬] " + ChatColor.RESET;
            case NAVIGATOR:
                return ChatColor.DARK_PURPLE + "[항해사] " + ChatColor.RESET;
            case CAPTAIN:
                return ChatColor.YELLOW + "[선장] " + ChatColor.RESET;
            case SHAPESHIFTER:
                return ChatColor.LIGHT_PURPLE + "[생명체] " + ChatColor.RESET;
            case JUGGLER:
                return ChatColor.WHITE + "[저글러] " + ChatColor.RESET;
            case DRAGON_FURY:
                return ChatColor.DARK_RED + "[용의 분노자] " + ChatColor.RESET;
            case UNDEAD:
                return ChatColor.DARK_GRAY + "[언데드] " + ChatColor.RESET;
            case STAMPER:
                return ChatColor.AQUA + "[스탬퍼] " + ChatColor.RESET;
            case TIME_ENGRAVER:
                return ChatColor.GOLD + "[시간 각인자] " + ChatColor.RESET;
            case CAVALRY:
                return ChatColor.DARK_AQUA + "[기마병] " + ChatColor.RESET;
            case VITALITY_CUTTER:
                return ChatColor.RED + "[활력 절단자] " + ChatColor.RESET;
            case MARATHONER:
                return ChatColor.AQUA + "[마라토너] " + ChatColor.RESET;
            default:
                return ChatColor.GRAY + "[???] " + ChatColor.RESET;
        }
    }
    
    /**
     * Get the color for a class type
     */
    private ChatColor getClassColor(ClassType classType) {
        switch (classType) {
            case SWORDSMAN:
                return ChatColor.GOLD;
            case SCIENTIST:
                return ChatColor.GREEN;
            case PLAGUE_SPREADER:
                return ChatColor.DARK_GREEN;
            case SHIELD_SOLDIER:
                return ChatColor.BLUE;
            case CRITICAL_CUTTER:
                return ChatColor.RED;
            case NAVIGATOR:
                return ChatColor.DARK_PURPLE;
            case CAPTAIN:
                return ChatColor.YELLOW;
            case SHAPESHIFTER:
                return ChatColor.LIGHT_PURPLE;
            case JUGGLER:
                return ChatColor.WHITE;
            case DRAGON_FURY:
                return ChatColor.DARK_RED;
            case UNDEAD:
                return ChatColor.DARK_GRAY;
            case STAMPER:
                return ChatColor.AQUA;
            case TIME_ENGRAVER:
                return ChatColor.GOLD;
            case CAVALRY:
                return ChatColor.DARK_AQUA;
            case VITALITY_CUTTER:
                return ChatColor.RED;
            case MARATHONER:
                return ChatColor.AQUA;
            default:
                return ChatColor.GRAY;
        }
    }
    
    /**
     * Set a player's class tag
     */
    public void setPlayerClassTag(Player player, ClassType classType) {
        if (player == null || classType == null) {
            return;
        }
        
        // Remove player from all class teams first
        removePlayerFromAllTeams(player);
        
        // Add player to their class team
        String teamName = "class_" + classType.name().toLowerCase();
        org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
        
        if (team != null) {
            team.addEntry(player.getName());
            
            // In team mode, set team color for name display
            if (gameManager != null && gameManager.getGameMode() == GameMode.TEAM) {
                Team playerTeam = teamManager.getPlayerTeam(player);
                if (playerTeam == Team.BLUE) {
                    team.setColor(ChatColor.BLUE);
                } else if (playerTeam == Team.RED) {
                    team.setColor(ChatColor.RED);
                }
                
                // Shapeshifter: disable glow effect
                if (classType == ClassType.SHAPESHIFTER) {
                    team.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, 
                        org.bukkit.scoreboard.Team.OptionStatus.NEVER);
                    // Remove glow by not setting any color option
                    player.setGlowing(false);
                } else {
                    // Other classes: enable glow with team color
                    player.setGlowing(true);
                }
            } else {
                // In solo mode, use class color
                team.setColor(getClassColor(classType));
                
                // Enable glow for specific classes in solo mode only
                if (classType == ClassType.DRAGON_FURY || 
                    classType == ClassType.UNDEAD ||
                    classType == ClassType.CAVALRY ||
                    classType == ClassType.VITALITY_CUTTER) {
                    player.setGlowing(true);
                } else {
                    player.setGlowing(false);
                }
            }
        }
    }
    
    /**
     * Remove a player's class tag
     */
    public void removePlayerClassTag(Player player) {
        if (player == null) {
            return;
        }
        
        removePlayerFromAllTeams(player);
    }
    
    /**
     * Remove a player from all class teams
     */
    private void removePlayerFromAllTeams(Player player) {
        for (ClassType classType : ClassType.values()) {
            String teamName = "class_" + classType.name().toLowerCase();
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
            
            if (team != null && team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }
    }
    
    /**
     * Get a player's class from their team
     */
    public ClassType getPlayerClassFromTag(Player player) {
        if (player == null) {
            return null;
        }
        
        for (ClassType classType : ClassType.values()) {
            String teamName = "class_" + classType.name().toLowerCase();
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
            
            if (team != null && team.hasEntry(player.getName())) {
                return classType;
            }
        }
        
        return null;
    }
    
    /**
     * Update all player colors based on current game mode
     */
    public void updateAllPlayerColors() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ClassType classType = getPlayerClassFromTag(player);
            if (classType != null) {
                setPlayerClassTag(player, classType);
            }
        }
    }
    
    /**
     * Cleanup all teams (called on plugin disable)
     */
    public void cleanup() {
        for (ClassType classType : ClassType.values()) {
            String teamName = "class_" + classType.name().toLowerCase();
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
            
            if (team != null) {
                team.unregister();
            }
        }
    }
}

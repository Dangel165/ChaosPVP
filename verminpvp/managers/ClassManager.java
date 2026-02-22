package com.verminpvp.managers;

import com.verminpvp.models.ClassData;
import com.verminpvp.models.ClassType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player class selection, switching, and state.
 * 
 * Responsibilities:
 * - Track which class each player has selected
 * - Provide starting items when a class is selected
 * - Clear items and effects when switching classes
 * - Store per-player class data (resources, cooldowns)
 */
public class ClassManager {
    
    private final Map<UUID, ClassType> playerClasses;
    private final Map<UUID, ClassData> playerData;
    private ItemProvider itemProvider;
    private ClassTagManager tagManager;
    private TeamManager teamManager;
    private GameManager gameManager;
    private Object scientistHandler;
    private Object plagueSpreaderHandler;
    private Object shieldSoldierHandler;
    private Object navigatorHandler;
    private Object criticalCutterHandler;
    private Object shapeshifterHandler;
    private Object jugglerHandler;
    private Object dragonFuryHandler;
    private Object undeadHandler;
    private Object swordsmanHandler;
    private Object stamperHandler;
    private Object timeEngraverHandler;
    
    public ClassManager() {
        this.playerClasses = new HashMap<>();
        this.playerData = new HashMap<>();
        // tagManager will be initialized later when TeamManager and GameManager are available
    }
    
    /**
     * Set the TeamManager and GameManager (called after initialization)
     */
    public void setManagers(TeamManager teamManager, GameManager gameManager) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.tagManager = new ClassTagManager(teamManager, gameManager);
    }
    
    /**
     * Set the ItemProvider (called after initialization)
     */
    public void setItemProvider(ItemProvider itemProvider) {
        this.itemProvider = itemProvider;
    }
    
    /**
     * Set handlers for starting schedulers
     */
    public void setHandlers(Object scientistHandler, Object plagueSpreaderHandler, 
                           Object shieldSoldierHandler, Object navigatorHandler,
                           Object criticalCutterHandler, Object shapeshifterHandler,
                           Object jugglerHandler, Object dragonFuryHandler,
                           Object undeadHandler, Object swordsmanHandler,
                           Object stamperHandler, Object timeEngraverHandler) {
        this.scientistHandler = scientistHandler;
        this.plagueSpreaderHandler = plagueSpreaderHandler;
        this.shieldSoldierHandler = shieldSoldierHandler;
        this.navigatorHandler = navigatorHandler;
        this.criticalCutterHandler = criticalCutterHandler;
        this.shapeshifterHandler = shapeshifterHandler;
        this.jugglerHandler = jugglerHandler;
        this.dragonFuryHandler = dragonFuryHandler;
        this.undeadHandler = undeadHandler;
        this.swordsmanHandler = swordsmanHandler;
        this.stamperHandler = stamperHandler;
        this.timeEngraverHandler = timeEngraverHandler;
    }
    
    /**
     * Set a player's class and initialize their class data
     * 
     * @param player The player to set the class for
     * @param classType The class type to set
     */
    public void setPlayerClass(Player player, ClassType classType) {
        if (player == null || classType == null) {
            throw new IllegalArgumentException("Player and classType cannot be null");
        }
        
        UUID playerId = player.getUniqueId();
        
        // Store the class type
        playerClasses.put(playerId, classType);
        
        // Create or update class data
        ClassData data = playerData.get(playerId);
        if (data == null) {
            data = new ClassData(playerId, classType);
            playerData.put(playerId, data);
        } else {
            data.setClassType(classType);
        }
        
        // Set player's class tag (displayed above head)
        tagManager.setPlayerClassTag(player, classType);
        
        // Provide starting items
        provideStartingItems(player, classType);
        
        // If player is in practice mode, start schedulers immediately
        if (gameManager != null && gameManager.isInPracticeMode(player)) {
            startClassSchedulers(player, classType);
        }
        // Otherwise, schedulers will be started when the game actually begins in startGame()
    }
    
    /**
     * Start schedulers for classes that need them
     * This should be called when the game actually starts, not when classes are selected
     */
    public void startClassSchedulers(Player player, ClassType classType) {
        try {
            switch (classType) {
                case SCIENTIST:
                    if (scientistHandler != null) {
                        scientistHandler.getClass()
                            .getMethod("startPotionGeneration", Player.class)
                            .invoke(scientistHandler, player);
                    }
                    break;
                case PLAGUE_SPREADER:
                    if (plagueSpreaderHandler != null) {
                        plagueSpreaderHandler.getClass()
                            .getMethod("startGenerationSchedulers", Player.class)
                            .invoke(plagueSpreaderHandler, player);
                    }
                    break;
                case SHIELD_SOLDIER:
                    if (shieldSoldierHandler != null) {
                        shieldSoldierHandler.getClass()
                            .getMethod("startAbsorptionPassive", Player.class)
                            .invoke(shieldSoldierHandler, player);
                    }
                    break;
                case NAVIGATOR:
                    if (navigatorHandler != null) {
                        navigatorHandler.getClass()
                            .getMethod("startNavalCombatGeneration", Player.class)
                            .invoke(navigatorHandler, player);
                    }
                    break;
                case CRITICAL_CUTTER:
                    if (criticalCutterHandler != null) {
                        criticalCutterHandler.getClass()
                            .getMethod("startActionBarDisplay", Player.class)
                            .invoke(criticalCutterHandler, player);
                    }
                    break;
                case SHAPESHIFTER:
                    if (shapeshifterHandler != null) {
                        shapeshifterHandler.getClass()
                            .getMethod("startEvolutionSystem", Player.class)
                            .invoke(shapeshifterHandler, player);
                    }
                    break;
                case JUGGLER:
                    if (jugglerHandler != null) {
                        jugglerHandler.getClass()
                            .getMethod("startThrowTimeGainSystem", Player.class)
                            .invoke(jugglerHandler, player);
                    }
                    break;
                case STAMPER:
                    if (stamperHandler != null) {
                        stamperHandler.getClass()
                            .getMethod("startDiveGainSystem", Player.class)
                            .invoke(stamperHandler, player);
                    }
                    break;
            }
        } catch (Exception e) {
            // Silently fail if handlers not set yet
        }
    }
    
    /**
     * Start all class schedulers for all online players
     * Called when the game starts
     */
    public void startAllClassSchedulers() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            ClassType classType = getPlayerClass(player);
            if (classType != null) {
                startClassSchedulers(player, classType);
            }
        }
    }
    
    /**
     * Provide starting items for a class
     */
    private void provideStartingItems(Player player, ClassType classType) {
        if (itemProvider == null) {
            return;
        }
        
        // Clear inventory first
        player.getInventory().clear();
        
        // Dragon Fury provides its own items completely (no default weapon or armor)
        if (classType == ClassType.DRAGON_FURY) {
            if (dragonFuryHandler != null) {
                try {
                    dragonFuryHandler.getClass()
                        .getMethod("giveStartingItems", Player.class)
                        .invoke(dragonFuryHandler, player);
                } catch (Exception e) {
                    // Silently fail
                }
            }
            return; // Skip default weapon and armor
        }
        
        // Give class weapon
        ItemStack weapon = itemProvider.createClassWeapon(classType);
        if (weapon != null) {
            player.getInventory().addItem(weapon);
        }
        
        // Give class-specific starting items
        switch (classType) {
            case SWORDSMAN:
                // Use SwordsmanHandler to give starting items
                if (swordsmanHandler != null) {
                    try {
                        swordsmanHandler.getClass()
                            .getMethod("giveStartingItems", Player.class)
                            .invoke(swordsmanHandler, player);
                    } catch (Exception e) {
                        // Silently fail
                    }
                }
                break;
                
            case SHIELD_SOLDIER:
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "red_shield"));
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "blue_shield"));
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.SHIELD_SOLDIER, "basic_shield"));
                break;
                
            case CRITICAL_CUTTER:
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.CRITICAL_CUTTER, "guaranteed_critical"));
                break;
                
            case NAVIGATOR:
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.NAVIGATOR, "wave_riding_item"));
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.NAVIGATOR, "harpoon"));
                break;
                
            case CAPTAIN:
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.CAPTAIN, "captains_command"));
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.CAPTAIN, "captains_harpoon"));
                break;
                
            case JUGGLER:
                player.getInventory().addItem(itemProvider.getJugglerLightThing());
                player.getInventory().addItem(itemProvider.getJugglerHeavyThing());
                break;
                
            case UNDEAD:
                // Undead starts with default items (stone sword + diamond chestplate)
                // Initialize undead state
                if (undeadHandler != null) {
                    try {
                        undeadHandler.getClass()
                            .getMethod("initializePlayer", Player.class)
                            .invoke(undeadHandler, player);
                    } catch (Exception e) {
                        // Silently fail
                    }
                }
                break;
                
            case STAMPER:
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.STAMPER, "stamp"));
                break;
                
            case TIME_ENGRAVER:
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.TIME_ENGRAVER, "time_engrave"));
                player.getInventory().addItem(itemProvider.createSpecialItem(ClassType.TIME_ENGRAVER, "clock_needle_stitch"));
                break;
        }
    }
    
    /**
     * Get a player's current class
     * 
     * @param player The player to get the class for
     * @return The player's class type, or null if no class is set
     */
    public ClassType getPlayerClass(Player player) {
        if (player == null) {
            return null;
        }
        return playerClasses.get(player.getUniqueId());
    }
    
    /**
     * Clear a player's class and all associated data
     * 
     * @param player The player to clear the class for
     */
    public void clearPlayerClass(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Remove class type
        playerClasses.remove(playerId);
        
        // Remove class tag
        tagManager.removePlayerClassTag(player);
        
        // Clear class data if it exists
        ClassData data = playerData.get(playerId);
        if (data != null) {
            data.clear();
        }
    }
    
    /**
     * Get a player's class data
     * 
     * @param player The player to get the data for
     * @return The player's class data, or null if no class is set
     */
    public ClassData getClassData(Player player) {
        if (player == null) {
            return null;
        }
        return playerData.get(player.getUniqueId());
    }
    
    /**
     * Check if a player has a class set
     * 
     * @param player The player to check
     * @return true if the player has a class, false otherwise
     */
    public boolean hasClass(Player player) {
        return player != null && playerClasses.containsKey(player.getUniqueId());
    }
    
    /**
     * Remove all data for a player (used when player leaves)
     * 
     * @param playerId The UUID of the player to remove
     */
    public void removePlayer(UUID playerId) {
        playerClasses.remove(playerId);
        playerData.remove(playerId);
    }
    
    /**
     * Get the tag manager
     */
    public ClassTagManager getTagManager() {
        return tagManager;
    }
    
    /**
     * Get a player's team name from scoreboard
     * 
     * @param player The player to get the team for
     * @return The team name, or null if no team
     */
    public String getPlayerTeam(Player player) {
        if (player == null) {
            return null;
        }
        
        org.bukkit.scoreboard.Scoreboard scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = scoreboard.getEntryTeam(player.getName());
        
        return team != null ? team.getName() : null;
    }
    
    /**
     * Check if a class is already taken by another player
     * 
     * @param classType The class type to check
     * @return true if the class is taken, false otherwise
     */
    public boolean isClassTaken(ClassType classType) {
        if (classType == null) {
            return false;
        }
        
        for (ClassType takenClass : playerClasses.values()) {
            if (takenClass == classType) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a class is already taken by another player in a specific team
     * 
     * @param classType The class type to check
     * @param team The team to check within
     * @return true if the class is taken by someone in that team, false otherwise
     */
    public boolean isClassTakenByTeam(ClassType classType, com.verminpvp.models.Team team) {
        if (classType == null || team == null || teamManager == null) {
            return false;
        }
        
        for (Map.Entry<UUID, ClassType> entry : playerClasses.entrySet()) {
            if (entry.getValue() == classType) {
                Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    com.verminpvp.models.Team playerTeam = teamManager.getPlayerTeam(player);
                    if (playerTeam == team) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Get the player who has the specified class
     * 
     * @param classType The class type to check
     * @return The player with that class, or null if no one has it
     */
    public Player getPlayerWithClass(ClassType classType) {
        for (Map.Entry<UUID, ClassType> entry : playerClasses.entrySet()) {
            if (entry.getValue() == classType) {
                Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    return player;
                }
            }
        }
        return null;
    }
    
    /**
     * Get the player who has the specified class in a specific team
     * 
     * @param classType The class type to check
     * @param team The team to check within
     * @return The player with that class in that team, or null if no one has it
     */
    public Player getPlayerWithClassInTeam(ClassType classType, com.verminpvp.models.Team team) {
        if (classType == null || team == null || teamManager == null) {
            return null;
        }
        
        for (Map.Entry<UUID, ClassType> entry : playerClasses.entrySet()) {
            if (entry.getValue() == classType) {
                Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    com.verminpvp.models.Team playerTeam = teamManager.getPlayerTeam(player);
                    if (playerTeam == team) {
                        return player;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Cleanup resources (called on plugin disable)
     */
    public void cleanup() {
        if (tagManager != null) {
            tagManager.cleanup();
        }
    }
}

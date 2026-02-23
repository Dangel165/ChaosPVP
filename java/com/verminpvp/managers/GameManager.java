package com.verminpvp.managers;

import com.verminpvp.VerminPVP;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.GameMode;
import com.verminpvp.models.Team;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages game state, timer, and player effects during the game
 */
public class GameManager {
    
    private final Plugin plugin;
    private final ClassManager classManager;
    private final TeamManager teamManager;
    private ExcludeManager excludeManager;
    private MapManager mapManager;
    private LobbyManager lobbyManager;
    
    private boolean gameActive = false;
    private boolean countdownActive = false;
    private boolean monitoringClassSelection = false;
    private GameMode gameMode = GameMode.TEAM; // Default to team mode
    private int gameTimeRemaining = 0; // in seconds
    private BukkitTask gameTimerTask;
    private BukkitTask effectTask;
    private BukkitTask classSelectionMonitorTask;
    private org.bukkit.boss.BossBar timeBossBar; // Boss bar for time display
    
    // Track original player states
    private final Map<UUID, Boolean> originalOpStatus = new HashMap<>();
    private final Map<UUID, org.bukkit.GameMode> originalGameMode = new HashMap<>();
    
    // Track players in practice mode
    private final Map<UUID, Boolean> practiceModeStatus = new HashMap<>();
    
    // Track practice mode cooldowns (player UUID -> cooldown end time in milliseconds)
    private final Map<UUID, Long> practiceModeCooldowns = new HashMap<>();
    
    // Track practice mode timers (player UUID -> BukkitTask)
    private final Map<UUID, BukkitTask> practiceModeTimers = new HashMap<>();
    
    // Track practice mode time remaining (player UUID -> seconds remaining)
    private final Map<UUID, Integer> practiceModeTimeRemaining = new HashMap<>();
    
    // Track practice mode boss bars (player UUID -> BossBar)
    private final Map<UUID, org.bukkit.boss.BossBar> practiceModeBossBars = new HashMap<>();
    
    // Track game worlds for reset
    private final Set<String> gameWorlds = new HashSet<>();
    private boolean multiverseEnabled = false;
    
    private static final int GAME_DURATION = 300; // 5 minutes in seconds
    private static final int COUNTDOWN_DURATION = 10; // 10 seconds countdown
    private static final int FREEZE_DURATION = 10; // 10 seconds freeze
    private static final int PRACTICE_MODE_COOLDOWN = 300; // 5 minutes in seconds
    private static final int PRACTICE_MODE_DURATION = 300; // 5 minutes in seconds
    
    public GameManager(Plugin plugin, ClassManager classManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.teamManager = teamManager;
        
        // Check if Multiverse-Core is available
        checkMultiverseCore();
    }
    
    /**
     * Check if Multiverse-Core plugin is available
     */
    private void checkMultiverseCore() {
        Plugin mvPlugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if (mvPlugin != null && mvPlugin.isEnabled()) {
            multiverseEnabled = true;
            plugin.getLogger().info("Multiverse-Core detected! World reset feature enabled.");
        } else {
            multiverseEnabled = false;
            plugin.getLogger().info("Multiverse-Core not found. World reset feature disabled.");
            plugin.getLogger().info("Install Multiverse-Core to enable automatic world reset after games.");
        }
    }
    
    /**
     * Get the plugin instance
     */
    public Plugin getPlugin() {
        return plugin;
    }
    
    /**
     * Save original OP status for a player before removing it
     */
    public void saveOriginalOpStatus(Player player) {
        boolean isOp = player.isOp();
        originalOpStatus.put(player.getUniqueId(), isOp);
        Bukkit.getLogger().info("[ChaosPVP] Saved OP status for " + player.getName() + ": " + isOp);
    }
    
    /**
     * Get the MapManager instance
     */
    public MapManager getMapManager() {
        return mapManager;
    }
    
    /**
     * Set the ExcludeManager, MapManager, and LobbyManager (called after initialization)
     */
    public void setManagers(ExcludeManager excludeManager, MapManager mapManager, LobbyManager lobbyManager) {
        this.excludeManager = excludeManager;
        this.mapManager = mapManager;
        this.lobbyManager = lobbyManager;
    }
    
    /**
     * Start monitoring for class selection completion
     * Once all non-excluded players have selected a class, start the countdown
     */
    public void startClassSelectionMonitoring() {
        if (monitoringClassSelection || gameActive || countdownActive) {
            return;
        }
        
        monitoringClassSelection = true;
        
        // Check every second if all players have selected classes
        classSelectionMonitorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Stop monitoring if game is no longer active or monitoring was cancelled
            if (!monitoringClassSelection || gameActive || countdownActive) {
                if (classSelectionMonitorTask != null) {
                    classSelectionMonitorTask.cancel();
                    classSelectionMonitorTask = null;
                }
                monitoringClassSelection = false;
                return;
            }
            
            // Count players who need to select classes
            int totalPlayers = 0;
            int playersWithClass = 0;
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Skip excluded players
                if (excludeManager != null && excludeManager.isExcluded(player)) {
                    continue;
                }
                
                totalPlayers++;
                
                // Check if player has selected a class
                if (classManager.getPlayerClass(player) != null) {
                    playersWithClass++;
                }
            }
            
            // If all players have selected classes, start countdown
            if (totalPlayers > 0 && playersWithClass >= totalPlayers) {
                monitoringClassSelection = false;
                
                // Cancel monitoring task
                if (classSelectionMonitorTask != null) {
                    classSelectionMonitorTask.cancel();
                    classSelectionMonitorTask = null;
                }
                
                // Broadcast that all players have selected
                Bukkit.broadcastMessage("§a모든 플레이어가 클래스를 선택했습니다!");
                
                // Start countdown and game
                startCountdownAndGame();
            }
        }, 20L, 20L); // Check every second
    }
    
    /**
     * Start countdown before teleporting to map
     * Called after all players have selected their classes
     */
    public void startCountdownAndGame() {
        if (gameActive || countdownActive) {
            return;
        }
        
        countdownActive = true;
        
        // Schedule individual countdown messages using runTaskLater
        for (int i = 0; i <= COUNTDOWN_DURATION; i++) {
            final int secondsLeft = COUNTDOWN_DURATION - i;
            
            if (secondsLeft > 0) {
                // Schedule countdown message
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (countdownActive) {
                        Bukkit.broadcastMessage("§e게임 시작까지: §f" + secondsLeft + "초");
                    }
                }, i * 20L);
            } else {
                // Schedule game start
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (countdownActive) {
                        countdownActive = false;
                        teleportAndStartGame();
                    }
                }, i * 20L);
            }
        }
    }
    
    /**
     * Teleport players to lobby and start the game with freeze period
     */
    private void teleportAndStartGame() {
        // Get lobby location
        Location lobbyLocation = null;
        if (lobbyManager != null && lobbyManager.hasLobbyLocation()) {
            lobbyLocation = lobbyManager.getLobbyLocation();
        }
        
        if (lobbyLocation == null) {
            Bukkit.broadcastMessage("§c로비를 찾을 수 없습니다!");
            Bukkit.broadcastMessage("§7관리자는 /로비 지정 명령어로 로비를 설정해주세요.");
            return;
        }
        
        // Broadcast lobby teleport
        Bukkit.broadcastMessage("§a로비로 이동합니다...");
        
        // Set world difficulty to NORMAL and disable natural regeneration
        if (lobbyLocation.getWorld() != null) {
            lobbyLocation.getWorld().setDifficulty(org.bukkit.Difficulty.NORMAL);
            lobbyLocation.getWorld().setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, false);
        }
        
        // Teleport all non-excluded players to lobby
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (excludeManager != null && excludeManager.isExcluded(player)) {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                player.sendMessage("§7게임 참여에서 제외되어 관전 모드로 전환되었습니다.");
                continue;
            }
            
            // NOTE: OP status and game mode are already saved and handled by StartGameCommand
            // Do NOT save/remove OP here to avoid overwriting the original status
            
            // Set max health to 20 hearts (40 HP)
            player.setMaxHealth(40.0);
            player.setHealth(40.0);
            
            // Teleport to lobby location
            player.teleport(lobbyLocation);
            
            // Apply game effects
            applyGameEffects(player);
        }
        
        // Immediately teleport to map (no 10 second wait)
        teleportPlayersToRandomMapSpawns();
    }
    
    /**
     * Teleport each player to a random map spawn location
     */
    private void teleportPlayersToRandomMapSpawns() {
        // Check if maps are available
        if (mapManager == null || !mapManager.hasSpawnLocations()) {
            Bukkit.broadcastMessage("§c맵을 찾을 수 없습니다!");
            Bukkit.broadcastMessage("§7관리자는 /맵지정 명령어로 맵을 설정해주세요.");
            return;
        }
        
        java.util.List<Location> availableSpawns = new java.util.ArrayList<>(mapManager.getAllSpawnLocations());
        
        if (availableSpawns.isEmpty()) {
            Bukkit.broadcastMessage("§c사용 가능한 맵이 없습니다!");
            return;
        }
        
        // Broadcast map teleport
        Bukkit.broadcastMessage("§e맵으로 이동합니다!");
        
        // Clear game worlds set
        gameWorlds.clear();
        
        // Shuffle spawn locations to ensure randomness
        java.util.Collections.shuffle(availableSpawns);
        
        // Teleport each player to a unique spawn location
        int spawnIndex = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                // Get unique spawn location (cycle through if more players than spawns)
                Location spawn = availableSpawns.get(spawnIndex % availableSpawns.size()).clone();
                spawnIndex++;
                
                // Track game world for reset
                if (spawn.getWorld() != null) {
                    gameWorlds.add(spawn.getWorld().getName());
                }
                
                // Teleport player
                player.teleport(spawn);
                
                // Show title
                player.sendTitle("§a맵 도착", "§710초 후 게임 시작", 10, 40, 10);
            }
        }
        
        // Start freeze period
        startFreezePeriodWithCountdown();
    }
    
    /**
     * Start freeze period with countdown (non-repeating)
     */
    private void startFreezePeriodWithCountdown() {
        Bukkit.broadcastMessage("§e10초 후 게임이 시작됩니다!");
        
        // Apply freeze effects to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                // Slowness 255 (완전히 움직이지 못하게)
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 
                    FREEZE_DURATION * 20, 255, false, false, false));
                // Jump Boost -100 (점프 불가)
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 
                    FREEZE_DURATION * 20, 128, false, false, false));
                // Mining Fatigue 255 (블록 파괴 불가)
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 
                    FREEZE_DURATION * 20, 255, false, false, false));
            }
        }
        
        // Schedule countdown messages
        for (int i = 1; i <= FREEZE_DURATION; i++) {
            final int secondsLeft = FREEZE_DURATION - i + 1;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                            new TextComponent("§c움직일 수 있을 때까지: §f" + secondsLeft + "초"));
                    }
                }
            }, i * 20L);
        }
        
        // Start game after freeze period
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            startGame();
        }, (FREEZE_DURATION + 1) * 20L);
    }
    
    /**
     * Start the game (called after freeze period)
     */
    public void startGame() {
        if (gameActive) {
            return;
        }
        
        gameActive = true;
        
        // Set game duration
        gameTimeRemaining = GAME_DURATION; // 5 minutes
        
        // Broadcast game start
        Bukkit.broadcastMessage("§a§l게임 시작!");
        Bukkit.broadcastMessage("§e게임 모드: " + gameMode.getDisplayName());
        Bukkit.broadcastMessage("§e게임 시간: 5분");
        
        // Create boss bar for time display
        createTimeBossBar();
        
        // Remove freeze effects and give starting items
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.removePotionEffect(PotionEffectType.JUMP_BOOST);
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                
                // Get player's class
                ClassType playerClass = classManager.getPlayerClass(player);
                
                // Give stone sword to classes that need it
                // Exclude: Plague Spreader, Shield Soldier, Critical Cutter, Navigator, Captain, Shapeshifter, Juggler, Dragon Fury, Swordsman
                if (playerClass != ClassType.PLAGUE_SPREADER &&
                    playerClass != ClassType.SHIELD_SOLDIER && 
                    playerClass != ClassType.CRITICAL_CUTTER &&
                    playerClass != ClassType.NAVIGATOR &&
                    playerClass != ClassType.CAPTAIN &&
                    playerClass != ClassType.SHAPESHIFTER &&
                    playerClass != ClassType.JUGGLER &&
                    playerClass != ClassType.DRAGON_FURY &&
                    playerClass != ClassType.SWORDSMAN) {
                    // All other classes get stone sword
                    org.bukkit.inventory.ItemStack stoneSword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE_SWORD);
                    org.bukkit.inventory.meta.ItemMeta swordMeta = stoneSword.getItemMeta();
                    if (swordMeta != null) {
                        swordMeta.setUnbreakable(true);
                        swordMeta.setDisplayName("§7돌검");
                        stoneSword.setItemMeta(swordMeta);
                    }
                    player.getInventory().addItem(stoneSword);
                }
                
                // Give diamond chestplate (unbreakable) to all classes
                // Dragon Fury has iron armor set, so skip
                if (playerClass != ClassType.DRAGON_FURY) {
                    org.bukkit.inventory.ItemStack diamondChestplate = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_CHESTPLATE);
                    org.bukkit.inventory.meta.ItemMeta chestplateMeta = diamondChestplate.getItemMeta();
                    if (chestplateMeta != null) {
                        chestplateMeta.setUnbreakable(true);
                        chestplateMeta.setDisplayName("§b다이아몬드 흉갑");
                        diamondChestplate.setItemMeta(chestplateMeta);
                    }
                    player.getInventory().setChestplate(diamondChestplate);
                }
            }
        }
        
        // Start all class-specific schedulers NOW (cooldowns and item generation begin here)
        classManager.startAllClassSchedulers();
        
        // Start game timer
        startGameTimer();
        
        // Start effect refresh task
        startEffectRefresh();
        
        // Check win condition immediately (for single player games)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (gameActive) {
                Bukkit.getLogger().info("[ChaosPVP] Checking initial win condition after game start");
                checkWinCondition();
            }
        }, 20L); // Check after 1 second
    }
    
    /**
     * Create boss bar for time display
     */
    private void createTimeBossBar() {
        // Remove existing boss bar if any
        if (timeBossBar != null) {
            timeBossBar.removeAll();
            timeBossBar = null;
        }
        
        // Create new boss bar
        timeBossBar = Bukkit.createBossBar(
            "§a§l게임 진행 중",
            org.bukkit.boss.BarColor.GREEN,
            org.bukkit.boss.BarStyle.SOLID
        );
        
        // Add all adventure mode players to boss bar
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                timeBossBar.addPlayer(player);
            }
        }
        
        // Make boss bar visible
        timeBossBar.setVisible(true);
    }
    
    /**
     * Update boss bar with current time
     */
    private void updateTimeBossBar() {
        if (timeBossBar == null) return;
        
        int minutes = gameTimeRemaining / 60;
        int seconds = gameTimeRemaining % 60;
        
        // Update title
        String title = String.format("§e남은 시간: §f%d:%02d", minutes, seconds);
        timeBossBar.setTitle(title);
        
        // Update progress (0.0 to 1.0)
        double progress = (double) gameTimeRemaining / GAME_DURATION;
        timeBossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        
        // Change color based on time remaining
        if (gameTimeRemaining <= 30) {
            timeBossBar.setColor(org.bukkit.boss.BarColor.RED);
        } else if (gameTimeRemaining <= 60) {
            timeBossBar.setColor(org.bukkit.boss.BarColor.YELLOW);
        } else {
            timeBossBar.setColor(org.bukkit.boss.BarColor.GREEN);
        }
    }
    
    /**
     * Remove boss bar
     */
    private void removeTimeBossBar() {
        if (timeBossBar != null) {
            timeBossBar.removeAll();
            timeBossBar.setVisible(false);
            timeBossBar = null;
        }
    }
    
    /**
     * Set the game mode (team or solo)
     */
    public void setGameMode(GameMode mode) {
        this.gameMode = mode;
    }
    
    /**
     * Get the current game mode
     */
    public GameMode getGameMode() {
        return gameMode;
    }
    
    /**
     * End the game and announce winner
     * This is called when the game timer runs out
     */
    public void endGame() {
        if (!gameActive) {
            return;
        }
        
        // Set all flags to false
        gameActive = false;
        countdownActive = false;
        monitoringClassSelection = false;
        
        // Cancel tasks
        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }
        
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }
        
        if (classSelectionMonitorTask != null) {
            classSelectionMonitorTask.cancel();
            classSelectionMonitorTask = null;
        }
        
        // Reset game time
        gameTimeRemaining = 0;
        
        // Remove boss bar
        removeTimeBossBar();
        
        // Clean up class-specific effects
        cleanupClassEffects();
        
        // Clean up world (remove mobs and items)
        cleanupWorld();
        
        // Restore world settings
        restoreWorldSettings();
        
        // Restore player states and remove effects
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeGameEffects(player);
            restorePlayerState(player);
            
            // Remove glowing effect
            player.setGlowing(false);
            
            // Clear all resources (including Scientist's Ultimate Ingredient stacks)
            VerminPVP pluginInstance = (VerminPVP) plugin;
            if (pluginInstance.getResourceTracker() != null) {
                pluginInstance.getResourceTracker().clearResources(player.getUniqueId());
            }
        }
        
        // CRITICAL: Clear all player classes to prevent duplicate class issues in next game
        for (Player player : Bukkit.getOnlinePlayers()) {
            classManager.clearPlayerClass(player);
        }
        
        // Determine winner
        announceWinner();
    }
    
    /**
     * Forcefully end the game without announcing winner
     */
    public void forceEndGame() {
        // Set all flags to false FIRST
        gameActive = false;
        countdownActive = false;
        monitoringClassSelection = false;
        
        // Cancel ALL scheduled tasks for this plugin
        Bukkit.getScheduler().cancelTasks(plugin);
        
        // Explicitly set task references to null
        gameTimerTask = null;
        effectTask = null;
        classSelectionMonitorTask = null;
        
        // Reset game time
        gameTimeRemaining = 0;
        
        // Remove boss bar
        removeTimeBossBar();
        
        // Clean up class-specific effects
        cleanupClassEffects();
        
        // Clean up world (remove mobs and items)
        cleanupWorld();
        
        // Restore world settings
        restoreWorldSettings();
        
        // Restore player states and remove effects
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeGameEffects(player);
            restorePlayerState(player);
            
            // Remove glowing effect
            player.setGlowing(false);
            
            // Clear all resources (including Scientist's Ultimate Ingredient stacks)
            VerminPVP pluginInstance = (VerminPVP) plugin;
            if (pluginInstance.getResourceTracker() != null) {
                pluginInstance.getResourceTracker().clearResources(player.getUniqueId());
            }
        }
        
        // CRITICAL: Clear all player classes to prevent duplicate class issues in next game
        for (Player player : Bukkit.getOnlinePlayers()) {
            classManager.clearPlayerClass(player);
        }
        
        // Teleport all players to lobby if set
        teleportAllToLobby();
        
        Bukkit.broadcastMessage("§c게임이 강제 종료되었습니다!");
    }
    
    /**
     * Teleport all players to lobby location
     */
    private void teleportAllToLobby() {
        if (lobbyManager == null || !lobbyManager.hasLobbyLocation()) {
            return;
        }
        
        Location lobbyLocation = lobbyManager.getLobbyLocation();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(lobbyLocation);
        }
        
        Bukkit.broadcastMessage("§a모든 플레이어가 로비로 이동했습니다!");
        
        // Reset game worlds after teleporting players
        resetGameWorlds();
    }
    
    /**
     * Reset game worlds using Multiverse-Core
     */
    private void resetGameWorlds() {
        if (!multiverseEnabled || gameWorlds.isEmpty()) {
            return;
        }
        
        plugin.getLogger().info("Resetting game worlds: " + gameWorlds);
        
        for (String worldName : gameWorlds) {
            try {
                // Use Bukkit command to regenerate world via Multiverse-Core
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv regen " + worldName);
                    plugin.getLogger().info("World reset command sent for: " + worldName);
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to reset world " + worldName + ": " + e.getMessage());
            }
        }
        
        // Clear the set after reset
        gameWorlds.clear();
    }
    
    /**
     * Restore world settings to default
     */
    private void restoreWorldSettings() {
        // Restore world settings for all worlds
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            // Re-enable natural regeneration
            world.setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, true);
        }
    }
    
    /**
     * Restore player's original state (OP and game mode)
     */
    private void restorePlayerState(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Clear player class
        classManager.clearPlayerClass(player);
        
        // Clear inventory
        player.getInventory().clear();
        
        // If player is in spectator mode (dead), respawn them first
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            // Set to survival temporarily to allow respawn
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            
            // Respawn the player
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.spigot().respawn();
                
                // Then restore to creative after respawn
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setGameMode(org.bukkit.GameMode.CREATIVE);
                    player.setHealth(20.0);
                }, 2L);
            }, 1L);
        } else {
            // Restore game mode to creative
            player.setGameMode(org.bukkit.GameMode.CREATIVE);
        }
        
        // Restore OP status
        Boolean wasOp = originalOpStatus.get(playerId);
        Bukkit.getLogger().info("[ChaosPVP] Restoring OP for " + player.getName() + ", wasOp: " + wasOp);
        if (wasOp != null && wasOp) {
            player.setOp(true);
            player.sendMessage("§aOP 권한이 복구되었습니다.");
        }
        
        // Restore max health to default (20 HP = 10 hearts)
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
        
        // Clear tracking
        originalOpStatus.remove(playerId);
        originalGameMode.remove(playerId);
    }
    
    /**
     * Clean up all class-specific effects (called on game end)
     */
    private void cleanupClassEffects() {
        VerminPVP pluginInstance = (VerminPVP) plugin;
        
        // Clean up Plague Spreader poison fields
        if (pluginInstance.getPlagueSpreaderHandler() != null) {
            pluginInstance.getPlagueSpreaderHandler().cleanupAll();
        }
        
        // Clean up Shield Soldier passive tasks and tracking
        if (pluginInstance.getShieldSoldierHandler() != null) {
            pluginInstance.getShieldSoldierHandler().cleanupAll();
        }
        
        // Clean up Shapeshifter evolution tasks and disguises
        if (pluginInstance.getShapeshifterHandler() != null) {
            pluginInstance.getShapeshifterHandler().cleanupAll();
        }
        
        // Clean up Juggler throw time gain tasks
        if (pluginInstance.getJugglerHandler() != null) {
            pluginInstance.getJugglerHandler().cleanupAll();
        }
    }
    
    /**
     * Clean up world by removing mobs and items (called on game end)
     */
    private void cleanupWorld() {
        // Remove all mobs and items from all worlds
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                // Skip players
                if (entity instanceof Player) {
                    continue;
                }
                
                // Remove mobs (monsters, animals, etc.)
                if (entity instanceof org.bukkit.entity.Monster ||
                    entity instanceof org.bukkit.entity.Animals ||
                    entity instanceof org.bukkit.entity.WaterMob ||
                    entity instanceof org.bukkit.entity.Flying ||
                    entity instanceof org.bukkit.entity.Slime ||
                    entity instanceof org.bukkit.entity.Ambient) {
                    entity.remove();
                }
                
                // Remove dropped items
                if (entity instanceof org.bukkit.entity.Item) {
                    entity.remove();
                }
            }
        }
        
        Bukkit.getLogger().info("[ChaosPVP] World cleanup complete - removed all mobs and items");
    }
    
    /**
     * Apply game effects to a player (saturation, night vision, hide effects)
     */
    public void applyGameEffects(Player player) {
        // Saturation (no hunger)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 
            Integer.MAX_VALUE, 0, false, false, false));
        
        // Night Vision
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 
            Integer.MAX_VALUE, 0, false, false, false));
        
        // Apply glow effect (team mode or solo mode)
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 
            Integer.MAX_VALUE, 0, false, false, false));
    }
    
    /**
     * Apply team-colored glow effect
     */
    private void applyTeamGlow(Player player) {
        Team team = teamManager.getPlayerTeam(player);
        if (team == null) return;
        
        // Apply glowing effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 
            Integer.MAX_VALUE, 0, false, false, false));
    }
    
    /**
     * Remove game effects from a player
     */
    public void removeGameEffects(Player player) {
        player.removePotionEffect(PotionEffectType.SATURATION);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.GLOWING);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
    }
    
    /**
     * Start the game timer countdown
     */
    private void startGameTimer() {
        gameTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameActive) {
                return;
            }
            
            gameTimeRemaining--;
            
            // Update boss bar
            updateTimeBossBar();
            
            // Check if time is up
            if (gameTimeRemaining <= 0) {
                endGame();
            }
            
            // Announce time milestones
            if (gameTimeRemaining == 240) { // 4 minutes
                Bukkit.broadcastMessage("§e남은 시간: 4분");
            } else if (gameTimeRemaining == 180) { // 3 minutes
                Bukkit.broadcastMessage("§e남은 시간: 3분");
            } else if (gameTimeRemaining == 120) { // 2 minutes
                Bukkit.broadcastMessage("§e남은 시간: 2분");
            } else if (gameTimeRemaining == 60) { // 1 minute
                Bukkit.broadcastMessage("§c남은 시간: 1분!");
            } else if (gameTimeRemaining == 30) { // 30 seconds
                Bukkit.broadcastMessage("§c남은 시간: 30초!");
            } else if (gameTimeRemaining <= 10 && gameTimeRemaining > 0) {
                Bukkit.broadcastMessage("§c" + gameTimeRemaining + "초!");
            }
        }, 20L, 20L); // Run every second
    }
    
    /**
     * Start effect refresh task to maintain effects
     */
    private void startEffectRefresh() {
        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameActive) {
                return;
            }
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Refresh effects if they're missing
                if (!player.hasPotionEffect(PotionEffectType.SATURATION)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 
                        Integer.MAX_VALUE, 0, false, false, false));
                }
                if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 
                        Integer.MAX_VALUE, 0, false, false, false));
                }
                // Refresh glow effect in team mode
                if (gameMode == GameMode.TEAM && !player.hasPotionEffect(PotionEffectType.GLOWING)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 
                        Integer.MAX_VALUE, 0, false, false, false));
                }
            }
        }, 100L, 100L); // Run every 5 seconds
    }
    
    /**
     * Display time remaining to all players via action bar
     */
    private void displayTimeRemaining() {
        int minutes = gameTimeRemaining / 60;
        int seconds = gameTimeRemaining % 60;
        
        String timeText = String.format("§e남은 시간: §f%d:%02d", minutes, seconds);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                new TextComponent(timeText));
        }
    }
    
    /**
     * Announce the winner based on remaining players
     */
    private void announceWinner() {
        if (gameMode == GameMode.TEAM) {
            announceTeamWinner();
        } else {
            announceSoloWinner();
        }
    }
    
    /**
     * Announce team mode winner
     */
    private void announceTeamWinner() {
        Map<Team, Integer> teamCounts = new HashMap<>();
        
        // Count alive players per team (Adventure mode only)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                Team team = teamManager.getPlayerTeam(player);
                if (team != null) {
                    teamCounts.put(team, teamCounts.getOrDefault(team, 0) + 1);
                }
            }
        }
        
        // Find winning team
        Team winningTeam = null;
        int maxPlayers = 0;
        
        for (Map.Entry<Team, Integer> entry : teamCounts.entrySet()) {
            if (entry.getValue() > maxPlayers) {
                maxPlayers = entry.getValue();
                winningTeam = entry.getKey();
            }
        }
        
        // Announce winner
        Bukkit.broadcastMessage("§6§l======================");
        Bukkit.broadcastMessage("§a§l게임 종료!");
        
        if (winningTeam != null && maxPlayers > 0) {
            Bukkit.broadcastMessage("§e§l승리 팀: §f" + winningTeam.getDisplayName());
            Bukkit.broadcastMessage("§7생존 인원: " + maxPlayers + "명");
        } else {
            Bukkit.broadcastMessage("§7무승부!");
        }
        
        Bukkit.broadcastMessage("§6§l======================");
        
        // Teleport all players to lobby
        teleportAllToLobby();
    }
    
    /**
     * Announce solo mode winner
     */
    private void announceSoloWinner() {
        Player winner = null;
        int aliveCount = 0;
        
        // Find alive players (Adventure mode only)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                winner = player;
                aliveCount++;
            }
        }
        
        // Announce winner
        Bukkit.broadcastMessage("§6§l======================");
        Bukkit.broadcastMessage("§a§l게임 종료!");
        
        if (aliveCount == 1 && winner != null) {
            Bukkit.broadcastMessage("§e§l승리자: §f" + winner.getName());
        } else if (aliveCount > 1) {
            Bukkit.broadcastMessage("§7생존자: " + aliveCount + "명");
        } else {
            Bukkit.broadcastMessage("§7승자 없음!");
        }
        
        Bukkit.broadcastMessage("§6§l======================");
        
        // Teleport all players to lobby
        teleportAllToLobby();
    }
    
    /**
     * Check win condition during the game
     * Called after each player death to see if game should end
     */
    public void checkWinCondition() {
        if (!gameActive) {
            return;
        }
        
        Bukkit.getLogger().info("[ChaosPVP] Checking win condition, game mode: " + gameMode);
        
        if (gameMode == GameMode.TEAM) {
            checkTeamWinCondition();
        } else {
            checkSoloWinCondition();
        }
    }
    
    /**
     * Check win condition for team mode
     */
    private void checkTeamWinCondition() {
        Map<Team, Integer> teamCounts = new HashMap<>();
        int playersWithoutTeam = 0;
        
        // Count alive players per team (Adventure mode only)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                Team team = teamManager.getPlayerTeam(player);
                if (team != null) {
                    teamCounts.put(team, teamCounts.getOrDefault(team, 0) + 1);
                } else {
                    playersWithoutTeam++;
                }
            }
        }
        
        Bukkit.getLogger().info("[ChaosPVP] Team mode - Team counts: " + teamCounts + ", Players without team: " + playersWithoutTeam);
        
        // If there are players without team, treat as solo mode
        if (playersWithoutTeam > 0 && teamCounts.isEmpty()) {
            Bukkit.getLogger().info("[ChaosPVP] Team mode but no teams assigned, switching to solo check");
            checkSoloWinCondition();
            return;
        }
        
        // Find winning team
        int teamsWithPlayers = 0;
        Team winningTeam = null;
        int winningTeamCount = 0;
        
        for (Map.Entry<Team, Integer> entry : teamCounts.entrySet()) {
            if (entry.getValue() > 0) {
                teamsWithPlayers++;
                winningTeam = entry.getKey();
                winningTeamCount = entry.getValue();
            }
        }
        
        Bukkit.getLogger().info("[ChaosPVP] Team mode - Teams with players: " + teamsWithPlayers);
        
        // If only one team has players (or no teams), they win
        if (teamsWithPlayers == 1 && winningTeam != null) {
            Bukkit.getLogger().info("[ChaosPVP] Team mode - Winning team: " + winningTeam.getDisplayName());
            Bukkit.broadcastMessage("§6§l======================");
            Bukkit.broadcastMessage("§a§l게임 종료!");
            Bukkit.broadcastMessage("§e§l승리 팀: §f" + winningTeam.getDisplayName());
            Bukkit.broadcastMessage("§7생존 인원: " + winningTeamCount + "명");
            Bukkit.broadcastMessage("§6§l======================");
            
            // End game
            endGameImmediately();
        } else if (teamsWithPlayers == 0) {
            // No survivors
            Bukkit.getLogger().info("[ChaosPVP] Team mode - No survivors");
            Bukkit.broadcastMessage("§6§l======================");
            Bukkit.broadcastMessage("§a§l게임 종료!");
            Bukkit.broadcastMessage("§7승자 없음!");
            Bukkit.broadcastMessage("§6§l======================");
            
            // End game
            endGameImmediately();
        } else {
            Bukkit.getLogger().info("[ChaosPVP] Team mode - Game continues with " + teamsWithPlayers + " teams");
        }
        // If more than 1 team has players, game continues
    }
    
    /**
     * Check win condition for solo mode
     */
    private void checkSoloWinCondition() {
        Player winner = null;
        int aliveCount = 0;
        
        // Find alive players (Adventure mode only)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                winner = player;
                aliveCount++;
            }
        }
        
        Bukkit.getLogger().info("[ChaosPVP] Solo mode - Alive players: " + aliveCount);
        
        // If only one player alive (or less), they win
        if (aliveCount == 1 && winner != null) {
            Bukkit.getLogger().info("[ChaosPVP] Solo mode - Winner: " + winner.getName());
            Bukkit.broadcastMessage("§6§l======================");
            Bukkit.broadcastMessage("§a§l게임 종료!");
            Bukkit.broadcastMessage("§e§l승리자: §f" + winner.getName());
            Bukkit.broadcastMessage("§6§l======================");
            
            // End game
            endGameImmediately();
        } else if (aliveCount == 0) {
            // No survivors
            Bukkit.getLogger().info("[ChaosPVP] Solo mode - No survivors");
            Bukkit.broadcastMessage("§6§l======================");
            Bukkit.broadcastMessage("§a§l게임 종료!");
            Bukkit.broadcastMessage("§7승자 없음!");
            Bukkit.broadcastMessage("§6§l======================");
            
            // End game
            endGameImmediately();
        } else {
            Bukkit.getLogger().info("[ChaosPVP] Solo mode - Game continues with " + aliveCount + " players");
        }
        // If more than 1 player, game continues
    }
    
    /**
     * End game immediately without checking win conditions again
     */
    private void endGameImmediately() {
        // Set all flags to false
        gameActive = false;
        countdownActive = false;
        monitoringClassSelection = false;
        
        // Cancel tasks
        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }
        
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }
        
        if (classSelectionMonitorTask != null) {
            classSelectionMonitorTask.cancel();
            classSelectionMonitorTask = null;
        }
        
        // Reset game time
        gameTimeRemaining = 0;
        
        // Remove boss bar
        removeTimeBossBar();
        
        // Clean up class-specific effects
        cleanupClassEffects();
        
        // Restore world settings
        restoreWorldSettings();
        
        // Restore player states and remove effects
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeGameEffects(player);
            restorePlayerState(player);
            
            // Remove glowing effect
            player.setGlowing(false);
            
            // Clear all resources (including Scientist's Ultimate Ingredient stacks)
            VerminPVP pluginInstance = (VerminPVP) plugin;
            if (pluginInstance.getResourceTracker() != null) {
                pluginInstance.getResourceTracker().clearResources(player.getUniqueId());
            }
        }
        
        // CRITICAL: Clear all player classes to prevent duplicate class issues in next game
        for (Player player : Bukkit.getOnlinePlayers()) {
            classManager.clearPlayerClass(player);
        }
        
        // Teleport all players to lobby
        teleportAllToLobby();
    }
    
    /**
     * Check if game is currently active
     */
    public boolean isGameActive() {
        return gameActive;
    }
    
    /**
     * Check if monitoring class selection (game is starting)
     */
    public boolean isMonitoringClassSelection() {
        return monitoringClassSelection;
    }
    
    /**
     * Get remaining game time in seconds
     */
    public int getGameTimeRemaining() {
        return gameTimeRemaining;
    }
    
    /**
     * Set practice mode status for a player
     */
    public void setPracticeMode(Player player, boolean inPracticeMode) {
        UUID playerId = player.getUniqueId();
        
        if (inPracticeMode) {
            practiceModeStatus.put(playerId, true);
            
            // Start 5-minute timer with boss bar
            startPracticeModeTimer(player);
        } else {
            practiceModeStatus.remove(playerId);
            
            // Cancel timer if exists
            BukkitTask timer = practiceModeTimers.remove(playerId);
            if (timer != null) {
                timer.cancel();
            }
            
            // Remove time remaining
            practiceModeTimeRemaining.remove(playerId);
            
            // Remove boss bar
            org.bukkit.boss.BossBar bossBar = practiceModeBossBars.remove(playerId);
            if (bossBar != null) {
                bossBar.removePlayer(player);
                bossBar.setVisible(false);
            }
        }
    }
    
    /**
     * Start practice mode timer (5 minutes) with boss bar
     */
    private void startPracticeModeTimer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing timer if any
        BukkitTask existingTimer = practiceModeTimers.get(playerId);
        if (existingTimer != null) {
            existingTimer.cancel();
        }
        
        // Remove existing boss bar if any
        org.bukkit.boss.BossBar existingBossBar = practiceModeBossBars.get(playerId);
        if (existingBossBar != null) {
            existingBossBar.removePlayer(player);
            existingBossBar.setVisible(false);
        }
        
        // Initialize time remaining
        practiceModeTimeRemaining.put(playerId, PRACTICE_MODE_DURATION);
        
        // Create boss bar
        org.bukkit.boss.BossBar bossBar = Bukkit.createBossBar(
            "§a§l연습모드",
            org.bukkit.boss.BarColor.GREEN,
            org.bukkit.boss.BarStyle.SOLID
        );
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
        practiceModeBossBars.put(playerId, bossBar);
        
        // Update boss bar immediately
        updatePracticeModeBossBar(player);
        
        // Start timer that updates every second
        BukkitTask timer = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isInPracticeMode(player)) {
                // Player logged out or left practice mode
                BukkitTask task = practiceModeTimers.remove(playerId);
                if (task != null) {
                    task.cancel();
                }
                practiceModeTimeRemaining.remove(playerId);
                org.bukkit.boss.BossBar bar = practiceModeBossBars.remove(playerId);
                if (bar != null) {
                    bar.removePlayer(player);
                    bar.setVisible(false);
                }
                return;
            }
            
            // Decrease time
            Integer timeLeft = practiceModeTimeRemaining.get(playerId);
            if (timeLeft == null) {
                timeLeft = PRACTICE_MODE_DURATION;
            }
            timeLeft--;
            practiceModeTimeRemaining.put(playerId, timeLeft);
            
            // Update boss bar
            updatePracticeModeBossBar(player);
            
            // Check if time is up
            if (timeLeft <= 0) {
                // Auto-end practice mode
                endPracticeModeForPlayer(player);
                
                // Teleport back to practice map if it exists
                if (mapManager != null && mapManager.hasPracticeMap()) {
                    org.bukkit.Location practiceMap = mapManager.getPracticeMap();
                    player.teleport(practiceMap);
                    player.sendMessage("§e연습모드 시간이 종료되었습니다!");
                    player.sendMessage("§7연습모드 맵으로 돌아왔습니다.");
                } else {
                    player.sendMessage("§e연습모드 시간이 종료되었습니다!");
                }
                
                // Remove timer
                BukkitTask task = practiceModeTimers.remove(playerId);
                if (task != null) {
                    task.cancel();
                }
            }
        }, 20L, 20L); // Run every second
        
        practiceModeTimers.put(playerId, timer);
    }
    
    /**
     * Update practice mode boss bar for a player
     */
    private void updatePracticeModeBossBar(Player player) {
        UUID playerId = player.getUniqueId();
        org.bukkit.boss.BossBar bossBar = practiceModeBossBars.get(playerId);
        Integer timeLeft = practiceModeTimeRemaining.get(playerId);
        
        if (bossBar == null || timeLeft == null) {
            return;
        }
        
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        
        // Update title
        String title = String.format("§e남은 시간: §f%d:%02d", minutes, seconds);
        bossBar.setTitle(title);
        
        // Update progress (0.0 to 1.0)
        double progress = (double) timeLeft / PRACTICE_MODE_DURATION;
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        
        // Change color based on time remaining
        if (timeLeft <= 30) {
            bossBar.setColor(org.bukkit.boss.BarColor.RED);
        } else if (timeLeft <= 60) {
            bossBar.setColor(org.bukkit.boss.BarColor.YELLOW);
        } else {
            bossBar.setColor(org.bukkit.boss.BarColor.GREEN);
        }
    }
    
    /**
     * End practice mode for a specific player
     */
    private void endPracticeModeForPlayer(Player player) {
        VerminPVP pluginInstance = (VerminPVP) plugin;
        
        // Clean up all class-specific effects for this player
        if (pluginInstance.getPlagueSpreaderHandler() != null) {
            pluginInstance.getPlagueSpreaderHandler().cleanupPlayer(player);
        }
        if (pluginInstance.getShieldSoldierHandler() != null) {
            pluginInstance.getShieldSoldierHandler().cleanupPlayer(player);
        }
        if (pluginInstance.getShapeshifterHandler() != null) {
            pluginInstance.getShapeshifterHandler().cleanupPlayer(player);
        }
        if (pluginInstance.getJugglerHandler() != null) {
            pluginInstance.getJugglerHandler().cleanupPlayer(player);
        }
        
        // Clear player class
        classManager.clearPlayerClass(player);
        
        // Clear inventory
        player.getInventory().clear();
        
        // Remove ALL potion effects
        for (org.bukkit.potion.PotionEffectType effectType : org.bukkit.potion.PotionEffectType.values()) {
            if (effectType != null && player.hasPotionEffect(effectType)) {
                player.removePotionEffect(effectType);
            }
        }
        
        // Remove game effects
        removeGameEffects(player);
        
        // Remove practice mode flag (this will also remove boss bar and timer)
        setPracticeMode(player, false);
        
        // Set to creative mode (original state)
        player.setGameMode(org.bukkit.GameMode.CREATIVE);
        
        // Restore max health to 10 hearts (20 HP)
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
        
        // Re-enable natural regeneration
        if (player.getWorld() != null) {
            player.getWorld().setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, true);
        }
    }
    
    /**
     * Check if a player is in practice mode
     */
    public boolean isInPracticeMode(Player player) {
        return practiceModeStatus.getOrDefault(player.getUniqueId(), false);
    }
    
    /**
     * Get practice mode time remaining for a player (in seconds)
     */
    public int getPracticeModeTimeRemaining(Player player) {
        Integer timeLeft = practiceModeTimeRemaining.get(player.getUniqueId());
        return timeLeft != null ? timeLeft : 0;
    }
    
    /**
     * Start class schedulers for a player in practice mode
     * This makes practice mode work exactly like the real game
     */
    public void startPracticeModeSchedulers(Player player) {
        // Get player's class
        ClassType playerClass = classManager.getPlayerClass(player);
        if (playerClass == null) {
            return;
        }
        
        // Start class-specific schedulers for THIS player only
        classManager.startClassSchedulers(player, playerClass);
    }
    
    /**
     * Check if a player is on practice mode cooldown
     */
    public boolean isOnPracticeModeCooldown(Player player) {
        Long cooldownEndTime = practiceModeCooldowns.get(player.getUniqueId());
        if (cooldownEndTime == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime >= cooldownEndTime) {
            // Cooldown expired, remove it
            practiceModeCooldowns.remove(player.getUniqueId());
            return false;
        }
        
        return true;
    }
    
    /**
     * Get remaining practice mode cooldown in seconds
     */
    public int getPracticeModeCooldownRemaining(Player player) {
        Long cooldownEndTime = practiceModeCooldowns.get(player.getUniqueId());
        if (cooldownEndTime == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long remainingMillis = cooldownEndTime - currentTime;
        
        if (remainingMillis <= 0) {
            practiceModeCooldowns.remove(player.getUniqueId());
            return 0;
        }
        
        return (int) (remainingMillis / 1000L);
    }
}

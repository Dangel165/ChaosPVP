package com.verminpvp.managers;

import com.verminpvp.VerminPVP;
import com.verminpvp.gui.ClassSelectionGUI;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages draft pick system for team mode
 * Players take turns selecting classes in alternating team order
 */
public class DraftPickManager {
    
    private final VerminPVP plugin;
    private final TeamManager teamManager;
    private final ClassManager classManager;
    private final ClassSelectionGUI classSelectionGUI;
    private GameManager gameManager;
    
    private boolean draftActive = false;
    private List<UUID> pickOrder; // Order of players to pick
    private int currentPickIndex = 0;
    private BukkitTask timerTask;
    private int timeRemaining = 30; // 30 seconds per pick
    
    private static final int PICK_TIME_LIMIT = 30; // seconds
    
    public DraftPickManager(VerminPVP plugin, TeamManager teamManager, ClassManager classManager, ClassSelectionGUI classSelectionGUI) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.classManager = classManager;
        this.classSelectionGUI = classSelectionGUI;
    }
    
    /**
     * Set the GameManager (called after initialization)
     */
    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    /**
     * Start the draft pick process
     */
    public void startDraftPick() {
        if (draftActive) {
            return;
        }
        
        // Build pick order: RED, BLUE, RED, BLUE, ...
        pickOrder = buildPickOrder();
        
        if (pickOrder.isEmpty()) {
            plugin.getLogger().warning("No players available for draft pick!");
            return;
        }
        
        draftActive = true;
        currentPickIndex = 0;
        
        // Announce draft pick start
        Bukkit.broadcastMessage("§6§l========================================");
        Bukkit.broadcastMessage("§e§l드래프트 픽 시작!");
        Bukkit.broadcastMessage("§7순서대로 클래스를 선택하세요.");
        Bukkit.broadcastMessage("§7같은 팀 내에서 클래스 중복 불가!");
        Bukkit.broadcastMessage("§6§l========================================");
        
        // Start first pick
        Bukkit.getScheduler().runTaskLater(plugin, this::startNextPick, 40L); // 2 second delay
    }
    
    /**
     * Build the pick order alternating between teams
     */
    private List<UUID> buildPickOrder() {
        List<UUID> order = new ArrayList<>();
        
        List<UUID> redPlayers = new ArrayList<>();
        List<UUID> bluePlayers = new ArrayList<>();
        
        // Separate players by team
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = teamManager.getPlayerTeam(player);
            if (team == Team.RED) {
                redPlayers.add(player.getUniqueId());
            } else if (team == Team.BLUE) {
                bluePlayers.add(player.getUniqueId());
            }
        }
        
        // Alternate between teams
        int maxSize = Math.max(redPlayers.size(), bluePlayers.size());
        for (int i = 0; i < maxSize; i++) {
            if (i < redPlayers.size()) {
                order.add(redPlayers.get(i));
            }
            if (i < bluePlayers.size()) {
                order.add(bluePlayers.get(i));
            }
        }
        
        return order;
    }
    
    /**
     * Start the next player's pick
     */
    private void startNextPick() {
        if (!draftActive || currentPickIndex >= pickOrder.size()) {
            completeDraftPick();
            return;
        }
        
        UUID currentPlayerId = pickOrder.get(currentPickIndex);
        Player currentPlayer = Bukkit.getPlayer(currentPlayerId);
        
        // Skip if player is offline or already has a class
        if (currentPlayer == null || !currentPlayer.isOnline()) {
            plugin.getLogger().info("Player " + currentPlayerId + " is offline, skipping...");
            currentPickIndex++;
            startNextPick();
            return;
        }
        
        if (classManager.getPlayerClass(currentPlayer) != null) {
            plugin.getLogger().info("Player " + currentPlayer.getName() + " already has a class, skipping...");
            currentPickIndex++;
            startNextPick();
            return;
        }
        
        Team team = teamManager.getPlayerTeam(currentPlayer);
        String teamColor;
        String teamName;
        
        if (team == Team.RED) {
            teamColor = "§c";
            teamName = "RED";
        } else if (team == Team.BLUE) {
            teamColor = "§9";
            teamName = "BLUE";
        } else {
            // Fallback for null team (shouldn't happen in team mode)
            teamColor = "§7";
            teamName = "UNKNOWN";
            plugin.getLogger().warning("Player " + currentPlayer.getName() + " has no team assigned!");
        }
        
        // Announce current picker
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l========================================");
        Bukkit.broadcastMessage(teamColor + "§l" + teamName + " 팀 §f" + currentPlayer.getName() + "§e§l님의 차례!");
        Bukkit.broadcastMessage("§7" + (currentPickIndex + 1) + "/" + pickOrder.size() + " 번째 픽");
        Bukkit.broadcastMessage("§6§l========================================");
        Bukkit.broadcastMessage("");
        
        // Play sound to current player
        currentPlayer.playSound(currentPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        
        // Open GUI for current player
        classSelectionGUI.openGUI(currentPlayer);
        
        // Show waiting message to other players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(currentPlayer)) {
                player.sendMessage("§7" + currentPlayer.getName() + "님이 클래스를 선택하는 중...");
            }
        }
        
        // Start timer
        timeRemaining = PICK_TIME_LIMIT;
        startPickTimer(currentPlayer);
    }
    
    /**
     * Start the pick timer for current player
     */
    private void startPickTimer(Player player) {
        if (timerTask != null) {
            timerTask.cancel();
        }
        
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!draftActive) {
                if (timerTask != null) {
                    timerTask.cancel();
                    timerTask = null;
                }
                return;
            }
            
            // Check if player selected a class
            if (classManager.getPlayerClass(player) != null) {
                // Player selected, move to next
                if (timerTask != null) {
                    timerTask.cancel();
                    timerTask = null;
                }
                
                currentPickIndex++;
                Bukkit.getScheduler().runTaskLater(plugin, this::startNextPick, 20L); // 1 second delay
                return;
            }
            
            timeRemaining--;
            
            // Show timer to current player
            if (timeRemaining > 0) {
                if (timeRemaining <= 5) {
                    player.sendTitle("§c§l" + timeRemaining, "§7빨리 선택하세요!", 0, 25, 5);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                } else if (timeRemaining % 5 == 0) {
                    player.sendMessage("§e남은 시간: §f" + timeRemaining + "초");
                }
            } else {
                // Time's up! Auto-select random class
                if (timerTask != null) {
                    timerTask.cancel();
                    timerTask = null;
                }
                
                autoSelectClass(player);
                
                currentPickIndex++;
                Bukkit.getScheduler().runTaskLater(plugin, this::startNextPick, 20L); // 1 second delay
            }
        }, 0L, 20L); // Run every second
    }
    
    /**
     * Auto-select a random available class for the player
     */
    private void autoSelectClass(Player player) {
        Team team = teamManager.getPlayerTeam(player);
        List<ClassType> availableClasses = new ArrayList<>();
        
        // Find available classes for this team
        for (ClassType classType : ClassType.values()) {
            if (!classManager.isClassTakenByTeam(classType, team)) {
                availableClasses.add(classType);
            }
        }
        
        if (availableClasses.isEmpty()) {
            // This shouldn't happen, but just in case
            player.sendMessage("§c사용 가능한 클래스가 없습니다!");
            Bukkit.broadcastMessage("§c" + player.getName() + "님은 사용 가능한 클래스가 없어 선택하지 못했습니다!");
            return;
        }
        
        // Select random class
        ClassType randomClass = availableClasses.get(new Random().nextInt(availableClasses.size()));
        classManager.setPlayerClass(player, randomClass);
        
        player.closeInventory();
        player.sendMessage("§c시간 초과! §e랜덤으로 §f" + randomClass.getDisplayName() + " §e클래스가 선택되었습니다!");
        Bukkit.broadcastMessage("§7" + player.getName() + "님이 §f" + randomClass.getDisplayName() + " §7클래스를 선택했습니다! (자동 선택)");
    }
    
    /**
     * Complete the draft pick process
     */
    private void completeDraftPick() {
        draftActive = false;
        
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        
        // Announce completion
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l========================================");
        Bukkit.broadcastMessage("§a§l드래프트 픽 완료!");
        Bukkit.broadcastMessage("§7모든 플레이어가 클래스를 선택했습니다.");
        Bukkit.broadcastMessage("§6§l========================================");
        Bukkit.broadcastMessage("");
        
        // Show team compositions
        showTeamCompositions();
        
        // Start game countdown
        if (gameManager != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                gameManager.startCountdownAndGame();
            }, 60L); // 3 second delay
        }
    }
    
    /**
     * Show team compositions after draft pick
     */
    private void showTeamCompositions() {
        Bukkit.broadcastMessage("§c§l=== RED 팀 ===");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (teamManager.getPlayerTeam(player) == Team.RED) {
                ClassType classType = classManager.getPlayerClass(player);
                if (classType != null) {
                    Bukkit.broadcastMessage("§f" + player.getName() + " §7- §e" + classType.getDisplayName());
                }
            }
        }
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§9§l=== BLUE 팀 ===");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (teamManager.getPlayerTeam(player) == Team.BLUE) {
                ClassType classType = classManager.getPlayerClass(player);
                if (classType != null) {
                    Bukkit.broadcastMessage("§f" + player.getName() + " §7- §e" + classType.getDisplayName());
                }
            }
        }
        
        Bukkit.broadcastMessage("");
    }
    
    /**
     * Check if draft pick is active
     */
    public boolean isDraftActive() {
        return draftActive;
    }
    
    /**
     * Cancel the draft pick process
     */
    public void cancelDraftPick() {
        if (!draftActive) {
            return;
        }
        
        draftActive = false;
        
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        
        Bukkit.broadcastMessage("§c드래프트 픽이 취소되었습니다!");
    }
    
    /**
     * Get the current picking player
     */
    public Player getCurrentPicker() {
        if (!draftActive || currentPickIndex >= pickOrder.size()) {
            return null;
        }
        
        UUID playerId = pickOrder.get(currentPickIndex);
        return Bukkit.getPlayer(playerId);
    }
    
    /**
     * Get the plugin instance
     */
    public VerminPVP getPlugin() {
        return plugin;
    }
}

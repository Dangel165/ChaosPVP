package com.verminpvp.commands;

import com.verminpvp.VerminPVP;
import com.verminpvp.gui.ClassBanVoteGUI;
import com.verminpvp.gui.ClassSelectionGUI;
import com.verminpvp.gui.MapVoteGUI;
import com.verminpvp.gui.TeamSelectionGUI;
import com.verminpvp.managers.ClassBanManager;
import com.verminpvp.managers.DraftPickManager;
import com.verminpvp.managers.ExcludeManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.LobbyManager;
import com.verminpvp.managers.MapManager;
import com.verminpvp.models.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Command to open the team/class selection GUI for all players and start the game
 * Korean command: /게임시작
 */
public class StartGameCommand implements CommandExecutor {
    
    private final Plugin plugin;
    private final ClassSelectionGUI classSelectionGUI;
    private final TeamSelectionGUI teamSelectionGUI;
    private final MapVoteGUI mapVoteGUI;
    private final ClassBanVoteGUI classBanVoteGUI;
    private final GameManager gameManager;
    private final ExcludeManager excludeManager;
    private final MapManager mapManager;
    private final LobbyManager lobbyManager;
    private final DraftPickManager draftPickManager;
    private final ClassBanManager classBanManager;
    
    public StartGameCommand(Plugin plugin, ClassSelectionGUI classSelectionGUI, 
                           TeamSelectionGUI teamSelectionGUI, MapVoteGUI mapVoteGUI,
                           ClassBanVoteGUI classBanVoteGUI, GameManager gameManager, 
                           ExcludeManager excludeManager, MapManager mapManager, 
                           LobbyManager lobbyManager, DraftPickManager draftPickManager,
                           ClassBanManager classBanManager) {
        this.plugin = plugin;
        this.classSelectionGUI = classSelectionGUI;
        this.teamSelectionGUI = teamSelectionGUI;
        this.mapVoteGUI = mapVoteGUI;
        this.classBanVoteGUI = classBanVoteGUI;
        this.gameManager = gameManager;
        this.excludeManager = excludeManager;
        this.mapManager = mapManager;
        this.lobbyManager = lobbyManager;
        this.draftPickManager = draftPickManager;
        this.classBanManager = classBanManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if lobby is configured
        if (!lobbyManager.hasLobbyLocation()) {
            sender.sendMessage("§c로비가 지정되지 않았습니다!");
            sender.sendMessage("§e/로비 지정 명령어로 로비를 먼저 지정해주세요.");
            return true;
        }
        
        // Check if both map slots are configured
        if (!mapManager.hasSlot1Map() || !mapManager.hasSlot2Map()) {
            sender.sendMessage("§c맵 슬롯이 설정되지 않았습니다!");
            if (!mapManager.hasSlot1Map()) {
                sender.sendMessage("§e/1번맵지정 [맵이름] 명령어로 1번 맵을 설정해주세요.");
            }
            if (!mapManager.hasSlot2Map()) {
                sender.sendMessage("§e/2번맵지정 [맵이름] 명령어로 2번 맵을 설정해주세요.");
            }
            return true;
        }
        
        // Check if both map slots have spawn points
        if (mapManager.getSlot1SpawnCount() == 0 || mapManager.getSlot2SpawnCount() == 0) {
            sender.sendMessage("§c맵 스폰 포인트가 설정되지 않았습니다!");
            if (mapManager.getSlot1SpawnCount() == 0) {
                sender.sendMessage("§e/1번스폰추가 명령어로 1번 맵 스폰을 추가해주세요.");
            }
            if (mapManager.getSlot2SpawnCount() == 0) {
                sender.sendMessage("§e/2번스폰추가 명령어로 2번 맵 스폰을 추가해주세요.");
            }
            return true;
        }
        
        // Count non-excluded players
        int playerCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!excludeManager.isExcluded(player)) {
                playerCount++;
            }
        }
        
        // Check minimum players for team mode
        if (gameManager.getGameMode() == GameMode.TEAM && playerCount < 2) {
            sender.sendMessage("§c팀 모드는 최소 2명 이상의 플레이어가 필요합니다!");
            sender.sendMessage("§7현재 플레이어: §f" + playerCount + "명");
            sender.sendMessage("§e개인전 모드로 변경하려면: §f/게임모드 개인전");
            return true;
        }
        
        // Teleport all players to lobby first and apply adventure mode + remove OP
        org.bukkit.Location lobbyLocation = lobbyManager.getLobbyLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip excluded players
            if (excludeManager.isExcluded(player)) {
                continue;
            }
            
            // If player is in practice mode, end it
            if (gameManager.isInPracticeMode(player)) {
                player.sendMessage("§e연습모드가 자동으로 종료되었습니다.");
                
                // Clean up all class-specific effects
                cleanupPlayerClassEffects(player);
                
                // Clear player class (this will also stop schedulers)
                VerminPVP pluginInstance = (VerminPVP) plugin;
                pluginInstance.getClassManager().clearPlayerClass(player);
                
                // Clear inventory
                player.getInventory().clear();
                
                // Remove ALL potion effects
                for (org.bukkit.potion.PotionEffectType effectType : org.bukkit.potion.PotionEffectType.values()) {
                    if (effectType != null && player.hasPotionEffect(effectType)) {
                        player.removePotionEffect(effectType);
                    }
                }
                
                // Remove game effects
                gameManager.removeGameEffects(player);
                
                // Remove practice mode flag
                gameManager.setPracticeMode(player, false);
                
                // Restore max health to default (20 HP = 10 hearts)
                player.setMaxHealth(20.0);
                player.setHealth(20.0);
            }
            
            // Set to adventure mode
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            
            // Save and remove OP
            gameManager.saveOriginalOpStatus(player);
            if (player.isOp()) {
                player.setOp(false);
                player.sendMessage("§7게임 중에는 OP 권한이 제거됩니다.");
            }
            
            // Teleport to lobby
            player.teleport(lobbyLocation);
        }
        
        Bukkit.broadcastMessage("§a모든 플레이어가 로비로 이동했습니다!");
        
        // Wait a moment before opening GUIs
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Always show map voting if both slots are set (regardless of game mode)
            if (mapManager.hasBothMapSlots()) {
                // Start map voting first
                startMapVoting();
            } else {
                // No map voting, proceed directly to team/class selection
                proceedToTeamOrClassSelection();
            }
        }, 20L); // Wait 1 second after teleport
        
        return true;
    }
    
    /**
     * Start map voting phase
     */
    private void startMapVoting() {
        Bukkit.broadcastMessage("§e맵 투표를 시작합니다!");
        
        // Reset voting data
        mapVoteGUI.resetVoting();
        
        // Set callback for when voting is complete
        mapVoteGUI.setOnVoteComplete(() -> {
            // After map voting, start class ban voting
            startClassBanVoting();
        });
        
        // Open map vote GUI for all non-excluded players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!excludeManager.isExcluded(player)) {
                mapVoteGUI.openGUI(player);
            }
        }
    }
    
    /**
     * Start class ban voting phase
     */
    private void startClassBanVoting() {
        Bukkit.broadcastMessage("§c클래스 밴 투표를 시작합니다!");
        Bukkit.broadcastMessage("§7밴할 클래스 1개를 투표로 선택합니다.");
        
        // Reset voting data
        classBanVoteGUI.resetVoting();
        
        // Clear previous banned class
        gameManager.clearBannedClass();
        
        // Set callback for when voting is complete
        classBanVoteGUI.setOnVoteComplete(() -> {
            // Set the banned class in GameManager
            gameManager.setBannedClass(classBanManager.getBannedClass());
            
            // Proceed to team or class selection
            proceedToTeamOrClassSelection();
        });
        
        // Open class ban vote GUI for all non-excluded players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!excludeManager.isExcluded(player)) {
                classBanVoteGUI.openGUI(player);
            }
        }
    }
    
    /**
     * Proceed to team selection (team mode) or class selection (solo mode)
     */
    private void proceedToTeamOrClassSelection() {
        // Check game mode and open appropriate GUI
        if (gameManager.getGameMode() == GameMode.TEAM) {
            // Team mode: open team selection GUI first, then start draft pick
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!excludeManager.isExcluded(player)) {
                    teamSelectionGUI.openGUI(player);
                }
            }
            
            // After team selection, start draft pick
            // TeamSelectionGUI will call startDraftPickAfterTeamSelection() when all teams are selected
        } else {
            // Solo mode: Start monitoring BEFORE opening GUIs
            // This ensures the monitoring flag is set when players open the GUI
            gameManager.startClassSelectionMonitoring();
            
            // Then open class selection GUI directly for all players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!excludeManager.isExcluded(player)) {
                    classSelectionGUI.openGUI(player);
                }
            }
            
            // Broadcast message to select classes
            Bukkit.broadcastMessage("§e클래스를 선택하세요!");
            Bukkit.broadcastMessage("§7모든 플레이어가 선택하면 게임이 시작됩니다.");
        }
    }
    
    /**
     * Start draft pick after team selection is complete (called by TeamSelectionGUI)
     */
    public void startDraftPickAfterTeamSelection() {
        // Start draft pick system
        draftPickManager.startDraftPick();
    }
    
    /**
     * Get the DraftPickManager
     */
    public DraftPickManager getDraftPickManager() {
        return draftPickManager;
    }
    
    /**
     * Clean up class-specific effects for a single player
     */
    private void cleanupPlayerClassEffects(Player player) {
        VerminPVP pluginInstance = (VerminPVP) plugin;
        
        // Clean up Plague Spreader poison fields for this player
        if (pluginInstance.getPlagueSpreaderHandler() != null) {
            pluginInstance.getPlagueSpreaderHandler().cleanupPlayer(player);
        }
        
        // Clean up Shield Soldier passive tasks for this player
        if (pluginInstance.getShieldSoldierHandler() != null) {
            pluginInstance.getShieldSoldierHandler().cleanupPlayer(player);
        }
        
        // Clean up Shapeshifter evolution tasks and disguises for this player
        if (pluginInstance.getShapeshifterHandler() != null) {
            pluginInstance.getShapeshifterHandler().cleanupPlayer(player);
        }
        
        // Clean up Juggler throw time gain tasks for this player
        if (pluginInstance.getJugglerHandler() != null) {
            pluginInstance.getJugglerHandler().cleanupPlayer(player);
        }
    }
}

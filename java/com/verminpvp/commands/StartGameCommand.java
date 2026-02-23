package com.verminpvp.commands;

import com.verminpvp.VerminPVP;
import com.verminpvp.gui.ClassSelectionGUI;
import com.verminpvp.gui.TeamSelectionGUI;
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
    private final GameManager gameManager;
    private final ExcludeManager excludeManager;
    private final MapManager mapManager;
    private final LobbyManager lobbyManager;
    private final DraftPickManager draftPickManager;
    
    public StartGameCommand(Plugin plugin, ClassSelectionGUI classSelectionGUI, 
                           TeamSelectionGUI teamSelectionGUI, GameManager gameManager,
                           ExcludeManager excludeManager, MapManager mapManager,
                           LobbyManager lobbyManager, DraftPickManager draftPickManager) {
        this.plugin = plugin;
        this.classSelectionGUI = classSelectionGUI;
        this.teamSelectionGUI = teamSelectionGUI;
        this.gameManager = gameManager;
        this.excludeManager = excludeManager;
        this.mapManager = mapManager;
        this.lobbyManager = lobbyManager;
        this.draftPickManager = draftPickManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if lobby is configured
        if (!lobbyManager.hasLobbyLocation()) {
            sender.sendMessage("§c로비가 지정되지 않았습니다!");
            sender.sendMessage("§e/로비 지정 명령어로 로비를 먼저 지정해주세요.");
            return true;
        }
        
        // Check if map is configured
        if (!mapManager.hasSpawnLocations()) {
            sender.sendMessage("§c맵이 지정되지 않았습니다!");
            sender.sendMessage("§e/맵지정 명령어로 맵을 먼저 지정해주세요.");
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
        }, 20L); // Wait 1 second after teleport
        
        return true;
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

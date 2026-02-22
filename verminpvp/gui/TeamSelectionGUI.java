package com.verminpvp.gui;

import com.verminpvp.commands.StartGameCommand;
import com.verminpvp.managers.ExcludeManager;
import com.verminpvp.managers.TeamManager;
import com.verminpvp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for team selection (Blue Team / Red Team)
 */
public class TeamSelectionGUI implements Listener {
    
    private final TeamManager teamManager;
    private final ClassSelectionGUI classSelectionGUI;
    private final ExcludeManager excludeManager;
    private StartGameCommand startGameCommand;
    private static final String GUI_TITLE = "§6팀 선택";
    
    public TeamSelectionGUI(TeamManager teamManager, ClassSelectionGUI classSelectionGUI, ExcludeManager excludeManager) {
        this.teamManager = teamManager;
        this.classSelectionGUI = classSelectionGUI;
        this.excludeManager = excludeManager;
    }
    
    /**
     * Set the StartGameCommand (called after initialization)
     */
    public void setStartGameCommand(StartGameCommand startGameCommand) {
        this.startGameCommand = startGameCommand;
    }
    
    /**
     * Opens the team selection GUI for a player
     */
    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);
        
        // Blue Team (slot 11)
        ItemStack blueTeam = new ItemStack(Material.BLUE_WOOL);
        ItemMeta blueMeta = blueTeam.getItemMeta();
        blueMeta.setDisplayName("§9블루팀");
        List<String> blueLore = new ArrayList<>();
        blueLore.add("§7클릭하여 블루팀 선택");
        blueMeta.setLore(blueLore);
        blueTeam.setItemMeta(blueMeta);
        gui.setItem(11, blueTeam);
        
        // Red Team (slot 15)
        ItemStack redTeam = new ItemStack(Material.RED_WOOL);
        ItemMeta redMeta = redTeam.getItemMeta();
        redMeta.setDisplayName("§c레드팀");
        List<String> redLore = new ArrayList<>();
        redLore.add("§7클릭하여 레드팀 선택");
        redMeta.setLore(redLore);
        redTeam.setItemMeta(redMeta);
        gui.setItem(15, redTeam);
        
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Blue Team selected
        if (clicked.getType() == Material.BLUE_WOOL) {
            player.sendMessage("§7[디버그] BLUE_WOOL 클릭됨");
            teamManager.setPlayerTeam(player, Team.BLUE);
            player.sendMessage("§9블루팀에 합류했습니다!");
            player.closeInventory();
            
            // Check if all players have selected teams
            checkAllTeamsSelected();
        }
        // Red Team selected
        else if (clicked.getType() == Material.RED_WOOL) {
            player.sendMessage("§7[디버그] RED_WOOL 클릭됨");
            teamManager.setPlayerTeam(player, Team.RED);
            player.sendMessage("§c레드팀에 합류했습니다!");
            player.closeInventory();
            
            // Check if all players have selected teams
            checkAllTeamsSelected();
        }
    }
    
    /**
     * Check if all players have selected teams, and start draft pick if so
     */
    private void checkAllTeamsSelected() {
        // Count non-excluded players without teams
        int playersWithoutTeam = 0;
        int redTeamCount = 0;
        int blueTeamCount = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip excluded players
            if (excludeManager != null && excludeManager.isExcluded(player)) {
                continue;
            }
            
            Team team = teamManager.getPlayerTeam(player);
            if (team == null) {
                playersWithoutTeam++;
            } else if (team == Team.RED) {
                redTeamCount++;
            } else if (team == Team.BLUE) {
                blueTeamCount++;
            }
        }
        
        // If all non-excluded players have teams, check team balance
        if (playersWithoutTeam == 0 && startGameCommand != null) {
            // Check if both teams have at least one player
            if (redTeamCount == 0 || blueTeamCount == 0) {
                // Team imbalance detected
                String emptyTeam = redTeamCount == 0 ? "§c레드팀" : "§9블루팀";
                Bukkit.broadcastMessage("§c§l경고: " + emptyTeam + "§c§l에 플레이어가 없습니다!");
                Bukkit.broadcastMessage("§e팀 모드는 양쪽 팀에 최소 1명씩 필요합니다.");
                Bukkit.broadcastMessage("§7팀을 다시 선택해주세요.");
                
                // Reset all team selections
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (excludeManager != null && excludeManager.isExcluded(player)) {
                        continue;
                    }
                    teamManager.setPlayerTeam(player, null);
                }
                
                // Reopen team selection GUI for all players
                Bukkit.getScheduler().runTaskLater(
                    startGameCommand.getDraftPickManager().getPlugin(),
                    () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (excludeManager != null && excludeManager.isExcluded(player)) {
                                continue;
                            }
                            openGUI(player);
                        }
                    },
                    40L // 2 second delay
                );
                return;
            }
            
            // Both teams have players, proceed with draft pick
            Bukkit.broadcastMessage("§a모든 플레이어가 팀을 선택했습니다!");
            Bukkit.broadcastMessage("§7레드팀: §c" + redTeamCount + "명 §7| 블루팀: §9" + blueTeamCount + "명");
            Bukkit.broadcastMessage("§e드래프트 픽을 시작합니다...");
            
            // Start draft pick after a short delay
            Bukkit.getScheduler().runTaskLater(
                startGameCommand.getDraftPickManager().getPlugin(), 
                () -> startGameCommand.startDraftPickAfterTeamSelection(), 
                20L
            );
        }
    }
    
    /**
     * Get the plugin instance from DraftPickManager
     */
    private org.bukkit.plugin.Plugin getPlugin() {
        if (startGameCommand != null && startGameCommand.getDraftPickManager() != null) {
            return startGameCommand.getDraftPickManager().getPlugin();
        }
        return null;
    }
}

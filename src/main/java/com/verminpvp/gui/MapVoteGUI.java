package com.verminpvp.gui;

import com.verminpvp.managers.ExcludeManager;
import com.verminpvp.managers.MapManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUI for map voting system
 */
public class MapVoteGUI implements Listener {
    
    private final Plugin plugin;
    private final MapManager mapManager;
    private final ExcludeManager excludeManager;
    private final Map<UUID, Boolean> hasVoted;
    private final Map<Integer, Integer> voteCount; // 1 or 2 -> vote count
    private Runnable onVoteComplete;
    
    public MapVoteGUI(Plugin plugin, MapManager mapManager, ExcludeManager excludeManager) {
        this.plugin = plugin;
        this.mapManager = mapManager;
        this.excludeManager = excludeManager;
        this.hasVoted = new HashMap<>();
        this.voteCount = new HashMap<>();
        voteCount.put(1, 0);
        voteCount.put(2, 0);
    }
    
    /**
     * Set callback for when voting is complete
     */
    public void setOnVoteComplete(Runnable callback) {
        this.onVoteComplete = callback;
    }
    
    /**
     * Open map vote GUI for a player
     */
    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6§l맵 투표");
        
        // Slot 11: Map 1
        ItemStack map1 = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta map1Meta = map1.getItemMeta();
        if (map1Meta != null) {
            String map1Name = mapManager.getSlot1MapName();
            map1Meta.setDisplayName("§a§l1번 맵");
            if (map1Name != null && !map1Name.isEmpty()) {
                map1Meta.setLore(Arrays.asList(
                    "§7맵 이름: §f" + map1Name,
                    "",
                    "§e클릭하여 투표하기"
                ));
            } else {
                map1Meta.setLore(Arrays.asList(
                    "§7맵 이름: §f1번 맵",
                    "",
                    "§e클릭하여 투표하기"
                ));
            }
            map1.setItemMeta(map1Meta);
        }
        gui.setItem(11, map1);
        
        // Slot 15: Map 2
        ItemStack map2 = new ItemStack(Material.STONE);
        ItemMeta map2Meta = map2.getItemMeta();
        if (map2Meta != null) {
            String map2Name = mapManager.getSlot2MapName();
            map2Meta.setDisplayName("§c§l2번 맵");
            if (map2Name != null && !map2Name.isEmpty()) {
                map2Meta.setLore(Arrays.asList(
                    "§7맵 이름: §f" + map2Name,
                    "",
                    "§e클릭하여 투표하기"
                ));
            } else {
                map2Meta.setLore(Arrays.asList(
                    "§7맵 이름: §f2번 맵",
                    "",
                    "§e클릭하여 투표하기"
                ));
            }
            map2.setItemMeta(map2Meta);
        }
        gui.setItem(15, map2);
        
        player.openInventory(gui);
    }
    
    /**
     * Reset voting data
     */
    public void resetVoting() {
        hasVoted.clear();
        voteCount.put(1, 0);
        voteCount.put(2, 0);
    }
    
    /**
     * Check if all players have voted
     */
    private void checkVotingComplete() {
        // Count non-excluded players
        int totalPlayers = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!excludeManager.isExcluded(player)) {
                totalPlayers++;
            }
        }
        
        // Check if all players have voted
        if (hasVoted.size() >= totalPlayers && totalPlayers > 0) {
            // Determine winner
            int map1Votes = voteCount.get(1);
            int map2Votes = voteCount.get(2);
            
            Location selectedMap;
            String selectedMapName;
            
            if (map1Votes > map2Votes) {
                selectedMap = mapManager.getSlot1Map();
                selectedMapName = mapManager.getSlot1MapName();
                Bukkit.broadcastMessage("§a1번 맵이 선택되었습니다! §7(" + map1Votes + "표)");
            } else if (map2Votes > map1Votes) {
                selectedMap = mapManager.getSlot2Map();
                selectedMapName = mapManager.getSlot2MapName();
                Bukkit.broadcastMessage("§c2번 맵이 선택되었습니다! §7(" + map2Votes + "표)");
            } else {
                // Tie - randomly select
                if (Math.random() < 0.5) {
                    selectedMap = mapManager.getSlot1Map();
                    selectedMapName = mapManager.getSlot1MapName();
                    Bukkit.broadcastMessage("§e동점! 1번 맵이 무작위로 선택되었습니다!");
                } else {
                    selectedMap = mapManager.getSlot2Map();
                    selectedMapName = mapManager.getSlot2MapName();
                    Bukkit.broadcastMessage("§e동점! 2번 맵이 무작위로 선택되었습니다!");
                }
            }
            
            if (selectedMapName != null && !selectedMapName.isEmpty()) {
                Bukkit.broadcastMessage("§7맵 이름: §f" + selectedMapName);
            }
            
            // Set voted map
            mapManager.setVotedMap(selectedMap);
            
            // Close all GUIs
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getTitle().equals("§6§l맵 투표")) {
                    player.closeInventory();
                }
            }
            
            // Call completion callback
            if (onVoteComplete != null) {
                Bukkit.getScheduler().runTaskLater(plugin, onVoteComplete, 20L);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if it's the map vote GUI
        if (!event.getView().getTitle().equals("§6§l맵 투표")) {
            return;
        }
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        // Check if player already voted
        if (hasVoted.containsKey(player.getUniqueId())) {
            player.sendMessage("§c이미 투표하셨습니다!");
            return;
        }
        
        int slot = event.getSlot();
        
        // Slot 11: Map 1
        if (slot == 11) {
            hasVoted.put(player.getUniqueId(), true);
            voteCount.put(1, voteCount.get(1) + 1);
            player.sendMessage("§a1번 맵에 투표했습니다!");
            player.closeInventory();
            checkVotingComplete();
        }
        // Slot 15: Map 2
        else if (slot == 15) {
            hasVoted.put(player.getUniqueId(), true);
            voteCount.put(2, voteCount.get(2) + 1);
            player.sendMessage("§c2번 맵에 투표했습니다!");
            player.closeInventory();
            checkVotingComplete();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Check if it's the map vote GUI
        if (!event.getView().getTitle().equals("§6§l맵 투표")) {
            return;
        }
        
        // If player hasn't voted, reopen GUI after 1 tick
        if (!hasVoted.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !hasVoted.containsKey(player.getUniqueId())) {
                    openGUI(player);
                }
            }, 1L);
        }
    }
}

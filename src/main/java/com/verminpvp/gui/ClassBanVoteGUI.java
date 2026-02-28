package com.verminpvp.gui;

import com.verminpvp.managers.ClassBanManager;
import com.verminpvp.managers.ExcludeManager;
import com.verminpvp.models.ClassType;
import org.bukkit.Bukkit;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI for class ban voting system
 * Players vote on which class to ban for the current game
 */
public class ClassBanVoteGUI implements Listener {
    
    private final Plugin plugin;
    private final ClassBanManager classBanManager;
    private final ExcludeManager excludeManager;
    private Runnable onVoteComplete;
    
    private static final String GUI_TITLE = "§c§l클래스 밴 투표";
    
    public ClassBanVoteGUI(Plugin plugin, ClassBanManager classBanManager, ExcludeManager excludeManager) {
        this.plugin = plugin;
        this.classBanManager = classBanManager;
        this.excludeManager = excludeManager;
    }
    
    /**
     * Set callback for when voting is complete
     */
    public void setOnVoteComplete(Runnable callback) {
        this.onVoteComplete = callback;
    }
    
    /**
     * Open class ban vote GUI for a player
     */
    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, GUI_TITLE);
        
        // Same layout as ClassSelectionGUI but for banning
        // Swordsman (slot 10)
        gui.setItem(10, createClassBanIcon(ClassType.SWORDSMAN, Material.STONE_SWORD, "§6§l검술가"));
        
        // Scientist (slot 11)
        gui.setItem(11, createClassBanIcon(ClassType.SCIENTIST, Material.SPLASH_POTION, "§a§l과학자"));
        
        // Plague Spreader (slot 12)
        gui.setItem(12, createClassBanIcon(ClassType.PLAGUE_SPREADER, Material.GHAST_TEAR, "§2§l역병 전파자"));
        
        // Shield Soldier (slot 13)
        gui.setItem(13, createClassBanIcon(ClassType.SHIELD_SOLDIER, Material.SHIELD, "§9§l방패 병사"));
        
        // Critical Cutter (slot 14)
        gui.setItem(14, createClassBanIcon(ClassType.CRITICAL_CUTTER, Material.IRON_SWORD, "§c§l크리티컬 커터"));
        
        // Navigator (slot 15)
        gui.setItem(15, createClassBanIcon(ClassType.NAVIGATOR, Material.TRIDENT, "§b§l항해사"));
        
        // Captain (slot 16)
        gui.setItem(16, createClassBanIcon(ClassType.CAPTAIN, Material.GOLDEN_SWORD, "§e§l선장"));
        
        // Shapeshifter (slot 19)
        gui.setItem(19, createClassBanIcon(ClassType.SHAPESHIFTER, Material.PHANTOM_MEMBRANE, "§d§l생명체"));
        
        // Juggler (slot 20)
        gui.setItem(20, createClassBanIcon(ClassType.JUGGLER, Material.SNOWBALL, "§f§l저글러"));
        
        // Dragon Fury (slot 21)
        gui.setItem(21, createClassBanIcon(ClassType.DRAGON_FURY, Material.DRAGON_BREATH, "§c§l용의 분노자"));
        
        // Undead (slot 23)
        gui.setItem(23, createClassBanIcon(ClassType.UNDEAD, Material.WITHER_SKELETON_SKULL, "§8§l언데드"));
        
        // Stamper (slot 24)
        gui.setItem(24, createClassBanIcon(ClassType.STAMPER, Material.IRON_BOOTS, "§a§l스탬퍼"));
        
        // Time Engraver (slot 25)
        gui.setItem(25, createClassBanIcon(ClassType.TIME_ENGRAVER, Material.CLOCK, "§e§l시간 각인자"));
        
        // Cavalry (slot 28)
        gui.setItem(28, createClassBanIcon(ClassType.CAVALRY, Material.SADDLE, "§6§l기마병"));
        
        // Vitality Cutter (slot 29)
        gui.setItem(29, createClassBanIcon(ClassType.VITALITY_CUTTER, Material.DIAMOND_SWORD, "§c§l활력 절단자"));
        
        // Marathoner (slot 30)
        gui.setItem(30, createClassBanIcon(ClassType.MARATHONER, Material.LEATHER_BOOTS, "§b§l마라토너"));
        
        // Status info (slot 35)
        gui.setItem(35, createStatusIcon());
        
        player.openInventory(gui);
    }
    
    /**
     * Create a class ban icon
     */
    private ItemStack createClassBanIcon(ClassType classType, Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            
            List<String> lore = new ArrayList<>();
            lore.add("§c클릭하여 이 클래스를 밴 투표");
            lore.add("");
            lore.add("§7현재 투표 수: §f" + classBanManager.getVoteCount(classType) + "표");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create status icon showing voting progress
     */
    private ItemStack createStatusIcon() {
        // Count players who need to vote
        int totalPlayers = 0;
        int playersVoted = classBanManager.getTotalVotes();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Skip excluded players
            if (excludeManager != null && excludeManager.isExcluded(p)) {
                continue;
            }
            totalPlayers++;
        }
        
        int remaining = totalPlayers - playersVoted;
        
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§l투표 현황");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7전체 플레이어: §f" + totalPlayers + "명");
            lore.add("§a투표 완료: §f" + playersVoted + "명");
            lore.add("§c미투표: §f" + remaining + "명");
            lore.add("");
            if (remaining > 0) {
                lore.add("§7모든 플레이어가 투표하면");
                lore.add("§7클래스 선택으로 넘어갑니다!");
            } else {
                lore.add("§a모든 플레이어가 투표했습니다!");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Reset voting data
     */
    public void resetVoting() {
        classBanManager.resetVoting();
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
        if (classBanManager.getTotalVotes() >= totalPlayers && totalPlayers > 0) {
            // Calculate banned class
            ClassType bannedClass = classBanManager.calculateBannedClass();
            
            // Announce result
            if (bannedClass != null) {
                Bukkit.broadcastMessage("§c§l" + bannedClass.getKoreanName() + " §c클래스가 밴되었습니다!");
                Bukkit.broadcastMessage("§7이번 게임에서는 선택할 수 없습니다.");
            } else {
                Bukkit.broadcastMessage("§e밴된 클래스가 없습니다!");
            }
            
            // Close all GUIs
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getTitle().equals(GUI_TITLE)) {
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
        
        // Check if it's the class ban vote GUI
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        // Ignore status icon clicks
        if (event.getSlot() == 35) {
            return;
        }
        
        // Check if player already voted
        if (classBanManager.hasVoted(player)) {
            player.sendMessage("§c이미 투표하셨습니다!");
            return;
        }
        
        // Determine which class was clicked
        ClassType selectedClass = null;
        switch (event.getSlot()) {
            case 10:
                selectedClass = ClassType.SWORDSMAN;
                break;
            case 11:
                selectedClass = ClassType.SCIENTIST;
                break;
            case 12:
                selectedClass = ClassType.PLAGUE_SPREADER;
                break;
            case 13:
                selectedClass = ClassType.SHIELD_SOLDIER;
                break;
            case 14:
                selectedClass = ClassType.CRITICAL_CUTTER;
                break;
            case 15:
                selectedClass = ClassType.NAVIGATOR;
                break;
            case 16:
                selectedClass = ClassType.CAPTAIN;
                break;
            case 19:
                selectedClass = ClassType.SHAPESHIFTER;
                break;
            case 20:
                selectedClass = ClassType.JUGGLER;
                break;
            case 21:
                selectedClass = ClassType.DRAGON_FURY;
                break;
            case 23:
                selectedClass = ClassType.UNDEAD;
                break;
            case 24:
                selectedClass = ClassType.STAMPER;
                break;
            case 25:
                selectedClass = ClassType.TIME_ENGRAVER;
                break;
            case 28:
                selectedClass = ClassType.CAVALRY;
                break;
            case 29:
                selectedClass = ClassType.VITALITY_CUTTER;
                break;
            case 30:
                selectedClass = ClassType.MARATHONER;
                break;
        }
        
        if (selectedClass != null) {
            // Record vote
            classBanManager.voteForClass(player, selectedClass);
            player.sendMessage("§a" + selectedClass.getKoreanName() + " §a클래스에 밴 투표했습니다!");
            player.closeInventory();
            
            // Check if voting is complete
            checkVotingComplete();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Check if it's the class ban vote GUI
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
        
        // If player hasn't voted, reopen GUI after 1 tick
        if (!classBanManager.hasVoted(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !classBanManager.hasVoted(player)) {
                    openGUI(player);
                }
            }, 1L);
        }
    }
}

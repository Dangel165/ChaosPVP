package com.verminpvp.gui;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.ExcludeManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.GameMode;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * GUI for class selection
 */
public class ClassSelectionGUI implements Listener {
    
    private final VerminPVP plugin;
    private final ClassManager classManager;
    private ExcludeManager excludeManager;
    private GameManager gameManager;
    private com.verminpvp.managers.TeamManager teamManager;
    private com.verminpvp.managers.DraftPickManager draftPickManager;
    private static final String GUI_TITLE = "§6§l클래스 선택";
    
    // Track players who have the GUI open
    private final Set<UUID> playersWithGUIOpen = new HashSet<>();
    
    public ClassSelectionGUI(VerminPVP plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
    }
    
    /**
     * Set the ExcludeManager (called after initialization)
     */
    public void setExcludeManager(ExcludeManager excludeManager) {
        this.excludeManager = excludeManager;
    }
    
    /**
     * Set the GameManager (called after initialization)
     */
    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    /**
     * Set the TeamManager (called after initialization)
     */
    public void setTeamManager(com.verminpvp.managers.TeamManager teamManager) {
        this.teamManager = teamManager;
    }
    
    /**
     * Set the DraftPickManager (called after initialization)
     */
    public void setDraftPickManager(com.verminpvp.managers.DraftPickManager draftPickManager) {
        this.draftPickManager = draftPickManager;
    }
    
    /**
     * Open the class selection GUI for a player
     */
    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, GUI_TITLE);
        
        // Track that this player has the GUI open
        playersWithGUIOpen.add(player.getUniqueId());
        
        org.bukkit.Bukkit.getLogger().info("[ClassSelection] Opening GUI for " + player.getName() + ", total tracked: " + playersWithGUIOpen.size());
        
        // Give class selection item in case they close the GUI
        giveClassSelectionItem(player);
        
        // Get player's team for checking taken classes
        com.verminpvp.models.Team playerTeam = null;
        boolean isPracticeMode = gameManager != null && gameManager.isInPracticeMode(player);
        boolean isTeamMode = !isPracticeMode && gameManager != null && gameManager.getGameMode() == GameMode.TEAM;
        
        if (isTeamMode && teamManager != null) {
            playerTeam = teamManager.getPlayerTeam(player);
        }
        
        // Swordsman (slot 10)
        gui.setItem(10, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.SWORDSMAN, Material.STONE_SWORD,
            "§6§l검술가",
            "§7돌검 공격 시 +1 추가 피해",
            "§7다이아 검: 8 피해, 12.5초 쿨타임",
            "§7네더라이트 검: 20 피해 (궁극기)"));
        
        // Scientist (slot 11)
        gui.setItem(11, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.SCIENTIST, Material.SPLASH_POTION,
            "§a§l과학자",
            "§7투척 시 속도 증가",
            "§7랜덤 물약 생성",
            "§7궁극기: 광역 물약 효과"));
        
        // Plague Spreader (slot 12)
        gui.setItem(12, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.PLAGUE_SPREADER, Material.GHAST_TEAR,
            "§2§l역병 전파자",
            "§7독 데미지 면역",
            "§7독 엔진 생성",
            "§7따라다니는 독 필드 생성"));
        
        // Shield Soldier (slot 13)
        gui.setItem(13, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.SHIELD_SOLDIER, Material.SHIELD,
            "§9§l방패 병사",
            "§7흡수 하트 패시브",
            "§7특수 방패 막기 효과",
            "§7체력 낮을 때 황금 방패"));
        
        // Critical Cutter (slot 14)
        gui.setItem(14, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.CRITICAL_CUTTER, Material.IRON_SWORD,
            "§c§l크리티컬 커터",
            "§75% 치명타 확률 (비치명타마다 +1%)",
            "§7치명타: 2배 피해 + 2 HP 회복",
            "§7확정 크리티컬: 12 피해 + 6 HP 회복"));
        
        // Navigator (slot 15)
        gui.setItem(15, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.NAVIGATOR, Material.TRIDENT,
            "§b§l항해사",
            "§7검에 넉백 I 효과",
            "§7해전 무기: 5 피해, 넉백 II",
            "§7파도 타기: 신속 IV 1초, 충돌 시 넉백, 11초 쿨타임",
            "§7작살 투척: 6 피해 + 둔화 V"));
        
        // Captain (slot 16)
        gui.setItem(16, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.CAPTAIN, Material.GOLDEN_SWORD,
            "§e§l선장",
            "§7적 공격 시 팀원에게 힘 I",
            "§7선장의 명령: 팀원 강화",
            "§7선장의 작살: 4 피해 + 팀원 버프, 12초 쿨타임",
            "§7적중 시 쿨타임 4초 감소"));
        
        // Shapeshifter (slot 19)
        gui.setItem(19, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.SHAPESHIFTER, Material.PHANTOM_MEMBRANE,
            "§d§l생명체",
            "§7시간에 따라 진화",
            "§70분: 오셀롯 (5하트, 신속 III)",
            "§71분: 소 (15하트, 재생, 돌검)",
            "§72분: 스니퍼 (25하트, 저항 I, 철검)",
            "§74분: 라바저 (35하트, 저항 II, 힘 I)"));
        
        // Juggler (slot 20)
        gui.setItem(20, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.JUGGLER, Material.SNOWBALL,
            "§f§l저글러",
            "§7가벼운 것: 1 피해, 4초 쿨타임",
            "§7무거운 것: 8 피해 + 둔화, 13초 쿨타임",
            "§760초마다 투척 시간 획득",
            "§7투척 시간: 광역 둔화 + 쿨타임 초기화"));
        
        // Dragon Fury (slot 21)
        gui.setItem(21, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.DRAGON_FURY, Material.DRAGON_BREATH,
            "§c§l용의 분노자",
            "§7피격 시 역린 +1 (최대 30개)",
            "§7역린 수에 따라 저항 증가",
            "§7역린의 반격: 역린만큼 피해",
            "§7처치 시 역린/2 만큼 회복"));
        
        // Random button (slot 22) - always available
        gui.setItem(22, createClassIcon(null, Material.NETHER_STAR,
            "§d§l랜덤 선택",
            "§7클릭하면 랜덤으로",
            "§7클래스가 선택됩니다!"));
        
        // Undead (slot 23)
        gui.setItem(23, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.UNDEAD, Material.WITHER_SKELETON_SKULL,
            "§8§l언데드",
            "§7사망 시 최대 체력으로 부활 (1회)",
            "§7부활 시 신속 I, 힘 II, 저항 V",
            "§73초 후부터 0.25초마다 1 피해",
            "§7두 번째 사망까지 버프 유지"));
        
        // Stamper (slot 24)
        gui.setItem(24, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.STAMPER, Material.IRON_BOOTS,
            "§a§l스탬퍼",
            "§7낙하 피해 면역",
            "§7착지 시 4칸 범위 6 피해",
            "§7도장 찍기: 10칸 위로 (20초)",
            "§7다이브: 40칸 위로 (200초마다)",
            "§7다이브 착지: 20칸 범위 16 피해"));
        
        // Time Engraver (slot 25)
        gui.setItem(25, createClassIconWithAvailability(player, playerTeam, isTeamMode, ClassType.TIME_ENGRAVER, Material.CLOCK,
            "§e§l시간 각인자",
            "§7시간 각인: 8칸 범위 2초 위치 고정",
            "§7시곗바늘: 6 피해 (구속 대상 8 피해)",
            "§7구속 대상에게 어둠 + 실명",
            "§7영원한 시계: 8초 전체 위치 고정",
            "§7시간 박제 7개로 획득"));
        
        // Status info (slot 35)
        gui.setItem(35, createStatusIcon());
        
        player.openInventory(gui);
    }
    
    /**
     * Create status icon showing how many players haven't selected yet
     */
    private ItemStack createStatusIcon() {
        // Count players who need to select
        int totalPlayers = 0;
        int playersWithClass = 0;
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Skip excluded players
            if (excludeManager != null && excludeManager.isExcluded(p)) {
                continue;
            }
            
            totalPlayers++;
            
            if (classManager.getPlayerClass(p) != null) {
                playersWithClass++;
            }
        }
        
        int remaining = totalPlayers - playersWithClass;
        
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l선택 현황");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7전체 플레이어: §f" + totalPlayers + "명");
        lore.add("§a선택 완료: §f" + playersWithClass + "명");
        lore.add("§c미선택: §f" + remaining + "명");
        lore.add("");
        if (remaining > 0) {
            lore.add("§7모든 플레이어가 선택하면");
            lore.add("§7게임이 시작됩니다!");
        } else {
            lore.add("§a모든 플레이어가 선택했습니다!");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a class icon with description
     */
    private ItemStack createClassIcon(ClassType classType, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a class icon with availability check (for team mode)
     * If class is taken by teammate, show as gray glass pane with "이미 선택됨" message
     */
    private ItemStack createClassIconWithAvailability(Player player, com.verminpvp.models.Team playerTeam, 
                                                      boolean isTeamMode, ClassType classType, Material material, 
                                                      String name, String... lore) {
        // Check if class is taken by teammate
        boolean isTaken = false;
        Player takenBy = null;
        
        if (isTeamMode && playerTeam != null && classManager != null) {
            if (classManager.isClassTakenByTeam(classType, playerTeam)) {
                takenBy = classManager.getPlayerWithClassInTeam(classType, playerTeam);
                // Only mark as taken if it's taken by someone else (not the current player)
                if (takenBy != null && !takenBy.equals(player)) {
                    isTaken = true;
                }
            }
        }
        
        if (isTaken) {
            // Create disabled icon (gray glass pane)
            ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§8§m" + name.replaceAll("§.", "")); // Remove color codes and add strikethrough
            
            List<String> newLore = new ArrayList<>();
            newLore.add("§c§l이미 선택됨!");
            newLore.add("§7선택한 플레이어: §f" + takenBy.getName());
            newLore.add("");
            newLore.add("§8" + String.join(" ", lore).replaceAll("§7", "§8")); // Make description darker
            
            meta.setLore(newLore);
            item.setItemMeta(meta);
            return item;
        } else {
            // Create normal icon
            return createClassIcon(classType, material, name, lore);
        }
    }
    
    /**
     * Handle GUI clicks
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Check if draft pick is active and if it's this player's turn
        if (draftPickManager != null && draftPickManager.isDraftActive()) {
            Player currentPicker = draftPickManager.getCurrentPicker();
            if (currentPicker == null || !currentPicker.equals(player)) {
                player.sendMessage("§c지금은 당신의 차례가 아닙니다!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }
        
        // Block clicks on disabled classes (gray glass pane)
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            player.sendMessage("§c이 클래스는 이미 같은 팀의 다른 플레이어가 선택했습니다!");
            return;
        }
        
        // Ignore status icon clicks
        if (event.getSlot() == 35) {
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
            case 22:
                // Random selection - find available class
                selectedClass = getRandomAvailableClass(player);
                if (selectedClass == null) {
                    player.sendMessage("§c사용 가능한 클래스가 없습니다!");
                    player.closeInventory();
                    return;
                }
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
        }
        
        if (selectedClass != null) {
            // Check if class is already taken by teammate
            // In TEAM mode: check per-team (Blue team can have 1 Scientist, Red team can have 1 Scientist)
            // In SOLO mode: no duplicate checking (multiple players can pick same class)
            // In PRACTICE mode: no duplicate checking (individual practice)
            boolean isPracticeMode = gameManager != null && gameManager.isInPracticeMode(player);
            
            if (!isPracticeMode && gameManager != null && gameManager.getGameMode() == GameMode.TEAM && teamManager != null) {
                com.verminpvp.models.Team playerTeam = teamManager.getPlayerTeam(player);
                
                if (playerTeam != null) {
                    // Check if class is taken by someone else on the same team
                    Player takenBy = classManager.getPlayerWithClassInTeam(selectedClass, playerTeam);
                    
                    if (takenBy != null && !takenBy.equals(player)) {
                        // Class is taken by a different teammate - block selection
                        player.sendMessage("§c이 클래스는 이미 같은 팀의 " + takenBy.getName() + "님이 선택했습니다!");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }
                }
            }
            
            // Clear old class
            classManager.clearPlayerClass(player);
            
            // Set new class (this will give starting items automatically)
            classManager.setPlayerClass(player, selectedClass);
            
            // If in practice mode, start schedulers (like real game)
            // Note: setPlayerClass() already gives starting items, so we don't need to do it here
            if (isPracticeMode) {
                gameManager.startPracticeModeSchedulers(player);
            }
            
            player.closeInventory();
            player.sendMessage("§a클래스 선택: §e" + selectedClass.getDisplayName());
            
            // Remove class selection item when class is selected
            removeClassSelectionItem(player);
            
            // Remove from tracking since GUI is closed
            playersWithGUIOpen.remove(player.getUniqueId());
            
            // Update all open GUIs to show new status (including other players' GUIs)
            // This ensures that when one player selects a class, all other players see it as unavailable
            Bukkit.getScheduler().runTask(plugin, () -> {
                updateAllOpenGUIs();
            });
        }
    }
    
    /**
     * Handle inventory close event to remove player from tracking
     * If player closes GUI without selecting a class, give them a class selection item
     */
    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        
        Player player = (Player) event.getPlayer();
        
        // Check if player has selected a class
        boolean hasSelectedClass = classManager.getPlayerClass(player) != null;
        
        // Check if game is starting (monitoring class selection)
        boolean isGameStarting = gameManager != null && gameManager.isMonitoringClassSelection();
        
        // Check if draft pick is active
        boolean isDraftActive = draftPickManager != null && draftPickManager.isDraftActive();
        
        // Check if player is in practice mode
        boolean isPracticeMode = gameManager != null && gameManager.isInPracticeMode(player);
        
        org.bukkit.Bukkit.getLogger().info("[ClassSelection] GUI closed for " + player.getName() + 
            ", hasClass: " + hasSelectedClass + ", isGameStarting: " + isGameStarting + 
            ", isDraftActive: " + isDraftActive + ", isPracticeMode: " + isPracticeMode);
        
        // If player hasn't selected a class and (game is starting OR draft is active OR in practice mode), give them the item
        if (!hasSelectedClass && (isGameStarting || isDraftActive || isPracticeMode)) {
            giveClassSelectionItem(player);
            player.sendMessage("§e클래스를 선택하려면 §6[클래스 선택] §e아이템을 우클릭하세요!");
        } else {
            // Remove from tracking only if they selected a class or game is not starting
            playersWithGUIOpen.remove(player.getUniqueId());
            org.bukkit.Bukkit.getLogger().info("[ClassSelection] Removed from tracking: " + player.getName() + ", remaining: " + playersWithGUIOpen.size());
        }
    }
    
    /**
     * Give player a class selection item
     */
    private void giveClassSelectionItem(Player player) {
        // Create the class selection item (Nether Star)
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHER_STAR);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l[클래스 선택]");
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7우클릭하여 클래스 선택 GUI 열기");
            lore.add("");
            lore.add("§e클래스를 선택해야 게임을 시작할 수 있습니다!");
            meta.setLore(lore);
            
            // Add custom model data to identify this item
            meta.setCustomModelData(12345);
            
            item.setItemMeta(meta);
        }
        
        // Give item to player (slot 4 - middle of hotbar)
        player.getInventory().setItem(4, item);
    }
    
    /**
     * Remove class selection item from player
     */
    private void removeClassSelectionItem(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == org.bukkit.Material.NETHER_STAR) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 12345) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }
    
    /**
     * Handle player interact event to open GUI when clicking the class selection item
     */
    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();
        org.bukkit.inventory.ItemStack item = event.getItem();
        
        // Check if player right-clicked with the class selection item
        if (item != null && item.getType() == org.bukkit.Material.NETHER_STAR) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 12345) {
                event.setCancelled(true);
                
                // Open the class selection GUI
                openGUI(player);
                player.sendMessage("§a클래스 선택 GUI를 열었습니다!");
            }
        }
    }
    
    /**
     * Update all players who have the class selection GUI open
     */
    private void updateAllOpenGUIs() {
        org.bukkit.Bukkit.getLogger().info("[ClassSelection] updateAllOpenGUIs called, tracked players: " + playersWithGUIOpen.size());
        
        // Create a copy to avoid concurrent modification
        Set<UUID> playersCopy = new HashSet<>(playersWithGUIOpen);
        
        for (UUID playerId : playersCopy) {
            Player p = Bukkit.getPlayer(playerId);
            
            // Skip if player is offline or no longer exists
            if (p == null || !p.isOnline()) {
                org.bukkit.Bukkit.getLogger().info("[ClassSelection] Removing offline player: " + playerId);
                playersWithGUIOpen.remove(playerId);
                continue;
            }
            
            org.bukkit.Bukkit.getLogger().info("[ClassSelection] Updating GUI for player: " + p.getName());
            
            // Reopen GUI to refresh all icons with latest class selections
            // This ensures real-time updates when other players select classes
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.isOnline()) {
                    org.bukkit.Bukkit.getLogger().info("[ClassSelection] Reopening GUI for: " + p.getName());
                    openGUI(p);
                }
            });
        }
    }
    
    /**
     * Get a random available class that hasn't been taken
     * In TEAM mode: only select from classes not taken by player's team
     * In SOLO mode: all classes are available
     */
    private ClassType getRandomAvailableClass(Player player) {
        ClassType[] allClasses = ClassType.values();
        java.util.List<ClassType> availableClasses = new java.util.ArrayList<>();
        
        // In TEAM mode, only select from available classes in player's team
        // In SOLO mode, all classes are available
        if (gameManager != null && gameManager.getGameMode() == GameMode.TEAM && teamManager != null) {
            com.verminpvp.models.Team playerTeam = teamManager.getPlayerTeam(player);
            if (playerTeam != null) {
                for (ClassType classType : allClasses) {
                    if (!classManager.isClassTakenByTeam(classType, playerTeam)) {
                        availableClasses.add(classType);
                    }
                }
            } else {
                // No team assigned yet, all classes available
                availableClasses.addAll(Arrays.asList(allClasses));
            }
        } else {
            // SOLO mode - all classes available
            availableClasses.addAll(Arrays.asList(allClasses));
        }
        
        if (availableClasses.isEmpty()) {
            return null;
        }
        
        return availableClasses.get((int) (Math.random() * availableClasses.size()));
    }
}

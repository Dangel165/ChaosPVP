package com.verminpvp.handlers;

import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.ItemProvider;
import com.verminpvp.models.ClassType;
import com.verminpvp.models.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents players from dropping class items
 * Exception: Scientists can drop items in team mode
 */
public class ItemDropHandler implements Listener {
    
    private final ClassManager classManager;
    private final GameManager gameManager;
    private final ItemProvider itemProvider;
    
    public ItemDropHandler(ClassManager classManager, GameManager gameManager, ItemProvider itemProvider) {
        this.classManager = classManager;
        this.gameManager = gameManager;
        this.itemProvider = itemProvider;
    }
    
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        
        // Check if this is a class item
        String itemId = itemProvider.getItemId(item);
        if (itemId == null) {
            // Not a class item, allow dropping
            return;
        }
        
        // Get player's class
        ClassType playerClass = classManager.getPlayerClass(player);
        
        // Exception: Scientists can drop items in team mode
        if (playerClass == ClassType.SCIENTIST && gameManager.getGameMode() == GameMode.TEAM) {
            // Allow scientists to drop items in team mode
            return;
        }
        
        // Prevent dropping class items
        event.setCancelled(true);
        player.sendMessage("§c클래스 아이템은 버릴 수 없습니다!");
    }
}

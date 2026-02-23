package com.verminpvp.commands;

import com.verminpvp.managers.DataManager;
import com.verminpvp.managers.MapManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to add a spawn point for map slot 1
 * Korean command: /1번스폰추가
 */
public class MapSlot1SpawnAddCommand implements CommandExecutor {
    
    private final MapManager mapManager;
    private final DataManager dataManager;
    
    public MapSlot1SpawnAddCommand(MapManager mapManager, DataManager dataManager) {
        this.mapManager = mapManager;
        this.dataManager = dataManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다!");
            return true;
        }
        
        Player player = (Player) sender;
        Location location = player.getLocation();
        
        // Add spawn to slot 1
        mapManager.addSlot1Spawn(location);
        
        // Save data
        dataManager.saveData();
        
        // Send confirmation message
        player.sendMessage("§a1번 맵 전용 스폰 포인트가 추가되었습니다!");
        player.sendMessage("§7위치: " + String.format("%.1f, %.1f, %.1f", 
            location.getX(), location.getY(), location.getZ()));
        player.sendMessage("§7총 1번 맵 스폰: §f" + mapManager.getSlot1SpawnCount() + "개");
        
        return true;
    }
}

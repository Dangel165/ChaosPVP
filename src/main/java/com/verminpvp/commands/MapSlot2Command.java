package com.verminpvp.commands;

import com.verminpvp.managers.DataManager;
import com.verminpvp.managers.MapManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to set the second map slot for voting
 * Korean command: /2번맵지정
 */
public class MapSlot2Command implements CommandExecutor {
    
    private final MapManager mapManager;
    private final DataManager dataManager;
    
    public MapSlot2Command(MapManager mapManager, DataManager dataManager) {
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
        
        // Get map name from args (optional)
        String mapName = args.length > 0 ? String.join(" ", args) : null;
        
        // Clear existing slot 2 spawns
        int previousSpawnCount = mapManager.getSlot2SpawnCount();
        mapManager.clearSlot2Spawns();
        
        // Set slot 2 map
        mapManager.setSlot2Map(location, mapName);
        
        // Add current location as the only spawn point
        mapManager.addSlot2Spawn(location);
        
        // Save data
        dataManager.saveData();
        
        // Send confirmation message
        if (mapName != null && !mapName.isEmpty()) {
            player.sendMessage("§a2번 맵 슬롯이 설정되었습니다: §f" + mapName);
        } else {
            player.sendMessage("§a2번 맵 슬롯이 설정되었습니다!");
        }
        player.sendMessage("§7위치: " + String.format("%.1f, %.1f, %.1f", 
            location.getX(), location.getY(), location.getZ()));
        
        if (previousSpawnCount > 0) {
            player.sendMessage("§e기존 스폰 포인트 " + previousSpawnCount + "개가 삭제되고 현재 위치로 대체되었습니다.");
        }
        player.sendMessage("§7추가 스폰 포인트가 필요하면 §f/2번스폰추가 §7명령어를 사용하세요.");
        
        return true;
    }
}

package com.verminpvp.commands;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.MapManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to set regular map spawn locations
 * Usage: /맵지정 [맵이름]
 */
public class MapSetCommand implements CommandExecutor {
    
    private final MapManager mapManager;
    
    public MapSetCommand(MapManager mapManager) {
        this.mapManager = mapManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.isOp()) {
            player.sendMessage("§c이 명령어를 사용할 권한이 없습니다!");
            return true;
        }
        
        Location location = player.getLocation();
        String mapName = null;
        
        // Get map name if provided
        if (args.length > 0) {
            mapName = String.join(" ", args);
        }
        
        // Add spawn location
        mapManager.addSpawnLocation(location, mapName);
        
        // Auto-save data
        VerminPVP.getInstance().getDataManager().saveData();
        
        // Send confirmation message
        if (mapName != null && !mapName.isEmpty()) {
            player.sendMessage("§a맵이 지정되었습니다: §f" + mapName);
        } else {
            player.sendMessage("§a맵이 지정되었습니다!");
        }
        
        player.sendMessage("§7위치: " + 
            String.format("X: %.1f, Y: %.1f, Z: %.1f", 
                location.getX(), location.getY(), location.getZ()));
        player.sendMessage("§7총 맵 개수: §f" + mapManager.getSpawnLocationCount());
        
        return true;
    }
}

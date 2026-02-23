package com.verminpvp.commands;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.MapManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to remove a map by index
 * Usage: /맵제거 <번호>
 */
public class MapRemoveCommand implements CommandExecutor {
    
    private final MapManager mapManager;
    
    public MapRemoveCommand(MapManager mapManager) {
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
        
        if (args.length == 0) {
            player.sendMessage("§c사용법: /맵제거 <번호>");
            return true;
        }
        
        try {
            int index = Integer.parseInt(args[0]) - 1; // Convert to 0-based index
            
            if (mapManager.removeSpawnLocation(index)) {
                // Auto-save data
                VerminPVP.getInstance().getDataManager().saveData();
                
                player.sendMessage("§a맵이 제거되었습니다!");
                player.sendMessage("§7남은 맵 개수: §f" + mapManager.getSpawnLocationCount());
            } else {
                player.sendMessage("§c잘못된 맵 번호입니다!");
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c올바른 숫자를 입력하세요!");
            player.sendMessage("§7사용법: /맵제거 <번호>");
        }
        
        return true;
    }
}

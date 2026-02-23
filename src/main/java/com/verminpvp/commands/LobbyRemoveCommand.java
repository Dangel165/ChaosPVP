package com.verminpvp.commands;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.LobbyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to remove lobby location
 * Usage: /로비제거
 */
public class LobbyRemoveCommand implements CommandExecutor {
    
    private final LobbyManager lobbyManager;
    
    public LobbyRemoveCommand(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("verminpvp.admin")) {
            player.sendMessage("§c이 명령어를 사용할 권한이 없습니다!");
            return true;
        }
        
        if (!lobbyManager.hasLobbyLocation()) {
            player.sendMessage("§c제거할 로비 위치가 없습니다!");
            return true;
        }
        
        lobbyManager.removeLobbyLocation();
        
        // Auto-save data
        VerminPVP.getInstance().getDataManager().saveData();
        
        player.sendMessage("§a로비 위치가 제거되었습니다!");
        
        return true;
    }
}

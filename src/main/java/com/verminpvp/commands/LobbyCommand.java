package com.verminpvp.commands;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.LobbyManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to set or teleport to lobby location
 * Usage: /로비 [지정]
 */
public class LobbyCommand implements CommandExecutor {
    
    private final LobbyManager lobbyManager;
    
    public LobbyCommand(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // /로비 지정 - Set lobby location
        if (args.length > 0 && args[0].equalsIgnoreCase("지정")) {
            if (!player.hasPermission("verminpvp.admin")) {
                player.sendMessage("§c이 명령어를 사용할 권한이 없습니다!");
                return true;
            }
            
            Location location = player.getLocation();
            lobbyManager.setLobbyLocation(location);
            
            // Auto-save data
            VerminPVP.getInstance().getDataManager().saveData();
            
            player.sendMessage("§a로비 위치가 설정되었습니다!");
            player.sendMessage("§7위치: " + String.format("%.1f, %.1f, %.1f", 
                location.getX(), location.getY(), location.getZ()));
            
            return true;
        }
        
        // /로비 - Teleport to lobby
        if (!lobbyManager.hasLobbyLocation()) {
            player.sendMessage("§c로비 위치가 설정되지 않았습니다!");
            player.sendMessage("§7관리자는 /로비 지정 명령어로 로비를 설정할 수 있습니다.");
            return true;
        }
        
        Location lobbyLocation = lobbyManager.getLobbyLocation();
        player.teleport(lobbyLocation);
        player.sendMessage("§a로비로 이동했습니다!");
        
        return true;
    }
}

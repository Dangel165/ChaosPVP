package com.verminpvp.commands;

import com.verminpvp.managers.ExcludeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to include previously excluded players back into games
 */
public class IncludeCommand implements CommandExecutor {
    
    private final ExcludeManager excludeManager;
    
    public IncludeCommand(ExcludeManager excludeManager) {
        this.excludeManager = excludeManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c사용법: /인원추가 <플레이어명>");
            sender.sendMessage("§7예시: /인원추가 Steve");
            sender.sendMessage("§7제외된 플레이어를 다시 게임에 참여시킵니다.");
            return true;
        }
        
        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage("§c플레이어 '" + playerName + "'을(를) 찾을 수 없습니다.");
            return true;
        }
        
        if (!excludeManager.isExcluded(target)) {
            sender.sendMessage("§e" + target.getName() + "님은 이미 게임 참여 목록에 있습니다.");
            return true;
        }
        
        excludeManager.includePlayer(target);
        sender.sendMessage("§a" + target.getName() + "님을 게임 참여 목록에 추가했습니다.");
        target.sendMessage("§a이제 게임에 참여할 수 있습니다!");
        
        return true;
    }
}

package com.verminpvp.commands;

import com.verminpvp.managers.GameManager;
import com.verminpvp.models.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command to set game mode (team or solo)
 * Korean command: /게임모드
 */
public class GameModeCommand implements CommandExecutor {
    
    private final GameManager gameManager;
    
    public GameModeCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e현재 게임 모드: §f" + gameManager.getGameMode().getDisplayName());
            sender.sendMessage("§7사용법: /게임모드 <팀전|개인전>");
            return true;
        }
        
        String modeArg = args[0];
        GameMode newMode = null;
        
        if (modeArg.equals("팀전") || modeArg.equalsIgnoreCase("team")) {
            newMode = GameMode.TEAM;
        } else if (modeArg.equals("개인전") || modeArg.equalsIgnoreCase("solo")) {
            newMode = GameMode.SOLO;
        }
        
        if (newMode == null) {
            sender.sendMessage("§c잘못된 게임 모드입니다!");
            sender.sendMessage("§7사용법: /게임모드 <팀전|개인전>");
            return true;
        }
        
        gameManager.setGameMode(newMode);
        sender.sendMessage("§a게임 모드가 §e" + newMode.getDisplayName() + "§a(으)로 설정되었습니다!");
        
        return true;
    }
}

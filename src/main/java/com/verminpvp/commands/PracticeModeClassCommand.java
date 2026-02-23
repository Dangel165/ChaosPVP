package com.verminpvp.commands;

import com.verminpvp.gui.ClassSelectionGUI;
import com.verminpvp.managers.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open class selection GUI in practice mode
 * Usage: /연습클래스
 * Allows players to change their class during practice mode
 */
public class PracticeModeClassCommand implements CommandExecutor {
    
    private final ClassSelectionGUI classSelectionGUI;
    private final GameManager gameManager;
    
    public PracticeModeClassCommand(ClassSelectionGUI classSelectionGUI, GameManager gameManager) {
        this.classSelectionGUI = classSelectionGUI;
        this.gameManager = gameManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player is in practice mode
        if (!gameManager.isInPracticeMode(player)) {
            player.sendMessage("§c연습모드에서만 사용할 수 있습니다!");
            player.sendMessage("§7/연습모드 또는 /연습모드시작 명령어로 연습모드를 시작하세요.");
            return true;
        }
        
        // Open class selection GUI
        classSelectionGUI.openGUI(player);
        player.sendMessage("§a클래스 선택 GUI를 열었습니다.");
        
        return true;
    }
}

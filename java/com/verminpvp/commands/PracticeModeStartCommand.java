package com.verminpvp.commands;

import com.verminpvp.VerminPVP;
import com.verminpvp.gui.ClassSelectionGUI;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.models.ClassType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Command for starting practice mode with class selection
 * Korean command: /연습모드시작
 * Works exactly like /게임시작 but for individual practice (no win/loss/sudden death)
 */
public class PracticeModeStartCommand implements CommandExecutor {
    
    private final VerminPVP plugin;
    private final ClassSelectionGUI classSelectionGUI;
    private final ClassManager classManager;
    private final GameManager gameManager;
    
    public PracticeModeStartCommand(VerminPVP plugin, ClassSelectionGUI classSelectionGUI, ClassManager classManager, GameManager gameManager) {
        this.plugin = plugin;
        this.classSelectionGUI = classSelectionGUI;
        this.classManager = classManager;
        this.gameManager = gameManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Mark player as in practice mode FIRST
        gameManager.setPracticeMode(player, true);
        
        // Set to adventure mode
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        
        // Set max health to 20 hearts (40 HP)
        player.setMaxHealth(40.0);
        player.setHealth(40.0);
        
        // Disable natural regeneration in player's world
        if (player.getWorld() != null) {
            player.getWorld().setDifficulty(org.bukkit.Difficulty.NORMAL);
            player.getWorld().setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, false);
        }
        
        // Apply game effects (saturation, night vision, glowing)
        gameManager.applyGameEffects(player);
        
        // Open class selection GUI
        classSelectionGUI.openGUI(player);
        
        player.sendMessage("§a연습모드 시작!");
        player.sendMessage("§7클래스를 선택하세요.");
        player.sendMessage("§7/연습클래스 - 클래스 변경");
        player.sendMessage("§7/연습모드종료 - 연습모드 종료");
        
        return true;
    }
}

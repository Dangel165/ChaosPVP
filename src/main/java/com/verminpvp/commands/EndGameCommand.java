package com.verminpvp.commands;

import com.verminpvp.managers.ClassBanManager;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to forcefully end the game and reset all players
 * Korean command: /게임종료
 */
public class EndGameCommand implements CommandExecutor {
    
    private final ClassManager classManager;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final ClassBanManager classBanManager;
    
    public EndGameCommand(ClassManager classManager, GameManager gameManager, 
                         TeamManager teamManager, ClassBanManager classBanManager) {
        this.classManager = classManager;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.classBanManager = classBanManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Forcefully end the game
        gameManager.forceEndGame();
        
        // Clear banned class
        if (classBanManager != null) {
            classBanManager.clearBannedClass();
        }
        
        // Broadcast forced end message
        Bukkit.broadcastMessage("§c§l게임이 강제로 종료되었습니다!");
        
        // Reset all online players to no class and restore their state
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Clear class and team
            classManager.clearPlayerClass(player);
            teamManager.clearPlayerTeam(player);
            
            // Clear inventory
            player.getInventory().clear();
            
            // Remove ALL potion effects except SATURATION and NIGHT_VISION
            for (org.bukkit.potion.PotionEffectType effectType : org.bukkit.potion.PotionEffectType.values()) {
                if (effectType != null && 
                    effectType != org.bukkit.potion.PotionEffectType.SATURATION && 
                    effectType != org.bukkit.potion.PotionEffectType.NIGHT_VISION &&
                    player.hasPotionEffect(effectType)) {
                    player.removePotionEffect(effectType);
                }
            }
            
            // Remove all game effects (redundant but ensures cleanup)
            gameManager.removeGameEffects(player);
            
            // Restore to creative mode
            player.setGameMode(org.bukkit.GameMode.CREATIVE);
            
            // Restore OP if they should have it
            if (!player.isOp()) {
                player.setOp(true);
            }
            
            // Restore max health to default (20 HP = 10 hearts)
            player.setMaxHealth(20.0);
            player.setHealth(20.0);
            
            player.sendMessage("§a상태가 초기화되었습니다.");
        }
        
        return true;
    }
}

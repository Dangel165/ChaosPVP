package com.verminpvp.commands;

import com.verminpvp.VerminPVP;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Command to end practice mode
 * Usage: /연습모드종료
 * Resets player to creative mode and clears class
 */
public class PracticeModeEndCommand implements CommandExecutor {
    
    private final ClassManager classManager;
    private final GameManager gameManager;
    private final VerminPVP plugin;
    
    public PracticeModeEndCommand(ClassManager classManager, GameManager gameManager, VerminPVP plugin) {
        this.classManager = classManager;
        this.gameManager = gameManager;
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Clean up all class-specific effects for this player
        cleanupPlayerClassEffects(player);
        
        // Clear player class (this will also stop schedulers)
        classManager.clearPlayerClass(player);
        
        // Clear inventory
        player.getInventory().clear();
        
        // Remove ALL potion effects
        for (PotionEffectType effectType : PotionEffectType.values()) {
            if (effectType != null && player.hasPotionEffect(effectType)) {
                player.removePotionEffect(effectType);
            }
        }
        
        // Remove game effects (redundant but ensures cleanup)
        gameManager.removeGameEffects(player);
        
        // Remove practice mode flag
        gameManager.setPracticeMode(player, false);
        
        // Re-enable natural regeneration in player's world
        if (player.getWorld() != null) {
            player.getWorld().setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, true);
        }
        
        // Set to creative mode
        player.setGameMode(org.bukkit.GameMode.CREATIVE);
        
        // Restore max health to default (20 HP = 10 hearts)
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
        
        player.sendMessage("§a연습모드가 종료되었습니다.");
        player.sendMessage("§7크리에이티브 모드로 복귀했습니다.");
        
        return true;
    }
    
    /**
     * Clean up class-specific effects for a single player
     */
    private void cleanupPlayerClassEffects(Player player) {
        // Clean up Plague Spreader poison fields for this player
        if (plugin.getPlagueSpreaderHandler() != null) {
            plugin.getPlagueSpreaderHandler().cleanupPlayer(player);
        }
        
        // Clean up Shield Soldier passive tasks for this player
        if (plugin.getShieldSoldierHandler() != null) {
            plugin.getShieldSoldierHandler().cleanupPlayer(player);
        }
        
        // Clean up Shapeshifter evolution tasks and disguises for this player
        if (plugin.getShapeshifterHandler() != null) {
            plugin.getShapeshifterHandler().cleanupPlayer(player);
        }
        
        // Clean up Juggler throw time gain tasks for this player
        if (plugin.getJugglerHandler() != null) {
            plugin.getJugglerHandler().cleanupPlayer(player);
        }
        
        // Clean up Cavalry horses for this player
        if (plugin.getCavalryHandler() != null) {
            plugin.getCavalryHandler().cleanupPlayer(player);
        }
    }
}

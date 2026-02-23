package com.verminpvp.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;

/**
 * Command to remove all area effect clouds (poison fields)
 */
public class ParticleCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int removedCount = 0;
        
        // Remove all area effect clouds from all worlds
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof AreaEffectCloud) {
                    entity.remove();
                    removedCount++;
                }
            }
        }
        
        if (removedCount > 0) {
            sender.sendMessage("§a모든 입자 효과를 제거했습니다! (제거된 수: " + removedCount + ")");
        } else {
            sender.sendMessage("§7제거할 입자 효과가 없습니다.");
        }
        
        return true;
    }
}

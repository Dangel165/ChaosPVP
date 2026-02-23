package com.verminpvp.commands;

import com.verminpvp.gui.ClassSelectionGUI;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.models.ClassType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for /verminpvp class
 */
public class ClassCommand implements CommandExecutor, TabCompleter {
    
    private final ClassManager classManager;
    private final ClassSelectionGUI gui;
    
    public ClassCommand(ClassManager classManager, ClassSelectionGUI gui) {
        this.classManager = classManager;
        this.gui = gui;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // /verminpvp class - open GUI
        if (args.length == 0) {
            gui.openGUI(player);
            return true;
        }
        
        // /verminpvp class <classname> - select class directly
        if (args.length == 1) {
            String className = args[0];
            ClassType classType = ClassType.fromString(className);
            
            if (classType == null) {
                player.sendMessage("§cInvalid class name! Available classes:");
                for (ClassType type : ClassType.values()) {
                    player.sendMessage("§7- " + type.getDisplayName());
                }
                return true;
            }
            
            // Clear old class
            classManager.clearPlayerClass(player);
            
            // Set new class
            classManager.setPlayerClass(player, classType);
            
            player.sendMessage("§aYou selected: §e" + classType.getDisplayName());
            return true;
        }
        
        player.sendMessage("§cUsage: /verminpvp class [classname]");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Tab complete class names
            return Arrays.stream(ClassType.values())
                .map(ClassType::name)
                .map(String::toLowerCase)
                .filter(name -> name.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}

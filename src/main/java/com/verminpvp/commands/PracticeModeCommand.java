package com.verminpvp.commands;

import com.verminpvp.VerminPVP;
import com.verminpvp.gui.ClassSelectionGUI;
import com.verminpvp.managers.ClassManager;
import com.verminpvp.managers.GameManager;
import com.verminpvp.managers.MapManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to handle practice mode
 * Usage: 
 * - /연습모드 - Teleport to practice map and open class selection
 * - /연습모드 맵 [맵이름] - Set practice map
 */
public class PracticeModeCommand implements CommandExecutor {
    
    private final MapManager mapManager;
    private final ClassSelectionGUI classSelectionGUI;
    private final ClassManager classManager;
    private final GameManager gameManager;
    
    public PracticeModeCommand(MapManager mapManager, ClassSelectionGUI classSelectionGUI, 
                              ClassManager classManager, GameManager gameManager) {
        this.mapManager = mapManager;
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
        
        // Check if subcommand is "맵"
        if (args.length > 0 && args[0].equals("맵")) {
            // /연습모드 맵 [맵이름]
            if (!player.hasPermission("verminpvp.admin")) {
                player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
                return true;
            }
            
            // Get map name if provided
            String mapName = null;
            if (args.length > 1) {
                mapName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            }
            
            // Set practice map
            Location location = player.getLocation();
            mapManager.setPracticeMap(location, mapName);
            
            // Auto-save data
            VerminPVP.getInstance().getDataManager().saveData();
            
            if (mapName != null && !mapName.isEmpty()) {
                player.sendMessage("§a연습모드 맵이 설정되었습니다: §e" + mapName);
            } else {
                player.sendMessage("§a연습모드 맵이 설정되었습니다.");
            }
            
            player.sendMessage("§7위치: §f" + 
                String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ()));
            
            return true;
        }
        
        // /연습모드 (no args) - Teleport to practice map
        if (args.length == 0) {
            // Check if practice map is set
            if (!mapManager.hasPracticeMap()) {
                player.sendMessage("§c연습모드 맵이 설정되지 않았습니다.");
                player.sendMessage("§e/연습모드 맵 [맵이름] §7명령어로 맵을 설정하세요.");
                return true;
            }
            
            // Teleport to practice map
            Location practiceMap = mapManager.getPracticeMap();
            player.teleport(practiceMap);
            
            String mapName = mapManager.getPracticeMapName();
            if (mapName != null && !mapName.isEmpty()) {
                player.sendMessage("§a연습모드 맵으로 이동했습니다: §e" + mapName);
            } else {
                player.sendMessage("§a연습모드 맵으로 이동했습니다.");
            }
            
            // Set to adventure mode
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            
            // Set max health to 20 hearts (40 HP)
            player.setMaxHealth(40.0);
            player.setHealth(40.0);
            
            // Disable natural regeneration in player's world
            if (player.getWorld() != null) {
                player.getWorld().setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, false);
            }
            
            // Apply game effects
            gameManager.applyGameEffects(player);
            
            // Mark player as in practice mode
            gameManager.setPracticeMode(player, true);
            
            // Open class selection GUI after a short delay
            Bukkit.getScheduler().runTaskLater(gameManager.getPlugin(), () -> {
                classSelectionGUI.openGUI(player);
                player.sendMessage("§a연습모드 활성화!");
                player.sendMessage("§7클래스를 선택하여 테스트하세요.");
            }, 10L); // 0.5 second delay after teleport
            
            return true;
        }
        
        // Invalid usage
        player.sendMessage("§c사용법:");
        player.sendMessage("§e/연습모드 §7- 연습모드 맵으로 이동");
        player.sendMessage("§e/연습모드 맵 [맵이름] §7- 연습모드 맵 설정");
        return true;
    }
}

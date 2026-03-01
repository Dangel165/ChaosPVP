package com.verminpvp.commands;

import com.verminpvp.managers.MusicManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to configure game start music
 * Korean command: /음악설정
 */
public class MusicCommand implements CommandExecutor {
    
    private final MusicManager musicManager;
    
    public MusicCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.isOp()) {
            player.sendMessage("§c이 명령어는 OP 권한이 필요합니다.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "url":
            case "링크":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /음악설정 url <유튜브링크>");
                    return true;
                }
                String url = args[1];
                musicManager.setMusicUrl(url);
                musicManager.setUseMusicUrl(true);
                player.sendMessage("§a음악 URL이 설정되었습니다!");
                player.sendMessage("§7URL: §f" + url);
                player.sendMessage("§e");
                player.sendMessage("§e게임 시작 시 플레이어에게 클릭 가능한 링크가 전송됩니다.");
                player.sendMessage("§7플레이어가 링크를 클릭하면 브라우저에서 음악이 재생됩니다.");
                break;
                
            case "sound":
            case "사운드":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /음악설정 사운드 <사운드이름>");
                    player.sendMessage("§7예시: MUSIC_DISC_PIGSTEP, MUSIC_DISC_OTHERSIDE");
                    return true;
                }
                String soundName = args[1].toUpperCase();
                try {
                    Sound sound = Sound.valueOf(soundName);
                    musicManager.setBuiltInSound(sound);
                    musicManager.setUseMusicUrl(false);
                    player.sendMessage("§a내장 사운드가 설정되었습니다!");
                    player.sendMessage("§7사운드: §f" + soundName);
                    
                    // Play preview
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§c잘못된 사운드 이름입니다: " + soundName);
                    player.sendMessage("§7사용 가능한 음악: MUSIC_DISC_PIGSTEP, MUSIC_DISC_OTHERSIDE, MUSIC_DISC_5");
                }
                break;
                
            case "volume":
            case "볼륨":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /음악설정 볼륨 <0.0-1.0>");
                    return true;
                }
                try {
                    float volume = Float.parseFloat(args[1]);
                    musicManager.setVolume(volume);
                    player.sendMessage("§a볼륨이 설정되었습니다: §f" + volume);
                } catch (NumberFormatException e) {
                    player.sendMessage("§c잘못된 숫자 형식입니다.");
                }
                break;
                
            case "pitch":
            case "피치":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /음악설정 피치 <0.5-2.0>");
                    return true;
                }
                try {
                    float pitch = Float.parseFloat(args[1]);
                    musicManager.setPitch(pitch);
                    player.sendMessage("§a피치가 설정되었습니다: §f" + pitch);
                } catch (NumberFormatException e) {
                    player.sendMessage("§c잘못된 숫자 형식입니다.");
                }
                break;
                
            case "info":
            case "정보":
                player.sendMessage("§e=== 음악 설정 정보 ===");
                player.sendMessage(musicManager.getMusicInfo());
                player.sendMessage("§e볼륨: §f" + musicManager.getVolume());
                player.sendMessage("§e피치: §f" + musicManager.getPitch());
                break;
                
            case "test":
            case "테스트":
                player.sendMessage("§a음악을 테스트합니다...");
                musicManager.playMusicForPlayer(player);
                break;
                
            case "stop":
            case "정지":
                musicManager.stopMusicForPlayer(player);
                player.sendMessage("§a음악이 정지되었습니다.");
                break;
                
            case "disable":
            case "비활성화":
                musicManager.disableMusic();
                player.sendMessage("§c음악이 비활성화되었습니다.");
                player.sendMessage("§7게임 시작 시 음악이 재생되지 않습니다.");
                break;
                
            default:
                sendHelp(player);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§e=== 음악 설정 명령어 ===");
        player.sendMessage("§f/음악설정 url <링크> §7- 유튜브 링크 설정 (클릭 가능한 링크 전송)");
        player.sendMessage("§f/음악설정 사운드 <이름> §7- 내장 사운드 설정");
        player.sendMessage("§f/음악설정 볼륨 <0.0-1.0> §7- 볼륨 설정");
        player.sendMessage("§f/음악설정 피치 <0.5-2.0> §7- 피치 설정");
        player.sendMessage("§f/음악설정 정보 §7- 현재 설정 확인");
        player.sendMessage("§f/음악설정 테스트 §7- 음악 테스트");
        player.sendMessage("§f/음악설정 정지 §7- 음악 정지");
        player.sendMessage("§f/음악설정 비활성화 §7- 음악 기능 끄기");
        player.sendMessage("§7");
        player.sendMessage("§e추천 사운드:");
        player.sendMessage("§7- MUSIC_DISC_PIGSTEP (기본)");
        player.sendMessage("§7- MUSIC_DISC_OTHERSIDE");
        player.sendMessage("§7- MUSIC_DISC_5");
        player.sendMessage("§7");
        player.sendMessage("§e유튜브 링크 사용:");
        player.sendMessage("§7게임 시작 시 플레이어에게 클릭 가능한 링크가 전송됩니다.");
        player.sendMessage("§7플레이어가 링크를 클릭하면 브라우저에서 음악이 재생됩니다.");
        player.sendMessage("§7");
        player.sendMessage("§c참고: 음악은 선택 사항입니다. 설정하지 않으면 재생되지 않습니다.");
    }
}

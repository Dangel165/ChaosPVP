package com.verminpvp.commands;

import com.verminpvp.models.ClassType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Help command to show plugin information
 */
public class HelpCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Send help message
        player.sendMessage("§6§l========== ChaosPVP 도움말 ==========");
        player.sendMessage("");
        player.sendMessage("§e§l[ 게임 관리 ]");
        player.sendMessage("§a/게임시작 §7- 게임을 시작합니다");
        player.sendMessage("§a/게임종료 §7- 게임을 강제 종료합니다");
        player.sendMessage("§a/게임모드 <팀전|개인전> §7- 게임 모드를 설정합니다");
        player.sendMessage("");
        player.sendMessage("§e§l[ 맵 관리 ]");
        player.sendMessage("§a/로비 지정 §7- 현재 위치를 로비로 설정합니다");
        player.sendMessage("§a/로비제거 §7- 로비 위치를 제거합니다");
        player.sendMessage("§a/1번맵지정 [맵이름] §7- 1번 맵 슬롯을 설정합니다");
        player.sendMessage("§a/2번맵지정 [맵이름] §7- 2번 맵 슬롯을 설정합니다");
        player.sendMessage("§a/1번스폰추가 §7- 1번 맵 전용 스폰 포인트를 추가합니다");
        player.sendMessage("§a/2번스폰추가 §7- 2번 맵 전용 스폰 포인트를 추가합니다");
        player.sendMessage("§a/연습모드 맵 [맵이름] §7- 연습모드 맵을 설정합니다 (1개만)");
        player.sendMessage("§a/연습모드 §7- 연습모드 맵으로 이동합니다");
        player.sendMessage("");
        player.sendMessage("§e§l[ 플레이어 관리 ]");
        player.sendMessage("§a/인원제외 <플레이어명> §7- 플레이어를 게임에서 제외합니다");
        player.sendMessage("§a/인원추가 <플레이어명> §7- 제외된 플레이어를 다시 추가합니다");
        player.sendMessage("");
        player.sendMessage("§e§l[ 기타 ]");
        player.sendMessage("§a/연습모드시작 §7- 클래스를 테스트합니다");
        player.sendMessage("§a/연습모드종료 §7- 연습모드를 종료합니다");
        player.sendMessage("§a/입자끄기 §7- 맹독장판 입자를 제거합니다");
        player.sendMessage("§a/도움말 §7- 이 도움말을 표시합니다");
        player.sendMessage("");
        player.sendMessage("§e§l게임 규칙:");
        player.sendMessage("§7- 게임 시간: §f10분");
        player.sendMessage("§7- 시작 체력: §f20하트 (40HP)");
        player.sendMessage("§7- 팀전: 팀별 클래스 중복 불가");
        player.sendMessage("§7- 개인전: 클래스 중복 가능");
        player.sendMessage("§7- 배고픔 안 닳음 (포화 효과)");
        player.sendMessage("§7- 야간투시 무한");
        player.sendMessage("");
        player.sendMessage("§e§l사용 가능한 클래스:");
        
        for (ClassType classType : ClassType.values()) {
            player.sendMessage("§7- " + classType.getDisplayName());
        }
        
        player.sendMessage("");
        player.sendMessage("§e§l게임 시작 흐름:");
        player.sendMessage("§71. §f/로비 지정 §7- 로비 위치 설정 (필수)");
        player.sendMessage("§72. §f/1번맵지정, /2번맵지정 §7- 맵 슬롯 설정");
        player.sendMessage("§73. §f/1번스폰추가, /2번스폰추가 §7- 맵별 스폰 설정");
        player.sendMessage("§74. §f/게임모드 §7- 팀전 또는 개인전 선택");
        player.sendMessage("§75. §f/게임시작 §7- 게임 시작");
        player.sendMessage("§76. §f클래스 선택 §7- 원하는 클래스 선택");
        player.sendMessage("§77. §f로비 이동 §7- 10초 대기");
        player.sendMessage("§78. §f맵 투표 §7- 1번 또는 2번 맵 선택");
        player.sendMessage("§79. §f맵 텔포 §7- 선택된 맵으로 이동");
        player.sendMessage("§710. §f10초 프리즈 §7- 이동 불가 상태");
        player.sendMessage("§711. §f게임 시작! §7- 10분 전투");
        player.sendMessage("");
        player.sendMessage("§e§l연습모드:");
        player.sendMessage("§7- §f/연습모드 §7- 연습모드 맵으로 이동 + 클래스 선택");
        player.sendMessage("§7- §f/연습모드시작 §7- 현재 위치에서 클래스 선택");
        player.sendMessage("§7- §f/연습클래스 §7- 연습모드 중 클래스 변경");
        player.sendMessage("§7- §f/연습모드종료 §7- 연습모드 종료");
        player.sendMessage("§7- 승리/패배 없음, 자유롭게 클래스 테스트");
        player.sendMessage("§7- 5분 쿨타임");
        player.sendMessage("");
        player.sendMessage("§e§l[ 크레딧 ]");
        player.sendMessage("§7플러그인 제작자: §6Dangel");
        player.sendMessage("§7맵 제작: §6145wir, ssadw1348, gyangsongsanghyeoninde");
        player.sendMessage("§7밸런스 조정: §6145wir");
        player.sendMessage("§7기획: §6145wir, ssadw1348");
        player.sendMessage("§6§l====================================");
        
        return true;
    }
}

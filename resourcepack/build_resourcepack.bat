@echo off
chcp 65001 >nul
echo ========================================
echo ChaosPVP 리소스팩 빌드 스크립트
echo ========================================
echo.

REM 현재 디렉토리 확인
if not exist "pack.mcmeta" (
    echo [오류] pack.mcmeta 파일을 찾을 수 없습니다.
    echo resourcepack 폴더에서 실행해주세요.
    pause
    exit /b 1
)

REM 음악 파일 확인
echo [1/4] 음악 파일 확인 중...
if not exist "assets\minecraft\sounds\chaospvp\game_start.ogg" (
    echo [경고] game_start.ogg 파일이 없습니다!
    echo 음악 파일을 assets\minecraft\sounds\chaospvp\ 폴더에 추가해주세요.
    echo.
    echo 계속하려면 아무 키나 누르세요...
    pause >nul
)

REM 이전 빌드 삭제
echo [2/4] 이전 빌드 정리 중...
if exist "..\ChaosPVP-ResourcePack.zip" (
    del "..\ChaosPVP-ResourcePack.zip"
    echo 이전 빌드 삭제 완료
)

REM 리소스팩 압축
echo [3/4] 리소스팩 압축 중...
powershell -Command "Compress-Archive -Path * -DestinationPath ..\ChaosPVP-ResourcePack.zip -Force"

if %ERRORLEVEL% EQU 0 (
    echo 압축 완료!
) else (
    echo [오류] 압축 실패
    pause
    exit /b 1
)

REM SHA1 해시 생성
echo [4/4] SHA1 해시 생성 중...
powershell -Command "(Get-FileHash -Algorithm SHA1 ..\ChaosPVP-ResourcePack.zip).Hash" > ..\resourcepack_sha1.txt
echo SHA1 해시가 resourcepack_sha1.txt에 저장되었습니다.

echo.
echo ========================================
echo 빌드 완료!
echo ========================================
echo.
echo 생성된 파일:
echo - ChaosPVP-ResourcePack.zip
echo - resourcepack_sha1.txt
echo.
echo 다음 단계:
echo 1. ChaosPVP-ResourcePack.zip을 웹 호스팅에 업로드
echo 2. server.properties에 리소스팩 URL 설정
echo 3. resourcepack_sha1.txt의 해시값을 server.properties에 추가
echo.
pause

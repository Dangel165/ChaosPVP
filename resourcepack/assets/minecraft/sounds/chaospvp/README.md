# 음악 파일 위치

이 폴더에 OGG 형식의 음악 파일을 추가하세요.

## 필요한 파일

### 기본 (필수)
- `game_start.ogg` - 기본 게임 시작 음악

### 추가 (선택)
- `game_start_epic.ogg` - 에픽 버전
- `game_start_intense.ogg` - 강렬한 버전

## 파일 요구사항

- **형식**: OGG Vorbis
- **크기**: 5MB 이하 권장
- **비트레이트**: 128kbps 권장
- **길이**: 30초 ~ 2분 권장
- **샘플레이트**: 44100Hz 권장

## 변환 방법

### 온라인 변환
https://convertio.co/kr/mp3-ogg/

### FFmpeg 명령어
```bash
ffmpeg -i input.mp3 -c:a libvorbis -q:a 4 game_start.ogg
```

### Audacity
1. 파일 열기
2. 파일 → 내보내기 → OGG Vorbis로 내보내기
3. 품질: 5 (128kbps)

## 무료 음원 사이트

1. **YouTube Audio Library**
   - https://www.youtube.com/audiolibrary
   - 무료, 저작권 걱정 없음

2. **Incompetech**
   - https://incompetech.com/music/
   - Kevin MacLeod의 무료 음악

3. **Bensound**
   - https://www.bensound.com/
   - 고품질 무료 음악

## 예제 파일

현재 이 폴더는 비어있습니다.
위의 사이트에서 음악을 다운로드하고 OGG로 변환한 후 이 폴더에 추가하세요.

## 테스트

음악 파일을 추가한 후:
1. 리소스팩 빌드 (`build_resourcepack.bat`)
2. 서버에 적용
3. `/음악설정 사운드 chaospvp.game_start`
4. `/음악설정 테스트`

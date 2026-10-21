package com.ptit.uno.client.view.fx.game;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * FXSoundHelper: Quản lý nạp và phát hiệu ứng âm thanh game UNO.
 * Sử dụng JavaFX AudioClip cho độ trễ cực thấp (0ms), không làm lag luồng giao diện.
 */
public class FXSoundHelper {

    public static final String LOBBY_BGM_FILE = "Bolero Bonito.mp3";

    private static final Map<String, AudioClip> soundCache = new HashMap<>();
    private static MediaPlayer lobbyMusicPlayer = null;
    private static AudioClip lobbyMusicClipFallback = null;
    private static boolean lobbyActive = false;
    private static boolean roomWaitingActive = false;
    private static boolean soundEnabled = true;
    private static double lobbyMusicVolume = 0.5;
    private static boolean lobbyMusicMuted = false;

    static {
        preloadSounds();
    }

    private static void preloadSounds() {
        String[] sounds = {
            "card.mp3",
            "card_dealing.mp3",
            "special_card.mp3",
            "snap.mp3",
            "game_over.mp3",
            "press_button.mp3",
            "change_color.mp3"
        };

        for (String s : sounds) {
            AudioClip clip = loadClip(s);
            if (clip != null) {
                soundCache.put(s, clip);
            }
        }
    }

    private static String resolveSoundUri(String fileName) {
        try {
            // 1. Tìm trong Classpath
            String[] classpathCandidates = {
                "/resource/sounds/" + fileName,
                "resource/sounds/" + fileName,
                "/sounds/" + fileName
            };
            for (String p : classpathCandidates) {
                URL res = FXSoundHelper.class.getResource(p);
                if (res != null) {
                    return res.toExternalForm().replace(" ", "%20");
                }
            }

            // 2. Tìm trong File System
            String[] fileCandidates = {
                "src/resource/sounds/" + fileName,
                "UnoGameProject/src/resource/sounds/" + fileName,
                "resource/sounds/" + fileName,
                "D:/EndGamePTIT/LTM/BTL/implement/UnoGameProject/src/resource/sounds/" + fileName
            };
            for (String path : fileCandidates) {
                File f = new File(path);
                if (f.exists()) {
                    return f.toURI().toString();
                }
            }
        } catch (Exception e) {
            System.err.println("[FXSoundHelper] Không thể phân giải đường dẫn âm thanh " + fileName + ": " + e.getMessage());
        }
        return null;
    }

    private static AudioClip loadClip(String fileName) {
        try {
            String uri = resolveSoundUri(fileName);
            if (uri != null) {
                return new AudioClip(uri);
            }
        } catch (Exception e) {
            System.err.println("[FXSoundHelper] Không thể nạp âm thanh " + fileName + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Cập nhật trạng thái hiển thị của Sảnh chính (LobbyFXView) để tự động bật/tắt nhạc nền.
     */
    public static void setLobbyActive(boolean active) {
        lobbyActive = active;
        updateLobbyMusicState();
    }

    /**
     * Cập nhật trạng thái hiển thị của Phòng chờ (RoomWaitingFXView) để duy trì nhạc nền sảnh chờ.
     */
    public static void setRoomWaitingActive(boolean active) {
        roomWaitingActive = active;
        updateLobbyMusicState();
    }

    private static void updateLobbyMusicState() {
        if (lobbyActive || roomWaitingActive) {
            playLobbyMusic();
        } else {
            javafx.application.Platform.runLater(() -> {
                if (!lobbyActive && !roomWaitingActive) {
                    stopLobbyMusic();
                }
            });
        }
    }

    public static double getLobbyMusicVolume() {
        return lobbyMusicVolume;
    }

    public static void setLobbyMusicVolume(double volume) {
        lobbyMusicVolume = Math.max(0.0, Math.min(1.0, volume));
        if (lobbyMusicVolume > 0.001) {
            lobbyMusicMuted = false;
        }
        applyLobbyVolume();
    }

    public static boolean isLobbyMusicMuted() {
        return lobbyMusicMuted || lobbyMusicVolume <= 0.001;
    }

    public static void setLobbyMusicMuted(boolean muted) {
        lobbyMusicMuted = muted;
        applyLobbyVolume();
    }

    public static void toggleLobbyMusicMute() {
        setLobbyMusicMuted(!lobbyMusicMuted);
    }

    private static void applyLobbyVolume() {
        double effectiveVolume = lobbyMusicMuted ? 0.0 : lobbyMusicVolume;
        try {
            if (lobbyMusicPlayer != null) {
                lobbyMusicPlayer.setVolume(effectiveVolume);
            }
            if (lobbyMusicClipFallback != null) {
                lobbyMusicClipFallback.setVolume(effectiveVolume);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Phát nhạc nền sảnh chờ (Bolero Bonito.mp3) lặp vô hạn.
     * Nếu nhạc đang phát thì giữ nguyên để không bị ngắt quãng.
     */
    public static void playLobbyMusic() {
        if (!soundEnabled) return;
        try {
            double effectiveVolume = lobbyMusicMuted ? 0.0 : lobbyMusicVolume;
            if (lobbyMusicPlayer != null) {
                lobbyMusicPlayer.setVolume(effectiveVolume);
                if (lobbyMusicPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
                    lobbyMusicPlayer.play();
                }
                return;
            }
            if (lobbyMusicClipFallback != null) {
                lobbyMusicClipFallback.setVolume(effectiveVolume);
                if (!lobbyMusicClipFallback.isPlaying()) {
                    lobbyMusicClipFallback.play();
                }
                return;
            }

            String uri = resolveSoundUri(LOBBY_BGM_FILE);
            if (uri == null) {
                System.err.println("[FXSoundHelper] Không tìm thấy nhạc sảnh chờ: " + LOBBY_BGM_FILE);
                return;
            }

            try {
                Media media = new Media(uri);
                lobbyMusicPlayer = new MediaPlayer(media);
                lobbyMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                lobbyMusicPlayer.setVolume(effectiveVolume);
                lobbyMusicPlayer.setOnError(() -> {
                    try {
                        lobbyMusicPlayer.dispose();
                    } catch (Exception ignored) {}
                    lobbyMusicPlayer = null;
                    try {
                        lobbyMusicClipFallback = new AudioClip(uri);
                        lobbyMusicClipFallback.setCycleCount(AudioClip.INDEFINITE);
                        lobbyMusicClipFallback.setVolume(effectiveVolume);
                        if (soundEnabled && (lobbyActive || roomWaitingActive)) {
                            lobbyMusicClipFallback.play();
                        }
                    } catch (Exception ignored) {}
                });
                lobbyMusicPlayer.play();
            } catch (Exception mediaEx) {
                // Fallback sang AudioClip nếu MediaPlayer gặp sự cố codec hệ thống
                lobbyMusicClipFallback = new AudioClip(uri);
                lobbyMusicClipFallback.setCycleCount(AudioClip.INDEFINITE);
                lobbyMusicClipFallback.setVolume(effectiveVolume);
                lobbyMusicClipFallback.play();
            }
        } catch (Exception e) {
            System.err.println("[FXSoundHelper] Lỗi phát nhạc sảnh chờ: " + e.getMessage());
        }
    }

    /**
     * Dừng nhạc nền sảnh chờ khi vào bàn đấu hoặc đăng xuất.
     */
    public static void stopLobbyMusic() {
        try {
            if (lobbyMusicPlayer != null) {
                lobbyMusicPlayer.stop();
            }
            if (lobbyMusicClipFallback != null && lobbyMusicClipFallback.isPlaying()) {
                lobbyMusicClipFallback.stop();
            }
        } catch (Exception e) {
            System.err.println("[FXSoundHelper] Lỗi dừng nhạc sảnh chờ: " + e.getMessage());
        }
    }

    public static void playSound(String fileName) {
        if (!soundEnabled) return;
        try {
            AudioClip clip = soundCache.get(fileName);
            if (clip == null) {
                clip = loadClip(fileName);
                if (clip != null) soundCache.put(fileName, clip);
            }
            if (clip != null) {
                clip.play();
            }
        } catch (Exception e) {
            System.err.println("[FXSoundHelper] Lỗi phát âm thanh " + fileName + ": " + e.getMessage());
        }
    }

    public static void playCardPlay() {
        playSound("card.mp3");
    }

    public static void playCardDraw() {
        playSound("card_dealing.mp3");
    }

    public static void playSpecialCard() {
        playSound("special_card.mp3");
    }

    public static void playUnoSnap() {
        playSound("snap.mp3");
    }

    public static void playGameOver() {
        playSound("game_over.mp3");
    }

    private static long lastButtonClickTime = 0;

    public static void playButtonClick() {
        long now = System.currentTimeMillis();
        if (now - lastButtonClickTime < 60) {
            return;
        }
        lastButtonClickTime = now;
        playSound("press_button.mp3");
    }

    /**
     * Tự động gắn âm thanh click (giống nút '>' ở bảng hướng dẫn tân thủ)
     * cho tất cả các Button, ToggleButton và nút đóng cửa sổ trên một Scene.
     */
    public static void attachButtonSound(javafx.scene.Scene scene) {
        if (scene == null) return;
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getTarget() instanceof javafx.scene.Node) {
                javafx.scene.Node node = (javafx.scene.Node) e.getTarget();
                while (node != null) {
                    if (node instanceof javafx.scene.control.ButtonBase) {
                        if (!node.isDisabled()) {
                            playButtonClick();
                        }
                        break;
                    }
                    if (node.getStyleClass() != null &&
                            (node.getStyleClass().contains("window-control-btn")
                             || node.getStyleClass().contains("close-button"))) {
                        playButtonClick();
                        break;
                    }
                    node = node.getParent();
                }
            }
        });
    }

    public static void playChangeColor() {
        playSound("change_color.mp3");
    }

    public static void stopSound(String fileName) {
        try {
            AudioClip clip = soundCache.get(fileName);
            if (clip != null && clip.isPlaying()) {
                clip.stop();
            }
        } catch (Exception e) {
            System.err.println("[FXSoundHelper] Lỗi dừng âm thanh " + fileName + ": " + e.getMessage());
        }
    }

    public static void stopChangeColor() {
        stopSound("change_color.mp3");
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
        if (!enabled) {
            stopLobbyMusic();
        } else if (lobbyActive || roomWaitingActive) {
            playLobbyMusic();
        }
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }
}

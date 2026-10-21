package com.ptit.uno.client.view.fx.game;

import javafx.scene.media.AudioClip;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * FXSoundHelper: Quản lý nạp và phát hiệu ứng âm thanh game UNO.
 * Sử dụng JavaFX AudioClip cho độ trễ cực thấp (0ms), không làm lag luồng giao diện.
 */
public class FXSoundHelper {

    private static final Map<String, AudioClip> soundCache = new HashMap<>();
    private static boolean soundEnabled = true;

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

    private static AudioClip loadClip(String fileName) {
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
                    return new AudioClip(res.toExternalForm());
                }
            }

            // 2. Tìm trong File System
            String[] fileCandidates = {
                "src/resource/sounds/" + fileName,
                "resource/sounds/" + fileName,
                "D:/EndGamePTIT/LTM/BTL/implement/UnoGameProject/src/resource/sounds/" + fileName
            };
            for (String path : fileCandidates) {
                File f = new File(path);
                if (f.exists()) {
                    return new AudioClip(f.toURI().toString());
                }
            }
        } catch (Exception e) {
            System.err.println("[FXSoundHelper] Không thể nạp âm thanh " + fileName + ": " + e.getMessage());
        }
        return null;
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
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }
}

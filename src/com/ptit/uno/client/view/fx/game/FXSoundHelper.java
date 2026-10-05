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

    public static void playButtonClick() {
        playSound("press_button.mp3");
    }

    public static void playChangeColor() {
        playSound("change_color.mp3");
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }
}

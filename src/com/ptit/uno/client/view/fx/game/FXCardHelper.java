package com.ptit.uno.client.view.fx.game;

import com.ptit.uno.model.Card;
import com.ptit.uno.model.CardColor;
import com.ptit.uno.model.CardValue;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * FXCardHelper: Nạp, trích xuất và lưu cache hình ảnh các lá bài UNO dạng JavaFX Image.
 * Hỗ trợ nạp SpriteSheet HD (cards.png) từ Uno Online (156x242) và fallback deck.png.
 */
public class FXCardHelper {
    // Kích thước chuẩn của lá bài trong sprites/cards.png
    public static final int HD_CARD_WIDTH = 156;
    public static final int HD_CARD_HEIGHT = 242;

    // Fallback kích thước cũ
    public static final int LEGACY_WIDTH = 240;
    public static final int LEGACY_HEIGHT = 360;

    private static BufferedImage hdCardsSheet;
    private static BufferedImage legacyDeckSheet;
    private static BufferedImage legacyJollysSheet;
    private static BufferedImage legacyCardBackRaw;
    private static boolean isLoaded = false;

    // Cache các lá bài đã convert sang JavaFX Image
    private static final Map<String, Image> fxCache = new HashMap<>();
    private static Image fxCardBack;

    static {
        loadSheets();
    }

    private static synchronized void loadSheets() {
        if (isLoaded) return;

        // 1. Ưu tiên nạp SpriteSheet HD từ Uno Online
        hdCardsSheet = loadImage("sprites/cards.png");
        if (hdCardsSheet == null) {
            hdCardsSheet = loadImage("cards.png");
        }

        // 2. Nạp sheet cũ làm fallback
        legacyDeckSheet = loadImage("deck.png");
        legacyJollysSheet = loadImage("jollys.png");
        legacyCardBackRaw = loadImage("card_back.png");

        if (hdCardsSheet != null) {
            // Lấy mặt sau chuẩn từ ô (col 2, row 4)
            BufferedImage backBuf = hdCardsSheet.getSubimage(2 * HD_CARD_WIDTH, 4 * HD_CARD_HEIGHT, HD_CARD_WIDTH, HD_CARD_HEIGHT);
            fxCardBack = convertToFxImage(backBuf);
        } else if (legacyCardBackRaw != null) {
            fxCardBack = convertToFxImage(legacyCardBackRaw);
        }

        isLoaded = (hdCardsSheet != null || legacyDeckSheet != null);
    }

    private static BufferedImage loadImage(String fileName) {
        try {
            // 1. Nạp từ Classpath
            String[] paths = {
                "/resource/" + fileName,
                "resource/" + fileName,
                "/" + fileName
            };
            for (String p : paths) {
                InputStream is = FXCardHelper.class.getResourceAsStream(p);
                if (is != null) {
                    BufferedImage img = ImageIO.read(is);
                    if (img != null) return img;
                }
            }

            // 2. Nạp từ File System
            String[] filePaths = {
                "src/resource/" + fileName,
                "resource/" + fileName,
                "D:/EndGamePTIT/LTM/BTL/implement/UnoGameProject/src/resource/" + fileName
            };
            for (String fp : filePaths) {
                File f = new File(fp);
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) return img;
                }
            }
        } catch (Exception e) {
            System.err.println("[FXCardHelper] Lỗi nạp ảnh " + fileName + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy JavaFX Image của một lá bài UNO.
     */
    public static Image getCardImage(Card card) {
        if (card == null || !isLoaded) return getCardBackImage();

        String key = card.getColor() + "_" + card.getValue();
        if (fxCache.containsKey(key)) {
            return fxCache.get(key);
        }

        BufferedImage subImage = null;
        if (hdCardsSheet != null) {
            subImage = cropHdCardImage(card);
        }

        if (subImage == null && legacyDeckSheet != null) {
            subImage = cropLegacyCardImage(card);
        }

        if (subImage != null) {
            Image fxImg = convertToFxImage(subImage);
            fxCache.put(key, fxImg);
            return fxImg;
        }

        return getCardBackImage();
    }

    /**
     * Lấy JavaFX Image mặt sau của lá bài (Card Back).
     */
    public static Image getCardBackImage() {
        if (fxCardBack != null) return fxCardBack;
        if (legacyCardBackRaw != null) {
            fxCardBack = convertToFxImage(legacyCardBackRaw);
            return fxCardBack;
        }
        return null;
    }

    /**
     * Trích xuất lá bài từ SpriteSheet HD của Uno Online (13 cột x 5 hàng, ô 156x242).
     */
    private static BufferedImage cropHdCardImage(Card card) {
        try {
            CardValue val = card.getValue();
            CardColor col = card.getColor();

            int row;
            int colIdx;

            // Lá bài đặc biệt (Row 4)
            if (val == CardValue.WILD) {
                row = 4;
                colIdx = 0;
            } else if (val == CardValue.WILD_DRAW_FOUR) {
                row = 4;
                colIdx = 1;
            } else {
                // Lá thường 4 màu (Row 0: RED, Row 1: GREEN, Row 2: BLUE, Row 3: YELLOW)
                switch (col) {
                    case RED: row = 0; break;
                    case GREEN: row = 1; break;
                    case BLUE: row = 2; break;
                    case YELLOW: row = 3; break;
                    default: row = 0; break;
                }

                switch (val) {
                    case ZERO: colIdx = 0; break;
                    case ONE: colIdx = 1; break;
                    case TWO: colIdx = 2; break;
                    case THREE: colIdx = 3; break;
                    case FOUR: colIdx = 4; break;
                    case FIVE: colIdx = 5; break;
                    case SIX: colIdx = 6; break;
                    case SEVEN: colIdx = 7; break;
                    case EIGHT: colIdx = 8; break;
                    case NINE: colIdx = 9; break;
                    case SKIP: colIdx = 10; break;
                    case REVERSE: colIdx = 11; break;
                    case DRAW_TWO: colIdx = 12; break;
                    default: colIdx = 0; break;
                }
            }

            int x = colIdx * HD_CARD_WIDTH;
            int y = row * HD_CARD_HEIGHT;
            return hdCardsSheet.getSubimage(x, y, HD_CARD_WIDTH, HD_CARD_HEIGHT);
        } catch (Exception e) {
            System.err.println("[FXCardHelper] Lỗi trích xuất HD card: " + card + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Fallback cho spritesheet cũ (deck.png + jollys.png).
     */
    private static BufferedImage cropLegacyCardImage(Card card) {
        try {
            CardValue val = card.getValue();
            CardColor col = card.getColor();

            if (val == CardValue.WILD || val == CardValue.WILD_DRAW_FOUR) {
                if (legacyJollysSheet == null) return null;
                int row = (val == CardValue.WILD) ? 0 : 1;
                int colIdx = 0;
                if (col == CardColor.RED) colIdx = 1;
                else if (col == CardColor.BLUE) colIdx = 2;
                else if (col == CardColor.YELLOW) colIdx = 3;
                else if (col == CardColor.GREEN) colIdx = 4;

                int x = Math.min(colIdx * LEGACY_WIDTH, Math.max(0, legacyJollysSheet.getWidth() - LEGACY_WIDTH));
                int y = Math.min(row * LEGACY_HEIGHT, Math.max(0, legacyJollysSheet.getHeight() - LEGACY_HEIGHT));
                return legacyJollysSheet.getSubimage(x, y, LEGACY_WIDTH, LEGACY_HEIGHT);
            }

            int row = 0;
            if (col == CardColor.RED) row = 0;
            else if (col == CardColor.YELLOW) row = 1;
            else if (col == CardColor.GREEN) row = 2;
            else if (col == CardColor.BLUE) row = 3;

            int colIdx = 0;
            switch (val) {
                case ZERO: colIdx = 0; break;
                case ONE: colIdx = 1; break;
                case TWO: colIdx = 2; break;
                case THREE: colIdx = 3; break;
                case FOUR: colIdx = 4; break;
                case FIVE: colIdx = 5; break;
                case SIX: colIdx = 6; break;
                case SEVEN: colIdx = 7; break;
                case EIGHT: colIdx = 8; break;
                case NINE: colIdx = 9; break;
                case SKIP: colIdx = 10; break;
                case REVERSE: colIdx = 11; break;
                case DRAW_TWO: colIdx = 12; break;
                default: colIdx = 0; break;
            }

            int x = Math.min(colIdx * LEGACY_WIDTH, Math.max(0, legacyDeckSheet.getWidth() - LEGACY_WIDTH));
            int y = Math.min(row * LEGACY_HEIGHT, Math.max(0, legacyDeckSheet.getHeight() - LEGACY_HEIGHT));
            return legacyDeckSheet.getSubimage(x, y, LEGACY_WIDTH, LEGACY_HEIGHT);
        } catch (Exception e) {
            System.err.println("[FXCardHelper] Lỗi trích xuất Legacy card: " + card + " - " + e.getMessage());
            return null;
        }
    }

    private static Image convertToFxImage(BufferedImage bimg) {
        if (bimg == null) return null;
        int w = bimg.getWidth();
        int h = bimg.getHeight();
        WritableImage wr = new WritableImage(w, h);
        PixelWriter pw = wr.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, bimg.getRGB(x, y));
            }
        }
        return wr;
    }
}

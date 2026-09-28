package com.ptit.uno.client.view.fx;

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
 * Hỗ trợ lấy mặt sau lá bài, lá đặc biệt Wild và các lá số từ Sprite Sheet.
 */
public class FXCardHelper {
    public static final int RAW_WIDTH = 240;
    public static final int RAW_HEIGHT = 360;

    private static BufferedImage deckSheet;
    private static BufferedImage jollysSheet;
    private static BufferedImage cardBackRaw;
    private static boolean isLoaded = false;

    // Cache các lá bài đã convert sang JavaFX Image
    private static final Map<String, Image> fxCache = new HashMap<>();
    private static Image fxCardBack;

    static {
        loadSheets();
    }

    private static synchronized void loadSheets() {
        if (isLoaded) return;
        deckSheet = loadImage("deck.png");
        jollysSheet = loadImage("jollys.png");
        cardBackRaw = loadImage("card_back.png");
        if (cardBackRaw != null) {
            fxCardBack = convertToFxImage(cardBackRaw);
        }
        isLoaded = (deckSheet != null);
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

        BufferedImage subImage = cropCardImage(card);
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
        if (cardBackRaw != null) {
            fxCardBack = convertToFxImage(cardBackRaw);
            return fxCardBack;
        }
        return null;
    }

    private static BufferedImage cropCardImage(Card card) {
        try {
            CardValue val = card.getValue();
            CardColor col = card.getColor();

            // 1. Lá đặc biệt Wild và Wild Draw Four từ jollys.png
            if (val == CardValue.WILD || val == CardValue.WILD_DRAW_FOUR) {
                if (jollysSheet == null) return null;
                int row = (val == CardValue.WILD) ? 0 : 1;
                int colIdx = 0; // Mặc định đen trung tính
                if (col == CardColor.RED) colIdx = 1;
                else if (col == CardColor.BLUE) colIdx = 2;
                else if (col == CardColor.YELLOW) colIdx = 3;
                else if (col == CardColor.GREEN) colIdx = 4;

                int x = Math.min(colIdx * RAW_WIDTH, Math.max(0, jollysSheet.getWidth() - RAW_WIDTH));
                int y = Math.min(row * RAW_HEIGHT, Math.max(0, jollysSheet.getHeight() - RAW_HEIGHT));
                return jollysSheet.getSubimage(x, y, RAW_WIDTH, RAW_HEIGHT);
            }

            // 2. Lá bài thường 4 màu từ deck.png
            if (deckSheet == null) return null;
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

            int x = Math.min(colIdx * RAW_WIDTH, Math.max(0, deckSheet.getWidth() - RAW_WIDTH));
            int y = Math.min(row * RAW_HEIGHT, Math.max(0, deckSheet.getHeight() - RAW_HEIGHT));
            return deckSheet.getSubimage(x, y, RAW_WIDTH, RAW_HEIGHT);
        } catch (Exception e) {
            System.err.println("[FXCardHelper] Lỗi trích xuất ảnh lá " + card + ": " + e.getMessage());
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

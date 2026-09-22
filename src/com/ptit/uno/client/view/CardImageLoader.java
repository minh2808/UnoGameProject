package com.ptit.uno.client.view;

import com.ptit.uno.model.Card;
import com.ptit.uno.model.CardColor;
import com.ptit.uno.model.CardValue;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * CardImageLoader: Nạp và trích xuất hình ảnh từng lá bài UNO từ bộ ảnh Sprite Sheet (deck.png, jollys.png, card_back.png).
 * Áp dụng kỹ thuật Texture Atlas (Sprite Slicing) chuẩn trong lập trình game 2D.
 */
public class CardImageLoader {
    public static final int RAW_WIDTH = 240;
    public static final int RAW_HEIGHT = 360;

    private static BufferedImage deckSheet;
    private static BufferedImage jollysSheet;
    private static BufferedImage cardBackRaw;
    private static boolean isLoaded = false;

    // Cache các lá bài đã crop theo kích thước (key: "COLOR_VALUE_W_H")
    private static final Map<String, ImageIcon> iconCache = new HashMap<String, ImageIcon>();

    static {
        loadSheets();
    }

    private static synchronized void loadSheets() {
        if (isLoaded) return;
        deckSheet = loadImage("deck.png");
        jollysSheet = loadImage("jollys.png");
        cardBackRaw = loadImage("card_back.png");
        isLoaded = (deckSheet != null);
    }

    private static BufferedImage loadImage(String fileName) {
        try {
            // 1. Thử nạp từ Classpath (IDE & JAR)
            String[] paths = {
                "/resource/" + fileName,
                "resource/" + fileName,
                "/" + fileName
            };
            for (String p : paths) {
                InputStream is = CardImageLoader.class.getResourceAsStream(p);
                if (is != null) {
                    BufferedImage img = ImageIO.read(is);
                    if (img != null) return img;
                }
            }

            // 2. Thử nạp từ File System trực tiếp
            String[] filePaths = {
                "src/resource/" + fileName,
                "bin/resource/" + fileName,
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
            System.err.println("[CardImageLoader] Cảnh báo nạp file " + fileName + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy ImageIcon của một lá bài theo kích thước mong muốn.
     */
    public static ImageIcon getCardIcon(Card card, int width, int height) {
        if (card == null || !isLoaded) return null;

        String key = card.getColor() + "_" + card.getValue() + "_" + width + "_" + height;
        if (iconCache.containsKey(key)) {
            return iconCache.get(key);
        }

        BufferedImage subImage = cropCardImage(card);
        if (subImage != null) {
            Image scaled = subImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaled);
            iconCache.put(key, icon);
            return icon;
        }
        return null;
    }

    /**
     * Lấy ImageIcon của mặt sau lá bài (Draw Pile / Đối thủ).
     */
    public static ImageIcon getCardBackIcon(int width, int height) {
        if (cardBackRaw == null) return null;

        String key = "CARD_BACK_" + width + "_" + height;
        if (iconCache.containsKey(key)) {
            return iconCache.get(key);
        }

        Image scaled = cardBackRaw.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaled);
        iconCache.put(key, icon);
        return icon;
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

                int x = Math.min(colIdx * RAW_WIDTH, jollysSheet.getWidth() - RAW_WIDTH);
                int y = Math.min(row * RAW_HEIGHT, jollysSheet.getHeight() - RAW_HEIGHT);
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

            int x = Math.min(colIdx * RAW_WIDTH, deckSheet.getWidth() - RAW_WIDTH);
            int y = Math.min(row * RAW_HEIGHT, deckSheet.getHeight() - RAW_HEIGHT);
            return deckSheet.getSubimage(x, y, RAW_WIDTH, RAW_HEIGHT);
        } catch (Exception e) {
            System.err.println("[CardImageLoader] Lỗi cắt ảnh lá " + card + ": " + e.getMessage());
            return null;
        }
    }
}

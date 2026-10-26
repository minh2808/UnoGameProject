package com.ptit.uno.client.view.fx.lobby;

import com.ptit.uno.client.view.fx.game.FXCardHelper;
import com.ptit.uno.client.view.fx.game.FXSoundHelper;
import com.ptit.uno.model.Card;
import com.ptit.uno.model.CardColor;
import com.ptit.uno.model.CardValue;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;

/**
 * TutorialFXDialog: Hộp thoại Hướng dẫn tân thủ 7 bước theo chuẩn CPanelTutorial của Uno Online.
 * Sử dụng trực tiếp các sprite gốc (msg_box.png, arrow.png, but_skip.png, but_uno.png, cards.png)
 * kết hợp hiệu ứng chuyển lá bài tự động và âm thanh tương tác.
 */
public class TutorialFXDialog {

    private static final double BOX_WIDTH = 796.0;
    private static final double BOX_HEIGHT = 517.0;
    private static final double CENTER_X = BOX_WIDTH / 2.0;
    private static final double CENTER_Y = BOX_HEIGHT / 2.0;

    private static final String[] TUTORIAL_TEXTS = {
        "CHÀO MỪNG ĐẾN VỚI UNO!\nHÃY ĐÁNH LÁ BÀI CÙNG MÀU HOẶC CÙNG SỐ VÀ LÀ NGƯỜI ĐẦU TIÊN ĐÁNH HẾT BÀI TRÊN TAY!",
        "CÁC LÁ BÀI CHỨC NĂNG SẼ LÀM VÁN ĐẤU KỊCH TÍNH HƠN!\n\nLÁ RÚT 2 (+2) BUỘC NGƯỜI CHƠI TIẾP THEO PHẢI BỐC 2 LÁ BÀI VÀ MẤT LƯỢT",
        "LÁ CẤM LƯỢT (SKIP) SẼ CHẶN LƯỢT CỦA NGƯỜI CHƠI TIẾP THEO",
        "LÁ ĐẢO CHIỀU (REVERSE) SẼ ĐỔI CHIỀU VÒNG ĐÁNH HIỆN TẠI",
        "LÁ ĐỔI MÀU (WILD) CÓ THỂ ĐÁNH LÊN BẤT KỲ LÁ BÀI NÀO VÀ CHO PHÉP BẠN CHỌN MÀU TIẾP THEO",
        "LÁ ĐỔI MÀU +4 (WILD DRAW 4) CÓ THỂ ĐÁNH LÊN BẤT KỲ LÁ BÀI NÀO!\nCHO PHÉP BẠN ĐỔI MÀU MỚI, BUỘC NGƯỜI KẾ TIẾP BỐC 4 LÁ BÀI VÀ MẤT LƯỢT.",
        "ĐỪNG QUÊN BẤM NÚT 1 (UNO) KHI BẠN CHỈ CÒN 1 LÁ BÀI TRÊN TAY, NẾU KHÔNG BẠN SẼ BỊ PHẠT BỐC 2 LÁ BÀI!"
    };

    private Stage dialogStage;
    private int currentPage = 0;
    private Timeline cardAnimTimeline;

    private ImageView leftVisualView;
    private Label lblContent;
    private ImageView btnBackView;
    private ImageView btnNextView;
    private ImageView btnSkipView;
    private HBox endButtonsBox;

    private Runnable onSkipOrStartPractice;

    public TutorialFXDialog(Window owner, Runnable onSkipOrStartPractice) {
        this.onSkipOrStartPractice = onSkipOrStartPractice;
        initDialog(owner);
    }

    private void initDialog(Window owner) {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialogStage.initOwner(owner);
        }
        dialogStage.setTitle("Hướng dẫn tân thủ - UNO Online");

        // Lớp nền phủ mờ toàn cửa sổ
        StackPane overlayRoot = new StackPane();
        overlayRoot.setStyle("-fx-background-color: rgba(0, 0, 0, 0.65);");
        if (owner != null && owner.getWidth() > 0 && owner.getHeight() > 0) {
            overlayRoot.setPrefSize(owner.getWidth(), owner.getHeight());
        } else {
            overlayRoot.setPrefSize(1140, 700);
        }

        // Khung chứa CPanelTutorial đúng kích thước gốc 796x517
        Pane tutorialBox = new Pane();
        tutorialBox.setPrefSize(BOX_WIDTH, BOX_HEIGHT);
        tutorialBox.setMinSize(BOX_WIDTH, BOX_HEIGHT);
        tutorialBox.setMaxSize(BOX_WIDTH, BOX_HEIGHT);

        // 1. Nền msg_box.png
        Image msgBoxImg = loadSprite("msg_box.png");
        if (msgBoxImg != null) {
            ImageView bgView = new ImageView(msgBoxImg);
            bgView.setFitWidth(BOX_WIDTH);
            bgView.setFitHeight(BOX_HEIGHT);
            bgView.setOpacity(0.95);
            tutorialBox.getChildren().add(bgView);
        } else {
            tutorialBox.setStyle(
                "-fx-background-color: rgba(10, 25, 41, 0.94);" +
                "-fx-border-color: #607d8b;" +
                "-fx-border-width: 6;" +
                "-fx-border-radius: 14;" +
                "-fx-background-radius: 14;"
            );
        }

        // 2. Hình minh họa bên trái (Lá bài hoặc Nút UNO) tại tâm (CENTER_X - 215, CENTER_Y)
        leftVisualView = new ImageView();
        leftVisualView.setPreserveRatio(true);
        DropShadow cardShadow = new DropShadow();
        cardShadow.setRadius(12);
        cardShadow.setColor(Color.color(0, 0, 0, 0.55));
        leftVisualView.setEffect(cardShadow);
        tutorialBox.getChildren().add(leftVisualView);

        // 3. Nội dung chữ tiếng Việt bên phải
        lblContent = new Label();
        lblContent.setWrapText(true);
        lblContent.setPrefWidth(360);
        lblContent.setPrefHeight(340);
        lblContent.setLayoutX(CENTER_X - 75);
        lblContent.setLayoutY(CENTER_Y - 170);
        lblContent.setAlignment(Pos.CENTER_LEFT);
        lblContent.setTextAlignment(TextAlignment.LEFT);
        lblContent.setTextFill(Color.WHITE);
        lblContent.setFont(Font.font("System", FontWeight.BOLD, 22));
        DropShadow textShadow = new DropShadow();
        textShadow.setRadius(4);
        textShadow.setOffsetX(1);
        textShadow.setOffsetY(2);
        textShadow.setColor(Color.color(0, 0, 0, 0.8));
        lblContent.setEffect(textShadow);
        tutorialBox.getChildren().add(lblContent);

        // 4. Nút mũi tên Trái (Back) - xoay 180 độ từ arrow.png
        Image arrowImg = loadSprite("arrow.png");
        btnBackView = createSpriteButton(arrowImg, this::onButBack);
        btnBackView.setRotate(180);
        positionCentered(btnBackView, 50, CENTER_Y);
        tutorialBox.getChildren().add(btnBackView);

        // 5. Nút mũi tên Phải (Next) - arrow.png
        btnNextView = createSpriteButton(arrowImg, this::onButNext);
        positionCentered(btnNextView, BOX_WIDTH - 50, CENTER_Y);
        tutorialBox.getChildren().add(btnNextView);

        // 6. Nút Bỏ qua / Vào chơi thử (>>) ở góc dưới phải - but_skip.png
        Image skipImg = loadSprite("but_skip.png");
        btnSkipView = createSpriteButton(skipImg, this::onButSkip);
        positionCentered(btnSkipView, BOX_WIDTH - 53, BOX_HEIGHT - 53);
        tutorialBox.getChildren().add(btnSkipView);

        // 7. Các nút lựa chọn ở trang cuối: Hướng dẫn từng bước & Đấu tập với Bot
        endButtonsBox = new HBox(14);
        endButtonsBox.setAlignment(Pos.CENTER);

        Button btnStepByStep = new Button("HƯỚNG DẪN TỪNG BƯỚC");
        btnStepByStep.setCursor(Cursor.HAND);
        btnStepByStep.setStyle(
            "-fx-background-color: linear-gradient(to right, #10b981, #059669);" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-weight: 800;" +
            "-fx-font-size: 13.5px;" +
            "-fx-background-radius: 20px;" +
            "-fx-padding: 9px 20px;" +
            "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.45), 12, 0, 0, 2);"
        );
        btnStepByStep.setOnAction(e -> {
            FXSoundHelper.playButtonClick();
            onButSkip();
        });

        Button btnFreePlayBot = new Button("ĐẤU TẬP VỚI BOT");
        btnFreePlayBot.setCursor(Cursor.HAND);
        btnFreePlayBot.setStyle(
            "-fx-background-color: linear-gradient(to right, #3b82f6, #1d4ed8);" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-weight: 800;" +
            "-fx-font-size: 13.5px;" +
            "-fx-background-radius: 20px;" +
            "-fx-padding: 9px 20px;" +
            "-fx-effect: dropshadow(gaussian, rgba(59, 130, 246, 0.45), 12, 0, 0, 2);"
        );
        btnFreePlayBot.setOnAction(e -> {
            FXSoundHelper.playButtonClick();
            Window winOwner = (dialogStage != null) ? dialogStage.getOwner() : null;
            closeDialog();
            BotPracticeGameFXStage botStage = new BotPracticeGameFXStage(winOwner, "Tân Thủ");
            botStage.show();
        });

        endButtonsBox.getChildren().addAll(btnStepByStep, btnFreePlayBot);
        endButtonsBox.setLayoutX(CENTER_X - 190);
        endButtonsBox.setLayoutY(BOX_HEIGHT - 65);
        endButtonsBox.setVisible(false);
        tutorialBox.getChildren().add(endButtonsBox);

        overlayRoot.getChildren().add(tutorialBox);

        Scene scene = new Scene(overlayRoot);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case RIGHT:
                case D:
                    if (currentPage < TUTORIAL_TEXTS.length - 1) {
                        onButNext();
                    }
                    break;
                case LEFT:
                case A:
                    if (currentPage > 0) {
                        onButBack();
                    }
                    break;
                case ESCAPE:
                    closeDialog();
                    break;
                default:
                    break;
            }
        });

        dialogStage.setScene(scene);
        dialogStage.setOnHidden(e -> stopCardAnimation());

        loadPage(0);
    }

    private ImageView createSpriteButton(Image img, Runnable action) {
        ImageView iv = new ImageView();
        if (img != null) {
            iv.setImage(img);
        }
        iv.setCursor(Cursor.HAND);
        iv.setOnMousePressed(e -> {
            iv.setScaleX(0.9);
            iv.setScaleY(0.9);
        });
        iv.setOnMouseReleased(e -> {
            iv.setScaleX(1.0);
            iv.setScaleY(1.0);
        });
        iv.setOnMouseExited(e -> {
            iv.setScaleX(1.0);
            iv.setScaleY(1.0);
        });
        iv.setOnMouseClicked(e -> {
            FXSoundHelper.playButtonClick();
            if (action != null) {
                action.run();
            }
        });
        return iv;
    }

    private void positionCentered(ImageView iv, double centerX, double centerY) {
        Image img = iv.getImage();
        double w = (img != null && img.getWidth() > 0) ? img.getWidth() : 60;
        double h = (img != null && img.getHeight() > 0) ? img.getHeight() : 60;
        iv.setLayoutX(centerX - w / 2.0);
        iv.setLayoutY(centerY - h / 2.0);
    }

    private void loadPage(int pageIndex) {
        stopCardAnimation();

        btnBackView.setVisible(pageIndex > 0);
        btnNextView.setVisible(pageIndex < TUTORIAL_TEXTS.length - 1);
        if (endButtonsBox != null) {
            endButtonsBox.setVisible(pageIndex == TUTORIAL_TEXTS.length - 1);
        }

        lblContent.setText(TUTORIAL_TEXTS[pageIndex]);

        // Điều chỉnh cỡ chữ hợp lý cho câu dài (trang 2 và trang 6)
        if (pageIndex == 1 || pageIndex == 5) {
            lblContent.setFont(Font.font("System", FontWeight.BOLD, 20));
        } else {
            lblContent.setFont(Font.font("System", FontWeight.BOLD, 23));
        }

        switch (pageIndex) {
            case 0:
                // Trang 1: Đánh bài cùng màu hoặc cùng số (luân phiên 4 lá mẫu giống hệt main.js)
                startCardCycleAnimation(new Card[]{
                    new Card(CardColor.GREEN, CardValue.SEVEN),
                    new Card(CardColor.RED, CardValue.FIVE),
                    new Card(CardColor.YELLOW, CardValue.EIGHT),
                    new Card(CardColor.BLUE, CardValue.FIVE)
                });
                break;
            case 1:
                // Trang 2: Lá Rút 2 (+2)
                startCardCycleAnimation(new Card[]{
                    new Card(CardColor.RED, CardValue.DRAW_TWO),
                    new Card(CardColor.GREEN, CardValue.DRAW_TWO),
                    new Card(CardColor.BLUE, CardValue.DRAW_TWO),
                    new Card(CardColor.YELLOW, CardValue.DRAW_TWO)
                });
                break;
            case 2:
                // Trang 3: Lá Cấm lượt (Skip)
                startCardCycleAnimation(new Card[]{
                    new Card(CardColor.RED, CardValue.SKIP),
                    new Card(CardColor.GREEN, CardValue.SKIP),
                    new Card(CardColor.BLUE, CardValue.SKIP),
                    new Card(CardColor.YELLOW, CardValue.SKIP)
                });
                break;
            case 3:
                // Trang 4: Lá Đảo chiều (Reverse)
                startCardCycleAnimation(new Card[]{
                    new Card(CardColor.RED, CardValue.REVERSE),
                    new Card(CardColor.GREEN, CardValue.REVERSE),
                    new Card(CardColor.BLUE, CardValue.REVERSE),
                    new Card(CardColor.YELLOW, CardValue.REVERSE)
                });
                break;
            case 4:
                // Trang 5: Lá Đổi màu (Wild)
                setLeftCardImage(FXCardHelper.getCardImage(new Card(CardColor.WILD, CardValue.WILD)));
                break;
            case 5:
                // Trang 6: Lá Đổi màu +4 (Wild Draw 4)
                setLeftCardImage(FXCardHelper.getCardImage(new Card(CardColor.WILD, CardValue.WILD_DRAW_FOUR)));
                break;
            case 6:
                // Trang 7: Nút UNO (but_uno.png phóng to 1.5x giống main.js)
                Image unoBtnImg = loadSprite("but_uno.png");
                if (unoBtnImg != null) {
                    leftVisualView.setImage(unoBtnImg);
                    double w = unoBtnImg.getWidth() * 1.5;
                    double h = unoBtnImg.getHeight() * 1.5;
                    leftVisualView.setFitWidth(w);
                    leftVisualView.setFitHeight(h);
                    leftVisualView.setLayoutX((CENTER_X - 215) - w / 2.0);
                    leftVisualView.setLayoutY(CENTER_Y - h / 2.0);
                }
                break;
            default:
                break;
        }
    }

    private void setLeftCardImage(Image cardImg) {
        if (cardImg == null) return;
        leftVisualView.setImage(cardImg);
        double w = FXCardHelper.HD_CARD_WIDTH;
        double h = FXCardHelper.HD_CARD_HEIGHT;
        leftVisualView.setFitWidth(w);
        leftVisualView.setFitHeight(h);
        leftVisualView.setLayoutX((CENTER_X - 215) - w / 2.0);
        leftVisualView.setLayoutY(CENTER_Y - h / 2.0);
    }

    private void startCardCycleAnimation(Card[] cards) {
        if (cards == null || cards.length == 0) return;
        setLeftCardImage(FXCardHelper.getCardImage(cards[0]));

        final int[] idx = {0};
        cardAnimTimeline = new Timeline(new KeyFrame(Duration.millis(550), e -> {
            idx[0] = (idx[0] + 1) % cards.length;
            setLeftCardImage(FXCardHelper.getCardImage(cards[idx[0]]));
        }));
        cardAnimTimeline.setCycleCount(Timeline.INDEFINITE);
        cardAnimTimeline.play();
    }

    private void stopCardAnimation() {
        if (cardAnimTimeline != null) {
            cardAnimTimeline.stop();
            cardAnimTimeline = null;
        }
    }

    private void onButNext() {
        if (currentPage < TUTORIAL_TEXTS.length - 1) {
            currentPage++;
            loadPage(currentPage);
        }
    }

    private void onButBack() {
        if (currentPage > 0) {
            currentPage--;
            loadPage(currentPage);
        }
    }

    private void onButSkip() {
        Window winOwner = (dialogStage != null) ? dialogStage.getOwner() : null;
        closeDialog();
        if (onSkipOrStartPractice != null) {
            onSkipOrStartPractice.run();
        } else {
            TutorialPracticeFXStage practiceStage = new TutorialPracticeFXStage(winOwner, "Tân Thủ");
            practiceStage.show();
        }
    }

    private void closeDialog() {
        stopCardAnimation();
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    public void show() {
        if (dialogStage != null) {
            Window owner = dialogStage.getOwner();
            if (owner != null) {
                dialogStage.setX(owner.getX());
                dialogStage.setY(owner.getY());
                if (dialogStage.getScene() != null && dialogStage.getScene().getRoot() instanceof StackPane) {
                    StackPane root = (StackPane) dialogStage.getScene().getRoot();
                    root.setPrefSize(owner.getWidth(), owner.getHeight());
                }
            }
            currentPage = 0;
            loadPage(0);
            dialogStage.showAndWait();
        }
    }

    private Image loadSprite(String fileName) {
        try {
            String[] classpathPaths = {
                "/resource/sprites/" + fileName,
                "resource/sprites/" + fileName,
                "/sprites/" + fileName
            };
            for (String p : classpathPaths) {
                InputStream is = getClass().getResourceAsStream(p);
                if (is != null) {
                    return new Image(is);
                }
            }

            String[] filePaths = {
                "src/resource/sprites/" + fileName,
                "resource/sprites/" + fileName,
                "D:/EndGamePTIT/LTM/BTL/implement/UnoGameProject/src/resource/sprites/" + fileName
            };
            for (String fp : filePaths) {
                File f = new File(fp);
                if (f.exists()) {
                    return new Image(f.toURI().toString());
                }
            }
        } catch (Exception e) {
            System.err.println("[TutorialFXDialog] Không thể nạp sprite " + fileName + ": " + e.getMessage());
        }
        return null;
    }
}

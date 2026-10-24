package com.ptit.uno.client.view.fx.lobby;

import com.ptit.uno.client.view.fx.game.FXSoundHelper;
import com.ptit.uno.client.view.fx.game.GameBoardUIBridge;
import com.ptit.uno.model.Card;
import com.ptit.uno.model.CardColor;
import com.ptit.uno.model.CardValue;
import com.ptit.uno.model.GameState;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TutorialPracticeFXStage: Bàn chơi Hướng dẫn Tân thủ Tương tác (Interactive Guided Tutorial).
 * Tái sử dụng 100% giao diện game_board.fxml kết hợp:
 * - Spotlight Cut-out Mask (Shape.subtract) làm tối màn hình và chỉ chiếu sáng vùng cần thao tác.
 * - Viền phát sáng nhịp thở (Glowing Pulse Ring) + Ngón tay chỉ dẫn (finger.png) nhấp nhô.
 * - Kịch bản ván đấu 7 lá chuẩn luật UNO 100% với Bot Huấn Luyện.
 */
public class TutorialPracticeFXStage {

    private static final int PLAYER_USER_ID = 1;
    private static final int BOT_USER_ID = 2;
    private static final String BOT_NAME = "Bot Huấn Luyện";

    private static final int FINGER_FRAME_WIDTH = 202;
    private static final int FINGER_FRAME_HEIGHT = 277;
    private static final int FINGER_COLUMNS = 5;
    private static final int FINGER_TOTAL_FRAMES = 10;
    private static final double FINGER_DISPLAY_WIDTH = 56.0;
    private static final double FINGER_DISPLAY_HEIGHT = FINGER_DISPLAY_WIDTH * ((double) FINGER_FRAME_HEIGHT / FINGER_FRAME_WIDTH);

    private enum Step {
        DEALING,                // Đang chia 7 lá bài ban đầu
        STEP_1_OVERVIEW,        // Giới thiệu lá bài giữa bàn (Đỏ 5)
        STEP_2_SAME_COLOR,      // Đánh lá Đỏ 8 (Cùng màu Đỏ)
        STEP_3_SAME_NUMBER,     // Bot đánh Đỏ 2 -> Người chơi đánh Vàng 2 (Cùng số 2)
        STEP_4_DRAW_CARD,       // Bot đánh Xanh Lá 2 -> Người chơi kẹt bài phải Rút 1 lá (bốc được Wild)
        STEP_5_PLAY_WILD,       // Bot đánh Xanh Lá 6 -> Người chơi đánh Wild
        STEP_5B_CHOOSE_COLOR,   // Chọn màu Xanh Dương trên bảng chọn màu
        STEP_6A_PLAY_SKIP,      // Bot bốc bài mất lượt -> Người chơi đánh Xanh Dương Skip
        STEP_6B_PLAY_REVERSE,   // Người chơi đánh tiếp Xanh Dương Reverse
        STEP_6C_PLAY_DRAW_TWO,  // Người chơi đánh tiếp Xanh Dương +2
        STEP_7A_PLAY_TO_ONE,    // Người chơi đánh Xanh Dương 9 (còn 1 lá)
        STEP_7B_CALL_UNO,       // Bấm nút UNO! khẩn cấp khi còn 1 lá
        STEP_8_FINAL_WIN,       // Bot đánh Đỏ 9 -> Người chơi đánh Vàng 9 (Cùng số 9) thắng cuộc
        COMPLETED               // Hoàn thành khóa huấn luyện
    }

    private Stage stage;
    private StackPane rootStackPane;
    private GameBoardUIBridge boardController;
    private String playerUsername;

    // Lớp phủ Spotlight & Coachmark
    private Pane tutorialOverlayPane;
    private Shape currentMaskShape;
    private Rectangle pulseBorderRect;
    private Timeline pulseTimeline;
    private ImageView fingerPointerView;
    private Timeline fingerSpriteTimeline;
    private int currentFingerFrame = 0;
    private VBox coachmarkCard;
    private Label lblStepTitle;
    private Label lblStepDesc;
    private Label lblActionHint;
    private Button btnNextStep;

    // Trạng thái ván đấu kịch bản
    private Step currentStep = Step.DEALING;
    private boolean waitingForBot = false;
    private final List<Card> playerHand = new ArrayList<>();
    private int botCardCount = 7;
    private Card topCard;
    private CardColor activeColor;
    private boolean playerUnoCalled = false;

    public TutorialPracticeFXStage(Window owner, String playerUsername) {
        this.playerUsername = (playerUsername != null && !playerUsername.isEmpty()) ? playerUsername : "Tân Thủ";
        initStage(owner);
    }

    private void initStage(Window owner) {
        try {
            URL fxmlUrl = getClass().getResource("/resource/fxml/game_board.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/fxml/game_board.fxml");
            }
            if (fxmlUrl == null) {
                File f = new File("src/resource/fxml/game_board.fxml");
                if (f.exists()) {
                    fxmlUrl = f.toURI().toURL();
                }
            }

            if (fxmlUrl == null) {
                System.err.println("[TutorialPracticeFXStage] Không tìm thấy game_board.fxml!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent loadedRoot = loader.load();
            this.rootStackPane = (StackPane) loadedRoot;
            this.boardController = loader.getController();

            this.boardController.setTutorialMode(true);
            this.boardController.setPlayerInfo(playerUsername);
            setupActionListenerAndGuard();
            setupTutorialOverlayLayer();

            stage = new Stage();
            stage.setTitle("UNO Online - Bàn Chơi Hướng Dẫn Tân Thủ");
            if (owner != null) {
                stage.initOwner(owner);
                stage.initModality(Modality.WINDOW_MODAL);
            }
            Scene scene = new Scene(rootStackPane, 1150, 680);
            stage.setScene(scene);
            stage.setMinWidth(1050);
            stage.setMinHeight(640);
            stage.centerOnScreen();

            stage.setOnCloseRequest(e -> {
                stopAllAnimations();
                FXSoundHelper.playLobbyMusic();
            });

        } catch (Exception e) {
            System.err.println("[TutorialPracticeFXStage] Lỗi khởi tạo bàn chơi hướng dẫn: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void show() {
        if (stage != null) {
            FXSoundHelper.stopLobbyMusic();
            stage.show();
            stage.toFront();
            startScriptedTutorialMatch();
        }
    }

    // =========================================================================
    // THIẾT LẬP LỚP PHỦ SPOTLIGHT (KHOÉT LỖ SÁNG) + NGÓN TAY + KHUNG CHỈ DẪN
    // =========================================================================

    private void setupTutorialOverlayLayer() {
        tutorialOverlayPane = new Pane();
        tutorialOverlayPane.setPickOnBounds(false);
        tutorialOverlayPane.prefWidthProperty().bind(rootStackPane.widthProperty());
        tutorialOverlayPane.prefHeightProperty().bind(rootStackPane.heightProperty());

        // 1. Viền sáng nhịp thở quanh vùng Spotlight (Màu Vàng phát sáng nổi bật)
        pulseBorderRect = new Rectangle();
        pulseBorderRect.setFill(Color.TRANSPARENT);
        pulseBorderRect.setStroke(Color.web("#f1c40f"));
        pulseBorderRect.setStrokeWidth(3.5);
        pulseBorderRect.setArcWidth(18);
        pulseBorderRect.setArcHeight(18);
        pulseBorderRect.setMouseTransparent(true);
        pulseBorderRect.setEffect(new DropShadow(20, Color.web("#f1c40f")));
        pulseBorderRect.setVisible(false);

        pulseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(pulseBorderRect.scaleXProperty(), 1.0),
                new KeyValue(pulseBorderRect.scaleYProperty(), 1.0),
                new KeyValue(pulseBorderRect.opacityProperty(), 0.95)
            ),
            new KeyFrame(Duration.millis(700),
                new KeyValue(pulseBorderRect.scaleXProperty(), 1.07),
                new KeyValue(pulseBorderRect.scaleYProperty(), 1.07),
                new KeyValue(pulseBorderRect.opacityProperty(), 0.45)
            )
        );
        pulseTimeline.setAutoReverse(true);
        pulseTimeline.setCycleCount(Timeline.INDEFINITE);

        // 2. Ngón tay chỉ dẫn (finger.png - Sprite sheet 5x2 = 10 frames, mỗi frame 202x277)
        fingerPointerView = new ImageView();
        Image fingerImg = loadSprite("finger.png");
        if (fingerImg != null) {
            fingerPointerView.setImage(fingerImg);
            fingerPointerView.setViewport(new Rectangle2D(0, 0, FINGER_FRAME_WIDTH, FINGER_FRAME_HEIGHT));
            fingerPointerView.setFitWidth(FINGER_DISPLAY_WIDTH);
            fingerPointerView.setPreserveRatio(true);
            fingerPointerView.setSmooth(true);
        }
        fingerPointerView.setMouseTransparent(true);
        fingerPointerView.setEffect(new DropShadow(12, Color.color(0, 0, 0, 0.75)));
        fingerPointerView.setVisible(false);

        fingerSpriteTimeline = new Timeline(
            new KeyFrame(Duration.millis(55), e -> {
                currentFingerFrame = (currentFingerFrame + 1) % FINGER_TOTAL_FRAMES;
                int col = currentFingerFrame % FINGER_COLUMNS;
                int row = currentFingerFrame / FINGER_COLUMNS;
                fingerPointerView.setViewport(new Rectangle2D(
                    col * FINGER_FRAME_WIDTH,
                    row * FINGER_FRAME_HEIGHT,
                    FINGER_FRAME_WIDTH,
                    FINGER_FRAME_HEIGHT
                ));
            })
        );
        fingerSpriteTimeline.setCycleCount(Animation.INDEFINITE);

        // 3. Hộp thoại Hướng dẫn (Coachmark Card)
        coachmarkCard = createCoachmarkCard();
        coachmarkCard.setVisible(false);

        // 4. Nút "Thoát hướng dẫn" dạng Frosted Pill thanh lịch ở góc trên bên trái
        Button btnExitTutorial = new Button("Thoát Hướng Dẫn");
        FontIcon exitIcon = new FontIcon("fas-arrow-left");
        exitIcon.setIconSize(12);
        exitIcon.setIconColor(Color.web("#e2e8f0"));
        btnExitTutorial.setGraphic(exitIcon);
        btnExitTutorial.setLayoutX(18);
        btnExitTutorial.setLayoutY(14);
        btnExitTutorial.setCursor(Cursor.HAND);
        btnExitTutorial.setStyle(
            "-fx-background-color: rgba(15, 23, 42, 0.82);" +
            "-fx-text-fill: #f1f5f9;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12.5px;" +
            "-fx-background-radius: 20px;" +
            "-fx-padding: 7px 16px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.24);" +
            "-fx-border-radius: 20px;" +
            "-fx-border-width: 1px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 10, 0, 0, 2);"
        );
        btnExitTutorial.setOnAction(e -> {
            FXSoundHelper.playButtonClick();
            closeStage();
        });

        tutorialOverlayPane.getChildren().addAll(pulseBorderRect, coachmarkCard, fingerPointerView, btnExitTutorial);
        rootStackPane.getChildren().add(tutorialOverlayPane);
    }

    private VBox createCoachmarkCard() {
        VBox box = new VBox(12);
        box.setPrefWidth(390);
        box.setMaxWidth(390);
        // Thiết kế Pearl-Slate: Nền Trắng Ngọc Trai dịu mắt, viền Bạc mảnh tinh tế, tạo chiều sâu bằng bóng đổ mềm
        box.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc);" +
            "-fx-background-radius: 18px;" +
            "-fx-border-color: rgba(203, 213, 225, 0.95);" +
            "-fx-border-width: 1.5px;" +
            "-fx-border-radius: 18px;" +
            "-fx-padding: 18px 20px;" +
            "-fx-effect: dropshadow(gaussian, rgba(2, 6, 23, 0.68), 28, 0.22, 0, 10);"
        );

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setStyle(
            "-fx-padding: 0 0 8px 0;" +
            "-fx-border-color: transparent transparent #e2e8f0 transparent;" +
            "-fx-border-width: 0 0 1px 0;"
        );

        lblStepTitle = new Label("LUẬT CƠ BẢN");
        lblStepTitle.setWrapText(true);
        lblStepTitle.setMaxWidth(350);
        lblStepTitle.setStyle(
            "-fx-text-fill: #0f172a;" +
            "-fx-font-weight: 800;" +
            "-fx-font-size: 15.5px;"
        );

        headerRow.getChildren().add(lblStepTitle);

        lblStepDesc = new Label();
        lblStepDesc.setWrapText(true);
        lblStepDesc.setStyle(
            "-fx-text-fill: #334155;" +
            "-fx-font-weight: 500;" +
            "-fx-font-size: 13.6px;" +
            "-fx-line-spacing: 2.8px;"
        );

        // Hộp gợi ý thao tác tông Xanh Ngọc Lục Bảo (Soft Mint / Emerald) dịu mắt, dùng FontIcon của Ikonli
        lblActionHint = new Label();
        lblActionHint.setWrapText(true);
        lblActionHint.setMaxWidth(Double.MAX_VALUE);
        FontIcon hintLightbulbIcon = new FontIcon("fas-lightbulb");
        hintLightbulbIcon.setIconSize(15);
        hintLightbulbIcon.setIconColor(Color.web("#059669"));
        lblActionHint.setGraphic(hintLightbulbIcon);
        lblActionHint.setGraphicTextGap(9);
        lblActionHint.setStyle(
            "-fx-background-color: #ecfdf5;" +
            "-fx-border-color: #a7f3d0;" +
            "-fx-border-width: 1.2px;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-radius: 12px;" +
            "-fx-text-fill: #065f46;" +
            "-fx-font-weight: 700;" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 10px 13px;"
        );

        btnNextStep = new Button("ĐÃ HIỂU, TIẾP TỤC");
        FontIcon nextArrowIcon = new FontIcon("fas-arrow-right");
        nextArrowIcon.setIconSize(13);
        nextArrowIcon.setIconColor(Color.WHITE);
        btnNextStep.setGraphic(nextArrowIcon);
        btnNextStep.setContentDisplay(ContentDisplay.RIGHT);
        btnNextStep.setGraphicTextGap(8);
        btnNextStep.setCursor(Cursor.HAND);
        btnNextStep.setStyle(
            "-fx-background-color: linear-gradient(to right, #10b981, #059669);" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-weight: 800;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 10px;" +
            "-fx-padding: 9px 20px;" +
            "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.35), 10, 0, 0, 2);"
        );

        HBox footerRow = new HBox(btnNextStep);
        footerRow.setAlignment(Pos.CENTER_RIGHT);

        box.getChildren().addAll(headerRow, lblStepDesc, lblActionHint, footerRow);
        return box;
    }

    /**
     * Chiếu sáng (Spotlight) một Node mục tiêu trên bàn chơi, khoét lỗ xuyên thấu cho phép bấm trực tiếp
     * và làm tối phần còn lại của màn hình.
     */
    private void showSpotlightOnNode(
            Node primaryTarget,
            List<Node> extraClickableNodes,
            String badgeText,
            String titleText,
            String descText,
            String actionHintText,
            Runnable onNextButtonClick,
            double coachmarkX,
            double coachmarkY
    ) {
        Platform.runLater(() -> {
            clearSpotlightMaskOnly();
            rootStackPane.applyCss();
            rootStackPane.layout();

            double sceneW = Math.max(rootStackPane.getWidth(), 1150);
            double sceneH = Math.max(rootStackPane.getHeight(), 680);

            Rectangle fullScreenRect = new Rectangle(0, 0, sceneW, sceneH);
            Shape mask = fullScreenRect;

            Bounds targetBoundsInOverlay = null;
            boolean isHandCard = (primaryTarget != null && primaryTarget.getStyleClass().contains("hand-card-view"));
            double topPad = isHandCard ? 26 : 10;
            double bottomPad = 10;
            double sidePad = 10;

            if (primaryTarget != null && primaryTarget.getScene() != null) {
                Bounds sceneBounds = primaryTarget.localToScene(primaryTarget.getBoundsInLocal());
                targetBoundsInOverlay = tutorialOverlayPane.sceneToLocal(sceneBounds);

                Rectangle hole = new Rectangle(
                    targetBoundsInOverlay.getMinX() - sidePad,
                    targetBoundsInOverlay.getMinY() - topPad,
                    targetBoundsInOverlay.getWidth() + sidePad * 2,
                    targetBoundsInOverlay.getHeight() + topPad + bottomPad
                );
                hole.setArcWidth(18);
                hole.setArcHeight(18);
                mask = Shape.subtract(mask, hole);
            }

            if (extraClickableNodes != null) {
                for (Node extra : extraClickableNodes) {
                    if (extra != null && extra.getScene() != null) {
                        Bounds sb = extra.localToScene(extra.getBoundsInLocal());
                        Bounds lb = tutorialOverlayPane.sceneToLocal(sb);
                        double pad = 8;
                        Rectangle extraHole = new Rectangle(
                            lb.getMinX() - pad,
                            lb.getMinY() - pad,
                            lb.getWidth() + pad * 2,
                            lb.getHeight() + pad * 2
                        );
                        extraHole.setArcWidth(16);
                        extraHole.setArcHeight(16);
                        mask = Shape.subtract(mask, extraHole);
                    }
                }
            }

            mask.setFill(Color.rgb(0, 0, 0, 0.74));
            // Chặn click ở vùng tối, cho phép click xuyên qua lỗ thủng!
            mask.setPickOnBounds(false);
            mask.setOnMouseClicked(e -> nudgeCoachmark());
            currentMaskShape = mask;
            tutorialOverlayPane.getChildren().add(0, currentMaskShape);

            // Định vị viền phát sáng & ngón tay chỉ dẫn
            if (targetBoundsInOverlay != null) {
                pulseBorderRect.setX(targetBoundsInOverlay.getMinX() - sidePad);
                pulseBorderRect.setY(targetBoundsInOverlay.getMinY() - topPad);
                pulseBorderRect.setWidth(targetBoundsInOverlay.getWidth() + sidePad * 2);
                pulseBorderRect.setHeight(targetBoundsInOverlay.getHeight() + topPad + bottomPad);
                pulseBorderRect.setVisible(true);
                pulseTimeline.play();

                if (onNextButtonClick == null) {
                    // Chỉ hiện ngón tay khi bước đó yêu cầu người chơi tương tác trực tiếp vào Node.
                    // Sprite finger.png có bàn tay chỉ từ trên xuống dưới (đầu ngón trỏ nằm ở ~62% chiều ngang, đáy frame),
                    // nên đặt bàn tay ở phía TRÊN mục tiêu trỏ thẳng xuống cạnh trên của mục tiêu, không che mặt lá bài.
                    double fingerX = targetBoundsInOverlay.getCenterX() - (FINGER_DISPLAY_WIDTH * 0.62);
                    double fingerY = Math.max(8, targetBoundsInOverlay.getMinY() - topPad - FINGER_DISPLAY_HEIGHT + 12);
                    fingerPointerView.setLayoutX(fingerX);
                    fingerPointerView.setLayoutY(fingerY);
                    fingerPointerView.setVisible(true);
                    fingerSpriteTimeline.play();
                } else {
                    fingerPointerView.setVisible(false);
                    fingerSpriteTimeline.stop();
                }
            } else {
                pulseBorderRect.setVisible(false);
                pulseTimeline.stop();
                fingerPointerView.setVisible(false);
                fingerSpriteTimeline.stop();
            }

            // Cập nhật nội dung Coachmark Card
            lblStepTitle.setText(titleText);
            lblStepDesc.setText(descText);

            if (actionHintText != null && !actionHintText.isEmpty()) {
                lblActionHint.setText(actionHintText);
                lblActionHint.setVisible(true);
                lblActionHint.setManaged(true);
            } else {
                lblActionHint.setVisible(false);
                lblActionHint.setManaged(false);
            }

            if (onNextButtonClick != null) {
                btnNextStep.setVisible(true);
                btnNextStep.setManaged(true);
                btnNextStep.setOnAction(e -> {
                    FXSoundHelper.playButtonClick();
                    onNextButtonClick.run();
                });
            } else {
                btnNextStep.setVisible(false);
                btnNextStep.setManaged(false);
            }

            coachmarkCard.setLayoutX(coachmarkX);
            coachmarkCard.setLayoutY(coachmarkY);
            coachmarkCard.setVisible(true);

            // Hiệu ứng nảy nhẹ khi xuất hiện hướng dẫn bước mới
            ScaleTransition st = new ScaleTransition(Duration.millis(220), coachmarkCard);
            st.setFromX(0.92);
            st.setFromY(0.92);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void hideSpotlight() {
        Platform.runLater(() -> {
            clearSpotlightMaskOnly();
            pulseBorderRect.setVisible(false);
            pulseTimeline.stop();
            fingerPointerView.setVisible(false);
            fingerSpriteTimeline.stop();
            coachmarkCard.setVisible(false);
        });
    }

    private void clearSpotlightMaskOnly() {
        if (currentMaskShape != null) {
            tutorialOverlayPane.getChildren().remove(currentMaskShape);
            currentMaskShape = null;
        }
    }

    private void nudgeCoachmark() {
        if (coachmarkCard != null && coachmarkCard.isVisible()) {
            TranslateTransition shake = new TranslateTransition(Duration.millis(60), coachmarkCard);
            shake.setFromX(0);
            shake.setByX(8);
            shake.setCycleCount(4);
            shake.setAutoReverse(true);
            shake.play();
        }
    }

    // =========================================================================
    // KỊCH BẢN VÁN ĐẤU 7 LÁ CHUẨN LUẬT UNO 100%
    // =========================================================================

    private void startScriptedTutorialMatch() {
        stopAllAnimations();
        hideSpotlight();
        boardController.resetBoardForTutorial();

        // Khởi tạo đúng 7 lá bài trên tay người chơi
        playerHand.clear();
        playerHand.add(new Card("tut_1", CardColor.RED, CardValue.EIGHT));         // Bước 2: Đánh cùng màu Đỏ
        playerHand.add(new Card("tut_2", CardColor.YELLOW, CardValue.TWO));        // Bước 3: Đánh cùng số 2
        playerHand.add(new Card("tut_3", CardColor.BLUE, CardValue.SKIP));         // Bước 6A: Cấm lượt
        playerHand.add(new Card("tut_4", CardColor.BLUE, CardValue.REVERSE));      // Bước 6B: Đảo chiều
        playerHand.add(new Card("tut_5", CardColor.BLUE, CardValue.DRAW_TWO));     // Bước 6C: Rút 2 (+2)
        playerHand.add(new Card("tut_6", CardColor.BLUE, CardValue.NINE));         // Bước 7A: Đánh xuống còn 1 lá
        playerHand.add(new Card("tut_7", CardColor.YELLOW, CardValue.NINE));       // Bước 8: Lá quyết định thắng cuộc

        botCardCount = 7;
        topCard = new Card("tut_top_0", CardColor.RED, CardValue.FIVE); // Lá khởi đầu: Đỏ 5
        activeColor = CardColor.RED;
        playerUnoCalled = false;
        waitingForBot = true;
        currentStep = Step.DEALING;

        // Render 7 lá bài và trạng thái bàn cờ ban đầu (chạy hiệu ứng chia 7 lá mất ~2.7s)
        boardController.setHandCards(new ArrayList<>(playerHand));
        pushGameStateToView(0, "Bắt đầu khóa huấn luyện Tân thủ! Lá bài mở đầu là Đỏ 5.");
        boardController.addChatMessage(BOT_NAME, "Chào mừng bạn! Mình sẽ hướng dẫn bạn làm chủ UNO qua ván đấu 7 lá này nhé!", Color.web("#2ecc71"));

        // Đợi hiệu ứng chia 7 lá hoàn tất rồi bật Bước 1
        PauseTransition dealWait = new PauseTransition(Duration.millis(2850));
        dealWait.setOnFinished(e -> enterStep1Overview());
        dealWait.play();
    }

    /**
     * BƯỚC 1/8: Làm quen lá bài trên bàn (Đỏ 5)
     */
    private void enterStep1Overview() {
        currentStep = Step.STEP_1_OVERVIEW;
        waitingForBot = false;

        showSpotlightOnNode(
            boardController.getDiscardPileContainer(),
            Arrays.asList(boardController.getCurrentColorIndicator()),
            "BƯỚC 1/8",
            "LUẬT CƠ BẢN",
            "Giữa bàn đang là lá bài ĐỎ 5 (kèm biểu tượng hình thoi báo hiệu màu Đỏ bên cạnh).\n\n" +
            "Ở mỗi lượt, bạn phải đánh một lá bài CÙNG MÀU (màu Đỏ) hoặc CÙNG SỐ (số 5) với lá bài này!",
            "Hãy quan sát cọc bài giữa bàn rồi bấm Tiếp tục.",
            this::enterStep2SameColor,
            25, 130
        );
    }

    /**
     * BƯỚC 2/8: Đánh lá bài Cùng Màu (Đỏ 8 lên Đỏ 5)
     */
    private void enterStep2SameColor() {
        currentStep = Step.STEP_2_SAME_COLOR;
        waitingForBot = false;

        Card target = new Card(CardColor.RED, CardValue.EIGHT);
        Node cardNode = boardController.findCardNodeInHand(target);

        showSpotlightOnNode(
            cardNode,
            Arrays.asList(boardController.getBtnPlayCard(), boardController.getDiscardPileContainer()),
            "BƯỚC 2/8",
            "ĐÁNH BÀI CÙNG MÀU",
            "Trên bàn đang là ĐỎ 5. Trong 7 lá bài trên tay bạn, chỉ có duy nhất lá ĐỎ 8 là CÙNG MÀU ĐỎ!",
            "Hãy NHẤP ĐÚP vào lá ĐỎ 8 (hoặc chọn lá Đỏ 8 rồi bấm nút ĐÁNH BÀI)!",
            null,
            25, 165
        );
    }

    /**
     * BƯỚC 3/8: Đánh lá bài Cùng Số để đổi màu (Vàng 2 lên Đỏ 2)
     */
    private void enterStep3SameNumber() {
        currentStep = Step.STEP_3_SAME_NUMBER;
        waitingForBot = false;

        Card target = new Card(CardColor.YELLOW, CardValue.TWO);
        Node cardNode = boardController.findCardNodeInHand(target);

        showSpotlightOnNode(
            cardNode,
            Arrays.asList(boardController.getBtnPlayCard(), boardController.getDiscardPileContainer()),
            "BƯỚC 3/8",
            "ĐÁNH BÀI CÙNG SỐ",
            "Bot vừa đánh lá ĐỎ 2. Trên tay bạn đã hết bài màu Đỏ, nhưng bạn có lá VÀNG 2 (CÙNG SỐ 2 với Đỏ 2)!",
            "Hãy NHẤP ĐÚP vào lá VÀNG 2 để đánh và chuyển màu bàn chơi sang màu Vàng!",
            null,
            25, 165
        );
    }

    /**
     * BƯỚC 4/8: Không có bài hợp lệ -> Phải bấm Rút bài từ Cọc bài úp
     */
    private void enterStep4DrawCard() {
        currentStep = Step.STEP_4_DRAW_CARD;
        waitingForBot = false;

        showSpotlightOnNode(
            boardController.getDrawPileContainer(),
            null,
            "BƯỚC 4/8",
            "RÚT BÀI TỪ NỌC",
            "Bot vừa đánh lá XANH LÁ 2 (cùng số 2) làm đổi màu bàn chơi sang Xanh Lá!\n\n" +
            "Nhìn xuống 5 lá trên tay bạn: Không có lá nào màu Xanh Lá, cũng không có số 2. Khi bị kẹt bài, bạn phải rút 1 lá từ CỌC BÀI ÚP!",
            "Hãy nhấp chuột vào CỌC BÀI ÚP đang sáng ở giữa bàn!",
            null,
            25, 140
        );
    }

    /**
     * BƯỚC 5/8: Đánh lá Đổi Màu (Wild) vừa bốc được
     */
    private void enterStep5PlayWild() {
        currentStep = Step.STEP_5_PLAY_WILD;
        waitingForBot = false;

        Card target = new Card(CardColor.WILD, CardValue.WILD);
        Node cardNode = boardController.findCardNodeInHand(target);

        showSpotlightOnNode(
            cardNode,
            Arrays.asList(boardController.getBtnPlayCard(), boardController.getDiscardPileContainer()),
            "BƯỚC 5/8",
            "SỬ DỤNG LÁ ĐỔI MÀU",
            "May quá, bạn vừa rút được lá ĐỔI MÀU! Lá bài quyền năng này có thể đánh đè lên BẤT KỲ màu nào.\n\n" +
            "Trên tay bạn đang nắm tới 4 lá màu Xanh Dương, hãy đánh lá Đổi Màu để chuyển bàn chơi về màu chủ lực của bạn!",
            "Hãy NHẤP ĐÚP vào lá ĐỔI MÀU ở cuối tay bài!",
            null,
            25, 145
        );
    }

    /**
     * BƯỚC 6A/8: Đánh lá Cấm Lượt (Xanh Dương Skip)
     */
    private void enterStep6ASkip() {
        currentStep = Step.STEP_6A_PLAY_SKIP;
        waitingForBot = false;

        Card target = new Card(CardColor.BLUE, CardValue.SKIP);
        Node cardNode = boardController.findCardNodeInHand(target);

        showSpotlightOnNode(
            cardNode,
            Arrays.asList(boardController.getBtnPlayCard(), boardController.getDiscardPileContainer()),
            "BƯỚC 6/8 (COMBO 1/3)",
            "LÁ CẤM LƯỢT",
            "Bot không có bài màu Xanh Dương nên vừa phải bốc bài và mất lượt!\n\n" +
            "Giờ hãy tung lá CẤM LƯỢT màu Xanh Dương để khóa lượt của Bot và giành quyền đánh tiếp ngay lập tức!",
            "Hãy NHẤP ĐÚP vào lá Xanh Dương CẤM LƯỢT!",
            null,
            25, 145
        );
    }

    /**
     * BƯỚC 6B/8: Đánh lá Đảo Chiều (Xanh Dương Reverse)
     */
    private void enterStep6BReverse() {
        currentStep = Step.STEP_6B_PLAY_REVERSE;
        waitingForBot = false;

        Card target = new Card(CardColor.BLUE, CardValue.REVERSE);
        Node cardNode = boardController.findCardNodeInHand(target);

        showSpotlightOnNode(
            cardNode,
            Arrays.asList(boardController.getBtnPlayCard(), boardController.getDiscardPileContainer()),
            "BƯỚC 6/8 (COMBO 2/3)",
            "LÁ ĐẢO CHIỀU",
            "Bot đã bị cấm lượt! Lá ĐẢO CHIỀU giúp đảo ngược chiều vòng đánh.\n\n" +
            "Mẹo cực hay: Trong bàn 2 người chơi, lá Đảo Chiều hoạt động giống hệt lá Cấm Lượt!",
            "Hãy NHẤP ĐÚP vào lá Xanh Dương ĐẢO CHIỀU!",
            null,
            25, 145
        );
    }

    /**
     * BƯỚC 6C/8: Đánh lá Rút 2 (Xanh Dương +2)
     */
    private void enterStep6CDrawTwo() {
        currentStep = Step.STEP_6C_PLAY_DRAW_TWO;
        waitingForBot = false;

        Card target = new Card(CardColor.BLUE, CardValue.DRAW_TWO);
        Node cardNode = boardController.findCardNodeInHand(target);

        showSpotlightOnNode(
            cardNode,
            Arrays.asList(boardController.getBtnPlayCard(), boardController.getDiscardPileContainer()),
            "BƯỚC 6/8 (COMBO 3/3)",
            "LÁ RÚT 2 BÀI",
            "Combo liên hoàn! Tiếp theo hãy tung lá RÚT 2 màu Xanh Dương.\n\n" +
            "Lá +2 sẽ buộc đối thủ phải bốc phạt 2 lá bài VÀ bị mất lượt, giúp bạn tiếp tục giữ lượt!",
            "Hãy NHẤP ĐÚP vào lá Xanh Dương +2!",
            null,
            25, 145
        );
    }

    /**
     * BƯỚC 7A/8: Đánh lá Xanh Dương 9 để xuống còn đúng 1 lá bài
     */
    private void enterStep7APlayToOne() {
        currentStep = Step.STEP_7A_PLAY_TO_ONE;
        waitingForBot = false;

        Card target = new Card(CardColor.BLUE, CardValue.NINE);
        Node cardNode = boardController.findCardNodeInHand(target);

        showSpotlightOnNode(
            cardNode,
            Arrays.asList(boardController.getBtnPlayCard(), boardController.getDiscardPileContainer()),
            "BƯỚC 7/8 (PHẦN 1)",
            "CHUẨN BỊ HÔ UNO",
            "Bot vừa bị phạt bốc 2 lá bài và mất lượt! Trên tay bạn giờ chỉ còn 2 lá bài.\n\n" +
            "Hãy đánh lá XANH DƯƠNG 9 xuống bàn để rút xuống còn 1 lá cuối cùng!",
            "Hãy NHẤP ĐÚP vào lá XANH DƯƠNG 9!",
            null,
            25, 145
        );
    }

    /**
     * BƯỚC 7B/8: Bấm nút UNO! khẩn cấp khi trên tay chỉ còn 1 lá
     */
    private void enterStep7BCallUno() {
        currentStep = Step.STEP_7B_CALL_UNO;
        waitingForBot = false;

        showSpotlightOnNode(
            boardController.getBtnCallUno(),
            null,
            "BƯỚC 7/8 (KHẨN CẤP!)",
            "BẤM NÚT HÔ UNO!",
            "CẢNH BÁO! Trên tay bạn hiện chỉ còn ĐÚNG 1 LÁ BÀI!\n\n" +
            "Theo luật UNO, khi còn 1 lá bài bạn phải bấm ngay nút hình thoi UNO! trong vòng 5 giây, nếu quên sẽ bị phạt bốc 2 lá bài!",
            "Hãy bấm ngay vào nút hình thoi UNO! đang phát sáng bên phải cọc bài!",
            null,
            25, 150
        );
    }

    /**
     * BƯỚC 8/8: Đánh lá cuối cùng (Vàng 9 lên Đỏ 9) và giành chiến thắng
     */
    private void enterStep8FinalWin() {
        currentStep = Step.STEP_8_FINAL_WIN;
        waitingForBot = false;

        Card target = new Card(CardColor.YELLOW, CardValue.NINE);
        Node cardNode = boardController.findCardNodeInHand(target);

        showSpotlightOnNode(
            cardNode,
            Arrays.asList(boardController.getBtnPlayCard(), boardController.getDiscardPileContainer()),
            "BƯỚC 8/8 (QUYẾT ĐỊNH)",
            "ĐÁNH LÁ BÀI CUỐI CÙNG",
            "Bot vừa đánh lá ĐỎ 9. Lá bài cuối cùng trên tay bạn là VÀNG 9 — hoàn toàn hợp lệ vì CÙNG SỐ 9 với Đỏ 9!\n\n" +
            "Hãy đánh nốt lá bài cuối cùng này để giành chiến thắng!",
            "Hãy NHẤP ĐÚP vào lá VÀNG 9 để về đích!",
            null,
            25, 150
        );
    }

    // =========================================================================
    // BẮT SỰ KIỆN NGƯỜI CHƠI & KIỂM SOÁT THAO TÁC (ACTION GUARD)
    // =========================================================================

    private void setupActionListenerAndGuard() {
        boardController.setTutorialGuard(new GameBoardUIBridge.TutorialActionGuard() {
            @Override
            public boolean canSelectCard(Card card) {
                if (waitingForBot || card == null) return false;
                Card expected = getExpectedCardForCurrentStep();
                if (expected != null && card.getColor() == expected.getColor() && card.getValue() == expected.getValue()) {
                    // Nếu người dùng click đơn vào lá bài đúng, dịch ngón tay sang chỉ thêm nút ĐÁNH BÀI để hỗ trợ
                    return true;
                }
                nudgeCoachmark();
                return false;
            }

            @Override
            public boolean canPlayCard(Card card) {
                if (waitingForBot || card == null) return false;
                Card expected = getExpectedCardForCurrentStep();
                if (expected != null && card.getColor() == expected.getColor() && card.getValue() == expected.getValue()) {
                    return true;
                }
                nudgeCoachmark();
                return false;
            }

            @Override
            public boolean canDrawCard() {
                if (!waitingForBot && currentStep == Step.STEP_4_DRAW_CARD) {
                    return true;
                }
                nudgeCoachmark();
                return false;
            }

            @Override
            public boolean canCallUno() {
                if (!waitingForBot && currentStep == Step.STEP_7B_CALL_UNO) {
                    return true;
                }
                nudgeCoachmark();
                return false;
            }

            @Override
            public boolean canChooseColor(CardColor color) {
                if (currentStep == Step.STEP_5B_CHOOSE_COLOR && color == CardColor.BLUE) {
                    return true;
                }
                nudgeCoachmark();
                return false;
            }

            @Override
            public void onColorPickerOpened(HBox colorButtons, StackPane overlay) {
                if (currentStep == Step.STEP_5_PLAY_WILD) {
                    currentStep = Step.STEP_5B_CHOOSE_COLOR;
                    // Tìm đúng nút màu Xanh Dương (index 2 trong colors.png: RED, GREEN, BLUE, YELLOW)
                    Node blueBtn = colorButtons;
                    for (Node child : colorButtons.getChildren()) {
                        if (child.getUserData() == CardColor.BLUE) {
                            blueBtn = child;
                            break;
                        }
                    }
                    showSpotlightOnNode(
                        blueBtn,
                        null,
                        "BƯỚC 5/8 (CHỌN MÀU)",
                        "CHỌN MÀU XANH DƯƠNG",
                        "Bảng chọn 4 màu đã hiện lên! Vì trên tay bạn có tới 4 lá màu Xanh Dương, hãy chọn màu XANH DƯƠNG để làm chủ thế trận!",
                        "Hãy bấm vào ô hình thoi màu XANH DƯƠNG đang sáng!",
                        null,
                        25, 65
                    );
                }
            }
        });

        boardController.setActionListener(new GameBoardUIBridge.GameActionListener() {
            @Override
            public void onPlayCard(Card card, CardColor chosenColor) {
                handlePlayerPlayedCardInTutorial(card, chosenColor);
            }

            @Override
            public void onDrawCard() {
                handlePlayerDrewCardInTutorial();
            }

            @Override
            public void onCallUno() {
                handlePlayerCalledUnoInTutorial();
            }

            @Override
            public void onSendMessage(String message) {
                boardController.addChatMessage(playerUsername, message, Color.web("#f1c40f"));
                boardController.addChatMessage(BOT_NAME, "Hãy làm theo chỉ dẫn đang sáng trên màn hình nhé!", Color.web("#2ecc71"));
            }

            @Override
            public void onLeaveGame() {
                closeStage();
            }

            @Override
            public void onGameFinished() {
                closeStage();
            }
        });
    }

    private Card getExpectedCardForCurrentStep() {
        switch (currentStep) {
            case STEP_2_SAME_COLOR:
                return new Card(CardColor.RED, CardValue.EIGHT);
            case STEP_3_SAME_NUMBER:
                return new Card(CardColor.YELLOW, CardValue.TWO);
            case STEP_5_PLAY_WILD:
                return new Card(CardColor.WILD, CardValue.WILD);
            case STEP_6A_PLAY_SKIP:
                return new Card(CardColor.BLUE, CardValue.SKIP);
            case STEP_6B_PLAY_REVERSE:
                return new Card(CardColor.BLUE, CardValue.REVERSE);
            case STEP_6C_PLAY_DRAW_TWO:
                return new Card(CardColor.BLUE, CardValue.DRAW_TWO);
            case STEP_7A_PLAY_TO_ONE:
                return new Card(CardColor.BLUE, CardValue.NINE);
            case STEP_8_FINAL_WIN:
                return new Card(CardColor.YELLOW, CardValue.NINE);
            default:
                return null;
        }
    }

    // =========================================================================
    // XỬ LÝ DIỄN BIẾN SAU MỖI HÀNH ĐỘNG CỦA NGƯỜI CHƠI & PHẢN ĐÒN CỦA BOT
    // =========================================================================

    private void handlePlayerPlayedCardInTutorial(Card played, CardColor chosenColor) {
        hideSpotlight();
        waitingForBot = true;

        // Xóa lá bài vừa đánh khỏi tay người chơi
        for (int i = 0; i < playerHand.size(); i++) {
            Card c = playerHand.get(i);
            if (c.getColor() == played.getColor() && c.getValue() == played.getValue()) {
                playerHand.remove(i);
                break;
            }
        }

        topCard = played;
        activeColor = played.isWild() ? chosenColor : played.getColor();
        boardController.setHandCards(new ArrayList<>(playerHand));

        switch (currentStep) {
            case STEP_2_SAME_COLOR: {
                // Người chơi vừa đánh Đỏ 8 (còn 6 lá) -> Tới lượt Bot đánh Đỏ 2 (Cùng màu Đỏ)
                pushGameStateToView(1, playerUsername + " đã đánh lá Đỏ 8.");
                runAfterDelay(950, () -> {
                    botCardCount = 6;
                    topCard = new Card("bot_red_2", CardColor.RED, CardValue.TWO);
                    activeColor = CardColor.RED;
                    boardController.addChatMessage(BOT_NAME, "Bạn đánh Đỏ 8 rất chuẩn! Mình đánh tiếp lá ĐỎ 2 (cùng màu Đỏ) nhé!", Color.web("#2ecc71"));
                    pushGameStateToView(0, BOT_NAME + " đã đánh lá Đỏ 2.");
                    runAfterDelay(900, this::enterStep3SameNumber);
                });
                break;
            }

            case STEP_3_SAME_NUMBER: {
                // Người chơi vừa đánh Vàng 2 (còn 5 lá) -> Tới lượt Bot đánh Xanh Lá 2 (Cùng số 2)
                pushGameStateToView(1, playerUsername + " đã đánh lá Vàng 2 (đổi sang màu Vàng).");
                runAfterDelay(950, () -> {
                    botCardCount = 5;
                    topCard = new Card("bot_green_2", CardColor.GREEN, CardValue.TWO);
                    activeColor = CardColor.GREEN;
                    boardController.addChatMessage(BOT_NAME, "Hay lắm! Giờ mình cũng đánh lá XANH LÁ 2 (cùng số 2) để đổi sang màu Xanh Lá!", Color.web("#2ecc71"));
                    pushGameStateToView(0, BOT_NAME + " đã đánh lá Xanh Lá 2.");
                    runAfterDelay(900, this::enterStep4DrawCard);
                });
                break;
            }

            case STEP_5_PLAY_WILD:
            case STEP_5B_CHOOSE_COLOR: {
                // Người chơi vừa đánh Wild và chọn màu Xanh Dương (còn 5 lá)
                pushGameStateToView(1, playerUsername + " đã đánh Đổi Màu (Chọn màu Xanh Dương).");
                runAfterDelay(950, () -> {
                    // Bot không có màu Xanh Dương -> Bot phải bốc 1 lá bài và mất lượt!
                    botCardCount = 5; // Từ 4 tăng lên 5 lá
                    boardController.addChatMessage(BOT_NAME, "Ái chà, mình không có bài màu Xanh Dương nên phải rút 1 lá và nhường lượt cho bạn!", Color.web("#f39c12"));
                    pushGameStateToView(0, BOT_NAME + " không có bài màu Xanh Dương, phải bốc 1 lá!");
                    runAfterDelay(950, this::enterStep6ASkip);
                });
                break;
            }

            case STEP_6A_PLAY_SKIP: {
                // Người chơi đánh Xanh Dương Skip -> Bot mất lượt, người chơi đi tiếp ngay!
                pushGameStateToView(0, playerUsername + " đánh Cấm Lượt (Skip)! " + BOT_NAME + " bị mất lượt.");
                runAfterDelay(950, this::enterStep6BReverse);
                break;
            }

            case STEP_6B_PLAY_REVERSE: {
                // Người chơi đánh Xanh Dương Reverse -> Bot mất lượt, người chơi đi tiếp ngay!
                pushGameStateToView(0, playerUsername + " đánh Đảo Chiều (Reverse)! Bạn tiếp tục được đánh.");
                runAfterDelay(950, this::enterStep6CDrawTwo);
                break;
            }

            case STEP_6C_PLAY_DRAW_TWO: {
                // Người chơi đánh Xanh Dương +2 -> Bot bị cộng 2 lá (5 -> 7 lá) và mất lượt!
                botCardCount = 7;
                pushGameStateToView(0, playerUsername + " đánh Rút 2 (+2)! " + BOT_NAME + " phải bốc 2 lá và mất lượt.");
                boardController.addChatMessage(BOT_NAME, "Ui da! Combo 3 lá chức năng của bạn làm mình không kịp trở tay luôn!", Color.web("#e67e22"));
                runAfterDelay(1100, this::enterStep7APlayToOne);
                break;
            }

            case STEP_7A_PLAY_TO_ONE: {
                // Người chơi đánh Xanh Dương 9 -> Trên tay chỉ còn 1 lá (Vàng 9)! Phải bấm nút UNO! ngay!
                pushGameStateToView(0, playerUsername + " đã đánh Xanh Dương 9 (Chỉ còn 1 lá bài trên tay!).");
                runAfterDelay(500, this::enterStep7BCallUno);
                break;
            }

            case STEP_8_FINAL_WIN: {
                // Người chơi đánh lá cuối cùng (Vàng 9) -> Về 0 lá -> Chiến thắng!
                currentStep = Step.COMPLETED;
                pushGameStateToView(0, playerUsername + " đã đánh hết bài và hoàn thành khóa huấn luyện!");
                FXSoundHelper.playGameOver();
                runAfterDelay(650, this::showTutorialCompletionModal);
                break;
            }

            default:
                break;
        }
    }

    private void handlePlayerDrewCardInTutorial() {
        if (currentStep != Step.STEP_4_DRAW_CARD) return;
        hideSpotlight();
        waitingForBot = true;

        // Người chơi bốc được lá Đổi Màu (Wild) -> Tay tăng từ 5 lên 6 lá
        Card drawnWild = new Card("tut_drawn_wild", CardColor.WILD, CardValue.WILD);
        playerHand.add(drawnWild);
        boardController.setHandCards(new ArrayList<>(playerHand));
        pushGameStateToView(1, playerUsername + " đã rút 1 lá bài từ nọc.");

        // Tới lượt Bot: Bot đánh lá Xanh Lá 6 (Cùng màu Xanh Lá, bài Bot giảm từ 5 còn 4 lá)
        runAfterDelay(950, () -> {
            botCardCount = 4;
            topCard = new Card("bot_green_6", CardColor.GREEN, CardValue.SIX);
            activeColor = CardColor.GREEN;
            boardController.addChatMessage(BOT_NAME, "Bạn vừa bốc được lá Đổi Màu (Wild) kìa! Mình đánh XANH LÁ 6, giờ tới lượt bạn tung lá Wild đó!", Color.web("#2ecc71"));
            pushGameStateToView(0, BOT_NAME + " đã đánh lá Xanh Lá 6.");
            runAfterDelay(900, this::enterStep5PlayWild);
        });
    }

    private void handlePlayerCalledUnoInTutorial() {
        if (currentStep != Step.STEP_7B_CALL_UNO) return;
        hideSpotlight();
        waitingForBot = true;
        playerUnoCalled = true;

        boardController.addChatMessage(playerUsername, "UNO!!!", Color.web("#f1c40f"));
        pushGameStateToView(1, playerUsername + " đã hô UNO thành công!");

        // Tới lượt Bot: Trên bàn đang là Xanh Dương 9, Bot đánh lá Đỏ 9 (Cùng số 9!)
        runAfterDelay(1100, () -> {
            botCardCount = 6;
            topCard = new Card("bot_red_9", CardColor.RED, CardValue.NINE);
            activeColor = CardColor.RED;
            boardController.addChatMessage(BOT_NAME, "Bạn hô UNO nhanh đấy! Mình đánh lá ĐỎ 9 (cùng số 9), đố bạn kết thúc ván đấu!", Color.web("#2ecc71"));
            pushGameStateToView(0, BOT_NAME + " đã đánh lá Đỏ 9.");
            runAfterDelay(950, this::enterStep8FinalWin);
        });
    }

    private void pushGameStateToView(int currentTurnSeat, String actionLog) {
        GameState state = new GameState();
        state.setRoomId(999);
        state.setTopCard(topCard);
        state.setActiveColor(activeColor);
        state.setCurrentTurnSeat(currentTurnSeat);
        state.setDirection(1);
        state.setRemainingSeconds(15);
        state.setDrawPileCount(80);
        state.setGameOver(currentStep == Step.COMPLETED);
        if (currentStep == Step.COMPLETED) {
            state.setWinnerUsername(playerUsername);
        }
        state.setLastActionLog(actionLog);

        List<GameState.PlayerSummary> summaries = new ArrayList<>();
        summaries.add(new GameState.PlayerSummary(PLAYER_USER_ID, playerUsername, 0, playerHand.size(), playerUnoCalled, false));
        summaries.add(new GameState.PlayerSummary(BOT_USER_ID, BOT_NAME, 1, botCardCount, false, true));
        state.setPlayerSummaries(summaries);

        boardController.updateGameState(state, PLAYER_USER_ID);
    }

    // =========================================================================
    // BẢNG CHÚC MỪNG HOÀN THÀNH KHÓA HUẤN LUYỆN TÂN THỦ
    // =========================================================================

    private void showTutorialCompletionModal() {
        Platform.runLater(() -> {
            hideSpotlight();

            StackPane modalOverlay = new StackPane();
            modalOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.80);");
            modalOverlay.prefWidthProperty().bind(tutorialOverlayPane.widthProperty());
            modalOverlay.prefHeightProperty().bind(tutorialOverlayPane.heightProperty());

            VBox card = new VBox(16);
            card.setAlignment(Pos.CENTER);
            card.setMaxWidth(480);
            card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc);" +
                "-fx-background-radius: 20px;" +
                "-fx-border-color: #cbd5e1;" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 20px;" +
                "-fx-padding: 28px 34px;" +
                "-fx-effect: dropshadow(gaussian, rgba(2, 6, 23, 0.72), 30, 0.24, 0, 10);"
            );

            FontIcon crownIcon = new FontIcon("fas-graduation-cap");
            crownIcon.setIconSize(46);
            crownIcon.setIconColor(Color.web("#059669"));

            Label lblTitle = new Label("HOÀN THÀNH HƯỚNG DẪN TÂN THỦ!");
            lblTitle.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: #0f172a;"
            );

            VBox checklistBox = new VBox(8);
            checklistBox.setStyle(
                "-fx-background-color: #ecfdf5;" +
                "-fx-border-color: #a7f3d0;" +
                "-fx-border-width: 1.2px;" +
                "-fx-border-radius: 12px;" +
                "-fx-background-radius: 12px;" +
                "-fx-padding: 14px 18px;"
            );

            Label lblIntro = new Label("Chúc mừng " + playerUsername + "! Bạn đã nắm vững toàn bộ kỹ năng UNO:");
            lblIntro.setWrapText(true);
            lblIntro.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: 700; -fx-font-size: 14px;");
            checklistBox.getChildren().add(lblIntro);

            String[] items = {
                "Đánh bài Cùng Màu & Cùng Số để chuyển màu bàn",
                "Rút bài từ nọc khi không có nước đi hợp lệ",
                "Làm chủ bộ bài chức năng: Đổi Màu (Wild), Cấm Lượt, Đảo Chiều, +2",
                "Bấm nút Hô UNO! kịp thời khi còn 1 lá và giành chiến thắng!"
            };
            for (String itemText : items) {
                Label itemLbl = new Label(itemText);
                itemLbl.setWrapText(true);
                FontIcon checkBullet = new FontIcon("fas-check-circle");
                checkBullet.setIconSize(14);
                checkBullet.setIconColor(Color.web("#059669"));
                itemLbl.setGraphic(checkBullet);
                itemLbl.setGraphicTextGap(8);
                itemLbl.setStyle("-fx-text-fill: #334155; -fx-font-weight: 500; -fx-font-size: 13.5px;");
                checklistBox.getChildren().add(itemLbl);
            }

            HBox btnRow = new HBox(16);
            btnRow.setAlignment(Pos.CENTER);
            VBox.setMargin(btnRow, new Insets(6, 0, 0, 0));

            Button btnReplay = new Button("HƯỚNG DẪN LẠI");
            FontIcon replayIcon = new FontIcon("fas-redo");
            replayIcon.setIconSize(14);
            replayIcon.setIconColor(Color.web("#334155"));
            btnReplay.setGraphic(replayIcon);
            btnReplay.setCursor(Cursor.HAND);
            btnReplay.setStyle(
                "-fx-background-color: #f1f5f9;" +
                "-fx-border-color: #cbd5e1;" +
                "-fx-border-radius: 10px;" +
                "-fx-text-fill: #1e293b;" +
                "-fx-font-weight: 700;" +
                "-fx-font-size: 13.5px;" +
                "-fx-background-radius: 10px;" +
                "-fx-padding: 10px 16px;"
            );
            btnReplay.setOnAction(e -> {
                FXSoundHelper.playButtonClick();
                tutorialOverlayPane.getChildren().remove(modalOverlay);
                startScriptedTutorialMatch();
            });

            Button btnPlayWithBot = new Button("ĐẤU TẬP VỚI BOT");
            FontIcon botIcon = new FontIcon("fas-robot");
            botIcon.setIconSize(14);
            botIcon.setIconColor(Color.WHITE);
            btnPlayWithBot.setGraphic(botIcon);
            btnPlayWithBot.setCursor(Cursor.HAND);
            btnPlayWithBot.setStyle(
                "-fx-background-color: linear-gradient(to right, #3b82f6, #1d4ed8);" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-weight: 800;" +
                "-fx-font-size: 13.5px;" +
                "-fx-background-radius: 10px;" +
                "-fx-padding: 10px 18px;" +
                "-fx-effect: dropshadow(gaussian, rgba(59, 130, 246, 0.4), 12, 0, 0, 2);"
            );
            btnPlayWithBot.setOnAction(e -> {
                FXSoundHelper.playButtonClick();
                javafx.stage.Window owner = (stage != null) ? stage.getOwner() : null;
                closeStage();
                BotPracticeGameFXStage botStage = new BotPracticeGameFXStage(owner, playerUsername);
                botStage.show();
            });

            Button btnBackLobby = new Button("VÀO SẢNH CHIẾN");
            FontIcon checkIcon = new FontIcon("fas-play");
            checkIcon.setIconSize(14);
            checkIcon.setIconColor(Color.WHITE);
            btnBackLobby.setGraphic(checkIcon);
            btnBackLobby.setCursor(Cursor.HAND);
            btnBackLobby.setStyle(
                "-fx-background-color: linear-gradient(to right, #10b981, #059669);" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-weight: 800;" +
                "-fx-font-size: 13.5px;" +
                "-fx-background-radius: 10px;" +
                "-fx-padding: 10px 18px;" +
                "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.4), 12, 0, 0, 2);"
            );
            btnBackLobby.setOnAction(e -> {
                FXSoundHelper.playButtonClick();
                closeStage();
            });

            btnRow.getChildren().addAll(btnReplay, btnPlayWithBot, btnBackLobby);
            card.getChildren().addAll(crownIcon, lblTitle, checklistBox, btnRow);
            modalOverlay.getChildren().add(card);
            tutorialOverlayPane.getChildren().add(modalOverlay);

            ScaleTransition st = new ScaleTransition(Duration.millis(320), card);
            st.setFromX(0.65);
            st.setFromY(0.65);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void runAfterDelay(long millis, Runnable action) {
        PauseTransition pt = new PauseTransition(Duration.millis(millis));
        pt.setOnFinished(e -> {
            if (stage != null && stage.isShowing() && action != null) {
                action.run();
            }
        });
        pt.play();
    }

    private void stopAllAnimations() {
        if (pulseTimeline != null) pulseTimeline.stop();
        if (fingerSpriteTimeline != null) fingerSpriteTimeline.stop();
    }

    private void closeStage() {
        stopAllAnimations();
        if (stage != null) {
            stage.close();
        }
        FXSoundHelper.playLobbyMusic();
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
        } catch (Exception ignored) {}
        return null;
    }
}
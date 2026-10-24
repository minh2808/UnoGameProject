package com.ptit.uno.client.view.fx.game;

import com.ptit.uno.model.Card;
import com.ptit.uno.model.CardColor;
import com.ptit.uno.model.CardValue;
import com.ptit.uno.model.GameState;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * SpectatorUIBridge: Cầu nối UI Code-Behind cho spectator_view.fxml.
 * Thiết kế chuẩn đồng nhất 100% với giao diện bàn chơi chính (game_board.fxml),
 * tích hợp toàn bộ Animation Engine (bài bay, cấm lượt, đổi chiều, +2, +4, UNO cloud)
 * và hệ thống âm thanh SFX đồng bộ thời gian thực.
 */
public class SpectatorUIBridge implements Initializable {

    @FXML private BorderPane mainBoardPane;
    @FXML private Pane animationLayer;

    // Khu vực Host (Chủ phòng - Trên)
    @FXML private Label lblHostName;
    @FXML private Label lblHostCards;
    @FXML private Label lblHostStatus;
    @FXML private HBox hostHandBox;

    // Khu vực Guest (Khách - Dưới)
    @FXML private Label lblGuestName;
    @FXML private Label lblGuestCards;
    @FXML private Label lblGuestStatus;
    @FXML private HBox guestHandBox;

    // Bàn chơi trung tâm
    @FXML private HBox turnIndicatorBox;
    @FXML private Label lblCountdown;
    @FXML private Label lblTurnInfo;
    @FXML private Circle turnIndicatorDot;

    @FXML private StackPane drawPileContainer;
    @FXML private ImageView drawPileImageView;
    @FXML private StackPane discardPileContainer;
    @FXML private ImageView discardPileImageView;
    @FXML private ImageView currentColorIndicator;
    @FXML private ImageView unoBadge;

    // Góc phải & Chat (Chế độ xem chỉ đọc cho Khán giả)
    @FXML private Button btnLeave;
    @FXML private ListView<TextFlow> chatListView;

    private final ObservableList<TextFlow> chatMessages = FXCollections.observableArrayList();
    private Runnable onLeaveAction;

    private int remainingSeconds = 15;
    private Timeline countdownTimeline;

    // Biến giám sát chuyển động (Animation tracking & SFX)
    private Card previousTopCard = null;
    private int previousHostCardCount = -1;
    private int previousGuestCardCount = -1;
    private boolean previousHostUno = false;
    private boolean previousGuestUno = false;
    private int previousTurnSeat = -1;
    private boolean isGameOverHandled = false;
    private final Random random = new Random();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (chatListView != null) {
            chatListView.setItems(chatMessages);
        }
        renderCardBack(drawPileImageView);
    }

    public void setOnLeaveAction(Runnable r) { 
        this.onLeaveAction = r; 
    }

    @FXML
    private void handleLeave(ActionEvent event) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        if (onLeaveAction != null) {
            onLeaveAction.run();
        }
    }

    // ==========================================
    // CẬP NHẬT TRẠNG THÁI TỪ SERVER & ANIMATION
    // ==========================================

    public void updateGameState(GameState state, List<Card> hostHand, List<Card> guestHand) {
        Platform.runLater(() -> {
            try {
                if (state == null) {
                    if (countdownTimeline != null) countdownTimeline.stop();
                    javafx.stage.Window owner = (lblTurnInfo != null && lblTurnInfo.getScene() != null)
                            ? lblTurnInfo.getScene().getWindow()
                            : null;
                    com.ptit.uno.client.view.fx.util.UnoNotification.showError(
                            owner,
                            "Lỗi xem trận đấu",
                            "Không thể tải dữ liệu bàn đấu! Trận đấu chưa khởi tạo xong hoặc đã kết thúc."
                    );
                    if (onLeaveAction != null) {
                        onLeaveAction.run();
                    }
                    return;
                }

                int hostCards = (hostHand != null) ? hostHand.size() : 0;
                int guestCards = (guestHand != null) ? guestHand.size() : 0;

                // 1. Cập nhật bài trên tay của cả 2 người chơi (ngửa cho khán giả quan sát)
                renderHand(hostHandBox, hostHand);
                renderHand(guestHandBox, guestHand);

                if (lblHostCards != null) lblHostCards.setText(hostCards + " lá");
                if (lblGuestCards != null) lblGuestCards.setText(guestCards + " lá");

                // 2. Kiểm tra biến động lá bài trên bàn để kích hoạt Animation bài bay & SFX
                Card top = state.getTopCard();
                if (top != null) {
                    boolean isNewCard = (previousTopCard == null || !top.equals(previousTopCard));

                    if (isNewCard) {
                        if (previousTopCard == null) {
                            // Lần đầu vào xem hoặc đầu ván: hiển thị ngay lá bài hiện tại (không cần hiệu ứng xào/chia bài)
                            setTopCard(top, state.getActiveColor());
                        } else {
                            // Người chơi đánh bài trong trận: chạy animation bài bay
                            boolean playedByHost = false;
                            if (previousHostCardCount != -1 && hostCards < previousHostCardCount) {
                                playedByHost = true;
                            } else if (previousGuestCardCount != -1 && guestCards < previousGuestCardCount) {
                                playedByHost = false;
                            } else {
                                playedByHost = (previousTurnSeat == 0);
                            }

                            if (playedByHost) {
                                animatePlayCardFromHost(top, state.getActiveColor());
                            } else {
                                animatePlayCardFromGuest(top, state.getActiveColor());
                            }
                            triggerSpecialEffectIfAny(top);
                        }
                        previousTopCard = top;
                    } else {
                        setTopCard(top, state.getActiveColor());
                    }
                }

                // 3. Xử lý animation rút bài khi số lượng bài tăng lên
                if (previousHostCardCount != -1 && hostCards > previousHostCardCount) {
                    animateDrawCardToHost();
                }
                if (previousGuestCardCount != -1 && guestCards > previousGuestCardCount) {
                    animateDrawCardToGuest();
                }
                previousHostCardCount = hostCards;
                previousGuestCardCount = guestCards;

                // 4. Cập nhật và kích hoạt hiệu ứng UNO (Bong bóng thoại & SFX)
                boolean hostUnoState = false;
                boolean guestUnoState = false;
                if (state.getPlayerSummaries() != null) {
                    for (GameState.PlayerSummary ps : state.getPlayerSummaries()) {
                        if (ps.getSeatNumber() == 0 && ps.isUno()) hostUnoState = true;
                        if (ps.getSeatNumber() == 1 && ps.isUno()) guestUnoState = true;
                    }
                }
                if (hostCards == 1) hostUnoState = true;
                if (guestCards == 1) guestUnoState = true;

                // Kích hoạt hiệu ứng bong bóng thoại UNO nếu có người chơi vừa đạt trạng thái UNO
                if (!previousHostUno && hostUnoState) {
                    FXSoundHelper.playUnoSnap();
                    showUnoCloud(true);
                }
                if (!previousGuestUno && guestUnoState) {
                    FXSoundHelper.playUnoSnap();
                    showUnoCloud(false);
                }
                previousHostUno = hostUnoState;
                previousGuestUno = guestUnoState;

                // Đèn huy hiệu UNO cạnh bàn đấu phát sáng
                boolean isUnoActive = hostUnoState || guestUnoState;
                if (unoBadge != null) {
                    if (isUnoActive) {
                        unoBadge.setStyle("-fx-opacity: 1.0; -fx-effect: dropshadow(gaussian, #f39c12, 18, 0.7, 0, 0);");
                    } else {
                        unoBadge.setStyle("-fx-opacity: 0.35; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 8, 0, 0, 3);");
                    }
                }

                // 5. Cập nhật lượt đi và viên thuốc đếm ngược trung tâm
                previousTurnSeat = state.getCurrentTurnSeat();
                boolean isHostTurn = (state.getCurrentTurnSeat() == 0);
                if (isHostTurn) {
                    if (lblHostStatus != null) {
                        lblHostStatus.setText("Tới lượt");
                        lblHostStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    }
                    if (lblGuestStatus != null) {
                        lblGuestStatus.setText("Đang chờ");
                        lblGuestStatus.setStyle("-fx-text-fill: #b3c2b8; -fx-font-weight: normal;");
                    }
                    if (lblTurnInfo != null) {
                        lblTurnInfo.setText("Lượt: " + (lblHostName != null ? lblHostName.getText() : "Chủ phòng"));
                    }
                    if (turnIndicatorDot != null) {
                        turnIndicatorDot.setFill(Color.web("#e67e22"));
                    }
                    if (turnIndicatorBox != null) {
                        turnIndicatorBox.getStyleClass().remove("turn-timer-pill-opponent-turn");
                        if (!turnIndicatorBox.getStyleClass().contains("turn-timer-pill-my-turn")) {
                            turnIndicatorBox.getStyleClass().add("turn-timer-pill-my-turn");
                        }
                    }
                } else {
                    if (lblGuestStatus != null) {
                        lblGuestStatus.setText("Tới lượt");
                        lblGuestStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    }
                    if (lblHostStatus != null) {
                        lblHostStatus.setText("Đang chờ");
                        lblHostStatus.setStyle("-fx-text-fill: #b3c2b8; -fx-font-weight: normal;");
                    }
                    if (lblTurnInfo != null) {
                        lblTurnInfo.setText("Lượt: " + (lblGuestName != null ? lblGuestName.getText() : "Khách"));
                    }
                    if (turnIndicatorDot != null) {
                        turnIndicatorDot.setFill(Color.web("#2980b9"));
                    }
                    if (turnIndicatorBox != null) {
                        turnIndicatorBox.getStyleClass().remove("turn-timer-pill-my-turn");
                        if (!turnIndicatorBox.getStyleClass().contains("turn-timer-pill-opponent-turn")) {
                            turnIndicatorBox.getStyleClass().add("turn-timer-pill-opponent-turn");
                        }
                    }
                }

                // 6. Kết thúc trận đấu
                if (state.isGameOver() && !isGameOverHandled) {
                    isGameOverHandled = true;
                    if (countdownTimeline != null) countdownTimeline.stop();
                    FXSoundHelper.playGameOver();
                    String winner = state.getWinnerUsername() != null ? state.getWinnerUsername() : "Không rõ";
                    Alert winAlert = new Alert(Alert.AlertType.INFORMATION);
                    winAlert.setTitle("Kết quả trận đấu");
                    winAlert.setHeaderText(null);
                    winAlert.setContentText("Trận đấu đã kết thúc!\nNgười chiến thắng: " + winner);
                    if (lblTurnInfo != null && lblTurnInfo.getScene() != null && lblTurnInfo.getScene().getWindow() != null) {
                        winAlert.initOwner(lblTurnInfo.getScene().getWindow());
                    }
                    winAlert.showAndWait();

                    if (onLeaveAction != null) {
                        onLeaveAction.run();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void setTopCard(Card card, CardColor activeColor) {
        if (card != null && discardPileImageView != null) {
            renderCardFront(discardPileImageView, card);
        }
        if (activeColor == null && card != null) {
            activeColor = card.getColor();
        }
        if (activeColor != null && discardPileContainer != null) {
            String borderCol;
            String colorImg = "colors.png";
            switch (activeColor) {
                case RED:
                    borderCol = "#e74c3c";
                    colorImg = "_oButRed.png";
                    break;
                case YELLOW:
                    borderCol = "#f1c40f";
                    colorImg = "_oButYellow.png";
                    break;
                case GREEN:
                    borderCol = "#2ecc71";
                    colorImg = "_oButGreen.png";
                    break;
                case BLUE:
                    borderCol = "#3498db";
                    colorImg = "_oButBlue.png";
                    break;
                default:
                    borderCol = "transparent";
                    break;
            }
            discardPileContainer.setStyle("-fx-border-color: " + borderCol + "; -fx-border-width: 3.5px; -fx-border-radius: 10px;");
            
            if (currentColorIndicator != null) {
                Image img = loadSpriteImage(colorImg);
                if (img != null) {
                    currentColorIndicator.setImage(img);
                }
            }
        }
    }

    // ==========================================
    // ANIMATION ENGINE & SFX (ĐỒNG NHẤT 100%)
    // ==========================================

    private Point2D getCenterOnAnimationLayer(Node node) {
        if (node == null || animationLayer == null || node.getScene() == null) {
            return new Point2D(575, 340);
        }
        javafx.geometry.Bounds bounds = node.localToScene(node.getBoundsInLocal());
        return animationLayer.sceneToLocal(bounds.getCenterX(), bounds.getCenterY());
    }

    private void animateFloatingCard(Point2D start, Point2D target, Image image, double endRotation, Runnable onFinished) {
        if (animationLayer == null || image == null) {
            if (onFinished != null) onFinished.run();
            return;
        }

        double cardW = 74;
        double cardH = 112;

        ImageView floatingView = new ImageView(image);
        floatingView.setFitWidth(cardW);
        floatingView.setFitHeight(cardH);
        floatingView.setPreserveRatio(true);
        floatingView.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.6)));
        floatingView.setMouseTransparent(true);

        floatingView.setLayoutX(start.getX() - cardW / 2.0);
        floatingView.setLayoutY(start.getY() - cardH / 2.0);

        animationLayer.getChildren().add(floatingView);

        TranslateTransition tt = new TranslateTransition(Duration.millis(320), floatingView);
        tt.setToX(target.getX() - start.getX());
        tt.setToY(target.getY() - start.getY());
        tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

        RotateTransition rt = new RotateTransition(Duration.millis(320), floatingView);
        rt.setToAngle(endRotation);

        ScaleTransition st = new ScaleTransition(Duration.millis(160), floatingView);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.12);
        st.setToY(1.12);
        st.setAutoReverse(true);
        st.setCycleCount(2);

        ParallelTransition pt = new ParallelTransition(floatingView, tt, rt, st);
        pt.setOnFinished(e -> {
            animationLayer.getChildren().remove(floatingView);
            if (onFinished != null) {
                onFinished.run();
            }
        });
        pt.play();
    }

    private void animatePlayCardFromHost(Card card, CardColor activeColor) {
        Point2D start = getCenterOnAnimationLayer(hostHandBox);
        Point2D target = getCenterOnAnimationLayer(discardPileContainer);

        FXSoundHelper.playCardPlay();
        double randomAngle = -6.0 + random.nextDouble() * 12.0;

        animateFloatingCard(start, target, FXCardHelper.getCardImage(card), randomAngle, () -> {
            setTopCard(card, activeColor);
        });
    }

    private void animatePlayCardFromGuest(Card card, CardColor activeColor) {
        Point2D start = getCenterOnAnimationLayer(guestHandBox);
        Point2D target = getCenterOnAnimationLayer(discardPileContainer);

        FXSoundHelper.playCardPlay();
        double randomAngle = -6.0 + random.nextDouble() * 12.0;

        animateFloatingCard(start, target, FXCardHelper.getCardImage(card), randomAngle, () -> {
            setTopCard(card, activeColor);
        });
    }

    private void animateDrawCardToHost() {
        Point2D start = getCenterOnAnimationLayer(drawPileContainer);
        Point2D target = getCenterOnAnimationLayer(hostHandBox);

        FXSoundHelper.playCardDraw();
        animateFloatingCard(start, target, FXCardHelper.getCardBackImage(), 0, null);
    }

    private void animateDrawCardToGuest() {
        Point2D start = getCenterOnAnimationLayer(drawPileContainer);
        Point2D target = getCenterOnAnimationLayer(guestHandBox);

        FXSoundHelper.playCardDraw();
        animateFloatingCard(start, target, FXCardHelper.getCardBackImage(), 0, null);
    }

    private void triggerSpecialEffectIfAny(Card card) {
        if (card == null) return;
        CardValue val = card.getValue();
        if (val == null) return;
        switch (val) {
            case SKIP:
                showSpecialEffectBanner("stop_turn.png", "CẤM LƯỢT!");
                FXSoundHelper.playSpecialCard();
                break;
            case REVERSE:
                showSpecialEffectBanner("change_clockwise.png", "ĐỔI CHIỀU!");
                FXSoundHelper.playSpecialCard();
                break;
            case DRAW_TWO:
                showSpecialEffectBanner("draw_2.png", "+2 LÁ!");
                FXSoundHelper.playSpecialCard();
                break;
            case WILD_DRAW_FOUR:
                showSpecialEffectBanner("draw_4.png", "+4 LÁ!");
                FXSoundHelper.playSpecialCard();
                break;
            default:
                break;
        }
    }

    public void showSpecialEffectBanner(String spriteFileName, String text) {
        if (animationLayer == null) return;

        Image img = loadSpriteImage(spriteFileName);
        if (img == null) return;

        VBox bannerBox = new VBox(6);
        bannerBox.setAlignment(Pos.CENTER);
        bannerBox.setMouseTransparent(true);

        ImageView iv = new ImageView(img);
        
        // Mỗi frame chuẩn của các hiệu ứng sprite đều có chiều rộng 292 pixel
        int frameWidth = 292;
        int cols = Math.max(1, (int) (img.getWidth() / frameWidth));
        int rows = (cols > 1) ? 2 : 1; 
        int frameHeight = (int) (img.getHeight() / rows);
        int frameCount = cols * rows;

        iv.setViewport(new javafx.geometry.Rectangle2D(0, 0, frameWidth, frameHeight));
        iv.setFitWidth(140);
        iv.setPreserveRatio(true);

        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f1c40f; -fx-effect: dropshadow(gaussian, #000, 8, 0, 0, 2);");

        bannerBox.getChildren().addAll(iv, lbl);

        Point2D center = new Point2D(mainBoardPane.getWidth() / 2.0, mainBoardPane.getHeight() / 2.0);
        bannerBox.setLayoutX(center.getX() - 80);
        bannerBox.setLayoutY(center.getY() - 100);

        animationLayer.getChildren().add(bannerBox);

        // Hiệu ứng Zoom nảy & Mờ dần
        ScaleTransition st = new ScaleTransition(Duration.millis(350), bannerBox);
        st.setFromX(0.2);
        st.setFromY(0.2);
        st.setToX(1.15);
        st.setToY(1.15);

        // Animation Sprite Flipbook
        Transition spriteAnim = new Transition() {
            {
                setCycleDuration(Duration.millis(800));
                setInterpolator(Interpolator.LINEAR);
                if (frameCount > 1) {
                    setCycleCount(Timeline.INDEFINITE); 
                }
            }
            @Override
            protected void interpolate(double k) {
                int index = Math.min((int) Math.floor(k * frameCount), frameCount - 1);
                int col = index % cols;
                int row = index / cols;
                iv.setViewport(new javafx.geometry.Rectangle2D(col * frameWidth, row * frameHeight, frameWidth, frameHeight));
            }
        };

        FadeTransition ft = new FadeTransition(Duration.millis(500), bannerBox);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.millis(900));

        ParallelTransition pt = new ParallelTransition(bannerBox, st);
        pt.setOnFinished(e -> {
            if (frameCount > 1) spriteAnim.play();
            ft.play();
        });
        ft.setOnFinished(e -> {
            spriteAnim.stop();
            animationLayer.getChildren().remove(bannerBox);
        });

        pt.play();
    }

    public void showUnoCloud(boolean isHost) {
        if (animationLayer == null) return;
        
        Image cloudImg = loadSpriteImage("cloud.png");
        Image unoIcon = loadSpriteImage("but_uno.png");
        if (cloudImg == null || unoIcon == null) return;
        
        ImageView cloudView = new ImageView(cloudImg);
        cloudView.setViewport(new javafx.geometry.Rectangle2D(isHost ? 261 : 0, 0, 261, 194));
        cloudView.setFitWidth(180);
        cloudView.setPreserveRatio(true);
        
        ImageView iconView = new ImageView(unoIcon);
        iconView.setFitWidth(65);
        iconView.setPreserveRatio(true);
        
        StackPane bannerBox = new StackPane(cloudView, iconView);
        bannerBox.setMouseTransparent(true);
        
        if (isHost) {
            StackPane.setMargin(iconView, new javafx.geometry.Insets(-15, 10, 0, 0));
        } else {
            StackPane.setMargin(iconView, new javafx.geometry.Insets(-15, 0, 0, 10));
        }
        
        Point2D center = new Point2D(mainBoardPane.getWidth() / 2.0, mainBoardPane.getHeight() / 2.0);
        
        // Vị trí bong bóng: Nếu Host (trên) -> y - 180, Guest (dưới) -> y + 40
        if (isHost) {
            bannerBox.setLayoutX(center.getX() - 100);
            bannerBox.setLayoutY(center.getY() - 180);
        } else {
            bannerBox.setLayoutX(center.getX() - 100);
            bannerBox.setLayoutY(center.getY() + 40);
        }
        
        animationLayer.getChildren().add(bannerBox);
        
        ScaleTransition st = new ScaleTransition(Duration.millis(300), bannerBox);
        st.setFromX(0.2);
        st.setFromY(0.2);
        st.setToX(1.15);
        st.setToY(1.15);
        
        FadeTransition ft = new FadeTransition(Duration.millis(400), bannerBox);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.millis(1200));
        
        ParallelTransition pt = new ParallelTransition(bannerBox, st);
        pt.setOnFinished(e -> ft.play());
        ft.setOnFinished(e -> animationLayer.getChildren().remove(bannerBox));
        
        pt.play();
    }

    private Image loadSpriteImage(String fileName) {
        try {
            String[] paths = {
                "/resource/sprites/" + fileName,
                "resource/sprites/" + fileName,
                "/sprites/" + fileName
            };
            for (String p : paths) {
                InputStream is = getClass().getResourceAsStream(p);
                if (is != null) {
                    return new Image(is);
                }
            }

            String[] filePaths = {
                "src/resource/sprites/" + fileName,
                "resource/sprites/" + fileName,
                "UnoGameProject/src/resource/sprites/" + fileName
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

    // ==========================================
    // CÁC HÀM TIỆN ÍCH KHÁC
    // ==========================================

    public void resetBoard() {
        Platform.runLater(() -> {
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }
            if (animationLayer != null) {
                animationLayer.getChildren().clear();
            }
            previousTopCard = null;
            previousHostCardCount = -1;
            previousGuestCardCount = -1;
            previousHostUno = false;
            previousGuestUno = false;
            previousTurnSeat = -1;
            isGameOverHandled = false;
            chatMessages.clear();
            if (hostHandBox != null) hostHandBox.getChildren().clear();
            if (guestHandBox != null) guestHandBox.getChildren().clear();
            if (lblHostCards != null) lblHostCards.setText("0 lá");
            if (lblGuestCards != null) lblGuestCards.setText("0 lá");
            if (lblHostStatus != null) lblHostStatus.setText("Đang chờ");
            if (lblGuestStatus != null) lblGuestStatus.setText("Đang chờ");
            if (lblCountdown != null) lblCountdown.setText("15s");
            if (lblTurnInfo != null) lblTurnInfo.setText("Lượt: Đang tải...");
            if (discardPileContainer != null) discardPileContainer.setStyle("");
            if (discardPileImageView != null) discardPileImageView.setImage(null);
            if (currentColorIndicator != null) {
                Image defImg = loadSpriteImage("colors.png");
                if (defImg != null) currentColorIndicator.setImage(defImg);
            }
            if (unoBadge != null) {
                unoBadge.setStyle("-fx-opacity: 0.35; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 8, 0, 0, 3);");
            }
        });
    }

    public void setPlayersInfo(String hostName, String guestName) {
        Platform.runLater(() -> {
            chatMessages.clear();
            if (lblHostName != null) lblHostName.setText(hostName);
            if (lblGuestName != null) lblGuestName.setText(guestName);
        });
    }

    public void updateCountdown(int timeLeft) {
        Platform.runLater(() -> {
            this.remainingSeconds = timeLeft;
            updateCountdownUI();
            
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }
            
            countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                remainingSeconds--;
                if (remainingSeconds <= 0) {
                    remainingSeconds = 0;
                    countdownTimeline.stop();
                }
                updateCountdownUI();
            }));
            countdownTimeline.setCycleCount(Timeline.INDEFINITE);
            countdownTimeline.play();
        });
    }

    private void updateCountdownUI() {
        if (lblCountdown == null) return;
        lblCountdown.setText(remainingSeconds + "s");
        if (remainingSeconds <= 5) {
            lblCountdown.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-family: monospace;");
        } else {
            lblCountdown.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-family: monospace;");
        }
    }

    public void addChatMessage(String sender, String message, boolean isSelf) {
        Platform.runLater(() -> {
            TextFlow flow = new TextFlow();
            Text senderText = new Text(sender + ": ");
            senderText.setStyle("-fx-font-weight: bold; -fx-fill: " + (isSelf ? "#4ff0a0" : "#8ea6d8") + "; -fx-font-size: 12px;");
            Text msgText = new Text(message);
            msgText.setStyle("-fx-fill: #eaf1ff; -fx-font-size: 12px;");
            flow.getChildren().addAll(senderText, msgText);
            chatMessages.add(flow);
            if (chatListView != null) {
                chatListView.scrollTo(chatMessages.size() - 1);
            }
        });
    }

    private void renderHand(HBox handBox, List<Card> hand) {
        if (handBox == null) return;
        handBox.getChildren().clear();
        if (hand == null) return;
        
        for (Card card : hand) {
            StackPane cardView = createCardView(card);
            handBox.getChildren().add(cardView);
        }
    }

    private StackPane createCardView(Card card) {
        StackPane container = new StackPane();
        container.getStyleClass().add("hand-card-view");
        container.setUserData(card);

        ImageView iv = new ImageView(FXCardHelper.getCardImage(card));
        iv.setFitWidth(72);
        iv.setFitHeight(110);
        iv.setPreserveRatio(true);

        container.getChildren().add(iv);
        return container;
    }

    private void renderCardFront(ImageView view, Card card) {
        if (view == null) return;
        Image img = FXCardHelper.getCardImage(card);
        if (img != null) {
            view.setImage(img);
        }
    }

    private void renderCardBack(ImageView view) {
        if (view == null) return;
        Image img = FXCardHelper.getCardBackImage();
        if (img != null) {
            view.setImage(img);
        }
    }
}

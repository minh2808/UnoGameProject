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
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.kordamp.ikonli.javafx.FontIcon;
import com.ptit.uno.client.view.fx.util.UnoNotification;

/**
 * GameBoardUIBridge: Cầu nối UI Code-Behind điều khiển giao diện bàn chơi UNO (JavaFX FXML).
 * Đóng vai trò là Presentation Helper thuần túy của tầng View,
 * kết hợp Animation Layer mượt mà như Uno Online và hệ thống âm thanh SFX độ trễ 0ms.
 */
public class GameBoardUIBridge implements Initializable {

    // 0. ROOT & ANIMATION OVERLAY LAYER
    @FXML
    private BorderPane mainBoardPane;
    @FXML
    private Pane animationLayer;

    // 1. LEFT: Game Log
    @FXML
    private ListView<TextFlow> gameLogListView;
    private final ObservableList<TextFlow> gameLogMessages = FXCollections.observableArrayList();
    private String lastActionLog = "";
    private int lastTurnSeat = -999;

    // 2. TOP: Đối thủ
    @FXML
    private Label lblOpponentName;
    @FXML
    private Label lblOpponentCards;
    @FXML
    private Label lblOpponentStatus;
    @FXML
    private HBox opponentHandBox;

    // 3. CENTER: Bàn chơi
    @FXML
    private StackPane drawPileContainer;
    @FXML
    private ImageView drawPileImageView;
    @FXML
    private StackPane discardPileContainer;
    @FXML
    private ImageView discardPileImageView;
    @FXML
    private HBox turnIndicatorBox;
    @FXML
    private Label lblTurnTimer;
    @FXML
    private Label lblTurnInfo;
    @FXML
    private Circle turnIndicatorDot;
    @FXML
    private Button btnPlayCard;
    @FXML
    private ImageView currentColorIndicator;
    @FXML
    private Button btnCallUno;

    // 4. RIGHT: Chat & Tiện ích
    @FXML
    private Button btnHelp;
    @FXML
    private Button btnLeave;
    @FXML
    private ListView<TextFlow> chatListView;
    @FXML
    private TextField txtChatInput;
    @FXML
    private Button btnSendChat;

    // 5. BOTTOM: Người chơi & Bài trên tay
    @FXML
    private Label lblPlayerName;
    @FXML
    private Label lblPlayerStatus;
    @FXML
    private Label lblPlayHint;
    @FXML
    private ScrollPane handScrollPane;
    @FXML
    private HBox handCardsBox;

    // Trạng thái nội tại
    private final List<Card> currentHand = new ArrayList<>();
    private Card selectedCard = null;
    private StackPane selectedCardView = null;
    private final ObservableList<TextFlow> chatMessages = FXCollections.observableArrayList();

    private Timeline countdownTimeline;
    private int remainingSeconds = 15;
    private Card currentTopCard;
    private CardColor currentActiveColor;
    private boolean isMyTurn = false;

    // Biến giám sát chuyển động (Animation tracking)
    private Card previousTopCard = null;
    private int previousOpponentCardCount = -1;
    private boolean previousOpponentUnoState = false;
    private boolean isLocallyPlayingCard = false;
    private final Random random = new Random();

    // Callback kết nối với ClientControl
    private GameActionListener actionListener;

    public interface GameActionListener {
        void onPlayCard(Card card, CardColor chosenColor);

        void onDrawCard();

        void onCallUno();

        void onSendMessage(String message);

        void onLeaveGame();

        void onGameFinished();

        default void onRematchRequest() {}

        default void onRematchResponse(boolean accept) {}
    }

    public void setActionListener(GameActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatListView.setItems(chatMessages);
        if (gameLogListView != null) {
            gameLogListView.setItems(gameLogMessages);
        }

        // Nạp ảnh nọc rút mặc định
        Image backImg = FXCardHelper.getCardBackImage();
        if (backImg != null) {
            drawPileImageView.setImage(backImg);
        }

        // Khởi tạo trạng thái ban đầu sạch sẽ
        lblOpponentName.setText("Đối thủ");
        lblOpponentCards.setText("0 lá");
        lblOpponentStatus.setText("Đang chờ");
        if (lblTurnInfo != null) {
            lblTurnInfo.setText("Lượt: Đang kết nối...");
        }
        if (lblPlayerStatus != null) {
            lblPlayerStatus.setText("Đang chờ");
        }
        if (lblPlayHint != null) {
            lblPlayHint.setText("Hoặc nhấp đúp thẻ");
        }
        if (btnPlayCard != null) {
            btnPlayCard.setDisable(true);
        }

        updateUnoButtonVisual();

        addGameLog("HỆ THỐNG: ", "Trận đấu đang chuẩn bị...", Color.web("#f1c40f"), Color.web("#ecf0f1"));
    }

    public void setPlayerInfo(String username) {
        Platform.runLater(() -> {
            if (lblPlayerName != null) {
                lblPlayerName.setText(username);
            }
        });
    }

    private javafx.stage.Window getBoardWindow() {
        if (mainBoardPane != null && mainBoardPane.getScene() != null) {
            return mainBoardPane.getScene().getWindow();
        }
        if (btnLeave != null && btnLeave.getScene() != null) {
            return btnLeave.getScene().getWindow();
        }
        return null;
    }

    // ==========================================
    // CẬP NHẬT TRẠNG THÁI TỪ SERVER (MVC VIEW)
    // ==========================================

    /**
     * Cập nhật toàn bộ trạng thái bàn cờ từ GameState của Server
     */
    public void updateGameState(GameState state, int currentUserId) {
        if (state == null)
            return;
        Platform.runLater(() -> {
            // 1. Kiểm tra biến động lá bài trên bàn để kích hoạt Animation bài bay
            Card top = state.getTopCard();
            if (top != null) {
                boolean isNewCard = (previousTopCard == null || !top.equals(previousTopCard));
                currentTopCard = top;
                currentActiveColor = state.getActiveColor();

                boolean isInitialDeal = (previousTopCard == null && previousOpponentCardCount == -1);

                if (isNewCard) {
                    if (isInitialDeal) {
                        // Trì hoãn việc lật lá bài đầu tiên cho đến khi chia bài xong (khoảng 2.6s)
                        PauseTransition pt = new PauseTransition(Duration.millis(2600));
                        pt.setOnFinished(e -> {
                            FXSoundHelper.playCardPlay();
                            setTopCard(top, state.getActiveColor());
                            triggerSpecialEffectIfAny(top);
                        });
                        pt.play();
                    } else if (isLocallyPlayingCard) {
                        // Lá bài do chính người chơi vừa đánh (Animation đã chạy từ trước)
                        isLocallyPlayingCard = false;
                        setTopCard(top, state.getActiveColor());
                        triggerSpecialEffectIfAny(top);
                    } else {
                        // Lá bài do ĐỐI THỦ đánh -> Chạy animation từ đối thủ xuống nọc bài!
                        animateOpponentPlayCard(top, state.getActiveColor());
                        triggerSpecialEffectIfAny(top);
                    }
                    previousTopCard = top;
                } else {
                    if (!isInitialDeal) {
                        setTopCard(top, state.getActiveColor());
                    }
                }
            }

            // 2. Cập nhật Game Log panel
            String action = state.getLastActionLog();
            if (action != null && !action.isEmpty()) {
                if (!action.equals(lastActionLog)) {
                    lastActionLog = action;
                    addGameLog("MỚI NHẤT: ", action, Color.web("#f1c40f"), Color.web("#ffffff"));
                }
            }

            // 3. Xác định chính xác lượt đi hiện tại thuộc về ai
            boolean myTurnStatus = false;
            String turnPlayerName = "Đối thủ";
            if (state.getPlayerSummaries() != null) {
                for (GameState.PlayerSummary p : state.getPlayerSummaries()) {
                    if (p.getSeatNumber() == state.getCurrentTurnSeat()) {
                        turnPlayerName = p.getUsername() + (p.isBot() ? " (Bot)" : "");
                        if (p.getUserId() == currentUserId) {
                            myTurnStatus = true;
                        }
                    }
                }
            }
            this.isMyTurn = myTurnStatus;
            updateTurnPillDisplay(this.isMyTurn, turnPlayerName);

            // Ghi log lượt đi và cập nhật bộ đếm khi lượt chuyển sang người mới
            if (state.getCurrentTurnSeat() != lastTurnSeat) {
                lastTurnSeat = state.getCurrentTurnSeat();
                addGameLog("HỆ THỐNG: ", isMyTurn ? "Đến lượt BẠN đánh!" : "Đến lượt " + turnPlayerName,
                        Color.web("#f1c40f"), Color.web("#a0c4ab"));
                startCountdown(state.getRemainingSeconds() > 0 ? state.getRemainingSeconds() : 15);
            }

            // 3. Đếm ngược
            startCountdown(state.getRemainingSeconds());

            // 4. Cập nhật danh sách người chơi & đối thủ
            List<GameState.PlayerSummary> summaries = state.getPlayerSummaries();
            if (summaries != null) {
                for (GameState.PlayerSummary p : summaries) {
                    boolean isHisTurn = (p.getSeatNumber() == state.getCurrentTurnSeat());

                    if (p.getUserId() != currentUserId) {
                        String status = isHisTurn ? "Đang đánh..." : "Đang chờ";
                        if (p.isUno())
                            status = "[UNO!] " + status;
                        
                        // Xử lý animation rút bài / chia bài ban đầu cho đối thủ
                        if (previousOpponentCardCount == -1 && p.getCardCount() >= 7) {
                            animateInitialDealOpponent(p.getCardCount());
                        } else if (previousOpponentCardCount != -1 && p.getCardCount() > previousOpponentCardCount) {
                            animateDrawCardToOpponent();
                        }
                        previousOpponentCardCount = p.getCardCount();
                        
                        // Kích hoạt hiệu ứng bong bóng UNO nếu đối thủ vừa hô UNO
                        if (!previousOpponentUnoState && p.isUno()) {
                            FXSoundHelper.playUnoSnap();
                            showUnoCloud(false);
                        }
                        previousOpponentUnoState = p.isUno();

                        setOpponentInfo(p.getUsername() + (p.isBot() ? " (Bot)" : ""), p.getCardCount(), status);
                    }
                }
            }

            // 5. Kết thúc ván đấu
            if (state.isGameOver()) {
                if (countdownTimeline != null)
                    countdownTimeline.stop();
                FXSoundHelper.playGameOver();
                showGameOverModal(state, currentUserId);
            }
        });
    }

    private void updateTurnPillDisplay(boolean myTurn, String currentTurnPlayerName) {
        if (lblTurnInfo != null) {
            if (myTurn) {
                lblTurnInfo.setText("ĐẾN LƯỢT BẠN ĐÁNH!");
                lblTurnInfo.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(46, 204, 113, 0.7), 8, 0, 0, 0);");
            } else {
                String name = (currentTurnPlayerName != null && !currentTurnPlayerName.isEmpty()) ? currentTurnPlayerName : "Đối thủ";
                lblTurnInfo.setText("Lượt của: " + name);
                lblTurnInfo.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 14px; -fx-font-weight: bold;");
            }
        }
        if (turnIndicatorDot != null) {
            turnIndicatorDot.setFill(myTurn ? Color.web("#2ecc71") : Color.web("#e67e22"));
        }
        if (turnIndicatorBox != null) {
            turnIndicatorBox.getStyleClass().removeAll("turn-timer-pill-my-turn", "turn-timer-pill-opponent-turn");
            if (myTurn) {
                turnIndicatorBox.getStyleClass().add("turn-timer-pill-my-turn");
            } else {
                turnIndicatorBox.getStyleClass().add("turn-timer-pill-opponent-turn");
            }
        }
        if (lblPlayerStatus != null) {
            if (myTurn) {
                lblPlayerStatus.setText("LƯỢT CỦA BẠN");
                lblPlayerStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            } else {
                lblPlayerStatus.setText("Đang chờ đối thủ...");
                lblPlayerStatus.setStyle("-fx-text-fill: #95a5a6;");
            }
        }
        if (lblPlayHint != null) {
            if (myTurn) {
                lblPlayHint.setText("Chọn thẻ bài để đánh");
                lblPlayHint.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11px;");
            } else {
                lblPlayHint.setText("Đang chờ đối thủ ra bài...");
                lblPlayHint.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
            }
        }
    }

    private void triggerSpecialEffectIfAny(Card card) {
        if (card == null) return;
        CardValue val = card.getValue();
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

    public void setOpponentInfo(String name, int cardCount, String status) {
        Platform.runLater(() -> {
            lblOpponentName.setText(name);
            lblOpponentCards.setText(cardCount + " lá");
            lblOpponentStatus.setText(status);

            opponentHandBox.getChildren().clear();
            opponentHandBox.setAlignment(Pos.CENTER);

            double spacing = -28.0;
            if (cardCount > 9) {
                spacing = Math.max(-38.0, -28.0 - (cardCount - 9) * 0.7);
            }
            opponentHandBox.setSpacing(spacing);

            Image backImg = FXCardHelper.getCardBackImage();
            for (int i = 0; i < Math.min(cardCount, 30); i++) {
                ImageView iv = new ImageView(backImg);
                iv.setFitWidth(52);
                iv.setFitHeight(78);
                iv.setPreserveRatio(true);
                opponentHandBox.getChildren().add(iv);
            }
        });
    }

    public void startCountdown(int seconds) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        remainingSeconds = seconds;
        if (lblTurnTimer != null) {
            lblTurnTimer.setText(remainingSeconds + "s");
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            if (lblTurnTimer != null) {
                lblTurnTimer.setText(Math.max(0, remainingSeconds) + "s");
            }
            if (remainingSeconds <= 0) {
                countdownTimeline.stop();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    public void setTopCard(Card card, CardColor activeColor) {
        Platform.runLater(() -> {
            Image img = FXCardHelper.getCardImage(card);
            if (img != null) {
                discardPileImageView.setImage(img);
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
                    try {
                        java.io.InputStream is = getClass().getResourceAsStream("/resource/sprites/" + colorImg);
                        if (is != null) {
                            currentColorIndicator.setImage(new Image(is));
                        }
                    } catch (Exception e) {
                        System.err.println("Could not load color indicator image: " + colorImg);
                    }
                }
            } else if (currentColorIndicator != null) {
                try {
                    java.io.InputStream is = getClass().getResourceAsStream("/resource/sprites/colors.png");
                    if (is != null) {
                        currentColorIndicator.setImage(new Image(is));
                    }
                } catch (Exception e) {
                    System.err.println("Could not load colors.png");
                }
            }
        });
    }

    public void setHandCards(List<Card> cards) {
        Platform.runLater(() -> {
            boolean isInitialDeal = currentHand.isEmpty() && cards != null && cards.size() >= 7;

            currentHand.clear();
            if (cards != null) {
                currentHand.addAll(cards);
            }
            handCardsBox.getChildren().clear();
            clearSelection();

            if (isInitialDeal) {
                List<StackPane> cardViews = new ArrayList<>();
                for (Card card : currentHand) {
                    StackPane cardView = createCardView(card);
                    cardView.setVisible(false);
                    handCardsBox.getChildren().add(cardView);
                    cardViews.add(cardView);
                }

                Platform.runLater(() -> {
                    Point2D start = getCenterOnAnimationLayer(drawPileContainer);
                    for (int i = 0; i < currentHand.size(); i++) {
                        Card card = currentHand.get(i);
                        StackPane targetView = cardViews.get(i);
                        
                        PauseTransition delay = new PauseTransition(Duration.millis(i * 300));
                        delay.setOnFinished(e -> {
                            Point2D target = getCenterOnAnimationLayer(targetView);
                            FXSoundHelper.playCardDraw();
                            animateFloatingCard(start, target, FXCardHelper.getCardImage(card), 0, () -> {
                                targetView.setVisible(true);
                            });
                        });
                        delay.play();
                    }
                });
            } else {
                for (Card card : currentHand) {
                    StackPane cardView = createCardView(card);
                    handCardsBox.getChildren().add(cardView);
                }
            }

            updateUnoButtonVisual();
        });
    }

    private StackPane createCardView(Card card) {
        StackPane container = new StackPane();
        container.getStyleClass().add("hand-card-view");

        ImageView iv = new ImageView(FXCardHelper.getCardImage(card));
        iv.setFitWidth(72);
        iv.setFitHeight(110);
        iv.setPreserveRatio(true);

        container.getChildren().add(iv);

        container.setOnMouseClicked((MouseEvent e) -> {
            FXSoundHelper.playButtonClick();
            selectCard(card, container);
            if (e.getClickCount() == 2) {
                handlePlayCard(null);
            }
        });

        return container;
    }

    private void selectCard(Card card, StackPane cardView) {
        clearSelection();
        selectedCard = card;
        selectedCardView = cardView;

        selectedCardView.getStyleClass().add("hand-card-selected");
        if (btnPlayCard != null) {
            btnPlayCard.setDisable(!isMyTurn);
        }
    }

    private void clearSelection() {
        if (selectedCardView != null) {
            selectedCardView.getStyleClass().remove("hand-card-selected");
        }
        selectedCard = null;
        selectedCardView = null;
        if (btnPlayCard != null) {
            btnPlayCard.setDisable(true);
        }
    }

    // ==========================================
    // ANIMATION ENGINE (CHUYỂN ĐỘNG BÀI BAY & SFX)
    // ==========================================

    /**
     * Chuyển đổi tọa độ từ bất kỳ Node nào sang tọa độ chuẩn trên animationLayer
     */
    private Point2D getCenterOnAnimationLayer(Node node) {
        if (node == null || animationLayer == null || node.getScene() == null) {
            return new Point2D(550, 325);
        }
        javafx.geometry.Bounds bounds = node.localToScene(node.getBoundsInLocal());
        return animationLayer.sceneToLocal(bounds.getCenterX(), bounds.getCenterY());
    }

    /**
     * Animation bay bài tổng quát với Easing Cubic và Scale Punch
     */
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

    /**
     * Animation người chơi đánh lá bài lên bàn
     */
    private void animatePlayerPlayCard(Card card, CardColor chosenColor) {
        Point2D start = (selectedCardView != null) ? getCenterOnAnimationLayer(selectedCardView) : new Point2D(550, 580);
        Point2D target = getCenterOnAnimationLayer(discardPileContainer);

        // Đánh dấu để updateGameState không chạy lặp animation
        isLocallyPlayingCard = true;
        FXSoundHelper.playCardPlay();

        // Ẩn lá bài đang chọn trên tay
        if (selectedCardView != null) {
            selectedCardView.setVisible(false);
        }

        double randomAngle = -6.0 + random.nextDouble() * 12.0;
        animateFloatingCard(start, target, FXCardHelper.getCardImage(card), randomAngle, () -> {
            setTopCard(card, chosenColor);
            clearSelection();
            if (actionListener != null) {
                actionListener.onPlayCard(card, chosenColor);
            }
        });
    }

    /**
     * Animation đối thủ đánh lá bài xuống bàn
     */
    private void animateOpponentPlayCard(Card card, CardColor activeColor) {
        Point2D start = getCenterOnAnimationLayer(opponentHandBox);
        Point2D target = getCenterOnAnimationLayer(discardPileContainer);

        FXSoundHelper.playCardPlay();
        double randomAngle = -6.0 + random.nextDouble() * 12.0;

        animateFloatingCard(start, target, FXCardHelper.getCardImage(card), randomAngle, () -> {
            setTopCard(card, activeColor);
        });
    }

    /**
     * Animation rút bài từ nọc về tay người chơi
     */
    private void animateDrawCardToPlayer() {
        Point2D start = getCenterOnAnimationLayer(drawPileContainer);
        Point2D target = getCenterOnAnimationLayer(handScrollPane);

        FXSoundHelper.playCardDraw();
        animateFloatingCard(start, target, FXCardHelper.getCardBackImage(), 0, null);
    }

    /**
     * Animation đối thủ rút bài từ nọc
     */
    private void animateDrawCardToOpponent() {
        Point2D start = getCenterOnAnimationLayer(drawPileContainer);
        Point2D target = getCenterOnAnimationLayer(opponentHandBox);

        FXSoundHelper.playCardDraw();
        animateFloatingCard(start, target, FXCardHelper.getCardBackImage(), 0, null);
    }

    /**
     * Animation chia bài ban đầu cho đối thủ (chạy từng lá)
     */
    private void animateInitialDealOpponent(int cardCount) {
        Platform.runLater(() -> { 
            Platform.runLater(() -> { 
                ObservableList<Node> opponentCards = opponentHandBox.getChildren();
                for (Node n : opponentCards) {
                    n.setVisible(false);
                }
                
                Point2D start = getCenterOnAnimationLayer(drawPileContainer);
                Image backImg = FXCardHelper.getCardBackImage();

                for (int i = 0; i < Math.min(cardCount, opponentCards.size()); i++) {
                    Node targetView = opponentCards.get(i);
                    PauseTransition delay = new PauseTransition(Duration.millis(i * 300 + 150)); 
                    delay.setOnFinished(e -> {
                        Point2D target = getCenterOnAnimationLayer(targetView);
                        FXSoundHelper.playCardDraw();
                        animateFloatingCard(start, target, backImg, 0, () -> {
                            targetView.setVisible(true);
                        });
                    });
                    delay.play();
                }
            });
        });
    }

    /**
     * Hiển thị biểu ngữ hiệu ứng đặc biệt (Cấm lượt, Đổi chiều, +2, +4, UNO)
     */
    public void showSpecialEffectBanner(String spriteFileName, String text) {
        if (animationLayer == null) return;

        Image img = loadSpriteImage(spriteFileName);
        if (img == null) return;

        VBox bannerBox = new VBox(6);
        bannerBox.setAlignment(Pos.CENTER);
        bannerBox.setMouseTransparent(true);

        ImageView iv = new ImageView(img);
        
        // Mỗi frame chuẩn của các hiệu ứng đều có chiều rộng 292 pixel
        int frameWidth = 292;
        int cols = Math.max(1, (int) (img.getWidth() / frameWidth));
        int rows = (cols > 1) ? 2 : 1; // draw_2 và draw_4 đều có 14 frame (7 cột x 2 hàng)
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
                setCycleDuration(Duration.millis(800)); // Chạy trọn vẹn 14 frame trong 800ms
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
        ft.setDelay(Duration.millis(900)); // Hiện banner trong 900ms rồi mới mờ đi

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

    /**
     * Hiển thị đám mây hô UNO (cắt từ cloud.png)
     */
    public void showUnoCloud(boolean isPlayer) {
        if (animationLayer == null) return;
        
        Image cloudImg = loadSpriteImage("cloud.png");
        Image unoIcon = loadSpriteImage("but_uno.png");
        if (cloudImg == null || unoIcon == null) return;
        
        // cloud.png có 2 phần: trái (bên mình) và phải (địch). Kích thước tổng 522x194.
        ImageView cloudView = new ImageView(cloudImg);
        cloudView.setViewport(new javafx.geometry.Rectangle2D(isPlayer ? 0 : 261, 0, 261, 194));
        cloudView.setFitWidth(180);
        cloudView.setPreserveRatio(true);
        
        ImageView iconView = new ImageView(unoIcon);
        iconView.setFitWidth(65);
        iconView.setPreserveRatio(true);
        
        StackPane bannerBox = new StackPane(cloudView, iconView);
        bannerBox.setMouseTransparent(true);
        
        // Tinh chỉnh icon khớp với tâm bong bóng thoại (lệch một chút so với cái đuôi)
        if (isPlayer) {
            StackPane.setMargin(iconView, new javafx.geometry.Insets(-15, 0, 0, 10));
        } else {
            StackPane.setMargin(iconView, new javafx.geometry.Insets(-15, 10, 0, 0));
        }
        
        Point2D center = new Point2D(mainBoardPane.getWidth() / 2.0, mainBoardPane.getHeight() / 2.0);
        
        // Vị trí bong bóng: Nếu là mình, hiện gần bài dưới. Nếu địch, hiện gần bài trên.
        if (isPlayer) {
            bannerBox.setLayoutX(center.getX() - 180);
            bannerBox.setLayoutY(center.getY() + 40);
        } else {
            bannerBox.setLayoutX(center.getX() + 0);
            bannerBox.setLayoutY(center.getY() - 180);
        }
        
        animationLayer.getChildren().add(bannerBox);
        
        // Hiệu ứng Zoom nảy & Mờ dần
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

    // ==========================================
    // SỰ KIỆN TƯƠNG TÁC (ACTIONS GỬI VỀ SERVER)
    // ==========================================

    @FXML
    private void handleDiscardPileClick(MouseEvent event) {
        if (selectedCard != null) {
            handlePlayCard(null);
        }
    }

    private void showSwingWarning(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cảnh báo");
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (mainBoardPane != null && mainBoardPane.getScene() != null && mainBoardPane.getScene().getWindow() != null) {
                alert.initOwner(mainBoardPane.getScene().getWindow());
            }
            alert.showAndWait();
        });
    }

    private void showSwingInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (mainBoardPane != null && mainBoardPane.getScene() != null && mainBoardPane.getScene().getWindow() != null) {
                alert.initOwner(mainBoardPane.getScene().getWindow());
            }
            alert.showAndWait();
        });
    }

    @FXML
    private void handlePlayCard(ActionEvent event) {
        FXSoundHelper.playButtonClick();
        if (selectedCard == null)
            return;

        if (!isMyTurn) {
            showSwingWarning("Chưa đến lượt của bạn!");
            return;
        }

        if (currentTopCard != null && !selectedCard.canPlayOn(currentTopCard, currentActiveColor)) {
            showSwingWarning("Lá bài này không thể đánh!\nPhải cùng màu, cùng số/chức năng, hoặc bài Đen (Wild).");
            return;
        }

        if (selectedCard.isWild()) {
            showChooseColorDialog(selectedCard);
        } else {
            Card cardToPlay = selectedCard;
            animatePlayerPlayCard(cardToPlay, cardToPlay.getColor());
        }
    }

    @FXML
    private void handleDrawCard(MouseEvent event) {
        FXSoundHelper.playButtonClick();
        if (!isMyTurn) {
            showSwingWarning("Chưa đến lượt của bạn!");
            return;
        }

        // Chạy animation rút bài về tay mình
        animateDrawCardToPlayer();

        if (actionListener != null) {
            actionListener.onDrawCard();
        }
    }

    @FXML
    private void handleCallUno(ActionEvent event) {
        if (currentHand == null || currentHand.size() != 1) {
            showSwingWarning("Bạn chỉ được hô UNO khi trên tay còn đúng 1 lá bài!");
            return;
        }
        FXSoundHelper.playUnoSnap();
        showUnoCloud(true);
        if (actionListener != null) {
            actionListener.onCallUno();
        }
    }

    private void updateUnoButtonVisual() {
        if (btnCallUno == null) return;
        if (currentHand != null && currentHand.size() == 1) {
            btnCallUno.setStyle("-fx-effect: dropshadow(gaussian, #f39c12, 18, 0.7, 0, 0);");
        } else {
            btnCallUno.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 8, 0, 0, 3);");
        }
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        String msg = txtChatInput.getText().trim();
        if (!msg.isEmpty()) {
            txtChatInput.clear();
            FXSoundHelper.playButtonClick();
            if (actionListener != null) {
                actionListener.onSendMessage(msg);
            }
        }
    }

    @FXML
    private void handleHelp(ActionEvent event) {
        FXSoundHelper.playButtonClick();
        showSwingInfo("Trợ giúp UNO", "1. Đánh lá bài cùng màu hoặc cùng số/ký hiệu với lá bài trên bàn.\n" +
                "2. Lá Đổi màu (Wild) có thể đánh bất cứ lúc nào.\n" +
                "3. Khi còn 2 lá chuẩn bị đánh xuống 1 lá, nhớ bấm 'HÔ UNO!'.\n" +
                "4. Nếu không có bài đánh, bấm vào chồng bài úp để bốc bài.");
    }

    @FXML
    public void handleLeave(ActionEvent event) {
        FXSoundHelper.playButtonClick();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc muốn thoát ván đấu không?");

        if (mainBoardPane != null && mainBoardPane.getScene() != null && mainBoardPane.getScene().getWindow() != null) {
            alert.initOwner(mainBoardPane.getScene().getWindow());
        }

        Optional<ButtonType> opt = alert.showAndWait();
        if (opt.isPresent() && opt.get() == ButtonType.OK) {
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }
            if (actionListener != null) {
                actionListener.onLeaveGame();
            }
        }
    }

    public void addChatMessage(String sender, String message, Color senderColor) {
        Platform.runLater(() -> {
            Text senderText = new Text(sender + ": ");
            senderText.setFill(senderColor);
            senderText.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

            Text messageText = new Text(message);
            messageText.setFill(Color.web("#e0e0e0"));
            messageText.setStyle("-fx-font-size: 12px;");

            TextFlow flow = new TextFlow(senderText, messageText);
            chatMessages.add(flow);
            chatListView.scrollTo(chatMessages.size() - 1);
        });
    }

    public void addGameLog(String prefix, String message, Color prefixColor, Color messageColor) {
        Platform.runLater(() -> {
            if (gameLogListView == null) {
                return;
            }
            Text prefixText = new Text(prefix);
            prefixText.setFill(prefixColor);
            prefixText.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

            Text contentText = new Text(message + "\n");
            contentText.setFill(messageColor);
            contentText.setStyle("-fx-font-size: 12px;");

            TextFlow flow = new TextFlow(prefixText, contentText);
            gameLogMessages.add(flow);
            gameLogListView.scrollTo(gameLogMessages.size() - 1);
        });
    }

    private StackPane gameOverOverlay = null;

    private void showChooseColorDialog(Card card) {
        if (animationLayer == null) return;

        // Bắt đầu phát âm thanh đổi màu khi bảng chọn màu xuất hiện
        FXSoundHelper.playChangeColor();

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        overlay.setPrefSize(mainBoardPane.getWidth(), mainBoardPane.getHeight());

        StackPane panelWrapper = new StackPane();
        Image panelImg = loadSpriteImage("select_color_panel.png");
        if (panelImg != null) {
            ImageView panelView = new ImageView(panelImg);
            panelView.setFitWidth(400); 
            panelView.setPreserveRatio(true);
            panelWrapper.getChildren().add(panelView);
        }

        HBox colorButtons = new HBox(15);
        colorButtons.setAlignment(Pos.CENTER);
        // Dịch khung nút chọn màu xuống 1 chút để không đè lên chữ trên bảng
        colorButtons.setTranslateY(30); 

        Image colorsImg = loadSpriteImage("colors.png");
        CardColor[] cols = {CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE};
        // Dựa trên kích thước colors.png (412x102) => mỗi màu rộng khoảng 103px
        int bw = 103;
        int bh = 102;

        for (int i = 0; i < 4; i++) {
            ImageView btn = new ImageView(colorsImg);
            btn.setViewport(new javafx.geometry.Rectangle2D(i * bw, 0, bw, bh));
            btn.setFitWidth(75);
            btn.setFitHeight(75);
            btn.setCursor(javafx.scene.Cursor.HAND);
            final CardColor chosenColor = cols[i];
            
            // Hiệu ứng Hover
            btn.setOnMouseEntered(e -> {
                btn.setScaleX(1.15);
                btn.setScaleY(1.15);
            });
            btn.setOnMouseExited(e -> {
                btn.setScaleX(1.0);
                btn.setScaleY(1.0);
            });
            
            btn.setOnMouseClicked(e -> {
                // Tắt ngay âm thanh đổi màu khi người dùng chọn xong màu
                FXSoundHelper.stopChangeColor();
                FXSoundHelper.playButtonClick();
                animationLayer.getChildren().remove(overlay);
                animatePlayerPlayCard(card, chosenColor);
            });
            colorButtons.getChildren().add(btn);
        }

        panelWrapper.getChildren().add(colorButtons);
        overlay.getChildren().add(panelWrapper);
        animationLayer.getChildren().add(overlay);

        // Hiệu ứng bật Popup (Zoom nảy)
        ScaleTransition st = new ScaleTransition(Duration.millis(300), panelWrapper);
        st.setFromX(0.5); 
        st.setFromY(0.5);
        st.setToX(1.0); 
        st.setToY(1.0);
        st.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0)); // Fixed spline control point
        st.play();
    }

    /**
     * Hiển thị bảng kết quả khi kết thúc trận đấu:
     * - player1 thắng thì cộng 20, player2 thua thì -20
     * - 2 nút: 1 là Đấu lại, 2 là Sảnh chính
     */
    private void showGameOverModal(GameState state, int currentUserId) {
        if (animationLayer == null) return;
        if (gameOverOverlay != null) {
            animationLayer.getChildren().remove(gameOverOverlay);
            gameOverOverlay = null;
        }

        gameOverOverlay = new StackPane();
        gameOverOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.78);");
        gameOverOverlay.setPrefSize(mainBoardPane.getWidth(), mainBoardPane.getHeight());

        VBox cardBox = new VBox(15);
        cardBox.setAlignment(Pos.CENTER);
        cardBox.setMaxWidth(440);
        cardBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #112217, #09130d);" +
            "-fx-background-radius: 18px;" +
            "-fx-border-color: #f1c40f;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 18px;" +
            "-fx-padding: 25px 30px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 25, 0, 0, 6);"
        );

        FontIcon trophyIcon = new FontIcon("fas-trophy");
        trophyIcon.setIconSize(22);
        trophyIcon.setIconColor(Color.web("#f1c40f"));

        Label lblTitle = new Label("KẾT QUẢ TRẬN ĐẤU");
        lblTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f1c40f; -fx-effect: dropshadow(gaussian, rgba(241, 196, 15, 0.6), 10, 0, 0, 0);");

        HBox titleBox = new HBox(8, trophyIcon, lblTitle);
        titleBox.setAlignment(Pos.CENTER);

        String winner = state.getWinnerUsername() != null ? state.getWinnerUsername() : "Không rõ";
        String loser = "Đối thủ";
        boolean iAmWinner = false;

        if (state.getPlayerSummaries() != null) {
            for (GameState.PlayerSummary p : state.getPlayerSummaries()) {
                if (!p.getUsername().equalsIgnoreCase(winner)) {
                    loser = p.getUsername() + (p.isBot() ? " (Bot)" : "");
                }
                if (p.getUserId() == currentUserId && p.getUsername().equalsIgnoreCase(winner)) {
                    iAmWinner = true;
                }
            }
        }

        FontIcon myStatusIcon = new FontIcon(iAmWinner ? "fas-check-circle" : "fas-times-circle");
        myStatusIcon.setIconSize(16);
        myStatusIcon.setIconColor(Color.web(iAmWinner ? "#2ecc71" : "#e74c3c"));

        Label lblMyStatus = new Label(iAmWinner ? "BẠN ĐÃ CHIẾN THẮNG!" : "BẠN ĐÃ THUA CUỘC!");
        lblMyStatus.setStyle(iAmWinner 
            ? "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;" 
            : "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        HBox myStatusBox = new HBox(8, myStatusIcon, lblMyStatus);
        myStatusBox.setAlignment(Pos.CENTER);

        // Bảng kết quả điểm: Người thắng +20 điểm, người thua -20 điểm dùng Ikonli FontIcon
        VBox scoreListBox = new VBox(10);
        scoreListBox.setAlignment(Pos.CENTER);
        scoreListBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10px; -fx-padding: 12px 20px;");

        FontIcon winnerIcon = new FontIcon("fas-trophy");
        winnerIcon.setIconSize(16);
        winnerIcon.setIconColor(Color.web("#f1c40f"));

        Label lblWinnerScore = new Label(winner + " (Thắng) : +20 điểm");
        lblWinnerScore.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");
        HBox winnerRow = new HBox(8, winnerIcon, lblWinnerScore);
        winnerRow.setAlignment(Pos.CENTER);

        FontIcon loserIcon = new FontIcon("fas-times-circle");
        loserIcon.setIconSize(16);
        loserIcon.setIconColor(Color.web("#e74c3c"));

        Label lblLoserScore = new Label(loser + " (Thua) : -20 điểm");
        lblLoserScore.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        HBox loserRow = new HBox(8, loserIcon, lblLoserScore);
        loserRow.setAlignment(Pos.CENTER);

        scoreListBox.getChildren().addAll(winnerRow, loserRow);

        Label lblRematchStatus = new Label("");
        lblRematchStatus.setStyle("-fx-font-size: 13px; -fx-text-fill: #f39c12; -fx-font-style: italic;");

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button btnRematch = new Button("ĐẤU LẠI");
        FontIcon rematchIcon = new FontIcon("fas-sync-alt");
        rematchIcon.setIconSize(14);
        rematchIcon.setIconColor(Color.WHITE);
        btnRematch.setGraphic(rematchIcon);
        btnRematch.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #2ecc71, #27ae60);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 9px 22px;" +
            "-fx-cursor: hand;"
        );

        Button btnLobby = new Button("SẢNH CHÍNH");
        FontIcon homeIcon = new FontIcon("fas-home");
        homeIcon.setIconSize(14);
        homeIcon.setIconColor(Color.WHITE);
        btnLobby.setGraphic(homeIcon);
        btnLobby.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #e74c3c, #c0392b);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 9px 22px;" +
            "-fx-cursor: hand;"
        );

        btnRematch.setOnAction(e -> {
            FXSoundHelper.playButtonClick();
            btnRematch.setDisable(true);
            btnRematch.setText("Đang đợi...");
            FontIcon waitIcon = new FontIcon("fas-clock");
            waitIcon.setIconSize(14);
            waitIcon.setIconColor(Color.WHITE);
            btnRematch.setGraphic(waitIcon);
            lblRematchStatus.setText("Đang đợi đối thủ xác nhận sẵn sàng...");
            if (actionListener != null) {
                actionListener.onRematchRequest();
            }
        });

        btnLobby.setOnAction(e -> {
            FXSoundHelper.playButtonClick();
            if (gameOverOverlay != null) {
                animationLayer.getChildren().remove(gameOverOverlay);
                gameOverOverlay = null;
            }
            if (actionListener != null) {
                actionListener.onLeaveGame();
            }
        });

        buttonBox.getChildren().addAll(btnRematch, btnLobby);
        cardBox.getChildren().addAll(titleBox, myStatusBox, scoreListBox, lblRematchStatus, buttonBox);

        gameOverOverlay.getChildren().add(cardBox);
        animationLayer.getChildren().add(gameOverOverlay);

        ScaleTransition st = new ScaleTransition(Duration.millis(350), cardBox);
        st.setFromX(0.6);
        st.setFromY(0.6);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));
        st.play();
    }

    /**
     * Nhận lời mời đấu lại từ đối thủ: hiển thị thông báo Swing xác nhận
     */
    public void promptRematch(String opponentUsername) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Lời mời Đấu lại");
            alert.setHeaderText(null);
            alert.setContentText("Đối thủ [" + opponentUsername + "] muốn đấu lại với bạn! Bạn có đồng ý không?");

            if (mainBoardPane != null && mainBoardPane.getScene() != null && mainBoardPane.getScene().getWindow() != null) {
                alert.initOwner(mainBoardPane.getScene().getWindow());
            }

            Optional<ButtonType> ans = alert.showAndWait();
            if (ans.isPresent() && ans.get() == ButtonType.OK) {
                if (actionListener != null) {
                    actionListener.onRematchResponse(true);
                }
            } else {
                if (gameOverOverlay != null && animationLayer != null) {
                    animationLayer.getChildren().remove(gameOverOverlay);
                    gameOverOverlay = null;
                }
                if (actionListener != null) {
                    actionListener.onRematchResponse(false);
                    actionListener.onLeaveGame();
                }
            }
        });
    }

    /**
     * Đếm ngược thời gian dùng animation khi cả 2 người chơi đã sẵn sàng đấu lại
     */
    public void startRematchCountdownAnimation() {
        Platform.runLater(() -> {
            if (gameOverOverlay != null && animationLayer != null) {
                animationLayer.getChildren().remove(gameOverOverlay);
                gameOverOverlay = null;
            }

            Label lblCountdown = new Label();
            lblCountdown.setAlignment(Pos.CENTER);
            lblCountdown.setMouseTransparent(true);
            StackPane.setAlignment(lblCountdown, Pos.CENTER);

            StackPane countPane = new StackPane(lblCountdown);
            countPane.setMouseTransparent(true);
            countPane.setPrefSize(mainBoardPane.getWidth(), mainBoardPane.getHeight());
            animationLayer.getChildren().add(countPane);

            String[] steps = {"3", "2", "1", "BẮT ĐẦU!"};
            String[] colors = {"#f1c40f", "#e67e22", "#e74c3c", "#2ecc71"};

            Timeline tl = new Timeline();
            for (int i = 0; i < steps.length; i++) {
                final int idx = i;
                KeyFrame kf = new KeyFrame(Duration.millis(i * 900), e -> {
                    lblCountdown.setText(steps[idx]);
                    lblCountdown.setStyle(
                        "-fx-font-size: " + (idx == 3 ? "48px" : "84px") + ";" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: " + colors[idx] + ";" +
                        "-fx-effect: dropshadow(gaussian, " + colors[idx] + ", 25, 0.7, 0, 0);"
                    );
                    FXSoundHelper.playUnoSnap();

                    ScaleTransition st = new ScaleTransition(Duration.millis(800), lblCountdown);
                    st.setFromX(0.4);
                    st.setFromY(0.4);
                    st.setToX(1.3);
                    st.setToY(1.3);

                    FadeTransition ft = new FadeTransition(Duration.millis(800), lblCountdown);
                    ft.setFromValue(1.0);
                    ft.setToValue(0.1);

                    ParallelTransition pt = new ParallelTransition(st, ft);
                    pt.play();
                });
                tl.getKeyFrames().add(kf);
            }

            tl.setOnFinished(e -> {
                animationLayer.getChildren().remove(countPane);
            });
            tl.play();
        });
    }
}

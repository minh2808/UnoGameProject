package com.ptit.uno.client.view.fx.game;

import com.ptit.uno.model.Card;
import com.ptit.uno.model.CardColor;
import com.ptit.uno.model.CardValue;
import com.ptit.uno.model.GameState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * GameBoardUIBridge: Cầu nối UI Code-Behind điều khiển giao diện bàn chơi UNO (JavaFX FXML).
 * Đóng vai trò là Presentation Helper thuần túy của tầng View,
 * kết nối hai chiều với UnoGameFXView và ClientControl qua GameActionListener.
 */
public class GameBoardUIBridge implements Initializable {

    // 0. LEFT: Game Log
    @FXML
    private ListView<TextFlow> gameLogListView;
    private final ObservableList<TextFlow> gameLogMessages = FXCollections.observableArrayList();
    private String lastActionLog = "";
    private int lastTurnSeat = -999;

    // 1. TOP: Đối thủ
    @FXML
    private Circle opponentAvatarCircle;
    @FXML
    private Label lblOpponentName;
    @FXML
    private Label lblOpponentCards;
    @FXML
    private Label lblOpponentStatus;
    @FXML
    private HBox opponentHandBox;

    // 2. CENTER: Bàn chơi
    @FXML
    private StackPane drawPileContainer;
    @FXML
    private ImageView drawPileImageView;
    @FXML
    private StackPane discardPileContainer;
    @FXML
    private ImageView discardPileImageView;
    @FXML
    private Label lblCountdown;
    @FXML
    private Label lblTurnInfo;
    @FXML
    private Circle turnIndicatorDot;
    @FXML
    private Button btnPlayCard;

    // 3. RIGHT: Chat & Tiện ích
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

    // 4. BOTTOM: Người chơi & Bài trên tay
    @FXML
    private Button btnCallUno;
    @FXML
    private Circle playerAvatarCircle;
    @FXML
    private Label lblPlayerName;
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

    // Callback kết nối với ClientControl
    private GameActionListener actionListener;

    public interface GameActionListener {
        void onPlayCard(Card card, CardColor chosenColor);

        void onDrawCard();

        void onCallUno();

        void onSendMessage(String message);

        void onLeaveGame();
        void onGameFinished();
    }

    public void setActionListener(GameActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatListView.setItems(chatMessages);
        gameLogListView.setItems(gameLogMessages);

        // Nạp ảnh nọc rút mặc định
        Image backImg = FXCardHelper.getCardBackImage();
        if (backImg != null) {
            drawPileImageView.setImage(backImg);
        }

        // Khởi tạo trạng thái ban đầu sạch sẽ
        lblOpponentName.setText("Đối thủ");
        lblOpponentCards.setText("🂠 0 lá");
        lblOpponentStatus.setText("Đang chờ");
        btnPlayCard.setDisable(true);

        addGameLog("HỆ THỐNG: ", "Trận đấu đang chuẩn bị...", Color.web("#f1c40f"), Color.web("#ecf0f1"));
    }

    public void setPlayerInfo(String username) {
        Platform.runLater(() -> {
            if (lblPlayerName != null) {
                lblPlayerName.setText(username);
            }
        });
    }

    // ==========================================
    // CẬP NHẬT TRẠNG THÁI TỪ SERVER
    // ==========================================

    /**
     * Cập nhật toàn bộ trạng thái bàn cờ từ GameState của Server
     */
    public void updateGameState(GameState state, int currentUserId) {
        if (state == null)
            return;
        Platform.runLater(() -> {
            // 1. Cập nhật lá bài trên bàn
            Card top = state.getTopCard();
            if (top != null) {
                currentTopCard = top;
                currentActiveColor = state.getActiveColor();
                setTopCard(top, state.getActiveColor());
            }

            // 2. Cập nhật Game Log panel
            String action = state.getLastActionLog();
            if (action != null && !action.isEmpty()) {
                if (!action.equals(lastActionLog)) {
                    lastActionLog = action;
                    addGameLog("MỚI NHẤT: ", action, Color.web("#f1c40f"), Color.web("#ffffff"));
                }
            }

            // Ghi log lượt đi khi có thay đổi
            if (state.getCurrentTurnSeat() != lastTurnSeat) {
                lastTurnSeat = state.getCurrentTurnSeat();
                boolean myTurnStatus = false;
                String turnName = "Đối thủ";
                if (state.getPlayerSummaries() != null) {
                    for (GameState.PlayerSummary p : state.getPlayerSummaries()) {
                        if (p.getUserId() == currentUserId) {
                            if (p.getSeatNumber() == lastTurnSeat) myTurnStatus = true;
                        }
                        if (p.getSeatNumber() == lastTurnSeat) {
                            turnName = p.getUsername();
                        }
                    }
                }
                this.isMyTurn = myTurnStatus;
                addGameLog("HỆ THỐNG: ", isMyTurn ? "Đến lượt BẠN" : "Đến lượt " + turnName, Color.web("#f1c40f"),
                        Color.web("#a0c4ab"));
            } else {
                // Đảm bảo isMyTurn được cập nhật kể cả khi lastTurnSeat không đổi (trường hợp mới vào phòng)
                if (state.getPlayerSummaries() != null) {
                    for (GameState.PlayerSummary p : state.getPlayerSummaries()) {
                        if (p.getUserId() == currentUserId && p.getSeatNumber() == state.getCurrentTurnSeat()) {
                            this.isMyTurn = true;
                        }
                    }
                }
            }

            // 3. Đếm ngược
            startCountdown(state.getRemainingSeconds());

            // 4. Cập nhật danh sách người chơi & đối thủ
            List<GameState.PlayerSummary> summaries = state.getPlayerSummaries();
            if (summaries != null) {
                for (GameState.PlayerSummary p : summaries) {
                    boolean isHisTurn = (p.getSeatNumber() == state.getCurrentTurnSeat());

                    if (p.getUserId() != currentUserId) {
                        // Bàn 2 người: người còn lại là đối thủ
                        String status = isHisTurn ? "Đang đánh..." : "Đang chờ";
                        if (p.isUno())
                            status = "[UNO!] " + status;
                        setOpponentInfo(p.getUsername() + (p.isBot() ? " (Bot)" : ""), p.getCardCount(), status);
                    }
                }
            }

            // 5. Kết thúc ván đấu
            if (state.isGameOver()) {
                if (countdownTimeline != null)
                    countdownTimeline.stop();
                addGameLog("HỆ THỐNG: ", "Trận đấu kết thúc! Thắng: " + state.getWinnerUsername(), Color.web("#e74c3c"),
                        Color.web("#f1c40f"));
                Alert winAlert = new Alert(Alert.AlertType.INFORMATION);
                winAlert.setTitle("Kết quả trận đấu");
                winAlert.setHeaderText("TRẬN ĐẤU ĐÃ KẾT THÚC!");
                winAlert.setContentText("Người chiến thắng: " + state.getWinnerUsername());
                winAlert.showAndWait();
                
                // Trở về phòng chờ sau khi tắt Alert
                if (actionListener != null) {
                    actionListener.onGameFinished();
                }
            }
        });
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
        if (lblCountdown != null) {
            lblCountdown.setText(remainingSeconds + "s");
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            if (lblCountdown != null) {
                lblCountdown.setText(Math.max(0, remainingSeconds) + "s");
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
                switch (activeColor) {
                    case RED:
                        borderCol = "#e74c3c";
                        break;
                    case YELLOW:
                        borderCol = "#f1c40f";
                        break;
                    case GREEN:
                        borderCol = "#2ecc71";
                        break;
                    case BLUE:
                        borderCol = "#3498db";
                        break;
                    default:
                        borderCol = "transparent";
                        break;
                }
                discardPileContainer.setStyle("-fx-border-color: " + borderCol + "; -fx-border-width: 3px; -fx-border-radius: 8px;");
            }
        });
    }

    public void setHandCards(List<Card> cards) {
        Platform.runLater(() -> {
            currentHand.clear();
            if (cards != null) {
                currentHand.addAll(cards);
            }
            handCardsBox.getChildren().clear();
            clearSelection();

            for (Card card : currentHand) {
                StackPane cardView = createCardView(card);
                handCardsBox.getChildren().add(cardView);
            }
        });
    }

    private StackPane createCardView(Card card) {
        StackPane container = new StackPane();
        container.getStyleClass().add("hand-card-view");

        ImageView iv = new ImageView(FXCardHelper.getCardImage(card));
        iv.setFitWidth(70);
        iv.setFitHeight(105);
        iv.setPreserveRatio(true);

        container.getChildren().add(iv);

        container.setOnMouseClicked((MouseEvent e) -> {
            selectCard(card, container);
        });

        return container;
    }

    private void selectCard(Card card, StackPane cardView) {
        clearSelection();
        selectedCard = card;
        selectedCardView = cardView;

        selectedCardView.getStyleClass().add("hand-card-selected");
        btnPlayCard.setDisable(false);
    }

    private void clearSelection() {
        if (selectedCardView != null) {
            selectedCardView.getStyleClass().remove("hand-card-selected");
        }
        selectedCard = null;
        selectedCardView = null;
        btnPlayCard.setDisable(true);
    }

    // ==========================================
    // SỰ KIỆN TƯƠNG TÁC (ACTIONS GỬI VỀ SERVER)
    // ==========================================

    @FXML
    private void handlePlayCard(ActionEvent event) {
        if (selectedCard == null)
            return;

        if (!isMyTurn) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Chưa đến lượt của bạn!");
            alert.showAndWait();
            return;
        }

        if (currentTopCard != null && !selectedCard.canPlayOn(currentTopCard, currentActiveColor)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nước đi không hợp lệ");
            alert.setHeaderText(null);
            alert.setContentText("Lá bài này không thể đánh! Phải cùng màu, cùng số/chức năng, hoặc bài Đen (Wild).");
            alert.showAndWait();
            return;
        }

        if (selectedCard.isWild()) {
            showChooseColorDialog(selectedCard);
        } else {
            Card cardToPlay = selectedCard;
            clearSelection();
            if (actionListener != null) {
                actionListener.onPlayCard(cardToPlay, cardToPlay.getColor());
            }
        }
    }

    @FXML
    private void handleDrawCard(MouseEvent event) {
        if (!isMyTurn) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Chưa đến lượt của bạn!");
            alert.showAndWait();
            return;
        }
        if (actionListener != null) {
            actionListener.onDrawCard();
        }
    }

    @FXML
    private void handleCallUno(ActionEvent event) {
        if (actionListener != null) {
            actionListener.onCallUno();
        }
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        String msg = txtChatInput.getText().trim();
        if (!msg.isEmpty()) {
            txtChatInput.clear();
            if (actionListener != null) {
                actionListener.onSendMessage(msg);
            }
        }
    }

    @FXML
    private void handleHelp(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Trợ giúp UNO");
        alert.setHeaderText("Luật chơi UNO cơ bản");
        alert.setContentText("1. Đánh lá bài cùng màu hoặc cùng số/ký hiệu với lá bài trên bàn.\n" +
                "2. Lá Đổi màu (Wild) có thể đánh bất cứ lúc nào.\n" +
                "3. Khi còn 2 lá chuẩn bị đánh xuống 1 lá, nhớ bấm 'HÔ UNO!'.\n" +
                "4. Nếu không có bài đánh, bấm vào chồng bài úp để bốc bài.");
        alert.showAndWait();
    }

    @FXML
    private void handleLeave(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn thoát ván đấu? Bot sẽ đánh thay bạn!",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES && actionListener != null) {
                actionListener.onLeaveGame();
            }
        });
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

    private void showChooseColorDialog(Card card) {
        Dialog<CardColor> dialog = new Dialog<>();
        dialog.setTitle("Chọn màu hiệu lực");
        dialog.setHeaderText("Chọn màu tiếp theo cho bàn chơi:");

        ButtonType btnOk = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        HBox colorBox = new HBox(15);
        colorBox.setAlignment(Pos.CENTER);
        ToggleGroup group = new ToggleGroup();

        RadioButton rbRed = new RadioButton("Đỏ");
        rbRed.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        rbRed.setToggleGroup(group);
        rbRed.setSelected(true);
        rbRed.setUserData(CardColor.RED);

        RadioButton rbYellow = new RadioButton("Vàng");
        rbYellow.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold;");
        rbYellow.setToggleGroup(group);
        rbYellow.setUserData(CardColor.YELLOW);

        RadioButton rbGreen = new RadioButton("Lục");
        rbGreen.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        rbGreen.setToggleGroup(group);
        rbGreen.setUserData(CardColor.GREEN);

        RadioButton rbBlue = new RadioButton("Lam");
        rbBlue.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
        rbBlue.setToggleGroup(group);
        rbBlue.setUserData(CardColor.BLUE);

        colorBox.getChildren().addAll(rbRed, rbYellow, rbGreen, rbBlue);
        dialog.getDialogPane().setContent(colorBox);

        dialog.setResultConverter(b -> {
            if (b == btnOk) {
                return (CardColor) group.getSelectedToggle().getUserData();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(chosenColor -> {
            clearSelection();
            if (actionListener != null) {
                actionListener.onPlayCard(card, chosenColor);
            }
        });
    }
}

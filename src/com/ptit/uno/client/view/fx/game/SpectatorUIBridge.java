package com.ptit.uno.client.view.fx.game;

import com.ptit.uno.model.Card;
import com.ptit.uno.model.GameState;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SpectatorUIBridge implements Initializable {
    @FXML private Label lblHostName;
    @FXML private Label lblHostStatus;
    @FXML private HBox hostHandBox;

    @FXML private Label lblGuestName;
    @FXML private Label lblGuestStatus;
    @FXML private HBox guestHandBox;

    @FXML private ImageView drawPileImageView;
    @FXML private ImageView discardPileImageView;
    @FXML private Label lblCountdown;
    @FXML private Label lblTurnInfo;
    @FXML private Circle turnIndicatorDot;

    @FXML private ListView<TextFlow> gameLogListView;
    @FXML private ListView<TextFlow> chatListView;
    @FXML private TextField txtChatInput;

    private final ObservableList<TextFlow> gameLogMessages = FXCollections.observableArrayList();
    private final ObservableList<TextFlow> chatMessages = FXCollections.observableArrayList();
    
    private Runnable onLeaveAction;
    private java.util.function.Consumer<String> onSendChatAction;
    private String lastActionLog = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gameLogListView.setItems(gameLogMessages);
        chatListView.setItems(chatMessages);
        renderCardBack(drawPileImageView);
    }

    public void setOnLeaveAction(Runnable r) { this.onLeaveAction = r; }
    public void setOnSendChatAction(java.util.function.Consumer<String> c) { this.onSendChatAction = c; }

    @FXML
    private void handleLeave(ActionEvent event) {
        if (countdownTimeline != null) countdownTimeline.stop();
        if (onLeaveAction != null) onLeaveAction.run();
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        String msg = txtChatInput.getText().trim();
        if (!msg.isEmpty() && onSendChatAction != null) {
            onSendChatAction.accept(msg);
            txtChatInput.clear();
        }
    }

    public void updateGameState(GameState state, List<Card> hostHand, List<Card> guestHand) {
        Platform.runLater(() -> {
            // Update center pile
            if (state.getTopCard() != null) {
                renderCardFront(discardPileImageView, state.getTopCard());
            }

            // Update hands
            renderHand(hostHandBox, hostHand);
            renderHand(guestHandBox, guestHand);

            // Turn info
            if (state.getCurrentTurnSeat() == 0) {
                lblHostStatus.setText("Tới lượt");
                lblHostStatus.setStyle("-fx-text-fill: #f1c40f;");
                lblGuestStatus.setText("Đang chờ");
                lblGuestStatus.setStyle("-fx-text-fill: #7f8c8d;");
                lblTurnInfo.setText("Lượt: " + lblHostName.getText());
                turnIndicatorDot.setFill(Color.web("#e67e22"));
            } else {
                lblGuestStatus.setText("Tới lượt");
                lblGuestStatus.setStyle("-fx-text-fill: #f1c40f;");
                lblHostStatus.setText("Đang chờ");
                lblHostStatus.setStyle("-fx-text-fill: #7f8c8d;");
                lblTurnInfo.setText("Lượt: " + lblGuestName.getText());
                turnIndicatorDot.setFill(Color.web("#2980b9"));
            }

            // Game log
            if (state.getLastActionLog() != null && !state.getLastActionLog().isEmpty() && !state.getLastActionLog().equals(lastActionLog)) {
                lastActionLog = state.getLastActionLog();
                appendGameLog(lastActionLog, state.getCurrentTurnSeat() == 0);
            }

            if (state.isGameOver()) {
                if (countdownTimeline != null) countdownTimeline.stop();
                appendGameLog("HỆ THỐNG: Trận đấu kết thúc! Thắng: " + state.getWinnerUsername(), true);
                javafx.scene.control.Alert winAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                winAlert.setTitle("Kết quả trận đấu");
                winAlert.setHeaderText("TRẬN ĐẤU ĐÃ KẾT THÚC!");
                winAlert.setContentText("Người chiến thắng: " + state.getWinnerUsername());
                winAlert.showAndWait();
                
                if (onLeaveAction != null) {
                    onLeaveAction.run();
                }
            }
        });
    }

    public void setPlayersInfo(String hostName, String guestName) {
        Platform.runLater(() -> {
            lblHostName.setText(hostName);
            lblGuestName.setText(guestName);
        });
    }

    private int remainingSeconds = 15;
    private javafx.animation.Timeline countdownTimeline;

    public void updateCountdown(int timeLeft) {
        Platform.runLater(() -> {
            this.remainingSeconds = timeLeft;
            updateCountdownUI();
            
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }
            
            countdownTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                remainingSeconds--;
                if (remainingSeconds <= 0) {
                    remainingSeconds = 0;
                    countdownTimeline.stop();
                }
                updateCountdownUI();
            }));
            countdownTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
            countdownTimeline.play();
        });
    }

    private void updateCountdownUI() {
        lblCountdown.setText(remainingSeconds + "s");
        if (remainingSeconds <= 5) {
            lblCountdown.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else {
            lblCountdown.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: normal;");
        }
    }

    public void addChatMessage(String sender, String message, boolean isSelf) {
        Platform.runLater(() -> {
            TextFlow flow = new TextFlow();
            Text senderText = new Text(sender + ": ");
            senderText.setStyle("-fx-font-weight: bold; -fx-fill: " + (isSelf ? "#4ff0a0" : "#8ea6d8") + ";");
            Text msgText = new Text(message);
            msgText.setStyle("-fx-fill: #eaf1ff;");
            flow.getChildren().addAll(senderText, msgText);
            chatMessages.add(flow);
            chatListView.scrollTo(chatMessages.size() - 1);
        });
    }

    private void appendGameLog(String logMsg, boolean isHostTurn) {
        TextFlow flow = new TextFlow();
        Text text = new Text(logMsg);
        text.setStyle("-fx-fill: " + (isHostTurn ? "#f39c12" : "#3498db") + "; -fx-font-size: 13px;");
        flow.getChildren().add(text);
        gameLogMessages.add(flow);
        gameLogListView.scrollTo(gameLogMessages.size() - 1);
    }

    private void renderHand(HBox handBox, List<Card> hand) {
        handBox.getChildren().clear();
        if (hand == null) return;
        
        for (Card card : hand) {
            StackPane cardPane = new StackPane();
            cardPane.setAlignment(Pos.CENTER);
            cardPane.setPrefSize(70, 105);
            cardPane.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-radius: 8px; -fx-border-width: 1px;");
            
            ImageView imgView = new ImageView();
            imgView.setFitWidth(65);
            imgView.setFitHeight(100);
            renderCardFront(imgView, card);
            
            cardPane.getChildren().add(imgView);
            handBox.getChildren().add(cardPane);
        }
    }

    private void renderCardFront(ImageView view, Card card) {
        Image img = FXCardHelper.getCardImage(card);
        if (img != null) {
            view.setImage(img);
        }
    }

    private void renderCardBack(ImageView view) {
        Image img = FXCardHelper.getCardBackImage();
        if (img != null) {
            view.setImage(img);
        }
    }
}

package com.ptit.uno.client.view.fx.lobby;

import com.ptit.uno.client.view.fx.game.FXSoundHelper;
import com.ptit.uno.client.view.fx.game.GameBoardUIBridge;
import com.ptit.uno.model.Card;
import com.ptit.uno.model.CardColor;
import com.ptit.uno.model.CardValue;
import com.ptit.uno.model.GameState;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * BotPracticeGameFXStage: Bàn đấu thực hành tự do với Bot (Free-play Practice Match vs Bot).
 * - Tái sử dụng 100% giao diện game_board.fxml.
 * - Luật UNO chuẩn 108 lá: chia 7 lá, đánh cùng màu/cùng số, lá chức năng (+2, Skip, Reverse, Wild, +4).
 * - Bot tự động quét các lá hợp lệ trên tay để đánh hoặc rút bài từ nọc.
 * - Người chơi tự do thao tác, thử sức và làm quen nhịp độ trận đấu thực thụ.
 */
public class BotPracticeGameFXStage {

    private static final int PLAYER_USER_ID = 1;
    private static final int BOT_USER_ID = 2;
    private static final String BOT_NAME = "Bot Huấn Luyện";

    private Stage stage;
    private StackPane rootStackPane;
    private GameBoardUIBridge boardController;
    private String playerUsername;
    private Pane overlayPane;
    private StackPane modalOverlay;

    // Trạng thái ván đấu
    private final List<Card> drawPile = new ArrayList<>();
    private final List<Card> discardPile = new ArrayList<>();
    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> botHand = new ArrayList<>();

    private Card topCard;
    private CardColor activeColor;
    private int currentTurnSeat = 0; // 0: Người chơi, 1: Bot
    private boolean playerUnoCalled = false;
    private boolean botUnoCalled = false;
    private boolean isGameOver = false;
    private boolean waitingForBot = false;
    private int cardIdSequence = 1;
    private PauseTransition unoPenaltyTimer;

    public BotPracticeGameFXStage(Window owner, String playerUsername) {
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
                System.err.println("[BotPracticeGameFXStage] Không tìm thấy game_board.fxml!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent loadedRoot = loader.load();
            this.rootStackPane = (StackPane) loadedRoot;
            this.boardController = loader.getController();

            this.boardController.setTutorialMode(true);
            this.boardController.setTutorialGuard(null); // Cho phép người chơi tự do thao tác theo luật UNO
            this.boardController.setPlayerInfo(playerUsername);

            setupActionListener();
            setupOverlayLayer();

            stage = new Stage();
            stage.setTitle("UNO Online - Đấu Tập Tự Do Với Bot");
            if (owner != null) {
                stage.initOwner(owner);
                stage.initModality(Modality.WINDOW_MODAL);
            }
            Scene scene = new Scene(rootStackPane, 1150, 680);
            stage.setScene(scene);
            stage.setMinWidth(1050);
            stage.setMinHeight(640);
            stage.centerOnScreen();

            stage.setOnCloseRequest(e -> closeStage());

        } catch (Exception e) {
            System.err.println("[BotPracticeGameFXStage] Lỗi khởi tạo bàn đấu tập với Bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void show() {
        if (stage != null) {
            FXSoundHelper.stopLobbyMusic();
            stage.show();
            stage.toFront();
            startNewMatch();
        }
    }

    private void setupOverlayLayer() {
        overlayPane = new Pane();
        overlayPane.setPickOnBounds(false);
        overlayPane.prefWidthProperty().bind(rootStackPane.widthProperty());
        overlayPane.prefHeightProperty().bind(rootStackPane.heightProperty());

        // Nút "Thoát Phòng Tập" ở góc trên bên trái
        Button btnExit = new Button("Thoát Phòng Tập");
        FontIcon exitIcon = new FontIcon("fas-arrow-left");
        exitIcon.setIconSize(12);
        exitIcon.setIconColor(Color.web("#e2e8f0"));
        btnExit.setGraphic(exitIcon);
        btnExit.setLayoutX(18);
        btnExit.setLayoutY(14);
        btnExit.setCursor(Cursor.HAND);
        btnExit.setStyle(
            "-fx-background-color: rgba(15, 23, 42, 0.85);" +
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
        btnExit.setOnAction(e -> {
            FXSoundHelper.playButtonClick();
            closeStage();
        });

        overlayPane.getChildren().add(btnExit);
        rootStackPane.getChildren().add(overlayPane);
    }

    private void setupActionListener() {
        boardController.setActionListener(new GameBoardUIBridge.GameActionListener() {
            @Override
            public void onPlayCard(Card card, CardColor chosenColor) {
                handlePlayerPlayCard(card, chosenColor);
            }

            @Override
            public void onDrawCard() {
                handlePlayerDrawCard();
            }

            @Override
            public void onCallUno() {
                handlePlayerCallUno();
            }

            @Override
            public void onSendMessage(String message) {
                boardController.addChatMessage(playerUsername, message, Color.web("#f1c40f"));
                handleBotChatReply(message);
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

    // =========================================================================
    // VÒNG LẶP VÁN ĐẤU THỰC HÀNH VỚI BOT
    // =========================================================================

    public void startNewMatch() {
        cancelUnoPenaltyTimer();
        if (modalOverlay != null) {
            overlayPane.getChildren().remove(modalOverlay);
            modalOverlay = null;
        }

        isGameOver = false;
        waitingForBot = false;
        playerUnoCalled = false;
        botUnoCalled = false;
        currentTurnSeat = 0; // Người chơi đi trước

        // 1. Tạo bộ bài 108 lá chuẩn UNO
        initDeck();

        // 2. Chia 7 lá cho người chơi và 7 lá cho Bot
        playerHand.clear();
        botHand.clear();
        for (int i = 0; i < 7; i++) {
            playerHand.add(drawCardFromDeck());
            botHand.add(drawCardFromDeck());
        }

        // 3. Lật lá bài mở đầu (chọn lá số 0-9)
        Card firstCard;
        do {
            firstCard = drawCardFromDeck();
            if (firstCard.getValue().ordinal() <= CardValue.NINE.ordinal()) {
                break;
            } else {
                drawPile.add(0, firstCard);
            }
        } while (true);

        discardPile.clear();
        discardPile.add(firstCard);
        topCard = firstCard;
        activeColor = firstCard.getColor();

        // 4. Reset giao diện bàn đấu và hiển thị
        boardController.resetBoardForTutorial();
        boardController.setHandCards(new ArrayList<>(playerHand));

        pushGameStateToView(0, "Trận đấu thực hành bắt đầu! Lá bài mở đầu là " + getCardDisplayName(topCard) + ".");
        boardController.addChatMessage(BOT_NAME, "Chào " + playerUsername + "! Cùng đấu thử một ván sòng phẳng nhé!", Color.web("#2ecc71"));
    }

    private void initDeck() {
        drawPile.clear();
        discardPile.clear();
        cardIdSequence = 1;

        CardColor[] normalColors = {CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE};

        for (CardColor c : normalColors) {
            // Mỗi màu có 1 lá số 0
            drawPile.add(new Card("deck_" + (cardIdSequence++), c, CardValue.ZERO));

            // Mỗi màu có 2 lá từ 1 đến 9
            CardValue[] numbers = {
                CardValue.ONE, CardValue.TWO, CardValue.THREE, CardValue.FOUR,
                CardValue.FIVE, CardValue.SIX, CardValue.SEVEN, CardValue.EIGHT, CardValue.NINE
            };
            for (CardValue v : numbers) {
                drawPile.add(new Card("deck_" + (cardIdSequence++), c, v));
                drawPile.add(new Card("deck_" + (cardIdSequence++), c, v));
            }

            // Mỗi màu có 2 lá Skip, 2 lá Reverse, 2 lá Draw Two (+2)
            CardValue[] actions = {CardValue.SKIP, CardValue.REVERSE, CardValue.DRAW_TWO};
            for (CardValue v : actions) {
                drawPile.add(new Card("deck_" + (cardIdSequence++), c, v));
                drawPile.add(new Card("deck_" + (cardIdSequence++), c, v));
            }
        }

        // 4 lá Wild và 4 lá Wild Draw Four (+4)
        for (int i = 0; i < 4; i++) {
            drawPile.add(new Card("deck_" + (cardIdSequence++), CardColor.WILD, CardValue.WILD));
            drawPile.add(new Card("deck_" + (cardIdSequence++), CardColor.WILD, CardValue.WILD_DRAW_FOUR));
        }

        Collections.shuffle(drawPile);
    }

    private Card drawCardFromDeck() {
        if (drawPile.isEmpty()) {
            if (discardPile.size() > 1) {
                Card top = discardPile.remove(discardPile.size() - 1);
                drawPile.addAll(discardPile);
                discardPile.clear();
                discardPile.add(top);
                Collections.shuffle(drawPile);
                boardController.addGameLog("HỆ THỐNG: ", "Nọc bài đã được xào lại từ các lá đã đánh.", Color.web("#f1c40f"), Color.web("#94a3b8"));
            } else {
                drawPile.add(new Card("fallback_" + (cardIdSequence++), CardColor.RED, CardValue.FIVE));
            }
        }
        return drawPile.remove(drawPile.size() - 1);
    }

    // =========================================================================
    // XỬ LÝ NƯỚC ĐI CỦA NGƯỜI CHƠI
    // =========================================================================

    private void handlePlayerPlayCard(Card card, CardColor chosenColor) {
        if (isGameOver || waitingForBot || currentTurnSeat != 0) {
            return;
        }

        // Tìm lá bài trên tay người chơi
        Card cardToRemove = null;
        for (Card c : playerHand) {
            if (c.equals(card) || (c.getColor() == card.getColor() && c.getValue() == card.getValue())) {
                cardToRemove = c;
                break;
            }
        }
        if (cardToRemove != null) {
            playerHand.remove(cardToRemove);
        } else {
            playerHand.remove(card);
        }

        discardPile.add(card);
        topCard = card;
        activeColor = card.isWild() ? (chosenColor != null && chosenColor != CardColor.WILD ? chosenColor : CardColor.RED) : card.getColor();
        boardController.setHandCards(new ArrayList<>(playerHand));

        // Kiểm tra Hết bài -> Người chơi thắng!
        if (playerHand.isEmpty()) {
            handleGameOver(true);
            return;
        }

        // Kiểm tra còn 1 lá bài: nếu chưa bấm UNO trong 5 giây sẽ bị phạt bốc 2 lá
        if (playerHand.size() == 1) {
            playerUnoCalled = false;
            boardController.addChatMessage(BOT_NAME, "Bạn chỉ còn 1 lá bài kìa! Nhớ bấm nút UNO trong 5 giây kẻo bị phạt bốc 2 lá nhé!", Color.web("#f39c12"));
            schedulePlayerUnoPenalty();
        } else {
            playerUnoCalled = false;
            cancelUnoPenaltyTimer();
        }

        // Xử lý hiệu ứng lá bài
        CardValue val = card.getValue();
        if (val == CardValue.SKIP) {
            currentTurnSeat = 0;
            pushGameStateToView(0, playerUsername + " đã đánh lá CẤM LƯỢT (Skip)! Bot bị mất lượt.");
            boardController.addChatMessage(BOT_NAME, "Úi, mình bị cấm lượt rồi!", Color.web("#e67e22"));
        } else if (val == CardValue.REVERSE) {
            currentTurnSeat = 0;
            pushGameStateToView(0, playerUsername + " đã đánh lá ĐẢO CHIỀU (Reverse)! Bot bị mất lượt.");
            boardController.addChatMessage(BOT_NAME, "Bàn 2 người đảo chiều như cấm lượt vậy! Bạn đi tiếp nhé.", Color.web("#e67e22"));
        } else if (val == CardValue.DRAW_TWO) {
            currentTurnSeat = 0;
            botHand.add(drawCardFromDeck());
            botHand.add(drawCardFromDeck());
            pushGameStateToView(0, playerUsername + " đã đánh lá RÚT 2 (+2)! Bot bị bốc 2 lá và mất lượt.");
            boardController.addChatMessage(BOT_NAME, "+2 lá đau quá! Bạn đi tiếp đi!", Color.web("#e74c3c"));
        } else if (val == CardValue.WILD_DRAW_FOUR) {
            currentTurnSeat = 0;
            for (int i = 0; i < 4; i++) {
                botHand.add(drawCardFromDeck());
            }
            pushGameStateToView(0, playerUsername + " đã đánh WILD +4 (Chọn màu " + getColorDisplayName(activeColor) + ")! Bot phải bốc 4 lá.");
            boardController.addChatMessage(BOT_NAME, "+4 luôn cơ à! Combo mạnh thật đấy!", Color.web("#e74c3c"));
        } else {
            currentTurnSeat = 1;
            String msg = playerUsername + " đã đánh lá " + getCardDisplayName(card) + (card.isWild() ? " (Chọn màu " + getColorDisplayName(activeColor) + ")" : "") + ".";
            pushGameStateToView(1, msg);
            scheduleBotTurn(1100);
        }
    }

    private void handlePlayerDrawCard() {
        if (isGameOver || waitingForBot || currentTurnSeat != 0) {
            return;
        }

        Card drawn = drawCardFromDeck();
        playerHand.add(drawn);
        playerUnoCalled = false;
        cancelUnoPenaltyTimer();
        boardController.setHandCards(new ArrayList<>(playerHand));

        currentTurnSeat = 1;
        pushGameStateToView(1, playerUsername + " đã rút 1 lá bài từ nọc.");
        scheduleBotTurn(1200);
    }

    private void handlePlayerCallUno() {
        playerUnoCalled = true;
        cancelUnoPenaltyTimer();
        boardController.addChatMessage(playerUsername, "UNO!!!", Color.web("#f1c40f"));
        pushGameStateToView(currentTurnSeat, playerUsername + " đã hô UNO thành công!");
    }

    private void schedulePlayerUnoPenalty() {
        cancelUnoPenaltyTimer();
        unoPenaltyTimer = new PauseTransition(Duration.seconds(5));
        unoPenaltyTimer.setOnFinished(e -> {
            if (!isGameOver && playerHand.size() == 1 && !playerUnoCalled) {
                Card c1 = drawCardFromDeck();
                Card c2 = drawCardFromDeck();
                if (c1 != null) playerHand.add(c1);
                if (c2 != null) playerHand.add(c2);
                boardController.setHandCards(new ArrayList<>(playerHand));
                boardController.addChatMessage(BOT_NAME, "Úi! Bạn quên hô UNO trong 5 giây nên bị phạt bốc 2 lá bài rồi!", Color.web("#e74c3c"));
                pushGameStateToView(currentTurnSeat, "HỆ THỐNG: " + playerUsername + " quên hô UNO trong 5 giây! Bị phạt bốc 2 lá bài.");
            }
        });
        unoPenaltyTimer.play();
    }

    private void cancelUnoPenaltyTimer() {
        if (unoPenaltyTimer != null) {
            unoPenaltyTimer.stop();
            unoPenaltyTimer = null;
        }
    }

    // =========================================================================
    // XỬ LÝ LƯỢT ĐI CỦA BOT (QUÉT BÀI HỢP LỆ TRÊN TAY)
    // =========================================================================

    private void scheduleBotTurn(long delayMillis) {
        waitingForBot = true;
        PauseTransition pt = new PauseTransition(Duration.millis(delayMillis));
        pt.setOnFinished(e -> executeBotTurn());
        pt.play();
    }

    private void executeBotTurn() {
        if (isGameOver) {
            waitingForBot = false;
            return;
        }

        // 1. Bot tự quét tất cả các lá bài hợp lệ trên tay nó
        List<Card> playableCards = new ArrayList<>();
        for (Card c : botHand) {
            if (c.canPlayOn(topCard, activeColor)) {
                playableCards.add(c);
            }
        }

        // 2. Nếu có bài đánh được
        if (!playableCards.isEmpty()) {
            Card chosenCard = chooseBestCardForBot(playableCards);
            botHand.remove(chosenCard);
            discardPile.add(chosenCard);
            topCard = chosenCard;

            if (chosenCard.isWild()) {
                activeColor = chooseBestColorForBot();
            } else {
                activeColor = chosenCard.getColor();
            }

            if (botHand.size() == 1) {
                botUnoCalled = true;
                boardController.addChatMessage(BOT_NAME, "UNO!!! Mình chỉ còn đúng 1 lá thôi nhé!", Color.web("#2ecc71"));
            } else {
                botUnoCalled = false;
            }

            if (botHand.isEmpty()) {
                handleGameOver(false);
                return;
            }

            CardValue val = chosenCard.getValue();
            if (val == CardValue.SKIP) {
                currentTurnSeat = 1;
                pushGameStateToView(1, BOT_NAME + " đã đánh lá CẤM LƯỢT (Skip)! Bạn bị mất lượt.");
                boardController.addChatMessage(BOT_NAME, "Mình cấm lượt bạn nhé!", Color.web("#2ecc71"));
                scheduleBotTurn(1200);
            } else if (val == CardValue.REVERSE) {
                currentTurnSeat = 1;
                pushGameStateToView(1, BOT_NAME + " đã đánh lá ĐẢO CHIỀU (Reverse)! Bạn bị mất lượt.");
                boardController.addChatMessage(BOT_NAME, "Đảo chiều! Mình đi tiếp một lượt nữa.", Color.web("#2ecc71"));
                scheduleBotTurn(1200);
            } else if (val == CardValue.DRAW_TWO) {
                currentTurnSeat = 1;
                playerHand.add(drawCardFromDeck());
                playerHand.add(drawCardFromDeck());
                boardController.setHandCards(new ArrayList<>(playerHand));
                pushGameStateToView(1, BOT_NAME + " đánh lá RÚT 2 (+2)! Bạn phải bốc 2 lá và mất lượt.");
                boardController.addChatMessage(BOT_NAME, "Tặng bạn 2 lá nè! ^^", Color.web("#e67e22"));
                scheduleBotTurn(1200);
            } else if (val == CardValue.WILD_DRAW_FOUR) {
                currentTurnSeat = 1;
                for (int i = 0; i < 4; i++) {
                    playerHand.add(drawCardFromDeck());
                }
                boardController.setHandCards(new ArrayList<>(playerHand));
                pushGameStateToView(1, BOT_NAME + " đánh WILD +4 (Chọn màu " + getColorDisplayName(activeColor) + ")! Bạn phải bốc 4 lá.");
                boardController.addChatMessage(BOT_NAME, "Lá +4 nhé! Đừng giận mình nha!", Color.web("#e74c3c"));
                scheduleBotTurn(1200);
            } else {
                currentTurnSeat = 0;
                waitingForBot = false;
                String msg = BOT_NAME + " đã đánh lá " + getCardDisplayName(chosenCard) + (chosenCard.isWild() ? " (Chọn màu " + getColorDisplayName(activeColor) + ")" : "") + ".";
                pushGameStateToView(0, msg);
            }

        } else {
            // 3. Nếu KHÔNG có bài đánh được -> Bot rút 1 lá từ nọc
            Card drawn = drawCardFromDeck();
            botHand.add(drawn);
            botUnoCalled = false;

            if (drawn.canPlayOn(topCard, activeColor)) {
                botHand.remove(drawn);
                discardPile.add(drawn);
                topCard = drawn;
                if (drawn.isWild()) {
                    activeColor = chooseBestColorForBot();
                } else {
                    activeColor = drawn.getColor();
                }

                if (botHand.size() == 1) {
                    botUnoCalled = true;
                    boardController.addChatMessage(BOT_NAME, "UNO!!! Mình chỉ còn đúng 1 lá thôi nhé!", Color.web("#2ecc71"));
                }

                if (botHand.isEmpty()) {
                    handleGameOver(false);
                    return;
                }

                CardValue val = drawn.getValue();
                if (val == CardValue.SKIP || val == CardValue.REVERSE || val == CardValue.DRAW_TWO || val == CardValue.WILD_DRAW_FOUR) {
                    currentTurnSeat = 1;
                    pushGameStateToView(1, BOT_NAME + " vừa bốc được lá " + getCardDisplayName(drawn) + " và đánh ra ngay!");
                    scheduleBotTurn(1200);
                } else {
                    currentTurnSeat = 0;
                    waitingForBot = false;
                    pushGameStateToView(0, BOT_NAME + " vừa bốc được lá " + getCardDisplayName(drawn) + " và đánh ra ngay!");
                }
            } else {
                currentTurnSeat = 0;
                waitingForBot = false;
                boardController.addChatMessage(BOT_NAME, "Mình kẹt bài rồi, phải rút 1 lá nhường bạn đi tiếp!", Color.web("#94a3b8"));
                pushGameStateToView(0, BOT_NAME + " không có bài hợp lệ, đã rút 1 lá bài.");
            }
        }
    }

    private Card chooseBestCardForBot(List<Card> playable) {
        List<Card> nonWildCards = new ArrayList<>();
        List<Card> actionCards = new ArrayList<>();

        for (Card c : playable) {
            if (!c.isWild()) {
                nonWildCards.add(c);
                if (c.isActionCard()) {
                    actionCards.add(c);
                }
            }
        }

        if (!actionCards.isEmpty()) {
            return actionCards.get(0);
        }
        if (!nonWildCards.isEmpty()) {
            return nonWildCards.get(0);
        }
        return playable.get(0);
    }

    private CardColor chooseBestColorForBot() {
        int red = 0, yellow = 0, green = 0, blue = 0;
        for (Card c : botHand) {
            if (c.getColor() == CardColor.RED) red++;
            else if (c.getColor() == CardColor.YELLOW) yellow++;
            else if (c.getColor() == CardColor.GREEN) green++;
            else if (c.getColor() == CardColor.BLUE) blue++;
        }

        int max = Math.max(Math.max(red, yellow), Math.max(green, blue));
        if (max == red && red > 0) return CardColor.RED;
        if (max == blue && blue > 0) return CardColor.BLUE;
        if (max == green && green > 0) return CardColor.GREEN;
        if (max == yellow && yellow > 0) return CardColor.YELLOW;
        return CardColor.RED;
    }

    private void pushGameStateToView(int currentSeat, String actionLog) {
        GameState state = new GameState();
        state.setRoomId(999);
        state.setTopCard(topCard);
        state.setActiveColor(activeColor);
        state.setCurrentTurnSeat(currentSeat);
        state.setDirection(1);
        state.setRemainingSeconds(15);
        state.setDrawPileCount(drawPile.size());
        state.setGameOver(isGameOver);
        if (isGameOver) {
            state.setWinnerUsername(currentSeat == 0 ? playerUsername : BOT_NAME);
        }
        state.setLastActionLog(actionLog);

        List<GameState.PlayerSummary> summaries = new ArrayList<>();
        summaries.add(new GameState.PlayerSummary(PLAYER_USER_ID, playerUsername, 0, playerHand.size(), playerUnoCalled, false));
        summaries.add(new GameState.PlayerSummary(BOT_USER_ID, BOT_NAME, 1, botHand.size(), botUnoCalled, true));
        state.setPlayerSummaries(summaries);

        boardController.updateGameState(state, PLAYER_USER_ID);
    }

    private void handleBotChatReply(String playerMessage) {
        String lower = playerMessage.toLowerCase();
        String reply;
        if (lower.contains("chào") || lower.contains("hi") || lower.contains("hello")) {
            reply = "Chào bạn! Chúc bạn chơi vui vẻ nhé!";
        } else if (lower.contains("thua") || lower.contains("sợ")) {
            reply = "Đừng lo, hãy bình tĩnh quan sát màu trên bàn!";
        } else if (lower.contains("uno")) {
            reply = "Hô UNO nhanh thế! Mình phải cẩn thận mới được.";
        } else {
            String[] randomReplies = {
                "Nước đi hay đấy!",
                "Trận này kịch tính thật!",
                "Cùng cố gắng nhé!",
                "Lá bài đẹp lắm!",
                "Để xem ai về đích trước nào!"
            };
            reply = randomReplies[new Random().nextInt(randomReplies.length)];
        }

        PauseTransition pt = new PauseTransition(Duration.millis(750));
        pt.setOnFinished(e -> boardController.addChatMessage(BOT_NAME, reply, Color.web("#2ecc71")));
        pt.play();
    }

    private void handleGameOver(boolean playerWon) {
        isGameOver = true;
        waitingForBot = false;
        cancelUnoPenaltyTimer();

        pushGameStateToView(playerWon ? 0 : 1, playerWon ? (playerUsername + " đã đánh hết bài và CHIẾN THẮNG!") : (BOT_NAME + " đã giành CHIẾN THẮNG!"));
        FXSoundHelper.playGameOver();

        Platform.runLater(() -> {
            modalOverlay = new StackPane();
            modalOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.82);");
            modalOverlay.prefWidthProperty().bind(overlayPane.widthProperty());
            modalOverlay.prefHeightProperty().bind(overlayPane.heightProperty());

            VBox card = new VBox(16);
            card.setAlignment(Pos.CENTER);
            card.setMaxWidth(480);
            card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc);" +
                "-fx-background-radius: 20px;" +
                "-fx-border-color: #cbd5e1;" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 20px;" +
                "-fx-padding: 30px 36px;" +
                "-fx-effect: dropshadow(gaussian, rgba(2, 6, 23, 0.75), 32, 0.25, 0, 10);"
            );

            FontIcon resultIcon = new FontIcon(playerWon ? "fas-trophy" : "fas-robot");
            resultIcon.setIconSize(48);
            resultIcon.setIconColor(Color.web(playerWon ? "#f59e0b" : "#64748b"));

            Label lblTitle = new Label(playerWon ? "CHIẾN THẮNG TUYỆT VỜI!" : "BOT ĐÃ CHIẾN THẮNG!");
            lblTitle.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: " + (playerWon ? "#0f172a;" : "#334155;")
            );

            Label lblDesc = new Label(playerWon ?
                "Chúc mừng " + playerUsername + "! Bạn đã nắm vững luật chơi và đánh bại Bot Huấn Luyện thành công!" :
                "Bot đã may mắn về đích trước! Trận đấu thực hành rất bổ ích, bạn có muốn thử lại ván khác không?");
            lblDesc.setWrapText(true);
            lblDesc.setAlignment(Pos.CENTER);
            lblDesc.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-alignment: center;");

            HBox btnRow = new HBox(16);
            btnRow.setAlignment(Pos.CENTER);
            VBox.setMargin(btnRow, new Insets(8, 0, 0, 0));

            Button btnReplay = new Button("CHƠI LẠI VỚI BOT");
            FontIcon replayIcon = new FontIcon("fas-redo");
            replayIcon.setIconSize(14);
            replayIcon.setIconColor(Color.WHITE);
            btnReplay.setGraphic(replayIcon);
            btnReplay.setCursor(Cursor.HAND);
            btnReplay.setStyle(
                "-fx-background-color: linear-gradient(to right, #10b981, #059669);" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-weight: 800;" +
                "-fx-font-size: 14px;" +
                "-fx-background-radius: 10px;" +
                "-fx-padding: 11px 22px;" +
                "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.4), 12, 0, 0, 2);"
            );
            btnReplay.setOnAction(e -> {
                FXSoundHelper.playButtonClick();
                startNewMatch();
            });

            Button btnBackLobby = new Button("VỀ SẢNH CHỜ");
            FontIcon lobbyIcon = new FontIcon("fas-door-open");
            lobbyIcon.setIconSize(14);
            lobbyIcon.setIconColor(Color.web("#1e293b"));
            btnBackLobby.setGraphic(lobbyIcon);
            btnBackLobby.setCursor(Cursor.HAND);
            btnBackLobby.setStyle(
                "-fx-background-color: #f1f5f9;" +
                "-fx-border-color: #cbd5e1;" +
                "-fx-border-radius: 10px;" +
                "-fx-text-fill: #1e293b;" +
                "-fx-font-weight: 700;" +
                "-fx-font-size: 14px;" +
                "-fx-background-radius: 10px;" +
                "-fx-padding: 11px 22px;"
            );
            btnBackLobby.setOnAction(e -> {
                FXSoundHelper.playButtonClick();
                closeStage();
            });

            btnRow.getChildren().addAll(btnReplay, btnBackLobby);
            card.getChildren().addAll(resultIcon, lblTitle, lblDesc, btnRow);
            modalOverlay.getChildren().add(card);
            overlayPane.getChildren().add(modalOverlay);

            ScaleTransition st = new ScaleTransition(Duration.millis(320), card);
            st.setFromX(0.7);
            st.setFromY(0.7);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void closeStage() {
        cancelUnoPenaltyTimer();
        if (stage != null) {
            stage.close();
        }
        FXSoundHelper.playLobbyMusic();
    }

    private String getCardDisplayName(Card card) {
        if (card == null) return "Không xác định";
        if (card.getValue() == CardValue.WILD) return "Đổi Màu (Wild)";
        if (card.getValue() == CardValue.WILD_DRAW_FOUR) return "Đổi Màu +4 (Wild Draw 4)";

        String colorName = getColorDisplayName(card.getColor());
        String valName;
        switch (card.getValue()) {
            case ZERO: valName = "0"; break;
            case ONE: valName = "1"; break;
            case TWO: valName = "2"; break;
            case THREE: valName = "3"; break;
            case FOUR: valName = "4"; break;
            case FIVE: valName = "5"; break;
            case SIX: valName = "6"; break;
            case SEVEN: valName = "7"; break;
            case EIGHT: valName = "8"; break;
            case NINE: valName = "9"; break;
            case SKIP: valName = "Cấm Lượt"; break;
            case REVERSE: valName = "Đảo Chiều"; break;
            case DRAW_TWO: valName = "Rút 2 (+2)"; break;
            default: valName = card.getValue().toString(); break;
        }
        return colorName + " " + valName;
    }

    private String getColorDisplayName(CardColor color) {
        if (color == null) return "Không màu";
        switch (color) {
            case RED: return "Đỏ";
            case YELLOW: return "Vàng";
            case GREEN: return "Xanh Lá";
            case BLUE: return "Xanh Dương";
            case WILD: return "Đổi Màu";
            default: return color.toString();
        }
    }
}

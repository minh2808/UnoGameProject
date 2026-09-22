package com.ptit.uno.server.control;

import com.ptit.uno.model.*;
import com.ptit.uno.server.dao.MatchDAO;
import com.ptit.uno.server.dao.UserDAO;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * GameManager: Trọng tài quản lý toàn bộ vòng lặp và luật chơi bài UNO cho một phòng đấu.
 * Xử lý: Xào bài 108 lá, chia bài, bắt luật, timer đếm ngược 15s, Bot đánh thay khi ngắt kết nối.
 */
public class GameManager {
    private final Room room;
    private final ServerControl serverControl;
    private final UserDAO userDAO;
    private final MatchDAO matchDAO;

    private List<Card> drawPile;     // Nọc bài rút
    private List<Card> discardPile;  // Nọc bài đã đánh
    private GameState gameState;
    private int totalTurnsCount;

    private ScheduledExecutorService timerExecutor;
    private ScheduledFuture<?> timerTask;

    public GameManager(Room room, ServerControl serverControl) {
        this.room = room;
        this.serverControl = serverControl;
        this.userDAO = new UserDAO();
        this.matchDAO = new MatchDAO();
        this.timerExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Bắt đầu ván chơi: Tạo bộ bài 108 lá, xào, chia 7 lá cho mỗi người.
     */
    public synchronized void startGame() {
        room.setStatus(Room.STATUS_PLAYING);
        totalTurnsCount = 0;
        initDeck();

        // Chia cho mỗi người 7 lá bài
        for (Player p : room.getPlayers()) {
            p.getHand().clear();
            p.setUno(false);
            for (int i = 0; i < 7; i++) {
                p.addCard(drawCardFromDeck());
            }
            serverControl.sendHandUpdate(p.getUserId(), p.getHand());
        }

        // Bốc lá bài đầu tiên lật lên bàn (không được là lá Wild)
        Card firstCard;
        do {
            firstCard = drawCardFromDeck();
            if (firstCard.isWild()) {
                drawPile.add(0, firstCard); // Trả lại nếu là Wild
            } else {
                break;
            }
        } while (true);

        discardPile = new ArrayList<Card>();
        discardPile.add(firstCard);

        gameState = new GameState();
        gameState.setRoomId(room.getId());
        gameState.setTopCard(firstCard);
        gameState.setActiveColor(firstCard.getColor());
        gameState.setCurrentTurnSeat(0); // Ghế 0 đi trước
        gameState.setDirection(1);
        gameState.setDrawPileCount(drawPile.size());
        gameState.setLastActionLog("Trận đấu bắt đầu! Lá bài khởi đầu: " + firstCard);

        broadcastGameState();
        startTurnTimer();
    }

    /**
     * Khởi tạo bộ bài 108 lá chuẩn luật UNO quốc tế.
     */
    private void initDeck() {
        drawPile = new ArrayList<Card>();
        int idCounter = 1;

        CardColor[] normalColors = {CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE};

        for (CardColor c : normalColors) {
            // Mỗi màu có 1 lá số 0
            drawPile.add(new Card("c_" + (idCounter++), c, CardValue.ZERO));

            // Mỗi màu có 2 lá từ 1 đến 9
            CardValue[] numbers = {CardValue.ONE, CardValue.TWO, CardValue.THREE, CardValue.FOUR,
                    CardValue.FIVE, CardValue.SIX, CardValue.SEVEN, CardValue.EIGHT, CardValue.NINE};
            for (CardValue val : numbers) {
                drawPile.add(new Card("c_" + (idCounter++), c, val));
                drawPile.add(new Card("c_" + (idCounter++), c, val));
            }

            // Mỗi màu có 2 lá chức năng: SKIP, REVERSE, DRAW_TWO
            CardValue[] actions = {CardValue.SKIP, CardValue.REVERSE, CardValue.DRAW_TWO};
            for (CardValue val : actions) {
                drawPile.add(new Card("c_" + (idCounter++), c, val));
                drawPile.add(new Card("c_" + (idCounter++), c, val));
            }
        }

        // 4 lá Wild và 4 lá Wild Draw Four
        for (int i = 0; i < 4; i++) {
            drawPile.add(new Card("c_" + (idCounter++), CardColor.WILD, CardValue.WILD));
            drawPile.add(new Card("c_" + (idCounter++), CardColor.WILD, CardValue.WILD_DRAW_FOUR));
        }

        Collections.shuffle(drawPile);
    }

    private Card drawCardFromDeck() {
        if (drawPile.isEmpty()) {
            // Xào lại từ discardPile nếu nọc rút đã hết
            if (discardPile.size() > 1) {
                Card top = discardPile.remove(discardPile.size() - 1);
                drawPile.addAll(discardPile);
                discardPile.clear();
                discardPile.add(top);
                Collections.shuffle(drawPile);
            } else {
                return null;
            }
        }
        return drawPile.remove(drawPile.size() - 1);
    }

    /**
     * Xử lý yêu cầu đánh bài từ một người chơi.
     */
    public synchronized boolean handlePlayCard(int userId, Card cardToPlay, CardColor chosenColor) {
        Player player = room.getPlayer(userId);
        if (player == null || player.getSeatNumber() != gameState.getCurrentTurnSeat()) {
            return false; // Không đúng lượt
        }

        // Tìm lá bài thực sự trên tay
        Card foundCard = null;
        for (Card c : player.getHand()) {
            if (c.equals(cardToPlay) || (c.getColor() == cardToPlay.getColor() && c.getValue() == cardToPlay.getValue())) {
                foundCard = c;
                break;
            }
        }

        if (foundCard == null) return false;

        // Bắt luật: Kiểm tra lá bài có hợp lệ không
        if (!foundCard.canPlayOn(gameState.getTopCard(), gameState.getActiveColor())) {
            return false;
        }

        // Nước đi hợp lệ!
        player.removeCard(foundCard);
        discardPile.add(foundCard);
        gameState.setTopCard(foundCard);
        totalTurnsCount++;

        // Xác định màu sắc mới
        if (foundCard.isWild()) {
            gameState.setActiveColor(chosenColor != null && chosenColor != CardColor.WILD ? chosenColor : CardColor.RED);
        } else {
            gameState.setActiveColor(foundCard.getColor());
        }

        gameState.setLastActionLog(player.getUsername() + " đã đánh lá " + foundCard +
                (foundCard.isWild() ? " (Chọn màu " + gameState.getActiveColor() + ")" : ""));

        // Kiểm tra hết bài -> Thắng cuộc
        if (player.getCardCount() == 0) {
            handleGameOver(player);
            return true;
        }

        // Xử lý hiệu ứng lá bài đặc biệt
        applyCardEffects(foundCard);

        // Chuyển lượt kế tiếp
        advanceTurn();
        broadcastGameState();
        serverControl.sendHandUpdate(userId, player.getHand());
        startTurnTimer();

        checkBotAutoPlay();
        return true;
    }

    /**
     * Xử lý yêu cầu bốc bài từ người chơi.
     */
    public synchronized void handleDrawCard(int userId) {
        Player player = room.getPlayer(userId);
        if (player == null || player.getSeatNumber() != gameState.getCurrentTurnSeat()) {
            return;
        }

        Card newCard = drawCardFromDeck();
        if (newCard != null) {
            player.addCard(newCard);
            gameState.setLastActionLog(player.getUsername() + " đã bốc 1 lá bài.");
            serverControl.sendHandUpdate(userId, player.getHand());
        }

        advanceTurn();
        broadcastGameState();
        startTurnTimer();

        checkBotAutoPlay();
    }

    /**
     * Hô UNO khi còn đúng 1 lá.
     */
    public synchronized void handleCallUno(int userId) {
        Player player = room.getPlayer(userId);
        if (player != null && player.getCardCount() == 1) {
            player.setUno(true);
            gameState.setLastActionLog(player.getUsername() + " đã hô: 'UNO!!!'");
            broadcastGameState();
        }
    }

    private void applyCardEffects(Card card) {
        int numPlayers = room.getCurrentPlayerCount();
        if (card.getValue() == CardValue.REVERSE) {
            if (numPlayers == 2) {
                // Với 2 người, Reverse hoạt động như Skip
                gameState.setCurrentTurnSeat(getNextSeat(gameState.getCurrentTurnSeat()));
            } else {
                gameState.setDirection(gameState.getDirection() * -1);
            }
        } else if (card.getValue() == CardValue.SKIP) {
            // Bỏ qua lượt người tiếp theo
            gameState.setCurrentTurnSeat(getNextSeat(gameState.getCurrentTurnSeat()));
        } else if (card.getValue() == CardValue.DRAW_TWO) {
            // Người tiếp theo bị bốc 2 lá và mất lượt
            int nextSeat = getNextSeat(gameState.getCurrentTurnSeat());
            Player nextPlayer = room.getPlayerBySeat(nextSeat);
            if (nextPlayer != null) {
                nextPlayer.addCard(drawCardFromDeck());
                nextPlayer.addCard(drawCardFromDeck());
                serverControl.sendHandUpdate(nextPlayer.getUserId(), nextPlayer.getHand());
            }
            gameState.setCurrentTurnSeat(nextSeat);
        } else if (card.getValue() == CardValue.WILD_DRAW_FOUR) {
            // Người tiếp theo bị bốc 4 lá và mất lượt
            int nextSeat = getNextSeat(gameState.getCurrentTurnSeat());
            Player nextPlayer = room.getPlayerBySeat(nextSeat);
            if (nextPlayer != null) {
                for (int i = 0; i < 4; i++) {
                    nextPlayer.addCard(drawCardFromDeck());
                }
                serverControl.sendHandUpdate(nextPlayer.getUserId(), nextPlayer.getHand());
            }
            gameState.setCurrentTurnSeat(nextSeat);
        }
    }

    private int getNextSeat(int currentSeat) {
        int num = room.getCurrentPlayerCount();
        int next = (currentSeat + gameState.getDirection()) % num;
        if (next < 0) next += num;
        return next;
    }

    private void advanceTurn() {
        gameState.setCurrentTurnSeat(getNextSeat(gameState.getCurrentTurnSeat()));
    }

    /**
     * Bắt đầu đếm ngược 15 giây cho lượt đi hiện tại.
     */
    private synchronized void startTurnTimer() {
        if (timerTask != null && !timerTask.isDone()) {
            timerTask.cancel(true);
        }
        gameState.setRemainingSeconds(15);

        timerTask = timerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                synchronized (GameManager.this) {
                    if (gameState.isGameOver()) {
                        timerTask.cancel(true);
                        return;
                    }
                    int rem = gameState.getRemainingSeconds() - 1;
                    gameState.setRemainingSeconds(rem);

                    if (rem <= 0) {
                        timerTask.cancel(true);
                        // Hết 15 giây: Server tự động cho người chơi bốc bài và chuyển lượt
                        Player currentPlayer = room.getPlayerBySeat(gameState.getCurrentTurnSeat());
                        if (currentPlayer != null) {
                            gameState.setLastActionLog("Hết 15s! " + currentPlayer.getUsername() + " bị tự động bốc bài.");
                            handleDrawCard(currentPlayer.getUserId());
                        }
                    }
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Kiểm tra nếu người đến lượt là Bot (do người chơi ngắt kết nối), Bot tự động đánh.
     */
    private void checkBotAutoPlay() {
        Player current = room.getPlayerBySeat(gameState.getCurrentTurnSeat());
        if (current != null && current.isBot()) {
            timerExecutor.schedule(() -> {
                synchronized (GameManager.this) {
                    botPlay(current);
                }
            }, 1500, TimeUnit.MILLISECONDS);
        }
    }

    private void botPlay(Player bot) {
        // Tìm lá bài hợp lệ đầu tiên để đánh
        Card validCard = null;
        for (Card c : bot.getHand()) {
            if (c.canPlayOn(gameState.getTopCard(), gameState.getActiveColor())) {
                validCard = c;
                break;
            }
        }

        if (validCard != null) {
            handlePlayCard(bot.getUserId(), validCard, CardColor.RED);
        } else {
            handleDrawCard(bot.getUserId());
        }
    }

    /**
     * Xử lý người chơi thoát trận: chuyển sang chế độ Bot để ván chơi tiếp tục.
     */
    public synchronized void handlePlayerDisconnect(int userId) {
        Player p = room.getPlayer(userId);
        if (p != null) {
            p.setBot(true);
            gameState.setLastActionLog(p.getUsername() + " đã ngắt kết nối. Bot sẽ tự động đánh thay!");
            broadcastGameState();
            checkBotAutoPlay();
        }
    }

    /**
     * Kết thúc ván chơi: Tính điểm và ghi nhận CSDL.
     */
    private void handleGameOver(Player winner) {
        if (timerTask != null) timerTask.cancel(true);

        gameState.setGameOver(true);
        gameState.setWinnerUsername(winner.getUsername());
        gameState.setLastActionLog("VÁN ĐẤU KẾT THÚC! NGƯỜI CHIẾN THẮNG: " + winner.getUsername());

        // Tính điểm: Người thắng được +100 điểm, người thua bị trừ điểm
        Map<Integer, Integer> scoreChanges = new HashMap<Integer, Integer>();
        scoreChanges.put(winner.getUserId(), 100);
        userDAO.updateStats(winner.getUserId(), 100, true);

        for (Player p : room.getPlayers()) {
            if (p.getUserId() != winner.getUserId()) {
                scoreChanges.put(p.getUserId(), -20);
                userDAO.updateStats(p.getUserId(), -20, false);
            }
        }

        // Lưu vào CSDL qua MatchDAO
        matchDAO.saveMatchResult(room.getName(), winner.getUserId(), totalTurnsCount, room.getPlayers(), scoreChanges);

        room.setStatus(Room.STATUS_WAITING);
        broadcastGameState();
    }

    /**
     * Gửi trạng thái bàn cờ hiện tại tới tất cả người chơi trong phòng.
     */
    private void broadcastGameState() {
        gameState.setDrawPileCount(drawPile != null ? drawPile.size() : 0);

        List<GameState.PlayerSummary> summaries = new ArrayList<GameState.PlayerSummary>();
        for (Player p : room.getPlayers()) {
            summaries.add(new GameState.PlayerSummary(
                    p.getUserId(), p.getUsername(), p.getSeatNumber(),
                    p.getCardCount(), p.isUno(), p.isBot()
            ));
        }
        gameState.setPlayerSummaries(summaries);

        serverControl.broadcastToRoom(room.getId(), gameState);
    }

    public GameState getGameState() {
        return gameState;
    }
}

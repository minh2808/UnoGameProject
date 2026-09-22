package com.ptit.uno.client.view;

import com.ptit.uno.model.Card;
import com.ptit.uno.model.CardColor;
import com.ptit.uno.model.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện bàn cờ UNO chính thức (GameFrm):
 * Hiển thị nọc bài giữa bàn, lá bài hiện tại, 15s đếm ngược, bài trên tay người chơi, đối thủ.
 * Passive View chuẩn MVC Cải tiến.
 */
public class GameFrm extends JFrame {
    private JLabel lblTurnInfo;
    private JLabel lblTimer;
    private JLabel lblDirection;
    private JLabel lblActionLog;

    // Khu vực giữa bàn
    private JButton btnDrawPile;
    private JButton btnTopCard;
    private JLabel lblActiveColor;

    // Khu vực đối thủ xung quanh bàn
    private JLabel lblOpponentTop;
    private JLabel lblOpponentLeft;
    private JLabel lblOpponentRight;

    // Khu vực bài trên tay người chơi
    private JPanel handPanel;
    private JScrollPane scrollHand;
    private JButton btnCallUno;
    private JButton btnLeave;

    private List<Card> currentHand;
    private Card selectedCardToPlay;
    private CardColor selectedWildColor;
    private ActionListener playCardActionListener;

    public GameFrm() {
        super("UNO Multiplayer - Bàn chơi (In-Game)");
        this.currentHand = new ArrayList<Card>();
        initComponents();
    }

    private void initComponents() {
        this.setSize(1000, 700);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(20, 80, 40)); // Màu thảm bàn cờ casino xanh lá
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // --- 1. TOP PANEL: ĐỐI THỦ PHÍA TRÊN & THÔNG TIN LƯỢT ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        statusPanel.setOpaque(false);
        lblTurnInfo = new JLabel("Lượt chơi: Đang kết nối...");
        lblTurnInfo.setFont(new Font("Arial", Font.BOLD, 16));
        lblTurnInfo.setForeground(Color.WHITE);

        lblTimer = new JLabel("⏳ 15s");
        lblTimer.setFont(new Font("Arial", Font.BOLD, 18));
        lblTimer.setForeground(Color.YELLOW);

        lblDirection = new JLabel("Chiều: Xuôi ↻");
        lblDirection.setFont(new Font("Arial", Font.BOLD, 14));
        lblDirection.setForeground(Color.CYAN);

        statusPanel.add(lblTurnInfo);
        statusPanel.add(lblTimer);
        statusPanel.add(lblDirection);
        topPanel.add(statusPanel, BorderLayout.NORTH);

        lblOpponentTop = createOpponentLabel("[Đối thủ 2]");
        topPanel.add(lblOpponentTop, BorderLayout.CENTER);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // --- 2. CENTER PANEL: BÀN CỜ (NỌC RÚT & LÁ BÀI TRÊN BÀN) ---
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);

        // Nọc bài rút (Draw Pile)
        btnDrawPile = new JButton();
        ImageIcon backImg = CardImageLoader.getCardBackIcon(100, 150);
        if (backImg != null) {
            btnDrawPile.setIcon(backImg);
            btnDrawPile.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
            btnDrawPile.setContentAreaFilled(false);
        } else {
            btnDrawPile.setText("<html><center><b>NỌC RÚT</b><br>(Click bốc bài)</center></html>");
            btnDrawPile.setBackground(new Color(40, 40, 40));
            btnDrawPile.setForeground(Color.WHITE);
            btnDrawPile.setFont(new Font("Arial", Font.BOLD, 12));
        }
        btnDrawPile.setPreferredSize(new Dimension(100, 150));
        btnDrawPile.setToolTipText("Bốc bài từ nọc rút");
        gbc.gridx = 0;
        gbc.gridy = 0;
        centerPanel.add(btnDrawPile, gbc);

        // Lá bài hiện tại trên bàn (Discard Pile)
        btnTopCard = new JButton();
        btnTopCard.setPreferredSize(new Dimension(100, 150));
        btnTopCard.setFont(new Font("Arial", Font.BOLD, 14));
        btnTopCard.setEnabled(false);
        btnTopCard.setContentAreaFilled(false);
        gbc.gridx = 1;
        gbc.gridy = 0;
        centerPanel.add(btnTopCard, gbc);

        // Màu hiệu lực & Nhật ký hành động
        lblActiveColor = new JLabel("Màu hiệu lực: ĐANG CHỜ", JLabel.CENTER);
        lblActiveColor.setFont(new Font("Arial", Font.BOLD, 14));
        lblActiveColor.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        centerPanel.add(lblActiveColor, gbc);

        lblActionLog = new JLabel("Sẵn sàng ván đấu.", JLabel.CENTER);
        lblActionLog.setFont(new Font("Arial", Font.ITALIC, 13));
        lblActionLog.setForeground(new Color(220, 255, 220));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        centerPanel.add(lblActionLog, gbc);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // --- 3. LEFT & RIGHT: ĐỐI THỦ HAI BÊN ---
        lblOpponentLeft = createOpponentLabel("[Đối thủ 1]");
        mainPanel.add(lblOpponentLeft, BorderLayout.WEST);

        lblOpponentRight = createOpponentLabel("[Đối thủ 3]");
        mainPanel.add(lblOpponentRight, BorderLayout.EAST);

        // --- 4. BOTTOM PANEL: BÀI TRÊN TAY CỦA NGƯỜI CHƠI ---
        JPanel bottomContainer = new JPanel(new BorderLayout(5, 5));
        bottomContainer.setOpaque(false);

        // Thanh nút điều khiển
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);
        btnCallUno = new JButton("🚨 HÔ UNO!");
        btnCallUno.setBackground(new Color(220, 40, 40));
        btnCallUno.setForeground(Color.WHITE);
        btnCallUno.setFont(new Font("Arial", Font.BOLD, 13));

        btnLeave = new JButton("Thoát bàn");
        btnLeave.setForeground(Color.DARK_GRAY);

        bottomBar.add(btnCallUno);
        bottomBar.add(btnLeave);
        bottomContainer.add(bottomBar, BorderLayout.NORTH);

        // Dải bài trên tay (Scrollable)
        handPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        handPanel.setBackground(new Color(15, 60, 30));
        scrollHand = new JScrollPane(handPanel);
        scrollHand.setPreferredSize(new Dimension(950, 170));
        scrollHand.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE),
                "Bài trên tay của bạn", 0, 0,
                new Font("Arial", Font.BOLD, 13), Color.WHITE
        ));
        scrollHand.getViewport().setBackground(new Color(15, 60, 30));
        bottomContainer.add(scrollHand, BorderLayout.CENTER);

        mainPanel.add(bottomContainer, BorderLayout.SOUTH);

        this.setContentPane(mainPanel);
    }

    private JLabel createOpponentLabel(String text) {
        JLabel lbl = new JLabel("<html><center>👤 " + text + "<br><font color='yellow'>🂠 7 lá</font></center></html>", JLabel.CENTER);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        lbl.setForeground(Color.WHITE);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 100)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        return lbl;
    }

    /**
     * Cập nhật danh sách bài trên tay của người chơi.
     */
    public void renderHand(List<Card> hand) {
        this.currentHand = hand != null ? hand : new ArrayList<Card>();
        handPanel.removeAll();

        for (final Card card : this.currentHand) {
            JButton cardBtn = new JButton();
            ImageIcon cardIcon = CardImageLoader.getCardIcon(card, 85, 128);
            if (cardIcon != null) {
                cardBtn.setIcon(cardIcon);
                cardBtn.setContentAreaFilled(false);
                cardBtn.setBorder(BorderFactory.createLineBorder(new Color(30, 30, 30), 1));
            } else {
                cardBtn.setText(formatCardHtml(card));
                styleCardButton(cardBtn, card.getColor());
            }
            cardBtn.setPreferredSize(new Dimension(85, 128));
            cardBtn.setToolTipText(card.toString());

            cardBtn.addActionListener(e -> {
                selectedCardToPlay = card;
                if (card.isWild()) {
                    // Hỏi người chơi chọn màu gì
                    String[] options = {"ĐỎ (RED)", "VÀNG (YELLOW)", "XANH LÁ (GREEN)", "XANH DƯƠNG (BLUE)"};
                    int choice = JOptionPane.showOptionDialog(
                            this, "Hãy chọn màu sắc tiếp theo cho bàn chơi:", "Chọn màu bài Wild",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]
                    );
                    if (choice == 0) selectedWildColor = CardColor.RED;
                    else if (choice == 1) selectedWildColor = CardColor.YELLOW;
                    else if (choice == 2) selectedWildColor = CardColor.GREEN;
                    else if (choice == 3) selectedWildColor = CardColor.BLUE;
                    else selectedWildColor = CardColor.RED;
                } else {
                    selectedWildColor = null;
                }

                // Kích hoạt listener chuyển xuống Controller
                if (playCardActionListener != null) {
                    playCardActionListener.actionPerformed(e);
                }
            });

            handPanel.add(cardBtn);
        }

        handPanel.revalidate();
        handPanel.repaint();
    }

    /**
     * Cập nhật toàn bộ trạng thái bàn cờ nhận từ Server.
     */
    public void updateGameState(GameState state, int currentUserId) {
        if (state == null) return;

        // Cập nhật lá bài trên bàn
        Card top = state.getTopCard();
        if (top != null) {
            ImageIcon topIcon = CardImageLoader.getCardIcon(top, 100, 150);
            if (topIcon != null) {
                btnTopCard.setIcon(topIcon);
                btnTopCard.setDisabledIcon(topIcon);
                btnTopCard.setText("");
                btnTopCard.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            } else {
                btnTopCard.setText(formatCardHtml(top));
                styleCardButton(btnTopCard, top.getColor());
            }
            btnTopCard.setToolTipText("Lá bài trên bàn: " + top);
        }

        // Màu sắc có hiệu lực
        CardColor ac = state.getActiveColor();
        lblActiveColor.setText("Màu hiệu lực: " + (ac != null ? ac.name() : "Mặc định"));
        if (ac == CardColor.RED) lblActiveColor.setForeground(new Color(255, 100, 100));
        else if (ac == CardColor.YELLOW) lblActiveColor.setForeground(Color.YELLOW);
        else if (ac == CardColor.GREEN) lblActiveColor.setForeground(new Color(100, 255, 100));
        else if (ac == CardColor.BLUE) lblActiveColor.setForeground(new Color(100, 200, 255));

        // Đồng hồ và chiều quay
        lblTimer.setText("⏳ " + state.getRemainingSeconds() + "s");
        lblDirection.setText("Chiều: " + (state.getDirection() == 1 ? "Xuôi ↻" : "Ngược ↺"));
        lblActionLog.setText(state.getLastActionLog() != null ? state.getLastActionLog() : "");

        ImageIcon backIcon = CardImageLoader.getCardBackIcon(100, 150);
        if (backIcon != null) {
            btnDrawPile.setIcon(backIcon);
            btnDrawPile.setText("");
            btnDrawPile.setToolTipText("Nọc rút: " + state.getDrawPileCount() + " lá bài (Click để bốc)");
        } else {
            btnDrawPile.setText("<html><center><b>NỌC RÚT</b><br>(" + state.getDrawPileCount() + " lá)</center></html>");
        }

        // Cập nhật thông tin đối thủ xung quanh bàn
        List<GameState.PlayerSummary> list = state.getPlayerSummaries();
        if (list != null) {
            int oppIndex = 0;
            JLabel[] oppLabels = {lblOpponentLeft, lblOpponentTop, lblOpponentRight};

            for (GameState.PlayerSummary p : list) {
                if (p.getUserId() == currentUserId) {
                    boolean isMyTurn = (p.getSeatNumber() == state.getCurrentTurnSeat());
                    lblTurnInfo.setText(isMyTurn ? "👉 LƯỢT CỦA BẠN!" : "Lượt của: " + getTurnPlayerName(list, state.getCurrentTurnSeat()));
                    lblTurnInfo.setForeground(isMyTurn ? Color.YELLOW : Color.WHITE);
                } else if (oppIndex < oppLabels.length) {
                    boolean isTheirTurn = (p.getSeatNumber() == state.getCurrentTurnSeat());
                    oppLabels[oppIndex].setText("<html><center>" + (isTheirTurn ? "👉 " : "") +
                            p.getUsername() + (p.isBot() ? " (Bot)" : "") + "<br>" +
                            "<font color='yellow'>🂠 " + p.getCardCount() + " lá</font>" +
                            (p.isUno() ? " <font color='red'><b>[UNO!]</b></font>" : "") +
                            "</center></html>");
                    oppIndex++;
                }
            }
        }

        if (state.isGameOver()) {
            JOptionPane.showMessageDialog(this,
                    "🎉 TRẬN ĐẤU ĐÃ KẾT THÚC!\nNgười chiến thắng: " + state.getWinnerUsername(),
                    "Kết quả ván đấu", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String getTurnPlayerName(List<GameState.PlayerSummary> list, int turnSeat) {
        for (GameState.PlayerSummary p : list) {
            if (p.getSeatNumber() == turnSeat) return p.getUsername();
        }
        return "Ghế #" + turnSeat;
    }

    private void styleCardButton(JButton btn, CardColor color) {
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        if (color == CardColor.RED) {
            btn.setBackground(new Color(210, 40, 40));
            btn.setForeground(Color.WHITE);
        } else if (color == CardColor.YELLOW) {
            btn.setBackground(new Color(230, 190, 20));
            btn.setForeground(Color.BLACK);
        } else if (color == CardColor.GREEN) {
            btn.setBackground(new Color(40, 160, 50));
            btn.setForeground(Color.WHITE);
        } else if (color == CardColor.BLUE) {
            btn.setBackground(new Color(30, 100, 220));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(40, 40, 40));
            btn.setForeground(Color.WHITE);
        }
    }

    private String formatCardHtml(Card card) {
        return "<html><center><b>" + card.getColor() + "</b><br>" + card.getValue() + "</center></html>";
    }

    public Card getSelectedCardToPlay() {
        return selectedCardToPlay;
    }

    public CardColor getSelectedWildColor() {
        return selectedWildColor;
    }

    public void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    // --- BỘ ĐĂNG KÝ LISTENER CHUẨN MVC CẢI TIẾN ---
    public void addPlayCardListener(ActionListener log) {
        this.playCardActionListener = log;
    }

    public void addDrawCardListener(ActionListener log) {
        btnDrawPile.addActionListener(log);
    }

    public void addCallUnoListener(ActionListener log) {
        btnCallUno.addActionListener(log);
    }

    public void addLeaveGameListener(ActionListener log) {
        btnLeave.addActionListener(log);
    }
}

package com.ptit.uno.client.view.fx.lobby;

import com.ptit.uno.model.Room;
import com.ptit.uno.model.User;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * LobbyUIBridge: Lớp cầu nối UI Code-Behind cho lobby_view.fxml.
 * Quản lý 2 bảng TableView phòng chơi và người online, bắt sự kiện và hiển thị
 * Dark Dialog tạo phòng.
 */
public class LobbyUIBridge implements Initializable {

    @FXML private Label lblUsername;
    @FXML private Label lblElo;
    @FXML private Label lblWinRate;
    @FXML
    private Button btnLeaderboard;
    @FXML
    private Button btnHistory;
    @FXML
    private Button btnLogout;

    @FXML private Label lblRoomCount;
    @FXML private HBox searchBoxContainer;
    @FXML private TextField txtSearchRoom;
    @FXML private ToggleGroup filterGroup;
    @FXML private ToggleButton tglFilterAll;
    @FXML private ToggleButton tglFilterWaiting;
    @FXML private ToggleButton tglFilterPlaying;
    @FXML private Button btnRefreshGrid;
    @FXML private FlowPane fpRoomContainer;
    @FXML private Button btnCreateRoom;

    private int selectedRoomId = -1;
    private final List<Room> allRooms = new ArrayList<>();

    @FXML
    private TableView<OnlineUserRow> tblOnline;
    @FXML
    private TableColumn<OnlineUserRow, String> colOnlineName;
    @FXML
    private TableColumn<OnlineUserRow, Number> colOnlineElo;
    @FXML
    private TableColumn<OnlineUserRow, String> colOnlineStatus;

    @FXML private Button btnQuickMatch;
    @FXML private Button btnInviteSelected;

    private final ObservableList<OnlineUserRow> onlineList = FXCollections.observableArrayList();
    private String currentUsername = "";

    private Runnable onCreateRoomAction;
    private Runnable onJoinRoomAction;
    private Runnable onRefreshAction;
    private Runnable onLeaderboardAction;
    private Runnable onHistoryAction;
    private Runnable onLogoutAction;
    private Runnable onSpectateAction;
    private java.util.function.Consumer<String> onInvitePlayerAction;

    private String lastCreatedRoomName = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (btnRefreshGrid != null) {
            btnRefreshGrid.setOnAction(e -> {
                if (onRefreshAction != null) onRefreshAction.run();
            });
        }

        // Bắt sự kiện ô tìm kiếm phòng (Search Box)
        if (txtSearchRoom != null) {
            txtSearchRoom.textProperty().addListener((obs, oldVal, newVal) -> applyRoomFilter());

            if (searchBoxContainer != null) {
                txtSearchRoom.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    if (isFocused) {
                        if (!searchBoxContainer.getStyleClass().contains("lobby-search-box-focused")) {
                            searchBoxContainer.getStyleClass().add("lobby-search-box-focused");
                        }
                    } else {
                        searchBoxContainer.getStyleClass().remove("lobby-search-box-focused");
                    }
                });
            }
        }

        // Bắt sự kiện bộ lọc trạng thái phòng (Segmented buttons)
        if (filterGroup != null) {
            filterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    // Ngăn không cho bỏ chọn tất cả nút (luôn giữ 1 nút active)
                    if (oldVal != null) {
                        oldVal.setSelected(true);
                    } else if (tglFilterAll != null) {
                        tglFilterAll.setSelected(true);
                    }
                } else {
                    applyRoomFilter();
                }
            });
        } else {
            // Fallback nếu filterGroup không được bind
            if (tglFilterAll != null) tglFilterAll.setOnAction(e -> applyRoomFilter());
            if (tglFilterWaiting != null) tglFilterWaiting.setOnAction(e -> applyRoomFilter());
            if (tglFilterPlaying != null) tglFilterPlaying.setOnAction(e -> applyRoomFilter());
        }

        // Cấu hình bảng Người chơi trực tuyến (3 cột chuẩn: Tên, Elo, Trạng thái)
        if (colOnlineName != null) {
            colOnlineName.setCellValueFactory(d -> d.getValue().nameProperty());
        }
        if (colOnlineElo != null) {
            colOnlineElo.setCellValueFactory(d -> d.getValue().eloProperty());
        }
        if (colOnlineStatus != null) {
            colOnlineStatus.setCellValueFactory(d -> d.getValue().statusProperty());
            colOnlineStatus.setCellFactory(col -> new TableCell<OnlineUserRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText("• " + item);
                        if ("Rảnh".equalsIgnoreCase(item)) {
                            setStyle("-fx-text-fill: #4ff0a0; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        } else if ("Đang chơi".equalsIgnoreCase(item)) {
                            setStyle("-fx-text-fill: #ff9f1c; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        } else {
                            setStyle("-fx-text-fill: #ff5b62; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        }
                    }
                }
            });
        }

        if (tblOnline != null) {
            tblOnline.setItems(onlineList);
        }

        // Bắt sự kiện 2 nút ở dưới cùng bảng Người chơi trực tuyến
        if (btnQuickMatch != null) {
            btnQuickMatch.setOnAction(e -> handleQuickMatch());
        }
        if (btnInviteSelected != null) {
            btnInviteSelected.setOnAction(e -> handleInviteSelected());
        }

        // Bắt sự kiện các nút khác
        if (btnCreateRoom != null) {
            btnCreateRoom.setOnAction(e -> {
                if (onCreateRoomAction != null) onCreateRoomAction.run();
            });
        }
        btnLeaderboard.setOnAction(e -> {
            if (onLeaderboardAction != null)
                onLeaderboardAction.run();
        });
        btnHistory.setOnAction(e -> {
            if (onHistoryAction != null)
                onHistoryAction.run();
        });
        btnLogout.setOnAction(e -> {
            if (onLogoutAction != null)
                onLogoutAction.run();
        });
    }

    /**
     * Xử lý nút "⚡ Ghép ngẫu nhiên":
     * Tự động tìm phòng chờ khả dụng đầu tiên và vào ngay. Nếu không có phòng, hỏi người chơi có muốn tạo phòng mới không.
     */
    private void handleQuickMatch() {
        Room targetRoom = null;
        for (Room r : allRooms) {
            boolean isPlaying = "PLAYING".equalsIgnoreCase(r.getStatus()) || r.isFull();
            if (!isPlaying) {
                targetRoom = r;
                break;
            }
        }

        if (targetRoom != null) {
            selectedRoomId = targetRoom.getId();
            if (onJoinRoomAction != null) {
                onJoinRoomAction.run();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Ghép trận ngẫu nhiên");
            alert.setHeaderText("Không tìm thấy phòng chơi đang chờ");
            alert.setContentText("Hiện tại không có phòng nào còn chỗ trống. Bạn có muốn tạo phòng mới ngay bây giờ không?");
            ButtonType btnYes = new ButtonType("Tạo phòng ngay", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancel = new ButtonType("Để sau", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnYes, btnCancel);

            try {
                alert.getDialogPane().getStylesheets().add(
                        getClass().getResource("/resource/css/uno_theme.css").toExternalForm());
                alert.getDialogPane().getStyleClass().add("auth-card");
            } catch (Exception ignored) {}

            Optional<ButtonType> opt = alert.showAndWait();
            if (opt.isPresent() && opt.get() == btnYes) {
                if (onCreateRoomAction != null) {
                    onCreateRoomAction.run();
                }
            }
        }
    }

    /**
     * Xử lý nút "✉ Mời bạn":
     * Kiểm tra người chơi được chọn trên bảng tblOnline và gửi lời mời nếu hợp lệ (đang Rảnh và không phải chính mình).
     */
    private void handleInviteSelected() {
        if (tblOnline == null) return;
        OnlineUserRow selected = tblOnline.getSelectionModel().getSelectedItem();

        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Mời bạn chơi");
            alert.setHeaderText("Chưa chọn người chơi");
            alert.setContentText("Vui lòng nhấp chọn một người chơi đang 'Rảnh' trong bảng trước khi gửi lời mời!");
            try {
                alert.getDialogPane().getStylesheets().add(
                        getClass().getResource("/resource/css/uno_theme.css").toExternalForm());
                alert.getDialogPane().getStyleClass().add("auth-card");
            } catch (Exception ignored) {}
            alert.showAndWait();
            return;
        }

        if (currentUsername != null && !currentUsername.isEmpty()
                && currentUsername.equalsIgnoreCase(selected.getName())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Mời bạn chơi");
            alert.setHeaderText("Không hợp lệ");
            alert.setContentText("Bạn không thể tự gửi lời mời thách đấu cho chính mình!");
            try {
                alert.getDialogPane().getStylesheets().add(
                        getClass().getResource("/resource/css/uno_theme.css").toExternalForm());
                alert.getDialogPane().getStyleClass().add("auth-card");
            } catch (Exception ignored) {}
            alert.showAndWait();
            return;
        }

        if (!"Rảnh".equalsIgnoreCase(selected.getStatus())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Mời bạn chơi");
            alert.setHeaderText("Người chơi đang bận");
            alert.setContentText("Người chơi '" + selected.getName() + "' hiện đang ở trạng thái '" + selected.getStatus() + "', không thể mời lúc này!");
            try {
                alert.getDialogPane().getStylesheets().add(
                        getClass().getResource("/resource/css/uno_theme.css").toExternalForm());
                alert.getDialogPane().getStyleClass().add("auth-card");
            } catch (Exception ignored) {}
            alert.showAndWait();
            return;
        }

        // Debounce hiệu ứng nút
        if (btnInviteSelected != null) {
            String originalText = btnInviteSelected.getText();
            btnInviteSelected.setText("✓ Đã gửi mời!");
            btnInviteSelected.setDisable(true);

            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(ev -> {
                btnInviteSelected.setText(originalText);
                btnInviteSelected.setDisable(false);
            });
            pause.play();
        }

        if (onInvitePlayerAction != null) {
            onInvitePlayerAction.accept(selected.getName());
        }
    }

    public void updateUserInfo(User user) {
        Platform.runLater(() -> {
            if (user != null) {
                this.currentUsername = user.getUsername();
                if (lblUsername != null) lblUsername.setText(user.getUsername());
                if (lblElo != null) lblElo.setText(user.getScore() + " Elo");
                if (lblWinRate != null) lblWinRate.setText(String.format("Thắng %d/%d", user.getWinMatches(), user.getTotalMatches()));
                if (tblOnline != null) tblOnline.refresh();
            }
        });
    }

    public void updateRoomList(List<Room> rooms) {
        Platform.runLater(() -> {
            allRooms.clear();
            if (rooms != null) {
                allRooms.addAll(rooms);
            }
            applyRoomFilter();
        });
    }

    private void applyRoomFilter() {
        if (fpRoomContainer == null) return;

        String keyword = (txtSearchRoom != null && txtSearchRoom.getText() != null)
                ? txtSearchRoom.getText().trim().toLowerCase() : "";

        String statusFilter = "ALL";
        if (tglFilterWaiting != null && tglFilterWaiting.isSelected()) {
            statusFilter = "WAITING";
        } else if (tglFilterPlaying != null && tglFilterPlaying.isSelected()) {
            statusFilter = "PLAYING";
        }

        List<Room> filtered = new ArrayList<>();
        for (Room r : allRooms) {
            boolean isPlaying = "PLAYING".equalsIgnoreCase(r.getStatus()) || r.isFull();

            // 1. Lọc theo trạng thái phòng
            if ("WAITING".equals(statusFilter) && isPlaying) {
                continue;
            }
            if ("PLAYING".equals(statusFilter) && !isPlaying) {
                continue;
            }

            // 2. Lọc theo từ khóa tìm kiếm (ID, tên phòng, hoặc chủ phòng)
            if (!keyword.isEmpty()) {
                String idStr = String.valueOf(r.getId());
                String formattedId = String.format("#%02d", r.getId()).toLowerCase();
                String name = (r.getName() != null) ? r.getName().toLowerCase() : "";
                String host = (r.getHostName() != null) ? r.getHostName().toLowerCase() : "";

                boolean matchId = idStr.contains(keyword) || formattedId.contains(keyword);
                boolean matchName = name.contains(keyword);
                boolean matchHost = host.contains(keyword);

                if (!matchId && !matchName && !matchHost) {
                    continue;
                }
            }

            filtered.add(r);
        }

        renderRoomsToContainer(filtered);
    }

    private void renderRoomsToContainer(List<Room> rooms) {
        fpRoomContainer.getChildren().clear();

        if (lblRoomCount != null) {
            if (allRooms.isEmpty()) {
                lblRoomCount.setText("0 phòng");
            } else if (rooms.size() == allRooms.size()) {
                lblRoomCount.setText(rooms.size() + " phòng");
            } else {
                lblRoomCount.setText(rooms.size() + "/" + allRooms.size() + " phòng");
            }
        }

        if (rooms.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(50, 20, 50, 20));
            emptyBox.prefWidthProperty().bind(fpRoomContainer.widthProperty().subtract(30));

            Label lblEmpty = new Label("Không tìm thấy phòng chơi nào phù hợp");
            lblEmpty.setStyle("-fx-text-fill: #8c95a6; -fx-font-size: 14px; -fx-font-weight: bold;");

            Label lblSub = new Label("Vui lòng thử đổi từ khóa tìm kiếm hoặc chọn bộ lọc 'Tất cả'");
            lblSub.setStyle("-fx-text-fill: #5b6577; -fx-font-size: 12px;");

            emptyBox.getChildren().addAll(lblEmpty, lblSub);
            fpRoomContainer.getChildren().add(emptyBox);
        } else {
            for (Room r : rooms) {
                fpRoomContainer.getChildren().add(createRoomCard(r));
            }
        }
    }

    private VBox createRoomCard(Room r) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #1a1e27; -fx-border-color: #2a303c; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12;");
        
        // Tự động scale chiều rộng sao cho 1 hàng 2 thẻ (trừ đi khoảng cách hgap và scrollbar)
        card.prefWidthProperty().bind(fpRoomContainer.widthProperty().subtract(30).divide(2));
        
        // Header
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblId = new Label(String.format("#%02d", r.getId()));
        lblId.setStyle("-fx-text-fill: #4c8bf5; -fx-font-weight: bold;");
        Label lblName = new Label(r.getName());
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        lblName.setMaxWidth(180);
        
        javafx.scene.layout.Region spacer1 = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer1, javafx.scene.layout.Priority.ALWAYS);
        
        boolean isPlaying = "PLAYING".equalsIgnoreCase(r.getStatus()) || r.isFull();
        String statusText = isPlaying ? "Đang chơi" : "Đang chờ";
        String statusColor = isPlaying ? "#8c95a6" : "#4ff0a0";
        
        Label lblStatus = new Label("• " + statusText);
        lblStatus.setStyle("-fx-text-fill: " + statusColor + "; -fx-background-color: transparent; -fx-border-color: " + statusColor + "40; -fx-border-radius: 4; -fx-padding: 2 6; -fx-font-size: 11px;");
        
        header.getChildren().addAll(lblId, lblName, spacer1, lblStatus);
        
        // Info
        VBox infoBox = new VBox(4);
        Label lblHost = new Label("Chủ phòng: " + r.getHostName());
        lblHost.setStyle("-fx-text-fill: #b3bccf; -fx-font-size: 12px;");
        Label lblMode = new Label("Chế độ: Cơ bản 1v1 • Cược Elo: 100");
        lblMode.setStyle("-fx-text-fill: #b3bccf; -fx-font-size: 12px;");
        infoBox.getChildren().addAll(lblHost, lblMode);
        
        // Divider
        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-background-color: #2a303c; -fx-padding: 0;");
        
        // Bottom
        javafx.scene.layout.HBox bottom = new javafx.scene.layout.HBox(10);
        bottom.setAlignment(Pos.CENTER_LEFT);
        String pCount = r.getCurrentPlayerCount() + "/" + r.getMaxPlayers();
        if (r.isFull()) pCount += " (Đầy)";
        Label lblPlayers = new Label("Người: " + pCount);
        lblPlayers.setStyle("-fx-text-fill: #b3bccf; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        javafx.scene.layout.Region spacer2 = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer2, javafx.scene.layout.Priority.ALWAYS);
        
        Button btnAction = new Button(isPlaying ? "Xem trận" : "Vào phòng");
        if (isPlaying) {
            btnAction.setStyle("-fx-background-color: #2a303c; -fx-text-fill: white; -fx-padding: 6 12; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
            btnAction.setOnAction(e -> {
                selectedRoomId = r.getId();
                if (onSpectateAction != null) onSpectateAction.run();
            });
        } else {
            btnAction.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 12; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
            btnAction.setOnAction(e -> {
                selectedRoomId = r.getId();
                if (onJoinRoomAction != null) onJoinRoomAction.run();
            });
        }
        
        bottom.getChildren().addAll(lblPlayers, spacer2, btnAction);
        
        card.getChildren().addAll(header, infoBox, sep, bottom);
        
        // Hover
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1a1e27; -fx-border-color: #3b82f6; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #1a1e27; -fx-border-color: #2a303c; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12;"));
        
        return card;
    }

    public void updateOnlineUsers(List<User> users) {
        Platform.runLater(() -> {
            onlineList.clear();
            if (users != null) {
                int idx = 1;
                for (User u : users) {
                    String status = "Rảnh";
                    if (u.getStatus() != null && !u.getStatus().equalsIgnoreCase("ONLINE")
                            && !u.getStatus().equalsIgnoreCase("Rảnh")) {
                        status = u.getStatus();
                    }
                    onlineList.add(new OnlineUserRow(idx++, u.getUsername(), u.getScore(), status));
                }
            }
        });
    }

    public int getSelectedRoomId() {
        return selectedRoomId;
    }

    public String promptCreateRoomName() {
        TextInputDialog dialog = new TextInputDialog("Phòng UNO mới");
        dialog.setTitle("Tạo phòng UNO");
        dialog.setHeaderText("Nhập tên phòng mới để bắt đầu ván đấu:");
        dialog.setContentText("Tên phòng:");

        // Load stylesheet cho dialog
        try {
            dialog.getDialogPane().getStylesheets().add(
                    getClass().getResource("/resource/css/uno_theme.css").toExternalForm());
            dialog.getDialogPane().getStyleClass().add("auth-card");
        } catch (Exception ignored) {
        }

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            lastCreatedRoomName = result.get().trim();
            return lastCreatedRoomName;
        }
        return null;
    }

    public String getLastCreatedRoomName() {
        return lastCreatedRoomName;
    }

    public void setOnCreateRoomAction(Runnable r) {
        this.onCreateRoomAction = r;
    }

    public void setOnJoinRoomAction(Runnable r) {
        this.onJoinRoomAction = r;
    }

    public void setOnRefreshAction(Runnable r) {
        this.onRefreshAction = r;
    }

    public void setOnLeaderboardAction(Runnable r) {
        this.onLeaderboardAction = r;
    }

    public void setOnHistoryAction(Runnable r) {
        this.onHistoryAction = r;
    }

    public void setOnLogoutAction(Runnable r) {
        this.onLogoutAction = r;
    }

    public void setOnSpectateAction(Runnable r) {
        this.onSpectateAction = r;
    }

    public void setOnInvitePlayerAction(java.util.function.Consumer<String> action) {
        this.onInvitePlayerAction = action;
    }



    public static class OnlineUserRow {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty name;
        private final SimpleIntegerProperty elo;
        private final SimpleStringProperty status;

        public OnlineUserRow(int id, String name, int elo, String status) {
            this.id = new SimpleIntegerProperty(id);
            this.name = new SimpleStringProperty(name);
            this.elo = new SimpleIntegerProperty(elo);
            this.status = new SimpleStringProperty(status);
        }

        public SimpleIntegerProperty idProperty() {
            return id;
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleIntegerProperty eloProperty() {
            return elo;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public int getId() {
            return id.get();
        }

        public String getName() {
            return name.get();
        }

        public int getElo() {
            return elo.get();
        }

        public String getStatus() {
            return status.get();
        }
    }
}

package com.ptit.uno.client.view.fx.lobby;

import com.ptit.uno.model.Room;
import com.ptit.uno.model.User;
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
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * LobbyUIBridge: Lớp cầu nối UI Code-Behind cho lobby_view.fxml.
 * Quản lý 2 bảng TableView phòng chơi và người online, bắt sự kiện và hiển thị Dark Dialog tạo phòng.
 */
public class LobbyUIBridge implements Initializable {

    @FXML private Label lblUserInfo;
    @FXML private Button btnLeaderboard;
    @FXML private Button btnHistory;
    @FXML private Button btnLogout;

    @FXML private TableView<RoomRow> tblRooms;
    @FXML private TableColumn<RoomRow, Number> colRoomId;
    @FXML private TableColumn<RoomRow, String> colRoomName;
    @FXML private TableColumn<RoomRow, String> colRoomHost;
    @FXML private TableColumn<RoomRow, String> colRoomPlayers;
    @FXML private TableColumn<RoomRow, String> colRoomStatus;

    @FXML private Button btnCreateRoom;
    @FXML private Button btnJoinRoom;
    @FXML private Button btnSpectate;
    @FXML private Button btnRefresh;

    @FXML private TableView<OnlineUserRow> tblOnline;
    @FXML private TableColumn<OnlineUserRow, Number> colOnlineId;
    @FXML private TableColumn<OnlineUserRow, String> colOnlineName;
    @FXML private TableColumn<OnlineUserRow, Number> colOnlineElo;
    @FXML private TableColumn<OnlineUserRow, String> colOnlineStatus;

    private final ObservableList<RoomRow> roomList = FXCollections.observableArrayList();
    private final ObservableList<OnlineUserRow> onlineList = FXCollections.observableArrayList();

    private Runnable onCreateRoomAction;
    private Runnable onJoinRoomAction;
    private Runnable onRefreshAction;
    private Runnable onLeaderboardAction;
    private Runnable onHistoryAction;
    private Runnable onLogoutAction;
    private Runnable onSpectateAction;

    private String lastCreatedRoomName = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Cấu hình bảng Phòng chơi
        colRoomId.setCellValueFactory(d -> d.getValue().idProperty());
        colRoomName.setCellValueFactory(d -> d.getValue().nameProperty());
        colRoomHost.setCellValueFactory(d -> d.getValue().hostProperty());
        colRoomPlayers.setCellValueFactory(d -> d.getValue().playersProperty());
        colRoomStatus.setCellValueFactory(d -> d.getValue().statusProperty());

        // Định dạng màu sắc cho cột trạng thái phòng
        colRoomStatus.setCellFactory(col -> new TableCell<RoomRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Đang chờ".equalsIgnoreCase(item) || item.contains("chờ")) {
                        setStyle("-fx-text-fill: #4ff0a0; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ff9f1c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tblRooms.setItems(roomList);
        tblRooms.setRowFactory(tv -> {
            TableRow<RoomRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    if (onJoinRoomAction != null) {
                        onJoinRoomAction.run();
                    }
                }
            });
            return row;
        });

        // Cấu hình bảng Người chơi trực tuyến
        colOnlineId.setCellValueFactory(d -> d.getValue().idProperty());
        colOnlineName.setCellValueFactory(d -> d.getValue().nameProperty());
        colOnlineElo.setCellValueFactory(d -> d.getValue().eloProperty());
        colOnlineStatus.setCellValueFactory(d -> d.getValue().statusProperty());

        colOnlineStatus.setCellFactory(col -> new TableCell<OnlineUserRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Rảnh".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #4ff0a0; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ff5b62; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tblOnline.setItems(onlineList);

        // Bắt sự kiện các nút
        btnCreateRoom.setOnAction(e -> {
            if (onCreateRoomAction != null) onCreateRoomAction.run();
        });
        btnJoinRoom.setOnAction(e -> {
            if (onJoinRoomAction != null) onJoinRoomAction.run();
        });
        btnRefresh.setOnAction(e -> {
            if (onRefreshAction != null) onRefreshAction.run();
        });
        btnLeaderboard.setOnAction(e -> {
            if (onLeaderboardAction != null) onLeaderboardAction.run();
        });
        btnHistory.setOnAction(e -> {
            if (onHistoryAction != null) onHistoryAction.run();
        });
        btnLogout.setOnAction(e -> {
            if (onLogoutAction != null) onLogoutAction.run();
        });
        btnSpectate.setOnAction(e -> {
            if (onSpectateAction != null) onSpectateAction.run();
        });
    }

    public void updateUserInfo(User user) {
        Platform.runLater(() -> {
            if (user != null && lblUserInfo != null) {
                lblUserInfo.setText(String.format("Xin chào: %s | Điểm Elo: %d | Thắng: %d/%d",
                        user.getUsername(), user.getScore(), user.getWinMatches(), user.getTotalMatches()));
            }
        });
    }

    public void updateRoomList(List<Room> rooms) {
        Platform.runLater(() -> {
            roomList.clear();
            if (rooms != null) {
                for (Room r : rooms) {
                    String status = "Đang chờ";
                    if ("PLAYING".equalsIgnoreCase(r.getStatus()) || r.isFull()) {
                        status = "Đang chơi";
                    }
                    roomList.add(new RoomRow(r.getId(), r.getName(), r.getHostName(),
                            r.getCurrentPlayerCount() + "/2", status));
                }
            }
        });
    }

    public void updateOnlineUsers(List<User> users) {
        Platform.runLater(() -> {
            onlineList.clear();
            if (users != null) {
                int idx = 1;
                for (User u : users) {
                    String status = "Rảnh";
                    if (u.getStatus() != null && !u.getStatus().equalsIgnoreCase("ONLINE") && !u.getStatus().equalsIgnoreCase("Rảnh")) {
                        status = u.getStatus();
                    }
                    onlineList.add(new OnlineUserRow(idx++, u.getUsername(), u.getScore(), status));
                }
            }
        });
    }

    public int getSelectedRoomId() {
        RoomRow sel = tblRooms.getSelectionModel().getSelectedItem();
        return sel != null ? sel.getId() : -1;
    }

    public String promptCreateRoomName() {
        TextInputDialog dialog = new TextInputDialog("Phòng UNO mới");
        dialog.setTitle("Tạo phòng UNO");
        dialog.setHeaderText("Nhập tên phòng mới để bắt đầu ván đấu:");
        dialog.setContentText("Tên phòng:");
        
        // Load stylesheet cho dialog
        try {
            dialog.getDialogPane().getStylesheets().add(
                    getClass().getResource("/resource/css/uno_theme.css").toExternalForm()
            );
            dialog.getDialogPane().getStyleClass().add("auth-card");
        } catch (Exception ignored) {}

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

    public void setOnCreateRoomAction(Runnable r) { this.onCreateRoomAction = r; }
    public void setOnJoinRoomAction(Runnable r) { this.onJoinRoomAction = r; }
    public void setOnRefreshAction(Runnable r) { this.onRefreshAction = r; }
    public void setOnLeaderboardAction(Runnable r) { this.onLeaderboardAction = r; }
    public void setOnHistoryAction(Runnable r) { this.onHistoryAction = r; }
    public void setOnLogoutAction(Runnable r) { this.onLogoutAction = r; }
    public void setOnSpectateAction(Runnable r) { this.onSpectateAction = r; }

    // --- DATA MODEL CHO JAVAFX TABLEVIEW ---
    public static class RoomRow {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty host;
        private final SimpleStringProperty players;
        private final SimpleStringProperty status;

        public RoomRow(int id, String name, String host, String players, String status) {
            this.id = new SimpleIntegerProperty(id);
            this.name = new SimpleStringProperty(name);
            this.host = new SimpleStringProperty(host);
            this.players = new SimpleStringProperty(players);
            this.status = new SimpleStringProperty(status);
        }

        public int getId() { return id.get(); }
        public SimpleIntegerProperty idProperty() { return id; }
        public SimpleStringProperty nameProperty() { return name; }
        public SimpleStringProperty hostProperty() { return host; }
        public SimpleStringProperty playersProperty() { return players; }
        public SimpleStringProperty statusProperty() { return status; }
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

        public SimpleIntegerProperty idProperty() { return id; }
        public SimpleStringProperty nameProperty() { return name; }
        public SimpleIntegerProperty eloProperty() { return elo; }
        public SimpleStringProperty statusProperty() { return status; }
    }
}

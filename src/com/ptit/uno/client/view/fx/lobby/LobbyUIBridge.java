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
import javafx.scene.layout.*;

import java.net.URL;
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
    @FXML private TextField txtSearchRoom;
    @FXML private ToggleButton tglFilterAll;
    @FXML private ToggleButton tglFilterWaiting;
    @FXML private ToggleButton tglFilterPlaying;
    @FXML private Button btnRefreshGrid;
    @FXML private FlowPane fpRoomContainer;
    @FXML private Button btnCreateRoom;

    private int selectedRoomId = -1;

    @FXML
    private TableView<OnlineUserRow> tblOnline;
    @FXML
    private TableColumn<OnlineUserRow, Number> colOnlineId;
    @FXML
    private TableColumn<OnlineUserRow, String> colOnlineName;
    @FXML
    private TableColumn<OnlineUserRow, Number> colOnlineElo;
    @FXML
    private TableColumn<OnlineUserRow, String> colOnlineStatus;

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
        if (btnRefreshGrid != null) {
            btnRefreshGrid.setOnAction(e -> {
                if (onRefreshAction != null) onRefreshAction.run();
            });
        }

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

    public void updateUserInfo(User user) {
        Platform.runLater(() -> {
            if (user != null) {
                if (lblUsername != null) lblUsername.setText(user.getUsername());
                if (lblElo != null) lblElo.setText(user.getScore() + " Elo");
                if (lblWinRate != null) lblWinRate.setText(String.format("Thắng %d/%d", user.getWinMatches(), user.getTotalMatches()));
            }
        });
    }

    public void updateRoomList(List<Room> rooms) {
        Platform.runLater(() -> {
            if (fpRoomContainer != null) {
                fpRoomContainer.getChildren().clear();
                if (rooms != null) {
                    if (lblRoomCount != null) {
                        lblRoomCount.setText(rooms.size() + " phòng");
                    }
                    for (Room r : rooms) {
                        fpRoomContainer.getChildren().add(createRoomCard(r));
                    }
                } else {
                    if (lblRoomCount != null) lblRoomCount.setText("0 phòng");
                }
            }
        });
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
    }
}

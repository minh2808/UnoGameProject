package com.ptit.uno.client.view.fx.lobby;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class MatchHistoryUIBridge {

    @FXML private TableView<HistoryRow> tblHistory;
    @FXML private TableColumn<HistoryRow, String> colId;
    @FXML private TableColumn<HistoryRow, String> colRoom;
    @FXML private TableColumn<HistoryRow, String> colOpponent;
    @FXML private TableColumn<HistoryRow, String> colResult;
    @FXML private TableColumn<HistoryRow, String> colScoreChange;
    @FXML private TableColumn<HistoryRow, String> colTime;

    @FXML private Label lblEmptyMessage;
    @FXML private Label lblSummary;
    @FXML private Button btnClose;
    @FXML private Label lblCloseIcon;

    private ObservableList<HistoryRow> listData = FXCollections.observableArrayList();
    private String currentUsername = "";

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("matchId"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        colOpponent.setCellValueFactory(new PropertyValueFactory<>("opponentName"));
        colResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colScoreChange.setCellValueFactory(new PropertyValueFactory<>("scoreChange"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        tblHistory.setItems(listData);

        btnClose.setOnAction(e -> closeWindow());
        lblCloseIcon.setOnMouseClicked(e -> closeWindow());
    }

    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username != null ? username : "";
    }

    public void renderHistory(List<Map<String, Object>> list) {
        listData.clear();
        int wins = 0;
        int total = 0;

        if (list != null && !list.isEmpty()) {
            lblEmptyMessage.setVisible(false);
            for (Map<String, Object> map : list) {
                total++;
                String matchId = String.valueOf(map.get("matchId"));
                String roomName = String.valueOf(map.get("roomName"));
                
                // Adapter cho DAO cũ và mới
                String opponent = "";
                String result = "";
                String scoreChange = String.valueOf(map.get("scoreChange"));
                
                if (map.containsKey("opponentName")) {
                    opponent = String.valueOf(map.get("opponentName"));
                    result = String.valueOf(map.get("result"));
                    if ("THẮNG".equalsIgnoreCase(result) || Boolean.TRUE.equals(map.get("isWin"))) {
                        wins++;
                        result = "THẮNG";
                    } else {
                        result = "THUA";
                    }
                } else {
                    // Fallback DAO cũ (winnerName)
                    String winner = String.valueOf(map.get("winnerName"));
                    if (winner.equals(currentUsername)) {
                        result = "THẮNG";
                        wins++;
                        opponent = "Không rõ"; // DAO cũ không trả về đối thủ
                    } else {
                        result = "THUA";
                        opponent = winner;
                    }
                }

                String endTime = String.valueOf(map.get("endTime"));

                listData.add(new HistoryRow(matchId, roomName, opponent, result, scoreChange, endTime));
            }
        } else {
            lblEmptyMessage.setVisible(true);
        }

        lblSummary.setText(String.format("Tổng: %d trận | Thắng %d | Thua %d", total, wins, total - wins));
    }

    public static class HistoryRow {
        private final SimpleStringProperty matchId;
        private final SimpleStringProperty roomName;
        private final SimpleStringProperty opponentName;
        private final SimpleStringProperty result;
        private final SimpleStringProperty scoreChange;
        private final SimpleStringProperty endTime;

        public HistoryRow(String matchId, String roomName, String opponentName, String result, String scoreChange, String endTime) {
            this.matchId = new SimpleStringProperty(matchId);
            this.roomName = new SimpleStringProperty(roomName);
            this.opponentName = new SimpleStringProperty(opponentName);
            this.result = new SimpleStringProperty(result);
            this.scoreChange = new SimpleStringProperty(scoreChange);
            this.endTime = new SimpleStringProperty(endTime);
        }

        public String getMatchId() { return matchId.get(); }
        public String getRoomName() { return roomName.get(); }
        public String getOpponentName() { return opponentName.get(); }
        public String getResult() { return result.get(); }
        public String getScoreChange() { return scoreChange.get(); }
        public String getEndTime() { return endTime.get(); }
    }
}
